package cn.ChengZhiYa.BaiShenLauncher.upgrade;

import cn.ChengZhiYa.BaiShenLauncher.task.FileDownloadTask;
import cn.ChengZhiYa.BaiShenLauncher.util.Pack200Utils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.NetworkUtils;
import org.tukaani.xz.XZInputStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;

class HMCLDownloadTask extends FileDownloadTask {

    private RemoteVersion.Type archiveFormat;

    public HMCLDownloadTask(RemoteVersion version, Path target) {
        super(NetworkUtils.toURL(version.getUrl()), target.toFile(), version.getIntegrityCheck());
        archiveFormat = version.getType();
    }

    @Override
    public void execute() throws Exception {
        super.execute();

        try {
            Path target = getFile().toPath();

            switch (archiveFormat) {
                case JAR:
                    break;

                case PACK_XZ:
                    byte[] raw = Files.readAllBytes(target);
                    try (InputStream in = new XZInputStream(new ByteArrayInputStream(raw));
                         JarOutputStream out = new JarOutputStream(Files.newOutputStream(target))) {
                        Pack200Utils.unpack(in, out);
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Unknown format: " + archiveFormat);
            }
        } catch (Throwable e) {
            getFile().delete();
            throw e;
        }
    }

}
