package cn.ChengZhiYa.BaiShenLauncher.setting;

import cn.ChengZhiYa.BaiShenLauncher.auth.authlibinjector.AuthlibInjectorServer;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.gson.JsonUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.gson.Validation;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class AuthlibInjectorServers implements Validation {

    public static final String CONFIG_FILENAME = "authlib-injectors.json";
    private static final Path configLocation = Paths.get(CONFIG_FILENAME);
    private static AuthlibInjectorServers configInstance;
    private final List<String> urls;

    public AuthlibInjectorServers(List<String> urls) {
        this.urls = urls;
    }

    public synchronized static void init() {
        if (configInstance != null) {
            throw new IllegalStateException("AuthlibInjectorServers is already loaded");
        }

        configInstance = new AuthlibInjectorServers(Collections.emptyList());

        if (Files.exists(configLocation)) {
            try {
                String content = FileUtils.readText(configLocation);
                configInstance = JsonUtils.GSON.fromJson(content, AuthlibInjectorServers.class);
            } catch (IOException | JsonParseException e) {
                Logging.LOG.log(Level.WARNING, "Malformed authlib-injectors.json", e);
            }
        }

        if (ConfigHolder.isNewlyCreated() && !AuthlibInjectorServers.getConfigInstance().getUrls().isEmpty()) {
            ConfigHolder.config().setPreferredLoginType(Accounts.getLoginType(Accounts.FACTORY_AUTHLIB_INJECTOR));
            for (String url : AuthlibInjectorServers.getConfigInstance().getUrls()) {
                Task.supplyAsync(Schedulers.io(), () -> AuthlibInjectorServer.locateServer(url))
                        .thenAcceptAsync(Schedulers.javafx(), server -> ConfigHolder.config().getAuthlibInjectorServers().add(server))
                        .start();
            }
        }
    }

    public static AuthlibInjectorServers getConfigInstance() {
        return configInstance;
    }

    public List<String> getUrls() {
        return urls;
    }

    @Override
    public void validate() throws JsonParseException {
        if (urls == null)
            throw new JsonParseException("authlib-injectors.json -> urls cannot be null");
    }
}
