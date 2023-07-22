package cn.ChengZhiYa.BaiShenLauncher.ui.download;

import cn.ChengZhiYa.BaiShenLauncher.game.HMCLGameRepository;
import cn.ChengZhiYa.BaiShenLauncher.mod.server.ServerModpackManifest;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.MessageDialogPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.RequiredValidator;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.Validator;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardController;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import javafx.application.Platform;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public final class RemoteModpackPage extends ModpackPage {
    public static final String MODPACK_SERVER_MANIFEST = "MODPACK_SERVER_MANIFEST";
    public static final String MODPACK_NAME = "MODPACK_NAME";
    public static final String MODPACK_MANIFEST = "MODPACK_MANIFEST";
    private final ServerModpackManifest manifest;

    public RemoteModpackPage(WizardController controller) {
        super(controller);

        manifest = Lang.tryCast(controller.getSettings().get(MODPACK_SERVER_MANIFEST), ServerModpackManifest.class)
                .orElseThrow(() -> new IllegalStateException("MODPACK_SERVER_MANIFEST should exist"));
        lblModpackLocation.setText(manifest.getFileApi());

        try {
            controller.getSettings().put(MODPACK_MANIFEST, manifest.toModpack(null));
        } catch (IOException e) {
            Controllers.dialog(I18n.i18n("modpack.type.server.malformed"), I18n.i18n("message.error"), MessageDialogPane.MessageType.ERROR);
            Platform.runLater(controller::onEnd);
            return;
        }

        lblName.setText(manifest.getName());
        lblVersion.setText(manifest.getVersion());
        lblAuthor.setText(manifest.getAuthor());

        Profile profile = (Profile) controller.getSettings().get("PROFILE");
        Optional<String> name = Lang.tryCast(controller.getSettings().get(MODPACK_NAME), String.class);
        if (name.isPresent()) {
            txtModpackName.setText(name.get());
            txtModpackName.setDisable(true);
        } else {
            // trim: https://github.com/huanghongxun/HMCL/issues/962
            txtModpackName.setText(manifest.getName().trim());
            txtModpackName.getValidators().addAll(
                    new RequiredValidator(),
                    new Validator(I18n.i18n("install.new_game.already_exists"), str -> !profile.getRepository().versionIdConflicts(str)),
                    new Validator(I18n.i18n("install.new_game.malformed"), HMCLGameRepository::isValidVersionId));
        }
    }

    @Override
    public void cleanup(Map<String, Object> settings) {
        settings.remove(MODPACK_SERVER_MANIFEST);
    }

    protected void onInstall() {
        if (!txtModpackName.validate()) return;
        controller.getSettings().put(MODPACK_NAME, txtModpackName.getText());
        controller.onFinish();
    }

    protected void onDescribe() {
        FXUtils.showWebDialog(I18n.i18n("modpack.description"), manifest.getDescription());
    }
}
