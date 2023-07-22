package cn.ChengZhiYa.BaiShenLauncher.game;

import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.util.io.CompressingUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.Unzipper;

import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ManuallyCreatedModpackInstallTask extends Task<Path> {

    private final Profile profile;
    private final Path zipFile;
    private final Charset charset;
    private final String name;

    public ManuallyCreatedModpackInstallTask(Profile profile, Path zipFile, Charset charset, String name) {
        this.profile = profile;
        this.zipFile = zipFile;
        this.charset = charset;
        this.name = name;
    }

    @Override
    public void execute() throws Exception {
        Path subdirectory;
        try (FileSystem fs = CompressingUtils.readonly(zipFile).setEncoding(charset).build()) {
            subdirectory = ModpackHelper.findMinecraftDirectoryInManuallyCreatedModpack(zipFile.toString(), fs);
        }

        Path dest = Paths.get("externalgames").resolve(name);

        setResult(dest);

        new Unzipper(zipFile, dest)
                .setSubDirectory(subdirectory.toString())
                .setTerminateIfSubDirectoryNotExists()
                .setEncoding(charset)
                .unzip();
    }
}
