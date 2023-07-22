package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;

/**
 * @author yushijinhun
 * @see Controllers#dialog(javafx.scene.layout.Region)
 */
public interface DialogAware {

    default void onDialogShown() {
    }

    default void onDialogClosed() {
    }

}
