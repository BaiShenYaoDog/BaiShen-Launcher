package cn.ChengZhiYa.BaiShenLauncher.setting;

import cn.ChengZhiYa.BaiShenLauncher.Metadata;
import cn.ChengZhiYa.BaiShenLauncher.auth.authlibinjector.AuthlibInjectorServer;
import cn.ChengZhiYa.BaiShenLauncher.util.gson.EnumOrdinalDeserializer;
import cn.ChengZhiYa.BaiShenLauncher.util.gson.FileTypeAdapter;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.Locales;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.Locales.SupportedLocale;
import cn.ChengZhiYa.BaiShenLauncher.util.javafx.ObservableHelper;
import cn.ChengZhiYa.BaiShenLauncher.util.javafx.PropertyUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import org.hildan.fxgson.creators.ObservableListCreator;
import org.hildan.fxgson.creators.ObservableMapCreator;
import org.hildan.fxgson.creators.ObservableSetCreator;
import org.hildan.fxgson.factories.JavaFxPropertyTypeAdapterFactory;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.Proxy;
import java.util.Map;
import java.util.TreeMap;

public final class Config implements Cloneable, Observable {

    public static final int CURRENT_UI_VERSION = 0;

    public static final Gson CONFIG_GSON = new GsonBuilder()
            .registerTypeAdapter(File.class, FileTypeAdapter.INSTANCE)
            .registerTypeAdapter(ObservableList.class, new ObservableListCreator())
            .registerTypeAdapter(ObservableSet.class, new ObservableSetCreator())
            .registerTypeAdapter(ObservableMap.class, new ObservableMapCreator())
            .registerTypeAdapterFactory(new JavaFxPropertyTypeAdapterFactory(true, true))
            .registerTypeAdapter(EnumBackgroundImage.class, new EnumOrdinalDeserializer<>(EnumBackgroundImage.class)) // backward compatibility for backgroundType
            .registerTypeAdapter(Proxy.Type.class, new EnumOrdinalDeserializer<>(Proxy.Type.class)) // backward compatibility for hasProxy
            .setPrettyPrinting()
            .create();
    @SerializedName("last")
    private StringProperty selectedProfile = new SimpleStringProperty("");
    @SerializedName("backgroundType")
    private ObjectProperty<EnumBackgroundImage> backgroundImageType = new SimpleObjectProperty<>(EnumBackgroundImage.JKOP);
    @SerializedName("bgpath")
    private StringProperty backgroundImage = new SimpleStringProperty();
    @SerializedName("bgurl")
    private StringProperty backgroundImageUrl = new SimpleStringProperty();
    @SerializedName("commonDirType")
    private ObjectProperty<EnumCommonDirectory> commonDirType = new SimpleObjectProperty<>(EnumCommonDirectory.DEFAULT);
    @SerializedName("commonpath")
    private StringProperty commonDirectory = new SimpleStringProperty(Metadata.MINECRAFT_DIRECTORY.toString());
    @SerializedName("hasProxy")
    private BooleanProperty hasProxy = new SimpleBooleanProperty();
    @SerializedName("hasProxyAuth")
    private BooleanProperty hasProxyAuth = new SimpleBooleanProperty();
    @SerializedName("proxyType")
    private ObjectProperty<Proxy.Type> proxyType = new SimpleObjectProperty<>(Proxy.Type.HTTP);
    @SerializedName("proxyHost")
    private StringProperty proxyHost = new SimpleStringProperty();
    @SerializedName("proxyPort")
    private IntegerProperty proxyPort = new SimpleIntegerProperty();
    @SerializedName("proxyUserName")
    private StringProperty proxyUser = new SimpleStringProperty();
    @SerializedName("proxyPassword")
    private StringProperty proxyPass = new SimpleStringProperty();
    @SerializedName("width")
    private DoubleProperty width = new SimpleDoubleProperty(1100);
    @SerializedName("height")
    private DoubleProperty height = new SimpleDoubleProperty(650);
    @SerializedName("theme")
    private ObjectProperty<Theme> theme = new SimpleObjectProperty<>();
    @SerializedName("localization")
    private ObjectProperty<SupportedLocale> localization = new SimpleObjectProperty<>(Locales.DEFAULT);
    @SerializedName("autoDownloadThreads")
    private BooleanProperty autoDownloadThreads = new SimpleBooleanProperty(false);
    @SerializedName("downloadThreads")
    private IntegerProperty downloadThreads = new SimpleIntegerProperty(64);
    @SerializedName("downloadType")
    private StringProperty downloadType = new SimpleStringProperty("mcbbs");
    @SerializedName("autoChooseDownloadType")
    private BooleanProperty autoChooseDownloadType = new SimpleBooleanProperty(true);
    @SerializedName("versionListSource")
    private StringProperty versionListSource = new SimpleStringProperty("balanced");
    @SerializedName("configurations")
    private SimpleMapProperty<String, Profile> configurations = new SimpleMapProperty<>(FXCollections.observableMap(new TreeMap<>()));
    @SerializedName("selectedAccount")
    private StringProperty selectedAccount = new SimpleStringProperty();
    @SerializedName("accounts")
    private ObservableList<Map<Object, Object>> accountStorages = FXCollections.observableArrayList();
    @SerializedName("fontFamily")
    private StringProperty fontFamily = new SimpleStringProperty();
    @SerializedName("fontSize")
    private DoubleProperty fontSize = new SimpleDoubleProperty(12);
    @SerializedName("launcherFontFamily")
    private StringProperty launcherFontFamily = new SimpleStringProperty();
    @SerializedName("logLines")
    private IntegerProperty logLines = new SimpleIntegerProperty(100);
    @SerializedName("titleTransparent")
    private BooleanProperty titleTransparent = new SimpleBooleanProperty(true);
    @SerializedName("authlibInjectorServers")
    private ObservableList<AuthlibInjectorServer> authlibInjectorServers = FXCollections.observableArrayList(server -> new Observable[]{server});
    @SerializedName("addedLittleSkin")
    private BooleanProperty addedLittleSkin = new SimpleBooleanProperty(false);
    @SerializedName("promptedVersion")
    private StringProperty promptedVersion = new SimpleStringProperty();
    @SerializedName("_version")
    private IntegerProperty configVersion = new SimpleIntegerProperty(0);
    /**
     * The version of UI that the user have last used.
     * If there is a major change in UI, {@link Config#CURRENT_UI_VERSION} should be increased.
     * When {@link #CURRENT_UI_VERSION} is higher than the property, the user guide should be shown,
     * then this property is set to the same value as {@link #CURRENT_UI_VERSION}.
     * In particular, the property is default to 0, so that whoever open the application for the first time will see the guide.
     */
    @SerializedName("uiVersion")
    private IntegerProperty uiVersion = new SimpleIntegerProperty(0);
    /**
     * The preferred login type to use when the user wants to add an account.
     */
    @SerializedName("preferredLoginType")
    private StringProperty preferredLoginType = new SimpleStringProperty();
    @SerializedName("animationDisabled")
    private BooleanProperty animationDisabled = new SimpleBooleanProperty();
    @SerializedName("shownTips")
    private ObservableMap<String, Object> shownTips = FXCollections.observableHashMap();
    private transient ObservableHelper helper = new ObservableHelper(this);

