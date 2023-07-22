package cn.ChengZhiYa.BaiShenLauncher.ui.versions;

import cn.ChengZhiYa.BaiShenLauncher.event.Event;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.setting.VersionIconType;
import cn.ChengZhiYa.BaiShenLauncher.setting.VersionSetting;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.DialogPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.RipplerContainer;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class VersionIconDialog extends DialogPane {
    private final Profile profile;
    private final String versionId;
    private final Runnable onFinish;
    private final VersionSetting vs;

    public VersionIconDialog(Profile profile, String versionId, Runnable onFinish) {
        this.profile = profile;
        this.versionId = versionId;
        this.onFinish = onFinish;
        this.vs = profile.getRepository().getLocalVersionSettingOrCreate(versionId);

        setTitle(I18n.i18n("settings.icon"));
        FlowPane pane = new FlowPane();
        setBody(pane);

        pane.getChildren().setAll(
                createCustomIcon(),
                createIcon(VersionIconType.GRASS),
                createIcon(VersionIconType.CHEST),
                createIcon(VersionIconType.CHICKEN),
                createIcon(VersionIconType.COMMAND),
                createIcon(VersionIconType.CRAFT_TABLE),
                createIcon(VersionIconType.FABRIC),
                createIcon(VersionIconType.FORGE),
                createIcon(VersionIconType.FURNACE),
                createIcon(VersionIconType.QUILT)
        );
    }

    private void exploreIcon() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.i18n("extension.png"), "*.png"));
        File selectedFile = chooser.showOpenDialog(Controllers.getStage());
        if (selectedFile != null) {
            File iconFile = profile.getRepository().getVersionIconFile(versionId);
            try {
                FileUtils.copyFile(selectedFile, iconFile);

                if (vs != null) {
                    vs.setVersionIcon(VersionIconType.DEFAULT);
                }

                onAccept();
            } catch (IOException e) {
                Logging.LOG.log(Level.SEVERE, "Failed to copy icon file from " + selectedFile + " to " + iconFile, e);
            }
        }
    }

    private Node createCustomIcon() {
        Node shape = SVG.plusCircleOutline(Theme.blackFillBinding(), 32, 32);
        shape.setMouseTransparent(true);
        RipplerContainer container = new RipplerContainer(shape);
        FXUtils.setLimitWidth(container, 36);
        FXUtils.setLimitHeight(container, 36);
        container.setOnMouseClicked(e -> {
            exploreIcon();
        });
        return container;
    }

    private Node createIcon(VersionIconType type) {
        ImageView imageView = new ImageView(new Image(type.getResourceUrl()));
        imageView.setMouseTransparent(true);
        RipplerContainer container = new RipplerContainer(imageView);
        FXUtils.setLimitWidth(container, 36);
        FXUtils.setLimitHeight(container, 36);
        container.setOnMouseClicked(e -> {
            if (vs != null) {
                vs.setVersionIcon(type);
                onAccept();
            }
        });
        return container;
    }

    @Override
    protected void onAccept() {
        profile.getRepository().onVersionIconChanged.fireEvent(new Event(this));
        onFinish.run();
        super.onAccept();
    }
}
