package cn.ChengZhiYa.BaiShenLauncher;

import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.JarUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.OperatingSystem;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Stores metadata about this application.
 */
public final class Metadata {
    public static final String NAME = "BaiShen-Launcher";
    public static final String FULL_NAME = "BaiShen-Launcher";
    public static final String VERSION = "1.0.1";
    public static final String TITLE = NAME + " " + VERSION;
    public static final String FULL_TITLE = FULL_NAME + " v" + VERSION;
    public static final String CONTACT_URL = "https://bing.com";
    public static final String CHANGELOG_URL = "https://bing.com";
    public static final String EULA_URL = "https://bing.com";
    public static final String BUILD_CHANNEL = JarUtils.getManifestAttribute("Build-Channel", "nightly");
    public static final String GITHUB_SHA = JarUtils.getManifestAttribute("GitHub-SHA", null);
    public static final Path MINECRAFT_DIRECTORY = OperatingSystem.getWorkingDirectory("minecraft");
    public static final Path HMCL_DIRECTORY;

    static {
        String hmclHome = System.getProperty("BSL.home");
        if (hmclHome == null) {
            if (OperatingSystem.CURRENT_OS == OperatingSystem.LINUX) {
                String xdgData = System.getenv("XDG_DATA_HOME");
                if (StringUtils.isNotBlank(xdgData)) {
                    HMCL_DIRECTORY = Paths.get(xdgData, "BSL").toAbsolutePath();
                } else {
                    HMCL_DIRECTORY = Paths.get(System.getProperty("user.home", "."), ".local", "share", "BSL").toAbsolutePath();
                }
            } else {
                HMCL_DIRECTORY = OperatingSystem.getWorkingDirectory("BSL");
            }
        } else {
            HMCL_DIRECTORY = Paths.get(hmclHome).toAbsolutePath().normalize();
        }
    }

    private Metadata() {
    }

    public static boolean isStable() {
        return "stable".equals(BUILD_CHANNEL);
    }

    public static boolean isDev() {
        return "dev".equals(BUILD_CHANNEL);
    }

    public static boolean isNightly() {
        return !isStable() && !isDev();
    }
}
