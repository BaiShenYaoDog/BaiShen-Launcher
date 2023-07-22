package cn.ChengZhiYa.BaiShenLauncher.ui.download;

import cn.ChengZhiYa.BaiShenLauncher.download.DefaultDependencyManager;
import cn.ChengZhiYa.BaiShenLauncher.download.DownloadProvider;
import cn.ChengZhiYa.BaiShenLauncher.download.GameBuilder;
import cn.ChengZhiYa.BaiShenLauncher.download.RemoteVersion;
import cn.ChengZhiYa.BaiShenLauncher.setting.DownloadProviders;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardController;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardProvider;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import javafx.scene.Node;

import java.util.Map;

public final class VanillaInstallWizardProvider implements WizardProvider {
    public static final String PROFILE = "PROFILE";
    private final Profile profile;
    private final DefaultDependencyManager dependencyManager;
    private final DownloadProvider downloadProvider;

    public VanillaInstallWizardProvider(Profile profile) {
        this.profile = profile;
        this.downloadProvider = DownloadProviders.getDownloadProvider();
        this.dependencyManager = profile.getDependency(downloadProvider);
    }

    @Override
    public void start(Map<String, Object> settings) {
        settings.put(PROFILE, profile);
    }

    private Task<Void> finishVersionDownloadingAsync(Map<String, Object> settings) {
        GameBuilder builder = dependencyManager.gameBuilder();

        String name = (String) settings.get("name");
        builder.name(name);
        builder.gameVersion(((RemoteVersion) settings.get("game")).getGameVersion());

        for (Map.Entry<String, Object> entry : settings.entrySet())
            if (!"game".equals(entry.getKey()) && entry.getValue() instanceof RemoteVersion)
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
                return new VersionsPage(controller, I18n.i18n("install.installer.choose", I18n.i18n("install.installer.game")), "", downloadProvider, "game",
                        () -> controller.onNext(new InstallersPage(controller, profile.getRepository(), ((RemoteVersion) controller.getSettings().get("game")).getGameVersion(), downloadProvider)));
            default:
                throw new IllegalStateException("error step " + step + ", settings: " + settings + ", pages: " + controller.getPages());
        }
    }

    @Override
    public boolean cancel() {
        return true;
    }
}
