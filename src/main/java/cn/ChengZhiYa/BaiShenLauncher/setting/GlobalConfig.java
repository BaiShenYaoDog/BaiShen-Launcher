package cn.ChengZhiYa.BaiShenLauncher.setting;

import cn.ChengZhiYa.BaiShenLauncher.util.javafx.ObservableHelper;
import cn.ChengZhiYa.BaiShenLauncher.util.javafx.PropertyUtils;
import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;

@JsonAdapter(GlobalConfig.Serializer.class)
public class GlobalConfig implements Cloneable, Observable {

    private final Map<String, Object> unknownFields = new HashMap<>();
    private IntegerProperty agreementVersion = new SimpleIntegerProperty();

    private IntegerProperty platformPromptVersion = new SimpleIntegerProperty();

    private BooleanProperty multiplayerRelay = new SimpleBooleanProperty();

    private IntegerProperty multiplayerAgreementVersion = new SimpleIntegerProperty(0);
    private transient ObservableHelper helper = new ObservableHelper(this);

    public GlobalConfig() {
        PropertyUtils.attachListener(this, helper);
    }

    @Nullable
    public static GlobalConfig fromJson(String json) throws JsonParseException {
        GlobalConfig loaded = Config.CONFIG_GSON.fromJson(json, GlobalConfig.class);
        if (loaded == null) {
            return null;
        }
        GlobalConfig instance = new GlobalConfig();
        PropertyUtils.copyProperties(loaded, instance);
        instance.unknownFields.putAll(loaded.unknownFields);
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
        return Config.CONFIG_GSON.toJson(this);
    }

    @Override
    public GlobalConfig clone() {
        return fromJson(this.toJson());
    }

    public int getAgreementVersion() {
        return agreementVersion.get();
    }

    public void setAgreementVersion(int agreementVersion) {
        this.agreementVersion.set(agreementVersion);
    }

    public IntegerProperty agreementVersionProperty() {
        return agreementVersion;
    }

    public int getPlatformPromptVersion() {
        return platformPromptVersion.get();
    }

    public void setPlatformPromptVersion(int platformPromptVersion) {
        this.platformPromptVersion.set(platformPromptVersion);
    }

    public IntegerProperty platformPromptVersionProperty() {
        return platformPromptVersion;
    }

    public boolean isMultiplayerRelay() {
        return multiplayerRelay.get();
    }

    public void setMultiplayerRelay(boolean multiplayerRelay) {
        this.multiplayerRelay.set(multiplayerRelay);
    }

    public BooleanProperty multiplayerRelayProperty() {
        return multiplayerRelay;
    }

    public int getMultiplayerAgreementVersion() {
        return multiplayerAgreementVersion.get();
    }

    public void setMultiplayerAgreementVersion(int multiplayerAgreementVersion) {
        this.multiplayerAgreementVersion.set(multiplayerAgreementVersion);
    }

    public IntegerProperty multiplayerAgreementVersionProperty() {
        return multiplayerAgreementVersion;
    }

    public static class Serializer implements JsonSerializer<GlobalConfig>, JsonDeserializer<GlobalConfig> {
        private static final Set<String> knownFields = new HashSet<>(Arrays.asList(
                "agreementVersion",
                "platformPromptVersion",
                "multiplayerToken",
                "multiplayerRelay",
                "multiplayerAgreementVersion"
        ));

        @Override
        public JsonElement serialize(GlobalConfig src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return JsonNull.INSTANCE;
            }

            JsonObject jsonObject = new JsonObject();
            jsonObject.add("agreementVersion", context.serialize(src.getAgreementVersion()));
            jsonObject.add("platformPromptVersion", context.serialize(src.getPlatformPromptVersion()));
            jsonObject.add("multiplayerRelay", context.serialize(src.isMultiplayerRelay()));
            jsonObject.add("multiplayerAgreementVersion", context.serialize(src.getMultiplayerAgreementVersion()));
            for (Map.Entry<String, Object> entry : src.unknownFields.entrySet()) {
                jsonObject.add(entry.getKey(), context.serialize(entry.getValue()));
            }

            return jsonObject;
        }

        @Override
        public GlobalConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!(json instanceof JsonObject)) return null;

            JsonObject obj = (JsonObject) json;

            GlobalConfig config = new GlobalConfig();
            config.setAgreementVersion(Optional.ofNullable(obj.get("agreementVersion")).map(JsonElement::getAsInt).orElse(0));
            config.setPlatformPromptVersion(Optional.ofNullable(obj.get("platformPromptVersion")).map(JsonElement::getAsInt).orElse(0));
            config.setMultiplayerRelay(Optional.ofNullable(obj.get("multiplayerRelay")).map(JsonElement::getAsBoolean).orElse(false));
            config.setMultiplayerAgreementVersion(Optional.ofNullable(obj.get("multiplayerAgreementVersion")).map(JsonElement::getAsInt).orElse(0));

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (!knownFields.contains(entry.getKey())) {
                    config.unknownFields.put(entry.getKey(), context.deserialize(entry.getValue(), Object.class));
                }
            }

            return config;
        }
    }
}
