package cn.ChengZhiYa.BaiShenLauncher.util;

import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;

public final class HMCLService {
    private HMCLService() {
    }

    public static void openRedirectLink(String id) {
        FXUtils.openLink("https://hmcl.huangyuhui.net/api/redirect/" + id);
    }
}
