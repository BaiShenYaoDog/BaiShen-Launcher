package cn.ChengZhiYa.BaiShenLauncher.ui.versions;

import cn.ChengZhiYa.BaiShenLauncher.event.EventBus;
import cn.ChengZhiYa.BaiShenLauncher.event.EventPriority;
import cn.ChengZhiYa.BaiShenLauncher.event.RefreshedVersionsEvent;
import cn.ChengZhiYa.BaiShenLauncher.game.GameRepository;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.ui.WeakListenerHolder;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.ContainerAnimations;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.TransitionPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.*;
import cn.ChengZhiYa.BaiShenLauncher.ui.decorator.DecoratorAnimatedPage;
import cn.ChengZhiYa.BaiShenLauncher.ui.decorator.DecoratorPage;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import com.jfoenix.controls.JFXPopup;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.Optional;
import java.util.function.Supplier;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.runInFX;

public class VersionPage extends DecoratorAnimatedPage implements DecoratorPage {
    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>();
    private final TabHeader tab;
    private final TabHeader.Tab<VersionSettingsPage> versionSettingsTab = new TabHeader.Tab<>("versionSettingsTab");
    private final TabHeader.Tab<ModListPage> modListTab = new TabHeader.Tab<>("modListTab");
    private final TabHeader.Tab<InstallerListPage> installerListTab = new TabHeader.Tab<>("installerListTab");
    private final TabHeader.Tab<WorldListPage> worldListTab = new TabHeader.Tab<>("worldList");
    private final TransitionPane transitionPane = new TransitionPane();
    private final BooleanProperty currentVersionUpgradable = new SimpleBooleanProperty();
    private final ObjectProperty<Profile.ProfileVersion> version = new SimpleObjectProperty<>();
    private final WeakListenerHolder listenerHolder = new WeakListenerHolder();

    private String preferredVersionName = null;

    {
        versionSettingsTab.setNodeSupplier(loadVersionFor(() -> new VersionSettingsPage(false)));
        modListTab.setNodeSupplier(loadVersionFor(ModListPage::new));
        installerListTab.setNodeSupplier(loadVersionFor(InstallerListPage::new));
        worldListTab.setNodeSupplier(loadVersionFor(WorldListPage::new));

        tab = new TabHeader(versionSettingsTab, modListTab, installerListTab, worldListTab);

        addEventHandler(Navigator.NavigationEvent.NAVIGATED, this::onNavigated);

        tab.select(versionSettingsTab);
        FXUtils.onChangeAndOperate(tab.getSelectionModel().selectedItemProperty(), newValue -> {
            transitionPane.setContent(newValue.getNode(), ContainerAnimations.FADE.getAnimationProducer());
        });

        listenerHolder.add(EventBus.EVENT_BUS.channel(RefreshedVersionsEvent.class).registerWeak(event -> checkSelectedVersion(), EventPriority.HIGHEST));
    }

    public static Node wrap(Node node) {
        StackPane stackPane = new StackPane();
        stackPane.setAlignment(Pos.CENTER);
        FXUtils.setLimitWidth(stackPane, 30);
        FXUtils.setLimitHeight(stackPane, 20);
        stackPane.setPadding(new Insets(0, 0, 0, 0));
        stackPane.getChildren().setAll(node);
        return stackPane;
    }

    public static Node wrap(SVG.SVGIcon svg) {
        return wrap(svg.createIcon(null, 20, 20));
    }

    private void checkSelectedVersion() {
        runInFX(() -> {
            if (this.version.get() == null) return;
            GameRepository repository = this.version.get().getProfile().getRepository();
            if (!repository.hasVersion(this.version.get().getVersion())) {
                if (preferredVersionName != null) {
                    loadVersion(preferredVersionName, this.version.get().getProfile());
                } else {
                    fireEvent(new PageCloseEvent());
                }
            }
        });
    }

    private <T extends Node> Supplier<T> loadVersionFor(Supplier<T> nodeSupplier) {
        return () -> {
            T node = nodeSupplier.get();
            if (version.get() != null) {
                if (node instanceof VersionPage.VersionLoadable) {
                    ((VersionLoadable) node).loadVersion(version.get().getProfile(), version.get().getVersion());
                }
            }
            return node;
        };
    }

