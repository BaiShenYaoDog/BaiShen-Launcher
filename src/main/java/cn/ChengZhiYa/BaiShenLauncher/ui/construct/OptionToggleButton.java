package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import com.jfoenix.controls.JFXToggleButton;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class OptionToggleButton extends StackPane {
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty subtitle = new SimpleStringProperty();
    private final BooleanProperty selected = new SimpleBooleanProperty();

    public OptionToggleButton() {
        getProperties().put("ComponentList.noPadding", true);

        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(8, 8, 8, 16));
        RipplerContainer container = new RipplerContainer(pane);
        getChildren().setAll(container);

        VBox left = new VBox();
        left.setMouseTransparent(true);
        Label titleLabel = new Label();
        titleLabel.textProperty().bind(title);
        Label subtitleLabel = new Label();
        subtitleLabel.setMouseTransparent(true);
        subtitleLabel.setWrapText(true);
        subtitleLabel.textProperty().bind(subtitle);
        pane.setCenter(left);
        left.setAlignment(Pos.CENTER_LEFT);

        JFXToggleButton toggleButton = new JFXToggleButton();
        pane.setRight(toggleButton);
        toggleButton.selectedProperty().bindBidirectional(selected);
        toggleButton.setSize(8);
        FXUtils.setLimitHeight(toggleButton, 30);

        container.setOnMouseClicked(e -> {
            toggleButton.setSelected(!toggleButton.isSelected());
        });

        FXUtils.onChangeAndOperate(subtitleProperty(), subtitle -> {
            if (StringUtils.isNotBlank(subtitle)) {
                left.getChildren().setAll(titleLabel, subtitleLabel);
            } else {
                left.getChildren().setAll(titleLabel);
            }
        });
    }

    public String getTitle() {
        return title.get();
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public StringProperty titleProperty() {
        return title;
    }

    public String getSubtitle() {
        return subtitle.get();
    }

    public void setSubtitle(String subtitle) {
        this.subtitle.set(subtitle);
    }

    public StringProperty subtitleProperty() {
        return subtitle;
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
}
