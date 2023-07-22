package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXProgressBar;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.onEscPressed;

public class DialogPane extends JFXDialogLayout {
    protected final SpinnerPane acceptPane = new SpinnerPane();
    protected final JFXButton cancelButton = new JFXButton();
    protected final Label warningLabel = new Label();
    private final StringProperty title = new SimpleStringProperty();
    private final BooleanProperty valid = new SimpleBooleanProperty(true);
    private final JFXProgressBar progressBar = new JFXProgressBar();

    public DialogPane() {
        Label titleLabel = new Label();
        titleLabel.textProperty().bind(title);
        setHeading(titleLabel);
        getChildren().add(progressBar);

        progressBar.setVisible(false);
        StackPane.setMargin(progressBar, new Insets(-24.0D, -24.0D, -16.0D, -24.0D));
        StackPane.setAlignment(progressBar, Pos.TOP_CENTER);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        JFXButton acceptButton = new JFXButton(I18n.i18n("button.ok"));
        acceptButton.setOnAction(e -> onAccept());
        acceptButton.disableProperty().bind(valid.not());
        acceptButton.getStyleClass().add("dialog-accept");
        acceptPane.getStyleClass().add("small-spinner-pane");
        acceptPane.setContent(acceptButton);

        cancelButton.setText(I18n.i18n("button.cancel"));
        cancelButton.setOnAction(e -> onCancel());
        cancelButton.getStyleClass().add("dialog-cancel");
        onEscPressed(this, cancelButton::fire);

        setActions(warningLabel, acceptPane, cancelButton);
    }

    protected JFXProgressBar getProgressBar() {
        return progressBar;
    }

    public String getTitle() {
        return title.get();
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public StringProperty titleProperty() {
        return title;
    }

    public boolean isValid() {
        return valid.get();
    }

    public void setValid(boolean valid) {
        this.valid.set(valid);
    }

    public BooleanProperty validProperty() {
        return valid;
    }

    protected void onCancel() {
        fireEvent(new DialogCloseEvent());
    }

    protected void onAccept() {
        fireEvent(new DialogCloseEvent());
    }

    protected void setLoading() {
        acceptPane.showSpinner();
        warningLabel.setText("");
    }

    protected void onSuccess() {
        acceptPane.hideSpinner();
        fireEvent(new DialogCloseEvent());
    }

    protected void onFailure(String msg) {
        acceptPane.hideSpinner();
        warningLabel.setText(msg);
    }
}
