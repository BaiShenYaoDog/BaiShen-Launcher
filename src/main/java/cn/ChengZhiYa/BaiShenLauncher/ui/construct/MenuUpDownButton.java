package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import com.jfoenix.controls.JFXButton;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.HBox;

public class MenuUpDownButton extends Control {

    private final BooleanProperty selected = new SimpleBooleanProperty(this, "selected");
    private final StringProperty text = new SimpleStringProperty(this, "text");

    public MenuUpDownButton() {
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new MenuUpDownButtonSkin(this);
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public String getText() {
        return text.get();
    }

    public void setText(String text) {
        this.text.set(text);
    }

    public StringProperty textProperty() {
        return text;
    }

    private static class MenuUpDownButtonSkin extends SkinBase<MenuUpDownButton> {

        protected MenuUpDownButtonSkin(MenuUpDownButton control) {
            super(control);

            HBox content = new HBox(8);
            content.setAlignment(Pos.CENTER);
            Label label = new Label();
            label.setStyle("-fx-text-fill: black;");
            label.textProperty().bind(control.text);

            Node up = SVG.menuUp(Theme.blackFillBinding(), 16, 16);
            Node down = SVG.menuDown(Theme.blackFillBinding(), 16, 16);

            JFXButton button = new JFXButton();
            button.setGraphic(content);
            button.setOnAction(e -> {
                control.selected.set(!control.isSelected());
            });

            FXUtils.onChangeAndOperate(control.selected, selected -> {
                if (selected) {
                    content.getChildren().setAll(label, up);
                } else {
                    content.getChildren().setAll(label, down);
                }
            });

            getChildren().setAll(button);
        }
    }
}
