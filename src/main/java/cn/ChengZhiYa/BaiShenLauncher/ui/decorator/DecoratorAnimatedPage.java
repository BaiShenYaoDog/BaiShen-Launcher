package cn.ChengZhiYa.BaiShenLauncher.ui.decorator;

import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class DecoratorAnimatedPage extends Control {

    protected final VBox left = new VBox();
    protected final StackPane center = new StackPane();

    {
        getStyleClass().add("gray-background");
    }

    public VBox getLeft() {
        return left;
    }

    protected void setLeft(Node... children) {
        left.getChildren().setAll(children);
    }

    public StackPane getCenter() {
        return center;
    }

    protected void setCenter(Node... children) {
        center.getChildren().setAll(children);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new DecoratorAnimatedPageSkin<>(this);
    }

    public static class DecoratorAnimatedPageSkin<T extends DecoratorAnimatedPage> extends SkinBase<T> {

        protected DecoratorAnimatedPageSkin(T control) {
            super(control);

            BorderPane pane = new BorderPane();
            pane.setLeft(control.left);
            FXUtils.setLimitWidth(control.left, 200);
            pane.setCenter(control.center);
            getChildren().setAll(pane);
        }

        protected void setLeft(Node... children) {
            getSkinnable().setLeft(children);
        }

        protected void setCenter(Node... children) {
            getSkinnable().setCenter(children);
        }

    }

}
