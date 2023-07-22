package cn.ChengZhiYa.BaiShenLauncher.ui.versions;

import cn.ChengZhiYa.BaiShenLauncher.mod.LocalModFile;
import cn.ChengZhiYa.BaiShenLauncher.mod.ModManager;
import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.ContainerAnimations;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.TransitionPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.*;
import cn.ChengZhiYa.BaiShenLauncher.util.*;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.io.CompressingUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.NetworkUtils;
import com.jfoenix.controls.*;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SkinBase;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.onEscPressed;
import static cn.ChengZhiYa.BaiShenLauncher.ui.ToolbarListPageSkin.createToolbarButton2;

class ModListPageSkin extends SkinBase<ModListPage> {

    private static final Lazy<PopupMenu> menu = new Lazy<>(PopupMenu::new);
    private static final Lazy<JFXPopup> popup = new Lazy<>(() -> new JFXPopup(menu.get()));
    private final TransitionPane toolbarPane;
    private final HBox searchBar;
    private final HBox toolbarNormal;
    private final HBox toolbarSelecting;
    private final JFXListView<ModInfoObject> listView;
    private final JFXTextField searchField;
    // FXThread
    private boolean isSearching = false;

    ModListPageSkin(ModListPage skinnable) {
        super(skinnable);

        StackPane pane = new StackPane();
        pane.setPadding(new Insets(10));
        pane.getStyleClass().addAll("notice-pane");

        ComponentList root = new ComponentList();
        root.getStyleClass().add("no-padding");
        listView = new JFXListView<>();

        {
            toolbarPane = new TransitionPane();

            searchBar = new HBox();
            toolbarNormal = new HBox();
            toolbarSelecting = new HBox();

            // Search Bar
            searchBar.setAlignment(Pos.CENTER);
            searchBar.setPadding(new Insets(0, 5, 0, 5));
            searchField = new JFXTextField();
            searchField.setPromptText(I18n.i18n("search"));
            HBox.setHgrow(searchField, Priority.ALWAYS);
            searchField.setOnAction(e -> search());

            JFXButton closeSearchBar = createToolbarButton2(null, SVG::close,
                    () -> {
                        changeToolbar(toolbarNormal);

                        isSearching = false;
                        searchField.clear();
                        Bindings.bindContent(listView.getItems(), getSkinnable().getItems());
                    });

            searchBar.getChildren().setAll(searchField, closeSearchBar);

            // Toolbar Normal
            toolbarNormal.getChildren().setAll(
                    createToolbarButton2(I18n.i18n("button.refresh"), SVG::refresh, skinnable::refresh),
                    createToolbarButton2(I18n.i18n("mods.add"), SVG::plus, skinnable::add),
                    createToolbarButton2(I18n.i18n("folder.mod"), SVG::folderOpen, skinnable::openModFolder),
                    createToolbarButton2(I18n.i18n("mods.check_updates"), SVG::update, skinnable::checkUpdates),
                    createToolbarButton2(I18n.i18n("download"), SVG::downloadOutline, skinnable::download),
                    createToolbarButton2(I18n.i18n("search"), SVG::magnify, () -> changeToolbar(searchBar))
            );

            // Toolbar Selecting
            toolbarSelecting.getChildren().setAll(
                    createToolbarButton2(I18n.i18n("button.remove"), SVG::delete, () -> {
                        Controllers.confirm(I18n.i18n("button.remove.confirm"), I18n.i18n("button.remove"), () -> {
                            skinnable.removeSelected(listView.getSelectionModel().getSelectedItems());
                        }, null);
                    }),
                    createToolbarButton2(I18n.i18n("mods.enable"), SVG::check, () ->
                            skinnable.enableSelected(listView.getSelectionModel().getSelectedItems())),
                    createToolbarButton2(I18n.i18n("mods.disable"), SVG::close, () ->
                            skinnable.disableSelected(listView.getSelectionModel().getSelectedItems())),
                    createToolbarButton2(I18n.i18n("button.select_all"), SVG::selectAll, () ->
                            listView.getSelectionModel().selectAll()),
                    createToolbarButton2(I18n.i18n("button.cancel"), SVG::cancel, () ->
                            listView.getSelectionModel().clearSelection())
            );

            FXUtils.onChangeAndOperate(listView.getSelectionModel().selectedItemProperty(),
                    selectedItem -> {
                        if (selectedItem == null)
                            changeToolbar(isSearching ? searchBar : toolbarNormal);
                        else
                            changeToolbar(toolbarSelecting);
                    });
            root.getContent().add(toolbarPane);
        }

        {
            SpinnerPane center = new SpinnerPane();
            ComponentList.setVgrow(center, Priority.ALWAYS);
            center.getStyleClass().add("large-spinner-pane");
            center.loadingProperty().bind(skinnable.loadingProperty());

            Holder<Object> lastCell = new Holder<>();
            listView.setCellFactory(x -> new ModInfoListCell(listView, lastCell));
            listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            Bindings.bindContent(listView.getItems(), skinnable.getItems());

            center.setContent(listView);
            root.getContent().add(center);
        }

        Label label = new Label(I18n.i18n("mods.not_modded"));
        label.prefWidthProperty().bind(pane.widthProperty().add(-100));

        FXUtils.onChangeAndOperate(skinnable.moddedProperty(), modded -> {
            if (modded) pane.getChildren().setAll(root);
            else pane.getChildren().setAll(label);
        });

        getChildren().setAll(pane);
    }

