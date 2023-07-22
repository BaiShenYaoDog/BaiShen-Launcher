package cn.ChengZhiYa.BaiShenLauncher.ui.main;

import cn.ChengZhiYa.BaiShenLauncher.Metadata;
import cn.ChengZhiYa.BaiShenLauncher.game.Version;
import cn.ChengZhiYa.BaiShenLauncher.setting.ConfigHolder;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profiles;
import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.AnimationUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.AnnouncementCard;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.MessageDialogPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.PopupMenu;
import cn.ChengZhiYa.BaiShenLauncher.ui.decorator.DecoratorPage;
import cn.ChengZhiYa.BaiShenLauncher.ui.versions.GameItem;
import cn.ChengZhiYa.BaiShenLauncher.ui.versions.Versions;
import cn.ChengZhiYa.BaiShenLauncher.upgrade.RemoteVersion;
import cn.ChengZhiYa.BaiShenLauncher.upgrade.UpdateChecker;
import cn.ChengZhiYa.BaiShenLauncher.upgrade.UpdateHandler;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.javafx.MappedObservableList;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.JavaVersion;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPopup;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.SINE;

public final class MainPage extends StackPane implements DecoratorPage {
    private static final String ANNOUNCEMENT = "announcement";

    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>();

    private final PopupMenu menu = new PopupMenu();
    private final JFXPopup popup = new JFXPopup(menu);

    private final StringProperty currentGame = new SimpleStringProperty(this, "currentGame");
    private final BooleanProperty showUpdate = new SimpleBooleanProperty(this, "showUpdate");
    private final ObjectProperty<RemoteVersion> latestVersion = new SimpleObjectProperty<>(this, "latestVersion");
    private final ObservableList<Version> versions = FXCollections.observableArrayList();
    private final ObservableList<Node> versionNodes;
    private final StackPane updatePane;
    private final JFXButton menuButton;
    private Profile profile;
    private VBox announcementPane;

    {
        HBox titleNode = new HBox(8);
        titleNode.setPadding(new Insets(0, 0, 0, 2));
        titleNode.setAlignment(Pos.CENTER_LEFT);

        ImageView titleIcon = new ImageView();
        if (JavaVersion.CURRENT_JAVA.getParsedVersion() < 9) {
            // JavaFX 8 has some problems with @2x images
            // Force load the original icon
            try (InputStream is = MainPage.class.getResourceAsStream("/assets/img/icon.png")) {
                titleIcon.setImage(new Image(is, 32, 32, false, true));
            } catch (IOException ignored) {
            }
        } else {
            titleIcon.setImage(new Image("/assets/img/icon.png", 32, 32, false, true));
        }

        Label titleLabel = new Label(Metadata.FULL_TITLE);
        titleLabel.getStyleClass().add("jfx-decorator-title");
        titleNode.getChildren().setAll(titleIcon, titleLabel);

        state.setValue(new State(null, titleNode, false, false, true));

        setPadding(new Insets(20));

        if (Metadata.isNightly() || (Metadata.isDev() && !Objects.equals(Metadata.VERSION, ConfigHolder.config().getShownTips().get(ANNOUNCEMENT)))) {
            announcementPane = new VBox(16);
            announcementPane.getChildren().add(new AnnouncementCard("白神启动器", "欸嘿嘿,欢迎使用白神启动器 嗷呜\n" +
                    "此启动器与白神遥本人/PSPLive无关,仅个人使用 | 该启动器基于HMCL开源项目\n" +
                    "\n" +
                    "<a href=\"https://space.bilibili.com/477332594\">白神遥主页</a>  <a href=\"https://live.bilibili.com/21652717\">白神遥直播间</a>  <a href=\"https://Launcher.白神遥.cn\">启动器官网</a>"));
            getChildren().add(announcementPane);
        }

        updatePane = new StackPane();
        updatePane.setVisible(false);
        updatePane.getStyleClass().add("bubble");
        FXUtils.setLimitWidth(updatePane, 230);
        FXUtils.setLimitHeight(updatePane, 55);
        StackPane.setAlignment(updatePane, Pos.TOP_RIGHT);
        updatePane.setOnMouseClicked(e -> onUpgrade());
        FXUtils.onChange(showUpdateProperty(), this::showUpdate);

        {
            HBox hBox = new HBox();
            hBox.setSpacing(12);
            hBox.setAlignment(Pos.CENTER_LEFT);
            StackPane.setAlignment(hBox, Pos.CENTER_LEFT);
            StackPane.setMargin(hBox, new Insets(9, 12, 9, 16));

            updatePane.getChildren().setAll(hBox);
        }

        StackPane launchPane = new StackPane();
        launchPane.getStyleClass().add("launch-pane");
        launchPane.setMaxWidth(230);
        launchPane.setMaxHeight(55);
        launchPane.setOnScroll(event -> {
            int index = IntStream.range(0, versions.size())
                    .filter(i -> versions.get(i).getId().equals(getCurrentGame()))
                    .findFirst().orElse(-1);
            if (index < 0) return;
            if (event.getDeltaY() > 0) {
                index--;
            } else {
                index++;
            }
            profile.setSelectedVersion(versions.get((index + versions.size()) % versions.size()).getId());
        });
        StackPane.setAlignment(launchPane, Pos.BOTTOM_RIGHT);
        {
            JFXButton launchButton = new JFXButton();
            launchButton.setPrefWidth(230);
            launchButton.setPrefHeight(55);
            //launchButton.setButtonType(JFXButton.ButtonType.RAISED);
            launchButton.setOnAction(e -> launch());
            launchButton.setDefaultButton(true);
            launchButton.setClip(new Rectangle(-100, -100, 310, 200));
            {
                VBox graphic = new VBox();
                graphic.setAlignment(Pos.CENTER);
                graphic.setTranslateX(-7);
                graphic.setMaxWidth(200);
                Label launchLabel = new Label("和豹豹一起玩MC,嗷呜~");
                launchLabel.setStyle("-fx-font-size: 16px;");
                Label currentLabel = new Label();
                currentLabel.setStyle("-fx-font-size: 12px;");
                currentLabel.textProperty().bind(Bindings.createStringBinding(() -> {
                    if (getCurrentGame() == null) {
                        return "还没安装版本呢";
                    } else {
                        return getCurrentGame();
                    }
                }, currentGameProperty()));
                graphic.getChildren().setAll(launchLabel, currentLabel);

                launchButton.setGraphic(graphic);
            }

            Rectangle separator = new Rectangle();
            separator.setWidth(1);
            separator.setHeight(57);
            separator.setTranslateX(95);
            separator.setMouseTransparent(true);

            menuButton = new JFXButton();
            menuButton.setPrefHeight(55);
            menuButton.setPrefWidth(230);
            //menuButton.setButtonType(JFXButton.ButtonType.RAISED);
            menuButton.setStyle("-fx-font-size: 15px;");
            menuButton.setOnMouseClicked(e -> onMenu());
            menuButton.setClip(new Rectangle(211, -100, 100, 200));
            StackPane graphic = new StackPane();
            Node svg = SVG.triangle(Theme.foregroundFillBinding(), 10, 10);
            StackPane.setAlignment(svg, Pos.CENTER_RIGHT);
            graphic.getChildren().setAll(svg);
            graphic.setTranslateX(12);
            menuButton.setGraphic(graphic);

            launchPane.getChildren().setAll(launchButton, separator, menuButton);
        }

        getChildren().addAll(updatePane, launchPane);

        menu.setMaxHeight(365);
        menu.setMaxWidth(545);
        menu.setAlwaysShowingVBar(true);
        menu.setOnMouseClicked(e -> popup.hide());
        versionNodes = MappedObservableList.create(versions, version -> {
            Node node = PopupMenu.wrapPopupMenuItem(new GameItem(profile, version.getId()));
            node.setOnMouseClicked(e -> profile.setSelectedVersion(version.getId()));
            return node;
        });
        Bindings.bindContent(menu.getContent(), versionNodes);
    }

