package cn.ChengZhiYa.BaiShenLauncher.util.i18n;

import cn.ChengZhiYa.BaiShenLauncher.setting.ConfigHolder;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.Locales.SupportedLocale;

import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;

public final class I18n {

    private I18n() {
    }

    public static SupportedLocale getCurrentLocale() {
        try {
            return ConfigHolder.config().getLocalization();
        } catch (IllegalStateException e) {
            // e is thrown by ConfigHolder.config(), indicating the config hasn't been loaded
            // fallback to use default locale
            return Locales.DEFAULT;
        }
    }

    public static ResourceBundle getResourceBundle() {
        return getCurrentLocale().getResourceBundle();
    }

    public static String i18n(String key, Object... formatArgs) {
        try {
            return String.format(getResourceBundle().getString(key), formatArgs);
        } catch (MissingResourceException e) {
            Logging.LOG.log(Level.SEVERE, "Cannot find key " + key + " in resource bundle", e);
        } catch (IllegalFormatException e) {
            Logging.LOG.log(Level.SEVERE, "Illegal format string, key=" + key + ", args=" + Arrays.toString(formatArgs), e);
        }

        return key + Arrays.toString(formatArgs);
    }

    public static String i18n(String key) {
        try {
            return getResourceBundle().getString(key);
        } catch (MissingResourceException e) {
            Logging.LOG.log(Level.SEVERE, "Cannot find key " + key + " in resource bundle", e);
            return key;
        }
    }

    public static boolean hasKey(String key) {
        return getResourceBundle().containsKey(key);
    }
}
