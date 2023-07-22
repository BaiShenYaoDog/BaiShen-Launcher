package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.util.Lazy;
import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import com.jfoenix.controls.JFXButton;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class IconedTwoLineListItem extends HBox {
    private final StringProperty title = new SimpleStringProperty(this, "title");
    private final ObservableList<String> tags = FXCollections.observableArrayList();
    private final StringProperty subtitle = new SimpleStringProperty(this, "subtitle");
    private final StringProperty externalLink = new SimpleStringProperty(this, "externalLink");
    private final ObjectProperty<Image> image = new SimpleObjectProperty<>(this, "image");

    private final ImageView imageView = new ImageView();
    private final TwoLineListItem twoLineListItem = new TwoLineListItem();
    private final Lazy<JFXButton> externalLinkButton = new Lazy<>(() -> {
        JFXButton button = new JFXButton();
        button.getStyleClass().add("toggle-icon4");
        button.setGraphic(SVG.openInNew(Theme.blackFillBinding(), -1, -1));
        button.setOnAction(e -> FXUtils.openLink(externalLink.get()));
        return button;
    });
    @SuppressWarnings("FieldCanBeLocal")
    private final InvalidationListener observer;

    public IconedTwoLineListItem() {
        setAlignment(Pos.CENTER);
        setSpacing(16);
        imageView.imageProperty().bind(image);
        twoLineListItem.titleProperty().bind(title);
        twoLineListItem.subtitleProperty().bind(subtitle);
        HBox.setHgrow(twoLineListItem, Priority.ALWAYS);
        Bindings.bindContent(twoLineListItem.getTags(), tags);

        observer = FXUtils.observeWeak(() -> {
            getChildren().clear();
            if (image.get() != null) getChildren().add(imageView);
            getChildren().add(twoLineListItem);
            if (StringUtils.isNotBlank(externalLink.get())) getChildren().add(externalLinkButton.get());
        }, image, externalLink);
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

    public ObservableList<String> getTags() {
        return tags;
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

    public String getExternalLink() {
        return externalLink.get();
    }

    public void setExternalLink(String externalLink) {
        this.externalLink.set(externalLink);
    }

    public StringProperty externalLinkProperty() {
        return externalLink;
    }

    public Image getImage() {
        return image.get();
    }

    public void setImage(Image image) {
        this.image.set(image);
    }

    public ObjectProperty<Image> imageProperty() {
        return image;
    }

    public ImageView getImageView() {
        return imageView;
    }
}