    private void showUpdate(boolean show) {
        doAnimation(show);

        if (show && getLatestVersion() != null && !Objects.equals(ConfigHolder.config().getPromptedVersion(), getLatestVersion().getVersion())) {
            Controllers.dialog("", I18n.i18n("update.bubble.title", getLatestVersion().getVersion()), MessageDialogPane.MessageType.INFO, () -> {
                ConfigHolder.config().setPromptedVersion(getLatestVersion().getVersion());
                onUpgrade();
            });
        }
    }

    private void doAnimation(boolean show) {
        if (AnimationUtils.isAnimationEnabled()) {
            Duration duration = Duration.millis(320);
            Timeline nowAnimation = new Timeline();
            nowAnimation.getKeyFrames().addAll(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(updatePane.translateXProperty(), show ? 260 : 0, SINE)),
                    new KeyFrame(duration,
                            new KeyValue(updatePane.translateXProperty(), show ? 0 : 260, SINE)));
            if (show) nowAnimation.getKeyFrames().add(
                    new KeyFrame(Duration.ZERO, e -> updatePane.setVisible(true)));
            else nowAnimation.getKeyFrames().add(
                    new KeyFrame(duration, e -> updatePane.setVisible(false)));
            nowAnimation.play();
        } else {
            updatePane.setVisible(show);
        }
    }

    private void launch() {
        Versions.launch(Profiles.getSelectedProfile());
    }

    private void onMenu() {
        popup.show(menuButton, JFXPopup.PopupVPosition.BOTTOM, JFXPopup.PopupHPosition.RIGHT, 0, -menuButton.getHeight());
    }

    private void onUpgrade() {
        RemoteVersion target = UpdateChecker.getLatestVersion();
        if (target == null) {
            return;
        }
        UpdateHandler.updateFrom(target);
    }

    private void closeUpdateBubble() {
        showUpdate.unbind();
        showUpdate.set(false);
    }

    public void hideAnnouncementPane() {
        if (announcementPane != null) {
            ConfigHolder.config().getShownTips().put(ANNOUNCEMENT, Metadata.VERSION);
            Pane parent = (Pane) announcementPane.getParent();
            if (parent != null)
                parent.getChildren().remove(announcementPane);
            announcementPane = null;
        }
    }

    @Override
    public ReadOnlyObjectWrapper<State> stateProperty() {
        return state;
    }

    public String getCurrentGame() {
        return currentGame.get();
    }

    public void setCurrentGame(String currentGame) {
        this.currentGame.set(currentGame);
    }

    public StringProperty currentGameProperty() {
        return currentGame;
    }

    public boolean isShowUpdate() {
        return showUpdate.get();
    }

    public void setShowUpdate(boolean showUpdate) {
        this.showUpdate.set(showUpdate);
    }

    public BooleanProperty showUpdateProperty() {
        return showUpdate;
    }

    public RemoteVersion getLatestVersion() {
        return latestVersion.get();
    }

    public void setLatestVersion(RemoteVersion latestVersion) {
        this.latestVersion.set(latestVersion);
    }

    public ObjectProperty<RemoteVersion> latestVersionProperty() {
        return latestVersion;
    }

    public void initVersions(Profile profile, List<Version> versions) {
        FXUtils.checkFxUserThread();
        this.profile = profile;
        this.versions.setAll(versions);
    }
}
