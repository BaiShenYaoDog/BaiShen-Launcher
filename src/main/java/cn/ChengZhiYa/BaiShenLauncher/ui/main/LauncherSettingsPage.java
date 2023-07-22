package cn.ChengZhiYa.BaiShenLauncher.ui.main;

import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profiles;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.ContainerAnimations;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.TransitionPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.AdvancedListBox;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.PageAware;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.TabHeader;
import cn.ChengZhiYa.BaiShenLauncher.ui.decorator.DecoratorAnimatedPage;
import cn.ChengZhiYa.BaiShenLauncher.ui.decorator.DecoratorPage;
import cn.ChengZhiYa.BaiShenLauncher.ui.versions.VersionSettingsPage;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import static cn.ChengZhiYa.BaiShenLauncher.ui.versions.VersionPage.wrap;

public class LauncherSettingsPage extends DecoratorAnimatedPage implements DecoratorPage, PageAware {
    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(State.fromTitle(I18n.i18n("settings")));
    private final TabHeader tab;
    private final TabHeader.Tab<VersionSettingsPage> gameTab = new TabHeader.Tab<>("versionSettingsPage");
    private final TabHeader.Tab<SettingsPage> settingsTab = new TabHeader.Tab<>("settingsPage");
    private final TabHeader.Tab<PersonalizationPage> personalizationTab = new TabHeader.Tab<>("personalizationPage");
    private final TabHeader.Tab<DownloadSettingsPage> downloadTab = new TabHeader.Tab<>("downloadSettingsPage");
    private final TabHeader.Tab<AboutPage> aboutTab = new TabHeader.Tab<>("aboutPage");
    private final TransitionPane transitionPane = new TransitionPane();

    public LauncherSettingsPage() {
        gameTab.setNodeSupplier(() -> new VersionSettingsPage(true));
        settingsTab.setNodeSupplier(SettingsPage::new);
        personalizationTab.setNodeSupplier(PersonalizationPage::new);
        downloadTab.setNodeSupplier(DownloadSettingsPage::new);
        aboutTab.setNodeSupplier(AboutPage::new);
        tab = new TabHeader(gameTab, settingsTab, personalizationTab, downloadTab, aboutTab);

        tab.select(gameTab);
        gameTab.initializeIfNeeded();
        gameTab.getNode().loadVersion(Profiles.getSelectedProfile(), null);
        FXUtils.onChangeAndOperate(tab.getSelectionModel().selectedItemProperty(), newValue -> {
            transitionPane.setContent(newValue.getNode(), ContainerAnimations.FADE.getAnimationProducer());
        });

        {
            AdvancedListBox sideBar = new AdvancedListBox()
                    .addNavigationDrawerItem(settingsItem -> {
                        settingsItem.setTitle(I18n.i18n("settings.type.global.manage"));
                        settingsItem.setLeftGraphic(wrap(SVG::gamepad));
                        settingsItem.activeProperty().bind(tab.getSelectionModel().selectedItemProperty().isEqualTo(gameTab));
                        settingsItem.setOnAction(e -> tab.select(gameTab));
                    })
                    .startCategory(I18n.i18n("launcher"))
                    .addNavigationDrawerItem(settingsItem -> {
                        settingsItem.setTitle(I18n.i18n("settings.launcher.general"));
                        settingsItem.setLeftGraphic(wrap(SVG::applicationOutline));
                        settingsItem.activeProperty().bind(tab.getSelectionModel().selectedItemProperty().isEqualTo(settingsTab));
                        settingsItem.setOnAction(e -> tab.select(settingsTab));
                    })
                    .addNavigationDrawerItem(personalizationItem -> {
                        personalizationItem.setTitle(I18n.i18n("settings.launcher.appearance"));
                        personalizationItem.setLeftGraphic(wrap(SVG::styleOutline));
                        personalizationItem.activeProperty().bind(tab.getSelectionModel().selectedItemProperty().isEqualTo(personalizationTab));
                        personalizationItem.setOnAction(e -> tab.select(personalizationTab));
                    })
                    .addNavigationDrawerItem(downloadItem -> {
                        downloadItem.setTitle(I18n.i18n("download"));
                        downloadItem.setLeftGraphic(wrap(SVG::downloadOutline));
                        downloadItem.activeProperty().bind(tab.getSelectionModel().selectedItemProperty().isEqualTo(downloadTab));
                        downloadItem.setOnAction(e -> tab.select(downloadTab));
                    })
                    .startCategory("关于")
                    .addNavigationDrawerItem(aboutItem -> {
                        aboutItem.setTitle("关于启动器");
                        aboutItem.setLeftGraphic(wrap(SVG::informationOutline));
                        aboutItem.activeProperty().bind(tab.getSelectionModel().selectedItemProperty().isEqualTo(aboutTab));
                        aboutItem.setOnAction(e -> tab.select(aboutTab));
                    });
            FXUtils.setLimitWidth(sideBar, 200);
            setLeft(sideBar);
        }

        setCenter(transitionPane);
    }

    @Override
    public void onPageShown() {
        tab.onPageShown();
    }

    @Override
    public void onPageHidden() {
        tab.onPageHidden();
    }

    public void showGameSettings(Profile profile) {
        gameTab.getNode().loadVersion(profile, null);
        tab.select(gameTab);
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }
}
