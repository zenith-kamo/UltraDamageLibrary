package com.zenith.udl.client.gui;

import com.zenith.udl.config.item.ItemSettingModule;
import com.zenith.udl.config.item.SwordConfig;
import com.zenith.udl.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class SwordConfigScreen extends Screen {
    private final ItemStack swordStack;
    private boolean abilityEnabled;
    private final Map<ItemSettingModule, Boolean> featureStates = new HashMap<>();

    public SwordConfigScreen(ItemStack swordStack) {
        super(Component.literal("UltraDamage-Library Sword Settings"));
        this.swordStack = swordStack;
        this.abilityEnabled = SwordConfig.isAbilityEnabled(swordStack);

        for (ItemSettingModule module : ItemSettingModule.values()) {
            featureStates.put(module, SwordConfig.isFeatureEnabled(swordStack, module));
        }
    }

    @Override
    protected void init() {
        int startX = this.width / 2 - 110;
        int startY = 45;

        // 1. メイン能力の有効化トグル
        this.addRenderableWidget(
                CycleButton.booleanBuilder(Component.literal("有効"), Component.literal("無効"))
                        .withInitialValue(this.abilityEnabled)
                        .create(startX, startY, 220, 20, Component.literal("機能"), (button, value) -> {
                            this.abilityEnabled = value;
                            this.rebuildWidgets(); // メインのオン/オフに合わせて下のボタンの有効化を切り替え
                        })
        );

        // 2. 各機能モジュールの個別オン/オフボタン
        int yOffset = startY + 30;
        for (ItemSettingModule module : ItemSettingModule.values()) {
            final ItemSettingModule mod = module;
            boolean isEnabled = featureStates.getOrDefault(mod, false);

            Button btn = Button.builder(
                    Component.literal(mod.getDisplayName().getString() + ": " + (isEnabled ? "【有効】" : "【無効】")),
                    b -> {
                        boolean nextState = !featureStates.get(mod);
                        featureStates.put(mod, nextState);
                        b.setMessage(Component.literal(mod.getDisplayName().getString() + ": " + (nextState ? "【有効】" : "【無効】")));
                    }
            ).bounds(startX, yOffset, 220, 20).build();

            // メイン機能がOFFの場合は個別の設定ボタンを無効化
            btn.active = this.abilityEnabled;
            this.addRenderableWidget(btn);
            yOffset += 24;
        }

        // 3. 保存して閉じるボタン
        this.addRenderableWidget(
                Button.builder(Component.literal("Save and close"), b -> {
                    saveAndClose();
                }).bounds(startX, yOffset + 10, 220, 20).build()
        );
    }

    private void saveAndClose() {
        int mask = 0;
        for (ItemSettingModule module : ItemSettingModule.values()) {
            if (featureStates.getOrDefault(module, false)) {
                mask |= (1 << module.ordinal());
            }
        }
        // サーバーにパケットを送信して設定を保存
        NetworkHandler.CHANNEL.sendToServer(new NetworkHandler.PacketUpdateSwordConfig(abilityEnabled, mask));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFD700); // 金色タイトル
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}