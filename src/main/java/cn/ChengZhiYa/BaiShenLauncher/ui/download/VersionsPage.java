package cn.ChengZhiYa.BaiShenLauncher.ui.download;

import cn.ChengZhiYa.BaiShenLauncher.download.DownloadProvider;
import cn.ChengZhiYa.BaiShenLauncher.download.RemoteVersion;
import cn.ChengZhiYa.BaiShenLauncher.download.VersionList;
import cn.ChengZhiYa.BaiShenLauncher.download.fabric.FabricAPIRemoteVersion;
import cn.ChengZhiYa.BaiShenLauncher.download.fabric.FabricRemoteVersion;
import cn.ChengZhiYa.BaiShenLauncher.download.forge.ForgeRemoteVersion;
import cn.ChengZhiYa.BaiShenLauncher.download.game.GameRemoteVersion;
import cn.ChengZhiYa.BaiShenLauncher.download.liteloader.LiteLoaderRemoteVersion;
import cn.ChengZhiYa.BaiShenLauncher.download.optifine.OptiFineRemoteVersion;
import cn.ChengZhiYa.BaiShenLauncher.download.quilt.QuiltAPIRemoteVersion;
import cn.ChengZhiYa.BaiShenLauncher.download.quilt.QuiltRemoteVersion;
import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.setting.VersionIconType;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.ContainerAnimations;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.TransitionPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.ComponentList;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.HintPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.IconedTwoLineListItem;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.RipplerContainer;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.Navigation;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.Refreshable;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardPage;
import cn.ChengZhiYa.BaiShenLauncher.util.HMCLService;
import cn.ChengZhiYa.BaiShenLauncher.util.Holder;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.Locales;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXSpinner;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static cn.ChengZhiYa.BaiShenLauncher.ui.ToolbarListPageSkin.wrap;

public final class VersionsPage extends BorderPane implements WizardPage, Refreshable {
    private final String gameVersion;
    private final String libraryId;
    private final String title;
    private final Navigation navigation;

    private final JFXListView<RemoteVersion> list;
    private final JFXSpinner spinner;
    private final StackPane failedPane;
    private final StackPane emptyPane;
    private final TransitionPane root;
    private final JFXCheckBox chkRelease;
    private final JFXCheckBox chkSnapshot;
    private final JFXCheckBox chkOld;
    private final JFXButton btnRefresh;
    private final HBox checkPane;
    private final ComponentList centrePane;
    private final StackPane center;

    private final VersionList<?> versionList;
    private CompletableFuture<?> executor;