    private void changeToolbar(HBox newToolbar) {
        Node oldToolbar = toolbarPane.getCurrentNode();
        if (newToolbar != oldToolbar) {
            toolbarPane.setContent(newToolbar, ContainerAnimations.FADE.getAnimationProducer());
        }
    }

    private void search() {
        isSearching = true;

        Bindings.unbindContent(listView.getItems(), getSkinnable().getItems());

        String queryString = searchField.getText();
        if (StringUtils.isBlank(queryString)) {
            listView.getItems().setAll(getSkinnable().getItems());
        } else {
            listView.getItems().clear();

            Predicate<String> predicate;
            if (queryString.startsWith("regex:")) {
                try {
                    Pattern pattern = Pattern.compile(queryString.substring("regex:".length()));
                    predicate = s -> pattern.matcher(s).find();
                } catch (Throwable e) {
                    Logging.LOG.log(Level.WARNING, "Illegal regular expression", e);
                    return;
                }
            } else {
                String lowerQueryString = queryString.toLowerCase(Locale.ROOT);
                predicate = s -> s.toLowerCase(Locale.ROOT).contains(lowerQueryString);
            }

            // Do we need to search in the background thread?
            for (ModInfoObject item : getSkinnable().getItems()) {
                if (predicate.test(item.getModInfo().getFileName())) {
                    listView.getItems().add(item);
                }
            }
        }
    }

    static class ModInfoObject extends RecursiveTreeObject<ModInfoObject> implements Comparable<ModInfoObject> {
        private final BooleanProperty active;
        private final LocalModFile localModFile;
        private final String message;
        private final ModTranslations.Mod mod;

        ModInfoObject(LocalModFile localModFile) {
            this.localModFile = localModFile;
            this.active = localModFile.activeProperty();
            StringBuilder message = new StringBuilder(localModFile.getName());
            if (StringUtils.isNotBlank(localModFile.getVersion()))
                message.append(", ").append(I18n.i18n("archive.version")).append(": ").append(localModFile.getVersion());
            if (StringUtils.isNotBlank(localModFile.getGameVersion()))
                message.append(", ").append(I18n.i18n("archive.game_version")).append(": ").append(localModFile.getGameVersion());
            if (StringUtils.isNotBlank(localModFile.getAuthors()))
                message.append(", ").append(I18n.i18n("archive.author")).append(": ").append(localModFile.getAuthors());
            this.message = message.toString();
            this.mod = ModTranslations.MOD.getModById(localModFile.getId());
        }

