package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class AdvancedListItemSkin extends SkinBase<AdvancedListItem> {
    private final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    public AdvancedListItemSkin(AdvancedListItem skinnable) {
        super(skinnable);

        FXUtils.onChangeAndOperate(skinnable.activeProperty(), active -> {
            skinnable.pseudoClassStateChanged(SELECTED, active);
        });

        BorderPane root = new BorderPane();
        root.getStyleClass().add("container");
        root.setPickOnBounds(false);

        RipplerContainer container = new RipplerContainer(root);

        HBox left = new HBox();
        left.setMouseTransparent(true);

        TwoLineListItem item = new TwoLineListItem();
        root.setCenter(item);
        item.setMouseTransparent(true);
        item.titleProperty().bind(skinnable.titleProperty());
        item.subtitleProperty().bind(skinnable.subtitleProperty());

        FXUtils.onChangeAndOperate(skinnable.leftGraphicProperty(),
                newGraphic -> {
                    if (newGraphic == null) {
                        left.getChildren().clear();
                    } else {
                        left.getChildren().setAll(newGraphic);
                    }
                });
        root.setLeft(left);

        HBox right = new HBox();
        right.setAlignment(Pos.CENTER);
        right.getStyleClass().add("toggle-icon4");
        FXUtils.setLimitWidth(right, 40);
        FXUtils.onChangeAndOperate(skinnable.rightGraphicProperty(),
                newGraphic -> {
                    if (newGraphic == null) {
                        right.getChildren().clear();
                    } else {
                        right.getChildren().setAll(newGraphic);
                    }
                });

        FXUtils.onChangeAndOperate(skinnable.actionButtonVisibleProperty(),
                visible -> root.setRight(visible ? right : null));

        getChildren().setAll(container);
    }
}
