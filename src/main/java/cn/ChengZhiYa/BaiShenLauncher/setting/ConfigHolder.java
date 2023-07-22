package cn.ChengZhiYa.BaiShenLauncher.setting;

import cn.ChengZhiYa.BaiShenLauncher.Metadata;
import cn.ChengZhiYa.BaiShenLauncher.util.InvocationDispatcher;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.JarUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.OperatingSystem;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Level;

public final class ConfigHolder {

    public static final String CONFIG_FILENAME = "BSL.json";
    public static final String CONFIG_FILENAME_LINUX = ".BSL.json";
    public static final Path GLOBAL_CONFIG_PATH = Metadata.HMCL_DIRECTORY.resolve("config.json");
    private static final InvocationDispatcher<String> globalConfigWriter = InvocationDispatcher.runOn(Lang::thread, content -> {
        try {
            writeToGlobalConfig(content);
        } catch (IOException e) {
            Logging.LOG.log(Level.SEVERE, "Failed to save config", e);
        }
    });
    private static Path configLocation;
    private static final InvocationDispatcher<String> configWriter = InvocationDispatcher.runOn(Lang::thread, content -> {
        try {
            writeToConfig(content);
        } catch (IOException e) {
            Logging.LOG.log(Level.SEVERE, "Failed to save config", e);
        }
    });
    private static Config configInstance;
    private static GlobalConfig globalConfigInstance;
    private static boolean newlyCreated;
    private static boolean ownerChanged = false;

    private ConfigHolder() {
    }

    public static Config config() {
        if (configInstance == null) {
            throw new IllegalStateException("Configuration hasn't been loaded");
        }
        return configInstance;
    }

    public static GlobalConfig globalConfig() {
        if (globalConfigInstance == null) {
            throw new IllegalStateException("Configuration hasn't been loaded");
        }
        return globalConfigInstance;
    }

    public static Path configLocation() {
        return configLocation;
    }

    public static boolean isNewlyCreated() {
        return newlyCreated;
    }

    public static boolean isOwnerChanged() {
        return ownerChanged;
    }

    public synchronized static void init() throws IOException {
        if (configInstance != null) {
            throw new IllegalStateException("Configuration is already loaded");
        }

        configLocation = locateConfig();

        Logging.LOG.log(Level.INFO, "Config location: " + configLocation);

        configInstance = loadConfig();
        configInstance.addListener(source -> markConfigDirty());

        globalConfigInstance = loadGlobalConfig();
        globalConfigInstance.addListener(source -> markGlobalConfigDirty());

        Settings.init();

        if (newlyCreated) {
            saveConfigSync();

            // hide the config file on windows
            if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
                try {
                    Files.setAttribute(configLocation, "dos:hidden", true);
                } catch (IOException e) {
                    Logging.LOG.log(Level.WARNING, "Failed to set hidden attribute of " + configLocation, e);
                }
            }
        }

        if (!Files.isWritable(configLocation)) {
            if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS
                    && configLocation.getFileSystem() == FileSystems.getDefault()
                    && configLocation.toFile().canWrite()) {
                // There are some serious problems with the implementation of Samba or OpenJDK
                throw new SambaException();
            } else {
                // the config cannot be saved
                // throw up the error now to prevent further data loss
                throw new IOException("Config at " + configLocation + " is not writable");
            }
        }
    }

    private static Path locateConfig() {
        Path exePath = Paths.get("").toAbsolutePath();
        try {
            Path jarPath = JarUtils.thisJar().orElse(null);
            if (jarPath != null && Files.isRegularFile(jarPath) && Files.isWritable(jarPath)) {
                jarPath = jarPath.getParent();
                exePath = jarPath;

                Path config = jarPath.resolve(CONFIG_FILENAME);
                if (Files.isRegularFile(config))
                    return config;

                Path dotConfig = jarPath.resolve(CONFIG_FILENAME_LINUX);
                if (Files.isRegularFile(dotConfig))
                    return dotConfig;
            }

        } catch (Throwable ignore) {
        }

        Path config = Paths.get(CONFIG_FILENAME);
        if (Files.isRegularFile(config))
            return config;

        Path dotConfig = Paths.get(CONFIG_FILENAME_LINUX);
        if (Files.isRegularFile(dotConfig))
            return dotConfig;

        // create new
        return exePath.resolve(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS ? CONFIG_FILENAME : CONFIG_FILENAME_LINUX);
    }

    private static Config loadConfig() throws IOException {
        if (Files.exists(configLocation)) {
            try {
                if (OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS
                        && "root".equals(System.getProperty("user.name"))
                        && !"root".equals(Files.getOwner(configLocation).getName())) {
                    ownerChanged = true;
                }
            } catch (IOException e1) {
                Logging.LOG.log(Level.WARNING, "Failed to get owner");
            }
            try {
                String content = FileUtils.readText(configLocation);
                Config deserialized = Config.fromJson(content);
                if (deserialized == null) {
                    Logging.LOG.info("Config is empty");
                } else {
                    Map<?, ?> raw = new Gson().fromJson(content, Map.class);
                    ConfigUpgrader.upgradeConfig(deserialized, raw);
                    return deserialized;
                }
            } catch (JsonParseException e) {
                Logging.LOG.log(Level.WARNING, "Malformed config.", e);
            }
        }

        Logging.LOG.info("Creating an empty config");
        newlyCreated = true;
        return new Config();
    }

    private static void writeToConfig(String content) throws IOException {
        Logging.LOG.info("Saving config");
        synchronized (configLocation) {
            FileUtils.saveSafely(configLocation, content);
        }
    }

    static void markConfigDirty() {
        configWriter.accept(configInstance.toJson());
    }

    // Global Config

    private static void saveConfigSync() throws IOException {
        writeToConfig(configInstance.toJson());
    }

    private static GlobalConfig loadGlobalConfig() throws IOException {
        if (Files.exists(GLOBAL_CONFIG_PATH)) {
            try {
                String content = FileUtils.readText(GLOBAL_CONFIG_PATH);
                GlobalConfig deserialized = GlobalConfig.fromJson(content);
                if (deserialized == null) {
                    Logging.LOG.info("Config is empty");
                } else {
                    return deserialized;
                }
            } catch (JsonParseException e) {
                Logging.LOG.log(Level.WARNING, "Malformed config.", e);
            }
        }

        Logging.LOG.info("Creating an empty global config");
        return new GlobalConfig();
    }

    private static void writeToGlobalConfig(String content) throws IOException {
        Logging.LOG.info("Saving global config");
        synchronized (GLOBAL_CONFIG_PATH) {
            FileUtils.saveSafely(GLOBAL_CONFIG_PATH, content);
        }
    }

    static void markGlobalConfigDirty() {
        globalConfigWriter.accept(globalConfigInstance.toJson());
    }

    private static void saveGlobalConfigSync() throws IOException {
        writeToConfig(globalConfigInstance.toJson());
    }
}
