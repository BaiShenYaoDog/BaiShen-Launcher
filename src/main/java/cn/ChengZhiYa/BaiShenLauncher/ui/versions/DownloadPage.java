package cn.ChengZhiYa.BaiShenLauncher.ui.versions;

import cn.ChengZhiYa.BaiShenLauncher.mod.ModManager;
import cn.ChengZhiYa.BaiShenLauncher.mod.RemoteMod;
import cn.ChengZhiYa.BaiShenLauncher.mod.RemoteModRepository;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.task.FileDownloadTask;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.*;
import cn.ChengZhiYa.BaiShenLauncher.ui.decorator.DecoratorPage;
import cn.ChengZhiYa.BaiShenLauncher.util.SimpleMultimap;
import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.TaskCancellationAction;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.io.NetworkUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.versioning.VersionNumber;
import com.jfoenix.controls.JFXButton;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DownloadPage extends Control implements DecoratorPage {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());
    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>();
    private final BooleanProperty loaded = new SimpleBooleanProperty(false);
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty failed = new SimpleBooleanProperty(false);
    private final RemoteModRepository repository;
    private final ModTranslations translations;
    private final RemoteMod addon;
    private final ModTranslations.Mod mod;
    private final Profile.ProfileVersion version;
    private final DownloadCallback callback;
    private final DownloadListPage page;
    private List<RemoteMod> dependencies;
    private SimpleMultimap<String, RemoteMod.Version> versions;

    public DownloadPage(DownloadListPage page, RemoteMod addon, Profile.ProfileVersion version, @Nullable DownloadCallback callback) {
        this.page = page;
        this.repository = page.repository;
        this.addon = addon;
        this.translations = ModTranslations.getTranslationsByRepositoryType(repository.getType());
        this.mod = translations.getModByCurseForgeId(addon.getSlug());
        this.version = version;
        this.callback = callback;
        loadModVersions();

        this.state.set(State.fromTitle(addon.getTitle()));
    }

    private void loadModVersions() {
        File versionJar = StringUtils.isNotBlank(version.getVersion())
                ? version.getProfile().getRepository().getVersionJar(version.getVersion())
                : null;

        setLoading(true);
        setFailed(false);

        Task.allOf(
                        Task.supplyAsync(() -> addon.getData().loadDependencies(repository)),
                        Task.supplyAsync(() -> {
                            Stream<RemoteMod.Version> versions = addon.getData().loadVersions(repository);
//                            if (StringUtils.isNotBlank(version.getVersion())) {
//                                Optional<String> gameVersion = GameVersion.minecraftVersion(versionJar);
//                                if (gameVersion.isPresent()) {
//                                    return sortVersions(
//                                            .filter(file -> file.getGameVersions().contains(gameVersion.get())));
//                                }
//                            }
                            return sortVersions(versions);
                        }))
                .whenComplete(Schedulers.javafx(), (result, exception) -> {
                    if (exception == null) {
                        @SuppressWarnings("unchecked")
                        List<RemoteMod> dependencies = (List<RemoteMod>) result.get(0);
                        @SuppressWarnings("unchecked")
                        SimpleMultimap<String, RemoteMod.Version> versions = (SimpleMultimap<String, RemoteMod.Version>) result.get(1);

                        this.dependencies = dependencies;
                        this.versions = versions;

                        loaded.set(true);
                        setFailed(false);
                    } else {
                        setFailed(true);
                    }
                    setLoading(false);
                }).start();
    }

    private SimpleMultimap<String, RemoteMod.Version> sortVersions(Stream<RemoteMod.Version> versions) {
        SimpleMultimap<String, RemoteMod.Version> classifiedVersions
                = new SimpleMultimap<String, RemoteMod.Version>(HashMap::new, ArrayList::new);
        versions.forEach(version -> {
            for (String gameVersion : version.getGameVersions()) {
                classifiedVersions.put(gameVersion, version);
            }
        });

        for (String gameVersion : classifiedVersions.keys()) {
            List<RemoteMod.Version> versionList = (List<RemoteMod.Version>) classifiedVersions.get(gameVersion);
            versionList.sort(Comparator.comparing(RemoteMod.Version::getDatePublished).reversed());
        }
        return classifiedVersions;
    }

    public RemoteMod getAddon() {
        return addon;
    }

    public Profile.ProfileVersion getVersion() {
        return version;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public void setLoading(boolean loading) {
        this.loading.set(loading);
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public boolean isFailed() {
        return failed.get();
    }

    public void setFailed(boolean failed) {
        this.failed.set(failed);
    }

    public BooleanProperty failedProperty() {
        return failed;
    }

    public void download(RemoteMod.Version file) {
        if (this.callback == null) {
            saveAs(file);
        } else {
            this.callback.download(version.getProfile(), version.getVersion(), file);
        }
    }

    public void saveAs(RemoteMod.Version file) {
        String extension = StringUtils.substringAfterLast(file.getFile().getFilename(), '.');

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.i18n("button.save_as"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.i18n("file"), "*." + extension));
        fileChooser.setInitialFileName(file.getFile().getFilename());
        File dest = fileChooser.showSaveDialog(Controllers.getStage());
        if (dest == null) {
            return;
        }

        Controllers.taskDialog(
                Task.composeAsync(() -> {
                    FileDownloadTask task = new FileDownloadTask(NetworkUtils.toURL(file.getFile().getUrl()), dest, file.getFile().getIntegrityCheck());
                    task.setName(file.getName());
                    return task;
                }),
                I18n.i18n("message.downloading"),
                TaskCancellationAction.NORMAL);
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new ModDownloadPageSkin(this);
    }

    public interface DownloadCallback {
        void download(Profile profile, @Nullable String version, RemoteMod.Version file);
    }

    private static class ModDownloadPageSkin extends SkinBase<DownloadPage> {

        protected ModDownloadPageSkin(DownloadPage control) {
            super(control);

            VBox pane = new VBox(8);
            pane.getStyleClass().add("gray-background");
            pane.setPadding(new Insets(10));
            ScrollPane scrollPane = new ScrollPane(pane);
            FXUtils.smoothScrolling(scrollPane);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);

            HBox descriptionPane = new HBox(8);
            descriptionPane.setAlignment(Pos.CENTER);
            pane.getChildren().add(descriptionPane);
            descriptionPane.getStyleClass().add("card-non-transparent");
            BorderPane.setMargin(descriptionPane, new Insets(11, 11, 0, 11));
            {
                ImageView imageView = new ImageView();
                if (StringUtils.isNotBlank(getSkinnable().addon.getIconUrl())) {
                    imageView.setImage(new Image(getSkinnable().addon.getIconUrl(), 40, 40, true, true, true));
                }
                descriptionPane.getChildren().add(FXUtils.limitingSize(imageView, 40, 40));

                TwoLineListItem content = new TwoLineListItem();
                HBox.setHgrow(content, Priority.ALWAYS);
                ModTranslations.Mod mod = getSkinnable().translations.getModByCurseForgeId(getSkinnable().addon.getSlug());
                content.setTitle(mod != null && I18n.getCurrentLocale().getLocale() == Locale.CHINA ? mod.getDisplayName() : getSkinnable().addon.getTitle());
                content.setSubtitle(getSkinnable().addon.getDescription());
                content.getTags().setAll(getSkinnable().addon.getCategories().stream()
                        .map(category -> getSkinnable().page.getLocalizedCategory(category))
                        .collect(Collectors.toList()));
                descriptionPane.getChildren().add(content);

                if (getSkinnable().mod != null) {
                    JFXHyperlink openMcmodButton = new JFXHyperlink(I18n.i18n("mods.mcmod"));
                    openMcmodButton.setExternalLink(getSkinnable().translations.getMcmodUrl(getSkinnable().mod));
                    descriptionPane.getChildren().add(openMcmodButton);

                    if (StringUtils.isNotBlank(getSkinnable().mod.getMcbbs())) {
                        JFXHyperlink openMcbbsButton = new JFXHyperlink(I18n.i18n("mods.mcbbs"));
                        openMcbbsButton.setExternalLink(ModManager.getMcbbsUrl(getSkinnable().mod.getMcbbs()));
                        descriptionPane.getChildren().add(openMcbbsButton);
                    }
                }

                JFXHyperlink openUrlButton = new JFXHyperlink(control.page.getLocalizedOfficialPage());
                openUrlButton.setExternalLink(getSkinnable().addon.getPageUrl());
                descriptionPane.getChildren().add(openUrlButton);
            }

            {
                ComponentList dependencyPane = new ComponentList();
                dependencyPane.getStyleClass().add("no-padding");

                FXUtils.onChangeAndOperate(control.loaded, loaded -> {
                    if (loaded) {
                        dependencyPane.getContent().setAll(control.dependencies.stream()
                                .map(dependency -> new DependencyModItem(getSkinnable().page, dependency, control.version, control.callback))
                                .collect(Collectors.toList()));
                    }
                });

                Node title = ComponentList.createComponentListTitle(I18n.i18n("mods.dependencies"));

                BooleanBinding show = Bindings.createBooleanBinding(() -> control.loaded.get() && !control.dependencies.isEmpty(), control.loaded);
                title.managedProperty().bind(show);
                title.visibleProperty().bind(show);
                dependencyPane.managedProperty().bind(show);
                dependencyPane.visibleProperty().bind(show);

                pane.getChildren().addAll(title, dependencyPane);
            }

            SpinnerPane spinnerPane = new SpinnerPane();
            VBox.setVgrow(spinnerPane, Priority.ALWAYS);
            pane.getChildren().add(spinnerPane);
            {
                spinnerPane.loadingProperty().bind(getSkinnable().loadingProperty());
                spinnerPane.failedReasonProperty().bind(Bindings.createStringBinding(() -> {
                    if (getSkinnable().isFailed()) {
                        return I18n.i18n("download.failed.refresh");
                    } else {
                        return null;
                    }
                }, getSkinnable().failedProperty()));
                spinnerPane.setOnFailedAction(e -> getSkinnable().loadModVersions());

                ComponentList list = new ComponentList();
                StackPane.setAlignment(list, Pos.TOP_CENTER);
                spinnerPane.setContent(list);

                FXUtils.onChangeAndOperate(control.loaded, loaded -> {
                    if (control.versions == null) return;

                    for (String gameVersion : control.versions.keys().stream()
                            .sorted(VersionNumber.VERSION_COMPARATOR.reversed())
                            .collect(Collectors.toList())) {
                        ComponentList sublist = new ComponentList(() ->
                                control.versions.get(gameVersion).stream()
                                        .map(version -> new ModItem(version, control))
                                        .collect(Collectors.toList()));
                        sublist.getStyleClass().add("no-padding");
                        sublist.setTitle(gameVersion);

                        list.getContent().add(sublist);
                    }
                });
            }

            getChildren().setAll(scrollPane);
        }
    }

    private static final class DependencyModItem extends StackPane {

        DependencyModItem(DownloadListPage page, RemoteMod addon, Profile.ProfileVersion version, DownloadCallback callback) {
            HBox pane = new HBox(8);
            pane.setPadding(new Insets(8));
            pane.setAlignment(Pos.CENTER_LEFT);
            TwoLineListItem content = new TwoLineListItem();
            HBox.setHgrow(content, Priority.ALWAYS);
            ImageView imageView = new ImageView();
            pane.getChildren().setAll(FXUtils.limitingSize(imageView, 40, 40), content);

            RipplerContainer container = new RipplerContainer(pane);
            container.setOnMouseClicked(e -> Controllers.navigate(new DownloadPage(page, addon, version, callback)));
            getChildren().setAll(container);

            ModTranslations.Mod mod = ModTranslations.getTranslationsByRepositoryType(page.repository.getType()).getModByCurseForgeId(addon.getSlug());
            content.setTitle(mod != null && I18n.getCurrentLocale().getLocale() == Locale.CHINA ? mod.getDisplayName() : addon.getTitle());
            content.setSubtitle(addon.getDescription());
            content.getTags().setAll(addon.getCategories().stream()
                    .map(page::getLocalizedCategory)
                    .collect(Collectors.toList()));

            if (StringUtils.isNotBlank(addon.getIconUrl())) {
                imageView.setImage(new Image(addon.getIconUrl(), 40, 40, true, true, true));
            }
        }
    }

    private static final class ModItem extends StackPane {

        ModItem(RemoteMod.Version dataItem, DownloadPage selfPage) {
            HBox pane = new HBox(8);
            pane.setPadding(new Insets(8));
            pane.setAlignment(Pos.CENTER_LEFT);
            TwoLineListItem content = new TwoLineListItem();
            StackPane graphicPane = new StackPane();
            JFXButton saveAsButton = new JFXButton();

            RipplerContainer container = new RipplerContainer(pane);
            container.setOnMouseClicked(e -> selfPage.download(dataItem));
            getChildren().setAll(container);

            saveAsButton.getStyleClass().add("toggle-icon4");
            saveAsButton.setGraphic(SVG.contentSaveMoveOutline(Theme.blackFillBinding(), -1, -1));

            HBox.setHgrow(content, Priority.ALWAYS);
            pane.getChildren().setAll(graphicPane, content, saveAsButton);

            content.setTitle(dataItem.getName());
            content.setSubtitle(FORMATTER.format(dataItem.getDatePublished().toInstant()));
            saveAsButton.setOnAction(e -> selfPage.saveAs(dataItem));

            switch (dataItem.getVersionType()) {
                case Release:
                    graphicPane.getChildren().setAll(SVG.releaseCircleOutline(Theme.blackFillBinding(), 24, 24));
                    content.getTags().add(I18n.i18n("version.game.release"));
                    break;
                case Beta:
                    graphicPane.getChildren().setAll(SVG.betaCircleOutline(Theme.blackFillBinding(), 24, 24));
                    content.getTags().add(I18n.i18n("version.game.snapshot"));
                    break;
                case Alpha:
                    graphicPane.getChildren().setAll(SVG.alphaCircleOutline(Theme.blackFillBinding(), 24, 24));
                    content.getTags().add(I18n.i18n("version.game.snapshot"));
                    break;
            }

            // Workaround for https://github.com/huanghongxun/HMCL/issues/2129
            this.setMinHeight(50);
        }
    }
}
