package cn.ChengZhiYa.BaiShenLauncher.util;

import cn.ChengZhiYa.BaiShenLauncher.Metadata;
import cn.ChengZhiYa.BaiShenLauncher.countly.CrashReport;
import cn.ChengZhiYa.BaiShenLauncher.ui.CrashWindow;
import cn.ChengZhiYa.BaiShenLauncher.upgrade.IntegrityChecker;
import cn.ChengZhiYa.BaiShenLauncher.upgrade.UpdateChecker;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.io.NetworkUtils;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
 
public class CrashReporter implements Thread.UncaughtExceptionHandler {

    private final boolean showCrashWindow;

    public CrashReporter(boolean showCrashWindow) {
        this.showCrashWindow = showCrashWindow;
    }

    private boolean checkThrowable(Throwable e) {
        String s = StringUtils.getStackTrace(e);
        for (Pair<String, String> entry : Hole.SOURCE)
            if (s.contains(entry.getKey())) {
                if (StringUtils.isNotBlank(entry.getValue())) {
                    String info = entry.getValue();
                    Logging.LOG.severe(info);
                    try {
                        Alert alert = new Alert(AlertType.INFORMATION, info);
                        alert.setTitle(I18n.i18n("message.info"));
                        alert.setHeaderText(I18n.i18n("message.info"));
                        alert.showAndWait();
                    } catch (Throwable t) {
                        Logging.LOG.log(Level.SEVERE, "Unable to show message", t);
                    }
                }
                return false;
            }
        return true;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Logging.LOG.log(Level.SEVERE, "Uncaught exception in thread " + t.getName(), e);

        try {
            CrashReport report = new CrashReport(t, e);
            if (!report.shouldBeReport())
                return;

            String text = report.getDisplayText();

            Logging.LOG.log(Level.SEVERE, text);
            Platform.runLater(() -> {
                if (checkThrowable(e)) {
                    if (showCrashWindow) {
                        new CrashWindow(text).show();
                    }
                    if (!UpdateChecker.isOutdated() && IntegrityChecker.isSelfVerified()) {
                        reportToServer(report);
                    }
                }
            });
        } catch (Throwable handlingException) {
            Logging.LOG.log(Level.SEVERE, "Unable to handle uncaught exception", handlingException);
        }
    }

    private void reportToServer(CrashReport crashReport) {
        Thread t = new Thread(() -> {
            HashMap<String, String> map = new HashMap<>();
            map.put("crash_report", crashReport.getDisplayText());
            map.put("version", Metadata.VERSION);
            map.put("log", Logging.getLogs());
            try {
                String response = NetworkUtils.doPost(NetworkUtils.toURL("https://hmcl.huangyuhui.net/hmcl/crash.php"), map);
                if (StringUtils.isNotBlank(response))
                    Logging.LOG.log(Level.SEVERE, "Crash server response: " + response);
            } catch (IOException ex) {
                Logging.LOG.log(Level.SEVERE, "Unable to post HMCL server.", ex);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // Lazy initialization resources
    private static final class Hole {
        @SuppressWarnings("unchecked")
        static final Pair<String, String>[] SOURCE = (Pair<String, String>[]) new Pair<?, ?>[]{
                Pair.pair("Location is not set", I18n.i18n("crash.NoClassDefFound")),
                Pair.pair("UnsatisfiedLinkError", I18n.i18n("crash.user_fault")),
                Pair.pair("java.time.zone.ZoneRulesException: Unable to load TZDB time-zone rules", I18n.i18n("crash.user_fault")),
                Pair.pair("java.lang.NoClassDefFoundError", I18n.i18n("crash.NoClassDefFound")),
                Pair.pair("cn.ChengZhiYa.BaiShenLauncherutil.ResourceNotFoundError", I18n.i18n("crash.NoClassDefFound")),
                Pair.pair("java.lang.VerifyError", I18n.i18n("crash.NoClassDefFound")),
                Pair.pair("java.lang.NoSuchMethodError", I18n.i18n("crash.NoClassDefFound")),
                Pair.pair("java.lang.NoSuchFieldError", I18n.i18n("crash.NoClassDefFound")),
                Pair.pair("javax.imageio.IIOException", I18n.i18n("crash.NoClassDefFound")),
                Pair.pair("netscape.javascript.JSException", I18n.i18n("crash.NoClassDefFound")),
                Pair.pair("java.lang.IncompatibleClassChangeError", I18n.i18n("crash.NoClassDefFound")),
                Pair.pair("java.lang.ClassFormatError", I18n.i18n("crash.NoClassDefFound")),
                Pair.pair("com.sun.javafx.css.StyleManager.findMatchingStyles", I18n.i18n("launcher.update_java")),
                Pair.pair("NoSuchAlgorithmException", "Has your operating system been installed completely or is a ghost system?")
        };
    }
}
