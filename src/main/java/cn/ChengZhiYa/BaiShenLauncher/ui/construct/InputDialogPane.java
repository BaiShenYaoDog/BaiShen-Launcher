package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.util.FutureCallback;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXTextField;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.concurrent.CompletableFuture;

public class InputDialogPane extends JFXDialogLayout {
    private final CompletableFuture<String> future = new CompletableFuture<>();

    private final JFXTextField textField;
    private final Label lblCreationWarning;
    private final SpinnerPane acceptPane;

    public InputDialogPane(String text, String initialValue, FutureCallback<String> onResult) {
        textField = new JFXTextField(initialValue);

        this.setHeading(new HBox(new Label(text)));
        this.setBody(new VBox(textField));

        lblCreationWarning = new Label();

        acceptPane = new SpinnerPane();
        acceptPane.getStyleClass().add("small-spinner-pane");
        JFXButton acceptButton = new JFXButton(I18n.i18n("button.ok"));
        acceptButton.getStyleClass().add("dialog-accept");
        acceptPane.setContent(acceptButton);

        JFXButton cancelButton = new JFXButton(I18n.i18n("button.cancel"));
        cancelButton.getStyleClass().add("dialog-cancel");

        this.setActions(lblCreationWarning, acceptPane, cancelButton);

        cancelButton.setOnAction(e -> fireEvent(new DialogCloseEvent()));
        acceptButton.setOnAction(e -> {
            acceptPane.showSpinner();

            onResult.call(textField.getText(), () -> {
                acceptPane.hideSpinner();
                future.complete(textField.getText());
                fireEvent(new DialogCloseEvent());
            }, msg -> {
                acceptPane.hideSpinner();
                lblCreationWarning.setText(msg);
            });
        });
    }

    public CompletableFuture<String> getCompletableFuture() {
        return future;
    }
}
