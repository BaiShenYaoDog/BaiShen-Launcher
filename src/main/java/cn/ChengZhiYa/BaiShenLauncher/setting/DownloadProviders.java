package cn.ChengZhiYa.BaiShenLauncher.setting;

import cn.ChengZhiYa.BaiShenLauncher.download.*;
import cn.ChengZhiYa.BaiShenLauncher.task.DownloadException;
import cn.ChengZhiYa.BaiShenLauncher.task.FetchTask;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.Pair;
import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.io.ResponseCodeException;
import javafx.beans.InvalidationListener;

import javax.net.ssl.SSLHandshakeException;
import java.io.FileNotFoundException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.ChengZhiYa.BaiShenLauncher.setting.ConfigHolder.config;
import static cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n.i18n;

public final class DownloadProviders {
    public static final Map<String, DownloadProvider> providersById;
    public static final Map<String, DownloadProvider> rawProviders;
    public static final String DEFAULT_PROVIDER_ID = "balanced";
    public static final String DEFAULT_RAW_PROVIDER_ID = "mcbbs";
    private static final AdaptedDownloadProvider fileDownloadProvider = new AdaptedDownloadProvider();

    private static final MojangDownloadProvider MOJANG;
    private static final BMCLAPIDownloadProvider BMCLAPI;
    private static final BMCLAPIDownloadProvider MCBBS;
    private static final InvalidationListener observer;
    private static DownloadProvider currentDownloadProvider;

    static {
        String bmclapiRoot = "https://bmclapi2.bangbang93.com";
        String bmclapiRootOverride = System.getProperty("hmcl.bmclapi.override");
        if (bmclapiRootOverride != null) bmclapiRoot = bmclapiRootOverride;

        MOJANG = new MojangDownloadProvider();
        BMCLAPI = new BMCLAPIDownloadProvider(bmclapiRoot);
        MCBBS = new BMCLAPIDownloadProvider("https://download.mcbbs.net");
        rawProviders = Lang.mapOf(
                Pair.pair("mojang", MOJANG),
                Pair.pair("bmclapi", BMCLAPI),
                Pair.pair("mcbbs", MCBBS)
        );

        AdaptedDownloadProvider fileProvider = new AdaptedDownloadProvider();
        fileProvider.setDownloadProviderCandidates(Arrays.asList(MCBBS, BMCLAPI, MOJANG));
        BalancedDownloadProvider balanced = new BalancedDownloadProvider(Arrays.asList(MCBBS, BMCLAPI, MOJANG));

        providersById = Lang.mapOf(
                Pair.pair("official", new AutoDownloadProvider(MOJANG, fileProvider)),
                Pair.pair("balanced", new AutoDownloadProvider(balanced, fileProvider)),
                Pair.pair("mirror", new AutoDownloadProvider(MCBBS, fileProvider)));

        observer = FXUtils.observeWeak(() -> {
            FetchTask.setDownloadExecutorConcurrency(
                    config().getAutoDownloadThreads() ? FetchTask.DEFAULT_CONCURRENCY : config().getDownloadThreads());
        }, config().autoDownloadThreadsProperty(), config().downloadThreadsProperty());
    }

    private DownloadProviders() {
    }

    static void init() {
        FXUtils.onChangeAndOperate(config().versionListSourceProperty(), versionListSource -> {
            if (!providersById.containsKey(versionListSource)) {
                config().setVersionListSource(DEFAULT_PROVIDER_ID);
                return;
            }

            currentDownloadProvider = Optional.ofNullable(providersById.get(versionListSource))
                    .orElse(providersById.get(DEFAULT_PROVIDER_ID));
        });

        FXUtils.onChangeAndOperate(config().downloadTypeProperty(), downloadType -> {
            if (!rawProviders.containsKey(downloadType)) {
                config().setDownloadType(DEFAULT_RAW_PROVIDER_ID);
                return;
            }

            DownloadProvider primary = Optional.ofNullable(rawProviders.get(downloadType))
                    .orElse(rawProviders.get(DEFAULT_RAW_PROVIDER_ID));
            fileDownloadProvider.setDownloadProviderCandidates(
                    Stream.concat(
                            Stream.of(primary),
                            rawProviders.values().stream().filter(x -> x != primary)
                    ).collect(Collectors.toList())
            );
        });
    }

    public static String getPrimaryDownloadProviderId() {
        String downloadType = config().getDownloadType();
        if (providersById.containsKey(downloadType))
            return downloadType;
        else
            return DEFAULT_PROVIDER_ID;
    }

    public static DownloadProvider getDownloadProviderByPrimaryId(String primaryId) {
        return Optional.ofNullable(providersById.get(primaryId))
                .orElse(providersById.get(DEFAULT_PROVIDER_ID));
    }

    /**
     * Get current primary preferred download provider
     */
    public static DownloadProvider getDownloadProvider() {
        return config().isAutoChooseDownloadType() ? currentDownloadProvider : fileDownloadProvider;
    }

    public static String localizeErrorMessage(Throwable exception) {
        if (exception instanceof DownloadException) {
            URL url = ((DownloadException) exception).getUrl();
            if (exception.getCause() instanceof SocketTimeoutException) {
                return i18n("install.failed.downloading.timeout", url);
            } else if (exception.getCause() instanceof ResponseCodeException) {
                ResponseCodeException responseCodeException = (ResponseCodeException) exception.getCause();
                if (I18n.hasKey("download.code." + responseCodeException.getResponseCode())) {
                    return i18n("download.code." + responseCodeException.getResponseCode(), url);
                } else {
                    return i18n("install.failed.downloading.detail", url) + "\n" + StringUtils.getStackTrace(exception.getCause());
                }
            } else if (exception.getCause() instanceof FileNotFoundException) {
                return i18n("download.code.404", url);
            } else if (exception.getCause() instanceof AccessDeniedException) {
                return i18n("install.failed.downloading.detail", url) + "\n" + i18n("exception.access_denied", ((AccessDeniedException) exception.getCause()).getFile());
            } else if (exception.getCause() instanceof ArtifactMalformedException) {
                return i18n("install.failed.downloading.detail", url) + "\n" + i18n("exception.artifact_malformed");
            } else if (exception.getCause() instanceof SSLHandshakeException) {
                return i18n("install.failed.downloading.detail", url) + "\n" + i18n("exception.ssl_handshake");
            } else {
                return i18n("install.failed.downloading.detail", url) + "\n" + StringUtils.getStackTrace(exception.getCause());
            }
        } else if (exception instanceof ArtifactMalformedException) {
            return i18n("exception.artifact_malformed");
        } else if (exception instanceof CancellationException) {
            return i18n("message.cancelled");
        }
        return StringUtils.getStackTrace(exception);
    }
}
