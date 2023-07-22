package cn.ChengZhiYa.BaiShenLauncher.ui;

import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.ComponentList;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.SpinnerPane;
import com.jfoenix.controls.JFXButton;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public abstract class ToolbarListPageSkin<T extends ListPageBase<? extends Node>> extends SkinBase<T> {

    public ToolbarListPageSkin(T skinnable) {
        super(skinnable);

        SpinnerPane spinnerPane = new SpinnerPane();
        spinnerPane.loadingProperty().bind(skinnable.loadingProperty());
        spinnerPane.failedReasonProperty().bind(skinnable.failedReasonProperty());
        spinnerPane.onFailedActionProperty().bind(skinnable.onFailedActionProperty());
        spinnerPane.getStyleClass().add("large-spinner-pane");

        ComponentList root = new ComponentList();
        root.getStyleClass().add("no-padding");
        StackPane.setMargin(root, new Insets(10));

        List<Node> toolbarButtons = initializeToolbar(skinnable);
        if (!toolbarButtons.isEmpty()) {
            HBox toolbar = new HBox();
            toolbar.setAlignment(Pos.CENTER_LEFT);
            toolbar.setPickOnBounds(false);
            toolbar.getChildren().setAll(toolbarButtons);
            root.getContent().add(toolbar);
        }

        {
            ScrollPane scrollPane = new ScrollPane();
            ComponentList.setVgrow(scrollPane, Priority.ALWAYS);
            scrollPane.setFitToWidth(true);

            VBox content = new VBox();

            Bindings.bindContent(content.getChildren(), skinnable.itemsProperty());

            scrollPane.setContent(content);
            FXUtils.smoothScrolling(scrollPane);

            root.getContent().add(scrollPane);
        }

        spinnerPane.setContent(root);

        getChildren().setAll(spinnerPane);
    }

    public static Node wrap(Node node) {
        StackPane stackPane = new StackPane();
        stackPane.setPadding(new Insets(0, 5, 0, 2));
        stackPane.getChildren().setAll(node);
        return stackPane;
    }

    public static JFXButton createToolbarButton(String text, SVG.SVGIcon creator, Runnable onClick) {
        JFXButton ret = new JFXButton();
        ret.getStyleClass().add("jfx-tool-bar-button");
        ret.textFillProperty().bind(Theme.foregroundFillBinding());
        ret.setGraphic(wrap(creator.createIcon(Theme.foregroundFillBinding(), -1, -1)));
        ret.setText(text);
        ret.setOnMouseClicked(e -> onClick.run());
        return ret;
    }

    public static JFXButton createToolbarButton2(String text, SVG.SVGIcon creator, Runnable onClick) {
        JFXButton ret = new JFXButton();
        ret.getStyleClass().add("jfx-tool-bar-button");
        ret.setGraphic(wrap(creator.createIcon(Theme.blackFillBinding(), -1, -1)));
        ret.setText(text);
        ret.setOnMouseClicked(e -> onClick.run());
        return ret;
    }

    public static JFXButton createDecoratorButton(String tooltip, SVG.SVGIcon creator, Runnable onClick) {
        JFXButton ret = new JFXButton();
        ret.getStyleClass().add("jfx-decorator-button");
        ret.textFillProperty().bind(Theme.foregroundFillBinding());
        ret.setGraphic(wrap(creator.createIcon(Theme.foregroundFillBinding(), -1, -1)));
        FXUtils.installFastTooltip(ret, tooltip);
        ret.setOnMouseClicked(e -> onClick.run());
        return ret;
    }

    protected abstract List<Node> initializeToolbar(T skinnable);
}
