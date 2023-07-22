package cn.ChengZhiYa.BaiShenLauncher.ui.versions;

import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.IconedMenuItem;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.PopupMenu;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.RipplerContainer;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.TwoLineListItem;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPopup;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.SkinBase;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class WorldListItemSkin extends SkinBase<WorldListItem> {

    public WorldListItemSkin(WorldListItem skinnable) {
        super(skinnable);

        BorderPane root = new BorderPane();

        HBox center = new HBox();
        center.setMouseTransparent(true);
        center.setSpacing(8);
        center.setAlignment(Pos.CENTER_LEFT);

        StackPane imageViewContainer = new StackPane();
        FXUtils.setLimitWidth(imageViewContainer, 32);
        FXUtils.setLimitHeight(imageViewContainer, 32);

        ImageView imageView = new ImageView();
        FXUtils.limitSize(imageView, 32, 32);
        imageView.imageProperty().bind(skinnable.imageProperty());
        imageViewContainer.getChildren().setAll(imageView);

        TwoLineListItem item = new TwoLineListItem();
        item.titleProperty().bind(skinnable.titleProperty());
        item.subtitleProperty().bind(skinnable.subtitleProperty());
        BorderPane.setAlignment(item, Pos.CENTER);
        center.getChildren().setAll(imageView, item);
        root.setCenter(center);

        PopupMenu menu = new PopupMenu();
        JFXPopup popup = new JFXPopup(menu);

        menu.getContent().setAll(
                new IconedMenuItem(FXUtils.limitingSize(SVG.gearOutline(Theme.blackFillBinding(), 14, 14), 14, 14), I18n.i18n("world.datapack"), FXUtils.withJFXPopupClosing(skinnable::manageDatapacks, popup)),
                new IconedMenuItem(FXUtils.limitingSize(SVG.export(Theme.blackFillBinding(), 14, 14), 14, 14), I18n.i18n("world.export"), FXUtils.withJFXPopupClosing(skinnable::export, popup)),
                new IconedMenuItem(FXUtils.limitingSize(SVG.folderOutline(Theme.blackFillBinding(), 14, 14), 14, 14), I18n.i18n("world.reveal"), FXUtils.withJFXPopupClosing(skinnable::reveal, popup)));

        HBox right = new HBox();
        right.setAlignment(Pos.CENTER_RIGHT);

        JFXButton btnManage = new JFXButton();
        btnManage.setOnMouseClicked(e -> {
            popup.show(root, JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.RIGHT, 0, root.getHeight());
        });
        btnManage.getStyleClass().add("toggle-icon4");
        BorderPane.setAlignment(btnManage, Pos.CENTER);
        btnManage.setGraphic(SVG.dotsVertical(Theme.blackFillBinding(), -1, -1));
        right.getChildren().add(btnManage);
        root.setRight(right);

        root.getStyleClass().add("md-list-cell");
        root.setPadding(new Insets(8));

        getChildren().setAll(new RipplerContainer(root));
    }
}
