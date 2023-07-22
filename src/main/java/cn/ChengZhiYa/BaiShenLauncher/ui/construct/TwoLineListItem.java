package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.AggregatedObservableList;
import cn.ChengZhiYa.BaiShenLauncher.util.javafx.MappedObservableList;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class TwoLineListItem extends VBox {
    private static final String DEFAULT_STYLE_CLASS = "two-line-list-item";

    private final StringProperty title = new SimpleStringProperty(this, "title");
    private final ObservableList<String> tags = FXCollections.observableArrayList();
    private final StringProperty subtitle = new SimpleStringProperty(this, "subtitle");

    private final ObservableList<Node> tagLabels;
    private final AggregatedObservableList<Node> firstLineChildren;

    public TwoLineListItem(String titleString, String subtitleString) {
        this();

        title.set(titleString);
        subtitle.set(subtitleString);
    }

    public TwoLineListItem() {
        setMouseTransparent(true);

        HBox firstLine = new HBox();
        firstLine.getStyleClass().add("first-line");

        Label lblTitle = new Label();
        lblTitle.getStyleClass().add("title");
        lblTitle.textProperty().bind(title);

        tagLabels = MappedObservableList.create(tags, tag -> {
            Label tagLabel = new Label();
            tagLabel.getStyleClass().add("tag");
            tagLabel.setText(tag);
            HBox.setMargin(tagLabel, new Insets(0, 8, 0, 0));
            return tagLabel;
        });
        firstLineChildren = new AggregatedObservableList<>();
        firstLineChildren.appendList(FXCollections.singletonObservableList(lblTitle));
        firstLineChildren.appendList(tagLabels);
        Bindings.bindContent(firstLine.getChildren(), firstLineChildren.getAggregatedList());

        Label lblSubtitle = new Label();
        lblSubtitle.getStyleClass().add("subtitle");
        lblSubtitle.textProperty().bind(subtitle);

        HBox secondLine = new HBox();
        secondLine.getChildren().setAll(lblSubtitle);

        getChildren().setAll(firstLine, secondLine);

        FXUtils.onChangeAndOperate(subtitle, subtitleString -> {
            if (subtitleString == null) getChildren().setAll(firstLine);
            else getChildren().setAll(firstLine, secondLine);
        });

        getStyleClass().add(DEFAULT_STYLE_CLASS);
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

    public ObservableList<String> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
