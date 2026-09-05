package com.zenith.udl.config.item;

import net.minecraft.network.chat.Component;

public enum ItemSettingModule {
    SERVER_ENTITY_MANAGER("Server Entity Manager", "ServerLevelのPersistentStorage/SectionStorage/EntityGetterをダミー化"),
    ENTITY_TICK_LIST("Entity Tick List", "ServerLevelのentityTickListを空のインスタンスに置換"),
    CLIENT_ENTITY_STORAGE("Client Entity Storage", "ClientLevelのTransientEntitySectionManagerをダミー化");

    private final String displayName;
    private final String description;

    ItemSettingModule(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public Component getDisplayName() {
        return Component.literal(displayName);
    }

    public Component getDescription() {
        return Component.literal(description);
    }
}