package cn.ChengZhiYa.BaiShenLauncher.upgrade;

import cn.ChengZhiYa.BaiShenLauncher.Metadata;

public enum UpdateChannel {
    STABLE("stable"),
    DEVELOPMENT("dev"),
    NIGHTLY("nightly");

    public final String channelName;

    UpdateChannel(String channelName) {
        this.channelName = channelName;
    }

    public static UpdateChannel getChannel() {
        if (Metadata.isDev()) {
            return DEVELOPMENT;
        } else if (Metadata.isNightly()) {
            return NIGHTLY;
        } else {
            return STABLE;
        }
    }
}
