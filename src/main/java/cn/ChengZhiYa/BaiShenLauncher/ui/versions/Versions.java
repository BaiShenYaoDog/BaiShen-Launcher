package cn.ChengZhiYa.BaiShenLauncher.ui.versions;

import cn.ChengZhiYa.BaiShenLauncher.auth.Account;
import cn.ChengZhiYa.BaiShenLauncher.download.game.GameAssetDownloadTask;
import cn.ChengZhiYa.BaiShenLauncher.game.GameDirectoryType;
import cn.ChengZhiYa.BaiShenLauncher.game.GameRepository;
import cn.ChengZhiYa.BaiShenLauncher.game.LauncherHelper;
import cn.ChengZhiYa.BaiShenLauncher.mod.RemoteMod;
import cn.ChengZhiYa.BaiShenLauncher.setting.Accounts;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profiles;
import cn.ChengZhiYa.BaiShenLauncher.task.FileDownloadTask;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.task.TaskExecutor;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.account.CreateAccountPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.DialogCloseEvent;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.MessageDialogPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.PromptDialogPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.Validator;
import cn.ChengZhiYa.BaiShenLauncher.ui.download.ModpackInstallWizardProvider;
import cn.ChengZhiYa.BaiShenLauncher.ui.export.ExportWizardProvider;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.TaskCancellationAction;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.OperatingSystem;
import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class Versions {
    private Versions() {
    }

    public static void addNewGame() {
        Controllers.getDownloadPage().showGameDownloads();
        Controllers.navigate(Controllers.getDownloadPage());
    }

    public static void importModpack() {
        Profile profile = Profiles.getSelectedProfile();
        if (profile.getRepository().isLoaded()) {
            Controllers.getDecorator().startWizard(new ModpackInstallWizardProvider(profile), I18n.i18n("install.modpack"));
        }
    }

    public static void downloadModpackImpl(Profile profile, String version, RemoteMod.Version file) {
        Path modpack;
        URL downloadURL;
        try {
            modpack = Files.createTempFile("modpack", ".zip");
            downloadURL = new URL(file.getFile().getUrl());
        } catch (IOException e) {
            Controllers.dialog(
                    I18n.i18n("install.failed.downloading.detail", file.getFile().getUrl()) + "\n" + StringUtils.getStackTrace(e),
                    I18n.i18n("download.failed"), MessageDialogPane.MessageType.ERROR);
            return;
        }
        Controllers.taskDialog(
                new FileDownloadTask(downloadURL, modpack.toFile())
                        .whenComplete(Schedulers.javafx(), e -> {
                            if (e == null) {
                                Controllers.getDecorator().startWizard(new ModpackInstallWizardProvider(profile, modpack.toFile()));
                            } else if (e instanceof CancellationException) {
                                Controllers.showToast(I18n.i18n("message.cancelled"));
                            } else {
                                Controllers.dialog(
                                        I18n.i18n("install.failed.downloading.detail", file.getFile().getUrl()) + "\n" + StringUtils.getStackTrace(e),
                                        I18n.i18n("download.failed"), MessageDialogPane.MessageType.ERROR);
                            }
                        }).executor(true),
                I18n.i18n("message.downloading"),
                TaskCancellationAction.NORMAL
        );
    }

    public static void deleteVersion(Profile profile, String version) {
        boolean isIndependent = profile.getVersionSetting(version).getGameDirType() == GameDirectoryType.VERSION_FOLDER;
        boolean isMovingToTrashSupported = FileUtils.isMovingToTrashSupported();
        String message = isIndependent ? I18n.i18n("version.manage.remove.confirm.independent", version) :
                isMovingToTrashSupported ? I18n.i18n("version.manage.remove.confirm.trash", version, version + "_removed") :
                        I18n.i18n("version.manage.remove.confirm", version);

        JFXButton deleteButton = new JFXButton(I18n.i18n("button.delete"));
        deleteButton.getStyleClass().add("dialog-error");
        deleteButton.setOnAction(e -> profile.getRepository().removeVersionFromDisk(version));

        Controllers.confirmAction(message, I18n.i18n("message.warning"), MessageDialogPane.MessageType.WARNING, deleteButton);
    }

    public static CompletableFuture<String> renameVersion(Profile profile, String version) {
        return Controllers.prompt(I18n.i18n("version.manage.rename.message"), (newName, resolve, reject) -> {
            if (!OperatingSystem.isNameValid(newName)) {
                reject.accept(I18n.i18n("install.new_game.malformed"));
                return;
            }
            if (profile.getRepository().renameVersion(version, newName)) {
                resolve.run();
                profile.getRepository().refreshVersionsAsync()
                        .thenRunAsync(Schedulers.javafx(), () -> {
                            if (profile.getRepository().hasVersion(newName)) {
                                profile.setSelectedVersion(newName);
                            }
                        }).start();
            } else {
                reject.accept(I18n.i18n("version.manage.rename.fail"));
            }
        }, version);
    }

    public static void exportVersion(Profile profile, String version) {
        Controllers.getDecorator().startWizard(new ExportWizardProvider(profile, version), I18n.i18n("modpack.wizard"));
    }

    public static void openFolder(Profile profile, String version) {
        FXUtils.openFolder(profile.getRepository().getRunDirectory(version));
    }

    public static void duplicateVersion(Profile profile, String version) {
        Controllers.prompt(
                new PromptDialogPane.Builder(I18n.i18n("version.manage.duplicate.prompt"), (res, resolve, reject) -> {
                    String newVersionName = ((PromptDialogPane.Builder.StringQuestion) res.get(1)).getValue();
                    boolean copySaves = ((PromptDialogPane.Builder.BooleanQuestion) res.get(2)).getValue();
                    Task.runAsync(() -> profile.getRepository().duplicateVersion(version, newVersionName, copySaves))
                            .thenComposeAsync(profile.getRepository().refreshVersionsAsync())
                            .whenComplete(Schedulers.javafx(), (result, exception) -> {
                                if (exception == null) {
                                    resolve.run();
                                } else {
                                    reject.accept(StringUtils.getStackTrace(exception));
                                    profile.getRepository().removeVersionFromDisk(newVersionName);
                                }
                            }).start();
                })
                        .addQuestion(new PromptDialogPane.Builder.HintQuestion(I18n.i18n("version.manage.duplicate.confirm")))
                        .addQuestion(new PromptDialogPane.Builder.StringQuestion(null, version,
                                new Validator(I18n.i18n("install.new_game.already_exists"), newVersionName -> !profile.getRepository().hasVersion(newVersionName))))
                        .addQuestion(new PromptDialogPane.Builder.BooleanQuestion(I18n.i18n("version.manage.duplicate.duplicate_save"), false)));
    }

    public static void updateVersion(Profile profile, String version) {
        Controllers.getDecorator().startWizard(new ModpackInstallWizardProvider(profile, version));
    }

    public static void updateGameAssets(Profile profile, String version) {
        TaskExecutor executor = new GameAssetDownloadTask(profile.getDependency(), profile.getRepository().getVersion(version), GameAssetDownloadTask.DOWNLOAD_INDEX_FORCIBLY, true)
                .executor();
        Controllers.taskDialog(executor, I18n.i18n("version.manage.redownload_assets_index"), TaskCancellationAction.NO_CANCEL);
        executor.start();
    }

    public static void cleanVersion(Profile profile, String id) {
        try {
            profile.getRepository().clean(id);
        } catch (IOException e) {
            Logging.LOG.log(Level.WARNING, "Unable to clean game directory", e);
        }
    }

    public static void generateLaunchScript(Profile profile, String id) {
        if (!checkVersionForLaunching(profile, id))
            return;
        ensureSelectedAccount(account -> {
            GameRepository repository = profile.getRepository();
            FileChooser chooser = new FileChooser();
            if (repository.getRunDirectory(id).isDirectory())
                chooser.setInitialDirectory(repository.getRunDirectory(id));
            chooser.setTitle(I18n.i18n("version.launch_script.save"));
            chooser.getExtensionFilters().add(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS
                    ? new FileChooser.ExtensionFilter(I18n.i18n("extension.bat"), "*.bat")
                    : new FileChooser.ExtensionFilter(I18n.i18n("extension.sh"), "*.sh"));
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.i18n("extension.ps1"), "*.ps1"));
            File file = chooser.showSaveDialog(Controllers.getStage());
            if (file != null)
                new LauncherHelper(profile, account, id).makeLaunchScript(file);
        });
    }

    public static void launch(Profile profile) {
        launch(profile, profile.getSelectedVersion());
    }

    public static void launch(Profile profile, String id) {
        launch(profile, id, null);
    }

    public static void launch(Profile profile, String id, Consumer<LauncherHelper> injecter) {
        if (!checkVersionForLaunching(profile, id))
            return;
        ensureSelectedAccount(account -> {
            LauncherHelper launcherHelper = new LauncherHelper(profile, account, id);
            if (injecter != null)
                injecter.accept(launcherHelper);
            launcherHelper.launch();
        });
    }

    public static void testGame(Profile profile, String id) {
        launch(profile, id, LauncherHelper::setTestMode);
    }

    private static boolean checkVersionForLaunching(Profile profile, String id) {
        if (id == null || !profile.getRepository().isLoaded() || !profile.getRepository().hasVersion(id)) {
            Controllers.dialog(I18n.i18n("version.empty.launch"), I18n.i18n("launch.failed"), MessageDialogPane.MessageType.ERROR, () -> {
                Controllers.navigate(Controllers.getDownloadPage());
            });
            return false;
        } else {
            return true;
        }
    }

    private static void ensureSelectedAccount(Consumer<Account> action) {
        Account account = Accounts.getSelectedAccount();
        if (account == null) {
            CreateAccountPane dialog = new CreateAccountPane();
            dialog.addEventHandler(DialogCloseEvent.CLOSE, e -> {
                Account newAccount = Accounts.getSelectedAccount();
                if (newAccount == null) {
                    // user cancelled operation
                } else {
                    Platform.runLater(() -> action.accept(newAccount));
                }
            });
            Controllers.dialog(dialog);
        } else {
            action.accept(account);
        }
    }

    public static void modifyGlobalSettings(Profile profile) {
        Controllers.getSettingsPage().showGameSettings(profile);
        Controllers.navigate(Controllers.getSettingsPage());
    }

    public static void modifyGameSettings(Profile profile, String version) {
        Controllers.getVersionPage().setVersion(version, profile);
        // VersionPage.loadVersion will be invoked after navigation
        Controllers.navigate(Controllers.getVersionPage());
    }
}
