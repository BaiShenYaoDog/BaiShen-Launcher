package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.controls.JFXButton;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.onEscPressed;
import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.runInFX;

public final class MessageDialogPane extends HBox {

    private final HBox actions;
    private @Nullable ButtonBase cancelButton;

    public MessageDialogPane(@NotNull String text, @Nullable String title, @NotNull MessageType type) {
        this.setSpacing(8);
        this.getStyleClass().add("jfx-dialog-layout");

        Label graphic = new Label();
        graphic.setTranslateX(10);
        graphic.setTranslateY(10);
        graphic.setMinSize(40, 40);
        graphic.setMaxSize(40, 40);
        switch (type) {
            case INFO:
                graphic.setGraphic(SVG.infoCircle(Theme.blackFillBinding(), 40, 40));
                break;
            case ERROR:
                graphic.setGraphic(SVG.closeCircle(Theme.blackFillBinding(), 40, 40));
                break;
            case SUCCESS:
                graphic.setGraphic(SVG.checkCircle(Theme.blackFillBinding(), 40, 40));
                break;
            case WARNING:
                graphic.setGraphic(SVG.alert(Theme.blackFillBinding(), 40, 40));
                break;
            case QUESTION:
                graphic.setGraphic(SVG.helpCircle(Theme.blackFillBinding(), 40, 40));
                break;
            default:
                throw new IllegalArgumentException("Unrecognized message box message type " + type);
        }

        VBox vbox = new VBox();
        HBox.setHgrow(vbox, Priority.ALWAYS);
        {
            StackPane titlePane = new StackPane();
            titlePane.getStyleClass().addAll("jfx-layout-heading", "title");
            titlePane.getChildren().setAll(new Label(title != null ? title : I18n.i18n("message.info")));

            StackPane content = new StackPane();
            content.getStyleClass().add("jfx-layout-body");
            EnhancedTextFlow textFlow = new EnhancedTextFlow(text);
            textFlow.setStyle("-fx-font-size: 14px;");
            if (textFlow.computePrefHeight(400.0) <= 350.0)
                content.getChildren().setAll(textFlow);
            else {
                ScrollPane scrollPane = new ScrollPane(textFlow);
                scrollPane.setPrefHeight(350);
                VBox.setVgrow(scrollPane, Priority.ALWAYS);
                scrollPane.setFitToWidth(true);
                content.getChildren().setAll(scrollPane);
            }

            actions = new HBox();
            actions.getStyleClass().add("jfx-layout-actions");

            vbox.getChildren().setAll(titlePane, content, actions);
        }

        this.getChildren().setAll(graphic, vbox);

        onEscPressed(this, () -> {
            if (cancelButton != null) {
                cancelButton.fire();
            }
        });
    }

    public void addButton(Node btn) {
        btn.addEventHandler(ActionEvent.ACTION, e -> fireEvent(new DialogCloseEvent()));
        actions.getChildren().add(btn);
    }

    public ButtonBase getCancelButton() {
        return cancelButton;
    }

    public void setCancelButton(@Nullable ButtonBase btn) {
        cancelButton = btn;
    }

    public enum MessageType {
        ERROR,
        INFO,
        WARNING,
        QUESTION,
        SUCCESS;

        public String getDisplayName() {
            return I18n.i18n("message." + name().toLowerCase(Locale.ROOT));
        }
    }

    private static final class EnhancedTextFlow extends TextFlow {
        EnhancedTextFlow(String text) {
            this.getChildren().setAll(FXUtils.parseSegment(text, Controllers::onHyperlinkAction));
        }

        @Override
        public double computePrefHeight(double width) {
            return super.computePrefHeight(width);
        }
    }

    public static class Builder {
        private final MessageDialogPane dialog;

        public Builder(String text, String title, MessageType type) {
            this.dialog = new MessageDialogPane(text, title, type);
        }

        public Builder addAction(Node actionNode) {
            dialog.addButton(actionNode);
            actionNode.getStyleClass().add("dialog-accept");
            return this;
        }

        public Builder ok(@Nullable Runnable ok) {
            JFXButton btnOk = new JFXButton(I18n.i18n("button.ok"));
            btnOk.getStyleClass().add("dialog-accept");
            if (ok != null) {
                btnOk.setOnAction(e -> ok.run());
            }
            dialog.addButton(btnOk);
            dialog.setCancelButton(btnOk);
            return this;
        }

        public Builder addCancel(@Nullable Runnable cancel) {
            return addCancel(I18n.i18n("button.cancel"), cancel);
        }

        public Builder addCancel(String cancelText, @Nullable Runnable cancel) {
            JFXButton btnCancel = new JFXButton(cancelText);
            btnCancel.getStyleClass().add("dialog-cancel");
            if (cancel != null) {
                btnCancel.setOnAction(e -> cancel.run());
            }
            dialog.addButton(btnCancel);
            dialog.setCancelButton(btnCancel);
            return this;
        }

        public Builder yesOrNo(@Nullable Runnable yes, @Nullable Runnable no) {
            JFXButton btnYes = new JFXButton(I18n.i18n("button.yes"));
            btnYes.getStyleClass().add("dialog-accept");
            if (yes != null) {
                btnYes.setOnAction(e -> yes.run());
            }
            dialog.addButton(btnYes);

            addCancel(I18n.i18n("button.no"), no);
            return this;
        }

        public Builder actionOrCancel(ButtonBase actionButton, Runnable cancel) {
            dialog.addButton(actionButton);

            addCancel(cancel);
            return this;
        }

        public Builder cancelOnTimeout(long timeoutMs) {
            if (dialog.getCancelButton() == null) {
                throw new IllegalStateException("Call ok/yesOrNo/actionOrCancel before calling cancelOnTimeout");
            }

            ButtonBase cancelButton = dialog.getCancelButton();
            String originalText = cancelButton.getText();

            Timer timer = Lang.getTimer();
            timer.scheduleAtFixedRate(new TimerTask() {
                long timeout = timeoutMs;

                @Override
                public void run() {
                    if (timeout <= 0) {
                        cancel();
                        runInFX(() -> {
                            cancelButton.fire();
                        });
                        return;
                    }
                    timeout -= 1000;
                    long currentTimeout = timeout;
                    runInFX(() -> cancelButton.setText(originalText + " (" + (currentTimeout / 1000) + ")"));
                }
            }, 1000, 1000);

            return this;
        }

        public MessageDialogPane build() {
            return dialog;
        }
    }
}
