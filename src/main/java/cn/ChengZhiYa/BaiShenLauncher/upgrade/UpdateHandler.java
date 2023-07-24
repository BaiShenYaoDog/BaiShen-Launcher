package cn.ChengZhiYa.BaiShenLauncher.upgrade;

import cn.ChengZhiYa.BaiShenLauncher.Main;
import cn.ChengZhiYa.BaiShenLauncher.Metadata;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.task.TaskExecutor;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.UpgradeDialog;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.MessageDialogPane.MessageType;
import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.TaskCancellationAction;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.JarUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.JavaVersion;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.checkFxUserThread;
import static cn.ChengZhiYa.BaiShenLauncher.util.Lang.thread;
import static cn.ChengZhiYa.BaiShenLauncher.util.Logging.LOG;
import static cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n.i18n;

public final class UpdateHandler {
    private UpdateHandler() {
    }

    /**
     * @return whether to exit
     */
    public static boolean processArguments(String[] args) {
        return false;
    }

    public static void updateFrom(RemoteVersion version) {
        checkFxUserThread();

        Controllers.dialog(new UpgradeDialog(version, () -> {
            Path downloaded;
            try {
                downloaded = Files.createTempFile("hmcl-update-", ".jar");
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to create temp file", e);
                return;
            }

            Task<?> task = new HMCLDownloadTask(version, downloaded);

            TaskExecutor executor = task.executor(false);
            Controllers.taskDialog(executor, i18n("message.downloading"), TaskCancellationAction.NORMAL);
            executor.start();
            thread(() -> {
                boolean success = executor.test();

                if (success) {
                    try {
                        if (!IntegrityChecker.isSelfVerified()) {
                            throw new IOException("Current JAR is not verified");
                        }

                        requestUpdate(downloaded, getCurrentLocation());
                        System.exit(0);
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, "Failed to update to " + version, e);
                        Platform.runLater(() -> Controllers.dialog(StringUtils.getStackTrace(e), i18n("update.failed"), MessageType.ERROR));
                    }

                } else {
                    Exception e = executor.getException();
                    LOG.log(Level.WARNING, "Failed to update to " + version, e);
                    if (e instanceof CancellationException) {
                        Platform.runLater(() -> Controllers.showToast(i18n("message.cancelled")));
                    } else {
                        Platform.runLater(() -> Controllers.dialog(e.toString(), i18n("update.failed"), MessageType.ERROR));
                    }
                }
            });
        }));
    }

    private static void applyUpdate(Path target) throws IOException {
        LOG.info("Applying update to " + target);

        Path self = getCurrentLocation();
        IntegrityChecker.requireVerifiedJar(self);
        ExecutableHeaderHelper.copyWithHeader(self, target);

        Optional<Path> newFilename = tryRename(target, Metadata.VERSION);
        if (newFilename.isPresent()) {
            LOG.info("Move " + target + " to " + newFilename.get());
            try {
                Files.move(target, newFilename.get());
                target = newFilename.get();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to move target", e);
            }
        }

        startJava(target);
    }

    private static void requestUpdate(Path updateTo, Path self) throws IOException {
        IntegrityChecker.requireVerifiedJar(updateTo);
        startJava(updateTo, "--apply-to", self.toString());
    }

    private static void startJava(Path jar, String... appArgs) throws IOException {
        List<String> commandline = new ArrayList<>();
        commandline.add(JavaVersion.fromCurrentEnvironment().getBinary().toString());
        commandline.add("-jar");
        commandline.add(jar.toAbsolutePath().toString());
        commandline.addAll(Arrays.asList(appArgs));
        LOG.info("Starting process: " + commandline);
        new ProcessBuilder(commandline)
                .directory(Paths.get("").toAbsolutePath().toFile())
                .inheritIO()
                .start();
    }

    private static Optional<Path> tryRename(Path path, String newVersion) {
        String filename = path.getFileName().toString();
        Matcher matcher = Pattern.compile("^(?<prefix>[hH][mM][cC][lL][.-])(?<version>\\d+(?:\\.\\d+)*)(?<suffix>\\.[^.]+)$").matcher(filename);
        if (matcher.find()) {
            String newFilename = matcher.group("prefix") + newVersion + matcher.group("suffix");
            if (!newFilename.equals(filename)) {
                return Optional.of(path.resolveSibling(newFilename));
            }
        }
        return Optional.empty();
    }

    private static Path getCurrentLocation() throws IOException {
        return JarUtils.thisJar().orElseThrow(() -> new IOException("Failed to find current HMCL location"));
    }

    // ==== support for old versions ===
    private static void performMigration() throws IOException {
        LOG.info("Migrating from old versions");

        Path location = getParentApplicationLocation()
                .orElseThrow(() -> new IOException("Failed to get parent application location"));

        requestUpdate(getCurrentLocation(), location);
    }

    /**
     * This method must be called from the main thread.
     */
    private static boolean isNestedApplication() {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < stacktrace.length; i++) {
            StackTraceElement element = stacktrace[i];
            if (Main.class.getName().equals(element.getClassName()) && "main".equals(element.getMethodName())) {
                // we've reached the main method
                return i + 1 != stacktrace.length;
            }
        }
        return false;
    }

    private static Optional<Path> getParentApplicationLocation() {
        String command = System.getProperty("sun.java.command");
        if (command != null) {
            Path path = Paths.get(command);
            if (Files.isRegularFile(path)) {
                return Optional.of(path.toAbsolutePath());
            }
        }
        return Optional.empty();
    }

    private static boolean isFirstLaunchAfterUpgrade() {
        Optional<Path> currentPath = JarUtils.thisJar();
        if (currentPath.isPresent()) {
            Path updated = Metadata.BSL_DIRECTORY.resolve("HMCL-" + Metadata.VERSION + ".jar");
            if (currentPath.get().toAbsolutePath().equals(updated.toAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    private static void breakForceUpdateFeature() {
        Path hmclVersionJson = Metadata.BSL_DIRECTORY.resolve("hmclver.json");
        if (Files.isRegularFile(hmclVersionJson)) {
            try {
                Map<?, ?> content = new Gson().fromJson(FileUtils.readText(hmclVersionJson), Map.class);
                Object ver = content.get("ver");
                if (ver instanceof String && ((String) ver).startsWith("3.")) {
                    Files.delete(hmclVersionJson);
                    LOG.info("Successfully broke the force update feature");
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to break the force update feature", e);
            } catch (JsonParseException e) {
                hmclVersionJson.toFile().delete();
            }
        }
    }
    // ====
}