    public VersionsPage(Navigation navigation, String title, String gameVersion, DownloadProvider downloadProvider, String libraryId, Runnable callback) {
        this.title = title;
        this.gameVersion = gameVersion;
        this.libraryId = libraryId;
        this.navigation = navigation;

        HintPane hintPane = new HintPane();
        hintPane.setText("来选择一个你想要版本吧,嗷呜~");
        hintPane.getStyleClass().add("sponsor-pane");
        BorderPane.setMargin(hintPane, new Insets(10, 10, 0, 10));
        this.setTop(hintPane);

        root = new TransitionPane();
        {
            spinner = new JFXSpinner();

            center = new StackPane();
            center.setStyle("-fx-padding: 10;");
            {
                centrePane = new ComponentList();
                centrePane.getStyleClass().add("no-padding");
                {
                    checkPane = new HBox();
                    checkPane.setSpacing(10);
                    {
                        chkRelease = new JFXCheckBox(I18n.i18n("version.game.releases"));
                        chkRelease.setSelected(true);
                        HBox.setMargin(chkRelease, new Insets(10, 0, 10, 0));

                        chkSnapshot = new JFXCheckBox(I18n.i18n("version.game.snapshots"));
                        HBox.setMargin(chkSnapshot, new Insets(10, 0, 10, 0));

                        chkOld = new JFXCheckBox(I18n.i18n("version.game.old"));
                        HBox.setMargin(chkOld, new Insets(10, 0, 10, 0));

                        HBox pane = new HBox();
                        HBox.setHgrow(pane, Priority.ALWAYS);

                        btnRefresh = new JFXButton(I18n.i18n("button.refresh"));
                        btnRefresh.getStyleClass().add("jfx-tool-bar-button");
                        btnRefresh.setOnAction(e -> onRefresh());

                        checkPane.getChildren().setAll(chkRelease, chkSnapshot, chkOld, pane, btnRefresh);
                    }

                    list = new JFXListView<>();
                    list.getStyleClass().add("jfx-list-view-float");
                    VBox.setVgrow(list, Priority.ALWAYS);

                    centrePane.getContent().setAll(checkPane, list);
                }

                center.getChildren().setAll(centrePane);
            }

            failedPane = new StackPane();
            failedPane.getStyleClass().add("notice-pane");
            {
                Label label = new Label(I18n.i18n("download.failed.refresh"));
                label.setOnMouseClicked(e -> onRefresh());

                failedPane.getChildren().setAll(label);
            }

            emptyPane = new StackPane();
            emptyPane.getStyleClass().add("notice-pane");
            {
                Label label = new Label(I18n.i18n("download.failed.empty"));
                label.setOnMouseClicked(e -> onBack());

                emptyPane.getChildren().setAll(label);
            }
        }
        this.setCenter(root);

        versionList = downloadProvider.getVersionListById(libraryId);
        if (versionList.hasType()) {
            centrePane.getContent().setAll(checkPane, list);
        } else {
            centrePane.getContent().setAll(list);
        }
        ComponentList.setVgrow(list, Priority.ALWAYS);

        InvalidationListener listener = o -> list.getItems().setAll(loadVersions());
        chkRelease.selectedProperty().addListener(listener);
        chkSnapshot.selectedProperty().addListener(listener);
        chkOld.selectedProperty().addListener(listener);

        btnRefresh.setGraphic(wrap(SVG.refresh(Theme.blackFillBinding(), -1, -1)));

        Holder<RemoteVersionListCell> lastCell = new Holder<>();
        EnumMap<VersionIconType, Image> icons = new EnumMap<>(VersionIconType.class);
        list.setCellFactory(listView -> new RemoteVersionListCell(lastCell, icons));

        list.setOnMouseClicked(e -> {
            if (list.getSelectionModel().getSelectedIndex() < 0)
                return;
            navigation.getSettings().put(libraryId, list.getSelectionModel().getSelectedItem());
            callback.run();
        });

        refresh();
    }

    private List<RemoteVersion> loadVersions() {
        return versionList.getVersions(gameVersion).stream()
                .filter(it -> {
                    switch (it.getVersionType()) {
                        case RELEASE:
                            return chkRelease.isSelected();
                        case SNAPSHOT:
                            return chkSnapshot.isSelected();
                        case OLD:
                            return chkOld.isSelected();
                        default:
                            return true;
                    }
                })
                .sorted().collect(Collectors.toList());
    }

