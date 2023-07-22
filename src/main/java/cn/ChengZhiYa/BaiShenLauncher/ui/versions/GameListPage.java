package cn.ChengZhiYa.BaiShenLauncher.ui.versions;

import cn.ChengZhiYa.BaiShenLauncher.game.HMCLGameRepository;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profiles;
import cn.ChengZhiYa.BaiShenLauncher.ui.*;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.AdvancedListBox;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.AdvancedListItem;
import cn.ChengZhiYa.BaiShenLauncher.ui.decorator.DecoratorAnimatedPage;
import cn.ChengZhiYa.BaiShenLauncher.ui.decorator.DecoratorPage;
import cn.ChengZhiYa.BaiShenLauncher.ui.profile.ProfileListItem;
import cn.ChengZhiYa.BaiShenLauncher.ui.profile.ProfilePage;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.javafx.ExtendedProperties;
import cn.ChengZhiYa.BaiShenLauncher.util.javafx.MappedObservableList;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.runInFX;

public class GameListPage extends DecoratorAnimatedPage implements DecoratorPage {
    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(State.fromTitle(I18n.i18n("version.manage")));
    private final ListProperty<Profile> profiles = new SimpleListProperty<>(FXCollections.observableArrayList());
    @SuppressWarnings("FieldCanBeLocal")
    private final ObservableList<ProfileListItem> profileListItems;
    private final ObjectProperty<Profile> selectedProfile;

    private ToggleGroup toggleGroup;

    public GameListPage() {
        profileListItems = MappedObservableList.create(profilesProperty(), profile -> {
            ProfileListItem item = new ProfileListItem(profile);
            FXUtils.setLimitWidth(item, 200);
            return item;
        });
        selectedProfile = ExtendedProperties.createSelectedItemPropertyFor(profileListItems, Profile.class);

        GameList gameList = new GameList();

        {
            ScrollPane pane = new ScrollPane();
            VBox.setVgrow(pane, Priority.ALWAYS);
            {
                AdvancedListItem addProfileItem = new AdvancedListItem();
                addProfileItem.getStyleClass().add("navigation-drawer-item");
                addProfileItem.setTitle(I18n.i18n("profile.new"));
                addProfileItem.setActionButtonVisible(false);
                addProfileItem.setLeftGraphic(VersionPage.wrap(SVG::plusCircleOutline));
                addProfileItem.setOnAction(e -> Controllers.navigate(new ProfilePage(null)));

                pane.setFitToWidth(true);
                VBox wrapper = new VBox();
                wrapper.getStyleClass().add("advanced-list-box-content");
                VBox box = new VBox();
                box.setFillWidth(true);
                Bindings.bindContent(box.getChildren(), profileListItems);
                wrapper.getChildren().setAll(box, addProfileItem);
                pane.setContent(wrapper);
            }

            AdvancedListBox bottomLeftCornerList = new AdvancedListBox()
                    .addNavigationDrawerItem(installNewGameItem -> {
                        installNewGameItem.setTitle(I18n.i18n("install.new_game"));
                        installNewGameItem.setLeftGraphic(VersionPage.wrap(SVG::plusCircleOutline));
                        installNewGameItem.setOnAction(e -> Versions.addNewGame());
                    })
                    .addNavigationDrawerItem(installModpackItem -> {
                        installModpackItem.setTitle(I18n.i18n("install.modpack"));
                        installModpackItem.setLeftGraphic(VersionPage.wrap(SVG::pack));
                        installModpackItem.setOnAction(e -> Versions.importModpack());
                    })
                    .addNavigationDrawerItem(refreshItem -> {
                        refreshItem.setTitle(I18n.i18n("button.refresh"));
                        refreshItem.setLeftGraphic(VersionPage.wrap(SVG::refresh));
                        refreshItem.setOnAction(e -> gameList.refreshList());
                    })
                    .addNavigationDrawerItem(globalManageItem -> {
                        globalManageItem.setTitle(I18n.i18n("settings.type.global.manage"));
                        globalManageItem.setLeftGraphic(VersionPage.wrap(SVG::gearOutline));
                        globalManageItem.setOnAction(e -> modifyGlobalGameSettings());
                    });
            FXUtils.setLimitHeight(bottomLeftCornerList, 40 * 4 + 12 * 2);
            setLeft(pane, bottomLeftCornerList);
        }

        setCenter(gameList);
    }

    public ObjectProperty<Profile> selectedProfileProperty() {
        return selectedProfile;
    }

    public ObservableList<Profile> getProfiles() {
        return profiles.get();
    }

    public void setProfiles(ObservableList<Profile> profiles) {
        this.profiles.set(profiles);
    }

    public ListProperty<Profile> profilesProperty() {
        return profiles;
    }

    public void modifyGlobalGameSettings() {
        Versions.modifyGlobalSettings(Profiles.getSelectedProfile());
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }

    private class GameList extends ListPageBase<GameListItem> {
        public GameList() {
            super();

            Profiles.registerVersionsListener(this::loadVersions);

            setOnFailedAction(e -> Controllers.navigate(Controllers.getDownloadPage()));
        }

        private void loadVersions(Profile profile) {
            setLoading(true);
            setFailedReason(null);
            HMCLGameRepository repository = profile.getRepository();
            toggleGroup = new ToggleGroup();
            WeakListenerHolder listenerHolder = new WeakListenerHolder();
            toggleGroup.getProperties().put("ReferenceHolder", listenerHolder);
            runInFX(() -> {
                if (profile == Profiles.getSelectedProfile()) {
                    setLoading(false);
                    List<GameListItem> children = repository.getDisplayVersions()
                            .map(version -> new GameListItem(toggleGroup, profile, version.getId()))
                            .collect(Collectors.toList());
                    itemsProperty().setAll(children);
                    children.forEach(GameListItem::checkSelection);

                    if (children.isEmpty()) {
                        setFailedReason("呃,你好像没有安装版本欸,点击这里打开下载界面,嗷呜~");
                    }

                    profile.selectedVersionProperty().addListener(listenerHolder.weak((a, b, newValue) -> {
                        FXUtils.checkFxUserThread();
                        children.forEach(it -> it.selectedProperty().set(false));
                        children.stream()
                                .filter(it -> it.getVersion().equals(newValue))
                                .findFirst()
                                .ifPresent(it -> it.selectedProperty().set(true));
                    }));
                }
                toggleGroup.selectedToggleProperty().addListener((o, a, toggle) -> {
                    if (toggle == null) return;
                    GameListItem model = (GameListItem) toggle.getUserData();
                    model.getProfile().setSelectedVersion(model.getVersion());
                });
            });
        }

        public void refreshList() {
            Profiles.getSelectedProfile().getRepository().refreshVersionsAsync().start();
        }

        @Override
        protected GameListSkin createDefaultSkin() {
            return new GameListSkin();
        }

        private class GameListSkin extends ToolbarListPageSkin<GameList> {

            public GameListSkin() {
                super(GameList.this);
            }

            @Override
            protected List<Node> initializeToolbar(GameList skinnable) {
                return Collections.emptyList();
            }
        }
    }

}
