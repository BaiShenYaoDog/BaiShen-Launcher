package cn.ChengZhiYa.BaiShenLauncher.util;

import cn.ChengZhiYa.BaiShenLauncher.game.Library;
import cn.ChengZhiYa.BaiShenLauncher.game.NativesDirectoryType;
import cn.ChengZhiYa.BaiShenLauncher.game.Renderer;
import cn.ChengZhiYa.BaiShenLauncher.game.Version;
import cn.ChengZhiYa.BaiShenLauncher.setting.VersionSetting;
import cn.ChengZhiYa.BaiShenLauncher.util.gson.JsonUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.Architecture;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.JavaVersion;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.OperatingSystem;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.Platform;
import cn.ChengZhiYa.BaiShenLauncher.util.versioning.VersionNumber;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class NativePatcher {
    private static final Library NONEXISTENT_LIBRARY = new Library(null);
    private static final Map<Platform, Map<String, Library>> natives = new HashMap<>();

    private NativePatcher() {
    }

    private static Map<String, Library> getNatives(Platform platform) {
        return natives.computeIfAbsent(platform, p -> {
            //noinspection ConstantConditions
            try (Reader reader = new InputStreamReader(NativePatcher.class.getResourceAsStream("/assets/natives.json"), StandardCharsets.UTF_8)) {
                Map<String, Map<String, Library>> natives = JsonUtils.GSON.fromJson(reader, new TypeToken<Map<String, Map<String, Library>>>() {
                }.getType());

                return natives.getOrDefault(p.toString(), Collections.emptyMap());
            } catch (IOException e) {
                Logging.LOG.log(Level.WARNING, "Failed to load native library list", e);
                return Collections.emptyMap();
            }
        });
    }

    public static Version patchNative(Version version, String gameVersion, JavaVersion javaVersion, VersionSetting settings) {
        if (settings.getNativesDirType() == NativesDirectoryType.CUSTOM) {
            if (gameVersion != null && VersionNumber.VERSION_COMPARATOR.compare(gameVersion, "1.19") < 0)
                return version;

            ArrayList<Library> newLibraries = new ArrayList<>();
            for (Library library : version.getLibraries()) {
                if (!library.appliesToCurrentEnvironment())
                    continue;

                if (library.getClassifier() == null
                        || !library.getArtifactId().startsWith("lwjgl")
                        || !library.getClassifier().startsWith("natives")) {
                    newLibraries.add(library);
                }
            }
            return version.setLibraries(newLibraries);
        }

        final boolean useNativeGLFW = settings.isUseNativeGLFW();
        final boolean useNativeOpenAL = settings.isUseNativeOpenAL();

        if (OperatingSystem.CURRENT_OS == OperatingSystem.LINUX
                && (useNativeGLFW || useNativeOpenAL)
                && VersionNumber.VERSION_COMPARATOR.compare(gameVersion, "1.19") >= 0) {

            version = version.setLibraries(version.getLibraries().stream()
                    .filter(library -> {
                        if (library.getClassifier() != null && library.getClassifier().startsWith("natives")
                                && "org.lwjgl".equals(library.getGroupId())) {
                            if ((useNativeGLFW && "lwjgl-glfw".equals(library.getArtifactId()))
                                    || (useNativeOpenAL && "lwjgl-openal".equals(library.getArtifactId()))) {
                                Logging.LOG.info("Filter out " + library.getName());
                                return false;
                            }
                        }

                        return true;
                    })
                    .collect(Collectors.toList()));
        }

        // Try patch natives

        if (settings.isNotPatchNatives())
            return version;

        if (javaVersion.getArchitecture().isX86())
            return version;

        if (javaVersion.getPlatform().getOperatingSystem() == OperatingSystem.OSX
                && javaVersion.getArchitecture() == Architecture.ARM64
                && gameVersion != null
                && VersionNumber.VERSION_COMPARATOR.compare(gameVersion, "1.19") >= 0)
            return version;

        Map<String, Library> replacements = getNatives(javaVersion.getPlatform());
        if (replacements.isEmpty()) {
            Logging.LOG.warning("No alternative native library provided for platform " + javaVersion.getPlatform());
            return version;
        }

        ArrayList<Library> newLibraries = new ArrayList<>();
        for (Library library : version.getLibraries()) {
            if (!library.appliesToCurrentEnvironment())
                continue;

            if (library.isNative()) {
                Library replacement = replacements.getOrDefault(library.getName() + ":natives", NONEXISTENT_LIBRARY);
                if (replacement == NONEXISTENT_LIBRARY) {
                    Logging.LOG.warning("No alternative native library " + library.getName() + " provided for platform " + javaVersion.getPlatform());
                    newLibraries.add(library);
                } else if (replacement != null) {
                    newLibraries.add(replacement);
                }
            } else {
                Library replacement = replacements.getOrDefault(library.getName(), NONEXISTENT_LIBRARY);
                if (replacement == NONEXISTENT_LIBRARY) {
                    newLibraries.add(library);
                } else if (replacement != null) {
                    newLibraries.add(replacement);
                }
            }
        }

        return version.setLibraries(newLibraries);
    }

    public static Library getMesaLoader(JavaVersion javaVersion, Renderer renderer) {
        return getNatives(javaVersion.getPlatform()).get(renderer == Renderer.LLVMPIPE ? "software-renderer-loader" : "mesa-loader");
    }
}
