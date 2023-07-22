package cn.ChengZhiYa.BaiShenLauncher.ui.account;

import cn.ChengZhiYa.BaiShenLauncher.auth.authlibinjector.AuthlibInjectorServer;
import cn.ChengZhiYa.BaiShenLauncher.setting.ConfigHolder;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.ContainerAnimations;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.TransitionPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.DialogAware;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.DialogCloseEvent;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.SpinnerPane;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.io.NetworkUtils;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXTextField;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.logging.Level;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.onEscPressed;

public final class AddAuthlibInjectorServerPane extends TransitionPane implements DialogAware {

    private final Label lblServerUrl;
    private final Label lblServerName;
    private final Label lblCreationWarning;
    private final Label lblServerWarning;
    private final JFXTextField txtServerUrl;
    private final JFXDialogLayout addServerPane;
    private final JFXDialogLayout confirmServerPane;
    private final SpinnerPane nextPane;
    private final JFXButton btnAddNext;

    private AuthlibInjectorServer serverBeingAdded;

    public AddAuthlibInjectorServerPane(String url) {
        this();
        txtServerUrl.setText(url);
        onAddNext();
    }

    public AddAuthlibInjectorServerPane() {
        addServerPane = new JFXDialogLayout();
        addServerPane.setHeading(new Label(I18n.i18n("account.injector.add")));
        {
            txtServerUrl = new JFXTextField();
            txtServerUrl.setPromptText(I18n.i18n("account.injector.server_url"));
            txtServerUrl.setOnAction(e -> onAddNext());

            lblCreationWarning = new Label();
            lblCreationWarning.setWrapText(true);
            HBox actions = new HBox();
            {
                JFXButton cancel = new JFXButton(I18n.i18n("button.cancel"));
                cancel.getStyleClass().add("dialog-accept");
                cancel.setOnAction(e -> onAddCancel());

                nextPane = new SpinnerPane();
                nextPane.getStyleClass().add("small-spinner-pane");
                btnAddNext = new JFXButton(I18n.i18n("wizard.next"));
                btnAddNext.getStyleClass().add("dialog-accept");
                btnAddNext.setOnAction(e -> onAddNext());
                nextPane.setContent(btnAddNext);

                actions.getChildren().setAll(cancel, nextPane);
            }

            addServerPane.setBody(txtServerUrl);
            addServerPane.setActions(lblCreationWarning, actions);
        }

        confirmServerPane = new JFXDialogLayout();
        confirmServerPane.setHeading(new Label(I18n.i18n("account.injector.add")));
        {
            GridPane body = new GridPane();
            body.setStyle("-fx-padding: 15 0 0 0;");
            body.setVgap(15);
            body.setHgap(15);
            {
                body.getColumnConstraints().setAll(
                        Lang.apply(new ColumnConstraints(), c -> c.setMaxWidth(100)),
                        new ColumnConstraints()
                );

                lblServerUrl = new Label();
                GridPane.setColumnIndex(lblServerUrl, 1);
                GridPane.setRowIndex(lblServerUrl, 0);

                lblServerName = new Label();
                GridPane.setColumnIndex(lblServerName, 1);
                GridPane.setRowIndex(lblServerName, 1);

                lblServerWarning = new Label(I18n.i18n("account.injector.http"));
                lblServerWarning.setStyle("-fx-text-fill: red;");
                GridPane.setColumnIndex(lblServerWarning, 0);
                GridPane.setRowIndex(lblServerWarning, 2);
                GridPane.setColumnSpan(lblServerWarning, 2);

                body.getChildren().setAll(
                        Lang.apply(new Label(I18n.i18n("account.injector.server_url")), l -> {
                            GridPane.setColumnIndex(l, 0);
                            GridPane.setRowIndex(l, 0);
                        }),
                        Lang.apply(new Label(I18n.i18n("account.injector.server_name")), l -> {
                            GridPane.setColumnIndex(l, 0);
                            GridPane.setRowIndex(l, 1);
                        }),
                        lblServerUrl, lblServerName, lblServerWarning
                );
            }

            JFXButton prevButton = new JFXButton(I18n.i18n("wizard.prev"));
            prevButton.getStyleClass().add("dialog-cancel");
            prevButton.setOnAction(e -> onAddPrev());

            JFXButton cancelButton = new JFXButton(I18n.i18n("button.cancel"));
            cancelButton.getStyleClass().add("dialog-cancel");
            cancelButton.setOnAction(e -> onAddCancel());

            JFXButton finishButton = new JFXButton(I18n.i18n("wizard.finish"));
            finishButton.getStyleClass().add("dialog-accept");
            finishButton.setOnAction(e -> onAddFinish());

            confirmServerPane.setBody(body);
            confirmServerPane.setActions(prevButton, cancelButton, finishButton);
        }

        this.setContent(addServerPane, ContainerAnimations.NONE.getAnimationProducer());

        lblCreationWarning.maxWidthProperty().bind(((FlowPane) lblCreationWarning.getParent()).widthProperty());
        btnAddNext.disableProperty().bind(txtServerUrl.textProperty().isEmpty());
        nextPane.hideSpinner();

        onEscPressed(this, this::onAddCancel);
    }

    @Override
    public void onDialogShown() {
        txtServerUrl.requestFocus();
    }

    private String resolveFetchExceptionMessage(Throwable exception) {
        if (exception instanceof SSLException) {
            return I18n.i18n("account.failed.ssl");
        } else if (exception instanceof IOException) {
            return I18n.i18n("account.failed.connect_injector_server");
        } else {
            return exception.getClass().getName() + ": " + exception.getLocalizedMessage();
        }
    }

    private void onAddCancel() {
        fireEvent(new DialogCloseEvent());
    }

    private void onAddNext() {
        if (btnAddNext.isDisabled())
            return;

        lblCreationWarning.setText("");

        String url = txtServerUrl.getText();

        nextPane.showSpinner();
        addServerPane.setDisable(true);

        Task.runAsync(() -> {
            serverBeingAdded = AuthlibInjectorServer.locateServer(url);
        }).whenComplete(Schedulers.javafx(), exception -> {
            addServerPane.setDisable(false);
            nextPane.hideSpinner();

            if (exception == null) {
                lblServerName.setText(serverBeingAdded.getName());
                lblServerUrl.setText(serverBeingAdded.getUrl());

                lblServerWarning.setVisible("http".equals(NetworkUtils.toURL(serverBeingAdded.getUrl()).getProtocol()));

                this.setContent(confirmServerPane, ContainerAnimations.SWIPE_LEFT.getAnimationProducer());
            } else {
                Logging.LOG.log(Level.WARNING, "Failed to resolve auth server: " + url, exception);
                lblCreationWarning.setText(resolveFetchExceptionMessage(exception));
            }
        }).start();

    }

    private void onAddPrev() {
        this.setContent(addServerPane, ContainerAnimations.SWIPE_RIGHT.getAnimationProducer());
    }

    private void onAddFinish() {
        if (!ConfigHolder.config().getAuthlibInjectorServers().contains(serverBeingAdded)) {
            ConfigHolder.config().getAuthlibInjectorServers().add(serverBeingAdded);
        }
        fireEvent(new DialogCloseEvent());
    }

}
