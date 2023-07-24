package cn.ChengZhiYa.BaiShenLauncher;

import cn.ChengZhiYa.BaiShenLauncher.ui.AwtUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SwingUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.FractureiserDetector;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.SelfDependencyPatcher;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.Architecture;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.JavaVersion;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.OperatingSystem;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.setProperty("java.net.useSystemProxies", "true");
        System.setProperty("javafx.autoproxy.disable", "true");
        System.getProperties().putIfAbsent("http.agent", "BSL/" + Metadata.VERSION);

        checkDirectoryPath();

        if (JavaVersion.CURRENT_JAVA.getParsedVersion() < 9)
            // This environment check will take ~300ms
            Lang.thread(Main::fixLetsEncrypt, "CA Certificate Check", true);

        if (OperatingSystem.CURRENT_OS == OperatingSystem.OSX)
            initIcon();

        Logging.start(Metadata.BSL_DIRECTORY.resolve("logs"));

        checkJavaFX();
        detectFractureiser();

        Launcher.main(args);
    }

    private static void initIcon() {
        java.awt.Image image = java.awt.Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/assets/img/icon@8x.png"));
        AwtUtils.setAppleIcon(image);
    }

    private static void checkDirectoryPath() {
        String currentDirectory = new File("").getAbsolutePath();
        if (currentDirectory.contains("!")) {
            // No Chinese translation because both Swing and JavaFX cannot render Chinese character properly when exclamation mark exists in the path.
            showErrorAndExit("Exclamation mark(!) is not allowed in the path where HMCL is in.\n"
                    + "The path is " + currentDirectory);
        }
    }

    private static void detectFractureiser() {
        if (FractureiserDetector.detect()) {
            showErrorAndExit("警告!\n阿豹发现你的电脑已被 Fractureiser 病毒感染\n为了您的电脑安全请立即打开杀毒软件进行查杀!");
        }
    }

    private static void checkJavaFX() {
        try {
            SelfDependencyPatcher.patch();
        } catch (SelfDependencyPatcher.PatchException e) {
            showErrorAndExit("你的电脑没有JavaFX运行环境\n请尝试使用有OpenFX的Java运行白神启动器");
            e.printStackTrace();
        } catch (SelfDependencyPatcher.IncompatibleVersionException e) {
            if (Architecture.CURRENT_ARCH == Architecture.MIPS64EL
                    || Architecture.CURRENT_ARCH == Architecture.LOONGARCH64
                    || Architecture.CURRENT_ARCH == Architecture.LOONGARCH64_OW)
                showErrorAndExit("你的电脑没有JavaFX运行环境\n请使用龙芯JDK 8(http://www.loongnix.cn/zh/api/java/downloads-jdk8/index.html)");
            else
                showErrorAndExit("你的电脑没有JavaFX运行环境\n请尝试使用Java 11运行白神启动器");
            e.printStackTrace();
        } catch (CancellationException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Indicates that a fatal error has occurred, and that the application cannot start.
     */
    static void showErrorAndExit(String message) {
        System.err.println(message);
        ;

        try {
            if (Platform.isFxApplicationThread()) {
                new Alert(Alert.AlertType.ERROR, message).showAndWait();
                System.exit(1);
            }
        } catch (Throwable ignored) {
        }

        SwingUtils.showErrorDialog(message);
        System.exit(1);
    }

    /**
     * Indicates that potential issues have been detected, and that the application may not function properly (but it can still run).
     */
    static void showWarningAndContinue(String message) {
        System.err.println(message);

        try {
            if (Platform.isFxApplicationThread()) {
                new Alert(Alert.AlertType.WARNING, message).showAndWait();
                return;
            }
        } catch (Throwable ignored) {
        }

        SwingUtils.showWarningDialog(message);
    }

    static void fixLetsEncrypt() {
        try {
            KeyStore defaultKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            Path ksPath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");

            try (InputStream ksStream = Files.newInputStream(ksPath)) {
                defaultKeyStore.load(ksStream, "changeit".toCharArray());
            }

            KeyStore letsEncryptKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream letsEncryptFile = Main.class.getResourceAsStream("/assets/lekeystore.jks")) {
                letsEncryptKeyStore.load(letsEncryptFile, "supersecretpassword".toCharArray());
            }

            KeyStore merged = KeyStore.getInstance(KeyStore.getDefaultType());
            merged.load(null, new char[0]);
            for (String alias : Collections.list(letsEncryptKeyStore.aliases()))
                merged.setCertificateEntry(alias, letsEncryptKeyStore.getCertificate(alias));
            for (String alias : Collections.list(defaultKeyStore.aliases()))
                merged.setCertificateEntry(alias, defaultKeyStore.getCertificate(alias));

            TrustManagerFactory instance = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            instance.init(merged);
            SSLContext tls = SSLContext.getInstance("TLS");
            tls.init(null, instance.getTrustManagers(), null);
            HttpsURLConnection.setDefaultSSLSocketFactory(tls.getSocketFactory());
            Logging.LOG.info("Added Lets Encrypt root certificates as additional trust");
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException |
                 KeyManagementException e) {
            Logging.LOG.log(Level.SEVERE, "Failed to load lets encrypt certificate. Expect problems", e);
        }
    }
}
