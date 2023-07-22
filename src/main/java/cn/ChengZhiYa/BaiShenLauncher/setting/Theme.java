package cn.ChengZhiYa.BaiShenLauncher.setting;

import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.IOUtils;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonAdapter(Theme.TypeAdapter.class)
public class Theme {
    public static final Theme PINK = new Theme("pink", "#FF9999");
    public static final Color BLACK = Color.web("#292929");
    public static final Color[] SUGGESTED_COLORS = new Color[]{
            Color.web("#FF9999"),
            Color.web("#FF6666"),
            Color.web("#004EF2"),
            Color.web("#239CF3"),
            Color.web("#FFB366")
    };
    private static final ObjectBinding<Color> BLACK_FILL = Bindings.createObjectBinding(() -> BLACK);
    private static final ObjectBinding<Color> WHITE_FILL = Bindings.createObjectBinding(() -> Color.WHITE);
    private static Charset cssCharset;
    private static ObjectBinding<Color> FOREGROUND_FILL;
    private final Color paint;
    private final String color;
    private final String name;

    public Theme(String name, String color) {
        this.name = name;
        this.color = Objects.requireNonNull(color);
        this.paint = Color.web(color);
    }

    private static Charset getCssCharset() {
        if (cssCharset != null)
            return cssCharset;

        Charset defaultCharset = Charset.defaultCharset();
        if (defaultCharset != StandardCharsets.UTF_8) {
            // https://bugs.openjdk.org/browse/JDK-8279328
            // For JavaFX 17 or earlier, native encoding should be used
            String jfxVersion = System.getProperty("javafx.version");
            if (jfxVersion != null) {
                Matcher matcher = Pattern.compile("^(?<version>[0-9]+)").matcher(jfxVersion);
                if (matcher.find()) {
                    int v = Lang.parseInt(matcher.group(), -1);
                    if (v >= 18) {
                        cssCharset = StandardCharsets.UTF_8;
                    }
                }
            }
        }

        if (cssCharset == null)
            cssCharset = defaultCharset;

        return cssCharset;
    }

    public static Theme getTheme() {
        Theme theme = ConfigHolder.config().getTheme();
        return theme == null ? PINK : theme;
    }

    public static Theme custom(String color) {
        if (!color.startsWith("#"))
            throw new IllegalArgumentException();
        return new Theme(color, color);
    }

    public static Optional<Theme> getTheme(String name) {
        if (name == null)
            return Optional.empty();
        else if (name.startsWith("#"))
            try {
                Color.web(name);
                return Optional.of(custom(name));
            } catch (IllegalArgumentException ignore) {
            }
        else {
            if (name.toLowerCase(Locale.ROOT).equals("blue")) {
                return Optional.of(PINK);
            }
        }

        return Optional.empty();
    }

    public static String getColorDisplayName(Color c) {
        return c != null ? String.format("#%02x%02x%02x", Math.round(c.getRed() * 255.0D), Math.round(c.getGreen() * 255.0D), Math.round(c.getBlue() * 255.0D)).toUpperCase(Locale.ROOT) : null;
    }

    public static ObjectBinding<Color> foregroundFillBinding() {
        if (FOREGROUND_FILL == null)
            FOREGROUND_FILL = Bindings.createObjectBinding(
                    () -> Theme.getTheme().getForegroundColor(),
                    ConfigHolder.config().themeProperty()
            );

        return FOREGROUND_FILL;
    }

    public static ObjectBinding<Color> blackFillBinding() {
        return BLACK_FILL;
    }

    public static ObjectBinding<Color> whiteFillBinding() {
        return WHITE_FILL;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public boolean isCustom() {
        return name.startsWith("#");
    }

    public boolean isLight() {
        return paint.grayscale().getRed() >= 0.5;
    }

    public Color getForegroundColor() {
        return isLight() ? Color.BLACK : Color.WHITE;
    }

    public String[] getStylesheets(String overrideFontFamily) {
        String css = "/assets/css/pink.css";

        String fontFamily = System.getProperty("hmcl.font.override", overrideFontFamily);

        if (fontFamily != null || !this.color.equalsIgnoreCase(PINK.color)) {
            Color textFill = getForegroundColor();
            try {
                File temp = File.createTempFile("hmcl", ".css");
                String themeText = IOUtils.readFullyAsString(Theme.class.getResourceAsStream("/assets/css/custom.css"))
                        .replace("%base-color%", color)
                        .replace("%base-red%", Integer.toString((int) Math.ceil(paint.getRed() * 256)))
                        .replace("%base-green%", Integer.toString((int) Math.ceil(paint.getGreen() * 256)))
                        .replace("%base-blue%", Integer.toString((int) Math.ceil(paint.getBlue() * 256)))
                        .replace("%base-rippler-color%", String.format("rgba(%d, %d, %d, 0.3)", (int) Math.ceil(paint.getRed() * 256), (int) Math.ceil(paint.getGreen() * 256), (int) Math.ceil(paint.getBlue() * 256)))
                        .replace("%disabled-font-color%", String.format("rgba(%d, %d, %d, 0.7)", (int) Math.ceil(textFill.getRed() * 256), (int) Math.ceil(textFill.getGreen() * 256), (int) Math.ceil(textFill.getBlue() * 256)))
                        .replace("%font-color%", getColorDisplayName(getForegroundColor()))
                        .replace("%font%", Optional.ofNullable(fontFamily).map(f -> "-fx-font-family: \"" + f + "\";").orElse(""));
                FileUtils.writeText(temp, themeText, getCssCharset());
                temp.deleteOnExit();
                css = temp.toURI().toString();
            } catch (IOException | NullPointerException e) {
                Logging.LOG.log(Level.SEVERE, "Unable to create theme stylesheet. Fallback to blue theme.", e);
            }
        }

        return new String[]{css, "/assets/css/root.css"};
    }

    public static class TypeAdapter extends com.google.gson.TypeAdapter<Theme> {
        @Override
        public void write(JsonWriter out, Theme value) throws IOException {
            out.value(value.getName().toLowerCase(Locale.ROOT));
        }

        @Override
        public Theme read(JsonReader in) throws IOException {
            return getTheme(in.nextString()).orElse(Theme.PINK);
        }
    }
}