    @Override
    public void refresh() {
        VersionList<?> currentVersionList = versionList;
        root.setContent(spinner, ContainerAnimations.FADE.getAnimationProducer());
        executor = currentVersionList.refreshAsync(gameVersion).whenComplete((result, exception) -> {
            if (exception == null) {
                List<RemoteVersion> items = loadVersions();

                Platform.runLater(() -> {
                    if (versionList != currentVersionList) return;
                    if (currentVersionList.getVersions(gameVersion).isEmpty()) {
                        root.setContent(emptyPane, ContainerAnimations.FADE.getAnimationProducer());
                    } else {
                        if (items.isEmpty()) {
                            chkRelease.setSelected(true);
                            chkSnapshot.setSelected(true);
                            chkOld.setSelected(true);
                        } else {
                            list.getItems().setAll(items);
                        }
                        root.setContent(center, ContainerAnimations.FADE.getAnimationProducer());
                    }
                });
            } else {
                Logging.LOG.log(Level.WARNING, "Failed to fetch versions list", exception);
                Platform.runLater(() -> {
                    if (versionList != currentVersionList) return;
                    root.setContent(failedPane, ContainerAnimations.FADE.getAnimationProducer());
                });
            }

            // https://github.com/huanghongxun/HMCL/issues/938
            System.gc();
        });
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void cleanup(Map<String, Object> settings) {
        settings.remove(libraryId);
        if (executor != null)
            executor.cancel(true);
    }

    private void onRefresh() {
        refresh();
    }

    private void onBack() {
        navigation.onPrev(true);
    }

    private void onSponsor() {
        HMCLService.openRedirectLink("bmclapi_sponsor");
    }

    private static class RemoteVersionListCell extends ListCell<RemoteVersion> {
        final IconedTwoLineListItem content = new IconedTwoLineListItem();
        final RipplerContainer ripplerContainer = new RipplerContainer(content);
        final StackPane pane = new StackPane();

        private final Holder<RemoteVersionListCell> lastCell;
        private final EnumMap<VersionIconType, Image> icons;

        RemoteVersionListCell(Holder<RemoteVersionListCell> lastCell, EnumMap<VersionIconType, Image> icons) {
            this.lastCell = lastCell;
            this.icons = icons;

            pane.getStyleClass().add("md-list-cell");
            StackPane.setMargin(content, new Insets(10, 16, 10, 16));
            pane.getChildren().setAll(ripplerContainer);
        }

        private Image getIcon(VersionIconType type) {
            return icons.computeIfAbsent(type, iconType -> new Image(iconType.getResourceUrl()));
        }

        @Override
        public void updateItem(RemoteVersion remoteVersion, boolean empty) {
            super.updateItem(remoteVersion, empty);

            // https://mail.openjdk.org/pipermail/openjfx-dev/2022-July/034764.html
            if (this == lastCell.value && !isVisible())
                return;
            lastCell.value = this;

            if (empty) {
                setGraphic(null);
                return;
            }
            setGraphic(pane);

            content.setTitle(remoteVersion.getSelfVersion());
            if (remoteVersion.getReleaseDate() != null) {
                content.setSubtitle(Locales.DATE_TIME_FORMATTER.get().format(remoteVersion.getReleaseDate().toInstant()));
            } else {
                content.setSubtitle(null);
            }

            if (remoteVersion instanceof GameRemoteVersion) {
                switch (remoteVersion.getVersionType()) {
                    case RELEASE:
                        content.getTags().setAll(I18n.i18n("version.game.release"));
                        content.setImage(getIcon(VersionIconType.GRASS));
                        break;
                    case SNAPSHOT:
                        content.getTags().setAll(I18n.i18n("version.game.snapshot"));
                        content.setImage(getIcon(VersionIconType.COMMAND));
                        break;
                    default:
                        content.getTags().setAll(I18n.i18n("version.game.old"));
                        content.setImage(getIcon(VersionIconType.CRAFT_TABLE));
                        break;
                }
            } else {
                VersionIconType iconType;
                if (remoteVersion instanceof LiteLoaderRemoteVersion)
                    iconType = VersionIconType.CHICKEN;
                else if (remoteVersion instanceof OptiFineRemoteVersion)
                    iconType = VersionIconType.COMMAND;
                else if (remoteVersion instanceof ForgeRemoteVersion)
                    iconType = VersionIconType.FORGE;
                else if (remoteVersion instanceof FabricRemoteVersion || remoteVersion instanceof FabricAPIRemoteVersion)
                    iconType = VersionIconType.FABRIC;
                else if (remoteVersion instanceof QuiltRemoteVersion || remoteVersion instanceof QuiltAPIRemoteVersion)
                    iconType = VersionIconType.QUILT;
                else
                    iconType = null;

                content.setImage(iconType != null ? getIcon(iconType) : null);
                if (content.getSubtitle() == null)
                    content.setSubtitle(remoteVersion.getGameVersion());
                else
                    content.getTags().setAll(remoteVersion.getGameVersion());
            }
        }
    }
}
