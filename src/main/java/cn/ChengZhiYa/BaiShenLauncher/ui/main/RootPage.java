package cn.ChengZhiYa.BaiShenLauncher.ui.main;

import cn.ChengZhiYa.BaiShenLauncher.event.EventBus;
import cn.ChengZhiYa.BaiShenLauncher.event.RefreshedVersionsEvent;
import cn.ChengZhiYa.BaiShenLauncher.game.HMCLGameRepository;
import cn.ChengZhiYa.BaiShenLauncher.game.ModpackHelper;
import cn.ChengZhiYa.BaiShenLauncher.game.Version;
import cn.ChengZhiYa.BaiShenLauncher.setting.Accounts;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profiles;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.ui.account.AccountAdvancedListItem;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.AdvancedListBox;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.AdvancedListItem;
import cn.ChengZhiYa.BaiShenLauncher.ui.decorator.DecoratorAnimatedPage;
import cn.ChengZhiYa.BaiShenLauncher.ui.decorator.DecoratorPage;
import cn.ChengZhiYa.BaiShenLauncher.ui.download.ModpackInstallWizardProvider;
import cn.ChengZhiYa.BaiShenLauncher.ui.versions.GameAdvancedListItem;
import cn.ChengZhiYa.BaiShenLauncher.ui.versions.Versions;
import cn.ChengZhiYa.BaiShenLauncher.upgrade.UpdateChecker;
import cn.ChengZhiYa.BaiShenLauncher.util.TaskCancellationAction;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.io.CompressingUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.versioning.VersionNumber;
import javafx.beans.property.ReadOnlyObjectProperty;

import java.io.File;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.runInFX;
import static cn.ChengZhiYa.BaiShenLauncher.ui.versions.VersionPage.wrap;

public class RootPage extends DecoratorAnimatedPage implements DecoratorPage {
    private MainPage mainPage = null;
    private boolean checkedModpack = false;

    public RootPage() {
        EventBus.EVENT_BUS.channel(RefreshedVersionsEvent.class)
                .register(event -> onRefreshedVersions((HMCLGameRepository) event.getSource()));

        Profile profile = Profiles.getSelectedProfile();
        if (profile != null && profile.getRepository().isLoaded())
            onRefreshedVersions(Profiles.selectedProfileProperty().get().getRepository());

        getStyleClass().remove("gray-background");
        getLeft().getStyleClass().add("gray-background");
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return getMainPage().stateProperty();
    }

    @Override
    protected Skin createDefaultSkin() {
        return new Skin(this);
    }

    public MainPage getMainPage() {
        if (mainPage == null) {
            MainPage mainPage = new MainPage();
            FXUtils.applyDragListener(mainPage, ModpackHelper::isFileModpackByExtension, modpacks -> {
                File modpack = modpacks.get(0);
                Controllers.getDecorator().startWizard(
                        new ModpackInstallWizardProvider(Profiles.getSelectedProfile(), modpack),
                        I18n.i18n("install.modpack"));
            });

            FXUtils.onChangeAndOperate(Profiles.selectedVersionProperty(), mainPage::setCurrentGame);
            mainPage.showUpdateProperty().bind(UpdateChecker.outdatedProperty());
            mainPage.latestVersionProperty().bind(UpdateChecker.latestVersionProperty());

            Profiles.registerVersionsListener(profile -> {
                HMCLGameRepository repository = profile.getRepository();
                List<Version> children = repository.getVersions().parallelStream()
                        .filter(version -> !version.isHidden())
                        .sorted(Comparator
                                .comparing((Version version) -> version.getReleaseTime() == null ? new Date(0L)
                                        : version.getReleaseTime())
                                .thenComparing(a -> VersionNumber.asVersion(a.getId())))
                        .collect(Collectors.toList());
                runInFX(() -> {
                    if (profile == Profiles.getSelectedProfile())
                        mainPage.initVersions(profile, children);
                });
            });
            this.mainPage = mainPage;
        }
        return mainPage;
    }

