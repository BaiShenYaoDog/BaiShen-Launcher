package cn.ChengZhiYa.BaiShenLauncher.ui.versions;

import cn.ChengZhiYa.BaiShenLauncher.game.World;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.ui.*;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import com.jfoenix.controls.JFXCheckBox;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorldListPage extends ListPageBase<WorldListItem> implements VersionPage.VersionLoadable {
    private final BooleanProperty showAll = new SimpleBooleanProperty(this, "showAll", false);

    private Path savesDir;
    private List<World> worlds;
    private Profile profile;
    private String id;
    private String gameVersion;

    public WorldListPage() {
        FXUtils.applyDragListener(this, it -> "zip".equals(FileUtils.getExtension(it)), modpacks -> {
            installWorld(modpacks.get(0));
        });

        showAll.addListener(e -> {
            if (worlds != null)
                itemsProperty().setAll(worlds.stream()
                        .filter(world -> isShowAll() || world.getGameVersion() == null || world.getGameVersion().equals(gameVersion))
                        .map(WorldListItem::new).collect(Collectors.toList()));
        });
    }

    @Override
    protected ToolbarListPageSkin<WorldListPage> createDefaultSkin() {
        return new WorldListPageSkin();
    }

    public void loadVersion(Profile profile, String id) {
        this.profile = profile;
        this.id = id;
        this.savesDir = profile.getRepository().getRunDirectory(id).toPath().resolve("saves");
        refresh();
    }

    public CompletableFuture<?> refresh() {
        if (profile == null || id == null)
            return CompletableFuture.completedFuture(null);

        setLoading(true);
        return CompletableFuture
                .runAsync(() -> gameVersion = profile.getRepository().getGameVersion(id).orElse(null))
                .thenApplyAsync(unused -> {
                    try (Stream<World> stream = World.getWorlds(savesDir)) {
                        return stream.parallel().collect(Collectors.toList());
                    }
                })
                .whenCompleteAsync((result, exception) -> {
                    worlds = result;
                    setLoading(false);
                    if (exception == null)
                        itemsProperty().setAll(result.stream()
                                .filter(world -> isShowAll() || world.getGameVersion() == null || world.getGameVersion().equals(gameVersion))
                                .map(WorldListItem::new).collect(Collectors.toList()));

                    // https://github.com/huanghongxun/HMCL/issues/938
                    System.gc();
                }, Platform::runLater);
    }

    public void add() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.i18n("world.import.choose"));
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter(I18n.i18n("world.extension"), "*.zip"));
        List<File> res = chooser.showOpenMultipleDialog(Controllers.getStage());

        if (res == null || res.isEmpty()) return;
        installWorld(res.get(0));
    }

    public void download() {
        Controllers.getDownloadPage().showWorldDownloads();
        Controllers.navigate(Controllers.getDownloadPage());
    }

    private void installWorld(File zipFile) {
        // Only accept one world file because user is required to confirm the new world name
        // Or too many input dialogs are popped.
        Task.supplyAsync(() -> new World(zipFile.toPath()))
                .whenComplete(Schedulers.javafx(), world -> {
                    Controllers.prompt(I18n.i18n("world.name.enter"), (name, resolve, reject) -> {
                        Task.runAsync(() -> world.install(savesDir, name))
                                .whenComplete(Schedulers.javafx(), () -> {
                                    itemsProperty().add(new WorldListItem(new World(savesDir.resolve(name))));
                                    resolve.run();
                                }, e -> {
                                    if (e instanceof FileAlreadyExistsException)
                                        reject.accept(I18n.i18n("world.import.failed", I18n.i18n("world.import.already_exists")));
                                    else if (e instanceof IOException && e.getCause() instanceof InvalidPathException)
                                        reject.accept(I18n.i18n("world.import.failed", I18n.i18n("install.new_game.malformed")));
                                    else
                                        reject.accept(I18n.i18n("world.import.failed", e.getClass().getName() + ": " + e.getLocalizedMessage()));
                                }).start();
                    }, world.getWorldName());
                }, e -> {
                    Logging.LOG.log(Level.WARNING, "Unable to parse world file " + zipFile, e);
                    Controllers.dialog(I18n.i18n("world.import.invalid"));
                }).start();
    }

    public boolean isShowAll() {
        return showAll.get();
    }

    public void setShowAll(boolean showAll) {
        this.showAll.set(showAll);
    }

    public BooleanProperty showAllProperty() {
        return showAll;
    }

    private class WorldListPageSkin extends ToolbarListPageSkin<WorldListPage> {

        WorldListPageSkin() {
            super(WorldListPage.this);
        }

        @Override
        protected List<Node> initializeToolbar(WorldListPage skinnable) {
            JFXCheckBox chkShowAll = new JFXCheckBox();
            chkShowAll.setText(I18n.i18n("world.show_all"));
            chkShowAll.selectedProperty().bindBidirectional(skinnable.showAllProperty());

            return Arrays.asList(chkShowAll,
                    createToolbarButton2(I18n.i18n("button.refresh"), SVG::refresh, skinnable::refresh),
                    createToolbarButton2(I18n.i18n("world.add"), SVG::plus, skinnable::add),
                    createToolbarButton2(I18n.i18n("world.download"), SVG::plus, skinnable::download));
        }
    }
}
