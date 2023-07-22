package cn.ChengZhiYa.BaiShenLauncher.ui.download;

import cn.ChengZhiYa.BaiShenLauncher.download.DownloadProvider;
import cn.ChengZhiYa.BaiShenLauncher.download.RemoteVersion;
import cn.ChengZhiYa.BaiShenLauncher.game.HMCLGameRepository;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.InstallerItem;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.MessageDialogPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.RequiredValidator;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.Validator;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardController;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardPage;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

import java.util.Map;

import static javafx.beans.binding.Bindings.createBooleanBinding;

public class InstallersPage extends Control implements WizardPage {
    protected final WizardController controller;

    protected InstallerItem.InstallerItemGroup group = new InstallerItem.InstallerItemGroup();
    protected JFXTextField txtName = new JFXTextField();
    protected BooleanProperty installable = new SimpleBooleanProperty();

    public InstallersPage(WizardController controller, HMCLGameRepository repository, String gameVersion, DownloadProvider downloadProvider) {
        this.controller = controller;

        txtName.getValidators().addAll(
                new RequiredValidator(),
                new Validator(I18n.i18n("install.new_game.already_exists"), str -> !repository.versionIdConflicts(str)),
                new Validator(I18n.i18n("install.new_game.malformed"), HMCLGameRepository::isValidVersionId));
        installable.bind(createBooleanBinding(txtName::validate, txtName.textProperty()));
        txtName.setText(gameVersion);

        group.game.installable.setValue(false);

        for (InstallerItem item : group.getLibraries()) {
            item.setStyleMode(InstallerItem.Style.CARD);
        }

        for (InstallerItem library : group.getLibraries()) {
            String libraryId = library.getLibraryId();
            if (libraryId.equals("game")) continue;
            library.action.set(e -> {
                if ("fabric-api".equals(libraryId)) {
                    Controllers.dialog(I18n.i18n("install.installer.fabric-api.warning"), I18n.i18n("message.warning"), MessageDialogPane.MessageType.WARNING);
                }

                if (library.incompatibleLibraryName.get() == null)
                    controller.onNext(new VersionsPage(controller, I18n.i18n("install.installer.choose", I18n.i18n("install.installer." + libraryId)), gameVersion, downloadProvider, libraryId, () -> controller.onPrev(false)));
            });
            library.removeAction.set(e -> {
                controller.getSettings().remove(libraryId);
                reload();
            });
        }
    }

    @Override
    public String getTitle() {
        return I18n.i18n("install.new_game");
    }

    private String getVersion(String id) {
        return ((RemoteVersion) controller.getSettings().get(id)).getSelfVersion();
    }

    protected void reload() {
        for (InstallerItem library : group.getLibraries()) {
            String libraryId = library.getLibraryId();
            if (controller.getSettings().containsKey(libraryId)) {
                library.libraryVersion.set(getVersion(libraryId));
                library.removable.set(true);
            } else {
                library.libraryVersion.set(null);
                library.removable.set(false);
            }
        }
    }

    @Override
    public void onNavigate(Map<String, Object> settings) {
        reload();
    }

    @Override
    public void cleanup(Map<String, Object> settings) {
    }

    protected void onInstall() {
        controller.getSettings().put("name", txtName.getText());
        controller.onFinish();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new InstallersPageSkin(this);
    }

    protected static class InstallersPageSkin extends SkinBase<InstallersPage> {

        /**
         * Constructor for all SkinBase instances.
         *
         * @param control The control for which this Skin should attach to.
         */
        protected InstallersPageSkin(InstallersPage control) {
            super(control);

            BorderPane root = new BorderPane();
            root.setPadding(new Insets(16));

            {
                HBox versionNamePane = new HBox(8);
                versionNamePane.getStyleClass().add("card-non-transparent");
                versionNamePane.setStyle("-fx-padding: 20 8 20 16");
                versionNamePane.setAlignment(Pos.CENTER_LEFT);

                control.txtName.setMaxWidth(300);
                versionNamePane.getChildren().setAll(new Label(I18n.i18n("archive.name")), control.txtName);
                root.setTop(versionNamePane);
            }

            {
                FlowPane libraryPane = new FlowPane(control.group.getLibraries());
                BorderPane.setMargin(libraryPane, new Insets(16, 0, 16, 0));
                libraryPane.setVgap(16);
                libraryPane.setHgap(16);
                root.setCenter(libraryPane);
            }


            {
                JFXButton installButton = FXUtils.newRaisedButton(I18n.i18n("button.install"));
                installButton.disableProperty().bind(control.installable.not());
                installButton.setPrefWidth(100);
                installButton.setPrefHeight(40);
                installButton.setOnMouseClicked(e -> control.onInstall());
                BorderPane.setAlignment(installButton, Pos.CENTER_RIGHT);
                root.setBottom(installButton);
            }

            getChildren().setAll(root);
        }
    }
}
