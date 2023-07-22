package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class PopupMenu extends Control {

    private final ObservableList<Node> content = FXCollections.observableArrayList();
    private final BooleanProperty alwaysShowingVBar = new SimpleBooleanProperty();

    public PopupMenu() {
        getStyleClass().add("popup-menu");
    }

    public static Node wrapPopupMenuItem(Node node) {
        StackPane pane = new StackPane();
        pane.getChildren().setAll(node);
        pane.getStyleClass().add("menu-container");
        node.setMouseTransparent(true);
        return new RipplerContainer(pane);
    }

    public ObservableList<Node> getContent() {
        return content;
    }

    public boolean isAlwaysShowingVBar() {
        return alwaysShowingVBar.get();
    }

    public void setAlwaysShowingVBar(boolean alwaysShowingVBar) {
        this.alwaysShowingVBar.set(alwaysShowingVBar);
    }

    public BooleanProperty alwaysShowingVBarProperty() {
        return alwaysShowingVBar;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new PopupMenuSkin();
    }

    private class PopupMenuSkin extends SkinBase<PopupMenu> {

        protected PopupMenuSkin() {
            super(PopupMenu.this);

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToHeight(true);
            scrollPane.setFitToWidth(true);
            scrollPane.vbarPolicyProperty().bind(new When(alwaysShowingVBar)
                    .then(ScrollPane.ScrollBarPolicy.ALWAYS)
                    .otherwise(ScrollPane.ScrollBarPolicy.AS_NEEDED));

            VBox content = new VBox();
            content.getStyleClass().add("popup-menu-content");
            Bindings.bindContent(content.getChildren(), PopupMenu.this.getContent());
            scrollPane.setContent(content);

            FXUtils.smoothScrolling(scrollPane);

            getChildren().setAll(scrollPane);
        }
    }
}