    public Config() {
        PropertyUtils.attachListener(this, helper);
    }

    @Nullable
    public static Config fromJson(String json) throws JsonParseException {
        Config loaded = CONFIG_GSON.fromJson(json, Config.class);
        if (loaded == null) {
            return null;
        }
        Config instance = new Config();
        PropertyUtils.copyProperties(loaded, instance);
        return instance;
    }

    @Override
    public void addListener(InvalidationListener listener) {
        helper.addListener(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        helper.removeListener(listener);
    }

    public String toJson() {
        return CONFIG_GSON.toJson(this);
    }

    @Override
    public Config clone() {
        return fromJson(this.toJson());
    }

    // Getters & Setters & Properties
    public String getSelectedProfile() {
        return selectedProfile.get();
    }

    public void setSelectedProfile(String selectedProfile) {
        this.selectedProfile.set(selectedProfile);
    }

    public StringProperty selectedProfileProperty() {
        return selectedProfile;
    }

    public EnumBackgroundImage getBackgroundImageType() {
        return backgroundImageType.get();
    }

    public void setBackgroundImageType(EnumBackgroundImage backgroundImageType) {
        this.backgroundImageType.set(backgroundImageType);
    }

    public ObjectProperty<EnumBackgroundImage> backgroundImageTypeProperty() {
        return backgroundImageType;
    }

    public String getBackgroundImage() {
        return backgroundImage.get();
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage.set(backgroundImage);
    }

    public StringProperty backgroundImageProperty() {
        return backgroundImage;
    }

    public String getBackgroundImageUrl() {
        return backgroundImageUrl.get();
    }

    public void setBackgroundImageUrl(String backgroundImageUrl) {
        this.backgroundImageUrl.set(backgroundImageUrl);
    }

    public StringProperty backgroundImageUrlProperty() {
        return backgroundImageUrl;
    }

    public EnumCommonDirectory getCommonDirType() {
        return commonDirType.get();
    }

    public void setCommonDirType(EnumCommonDirectory commonDirType) {
        this.commonDirType.set(commonDirType);
    }

    public ObjectProperty<EnumCommonDirectory> commonDirTypeProperty() {
        return commonDirType;
    }

    public String getCommonDirectory() {
        return commonDirectory.get();
    }

    public void setCommonDirectory(String commonDirectory) {
        this.commonDirectory.set(commonDirectory);
    }

    public StringProperty commonDirectoryProperty() {
        return commonDirectory;
    }

    public boolean hasProxy() {
        return hasProxy.get();
    }

    public void setHasProxy(boolean hasProxy) {
        this.hasProxy.set(hasProxy);
    }

    public BooleanProperty hasProxyProperty() {
        return hasProxy;
    }

    public boolean hasProxyAuth() {
        return hasProxyAuth.get();
    }

    public void setHasProxyAuth(boolean hasProxyAuth) {
        this.hasProxyAuth.set(hasProxyAuth);
    }

    public BooleanProperty hasProxyAuthProperty() {
        return hasProxyAuth;
    }

    public Proxy.Type getProxyType() {
        return proxyType.get();
    }

    public void setProxyType(Proxy.Type proxyType) {
        this.proxyType.set(proxyType);
    }

    public ObjectProperty<Proxy.Type> proxyTypeProperty() {
        return proxyType;
    }

    public String getProxyHost() {
        return proxyHost.get();
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost.set(proxyHost);
    }

    public StringProperty proxyHostProperty() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort.get();
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort.set(proxyPort);
    }

