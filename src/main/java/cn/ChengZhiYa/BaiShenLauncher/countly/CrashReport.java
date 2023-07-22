package cn.ChengZhiYa.BaiShenLauncher.countly;

import cn.ChengZhiYa.BaiShenLauncher.Metadata;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.Pair;
import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.Architecture;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.OperatingSystem;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class CrashReport {

    private static final Long BYTES_IN_MB = 1024L * 1024;
    private final Thread thread;
    private final Throwable throwable;
    private final String stackTrace;
    private boolean nonFatal;

    public CrashReport(Thread thread, Throwable throwable) {
        this.thread = thread;
        this.throwable = throwable;
        stackTrace = StringUtils.getStackTrace(throwable);
        nonFatal = false;
    }

    private static long getMemoryAvailable() {
        Long total = Runtime.getRuntime().totalMemory();
        Long availMem = Runtime.getRuntime().freeMemory();
        return (total - availMem) / BYTES_IN_MB;
    }

    private static long getDiskAvailable() {
        long total = 0, free = 0;
        for (File f : File.listRoots()) {
            total += f.getTotalSpace();
            free += f.getUsableSpace();
        }
        return (total - free) / BYTES_IN_MB;
    }

    private static long getDiskTotal() {
        long total = 0;
        for (File f : File.listRoots()) {
            total += f.getTotalSpace();
        }
        return total / BYTES_IN_MB;
    }

    public CrashReport setNonFatal() {
        nonFatal = true;
        return this;
    }

    public boolean shouldBeReport() {
        if (!stackTrace.contains("cn.ChengZhiYa"))
            return false;

        return true;
    }

    public Map<String, Object> getMetrics(long runningTime) {
        return Lang.mapOf(
                Pair.pair("_run", runningTime),
                Pair.pair("_app_version", Metadata.VERSION),
                Pair.pair("_os", OperatingSystem.SYSTEM_NAME),
                Pair.pair("_os_version", OperatingSystem.SYSTEM_VERSION),
                Pair.pair("_disk_current", getDiskAvailable()),
                Pair.pair("_disk_total", getDiskTotal()),
                Pair.pair("_ram_current", getMemoryAvailable()),
                Pair.pair("_ram_total", Runtime.getRuntime().maxMemory() / BYTES_IN_MB),
                Pair.pair("_error", stackTrace),
                Pair.pair("_logs", Logging.getLogs()),
                Pair.pair("_name", throwable.getLocalizedMessage()),
                Pair.pair("_nonfatal", nonFatal)
        );
    }

    public String getDisplayText() {
        return "---- Hello Minecraft! Crash Report ----\n" +
                "  Version: " + Metadata.VERSION + "\n" +
                "  Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n" +
                "  Thread: " + thread + "\n" +
                "\n  Content: \n    " +
                stackTrace + "\n\n" +
                "-- System Details --\n" +
                "  Operating System: " + OperatingSystem.SYSTEM_NAME + ' ' + OperatingSystem.SYSTEM_VERSION + "\n" +
                "  System Architecture: " + Architecture.SYSTEM_ARCH_NAME + "\n" +
                "  Java Architecture: " + Architecture.CURRENT_ARCH_NAME + "\n" +
                "  Java Version: " + System.getProperty("java.version") + ", " + System.getProperty("java.vendor") + "\n" +
                "  Java VM Version: " + System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.info") + "), " + System.getProperty("java.vm.vendor") + "\n" +
                "  JVM Max Memory: " + Runtime.getRuntime().maxMemory() + "\n" +
                "  JVM Total Memory: " + Runtime.getRuntime().totalMemory() + "\n" +
                "  JVM Free Memory: " + Runtime.getRuntime().freeMemory() + "\n";
    }
}