        String getTitle() {
            return localModFile.getFileName();
        }

        String getSubtitle() {
            return message;
        }

        LocalModFile getModInfo() {
            return localModFile;
        }

        public ModTranslations.Mod getMod() {
            return mod;
        }

        @Override
        public int compareTo(@NotNull ModListPageSkin.ModInfoObject o) {
            return localModFile.getFileName().toLowerCase().compareTo(o.localModFile.getFileName().toLowerCase());
        }
    }

    static class ModInfoDialog extends JFXDialogLayout {

        ModInfoDialog(ModInfoObject modInfo) {
            HBox titleContainer = new HBox();
            titleContainer.setSpacing(8);

            ImageView imageView = new ImageView();
            if (StringUtils.isNotBlank(modInfo.getModInfo().getLogoPath())) {
                Task.supplyAsync(() -> {
                    try (FileSystem fs = CompressingUtils.createReadOnlyZipFileSystem(modInfo.getModInfo().getFile())) {
                        Path iconPath = fs.getPath(modInfo.getModInfo().getLogoPath());
                        if (Files.exists(iconPath)) {
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            Files.copy(iconPath, stream);
                            return new ByteArrayInputStream(stream.toByteArray());
                        }
                    }
                    return null;
                }).whenComplete(Schedulers.javafx(), (stream, exception) -> {
                    if (stream != null) {
                        imageView.setImage(new Image(stream, 40, 40, true, true));
                    } else {
                        imageView.setImage(new Image("/assets/img/command.png", 40, 40, true, true));
                    }
                }).start();
            }

            TwoLineListItem title = new TwoLineListItem();
            title.setTitle(modInfo.getModInfo().getName());
            if (StringUtils.isNotBlank(modInfo.getModInfo().getVersion())) {
                title.getTags().setAll(modInfo.getModInfo().getVersion());
            }
            title.setSubtitle(FileUtils.getName(modInfo.getModInfo().getFile()));

            titleContainer.getChildren().setAll(FXUtils.limitingSize(imageView, 40, 40), title);
            setHeading(titleContainer);

            Label description = new Label(modInfo.getModInfo().getDescription().toString());
            setBody(description);

            if (StringUtils.isNotBlank(modInfo.getModInfo().getUrl())) {
                JFXHyperlink officialPageButton = new JFXHyperlink(I18n.i18n("mods.url"));
                officialPageButton.setOnAction(e -> {
                    fireEvent(new DialogCloseEvent());
                    FXUtils.openLink(modInfo.getModInfo().getUrl());
                });

                getActions().add(officialPageButton);
            }

            if (modInfo.getMod() != null && StringUtils.isNotBlank(modInfo.getMod().getMcbbs())) {
                JFXHyperlink mcbbsButton = new JFXHyperlink(I18n.i18n("mods.mcbbs"));
                mcbbsButton.setOnAction(e -> {
                    fireEvent(new DialogCloseEvent());
                    FXUtils.openLink(ModManager.getMcbbsUrl(modInfo.getMod().getMcbbs()));
                });
                getActions().add(mcbbsButton);
            }

            if (modInfo.getMod() == null || StringUtils.isBlank(modInfo.getMod().getMcmod())) {
                JFXHyperlink searchButton = new JFXHyperlink(I18n.i18n("mods.mcmod.search"));
                searchButton.setOnAction(e -> {
                    fireEvent(new DialogCloseEvent());
                    FXUtils.openLink(NetworkUtils.withQuery("https://search.mcmod.cn/s", Lang.mapOf(
                            Pair.pair("key", modInfo.getModInfo().getName()),
                            Pair.pair("site", "all"),
                            Pair.pair("filter", "0")
                    )));
                });
                getActions().add(searchButton);
            } else {
                JFXHyperlink mcmodButton = new JFXHyperlink(I18n.i18n("mods.mcmod.page"));
                mcmodButton.setOnAction(e -> {
                    fireEvent(new DialogCloseEvent());
                    FXUtils.openLink(ModTranslations.MOD.getMcmodUrl(modInfo.getMod()));
                });
                getActions().add(mcmodButton);
            }

            JFXButton okButton = new JFXButton();
            okButton.getStyleClass().add("dialog-accept");
            okButton.setText(I18n.i18n("button.ok"));
            okButton.setOnAction(e -> fireEvent(new DialogCloseEvent()));
            getActions().add(okButton);

            onEscPressed(this, okButton::fire);
        }
    }