    public IntegerProperty proxyPortProperty() {
        return proxyPort;
    }

    public String getProxyUser() {
        return proxyUser.get();
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser.set(proxyUser);
    }

    public StringProperty proxyUserProperty() {
        return proxyUser;
    }

    public String getProxyPass() {
        return proxyPass.get();
    }

    public void setProxyPass(String proxyPass) {
        this.proxyPass.set(proxyPass);
    }

    public StringProperty proxyPassProperty() {
        return proxyPass;
    }

    public double getWidth() {
        return width.get();
    }

    public void setWidth(double width) {
        this.width.set(width);
    }

    public DoubleProperty widthProperty() {
        return width;
    }

    public double getHeight() {
        return height.get();
    }

    public void setHeight(double height) {
        this.height.set(height);
    }

    public DoubleProperty heightProperty() {
        return height;
    }

    public Theme getTheme() {
        return theme.get();
    }

    public void setTheme(Theme theme) {
        this.theme.set(theme);
    }

    public ObjectProperty<Theme> themeProperty() {
        return theme;
    }

    public SupportedLocale getLocalization() {
        return localization.get();
    }

    public void setLocalization(SupportedLocale localization) {
        this.localization.set(localization);
    }

    public ObjectProperty<SupportedLocale> localizationProperty() {
        return localization;
    }

    public boolean getAutoDownloadThreads() {
        return autoDownloadThreads.get();
    }

    public void setAutoDownloadThreads(boolean autoDownloadThreads) {
        this.autoDownloadThreads.set(autoDownloadThreads);
    }

    public BooleanProperty autoDownloadThreadsProperty() {
        return autoDownloadThreads;
    }

    public int getDownloadThreads() {
        return downloadThreads.get();
    }

    public void setDownloadThreads(int downloadThreads) {
        this.downloadThreads.set(downloadThreads);
    }

    public IntegerProperty downloadThreadsProperty() {
        return downloadThreads;
    }

    public String getDownloadType() {
        return downloadType.get();
    }

