package cn.ChengZhiYa.BaiShenLauncher.ui.versions;

import cn.ChengZhiYa.BaiShenLauncher.game.World;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardSinglePage;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Skin;

import java.nio.file.Path;
import java.nio.file.Paths;

public class WorldExportPage extends WizardSinglePage {
    private final StringProperty path = new SimpleStringProperty();
    private final StringProperty gameVersion = new SimpleStringProperty();
    private final StringProperty worldName = new SimpleStringProperty();
    private final World world;

    public WorldExportPage(World world, Path export, Runnable onFinish) {
        super(onFinish);

        this.world = world;

        path.set(export.toString());
        gameVersion.set(world.getGameVersion());
        worldName.set(world.getWorldName());
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new WorldExportPageSkin(this);
    }

    public StringProperty pathProperty() {
        return path;
    }

    public StringProperty gameVersionProperty() {
        return gameVersion;
    }

    public StringProperty worldNameProperty() {
        return worldName;
    }

    public void export() {
        onFinish.run();
    }

    @Override
    public String getTitle() {
        return I18n.i18n("world.export.wizard", world.getFileName());
    }

    @Override
    protected Object finish() {
        return Task.runAsync(I18n.i18n("world.export.wizard", worldName.get()), () -> world.export(Paths.get(path.get()), worldName.get()));
    }
}
