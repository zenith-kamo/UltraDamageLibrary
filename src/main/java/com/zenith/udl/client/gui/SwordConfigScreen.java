package com.zenith.udl.client.gui;

import com.zenith.udl.config.item.ItemSettingModule;
import com.zenith.udl.config.item.SwordConfig;
import com.zenith.udl.network.NetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SwordConfigScreen extends Screen {
    private final ItemStack swordStack;
    private boolean useUnsafe;
    private final Map<ItemSettingModule, Boolean> featureStates = new HashMap<>();

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_PADDING = 15;
    private static final int ROW_HEIGHT = 28;
    private static final int SUB_FEATURE_INDENT = 12; // サブ機能群の左インデント（余白）

    public SwordConfigScreen(ItemStack swordStack) {
        super(Component.literal("UltraDamage-Library Settings"));
        this.swordStack = swordStack;
        this.useUnsafe = SwordConfig.isUseUnsafe(swordStack);

        for (ItemSettingModule module : ItemSettingModule.values()) {
            featureStates.put(module, SwordConfig.isFeatureEnabled(swordStack, module));
        }
    }

    @Override
    protected void init() {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = 40;
        int contentWidth = PANEL_WIDTH - (PANEL_PADDING * 2);
        int startY = panelY + 35;

        // 1. メイン機能 (Use Unsafe)
        this.addRenderableWidget(new ModernToggleButton(
                panelX + PANEL_PADDING, startY, contentWidth, 24,
                Component.literal("Use Unsafe").withStyle(ChatFormatting.RED),
                Component.literal("サーバー/クライアントのエンティティ処理を書き換えます。予期しない動作やクラッシュを引き起こす可能性があります。").withStyle(ChatFormatting.RED),
                this.useUnsafe,
                (value) -> {
                    this.useUnsafe = value;
                    this.rebuildWidgets(); // サブ機能の有効/無効状態を更新
                }
        ));

        int yOffset = startY + 32;

        // 2. Use Unsafe のサブ機能モジュール群（左側にインデントを追加）
        int subX = panelX + PANEL_PADDING + SUB_FEATURE_INDENT;
        int subWidth = contentWidth - SUB_FEATURE_INDENT;

        for (ItemSettingModule module : ItemSettingModule.values()) {
            // DELETE_ENTITY_SAVE_DATA は下の独立メインセクションで配置するため除外
            if (module == ItemSettingModule.DELETE_ENTITY_SAVE_DATA) {
                continue;
            }

            final ItemSettingModule mod = module;
            boolean isEnabled = featureStates.getOrDefault(mod, false);

            ModernToggleButton btn = new ModernToggleButton(
                    subX, yOffset, subWidth, 24,
                    mod.getDisplayName(),
                    mod.getDescription(),
                    isEnabled,
                    (value) -> featureStates.put(mod, value)
            );

            // Use Unsafe が有効な場合のみ操作可能
            btn.active = this.useUnsafe;

            this.addRenderableWidget(btn);
            yOffset += ROW_HEIGHT;
        }

        // 3. メイン機能 (DELETE_ENTITY_SAVE_DATA)
        // サブ機能群の下に配置・インデントなし（メイン機能と同じ位置）で独立して操作可能
        yOffset += 4; // セクション間のスペーサー
        ItemSettingModule standaloneModule = ItemSettingModule.DELETE_ENTITY_SAVE_DATA;
        boolean standaloneEnabled = featureStates.getOrDefault(standaloneModule, false);

        ModernToggleButton standaloneBtn = new ModernToggleButton(
                panelX + PANEL_PADDING, yOffset, contentWidth, 24,
                standaloneModule.getDisplayName().copy().withStyle(ChatFormatting.GOLD),
                standaloneModule.getDescription(),
                standaloneEnabled,
                (value) -> featureStates.put(standaloneModule, value)
        );
        standaloneBtn.active = true; // 常に操作可能
        this.addRenderableWidget(standaloneBtn);
        yOffset += ROW_HEIGHT;

        // 4. 保存して閉じるボタン
        this.addRenderableWidget(new ModernButton(
                panelX + PANEL_PADDING, yOffset + 10, contentWidth, 24,
                Component.literal("Save and Close"),
                (b) -> saveAndClose()
        ));
    }

    private void saveAndClose() {
        int mask = 0;
        for (ItemSettingModule module : ItemSettingModule.values()) {
            if (featureStates.getOrDefault(module, false)) {
                mask |= (1 << module.ordinal());
            }
        }
        NetworkHandler.sendUpdateSwordConfig(this.useUnsafe, mask);
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. 画面全体を半透明の黒で塗りつぶし
        guiGraphics.fill(0, 0, this.width, this.height, 0xCC000000);

        // 2. メインパネルの描画
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = 40;

        // パネル背景の高さ計算
        int panelH = 35 + 24 + 32 + (ItemSettingModule.values().length * ROW_HEIGHT) + 4 + 10 + 24 + PANEL_PADDING;

        // パネル背景 (濃い黒)
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelH, 0xFF111111);
        // パネル枠線 (シアン)
        guiGraphics.renderOutline(panelX, panelY, PANEL_WIDTH, panelH, 0xFF00A8A8);

        // 3. タイトル描画
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panelY + 10, 0xFFD700);

        // 4. ウィジェットの描画
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // =========================================================================
    // カスタムウィジェット定義
    // =========================================================================

    public static class ModernToggleButton extends AbstractWidget {
        private boolean isOn;
        private final Component description;
        private final Consumer<Boolean> onPress;

        public ModernToggleButton(int x, int y, int width, int height, Component title, Component description, boolean initialState, Consumer<Boolean> onPress) {
            super(x, y, width, height, title);
            this.description = description;
            this.isOn = initialState;
            this.onPress = onPress;
            this.setTooltip(Tooltip.create(description));
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int alpha = this.active ? 255 : 100; // 無効時は半透明

            // 背景
            int bgColor = (alpha << 24) | 0x1A1A1A;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);

            // ホバー時の枠線
            int borderColor = (this.active && this.isHovered) ? 0xFF00A8A8 : 0xFF444444;
            guiGraphics.renderOutline(this.getX(), this.getY(), this.width, this.height, borderColor);

            // テキスト
            int textColor = this.active ? 0xEEEEEE : 0x888888;
            guiGraphics.drawString(Minecraft.getInstance().font, this.getMessage(), this.getX() + 6, this.getY() + (this.height - 8) / 2, textColor);

            // トグルスイッチの背景
            int toggleW = 40;
            int toggleH = this.height - 8;
            int toggleX = this.getX() + this.width - toggleW - 4;
            int toggleY = this.getY() + 4;

            int toggleBgColor = this.isOn ? 0xFF00AA00 : 0xFF333333;
            int finalToggleBg = (alpha << 24) | (toggleBgColor & 0x00FFFFFF);
            guiGraphics.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, finalToggleBg);
            guiGraphics.renderOutline(toggleX, toggleY, toggleW, toggleH, 0xFF666666);

            // トグルスイッチのノブ
            int knobSize = toggleH - 4;
            int knobX = this.isOn ? (toggleX + toggleW - 2 - knobSize) : (toggleX + 2);
            int knobY = toggleY + 2;
            guiGraphics.fill(knobX, knobY, knobX + knobSize, knobY + knobSize, 0xFFFFFFFF);

            // ホバー時のハイライトエフェクト
            if (this.active && this.isHovered) {
                guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x22FFFFFF);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (this.active) {
                this.isOn = !this.isOn;
                this.onPress.accept(this.isOn);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            this.defaultButtonNarrationText(narrationElementOutput);
        }
    }

    public static class ModernButton extends AbstractWidget {
        private final Consumer<ModernButton> onPress;

        public ModernButton(int x, int y, int width, int height, Component message, Consumer<ModernButton> onPress) {
            super(x, y, width, height, message);
            this.onPress = onPress;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int color = this.active ? (this.isHovered ? 0xFF00A8A8 : 0xFF007A7A) : 0xFF333333;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, color);
            guiGraphics.renderOutline(this.getX(), this.getY(), this.width, this.height, 0xFF555555);

            int textColor = this.active ? 0xFFFFFF : 0x888888;
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor);

            if (this.active && this.isHovered) {
                guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x22FFFFFF);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (this.active) {
                this.onPress.accept(this);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            this.defaultButtonNarrationText(narrationElementOutput);
        }
    }
}