package cn.ChengZhiYa.BaiShenLauncher.ui.export;

import cn.ChengZhiYa.BaiShenLauncher.mod.ModAdviser;
import cn.ChengZhiYa.BaiShenLauncher.mod.ModpackExportInfo;
import cn.ChengZhiYa.BaiShenLauncher.mod.mcbbs.McbbsModpackExportTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.multimc.MultiMCInstanceConfiguration;
import cn.ChengZhiYa.BaiShenLauncher.mod.multimc.MultiMCModpackExportTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.server.ServerModpackExportTask;
import cn.ChengZhiYa.BaiShenLauncher.setting.Config;
import cn.ChengZhiYa.BaiShenLauncher.setting.ConfigHolder;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.setting.VersionSetting;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardController;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardProvider;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.io.JarUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.Zipper;
import javafx.scene.Node;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ExportWizardProvider implements WizardProvider {
    private final Profile profile;
    private final String version;

    public ExportWizardProvider(Profile profile, String version) {
        this.profile = profile;
        this.version = version;
    }

    @Override
    public void start(Map<String, Object> settings) {
    }

    @Override
    public Object finish(Map<String, Object> settings) {
        @SuppressWarnings("unchecked")
        List<String> whitelist = (List<String>) settings.get(ModpackFileSelectionPage.MODPACK_FILE_SELECTION);
        File modpackFile = (File) settings.get(ModpackInfoPage.MODPACK_FILE);
        ModpackExportInfo exportInfo = (ModpackExportInfo) settings.get(ModpackInfoPage.MODPACK_INFO);
        exportInfo.setWhitelist(whitelist);
        String modpackType = (String) settings.get(ModpackTypeSelectionPage.MODPACK_TYPE);

        return exportWithLauncher(modpackType, exportInfo, modpackFile);
    }

    private Task<?> exportWithLauncher(String modpackType, ModpackExportInfo exportInfo, File modpackFile) {
        Optional<Path> launcherJar = JarUtils.thisJar();
        boolean packWithLauncher = exportInfo.isPackWithLauncher() && launcherJar.isPresent();
        return new Task<Object>() {
            File tempModpack;
            Task<?> exportTask;

            @Override
            public boolean doPreExecute() {
                return true;
            }

            @Override
            public void preExecute() throws Exception {
                File dest;
                if (packWithLauncher) {
                    dest = tempModpack = Files.createTempFile("hmcl", ".zip").toFile();
                } else {
                    dest = modpackFile;
                }

                switch (modpackType) {
                    case ModpackTypeSelectionPage.MODPACK_TYPE_MCBBS:
                        exportTask = exportAsMcbbs(exportInfo, dest);
                        break;
                    case ModpackTypeSelectionPage.MODPACK_TYPE_MULTIMC:
                        exportTask = exportAsMultiMC(exportInfo, dest);
                        break;
                    case ModpackTypeSelectionPage.MODPACK_TYPE_SERVER:
                        exportTask = exportAsServer(exportInfo, dest);
                        break;
                    default:
                        throw new IllegalStateException("Unrecognized modpack type " + modpackType);
                }

            }

            @Override
            public Collection<Task<?>> getDependents() {
                return Collections.singleton(exportTask);
            }

            @Override
            public void execute() throws Exception {
                if (!packWithLauncher) return;
                try (Zipper zip = new Zipper(modpackFile.toPath())) {
                    Config exported = new Config();

                    exported.setBackgroundImageType(ConfigHolder.config().getBackgroundImageType());
                    exported.setBackgroundImage(ConfigHolder.config().getBackgroundImage());
                    exported.setTheme(ConfigHolder.config().getTheme());
                    exported.setDownloadType(ConfigHolder.config().getDownloadType());
                    exported.setPreferredLoginType(ConfigHolder.config().getPreferredLoginType());
                    exported.getAuthlibInjectorServers().setAll(ConfigHolder.config().getAuthlibInjectorServers());

                    zip.putTextFile(exported.toJson(), ConfigHolder.CONFIG_FILENAME);
                    zip.putFile(tempModpack, "modpack.zip");

                    File bg = new File("bg").getAbsoluteFile();
                    if (bg.isDirectory())
                        zip.putDirectory(bg.toPath(), "bg");

                    File background_png = new File("background.png").getAbsoluteFile();
                    if (background_png.isFile())
                        zip.putFile(background_png, "background.png");

                    File background_jpg = new File("background.jpg").getAbsoluteFile();
                    if (background_jpg.isFile())
                        zip.putFile(background_jpg, "background.jpg");

                    File background_gif = new File("background.gif").getAbsoluteFile();
                    if (background_gif.isFile())
                        zip.putFile(background_gif, "background.gif");

                    zip.putFile(launcherJar.get(), launcherJar.get().getFileName().toString());
                }
            }
        };
    }

    private Task<?> exportAsMcbbs(ModpackExportInfo exportInfo, File modpackFile) {
        return new Task<Void>() {
            Task<?> dependency = null;

            @Override
            public void execute() {
                dependency = new McbbsModpackExportTask(profile.getRepository(), version, exportInfo, modpackFile);
            }

            @Override
            public Collection<Task<?>> getDependencies() {
                return Collections.singleton(dependency);
            }
        };
    }

    private Task<?> exportAsMultiMC(ModpackExportInfo exportInfo, File modpackFile) {
        return new Task<Void>() {
            Task<?> dependency;

            @Override
            public void execute() {
                VersionSetting vs = profile.getVersionSetting(version);
                dependency = new MultiMCModpackExportTask(profile.getRepository(), version, exportInfo.getWhitelist(),
                        new MultiMCInstanceConfiguration(
                                "OneSix",
                                exportInfo.getName() + "-" + exportInfo.getVersion(),
                                null,
                                Lang.toIntOrNull(vs.getPermSize()),
                                vs.getWrapper(),
                                vs.getPreLaunchCommand(),
                                null,
                                exportInfo.getDescription(),
                                null,
                                exportInfo.getJavaArguments(),
                                vs.isFullscreen(),
                                vs.getWidth(),
                                vs.getHeight(),
                                vs.getMaxMemory(),
                                exportInfo.getMinMemory(),
                                vs.isShowLogs(),
                                /* showConsoleOnError */ true,
                                /* autoCloseConsole */ false,
                                /* overrideMemory */ true,
                                /* overrideJavaLocation */ false,
                                /* overrideJavaArgs */ true,
                                /* overrideConsole */ true,
                                /* overrideCommands */ true,
                                /* overrideWindow */ true
                        ), modpackFile);
            }

            @Override
            public Collection<Task<?>> getDependencies() {
                return Collections.singleton(dependency);
            }
        };
    }

    private Task<?> exportAsServer(ModpackExportInfo exportInfo, File modpackFile) {
        return new Task<Void>() {
            Task<?> dependency;

            @Override
            public void execute() {
                dependency = new ServerModpackExportTask(profile.getRepository(), version, exportInfo, modpackFile);
            }

            @Override
            public Collection<Task<?>> getDependencies() {
                return Collections.singleton(dependency);
            }
        };
    }

    @Override
    public Node createPage(WizardController controller, int step, Map<String, Object> settings) {
        switch (step) {
            case 0:
                return new ModpackTypeSelectionPage(controller);
            case 1:
                return new ModpackInfoPage(controller, profile.getRepository(), version);
            case 2:
                return new ModpackFileSelectionPage(controller, profile, version, ModAdviser::suggestMod);
            default:
                throw new IllegalArgumentException("step");
        }
    }

    @Override
    public boolean cancel() {
        return true;
    }
}
