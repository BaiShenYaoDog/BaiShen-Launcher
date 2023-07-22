package cn.ChengZhiYa.BaiShenLauncher;

import cn.ChengZhiYa.BaiShenLauncher.setting.ConfigHolder;
import cn.ChengZhiYa.BaiShenLauncher.setting.SambaException;
import cn.ChengZhiYa.BaiShenLauncher.task.AsyncTaskExecutor;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.upgrade.UpdateHandler;
import cn.ChengZhiYa.BaiShenLauncher.util.CrashReporter;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.CommandBuilder;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.OperatingSystem;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class Launcher extends Application {
    public static final CookieManager COOKIE_MANAGER = new CookieManager();
    public static final CrashReporter CRASH_REPORTER = new CrashReporter(true);

    private static ButtonType showAlert(AlertType alertType, String contentText, ButtonType... buttons) {
        return new Alert(alertType, contentText, buttons).showAndWait().orElse(null);
    }

    private static boolean isConfigInTempDir() {
        String configPath = ConfigHolder.configLocation().toString();

        String tmpdir = System.getProperty("java.io.tmpdir");
        if (StringUtils.isNotBlank(tmpdir) && configPath.startsWith(tmpdir))
            return true;

        String[] tempFolderNames = {"Temp", "Cache", "Caches"};
        for (String name : tempFolderNames) {
            if (configPath.contains(File.separator + name + File.separator))
                return true;
        }

        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
            return configPath.contains("\\Temporary Internet Files\\")
                    || configPath.contains("\\INetCache\\")
                    || configPath.contains("\\$Recycle.Bin\\")
                    || configPath.contains("\\recycler\\");
        } else if (OperatingSystem.CURRENT_OS == OperatingSystem.LINUX) {
            return configPath.startsWith("/tmp/")
                    || configPath.startsWith("/var/tmp/")
                    || configPath.startsWith("/var/cache/")
                    || configPath.startsWith("/dev/shm/")
                    || configPath.contains("/Trash/");
        } else if (OperatingSystem.CURRENT_OS == OperatingSystem.OSX) {
            return configPath.startsWith("/var/folders/")
                    || configPath.startsWith("/private/var/folders/")
                    || configPath.startsWith("/tmp/")
                    || configPath.startsWith("/private/tmp/")
                    || configPath.startsWith("/var/tmp/")
                    || configPath.startsWith("/private/var/tmp/")
                    || configPath.contains("/.Trash/");
        } else {
            return false;
        }
    }

    private static void checkConfigInTempDir() {
        if (ConfigHolder.isNewlyCreated() && isConfigInTempDir()
                && showAlert(AlertType.WARNING, "豹豹现在在临时文件夹里\n这可能会让豹豹的记忆被重置,请移动到其他文件夹后重试!", ButtonType.YES, ButtonType.NO) == ButtonType.NO) {
            System.exit(0);
        }
    }

    private static void checkConfigOwner() {
        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS)
            return;

        String userName = System.getProperty("user.name");
        String owner;
        try {
            owner = Files.getOwner(ConfigHolder.configLocation()).getName();
        } catch (IOException ioe) {
            Logging.LOG.log(Level.WARNING, "Failed to get file owner", ioe);
            return;
        }

        if (Files.isWritable(ConfigHolder.configLocation()) || userName.equals("root") || userName.equals(owner))
            return;

        ArrayList<String> files = new ArrayList<>();
        files.add(ConfigHolder.configLocation().toString());
        if (Files.exists(Metadata.HMCL_DIRECTORY))
            files.add(Metadata.HMCL_DIRECTORY.toString());

        Path mcDir = Paths.get(".minecraft").toAbsolutePath().normalize();
        if (Files.exists(mcDir))
            files.add(mcDir.toString());

        String command = new CommandBuilder().add("sudo", "chown", "-R", userName).addAll(files).toString();
        ButtonType copyAndExit = new ButtonType(I18n.i18n("button.copy_and_exit"));

        if (showAlert(AlertType.ERROR, "这个文件夹是由" + owner + "创建的,阿豹没有权限访问! 你可以在命令行输入" + command + "将文件夹权限转让到当前账户",
                copyAndExit, ButtonType.CLOSE) == copyAndExit) {
            Clipboard.getSystemClipboard()
                    .setContent(Collections.singletonMap(DataFormat.PLAIN_TEXT, command));
        }
        System.exit(1);
    }

    public static void main(String[] args) {
        if (UpdateHandler.processArguments(args)) {
            return;
        }

        Thread.setDefaultUncaughtExceptionHandler(CRASH_REPORTER);
        AsyncTaskExecutor.setUncaughtExceptionHandler(new CrashReporter(false));

        try {
            launch(Launcher.class, args);
        } catch (Throwable e) {
            CRASH_REPORTER.uncaughtException(Thread.currentThread(), e);
        }
    }

    public static void stopApplication() {
        FXUtils.runInFX(() -> {
            if (Controllers.getStage() == null)
                return;
            Controllers.getStage().close();
            Schedulers.shutdown();
            Controllers.shutdown();
            Platform.exit();
        });
    }

    public static void stopWithoutPlatform() {
        Logging.LOG.info("Stopping application without JavaFX Toolkit.\n" + StringUtils.getStackTrace(Thread.currentThread().getStackTrace()));

        FXUtils.runInFX(() -> {
            if (Controllers.getStage() == null)
                return;
            Controllers.getStage().close();
            Schedulers.shutdown();
            Controllers.shutdown();
            Lang.executeDelayed(OperatingSystem::forceGC, TimeUnit.SECONDS, 5, true);
        });
    }

    @Override
    public void start(Stage primaryStage) {
        Thread.currentThread().setUncaughtExceptionHandler(CRASH_REPORTER);

        CookieHandler.setDefault(COOKIE_MANAGER);

        Logging.LOG.info("JavaFX Version: " + System.getProperty("javafx.runtime.version"));
        try {
            Object pipeline = Class.forName("com.sun.prism.GraphicsPipeline").getMethod("getPipeline").invoke(null);
            Logging.LOG.info("Prism pipeline: " + (pipeline == null ? "null" : pipeline.getClass().getName()));
        } catch (Throwable e) {
            Logging.LOG.log(Level.WARNING, "Failed to get prism pipeline", e);
        }

        try {
            try {
                ConfigHolder.init();
            } catch (SambaException ignored) {
                Main.showWarningAndContinue("不要让豹豹在Samba共享的文件夹里面\n豹豹会生气消失的!");
            } catch (IOException e) {
                Logging.LOG.log(Level.SEVERE, "没有文件夹权限", e);
                checkConfigInTempDir();
                checkConfigOwner();
                Main.showErrorAndExit("呜呜呜,阿豹没有权限打开这个文件夹: " + ConfigHolder.configLocation().getParent());
            }

            // https://lapcatsoftware.com/articles/app-translocation.html
            if (OperatingSystem.CURRENT_OS == OperatingSystem.OSX
                    && ConfigHolder.isNewlyCreated()
                    && System.getProperty("user.dir").startsWith("/private/var/folders/")) {
                if (showAlert(AlertType.WARNING, "由于MacOS的安全保护机制,会把豹豹关到临时文件夹里\n这可能会让豹豹的记忆被重置,请移动到其他文件夹后重试!", ButtonType.YES, ButtonType.NO) == ButtonType.NO)
                    return;
            } else {
                checkConfigInTempDir();
            }

            if (ConfigHolder.isOwnerChanged()) {
                if (showAlert(AlertType.WARNING, "您正在使用root账户启动白神启动器\n这会让其他账户无法使用白神启动器,是否继续", ButtonType.YES, ButtonType.NO) == ButtonType.NO)
                    return;
            }

            if (Metadata.HMCL_DIRECTORY.toString().indexOf('=') >= 0) {
                Main.showWarningAndContinue("糯米糍,路径里面不能有=,这会让豹豹被限制的!\n(将不能使用皮肤站账户以及离线登录更换皮肤功能)");
            }

            // runLater to ensure ConfigHolder.init() finished initialization
            Platform.runLater(() -> {
                // When launcher visibility is set to "hide and reopen" without Platform.implicitExit = false,
                // Stage.show() cannot work again because JavaFX Toolkit have already shut down.
                Platform.setImplicitExit(false);
                Controllers.initialize(primaryStage);

                primaryStage.show();
            });
        } catch (Throwable e) {
            CRASH_REPORTER.uncaughtException(Thread.currentThread(), e);
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        Controllers.onApplicationStop();
    }
}
