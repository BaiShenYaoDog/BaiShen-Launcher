package cn.ChengZhiYa.BaiShenLauncher.game;

import cn.ChengZhiYa.BaiShenLauncher.download.DefaultDependencyManager;
import cn.ChengZhiYa.BaiShenLauncher.mod.MismatchedModpackTypeException;
import cn.ChengZhiYa.BaiShenLauncher.mod.Modpack;
import cn.ChengZhiYa.BaiShenLauncher.mod.ModpackProvider;
import cn.ChengZhiYa.BaiShenLauncher.mod.ModpackUpdateTask;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.gson.JsonUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.CompressingUtils;
import com.google.gson.JsonParseException;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

public final class HMCLModpackProvider implements ModpackProvider {
    public static final HMCLModpackProvider INSTANCE = new HMCLModpackProvider();

    @Override
    public String getName() {
        return "BSL";
    }

    @Override
    public Task<?> createCompletionTask(DefaultDependencyManager dependencyManager, String version) {
        return null;
    }

    @Override
    public Task<?> createUpdateTask(DefaultDependencyManager dependencyManager, String name, File zipFile, Modpack modpack) throws MismatchedModpackTypeException {
        if (!(modpack.getManifest() instanceof HMCLModpackManifest))
            throw new MismatchedModpackTypeException(getName(), modpack.getManifest().getProvider().getName());

        if (!(dependencyManager.getGameRepository() instanceof HMCLGameRepository)) {
            throw new IllegalArgumentException("HMCLModpackProvider requires HMCLGameRepository");
        }

        HMCLGameRepository repository = (HMCLGameRepository) dependencyManager.getGameRepository();
        Profile profile = repository.getProfile();

        return new ModpackUpdateTask(dependencyManager.getGameRepository(), name, new HMCLModpackInstallTask(profile, zipFile, modpack, name));
    }

    @Override
    public Modpack readManifest(ZipFile file, Path path, Charset encoding) throws IOException, JsonParseException {
        String manifestJson = CompressingUtils.readTextZipEntry(file, "modpack.json");
        Modpack manifest = JsonUtils.fromNonNullJson(manifestJson, HMCLModpack.class).setEncoding(encoding);
        String gameJson = CompressingUtils.readTextZipEntry(file, "minecraft/pack.json");
        Version game = JsonUtils.fromNonNullJson(gameJson, Version.class);
        if (game.getJar() == null)
            if (StringUtils.isBlank(manifest.getVersion()))
                throw new JsonParseException("Cannot recognize the game version of modpack " + file + ".");
            else
                manifest.setManifest(HMCLModpackManifest.INSTANCE);
        else
            manifest.setManifest(HMCLModpackManifest.INSTANCE).setGameVersion(game.getJar());
        return manifest;
    }

    private static class HMCLModpack extends Modpack {
        @Override
        public Task<?> getInstallTask(DefaultDependencyManager dependencyManager, File zipFile, String name) {
            return new HMCLModpackInstallTask(((HMCLGameRepository) dependencyManager.getGameRepository()).getProfile(), zipFile, this, name);
        }
    }

}
