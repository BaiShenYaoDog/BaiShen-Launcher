package cn.ChengZhiYa.BaiShenLauncher.ui.main;

import cn.ChengZhiYa.BaiShenLauncher.setting.ConfigHolder;
import cn.ChengZhiYa.BaiShenLauncher.setting.Settings;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.MessageDialogPane;
import cn.ChengZhiYa.BaiShenLauncher.upgrade.RemoteVersion;
import cn.ChengZhiYa.BaiShenLauncher.upgrade.UpdateChecker;
import cn.ChengZhiYa.BaiShenLauncher.upgrade.UpdateHandler;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.Locales;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.javafx.ExtendedProperties;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Level;

public final class SettingsPage extends SettingsView {

    private InvalidationListener updateListener;

    public SettingsPage() {
        FXUtils.smoothScrolling(scroll);

        // ==== Languages ====
        cboLanguage.getItems().setAll(Locales.LOCALES);
        ExtendedProperties.selectedItemPropertyFor(cboLanguage).bindBidirectional(ConfigHolder.config().localizationProperty());
        // ====

        fileCommonLocation.selectedDataProperty().bindBidirectional(ConfigHolder.config().commonDirTypeProperty());
        fileCommonLocationSublist.subtitleProperty().bind(
                Bindings.createObjectBinding(() -> Optional.ofNullable(Settings.instance().getCommonDirectory())
                                .orElse(I18n.i18n("launcher.cache_directory.disabled")),
                        ConfigHolder.config().commonDirectoryProperty(), ConfigHolder.config().commonDirTypeProperty()));
    }

    @Override
    protected void onUpdate() {
        RemoteVersion target = UpdateChecker.getLatestVersion();
        if (target == null) {
            return;
        }
        UpdateHandler.updateFrom(target);
    }

    @Override
    protected void onExportLogs() {
        // We cannot determine which file is JUL using.
        // So we write all the logs to a new file.
        Lang.thread(() -> {
            Path logFile = Paths.get("hmcl-exported-logs-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss")) + ".log").toAbsolutePath();

            Logging.LOG.info("Exporting logs to " + logFile);
            try {
                Files.write(logFile, Logging.getRawLogs());
            } catch (IOException e) {
                Platform.runLater(() -> Controllers.dialog(I18n.i18n("settings.launcher.launcher_log.export.failed") + "\n" + e, null, MessageDialogPane.MessageType.ERROR));
                Logging.LOG.log(Level.WARNING, "Failed to export logs", e);
                return;
            }

            Platform.runLater(() -> Controllers.dialog(I18n.i18n("settings.launcher.launcher_log.export.success", logFile)));
            FXUtils.showFileInExplorer(logFile);
        });
    }

    @Override
    protected void onSponsor() {
        FXUtils.openLink("https://hmcl.huangyuhui.net/api/redirect/sponsor");
    }

    @Override
    protected void clearCacheDirectory() {
        FileUtils.cleanDirectoryQuietly(new File(Settings.instance().getCommonDirectory(), "cache"));
    }
}