    public void setDownloadType(String downloadType) {
        this.downloadType.set(downloadType);
    }

    public StringProperty downloadTypeProperty() {
        return downloadType;
    }

    public boolean isAutoChooseDownloadType() {
        return autoChooseDownloadType.get();
    }

    public void setAutoChooseDownloadType(boolean autoChooseDownloadType) {
        this.autoChooseDownloadType.set(autoChooseDownloadType);
    }

    public BooleanProperty autoChooseDownloadTypeProperty() {
        return autoChooseDownloadType;
    }

    public String getVersionListSource() {
        return versionListSource.get();
    }

    public void setVersionListSource(String versionListSource) {
        this.versionListSource.set(versionListSource);
    }

    public StringProperty versionListSourceProperty() {
        return versionListSource;
    }

    public MapProperty<String, Profile> getConfigurations() {
        return configurations;
    }

    public String getSelectedAccount() {
        return selectedAccount.get();
    }

    public void setSelectedAccount(String selectedAccount) {
        this.selectedAccount.set(selectedAccount);
    }

    public StringProperty selectedAccountProperty() {
        return selectedAccount;
    }

    public ObservableList<Map<Object, Object>> getAccountStorages() {
        return accountStorages;
    }

    public String getFontFamily() {
        return fontFamily.get();
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily.set(fontFamily);
    }

    public StringProperty fontFamilyProperty() {
        return fontFamily;
    }

    public double getFontSize() {
        return fontSize.get();
    }

    public void setFontSize(double fontSize) {
        this.fontSize.set(fontSize);
    }

    public DoubleProperty fontSizeProperty() {
        return fontSize;
    }

    public String getLauncherFontFamily() {
        return launcherFontFamily.get();
    }

    public void setLauncherFontFamily(String launcherFontFamily) {
        this.launcherFontFamily.set(launcherFontFamily);
    }

    public StringProperty launcherFontFamilyProperty() {
        return launcherFontFamily;
    }

    public int getLogLines() {
        return logLines.get();
    }

    public void setLogLines(int logLines) {
        this.logLines.set(logLines);
    }

    public IntegerProperty logLinesProperty() {
        return logLines;
    }

    public ObservableList<AuthlibInjectorServer> getAuthlibInjectorServers() {
        return authlibInjectorServers;
    }

    public boolean isAddedLittleSkin() {
        return addedLittleSkin.get();
    }

    public void setAddedLittleSkin(boolean addedLittleSkin) {
        this.addedLittleSkin.set(addedLittleSkin);
    }

    public BooleanProperty addedLittleSkinProperty() {
        return addedLittleSkin;
    }

    public int getConfigVersion() {
        return configVersion.get();
    }

    public void setConfigVersion(int configVersion) {
        this.configVersion.set(configVersion);
    }

    public IntegerProperty configVersionProperty() {
        return configVersion;
    }

    public int getUiVersion() {
        return uiVersion.get();
    }

    public void setUiVersion(int uiVersion) {
        this.uiVersion.set(uiVersion);
    }

    public IntegerProperty uiVersionProperty() {
        return uiVersion;
    }

    public String getPreferredLoginType() {
        return preferredLoginType.get();
    }

    public void setPreferredLoginType(String preferredLoginType) {
        this.preferredLoginType.set(preferredLoginType);
    }

    public StringProperty preferredLoginTypeProperty() {
        return preferredLoginType;
    }

    public boolean isAnimationDisabled() {
        return animationDisabled.get();
    }

    public void setAnimationDisabled(boolean animationDisabled) {
        this.animationDisabled.set(animationDisabled);
    }

    public BooleanProperty animationDisabledProperty() {
        return animationDisabled;
    }

    public boolean isTitleTransparent() {
        return titleTransparent.get();
    }

    public void setTitleTransparent(boolean titleTransparent) {
        this.titleTransparent.set(titleTransparent);
    }

    public BooleanProperty titleTransparentProperty() {
        return titleTransparent;
    }

    public String getPromptedVersion() {
        return promptedVersion.get();
    }

    public void setPromptedVersion(String promptedVersion) {
        this.promptedVersion.set(promptedVersion);
    }

    public StringProperty promptedVersionProperty() {
        return promptedVersion;
    }

    public ObservableMap<String, Object> getShownTips() {
        return shownTips;
    }
}
