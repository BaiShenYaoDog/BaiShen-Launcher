package cn.ChengZhiYa.BaiShenLauncher.ui.account;

import cn.ChengZhiYa.BaiShenLauncher.auth.Account;
import cn.ChengZhiYa.BaiShenLauncher.auth.authlibinjector.AuthlibInjectorAccount;
import cn.ChengZhiYa.BaiShenLauncher.auth.authlibinjector.AuthlibInjectorServer;
import cn.ChengZhiYa.BaiShenLauncher.game.TexturesLoader;
import cn.ChengZhiYa.BaiShenLauncher.setting.Accounts;
import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.SpinnerPane;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.javafx.BindingMapping;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.effects.JFXDepthManager;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.runInFX;

public class AccountListItemSkin extends SkinBase<AccountListItem> {

    public AccountListItemSkin(AccountListItem skinnable) {
        super(skinnable);

        BorderPane root = new BorderPane();

        JFXRadioButton chkSelected = new JFXRadioButton() {
            @Override
            public void fire() {
                skinnable.fire();
            }
        };
        BorderPane.setAlignment(chkSelected, Pos.CENTER);
        chkSelected.selectedProperty().bind(skinnable.selectedProperty());
        root.setLeft(chkSelected);

        HBox center = new HBox();
        center.setSpacing(8);
        center.setAlignment(Pos.CENTER_LEFT);

        Canvas canvas = new Canvas(32, 32);
        TexturesLoader.bindAvatar(canvas, skinnable.getAccount());

        Label title = new Label();
        title.getStyleClass().add("title");
        title.textProperty().bind(skinnable.titleProperty());
        Label subtitle = new Label();
        subtitle.getStyleClass().add("subtitle");
        subtitle.textProperty().bind(skinnable.subtitleProperty());
        if (skinnable.getAccount() instanceof AuthlibInjectorAccount) {
            Tooltip tooltip = new Tooltip();
            AuthlibInjectorServer server = ((AuthlibInjectorAccount) skinnable.getAccount()).getServer();
            tooltip.textProperty().bind(BindingMapping.of(server, AuthlibInjectorServer::toString));
            FXUtils.installSlowTooltip(subtitle, tooltip);
        }
        VBox item = new VBox(title, subtitle);
        item.getStyleClass().add("two-line-list-item");
        BorderPane.setAlignment(item, Pos.CENTER);

        center.getChildren().setAll(canvas, item);
        root.setCenter(center);

        HBox right = new HBox();
        right.setAlignment(Pos.CENTER_RIGHT);

        JFXButton btnMove = new JFXButton();
        SpinnerPane spinnerMove = new SpinnerPane();
        spinnerMove.getStyleClass().add("small-spinner-pane");
        btnMove.setOnMouseClicked(e -> {
            Account account = skinnable.getAccount();
            Accounts.getAccounts().remove(account);
            if (account.isPortable()) {
                account.setPortable(false);
                if (!Accounts.getAccounts().contains(account))
                    Accounts.getAccounts().add(account);
            } else {
                account.setPortable(true);
                if (!Accounts.getAccounts().contains(account)) {
                    int idx = 0;
                    for (int i = Accounts.getAccounts().size() - 1; i >= 0; i--) {
                        if (Accounts.getAccounts().get(i).isPortable()) {
                            idx = i + 1;
                            break;
                        }
                    }
                    Accounts.getAccounts().add(idx, account);
                }
            }
        });
        btnMove.getStyleClass().add("toggle-icon4");
        if (skinnable.getAccount().isPortable()) {
            btnMove.setGraphic(SVG.earth(Theme.blackFillBinding(), -1, -1));
            runInFX(() -> FXUtils.installFastTooltip(btnMove, I18n.i18n("account.move_to_global")));
        } else {
            btnMove.setGraphic(SVG.export(Theme.blackFillBinding(), -1, -1));
            runInFX(() -> FXUtils.installFastTooltip(btnMove, I18n.i18n("account.move_to_portable")));
        }
        spinnerMove.setContent(btnMove);
        right.getChildren().add(spinnerMove);

        JFXButton btnRefresh = new JFXButton();
        SpinnerPane spinnerRefresh = new SpinnerPane();
        spinnerRefresh.getStyleClass().setAll("small-spinner-pane");
        btnRefresh.setOnMouseClicked(e -> {
            spinnerRefresh.showSpinner();
            skinnable.refreshAsync()
                    .whenComplete(Schedulers.javafx(), ex -> {
                        spinnerRefresh.hideSpinner();

                        if (ex != null) {
                            Controllers.showToast(Accounts.localizeErrorMessage(ex));
                        }
                    })
                    .start();
        });
        btnRefresh.getStyleClass().add("toggle-icon4");
        btnRefresh.setGraphic(SVG.refresh(Theme.blackFillBinding(), -1, -1));
        runInFX(() -> FXUtils.installFastTooltip(btnRefresh, I18n.i18n("button.refresh")));
        spinnerRefresh.setContent(btnRefresh);
        right.getChildren().add(spinnerRefresh);

        JFXButton btnUpload = new JFXButton();
        SpinnerPane spinnerUpload = new SpinnerPane();
        btnUpload.setOnMouseClicked(e -> {
            Task<?> uploadTask = skinnable.uploadSkin();
            if (uploadTask != null) {
                spinnerUpload.showSpinner();
                uploadTask
                        .whenComplete(Schedulers.javafx(), ex -> spinnerUpload.hideSpinner())
                        .start();
            }
        });
        btnUpload.getStyleClass().add("toggle-icon4");
        btnUpload.setGraphic(SVG.hanger(Theme.blackFillBinding(), -1, -1));
        runInFX(() -> FXUtils.installFastTooltip(btnUpload, I18n.i18n("account.skin.upload")));
        spinnerUpload.managedProperty().bind(spinnerUpload.visibleProperty());
        spinnerUpload.visibleProperty().bind(skinnable.canUploadSkin());
        spinnerUpload.setContent(btnUpload);
        spinnerUpload.getStyleClass().add("small-spinner-pane");
        right.getChildren().add(spinnerUpload);

        JFXButton btnRemove = new JFXButton();
        btnRemove.setOnMouseClicked(e -> skinnable.remove());
        btnRemove.getStyleClass().add("toggle-icon4");
        BorderPane.setAlignment(btnRemove, Pos.CENTER);
        btnRemove.setGraphic(SVG.delete(Theme.blackFillBinding(), -1, -1));
        runInFX(() -> FXUtils.installFastTooltip(btnRemove, I18n.i18n("button.delete")));
        right.getChildren().add(btnRemove);
        root.setRight(right);

        root.getStyleClass().add("card");
        root.setStyle("-fx-padding: 8 8 8 0;");
        JFXDepthManager.setDepth(root, 1);

        getChildren().setAll(root);
    }
}
