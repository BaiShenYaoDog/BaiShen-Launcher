package cn.ChengZhiYa.BaiShenLauncher.game;

import cn.ChengZhiYa.BaiShenLauncher.mod.*;
import cn.ChengZhiYa.BaiShenLauncher.mod.curse.CurseModpackProvider;
import cn.ChengZhiYa.BaiShenLauncher.mod.mcbbs.McbbsModpackManifest;
import cn.ChengZhiYa.BaiShenLauncher.mod.mcbbs.McbbsModpackProvider;
import cn.ChengZhiYa.BaiShenLauncher.mod.modrinth.ModrinthModpackProvider;
import cn.ChengZhiYa.BaiShenLauncher.mod.multimc.MultiMCInstanceConfiguration;
import cn.ChengZhiYa.BaiShenLauncher.mod.multimc.MultiMCModpackProvider;
import cn.ChengZhiYa.BaiShenLauncher.mod.server.ServerModpackManifest;
import cn.ChengZhiYa.BaiShenLauncher.mod.server.ServerModpackProvider;
import cn.ChengZhiYa.BaiShenLauncher.mod.server.ServerModpackRemoteInstallTask;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profiles;
import cn.ChengZhiYa.BaiShenLauncher.setting.VersionSetting;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.Pair;
import cn.ChengZhiYa.BaiShenLauncher.util.function.ExceptionalConsumer;
import cn.ChengZhiYa.BaiShenLauncher.util.function.ExceptionalRunnable;
import cn.ChengZhiYa.BaiShenLauncher.util.gson.JsonUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.CompressingUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class ModpackHelper {
    private static final Map<String, ModpackProvider> providers = Lang.mapOf(
            Pair.pair(CurseModpackProvider.INSTANCE.getName(), CurseModpackProvider.INSTANCE),
            Pair.pair(McbbsModpackProvider.INSTANCE.getName(), McbbsModpackProvider.INSTANCE),
            Pair.pair(ModrinthModpackProvider.INSTANCE.getName(), ModrinthModpackProvider.INSTANCE),
            Pair.pair(MultiMCModpackProvider.INSTANCE.getName(), MultiMCModpackProvider.INSTANCE),
            Pair.pair(ServerModpackProvider.INSTANCE.getName(), ServerModpackProvider.INSTANCE),
            Pair.pair(HMCLModpackProvider.INSTANCE.getName(), HMCLModpackProvider.INSTANCE)
    );

    private ModpackHelper() {
    }

    @Nullable
    public static ModpackProvider getProviderByType(String type) {
        return providers.get(type);
    }

    public static boolean isFileModpackByExtension(File file) {
        String ext = FileUtils.getExtension(file);
        return "zip".equals(ext) || "mrpack".equals(ext);
    }

    public static Modpack readModpackManifest(Path file, Charset charset) throws UnsupportedModpackException, ManuallyCreatedModpackException {
        try (ZipFile zipFile = CompressingUtils.openZipFile(file, charset)) {
            // Order for trying detecting manifest is necessary here.
            // Do not change to iterating providers.
            for (ModpackProvider provider : new ModpackProvider[]{
                    McbbsModpackProvider.INSTANCE,
                    CurseModpackProvider.INSTANCE,
                    ModrinthModpackProvider.INSTANCE,
                    HMCLModpackProvider.INSTANCE,
                    MultiMCModpackProvider.INSTANCE,
                    ServerModpackProvider.INSTANCE}) {
                try {
                    return provider.readManifest(zipFile, file, charset);
                } catch (Exception ignored) {
                }
            }
        } catch (IOException ignored) {
        }

        try (FileSystem fs = CompressingUtils.createReadOnlyZipFileSystem(file, charset)) {
            findMinecraftDirectoryInManuallyCreatedModpack(file.toString(), fs);
            throw new ManuallyCreatedModpackException(file);
        } catch (IOException e) {
            // ignore it
        }

        throw new UnsupportedModpackException(file.toString());
    }

    public static Path findMinecraftDirectoryInManuallyCreatedModpack(String modpackName, FileSystem fs) throws IOException, UnsupportedModpackException {
        Path root = fs.getPath("/");
        if (isMinecraftDirectory(root)) return root;
        try (Stream<Path> firstLayer = Files.list(root)) {
            for (Path dir : Lang.toIterable(firstLayer)) {
                if (isMinecraftDirectory(dir)) return dir;

                try (Stream<Path> secondLayer = Files.list(dir)) {
                    for (Path subdir : Lang.toIterable(secondLayer)) {
                        if (isMinecraftDirectory(subdir)) return subdir;
                    }
                } catch (IOException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
        throw new UnsupportedModpackException(modpackName);
    }

    private static boolean isMinecraftDirectory(Path path) {
        return Files.isDirectory(path.resolve("versions")) &&
                (path.getFileName() == null || ".minecraft".equals(FileUtils.getName(path)));
    }

    public static ModpackConfiguration<?> readModpackConfiguration(File file) throws IOException {
        if (!file.exists())
            throw new FileNotFoundException(file.getPath());
        else
            try {
                return JsonUtils.GSON.fromJson(FileUtils.readText(file), new TypeToken<ModpackConfiguration<?>>() {
                }.getType());
            } catch (JsonParseException e) {
                throw new IOException("Malformed modpack configuration");
            }
    }

    public static Task<?> getInstallTask(Profile profile, ServerModpackManifest manifest, String name, Modpack modpack) {
        profile.getRepository().markVersionAsModpack(name);

        ExceptionalRunnable<?> success = () -> {
            HMCLGameRepository repository = profile.getRepository();
            repository.refreshVersions();
            VersionSetting vs = repository.specializeVersionSetting(name);
            repository.undoMark(name);
            if (vs != null)
                vs.setGameDirType(GameDirectoryType.VERSION_FOLDER);
        };

        ExceptionalConsumer<Exception, ?> failure = ex -> {
            if (ex instanceof ModpackCompletionException && !(ex.getCause() instanceof FileNotFoundException)) {
                success.run();
                // This is tolerable and we will not delete the game
            }
        };

        return new ServerModpackRemoteInstallTask(profile.getDependency(), manifest, name)
                .whenComplete(Schedulers.defaultScheduler(), success, failure)
                .withStagesHint(Arrays.asList("hmcl.modpack", "hmcl.modpack.download"));
    }

    public static boolean isExternalGameNameConflicts(String name) {
        return Files.exists(Paths.get("externalgames").resolve(name));
    }

    public static Task<?> getInstallManuallyCreatedModpackTask(Profile profile, File zipFile, String name, Charset charset) {
        if (isExternalGameNameConflicts(name)) {
            throw new IllegalArgumentException("name existing");
        }

        return new ManuallyCreatedModpackInstallTask(profile, zipFile.toPath(), charset, name)
                .thenAcceptAsync(Schedulers.javafx(), location -> {
                    Profile newProfile = new Profile(name, location.toFile());
                    newProfile.setUseRelativePath(true);
                    Profiles.getProfiles().add(newProfile);
                    Profiles.setSelectedProfile(newProfile);
                });
    }

    public static Task<?> getInstallTask(Profile profile, File zipFile, String name, Modpack modpack) {
        profile.getRepository().markVersionAsModpack(name);

        ExceptionalRunnable<?> success = () -> {
            HMCLGameRepository repository = profile.getRepository();
            repository.refreshVersions();
            VersionSetting vs = repository.specializeVersionSetting(name);
            repository.undoMark(name);
            if (vs != null)
                vs.setGameDirType(GameDirectoryType.VERSION_FOLDER);
        };

        ExceptionalConsumer<Exception, ?> failure = ex -> {
            if (ex instanceof ModpackCompletionException && !(ex.getCause() instanceof FileNotFoundException)) {
                success.run();
                // This is tolerable and we will not delete the game
            }
        };

        if (modpack.getManifest() instanceof MultiMCInstanceConfiguration)
            return modpack.getInstallTask(profile.getDependency(), zipFile, name)
                    .whenComplete(Schedulers.defaultScheduler(), success, failure)
                    .thenComposeAsync(createMultiMCPostInstallTask(profile, (MultiMCInstanceConfiguration) modpack.getManifest(), name));
        else if (modpack.getManifest() instanceof McbbsModpackManifest)
            return modpack.getInstallTask(profile.getDependency(), zipFile, name)
                    .whenComplete(Schedulers.defaultScheduler(), success, failure)
                    .thenComposeAsync(createMcbbsPostInstallTask(profile, (McbbsModpackManifest) modpack.getManifest(), name));
        else
            return modpack.getInstallTask(profile.getDependency(), zipFile, name)
                    .whenComplete(Schedulers.javafx(), success, failure);
    }

    public static Task<Void> getUpdateTask(Profile profile, ServerModpackManifest manifest, Charset charset, String name, ModpackConfiguration<?> configuration) throws UnsupportedModpackException {
        switch (configuration.getType()) {
            case ServerModpackRemoteInstallTask.MODPACK_TYPE:
                return new ModpackUpdateTask(profile.getRepository(), name, new ServerModpackRemoteInstallTask(profile.getDependency(), manifest, name))
                        .withStagesHint(Arrays.asList("hmcl.modpack", "hmcl.modpack.download"));
            default:
                throw new UnsupportedModpackException();
        }
    }

    public static Task<?> getUpdateTask(Profile profile, File zipFile, Charset charset, String name, ModpackConfiguration<?> configuration) throws UnsupportedModpackException, ManuallyCreatedModpackException, MismatchedModpackTypeException {
        Modpack modpack = ModpackHelper.readModpackManifest(zipFile.toPath(), charset);
        ModpackProvider provider = getProviderByType(configuration.getType());
        if (provider == null) {
            throw new UnsupportedModpackException();
        }
        return provider.createUpdateTask(profile.getDependency(), name, zipFile, modpack);
    }

    public static void toVersionSetting(MultiMCInstanceConfiguration c, VersionSetting vs) {
        vs.setUsesGlobal(false);
        vs.setGameDirType(GameDirectoryType.VERSION_FOLDER);

        if (c.isOverrideJavaLocation()) {
            vs.setJavaDir(Lang.nonNull(c.getJavaPath(), ""));
        }

        if (c.isOverrideMemory()) {
            vs.setPermSize(Optional.ofNullable(c.getPermGen()).map(Object::toString).orElse(""));
            if (c.getMaxMemory() != null)
                vs.setMaxMemory(c.getMaxMemory());
            vs.setMinMemory(c.getMinMemory());
        }

        if (c.isOverrideCommands()) {
            vs.setWrapper(Lang.nonNull(c.getWrapperCommand(), ""));
            vs.setPreLaunchCommand(Lang.nonNull(c.getPreLaunchCommand(), ""));
        }

        if (c.isOverrideJavaArgs()) {
            vs.setJavaArgs(Lang.nonNull(c.getJvmArgs(), ""));
        }

        if (c.isOverrideConsole()) {
            vs.setShowLogs(c.isShowConsole());
        }

        if (c.isOverrideWindow()) {
            vs.setFullscreen(c.isFullscreen());
            if (c.getWidth() != null)
                vs.setWidth(c.getWidth());
            if (c.getHeight() != null)
                vs.setHeight(c.getHeight());
        }
    }

    private static Task<Void> createMultiMCPostInstallTask(Profile profile, MultiMCInstanceConfiguration manifest, String version) {
        return Task.runAsync(Schedulers.javafx(), () -> {
            VersionSetting vs = Objects.requireNonNull(profile.getRepository().specializeVersionSetting(version));
            ModpackHelper.toVersionSetting(manifest, vs);
        });
    }

    private static Task<Void> createMcbbsPostInstallTask(Profile profile, McbbsModpackManifest manifest, String version) {
        return Task.runAsync(Schedulers.javafx(), () -> {
            VersionSetting vs = Objects.requireNonNull(profile.getRepository().specializeVersionSetting(version));
            if (manifest.getLaunchInfo().getMinMemory() > vs.getMaxMemory())
                vs.setMaxMemory(manifest.getLaunchInfo().getMinMemory());
        });
    }
}
