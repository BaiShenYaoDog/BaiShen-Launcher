package cn.ChengZhiYa.BaiShenLauncher.ui.versions;

import cn.ChengZhiYa.BaiShenLauncher.download.LibraryAnalyzer;
import cn.ChengZhiYa.BaiShenLauncher.mod.ModpackConfiguration;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.google.gson.JsonParseException;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.image.Image;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class GameItem extends Control {

    private static final ThreadPoolExecutor POOL_VERSION_RESOLVE = Lang.threadPool("VersionResolve", true, 1, 1, TimeUnit.SECONDS);

    private final Profile profile;
    private final String version;
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty tag = new SimpleStringProperty();
    private final StringProperty subtitle = new SimpleStringProperty();
    private final ObjectProperty<Image> image = new SimpleObjectProperty<>();

    public GameItem(Profile profile, String id) {
        this.profile = profile;
        this.version = id;

        // GameVersion.minecraftVersion() is a time-costing job (up to ~200 ms)
        CompletableFuture.supplyAsync(() -> profile.getRepository().getGameVersion(id).orElse(I18n.i18n("message.unknown")), POOL_VERSION_RESOLVE)
                .thenAcceptAsync(game -> {
                    StringBuilder libraries = new StringBuilder(game);
                    LibraryAnalyzer analyzer = LibraryAnalyzer.analyze(profile.getRepository().getResolvedPreservingPatchesVersion(id));
                    for (LibraryAnalyzer.LibraryMark mark : analyzer) {
                        String libraryId = mark.getLibraryId();
                        String libraryVersion = mark.getLibraryVersion();
                        if (libraryId.equals(LibraryAnalyzer.LibraryType.MINECRAFT.getPatchId())) continue;
                        if (I18n.hasKey("install.installer." + libraryId)) {
                            libraries.append(", ").append(I18n.i18n("install.installer." + libraryId));
                            if (libraryVersion != null)
                                libraries.append(": ").append(libraryVersion.replaceAll("(?i)" + libraryId, ""));
                        }
                    }

                    subtitle.set(libraries.toString());
                }, Platform::runLater)
                .exceptionally(Lang.handleUncaught);

        CompletableFuture.runAsync(() -> {
                    try {
                        ModpackConfiguration<?> config = profile.getRepository().readModpackConfiguration(version);
                        if (config == null) return;
                        tag.set(config.getVersion());
                    } catch (IOException | JsonParseException e) {
                        Logging.LOG.log(Level.WARNING, "Failed to read modpack configuration from " + version, e);
                    }
                }, Platform::runLater)
                .exceptionally(Lang.handleUncaught);

        title.set(id);
        image.set(profile.getRepository().getVersionIconImage(version));
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new GameItemSkin(this);
    }

    public Profile getProfile() {
        return profile;
    }

    public String getVersion() {
        return version;
    }

    public StringProperty titleProperty() {
        return title;
    }

    public StringProperty tagProperty() {
        return tag;
    }

    public StringProperty subtitleProperty() {
        return subtitle;
    }

    public ObjectProperty<Image> imageProperty() {
        return image;
    }
}