    final class ModInfoListCell extends MDListCell<ModInfoObject> {
        JFXCheckBox checkBox = new JFXCheckBox();
        TwoLineListItem content = new TwoLineListItem();
        JFXButton restoreButton = new JFXButton();
        JFXButton infoButton = new JFXButton();
        JFXButton revealButton = new JFXButton();
        BooleanProperty booleanProperty;

        ModInfoListCell(JFXListView<ModInfoObject> listView, Holder<Object> lastCell) {
            super(listView, lastCell);

            HBox container = new HBox(8);
            container.setPickOnBounds(false);
            container.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(content, Priority.ALWAYS);
            content.setMouseTransparent(true);
            setSelectable();

            restoreButton.getStyleClass().add("toggle-icon4");
            restoreButton.setGraphic(FXUtils.limitingSize(SVG.restore(Theme.blackFillBinding(), 24, 24), 24, 24));

            FXUtils.installFastTooltip(restoreButton, I18n.i18n("mods.restore"));

            revealButton.getStyleClass().add("toggle-icon4");
            revealButton.setGraphic(FXUtils.limitingSize(SVG.folderOutline(Theme.blackFillBinding(), 24, 24), 24, 24));

            infoButton.getStyleClass().add("toggle-icon4");
            infoButton.setGraphic(FXUtils.limitingSize(SVG.informationOutline(Theme.blackFillBinding(), 24, 24), 24, 24));

            container.getChildren().setAll(checkBox, content, restoreButton, revealButton, infoButton);

            StackPane.setMargin(container, new Insets(8));
            getContainer().getChildren().setAll(container);
        }

        @Override
        protected void updateControl(ModInfoObject dataItem, boolean empty) {
            if (empty) return;
            content.setTitle(dataItem.getTitle());
            if (dataItem.getMod() != null && I18n.getCurrentLocale().getLocale() == Locale.CHINA) {
                content.getTags().setAll(dataItem.getMod().getDisplayName());
            } else {
                content.getTags().clear();
            }
            content.setSubtitle(dataItem.getSubtitle());
            if (booleanProperty != null) {
                checkBox.selectedProperty().unbindBidirectional(booleanProperty);
            }
            checkBox.selectedProperty().bindBidirectional(booleanProperty = dataItem.active);
            restoreButton.setVisible(!dataItem.getModInfo().getMod().getOldFiles().isEmpty());
            restoreButton.setOnMouseClicked(e -> {
                menu.get().getContent().setAll(dataItem.getModInfo().getMod().getOldFiles().stream()
                        .map(localModFile -> new IconedMenuItem(null, localModFile.getVersion(), () -> {
                            popup.get().hide();
                            getSkinnable().rollback(dataItem.getModInfo(), localModFile);
                        }))
                        .collect(Collectors.toList())
                );

                popup.get().show(restoreButton, JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.RIGHT, 0, restoreButton.getHeight());
            });
            revealButton.setOnMouseClicked(e -> {
                FXUtils.showFileInExplorer(dataItem.getModInfo().getFile());
            });
            infoButton.setOnMouseClicked(e -> {
                Controllers.dialog(new ModInfoDialog(dataItem));
            });
        }
    }
}