    public void setVersion(String version, Profile profile) {
        this.version.set(new Profile.ProfileVersion(profile, version));
    }

    public void loadVersion(String version, Profile profile) {
        // If we jumped to game list page and deleted this version
        // and back to this page, we should return to main page.
        if (this.version.get() != null && (!getProfile().getRepository().isLoaded() ||
                !getProfile().getRepository().hasVersion(version))) {
            Platform.runLater(() -> fireEvent(new PageCloseEvent()));
            return;
        }

        setVersion(version, profile);
        preferredVersionName = version;

        if (versionSettingsTab.isInitialized())
            versionSettingsTab.getNode().loadVersion(profile, version);
        if (modListTab.isInitialized())
            modListTab.getNode().loadVersion(profile, version);
        if (installerListTab.isInitialized())
            installerListTab.getNode().loadVersion(profile, version);
        if (worldListTab.isInitialized())
            worldListTab.getNode().loadVersion(profile, version);
        currentVersionUpgradable.set(profile.getRepository().isModpack(version));
    }

    private void onNavigated(Navigator.NavigationEvent event) {
        if (this.version.get() == null)
            throw new IllegalStateException();

        // If we jumped to game list page and deleted this version
        // and back to this page, we should return to main page.
        if (!getProfile().getRepository().isLoaded() ||
                !getProfile().getRepository().hasVersion(getVersion())) {
            Platform.runLater(() -> fireEvent(new PageCloseEvent()));
            return;
        }

        loadVersion(getVersion(), getProfile());
    }

    private void onBrowse(String sub) {
        FXUtils.openFolder(new File(getProfile().getRepository().getRunDirectory(getVersion()), sub));
    }

    private void redownloadAssetIndex() {
        Versions.updateGameAssets(getProfile(), getVersion());
    }

    private void clearLibraries() {
        FileUtils.deleteDirectoryQuietly(new File(getProfile().getRepository().getBaseDirectory(), "libraries"));
    }

    private void clearAssets() {
        FileUtils.deleteDirectoryQuietly(new File(getProfile().getRepository().getBaseDirectory(), "assets"));
    }

    private void clearJunkFiles() {
        Versions.cleanVersion(getProfile(), getVersion());
    }

    private void testGame() {
        Versions.testGame(getProfile(), getVersion());
    }

    private void updateGame() {
        Versions.updateVersion(getProfile(), getVersion());
    }

    private void generateLaunchScript() {
        Versions.generateLaunchScript(getProfile(), getVersion());
    }

    private void export() {
        Versions.exportVersion(getProfile(), getVersion());
    }

    private void rename() {
        Versions.renameVersion(getProfile(), getVersion())
                .thenApply(newVersionName -> this.preferredVersionName = newVersionName);
    }

    private void remove() {
        Versions.deleteVersion(getProfile(), getVersion());
    }

    private void duplicate() {
        Versions.duplicateVersion(getProfile(), getVersion());
    }

    public Profile getProfile() {
        return Optional.ofNullable(version.get()).map(Profile.ProfileVersion::getProfile).orElse(null);
    }

    public String getVersion() {
        return Optional.ofNullable(version.get()).map(Profile.ProfileVersion::getVersion).orElse(null);
    }

    @Override
    protected Skin createDefaultSkin() {
        return new Skin(this);
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }

    public interface VersionLoadable {
        void loadVersion(Profile profile, String version);
    }

    public static class Skin extends DecoratorAnimatedPageSkin<VersionPage> {

