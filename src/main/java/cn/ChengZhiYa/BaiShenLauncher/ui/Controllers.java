package cn.ChengZhiYa.BaiShenLauncher.ui;

import cn.ChengZhiYa.BaiShenLauncher.Launcher;
import cn.ChengZhiYa.BaiShenLauncher.Metadata;
import cn.ChengZhiYa.BaiShenLauncher.game.ModpackHelper;
import cn.ChengZhiYa.BaiShenLauncher.setting.*;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.task.TaskExecutor;
import cn.ChengZhiYa.BaiShenLauncher.ui.account.AccountListPage;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.*;
import cn.ChengZhiYa.BaiShenLauncher.ui.decorator.DecoratorController;
import cn.ChengZhiYa.BaiShenLauncher.ui.download.DownloadPage;
import cn.ChengZhiYa.BaiShenLauncher.ui.download.ModpackInstallWizardProvider;
import cn.ChengZhiYa.BaiShenLauncher.ui.main.LauncherSettingsPage;
import cn.ChengZhiYa.BaiShenLauncher.ui.main.RootPage;
import cn.ChengZhiYa.BaiShenLauncher.ui.versions.GameListPage;
import cn.ChengZhiYa.BaiShenLauncher.ui.versions.VersionPage;
import cn.ChengZhiYa.BaiShenLauncher.util.FutureCallback;
import cn.ChengZhiYa.BaiShenLauncher.util.Lazy;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.TaskCancellationAction;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.Architecture;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.JavaVersion;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.OperatingSystem;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.newImage;

public final class Controllers {
    private static InvalidationListener stageSizeChangeListener;
    private static DoubleProperty stageWidth = new SimpleDoubleProperty();
    private static DoubleProperty stageHeight = new SimpleDoubleProperty();

    private static Scene scene;
    private static Stage stage;
    private static Lazy<VersionPage> versionPage = new Lazy<>(VersionPage::new);
    private static Lazy<RootPage> rootPage = new Lazy<>(RootPage::new);
    private static DecoratorController decorator;
    private static Lazy<GameListPage> gameListPage = new Lazy<>(() -> {
        GameListPage gameListPage = new GameListPage();
        gameListPage.selectedProfileProperty().bindBidirectional(Profiles.selectedProfileProperty());
        gameListPage.profilesProperty().bindContent(Profiles.profilesProperty());
        FXUtils.applyDragListener(gameListPage, ModpackHelper::isFileModpackByExtension, modpacks -> {
            File modpack = modpacks.get(0);
            Controllers.getDecorator().startWizard(new ModpackInstallWizardProvider(Profiles.getSelectedProfile(), modpack), I18n.i18n("install.modpack"));
        });
        return gameListPage;
    });
    private static Lazy<DownloadPage> downloadPage = new Lazy<>(DownloadPage::new);
    private static Lazy<AccountListPage> accountListPage = new Lazy<>(() -> {
        AccountListPage accountListPage = new AccountListPage();
        accountListPage.selectedAccountProperty().bindBidirectional(Accounts.selectedAccountProperty());
        accountListPage.accountsProperty().bindContent(Accounts.getAccounts());
        accountListPage.authServersProperty().bindContentBidirectional(ConfigHolder.config().getAuthlibInjectorServers());
        return accountListPage;
    });
    private static Lazy<LauncherSettingsPage> settingsPage = new Lazy<>(LauncherSettingsPage::new);

    private Controllers() {
    }

    public static Scene getScene() {
        return scene;
    }

    public static Stage getStage() {
        return stage;
    }

    // FXThread
    public static VersionPage getVersionPage() {
        return versionPage.get();
    }

    // FXThread
    public static GameListPage getGameListPage() {
        return gameListPage.get();
    }

    // FXThread
    public static RootPage getRootPage() {
        return rootPage.get();
    }

    // FXThread
    public static LauncherSettingsPage getSettingsPage() {
        return settingsPage.get();
    }

    // FXThread
    public static AccountListPage getAccountListPage() {
        return accountListPage.get();
    }

    // FXThread
    public static DownloadPage getDownloadPage() {
        return downloadPage.get();
    }

    // FXThread
    public static DecoratorController getDecorator() {
        return decorator;
    }

