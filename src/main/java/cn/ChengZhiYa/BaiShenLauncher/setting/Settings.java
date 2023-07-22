package cn.ChengZhiYa.BaiShenLauncher.setting;

import cn.ChengZhiYa.BaiShenLauncher.Metadata;
import cn.ChengZhiYa.BaiShenLauncher.game.HMCLCacheRepository;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.AnimationUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.CacheRepository;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import javafx.beans.binding.Bindings;

import java.util.Locale;

import static cn.ChengZhiYa.BaiShenLauncher.setting.ConfigHolder.config;

public final class Settings {

    private static Settings instance;

    private Settings() {
        config().localizationProperty().addListener(unused -> updateSystemLocale());
        updateSystemLocale();

        DownloadProviders.init();
        ProxyManager.init();
        Accounts.init();
        Profiles.init();
        AuthlibInjectorServers.init();
        AnimationUtils.init();

        CacheRepository.setInstance(HMCLCacheRepository.REPOSITORY);
        HMCLCacheRepository.REPOSITORY.directoryProperty().bind(Bindings.createStringBinding(() -> {
            if (FileUtils.canCreateDirectory(getCommonDirectory())) {
                return getCommonDirectory();
            } else {
                return getDefaultCommonDirectory();
            }
        }, config().commonDirectoryProperty(), config().commonDirTypeProperty()));
    }

    public static Settings instance() {
        if (instance == null) {
            throw new IllegalStateException("Settings hasn't been initialized");
        }
        return instance;
    }

    /**
     * Should be called from {@link ConfigHolder#init()}.
     */
    static void init() {
        instance = new Settings();
    }

    public static String getDefaultCommonDirectory() {
        return Metadata.MINECRAFT_DIRECTORY.toString();
    }

    private static void updateSystemLocale() {
        Locale.setDefault(config().getLocalization().getLocale());
    }

    public String getCommonDirectory() {
        switch (config().getCommonDirType()) {
            case DEFAULT:
                return getDefaultCommonDirectory();
            case CUSTOM:
                return config().getCommonDirectory();
            default:
                return null;
        }
    }
}
