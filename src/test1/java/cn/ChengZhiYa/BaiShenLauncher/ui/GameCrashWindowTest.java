package cn.ChengZhiYa.BaiShenLauncher.ui;

import cn.ChengZhiYa.BaiShenLauncher.JavaFXLauncher;
import cn.ChengZhiYa.BaiShenLauncher.game.ClassicVersion;
import cn.ChengZhiYa.BaiShenLauncher.game.LaunchOptions;
import cn.ChengZhiYa.BaiShenLauncher.launch.ProcessListener;
import cn.ChengZhiYa.BaiShenLauncher.util.Log4jLevel;
import cn.ChengZhiYa.BaiShenLauncher.util.Pair;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.JavaVersion;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.ManagedProcess;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.Platform;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class GameCrashWindowTest {

    @Test
    @Disabled
    public void test() throws Exception {
        JavaFXLauncher.start();

        ManagedProcess process = new ManagedProcess(null, Arrays.asList("commands", "2"));

        String logs = FileUtils.readText(new File("../HMCLCore/src/test/resources/logs/too_old_java.txt"));

        CountDownLatch latch = new CountDownLatch(1);
        FXUtils.runInFX(() -> {
            GameCrashWindow window = new GameCrashWindow(process, ProcessListener.ExitType.APPLICATION_ERROR, null,
                    new ClassicVersion(),
                    new LaunchOptions.Builder()
                            .setJava(new JavaVersion(Paths.get("."), "16", Platform.SYSTEM_PLATFORM))
                            .setGameDir(new File("."))
                            .create(),
                    Arrays.stream(logs.split("\\n"))
                            .map(log -> Pair.pair(log, Log4jLevel.guessLevel(log)))
                            .collect(Collectors.toList()));

            window.showAndWait();

            latch.countDown();
        });
        latch.await();
    }
}
