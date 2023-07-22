package cn.ChengZhiYa.BaiShenLauncher.ui.download;

import cn.ChengZhiYa.BaiShenLauncher.download.*;
import cn.ChengZhiYa.BaiShenLauncher.download.game.GameRemoteVersion;
import cn.ChengZhiYa.BaiShenLauncher.mod.RemoteMod;
import cn.ChengZhiYa.BaiShenLauncher.mod.curse.CurseForgeRemoteModRepository;
import cn.ChengZhiYa.BaiShenLauncher.setting.DownloadProviders;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profiles;
import cn.ChengZhiYa.BaiShenLauncher.task.FileDownloadTask;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.ui.WeakListenerHolder;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.ContainerAnimations;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.TransitionPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.AdvancedListBox;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.MessageDialogPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.TabControl;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.TabHeader;
import cn.ChengZhiYa.BaiShenLauncher.ui.decorator.DecoratorAnimatedPage;
import cn.ChengZhiYa.BaiShenLauncher.ui.decorator.DecoratorPage;
import cn.ChengZhiYa.BaiShenLauncher.ui.versions.*;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.Navigation;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardController;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardProvider;
import cn.ChengZhiYa.BaiShenLauncher.util.TaskCancellationAction;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.io.NetworkUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.OperatingSystem;
import com.jfoenix.controls.JFXButton;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.runInFX;
import static cn.ChengZhiYa.BaiShenLauncher.ui.versions.VersionPage.wrap;

public class DownloadPage extends DecoratorAnimatedPage implements DecoratorPage {
    private final ReadOnlyObjectWrapper<DecoratorPage.State> state = new ReadOnlyObjectWrapper<>(DecoratorPage.State.fromTitle(I18n.i18n("download"), -1));
    private final TabHeader tab;
    private final TabHeader.Tab<VersionsPage> newGameTab = new TabHeader.Tab<>("newGameTab");
    private final TabHeader.Tab<DownloadListPage> modTab = new TabHeader.Tab<>("modTab");
    private final TabHeader.Tab<DownloadListPage> modpackTab = new TabHeader.Tab<>("modpackTab");
    private final TabHeader.Tab<DownloadListPage> resourcePackTab = new TabHeader.Tab<>("resourcePackTab");
    private final TabHeader.Tab<DownloadListPage> customizationTab = new TabHeader.Tab<>("customizationTab");
    private final TabHeader.Tab<DownloadListPage> worldTab = new TabHeader.Tab<>("worldTab");
    private final TransitionPane transitionPane = new TransitionPane();
    private final DownloadNavigator versionPageNavigator = new DownloadNavigator();

    private WeakListenerHolder listenerHolder;

