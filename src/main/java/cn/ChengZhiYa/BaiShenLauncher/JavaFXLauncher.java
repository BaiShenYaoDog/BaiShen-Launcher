package cn.ChengZhiYa.BaiShenLauncher;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class JavaFXLauncher {

    private static boolean started = false;

    static {
        // init JavaFX Toolkit
        try {
            // Java 9 or Latter
            final MethodHandle startup =
                    MethodHandles.publicLookup().findStatic(
                            javafx.application.Platform.class, "startup", MethodType.methodType(void.class, Runnable.class));
            startup.invokeExact((Runnable) () -> {
            });
            started = true;
        } catch (NoSuchMethodException e) {
            // Java 8
            try {
                Class.forName("javafx.embed.swing.JFXPanel").getDeclaredConstructor().newInstance();
                started = true;
            } catch (Throwable e0) {
                e0.printStackTrace();
            }
        } catch (IllegalStateException e) {
            started = true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private JavaFXLauncher() {
    }

    public static void start() {
    }

    public static boolean isStarted() {
        return started;
    }
}
