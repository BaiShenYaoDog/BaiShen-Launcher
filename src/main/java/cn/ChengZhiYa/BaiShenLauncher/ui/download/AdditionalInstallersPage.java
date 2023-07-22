package cn.ChengZhiYa.BaiShenLauncher.ui.download;

import cn.ChengZhiYa.BaiShenLauncher.download.DownloadProvider;
import cn.ChengZhiYa.BaiShenLauncher.download.LibraryAnalyzer;
import cn.ChengZhiYa.BaiShenLauncher.download.RemoteVersion;
import cn.ChengZhiYa.BaiShenLauncher.game.GameRepository;
import cn.ChengZhiYa.BaiShenLauncher.game.HMCLGameRepository;
import cn.ChengZhiYa.BaiShenLauncher.game.Version;
import cn.ChengZhiYa.BaiShenLauncher.ui.InstallerItem;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardController;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.Map;
import java.util.Optional;

class AdditionalInstallersPage extends InstallersPage {
    protected final BooleanProperty compatible = new SimpleBooleanProperty();
    protected final GameRepository repository;
    protected final String gameVersion;
    protected final Version version;

    public AdditionalInstallersPage(String gameVersion, Version version, WizardController controller, HMCLGameRepository repository, DownloadProvider downloadProvider) {
        super(controller, repository, gameVersion, downloadProvider);
        this.gameVersion = gameVersion;
        this.version = version;
        this.repository = repository;

        txtName.getValidators().clear();
        txtName.setText(version.getId());
        txtName.setEditable(false);

        installable.bind(Bindings.createBooleanBinding(
                () -> compatible.get() && txtName.validate(),
                txtName.textProperty(), compatible));

        for (InstallerItem library : group.getLibraries()) {
            String libraryId = library.getLibraryId();
            if (libraryId.equals("game")) continue;
            library.removeAction.set(e -> {
                controller.getSettings().put(libraryId, new UpdateInstallerWizardProvider.RemoveVersionAction(libraryId));
                reload();
            });
        }
    }

    @Override
    protected void onInstall() {
        controller.onFinish();
    }

    @Override
    public String getTitle() {
        return I18n.i18n("settings.tabs.installers");
    }

    private String getVersion(String id) {
        return Optional.ofNullable(controller.getSettings().get(id))
                .flatMap(it -> Lang.tryCast(it, RemoteVersion.class))
                .map(RemoteVersion::getSelfVersion).orElse(null);
    }

    @Override
    protected void reload() {
        LibraryAnalyzer analyzer = LibraryAnalyzer.analyze(version.resolvePreservingPatches(repository));
        String game = analyzer.getVersion(LibraryAnalyzer.LibraryType.MINECRAFT).orElse(null);
        String forge = analyzer.getVersion(LibraryAnalyzer.LibraryType.FORGE).orElse(null);
        String liteLoader = analyzer.getVersion(LibraryAnalyzer.LibraryType.LITELOADER).orElse(null);
        String optiFine = analyzer.getVersion(LibraryAnalyzer.LibraryType.OPTIFINE).orElse(null);
        String fabric = analyzer.getVersion(LibraryAnalyzer.LibraryType.FABRIC).orElse(null);
        String fabricApi = analyzer.getVersion(LibraryAnalyzer.LibraryType.FABRIC_API).orElse(null);
        String quilt = analyzer.getVersion(LibraryAnalyzer.LibraryType.QUILT).orElse(null);
        String quiltApi = analyzer.getVersion(LibraryAnalyzer.LibraryType.QUILT_API).orElse(null);

        InstallerItem[] libraries = group.getLibraries();
        String[] versions = new String[]{game, forge, liteLoader, optiFine, fabric, fabricApi, quilt, quiltApi};

        String currentGameVersion = Lang.nonNull(getVersion("game"), game);

        boolean compatible = true;
        for (int i = 0; i < libraries.length; ++i) {
            String libraryId = libraries[i].getLibraryId();
            String libraryVersion = Lang.nonNull(getVersion(libraryId), versions[i]);
            boolean alreadyInstalled = versions[i] != null && !(controller.getSettings().get(libraryId) instanceof UpdateInstallerWizardProvider.RemoveVersionAction);
            if (!"game".equals(libraryId) && currentGameVersion != null && !currentGameVersion.equals(game) && getVersion(libraryId) == null && alreadyInstalled) {
                // For third-party libraries, if game version is being changed, and the library is not being reinstalled,
                // warns the user that we should update the library.
                libraries[i].setState(libraryVersion, /* incompatibleWithGame */ true, /* removable */ true);
                compatible = false;
            } else if (alreadyInstalled || getVersion(libraryId) != null) {
                libraries[i].setState(libraryVersion, /* incompatibleWithGame */ false, /* removable */ true);
            } else {
                libraries[i].setState(/* libraryVersion */ null, /* incompatibleWithGame */ false, /* removable */ false);
            }
        }
        this.compatible.set(compatible);
    }

    @Override
    public void cleanup(Map<String, Object> settings) {
    }
}
