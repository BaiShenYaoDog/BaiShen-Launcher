package cn.ChengZhiYa.BaiShenLauncher.setting;

import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

final class ConfigUpgrader {
    private static final int VERSION = 0;

    private ConfigUpgrader() {
    }

    /**
     * This method is for the compatibility with old HMCL 3.x as well as HMCL 2.x.
     *
     * @param deserialized deserialized config settings
     * @param rawJson      raw json structure of the config settings without modification
     * @return true if config version is upgraded
     */
    static boolean upgradeConfig(Config deserialized, Map<?, ?> rawJson) {
        boolean upgraded;
        if (deserialized.getConfigVersion() < VERSION) {
            deserialized.setConfigVersion(VERSION);
            // TODO: Add upgrade code here.
            upgraded = true;
        } else {
            upgraded = false;
        }

        upgradeV2(deserialized, rawJson);
        upgradeV3(deserialized, rawJson);

        return upgraded;
    }

    /**
     * Upgrade configuration of HMCL 2.x
     *
     * @param deserialized deserialized config settings
     * @param rawJson      raw json structure of the config settings without modification
     */
    private static void upgradeV2(Config deserialized, Map<?, ?> rawJson) {
        // Convert OfflineAccounts whose stored uuid is important.
        Lang.tryCast(rawJson.get("auth"), Map.class).ifPresent(auth -> {
            Lang.tryCast(auth.get("offline"), Map.class).ifPresent(offline -> {
                String selected = rawJson.containsKey("selectedAccount") ? null
                        : Lang.tryCast(offline.get("IAuthenticator_UserName"), String.class).orElse(null);

                Lang.tryCast(offline.get("uuidMap"), Map.class).ifPresent(uuidMap -> {
                    ((Map<?, ?>) uuidMap).forEach((key, value) -> {
                        Map<Object, Object> storage = new HashMap<>();
                        storage.put("type", "offline");
                        storage.put("username", key);
                        storage.put("uuid", value);
                        if (key.equals(selected)) {
                            storage.put("selected", true);
                        }
                        deserialized.getAccountStorages().add(storage);
                    });
                });
            });
        });
    }

    /**
     * Upgrade configuration of HMCL earlier than 3.1.70
     *
     * @param deserialized deserialized config settings
     * @param rawJson      raw json structure of the config settings without modification
     */
    private static void upgradeV3(Config deserialized, Map<?, ?> rawJson) {
        if (!rawJson.containsKey("commonDirType"))
            deserialized.setCommonDirType(deserialized.getCommonDirectory().equals(Settings.getDefaultCommonDirectory()) ? EnumCommonDirectory.DEFAULT : EnumCommonDirectory.CUSTOM);
        if (!rawJson.containsKey("backgroundType")) {
            deserialized.setBackgroundImageType(StringUtils.isNotBlank(deserialized.getBackgroundImage()) ? EnumBackgroundImage.CUSTOM : EnumBackgroundImage.JKOP);
        }
        if (!rawJson.containsKey("hasProxy"))
            deserialized.setHasProxy(StringUtils.isNotBlank(deserialized.getProxyHost()));
        if (!rawJson.containsKey("hasProxyAuth"))
            deserialized.setHasProxyAuth(StringUtils.isNotBlank(deserialized.getProxyUser()));

        if (!rawJson.containsKey("downloadType")) {
            Lang.tryCast(rawJson.get("downloadtype"), Number.class)
                    .map(Number::intValue)
                    .ifPresent(id -> {
                        if (id == 0) {
                            deserialized.setDownloadType("mojang");
                        } else if (id == 1) {
                            deserialized.setDownloadType("bmclapi");
                        }
                    });
        }
    }
}
