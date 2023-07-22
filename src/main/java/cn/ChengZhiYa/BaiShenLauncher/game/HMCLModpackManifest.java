package cn.ChengZhiYa.BaiShenLauncher.game;

import cn.ChengZhiYa.BaiShenLauncher.mod.ModpackManifest;
import cn.ChengZhiYa.BaiShenLauncher.mod.ModpackProvider;

public final class HMCLModpackManifest implements ModpackManifest {
    public static final HMCLModpackManifest INSTANCE = new HMCLModpackManifest();

    private HMCLModpackManifest() {
    }

    @Override
    public ModpackProvider getProvider() {
        return HMCLModpackProvider.INSTANCE;
    }
}