    public DownloadPage() {
        newGameTab.setNodeSupplier(loadVersionFor(() -> new VersionsPage(versionPageNavigator, I18n.i18n("install.installer.choose", I18n.i18n("install.installer.game")), "", DownloadProviders.getDownloadProvider(),
                "game", versionPageNavigator::onGameSelected)));
        modpackTab.setNodeSupplier(loadVersionFor(() -> {
            ModpackDownloadListPage page = new ModpackDownloadListPage(Versions::downloadModpackImpl, false);

            JFXButton installLocalModpackButton = FXUtils.newRaisedButton(I18n.i18n("install.modpack"));
            installLocalModpackButton.setOnAction(e -> Versions.importModpack());

            page.getActions().add(installLocalModpackButton);
            return page;
        }));
        modTab.setNodeSupplier(loadVersionFor(() -> new ModDownloadListPage((profile, version, file) -> download(profile, version, file, "mods"), true)));
        resourcePackTab.setNodeSupplier(loadVersionFor(() -> new DownloadListPage(CurseForgeRemoteModRepository.RESOURCE_PACKS, (profile, version, file) -> download(profile, version, file, "resourcepacks"), true)));
        customizationTab.setNodeSupplier(loadVersionFor(() -> new DownloadListPage(CurseForgeRemoteModRepository.CUSTOMIZATIONS)));
        worldTab.setNodeSupplier(loadVersionFor(() -> new DownloadListPage(CurseForgeRemoteModRepository.WORLDS)));
        tab = new TabHeader(newGameTab, modpackTab, modTab, resourcePackTab, worldTab);

        Profiles.registerVersionsListener(this::loadVersions);

        tab.select(newGameTab);
        FXUtils.onChangeAndOperate(tab.getSelectionModel().selectedItemProperty(), newValue -> {
            transitionPane.setContent(newValue.getNode(), ContainerAnimations.FADE.getAnimationProducer());
        });

        {
            AdvancedListBox sideBar = new AdvancedListBox()
                    .startCategory(I18n.i18n("download.game"))
                    .addNavigationDrawerItem(item -> {
                        item.setTitle(I18n.i18n("game"));
                        item.setLeftGraphic(wrap(SVG::gamepad));
                        item.activeProperty().bind(tab.getSelectionModel().selectedItemProperty().isEqualTo(newGameTab));
                        item.setOnAction(e -> tab.select(newGameTab));
                    })
                    .addNavigationDrawerItem(settingsItem -> {
                        settingsItem.setTitle(I18n.i18n("modpack"));
                        settingsItem.setLeftGraphic(wrap(SVG::pack));
                        settingsItem.activeProperty().bind(tab.getSelectionModel().selectedItemProperty().isEqualTo(modpackTab));
                        settingsItem.setOnAction(e -> tab.select(modpackTab));
                    })
                    .startCategory(I18n.i18n("download.content"))
                    .addNavigationDrawerItem(item -> {
                        item.setTitle(I18n.i18n("mods"));
                        item.setLeftGraphic(wrap(SVG::puzzle));
                        item.activeProperty().bind(tab.getSelectionModel().selectedItemProperty().isEqualTo(modTab));
                        item.setOnAction(e -> tab.select(modTab));
                    })
                    .addNavigationDrawerItem(item -> {
                        item.setTitle(I18n.i18n("resourcepack"));
                        item.setLeftGraphic(wrap(SVG::textureBox));
                        item.activeProperty().bind(tab.getSelectionModel().selectedItemProperty().isEqualTo(resourcePackTab));
                        item.setOnAction(e -> selectTabIfCurseForgeAvailable(resourcePackTab));
                    })
//                    .addNavigationDrawerItem(item -> {
//                        item.setTitle(i18n("download.curseforge.customization"));
//                        item.setLeftGraphic(wrap(SVG::script));
//                        item.activeProperty().bind(tab.getSelectionModel().selectedItemProperty().isEqualTo(customizationTab));
//                        item.setOnAction(e -> selectTabIfCurseForgeAvailable(customizationTab));
//                    })
                    .addNavigationDrawerItem(item -> {
                        item.setTitle(I18n.i18n("world"));
                        item.setLeftGraphic(wrap(SVG::earth));
                        item.activeProperty().bind(tab.getSelectionModel().selectedItemProperty().isEqualTo(worldTab));
                        item.setOnAction(e -> selectTabIfCurseForgeAvailable(worldTab));
                    });
            FXUtils.setLimitWidth(sideBar, 200);
            setLeft(sideBar);
        }

        setCenter(transitionPane);
    }

    private static <T extends Node> Supplier<T> loadVersionFor(Supplier<T> nodeSupplier) {
        return () -> {
            T node = nodeSupplier.get();
            if (node instanceof VersionPage.VersionLoadable) {
                ((VersionPage.VersionLoadable) node).loadVersion(Profiles.getSelectedProfile(), null);
            }
            return node;
        };
    }

    private static void download(Profile profile, @Nullable String version, RemoteMod.Version file, String subdirectoryName) {
        if (version == null) version = profile.getSelectedVersion();

        Path runDirectory = profile.getRepository().hasVersion(version) ? profile.getRepository().getRunDirectory(version).toPath() : profile.getRepository().getBaseDirectory().toPath();

        Controllers.prompt(I18n.i18n("archive.name"), (result, resolve, reject) -> {
            if (!OperatingSystem.isNameValid(result)) {
                reject.accept(I18n.i18n("install.new_game.malformed"));
                return;
            }
            Path dest = runDirectory.resolve(subdirectoryName).resolve(result);

            Controllers.taskDialog(Task.composeAsync(() -> {
                FileDownloadTask task = new FileDownloadTask(NetworkUtils.toURL(file.getFile().getUrl()), dest.toFile());
                task.setName(file.getName());
                return task;
            }).whenComplete(Schedulers.javafx(), exception -> {
                if (exception != null) {
                    if (exception instanceof CancellationException) {
                        Controllers.showToast(I18n.i18n("message.cancelled"));
                    } else {
                        Controllers.dialog(DownloadProviders.localizeErrorMessage(exception), I18n.i18n("install.failed.downloading"), MessageDialogPane.MessageType.ERROR);
                    }
                } else {
                    Controllers.showToast(I18n.i18n("install.success"));
                }
            }), I18n.i18n("message.downloading"), TaskCancellationAction.NORMAL);

            resolve.run();
        }, file.getFile().getFilename());

    }

    private void selectTabIfCurseForgeAvailable(TabControl.Tab<?> newTab) {
        if (CurseForgeRemoteModRepository.isAvailable())
            tab.select(newTab);
        else
            Controllers.dialog(I18n.i18n("download.curseforge.unavailable"));
    }

