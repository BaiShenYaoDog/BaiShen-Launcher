package cn.ChengZhiYa.BaiShenLauncher.util.i18n;

import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.Lazy;
import cn.ChengZhiYa.BaiShenLauncher.util.platform.JavaVersion;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public final class Locales {
    public static final SupportedLocale DEFAULT = new SupportedLocale(Locale.getDefault(), "lang.default");
    /**
     * English
     */
    public static final SupportedLocale EN = new SupportedLocale(Locale.ROOT);
    /**
     * Traditional Chinese
     */
    public static final SupportedLocale ZH = new SupportedLocale(Locale.TRADITIONAL_CHINESE);
    /**
     * Simplified Chinese
     */
    public static final SupportedLocale ZH_CN = new SupportedLocale(Locale.SIMPLIFIED_CHINESE);
    /**
     * Spanish
     */
    public static final SupportedLocale ES = new SupportedLocale(Locale.forLanguageTag("es"));
    /**
     * Russian
     */
    public static final SupportedLocale RU = new SupportedLocale(Locale.forLanguageTag("ru"));
    /**
     * Japanese
     */
    public static final SupportedLocale JA = new SupportedLocale(Locale.JAPANESE);
    public static final List<SupportedLocale> LOCALES = Lang.immutableListOf(DEFAULT, EN, ZH_CN, ZH, ES, RU, JA);
    public static final Lazy<SimpleDateFormat> SIMPLE_DATE_FORMAT = new Lazy<>(() -> new SimpleDateFormat(I18n.i18n("world.time")));
    public static final Lazy<DateTimeFormatter> DATE_TIME_FORMATTER = new Lazy<>(() -> DateTimeFormatter.ofPattern(I18n.i18n("world.time")).withZone(ZoneId.systemDefault()));

    private Locales() {
    }

    public static SupportedLocale getLocaleByName(String name) {
        if (name == null) return DEFAULT;
        switch (name.toLowerCase(Locale.ROOT)) {
            case "en":
                return EN;
            case "zh":
                return ZH;
            case "zh_cn":
                return ZH_CN;
            case "es":
                return ES;
            case "ru":
                return RU;
            case "ja":
                return JA;
            default:
                return DEFAULT;
        }
    }

    public static String getNameByLocale(SupportedLocale locale) {
        if (locale == EN) return "en";
        else if (locale == ZH) return "zh";
        else if (locale == ZH_CN) return "zh_CN";
        else if (locale == ES) return "es";
        else if (locale == RU) return "ru";
        else if (locale == JA) return "ja";
        else if (locale == DEFAULT) return "def";
        else throw new IllegalArgumentException("Unknown locale: " + locale);
    }

    @JsonAdapter(SupportedLocale.TypeAdapter.class)
    public static class SupportedLocale {
        private final Locale locale;
        private final String name;
        private final ResourceBundle resourceBundle;

        SupportedLocale(Locale locale) {
            this(locale, null);
        }

        SupportedLocale(Locale locale, String name) {
            this.locale = locale;
            this.name = name;
            if (JavaVersion.CURRENT_JAVA.getParsedVersion() == JavaVersion.JAVA_8) {
                resourceBundle = ResourceBundle.getBundle("assets.lang.I18N", locale, UTF8Control.INSTANCE);
            } else {
                // UTF-8 is supported in Java 9+
                resourceBundle = ResourceBundle.getBundle("assets.lang.I18N", locale);
            }
        }

        public Locale getLocale() {
            return locale;
        }

        public ResourceBundle getResourceBundle() {
            return resourceBundle;
        }

        public String getName(ResourceBundle newResourceBundle) {
            if (name == null) return resourceBundle.getString("lang");
            else return newResourceBundle.getString(name);
        }

        public static class TypeAdapter extends com.google.gson.TypeAdapter<SupportedLocale> {
            @Override
            public void write(JsonWriter out, SupportedLocale value) throws IOException {
                out.value(getNameByLocale(value));
            }

            @Override
            public SupportedLocale read(JsonReader in) throws IOException {
                return getLocaleByName(in.nextString());
            }
        }
    }
}
