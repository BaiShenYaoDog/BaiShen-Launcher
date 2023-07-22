package cn.ChengZhiYa.BaiShenLauncher.ui.wizard;

import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.task.TaskExecutor;
import cn.ChengZhiYa.BaiShenLauncher.task.TaskListener;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.DialogCloseEvent;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.MessageDialogPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.TaskExecutorDialogPane;
import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.TaskCancellationAction;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import javafx.beans.property.StringProperty;

import java.util.Map;
import java.util.Queue;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.runInFX;

public abstract class TaskExecutorDialogWizardDisplayer extends AbstractWizardDisplayer {

    public TaskExecutorDialogWizardDisplayer(Queue<Object> cancelQueue) {
        super(cancelQueue);
    }

    @Override
    public void handleTask(Map<String, Object> settings, Task<?> task) {
        TaskExecutorDialogPane pane = new TaskExecutorDialogPane(new TaskCancellationAction(it -> {
            it.fireEvent(new DialogCloseEvent());
            onEnd();
        }));

        pane.setTitle(I18n.i18n("message.doing"));
        if (settings.containsKey("title")) {
            Object title = settings.get("title");
            if (title instanceof StringProperty)
                pane.titleProperty().bind((StringProperty) title);
            else if (title instanceof String)
                pane.setTitle((String) title);
        }

        runInFX(() -> {
            TaskExecutor executor = task.executor(new TaskListener() {
                @Override
                public void onStop(boolean success, TaskExecutor executor) {
                    runInFX(() -> {
                        if (success) {
                            if (settings.containsKey("success_message") && settings.get("success_message") instanceof String)
                                Controllers.dialog((String) settings.get("success_message"), null, MessageDialogPane.MessageType.SUCCESS, () -> onEnd());
                            else if (!settings.containsKey("forbid_success_message"))
                                Controllers.dialog(I18n.i18n("message.success"), null, MessageDialogPane.MessageType.SUCCESS, () -> onEnd());
                        } else {
                            if (executor.getException() == null)
                                return;
                            String appendix = StringUtils.getStackTrace(executor.getException());
                            if (settings.get("failure_callback") instanceof WizardProvider.FailureCallback)
                                ((WizardProvider.FailureCallback) settings.get("failure_callback")).onFail(settings, executor.getException(), () -> onEnd());
                            else if (settings.get("failure_message") instanceof String)
                                Controllers.dialog(appendix, (String) settings.get("failure_message"), MessageDialogPane.MessageType.ERROR, () -> onEnd());
                            else if (!settings.containsKey("forbid_failure_message"))
                                Controllers.dialog(appendix, I18n.i18n("wizard.failed"), MessageDialogPane.MessageType.ERROR, () -> onEnd());
                        }

                    });
                }
            });
            pane.setExecutor(executor);
            Controllers.dialog(pane);
            executor.start();
        });
    }
}