    private void loadVersions(Profile profile) {
        listenerHolder = new WeakListenerHolder();
        runInFX(() -> {
            if (profile == Profiles.getSelectedProfile()) {
                listenerHolder.add(FXUtils.onWeakChangeAndOperate(profile.selectedVersionProperty(), version -> {
                    if (modTab.isInitialized()) {
                        modTab.getNode().loadVersion(profile, null);
                    }
                    if (modpackTab.isInitialized()) {
                        modpackTab.getNode().loadVersion(profile, null);
                    }
                    if (resourcePackTab.isInitialized()) {
                        resourcePackTab.getNode().loadVersion(profile, null);
                    }
                    if (customizationTab.isInitialized()) {
                        customizationTab.getNode().loadVersion(profile, null);
                    }
                    if (worldTab.isInitialized()) {
                        worldTab.getNode().loadVersion(profile, null);
                    }
                }));
            }
        });
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }

    public void showGameDownloads() {
        tab.select(newGameTab);
    }

    public void showModDownloads() {
        tab.select(modTab);
    }

    public void showWorldDownloads() {
        tab.select(worldTab);
    }

    private static final class DownloadNavigator implements Navigation {
        private final Map<String, Object> settings = new HashMap<>();

        @Override
        public void onStart() {

        }

        @Override
        public void onNext() {

        }

        @Override
        public void onPrev(boolean cleanUp) {
        }

        @Override
        public boolean canPrev() {
            return false;
        }

        @Override
        public void onFinish() {

        }

        @Override
        public void onEnd() {

        }

        @Override
        public void onCancel() {

        }

        @Override
        public Map<String, Object> getSettings() {
            return settings;
        }

        public void onGameSelected() {
            Profile profile = Profiles.getSelectedProfile();
            if (profile.getRepository().isLoaded()) {
                Controllers.getDecorator().startWizard(new VanillaInstallWizardProvider(profile, (GameRemoteVersion) settings.get("game")), I18n.i18n("install.new_game"));
            }
        }

    }

    private static class VanillaInstallWizardProvider implements WizardProvider {
        public static final String PROFILE = "PROFILE";
        private final Profile profile;
        private final DefaultDependencyManager dependencyManager;
        private final DownloadProvider downloadProvider;
        private final GameRemoteVersion gameVersion;

        public VanillaInstallWizardProvider(Profile profile, GameRemoteVersion gameVersion) {
            this.profile = profile;
            this.gameVersion = gameVersion;
            this.downloadProvider = DownloadProviders.getDownloadProvider();
            this.dependencyManager = profile.getDependency(downloadProvider);
        }

        @Override
        public void start(Map<String, Object> settings) {
            settings.put(PROFILE, profile);
            settings.put(LibraryAnalyzer.LibraryType.MINECRAFT.getPatchId(), gameVersion);
        }

        private Task<Void> finishVersionDownloadingAsync(Map<String, Object> settings) {
            GameBuilder builder = dependencyManager.gameBuilder();

            String name = (String) settings.get("name");
            builder.name(name);
            builder.gameVersion(((RemoteVersion) settings.get(LibraryAnalyzer.LibraryType.MINECRAFT.getPatchId())).getGameVersion());

            for (Map.Entry<String, Object> entry : settings.entrySet())
                if (!LibraryAnalyzer.LibraryType.MINECRAFT.getPatchId().equals(entry.getKey()) && entry.getValue() instanceof RemoteVersion)
                    builder.version((RemoteVersion) entry.getValue());

            return builder.buildAsync().whenComplete(any -> profile.getRepository().refreshVersions())
                    .thenRunAsync(Schedulers.javafx(), () -> profile.setSelectedVersion(name));
        }

        @Override
        public Object finish(Map<String, Object> settings) {
            settings.put("title", I18n.i18n("install.new_game"));
            settings.put("success_message", I18n.i18n("install.success"));
            settings.put("failure_callback", (FailureCallback) (settings1, exception, next) -> UpdateInstallerWizardProvider.alertFailureMessage(exception, next));

            return finishVersionDownloadingAsync(settings);
        }

        @Override
        public Node createPage(WizardController controller, int step, Map<String, Object> settings) {
            switch (step) {
                case 0:
                    return new InstallersPage(controller, profile.getRepository(), ((RemoteVersion) controller.getSettings().get("game")).getGameVersion(), downloadProvider);
                default:
                    throw new IllegalStateException("error step " + step + ", settings: " + settings + ", pages: " + controller.getPages());
            }
        }

        @Override
        public boolean cancel() {
            return true;
        }
    }
}
