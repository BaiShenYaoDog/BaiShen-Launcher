 package cn.ChengZhiYa.BaiShenLauncher.util;

import cn.ChengZhiYa.BaiShenLauncher.ui.construct.TaskExecutorDialogPane;

import java.util.function.Consumer;

public class TaskCancellationAction {
    public static TaskCancellationAction NO_CANCEL = new TaskCancellationAction((Consumer<TaskExecutorDialogPane>) null);
    public static TaskCancellationAction NORMAL = new TaskCancellationAction(() -> {
    });

    private final Consumer<TaskExecutorDialogPane> cancellationAction;

    public TaskCancellationAction(Runnable cancellationAction) {
        this.cancellationAction = it -> cancellationAction.run();
    }

    public TaskCancellationAction(Consumer<TaskExecutorDialogPane> cancellationAction) {
        this.cancellationAction = cancellationAction;
    }

    public Consumer<TaskExecutorDialogPane> getCancellationAction() {
        return cancellationAction;
    }
}
