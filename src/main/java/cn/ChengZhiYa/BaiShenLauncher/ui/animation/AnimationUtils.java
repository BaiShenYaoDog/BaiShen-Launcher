package cn.ChengZhiYa.BaiShenLauncher.ui.animation;

import cn.ChengZhiYa.BaiShenLauncher.setting.ConfigHolder;
import cn.ChengZhiYa.BaiShenLauncher.setting.Settings;

public final class AnimationUtils {

    private static final boolean enabled = !ConfigHolder.config().isAnimationDisabled();

    private AnimationUtils() {
    }

    /**
     * Trigger initialization of this class.
     * Should be called from {@link Settings#init()}.
     */
    @SuppressWarnings("JavadocReference")
    public static void init() {
    }

    public static boolean isAnimationEnabled() {
        return enabled;
    }
}
