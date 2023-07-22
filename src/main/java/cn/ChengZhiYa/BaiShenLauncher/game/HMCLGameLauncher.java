package cn.ChengZhiYa.BaiShenLauncher.game;

import cn.ChengZhiYa.BaiShenLauncher.Metadata;
import cn.ChengZhiYa.BaiShenLauncher.auth.AuthInfo;
import cn.ChengZhiYa.BaiShenLauncher.launch.DefaultLauncher;
import cn.ChengZhiYa.BaiShenLauncher.launch.ProcessListener;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.ManagedProcess;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
 
public final class HMCLGameLauncher extends DefaultLauncher {

    public HMCLGameLauncher(GameRepository repository, Version version, AuthInfo authInfo, LaunchOptions options) {
        this(repository, version, authInfo, options, null);
    }

    public HMCLGameLauncher(GameRepository repository, Version version, AuthInfo authInfo, LaunchOptions options, ProcessListener listener) {
        this(repository, version, authInfo, options, listener, true);
    }

    public HMCLGameLauncher(GameRepository repository, Version version, AuthInfo authInfo, LaunchOptions options, ProcessListener listener, boolean daemon) {
        super(repository, version, authInfo, options, listener, daemon);
    }

    @Override
    protected Map<String, String> getConfigurations() {
        Map<String, String> res = super.getConfigurations();
        res.put("${launcher_name}", Metadata.NAME);
        res.put("${launcher_version}", Metadata.VERSION);
        return res;
    }

    private void generateOptionsTxt() {
        File optionsFile = new File(repository.getRunDirectory(version.getId()), "options.txt");
        File configFolder = new File(repository.getRunDirectory(version.getId()), "config");

        if (optionsFile.exists())
            return;
        if (configFolder.isDirectory())
            if (findFiles(configFolder, "options.txt"))
                return;
        try {
            // TODO: Dirty implementation here
            if (I18n.getCurrentLocale().getLocale() == Locale.CHINA)
                FileUtils.writeText(optionsFile, "lang:zh_CN\nforceUnicodeFont:true\n");
        } catch (IOException e) {
            Logging.LOG.log(Level.WARNING, "Unable to generate options.txt", e);
        }
    }

    private boolean findFiles(File folder, String fileName) {
        File[] fs = folder.listFiles();
        if (fs != null) {
            for (File f : fs) {
                if (f.isDirectory())
                    if (f.listFiles((dir, name) -> name.equals(fileName)) != null)
                        return true;
                if (f.getName().equals(fileName))
                    return true;
            }
        }
        return false;
    }

    @Override
    public ManagedProcess launch() throws IOException, InterruptedException {
        generateOptionsTxt();
        return super.launch();
    }

    @Override
    public void makeLaunchScript(File scriptFile) throws IOException {
        generateOptionsTxt();
        super.makeLaunchScript(scriptFile);
    }
}
