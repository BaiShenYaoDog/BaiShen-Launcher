package cn.ChengZhiYa.BaiShenLauncher.ui.wizard;

import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import javafx.scene.Node;

import java.util.Map;

public interface WizardDisplayer {
    default void onStart() {
    }

    default void onEnd() {
    }

    default void onCancel() {
    }

    void navigateTo(Node page, Navigation.NavigationDirection nav);

    void handleTask(Map<String, Object> settings, Task<?> task);
}
