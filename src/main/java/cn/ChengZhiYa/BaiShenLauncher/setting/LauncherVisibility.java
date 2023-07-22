package cn.ChengZhiYa.BaiShenLauncher.setting;

/**
 * The visibility of launcher. 
 */
public enum LauncherVisibility {

    /**
     * Close the launcher anyway when the game process created even if failed to
     * launch game.
     */
    CLOSE,

    /**
     * Hide the launcher when the game process created, if failed to launch
     * game, will show the log window.
     */
    HIDE,

    /**
     * Keep the launcher visible even if the game launched successfully.
     */
    KEEP,

    /**
     * Hide the launcher and reopen it when game closes.
     */
    HIDE_AND_REOPEN;

    public boolean isDaemon() {
        return this != CLOSE;
    }
}
