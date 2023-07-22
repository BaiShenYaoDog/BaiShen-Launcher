package cn.ChengZhiYa.BaiShenLauncher.util;

import java.nio.file.Path;
import java.util.Set;

/**
 * Utility for Adding JavaFX to module path.
 *
 * @author ZekerZhayard
 */
public final class JavaFXPatcher {
    private JavaFXPatcher() {
    }

    public static void patch(Set<String> modules, Path[] jarPaths, String[] addOpens) {
        Logging.LOG.info("No need to patch JavaFX with Java 8");
    }
}
