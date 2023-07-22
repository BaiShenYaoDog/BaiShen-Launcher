package cn.ChengZhiYa.BaiShenLauncher.ui.profile;

import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.RipplerContainer;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.TwoLineListItem;
import cn.ChengZhiYa.BaiShenLauncher.ui.versions.VersionPage;
import com.jfoenix.controls.JFXButton;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class ProfileListItemSkin extends SkinBase<ProfileListItem> {
    private final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    public ProfileListItemSkin(ProfileListItem skinnable) {
        super(skinnable);


        BorderPane root = new BorderPane();
        root.setPickOnBounds(false);
        RipplerContainer container = new RipplerContainer(root);

        FXUtils.onChangeAndOperate(skinnable.selectedProperty(), active -> {
            skinnable.pseudoClassStateChanged(SELECTED, active);
        });

        getSkinnable().addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            getSkinnable().setSelected(true);
        });

        Node left = VersionPage.wrap(SVG::folderOutline);
        root.setLeft(left);
        BorderPane.setAlignment(left, Pos.CENTER_LEFT);

        TwoLineListItem item = new TwoLineListItem();
        item.setPickOnBounds(false);
        BorderPane.setAlignment(item, Pos.CENTER);
        root.setCenter(item);

        HBox right = new HBox();
        right.setAlignment(Pos.CENTER_RIGHT);

        JFXButton btnRemove = new JFXButton();
        btnRemove.setOnMouseClicked(e -> skinnable.remove());
        btnRemove.getStyleClass().add("toggle-icon4");
        BorderPane.setAlignment(btnRemove, Pos.CENTER);
        btnRemove.setGraphic(SVG.close(Theme.blackFillBinding(), 14, 14));
        right.getChildren().add(btnRemove);
        root.setRight(right);

        item.titleProperty().bind(skinnable.titleProperty());
        item.subtitleProperty().bind(skinnable.subtitleProperty());

        getChildren().setAll(container);
    }
}
