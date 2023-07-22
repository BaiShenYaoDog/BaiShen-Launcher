package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import javafx.scene.Node;

public class IconedMenuItem extends IconedItem {

    public IconedMenuItem(Node node, String text, Runnable action) {
        super(node, text);

        getStyleClass().setAll("iconed-menu-item");
        setOnMouseClicked(e -> action.run());
    }

    public IconedMenuItem addTooltip(String tooltip) {
        FXUtils.installFastTooltip(this, tooltip);
        return this;
    }
}
