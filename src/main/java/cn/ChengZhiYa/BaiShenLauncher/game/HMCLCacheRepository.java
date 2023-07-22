package cn.ChengZhiYa.BaiShenLauncher.game;

import cn.ChengZhiYa.BaiShenLauncher.download.DefaultCacheRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Paths;

public class HMCLCacheRepository extends DefaultCacheRepository {

    public static final HMCLCacheRepository REPOSITORY = new HMCLCacheRepository();
    private final StringProperty directory = new SimpleStringProperty();

    public HMCLCacheRepository() {
        directory.addListener((a, b, t) -> changeDirectory(Paths.get(t)));
    }

    public String getDirectory() {
        return directory.get();
    }

    public void setDirectory(String directory) {
        this.directory.set(directory);
    }

    public StringProperty directoryProperty() {
        return directory;
    }
}
