package cn.ChengZhiYa.BaiShenLauncher.game;

import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.Zipper;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.OperatingSystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class LogExporter {
    private LogExporter() {
    }

    public static CompletableFuture<Void> exportLogs(Path zipFile, DefaultGameRepository gameRepository, String versionId, String logs, String launchScript) {
        Path runDirectory = gameRepository.getRunDirectory(versionId).toPath();
        Path baseDirectory = gameRepository.getBaseDirectory().toPath();
        List<String> versions = new ArrayList<>();

        String currentVersionId = versionId;
        HashSet<String> resolvedSoFar = new HashSet<>();
        while (true) {
            if (resolvedSoFar.contains(currentVersionId)) break;
            resolvedSoFar.add(currentVersionId);
            Version currentVersion = gameRepository.getVersion(currentVersionId);
            versions.add(currentVersionId);

            if (StringUtils.isNotBlank(currentVersion.getInheritsFrom())) {
                currentVersionId = currentVersion.getInheritsFrom();
            } else {
                break;
            }
        }

        return CompletableFuture.runAsync(() -> {
            try (Zipper zipper = new Zipper(zipFile)) {
                Path logsDir = runDirectory.resolve("logs");
                if (Files.exists(logsDir.resolve("debug.log"))) {
                    zipper.putFile(logsDir.resolve("debug.log"), "debug.log");
                }
                if (Files.exists(logsDir.resolve("latest.log"))) {
                    zipper.putFile(logsDir.resolve("latest.log"), "latest.log");
                }
                if (Files.exists(logsDir.resolve("fml-client-latest.log"))) {
                    zipper.putFile(logsDir.resolve("fml-client-latest.log"), "fml-client-latest.log");
                }

                zipper.putTextFile(Logging.getLogs(), "hmcl.log");
                zipper.putTextFile(logs, "minecraft.log");
                zipper.putTextFile(Logging.filterForbiddenToken(launchScript), OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS ? "launch.bat" : "launch.sh");

                try (DirectoryStream<Path> stream = Files.newDirectoryStream(runDirectory, "hs_err_*.log")) {
                    long processStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();

                    for (Path file : stream) {
                        if (Files.isRegularFile(file)) {
                            FileTime time = Files.readAttributes(file, BasicFileAttributes.class).creationTime();
                            if (time.toMillis() >= processStartTime) {
                                String crashLog = Logging.filterForbiddenToken(FileUtils.readText(file));
                                zipper.putTextFile(crashLog, file.getFileName().toString());
                            }
                        }
                    }
                } catch (Throwable e) {
                    Logging.LOG.log(Level.WARNING, "Failed to find vm crash log", e);
                }

                for (String id : versions) {
                    Path versionJson = baseDirectory.resolve("versions").resolve(id).resolve(id + ".json");
                    if (Files.exists(versionJson)) {
                        zipper.putFile(versionJson, id + ".json");
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