    public static void onApplicationStop() {
        stageSizeChangeListener = null;
        if (stageHeight != null) {
            ConfigHolder.config().setHeight(stageHeight.get());
            stageHeight = null;
        }
        if (stageWidth != null) {
            ConfigHolder.config().setWidth(stageWidth.get());
            stageWidth = null;
        }
    }

    public static void initialize(Stage stage) {
        Logging.LOG.info("Start initializing application");

        Controllers.stage = stage;

        stageSizeChangeListener = o -> {
            ReadOnlyDoubleProperty sourceProperty = (ReadOnlyDoubleProperty) o;
            DoubleProperty targetProperty = "width".equals(sourceProperty.getName()) ? stageWidth : stageHeight;

            if (targetProperty != null
                    && Controllers.stage != null
                    && !Controllers.stage.isIconified()) {
                targetProperty.set(sourceProperty.get());
            }
        };

        WeakInvalidationListener weakListener = new WeakInvalidationListener(stageSizeChangeListener);

        double initHeight = ConfigHolder.config().getHeight();
        double initWidth = ConfigHolder.config().getWidth();

        stage.setHeight(initHeight);
        stage.setWidth(initWidth);
        stageHeight.set(initHeight);
        stageWidth.set(initWidth);
        stage.heightProperty().addListener(weakListener);
        stage.widthProperty().addListener(weakListener);

        stage.setOnCloseRequest(e -> Launcher.stopApplication());

        decorator = new DecoratorController(stage, getRootPage());

        if (ConfigHolder.config().getCommonDirType() == EnumCommonDirectory.CUSTOM &&
                !FileUtils.canCreateDirectory(ConfigHolder.config().getCommonDirectory())) {
            ConfigHolder.config().setCommonDirType(EnumCommonDirectory.DEFAULT);
            dialog(I18n.i18n("launcher.cache_directory.invalid"));
        }

        Task.runAsync(JavaVersion::initialize).start();

        scene = new Scene(decorator.getDecorator());
        scene.setFill(Color.TRANSPARENT);
        stage.setMinHeight(450 + 2 + 40 + 16); // bg height + border width*2 + toolbar height + shadow width*2
        stage.setMinWidth(800 + 2 + 16); // bg width + border width*2 + shadow width*2
        decorator.getDecorator().prefWidthProperty().bind(scene.widthProperty());
        decorator.getDecorator().prefHeightProperty().bind(scene.heightProperty());
        scene.getStylesheets().setAll(Theme.getTheme().getStylesheets(ConfigHolder.config().getLauncherFontFamily()));

        stage.getIcons().add(newImage("/assets/img/icon.png"));
        stage.setTitle(Metadata.FULL_TITLE);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);