        /**
         * Constructor for all SkinBase instances.
         *
         * @param control The control for which this Skin should attach to.
         */
        protected Skin(VersionPage control) {
            super(control);

            {
                BorderPane left = new BorderPane();
                FXUtils.setLimitWidth(left, 200);
                setLeft(left);

                AdvancedListItem versionSettingsItem = new AdvancedListItem();
                versionSettingsItem.getStyleClass().add("navigation-drawer-item");
                versionSettingsItem.setTitle(I18n.i18n("settings.game"));
                versionSettingsItem.setLeftGraphic(wrap(SVG::gearOutline));
                versionSettingsItem.setActionButtonVisible(false);
                versionSettingsItem.activeProperty().bind(control.tab.getSelectionModel().selectedItemProperty().isEqualTo(control.versionSettingsTab));
                versionSettingsItem.setOnAction(e -> control.tab.select(control.versionSettingsTab));

                AdvancedListItem modListItem = new AdvancedListItem();
                modListItem.getStyleClass().add("navigation-drawer-item");
                modListItem.setTitle(I18n.i18n("mods.manage"));
                modListItem.setLeftGraphic(wrap(SVG::puzzle));
                modListItem.setActionButtonVisible(false);
                modListItem.activeProperty().bind(control.tab.getSelectionModel().selectedItemProperty().isEqualTo(control.modListTab));
                modListItem.setOnAction(e -> control.tab.select(control.modListTab));

                AdvancedListItem installerListItem = new AdvancedListItem();
                installerListItem.getStyleClass().add("navigation-drawer-item");
                installerListItem.setTitle(I18n.i18n("settings.tabs.installers"));
                installerListItem.setLeftGraphic(wrap(SVG::cube));
                installerListItem.setActionButtonVisible(false);
                installerListItem.activeProperty().bind(control.tab.getSelectionModel().selectedItemProperty().isEqualTo(control.installerListTab));
                installerListItem.setOnAction(e -> control.tab.select(control.installerListTab));

                AdvancedListItem worldListItem = new AdvancedListItem();
                worldListItem.getStyleClass().add("navigation-drawer-item");
                worldListItem.setTitle(I18n.i18n("world.manage"));
                worldListItem.setLeftGraphic(wrap(SVG::earth));
                worldListItem.setActionButtonVisible(false);
                worldListItem.activeProperty().bind(control.tab.getSelectionModel().selectedItemProperty().isEqualTo(control.worldListTab));
                worldListItem.setOnAction(e -> control.tab.select(control.worldListTab));

                AdvancedListBox sideBar = new AdvancedListBox()
                        .add(versionSettingsItem)
                        .add(modListItem)
                        .add(installerListItem)
                        .add(worldListItem);
                VBox.setVgrow(sideBar, Priority.ALWAYS);

                PopupMenu browseList = new PopupMenu();
                JFXPopup browsePopup = new JFXPopup(browseList);
                browseList.getContent().setAll(
                        new IconedMenuItem(FXUtils.limitingSize(SVG.gamepad(Theme.blackFillBinding(), 14, 14), 14, 14), I18n.i18n("folder.game"), FXUtils.withJFXPopupClosing(() -> control.onBrowse(""), browsePopup)),
                        new IconedMenuItem(FXUtils.limitingSize(SVG.puzzle(Theme.blackFillBinding(), 14, 14), 14, 14), I18n.i18n("folder.mod"), FXUtils.withJFXPopupClosing(() -> control.onBrowse("mods"), browsePopup)),
                        new IconedMenuItem(FXUtils.limitingSize(SVG.gearOutline(Theme.blackFillBinding(), 14, 14), 14, 14), I18n.i18n("folder.config"), FXUtils.withJFXPopupClosing(() -> control.onBrowse("config"), browsePopup)),
                        new IconedMenuItem(FXUtils.limitingSize(SVG.texture(Theme.blackFillBinding(), 14, 14), 14, 14), I18n.i18n("folder.resourcepacks"), FXUtils.withJFXPopupClosing(() -> control.onBrowse("resourcepacks"), browsePopup)),
                        new IconedMenuItem(FXUtils.limitingSize(SVG.monitorScreenshot(Theme.blackFillBinding(), 14, 14), 14, 14), I18n.i18n("folder.screenshots"), FXUtils.withJFXPopupClosing(() -> control.onBrowse("screenshots"), browsePopup)),
                        new IconedMenuItem(FXUtils.limitingSize(SVG.earth(Theme.blackFillBinding(), 14, 14), 14, 14), I18n.i18n("folder.saves"), FXUtils.withJFXPopupClosing(() -> control.onBrowse("saves"), browsePopup))
                );

                PopupMenu managementList = new PopupMenu();
                JFXPopup managementPopup = new JFXPopup(managementList);
                managementList.getContent().setAll(
                        new IconedMenuItem(FXUtils.limitingSize(SVG.script(Theme.blackFillBinding(), 14, 14), 14, 14), I18n.i18n("version.launch_script"), FXUtils.withJFXPopupClosing(control::generateLaunchScript, managementPopup)),
                        new MenuSeparator(),
                        new IconedMenuItem(FXUtils.limitingSize(SVG.pencil(Theme.blackFillBinding(), 14, 14), 14, 14), I18n.i18n("version.manage.rename"), FXUtils.withJFXPopupClosing(control::rename, managementPopup)),
                        new IconedMenuItem(FXUtils.limitingSize(SVG.copy(Theme.blackFillBinding(), 14, 14), 14, 14), I18n.i18n("version.manage.duplicate"), FXUtils.withJFXPopupClosing(control::duplicate, managementPopup)),
                        new IconedMenuItem(FXUtils.limitingSize(SVG.deleteOutline(Theme.blackFillBinding(), 14, 14), 14, 14), I18n.i18n("version.manage.remove"), FXUtils.withJFXPopupClosing(control::remove, managementPopup)),
                        new IconedMenuItem(FXUtils.limitingSize(SVG.export(Theme.blackFillBinding(), 14, 14), 14, 14), I18n.i18n("modpack.export"), FXUtils.withJFXPopupClosing(control::export, managementPopup)),
                        new MenuSeparator(),
                        new IconedMenuItem(null, I18n.i18n("version.manage.redownload_assets_index"), FXUtils.withJFXPopupClosing(control::redownloadAssetIndex, managementPopup)),
                        new IconedMenuItem(null, I18n.i18n("version.manage.remove_assets"), FXUtils.withJFXPopupClosing(control::clearAssets, managementPopup)),
                        new IconedMenuItem(null, I18n.i18n("version.manage.remove_libraries"), FXUtils.withJFXPopupClosing(control::clearLibraries, managementPopup)),
                        new IconedMenuItem(null, I18n.i18n("version.manage.clean"), FXUtils.withJFXPopupClosing(control::clearJunkFiles, managementPopup)).addTooltip(I18n.i18n("version.manage.clean.tooltip"))
                );

                AdvancedListBox toolbar = new AdvancedListBox()
                        .addNavigationDrawerItem(upgradeItem -> {
                            upgradeItem.setTitle(I18n.i18n("version.update"));
                            upgradeItem.setLeftGraphic(wrap(SVG::update));
                            upgradeItem.visibleProperty().bind(control.currentVersionUpgradable);
                            upgradeItem.setOnAction(e -> control.updateGame());
                        })
                        .addNavigationDrawerItem(browseMenuItem -> {
                            browseMenuItem.setTitle(I18n.i18n("settings.game.exploration"));
                            browseMenuItem.setLeftGraphic(wrap(SVG::folderOutline));
                            browseMenuItem.setOnAction(e -> browsePopup.show(browseMenuItem, JFXPopup.PopupVPosition.BOTTOM, JFXPopup.PopupHPosition.LEFT, browseMenuItem.getWidth(), 0));
                        })
                        .addNavigationDrawerItem(managementItem -> {
                            managementItem.setTitle(I18n.i18n("settings.game.management"));
                            managementItem.setLeftGraphic(wrap(SVG::wrenchOutline));
                            managementItem.setOnAction(e -> managementPopup.show(managementItem, JFXPopup.PopupVPosition.BOTTOM, JFXPopup.PopupHPosition.LEFT, managementItem.getWidth(), 0));
                        });
                toolbar.getStyleClass().add("advanced-list-box-clear-padding");
                FXUtils.setLimitHeight(toolbar, 40 * 4 + 12 * 2);

                setLeft(sideBar, toolbar);
            }

            control.state.bind(Bindings.createObjectBinding(() ->
                            State.fromTitle(I18n.i18n("version.manage.manage.title", getSkinnable().getVersion()), -1),
                    getSkinnable().version));

            //control.transitionPane.getStyleClass().add("gray-background");
            //FXUtils.setOverflowHidden(control.transitionPane, 8);
            setCenter(control.transitionPane);
        }
    }
}
