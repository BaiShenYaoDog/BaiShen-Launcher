package cn.ChengZhiYa.BaiShenLauncher.ui.account;

import cn.ChengZhiYa.BaiShenLauncher.auth.AuthInfo;
import cn.ChengZhiYa.BaiShenLauncher.auth.ClassicAccount;
import cn.ChengZhiYa.BaiShenLauncher.auth.NoSelectedCharacterException;
import cn.ChengZhiYa.BaiShenLauncher.auth.authlibinjector.AuthlibInjectorAccount;
import cn.ChengZhiYa.BaiShenLauncher.auth.yggdrasil.YggdrasilAccount;
import cn.ChengZhiYa.BaiShenLauncher.auth.yggdrasil.YggdrasilService;
import cn.ChengZhiYa.BaiShenLauncher.setting.Accounts;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.DialogCloseEvent;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.JFXHyperlink;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.RequiredValidator;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXProgressBar;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.logging.Level;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.onEscPressed;

public class ClassicAccountLoginDialog extends StackPane {
    private final ClassicAccount oldAccount;
    private final Consumer<AuthInfo> success;
    private final Runnable failed;

    private final JFXPasswordField txtPassword;
    private final Label lblCreationWarning = new Label();
    private final JFXProgressBar progressBar;

    public ClassicAccountLoginDialog(ClassicAccount oldAccount, Consumer<AuthInfo> success, Runnable failed) {
        this.oldAccount = oldAccount;
        this.success = success;
        this.failed = failed;

        progressBar = new JFXProgressBar();
        StackPane.setAlignment(progressBar, Pos.TOP_CENTER);
        progressBar.setVisible(false);

        JFXDialogLayout dialogLayout = new JFXDialogLayout();

        {
            dialogLayout.setHeading(new Label(I18n.i18n("login.enter_password")));
        }

        {
            VBox body = new VBox(15);
            body.setPadding(new Insets(15, 0, 0, 0));

            Label usernameLabel = new Label(oldAccount.getUsername());

            txtPassword = new JFXPasswordField();
            txtPassword.setOnAction(e -> onAccept());
            txtPassword.getValidators().add(new RequiredValidator());
            txtPassword.setLabelFloat(true);
            txtPassword.setPromptText(I18n.i18n("account.password"));

            body.getChildren().setAll(usernameLabel, txtPassword);

            if (oldAccount instanceof YggdrasilAccount && !(oldAccount instanceof AuthlibInjectorAccount)) {
                HBox linkPane = new HBox(8);
                body.getChildren().add(linkPane);

                JFXHyperlink migrationLink = new JFXHyperlink(I18n.i18n("account.methods.yggdrasil.migration"));
                migrationLink.setExternalLink(YggdrasilService.PROFILE_URL);

                JFXHyperlink migrationHowLink = new JFXHyperlink(I18n.i18n("account.methods.yggdrasil.migration.how"));
                migrationHowLink.setExternalLink(YggdrasilService.MIGRATION_FAQ_URL);

                linkPane.getChildren().setAll(migrationLink, migrationHowLink);
            }

            dialogLayout.setBody(body);
        }

        {
            JFXButton acceptButton = new JFXButton(I18n.i18n("button.ok"));
            acceptButton.setOnAction(e -> onAccept());
            acceptButton.getStyleClass().add("dialog-accept");

            JFXButton cancelButton = new JFXButton(I18n.i18n("button.cancel"));
            cancelButton.setOnAction(e -> onCancel());
            cancelButton.getStyleClass().add("dialog-cancel");

            dialogLayout.setActions(lblCreationWarning, acceptButton, cancelButton);
        }

        getChildren().setAll(dialogLayout);

        onEscPressed(this, this::onCancel);
    }

    private void onAccept() {
        String password = txtPassword.getText();
        progressBar.setVisible(true);
        lblCreationWarning.setText("");
        Task.supplyAsync(() -> oldAccount.logInWithPassword(password))
                .whenComplete(Schedulers.javafx(), authInfo -> {
                    success.accept(authInfo);
                    fireEvent(new DialogCloseEvent());
                    progressBar.setVisible(false);
                }, e -> {
                    Logging.LOG.log(Level.INFO, "Failed to login with password: " + oldAccount, e);
                    if (e instanceof NoSelectedCharacterException) {
                        fireEvent(new DialogCloseEvent());
                    } else {
                        lblCreationWarning.setText(Accounts.localizeErrorMessage(e));
                    }
                    progressBar.setVisible(false);
                }).start();
    }

    private void onCancel() {
        failed.run();
        fireEvent(new DialogCloseEvent());
    }
}
