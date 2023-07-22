package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

public class AnnouncementCard extends VBox {

    public AnnouncementCard(String title, String content) {
        TextFlow tf = FXUtils.segmentToTextFlow(content, Controllers::onHyperlinkAction);

        Label label = new Label(title);
        label.getStyleClass().add("title");
        getChildren().setAll(label, tf);
        setSpacing(14);
        getStyleClass().addAll("card", "announcement");
    }
}
