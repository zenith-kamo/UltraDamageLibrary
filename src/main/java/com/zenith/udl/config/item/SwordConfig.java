package com.zenith.udl.config.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class SwordConfig {
    private static final String KEY_USE_UNSAFE = "UseUnsafe"; // 名前を変更
    private static final String KEY_FEATURE_MASK = "EnabledFeatures";

    // 危険な最適化機能全体のオン/オフ
    public static boolean isUseUnsafe(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getBoolean(KEY_USE_UNSAFE);
    }

    public static void setUseUnsafe(ItemStack stack, boolean value) {
        stack.getOrCreateTag().putBoolean(KEY_USE_UNSAFE, value);
    }

    // 個別機能のオン/オフ
    public static boolean isFeatureEnabled(ItemStack stack, ItemSettingModule feature) {
        CompoundTag tag = stack.getOrCreateTag();
        int mask = tag.getInt(KEY_FEATURE_MASK);
        return (mask & (1 << feature.ordinal())) != 0;
    }

    public static void setFeatureEnabled(ItemStack stack, ItemSettingModule feature, boolean enabled) {
        CompoundTag tag = stack.getOrCreateTag();
        int mask = tag.getInt(KEY_FEATURE_MASK);
        if (enabled) {
            mask |= (1 << feature.ordinal());
        } else {
            mask &= ~(1 << feature.ordinal());
        }
        tag.putInt(KEY_FEATURE_MASK, mask);
    }
}