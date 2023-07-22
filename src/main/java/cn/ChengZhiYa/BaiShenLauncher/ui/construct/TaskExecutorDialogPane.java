package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.task.FileDownloadTask;
import cn.ChengZhiYa.BaiShenLauncher.task.TaskExecutor;
import cn.ChengZhiYa.BaiShenLauncher.task.TaskListener;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.TaskCancellationAction;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.onEscPressed;
import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.runInFX;

public class TaskExecutorDialogPane extends BorderPane {
    private final Consumer<FileDownloadTask.SpeedEvent> speedEventHandler;
    private final Label lblTitle;
    private final Label lblProgress;
    private final JFXButton btnCancel;
    private final TaskListPane taskListPane;
    private TaskExecutor executor;
    private TaskCancellationAction onCancel;

    public TaskExecutorDialogPane(@NotNull TaskCancellationAction cancel) {
        FXUtils.setLimitWidth(this, 500);
        FXUtils.setLimitHeight(this, 300);

        VBox center = new VBox();
        this.setCenter(center);
        center.setPadding(new Insets(16));
        {
            lblTitle = new Label();
            lblTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: BOLD;");

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToHeight(true);
            scrollPane.setFitToWidth(true);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
            {
                taskListPane = new TaskListPane();
                scrollPane.setContent(taskListPane);
            }

            center.getChildren().setAll(lblTitle, scrollPane);
        }

        BorderPane bottom = new BorderPane();
        this.setBottom(bottom);
        bottom.setPadding(new Insets(0, 8, 8, 8));
        {
            lblProgress = new Label();
            bottom.setLeft(lblProgress);

            btnCancel = new JFXButton(I18n.i18n("button.cancel"));
            bottom.setRight(btnCancel);
        }

        setCancel(cancel);

        btnCancel.setOnAction(e -> {
            Optional.ofNullable(executor).ifPresent(TaskExecutor::cancel);
            if (onCancel.getCancellationAction() != null) {
                onCancel.getCancellationAction().accept(this);
            }
        });

        speedEventHandler = speedEvent -> {
            String unit = "B/s";
            double speed = speedEvent.getSpeed();
            if (speed > 1024) {
                speed /= 1024;
                unit = "KB/s";
            }
            if (speed > 1024) {
                speed /= 1024;
                unit = "MB/s";
            }
            double finalSpeed = speed;
            String finalUnit = unit;
            Platform.runLater(() -> {
                lblProgress.setText(String.format("%.1f %s", finalSpeed, finalUnit));
            });
        };
        FileDownloadTask.speedEvent.channel(FileDownloadTask.SpeedEvent.class).registerWeak(speedEventHandler);

        onEscPressed(this, btnCancel::fire);
    }

    public void setExecutor(TaskExecutor executor) {
        setExecutor(executor, true);
    }

    public void setExecutor(TaskExecutor executor, boolean autoClose) {
        this.executor = executor;

        if (executor != null) {
            taskListPane.setExecutor(executor);

            if (autoClose)
                executor.addTaskListener(new TaskListener() {
                    @Override
                    public void onStop(boolean success, TaskExecutor executor) {
                        Platform.runLater(() -> fireEvent(new DialogCloseEvent()));
                    }
                });
        }
    }

    public StringProperty titleProperty() {
        return lblTitle.textProperty();
    }

    public String getTitle() {
        return lblTitle.getText();
    }

    public void setTitle(String currentState) {
        lblTitle.setText(currentState);
    }

    public void setCancel(TaskCancellationAction onCancel) {
        this.onCancel = onCancel;

        runInFX(() -> btnCancel.setDisable(onCancel == null));
    }
}
