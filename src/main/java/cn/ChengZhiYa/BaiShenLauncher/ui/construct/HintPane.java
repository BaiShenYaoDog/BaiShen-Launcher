package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.Locale;

public class HintPane extends VBox {
    private final Text label = new Text();
    private final StringProperty text = new SimpleStringProperty(this, "text");
    private final TextFlow flow = new TextFlow();

    public HintPane() {
        this(MessageDialogPane.MessageType.INFO);
    }

    public HintPane(MessageDialogPane.MessageType type) {
        setFillWidth(true);
        getStyleClass().addAll("hint", type.name().toLowerCase(Locale.ROOT));
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_LEFT);

        switch (type) {
            case INFO:
                hbox.getChildren().add(SVG.informationOutline(Theme.blackFillBinding(), 16, 16));
                break;
            case ERROR:
                hbox.getChildren().add(SVG.closeCircleOutline(Theme.blackFillBinding(), 16, 16));
                break;
            case SUCCESS:
                hbox.getChildren().add(SVG.checkCircleOutline(Theme.blackFillBinding(), 16, 16));
                break;
            case WARNING:
                hbox.getChildren().add(SVG.alertOutline(Theme.blackFillBinding(), 16, 16));
                break;
            case QUESTION:
                hbox.getChildren().add(SVG.helpCircleOutline(Theme.blackFillBinding(), 16, 16));
                break;
            default:
                throw new IllegalArgumentException("Unrecognized message box message type " + type);
        }


        hbox.getChildren().add(new Text(type.getDisplayName()));
        flow.getChildren().setAll(label);
        getChildren().setAll(hbox, flow);
        label.textProperty().bind(text);
        VBox.setMargin(flow, new Insets(2, 2, 0, 2));
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

    public void setSegment(String segment) {
        flow.getChildren().setAll(FXUtils.parseSegment(segment, Controllers::onHyperlinkAction));
    }

    public void setChildren(Node... children) {
        flow.getChildren().setAll(children);
    }
}