    private void onRefreshedVersions(HMCLGameRepository repository) {
        runInFX(() -> {
            if (!checkedModpack) {
                checkedModpack = true;

                if (repository.getVersionCount() == 0) {
                    File modpackFile = new File("modpack.zip").getAbsoluteFile();
                    if (modpackFile.exists()) {
                        Task.supplyAsync(() -> CompressingUtils.findSuitableEncoding(modpackFile.toPath()))
                                .thenApplyAsync(
                                        encoding -> ModpackHelper.readModpackManifest(modpackFile.toPath(), encoding))
                                .thenApplyAsync(modpack -> ModpackHelper
                                        .getInstallTask(repository.getProfile(), modpackFile, modpack.getName(),
                                                modpack)
                                        .executor())
                                .thenAcceptAsync(Schedulers.javafx(), executor -> {
                                    Controllers.taskDialog(executor, I18n.i18n("modpack.installing"), TaskCancellationAction.NO_CANCEL);
                                    executor.start();
                                }).start();
                    }
                }
            }
        });
    }

    private static class Skin extends DecoratorAnimatedPageSkin<RootPage> {

        protected Skin(RootPage control) {
            super(control);

            // first item in left sidebar
            AccountAdvancedListItem accountListItem = new AccountAdvancedListItem();
            accountListItem.setOnAction(e -> Controllers.navigate(Controllers.getAccountListPage()));
            accountListItem.accountProperty().bind(Accounts.selectedAccountProperty());

            // second item in left sidebar
            GameAdvancedListItem gameListItem = new GameAdvancedListItem();
            gameListItem.setOnAction(e -> {
                Profile profile = Profiles.getSelectedProfile();
                String version = Profiles.getSelectedVersion();
                if (version == null) {
                    Controllers.navigate(Controllers.getGameListPage());
                } else {
                    Versions.modifyGameSettings(profile, version);
                }
            });

            // third item in left sidebar
            AdvancedListItem gameItem = new AdvancedListItem();
            gameItem.setLeftGraphic(wrap(SVG::viewList));
            gameItem.setActionButtonVisible(false);
            gameItem.setTitle(I18n.i18n("version.manage"));
            gameItem.setOnAction(e -> Controllers.navigate(Controllers.getGameListPage()));

            // forth item in left sidebar
            AdvancedListItem downloadItem = new AdvancedListItem();
            downloadItem.setLeftGraphic(wrap(SVG::downloadOutline));
            downloadItem.setActionButtonVisible(false);
            downloadItem.setTitle(I18n.i18n("download"));
            downloadItem.setOnAction(e -> Controllers.navigate(Controllers.getDownloadPage()));

            // sixth item in left sidebar
            AdvancedListItem launcherSettingsItem = new AdvancedListItem();
            launcherSettingsItem.setLeftGraphic(wrap(SVG::gearOutline));
            launcherSettingsItem.setActionButtonVisible(false);
            launcherSettingsItem.setTitle(I18n.i18n("settings"));
            launcherSettingsItem.setOnAction(e -> Controllers.navigate(Controllers.getSettingsPage()));

            // the left sidebar
            AdvancedListBox sideBar = new AdvancedListBox()
                    .startCategory(I18n.i18n("account").toUpperCase(Locale.ROOT))
                    .add(accountListItem)
                    .startCategory(I18n.i18n("version").toUpperCase(Locale.ROOT))
                    .add(gameListItem)
                    .add(gameItem)
                    .add(downloadItem)
                    .startCategory(I18n.i18n("settings.launcher.general").toUpperCase(Locale.ROOT))
                    .add(launcherSettingsItem);

            // the root page, with the sidebar in left, navigator in center.
            setLeft(sideBar);
            setCenter(getSkinnable().getMainPage());
        }

    }
}
