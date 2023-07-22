package cn.ChengZhiYa.BaiShenLauncher.setting;

public enum VersionIconType {
    DEFAULT("/assets/img/grass.png"),

    GRASS("/assets/img/grass.png"),
    CHEST("/assets/img/chest.png"),
    CHICKEN("/assets/img/chicken.png"),
    COMMAND("/assets/img/command.png"),
    CRAFT_TABLE("/assets/img/craft_table.png"),
    FABRIC("/assets/img/fabric.png"),
    FORGE("/assets/img/forge.png"),
    FURNACE("/assets/img/furnace.png"),
    QUILT("/assets/img/quilt.png");

    // Please append new items at last

    private final String resourceUrl;

    VersionIconType(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }
}