        if (!Architecture.SYSTEM_ARCH.isX86() && ConfigHolder.globalConfig().getPlatformPromptVersion() < 1) {
            Runnable continueAction = () -> ConfigHolder.globalConfig().setPlatformPromptVersion(1);

            if (OperatingSystem.CURRENT_OS == OperatingSystem.OSX && Architecture.SYSTEM_ARCH == Architecture.ARM64) {
                Controllers.dialog("你当前的系统环使用纯ARM的Java,可以获得最大性能\n如果出现兼容性问题请尝试x86或x64的Java", null, MessageDialogPane.MessageType.INFO, continueAction);
            } else if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS && Architecture.SYSTEM_ARCH == Architecture.ARM64) {
                Controllers.dialog("你当前的系统环境可能需要安装<a href=\"ms-windows-store://pdp/?productid=9NQPSL29BFFF\">OpenGL 兼容包</a>", null, MessageDialogPane.MessageType.INFO, continueAction);
            } else {
                Controllers.dialog("你当前的系统环境Minecraft不能提供最大的兼容,别担心阿豹会办法的!\n可以尝试在版本设置中打开“使用 OpenGL 软渲染器”哦~", null, MessageDialogPane.MessageType.WARNING, continueAction);
            }
        }


        if (ConfigHolder.globalConfig().getAgreementVersion() < 1) {
            JFXDialogLayout agreementPane = new JFXDialogLayout();
            agreementPane.setHeading(new Label(I18n.i18n("launcher.agreement")));
            agreementPane.setBody(new Label(I18n.i18n("launcher.agreement.hint")));
            JFXHyperlink agreementLink = new JFXHyperlink(I18n.i18n("launcher.agreement"));
            agreementLink.setExternalLink(Metadata.EULA_URL);
            JFXButton yesButton = new JFXButton(I18n.i18n("launcher.agreement.accept"));
            yesButton.getStyleClass().add("dialog-accept");
            yesButton.setOnAction(e -> {
                ConfigHolder.globalConfig().setAgreementVersion(1);
                agreementPane.fireEvent(new DialogCloseEvent());
            });
            JFXButton noButton = new JFXButton(I18n.i18n("launcher.agreement.decline"));
            noButton.getStyleClass().add("dialog-cancel");
            noButton.setOnAction(e -> System.exit(1));
            agreementPane.setActions(agreementLink, yesButton, noButton);
            Controllers.dialog(agreementPane);
        }
    }

    public static void dialog(Region content) {
        if (decorator != null)
            decorator.showDialog(content);
    }

    public static void dialog(String text) {
        dialog(text, null);
    }

    public static void dialog(String text, String title) {
        dialog(text, title, MessageDialogPane.MessageType.INFO);
    }

    public static void dialog(String text, String title, MessageDialogPane.MessageType type) {
        dialog(text, title, type, null);
    }

    public static void dialog(String text, String title, MessageDialogPane.MessageType type, Runnable ok) {
        dialog(new MessageDialogPane.Builder(text, title, type).ok(ok).build());
    }

    public static void confirm(String text, String title, Runnable yes, Runnable no) {
        confirm(text, title, MessageDialogPane.MessageType.QUESTION, yes, no);
    }

    public static void confirm(String text, String title, MessageDialogPane.MessageType type, Runnable yes, Runnable no) {
        dialog(new MessageDialogPane.Builder(text, title, type).yesOrNo(yes, no).build());
    }

    public static void confirmAction(String text, String title, MessageDialogPane.MessageType type, ButtonBase actionButton) {
        dialog(new MessageDialogPane.Builder(text, title, type).actionOrCancel(actionButton, null).build());
    }

    public static CompletableFuture<String> prompt(String title, FutureCallback<String> onResult) {
        return prompt(title, onResult, "");
    }

    public static CompletableFuture<String> prompt(String title, FutureCallback<String> onResult, String initialValue) {
        InputDialogPane pane = new InputDialogPane(title, initialValue, onResult);
        dialog(pane);
        return pane.getCompletableFuture();
    }

    public static CompletableFuture<List<PromptDialogPane.Builder.Question<?>>> prompt(PromptDialogPane.Builder builder) {
        PromptDialogPane pane = new PromptDialogPane(builder);
        dialog(pane);
        return pane.getCompletableFuture();
    }

    public static TaskExecutorDialogPane taskDialog(TaskExecutor executor, String title, TaskCancellationAction onCancel) {
        TaskExecutorDialogPane pane = new TaskExecutorDialogPane(onCancel);
        pane.setTitle(title);
        pane.setExecutor(executor);
        dialog(pane);
        return pane;
    }

    public static TaskExecutorDialogPane taskDialog(Task<?> task, String title, TaskCancellationAction onCancel) {
        TaskExecutor executor = task.executor();
        TaskExecutorDialogPane pane = new TaskExecutorDialogPane(onCancel);
        pane.setTitle(title);
        pane.setExecutor(executor);
        dialog(pane);
        executor.start();
        return pane;
    }

    public static void navigate(Node node) {
        decorator.navigate(node);
    }

    public static void showToast(String content) {
        decorator.showToast(content);
    }

    public static void onHyperlinkAction(String href) {
        if (href.startsWith("hmcl://")) {
            if (href.equals("hmcl://hide-announcement")) {
                Controllers.getRootPage().getMainPage().hideAnnouncementPane();
            }
        } else {
            FXUtils.openLink(href);
        }
    }

    public static boolean isStopped() {
        return decorator == null;
    }

    public static void shutdown() {
        rootPage = null;
        versionPage = null;
        gameListPage = null;
        downloadPage = null;
        accountListPage = null;
        settingsPage = null;
        decorator = null;
        stage = null;
        scene = null;
        onApplicationStop();
    }
}
