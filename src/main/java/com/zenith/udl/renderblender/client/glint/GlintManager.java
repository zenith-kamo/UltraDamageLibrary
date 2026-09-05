package com.zenith.udl.renderblender.client.glint;

import com.zenith.udl.Udl;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlintManager {
    public static final String GLINT_KEY = "%s:glint".formatted(Udl.MODID);
    public static final String GLINT_ALWAYS_KEY = "%s:glint_always".formatted(Udl.MODID);
    public static final List<GlintHolder> HOLDERS = new ArrayList<>();
    public static final Map<String, GlintHolder> BY_ID = new HashMap<>();

    public static final GlintHolder DEFAULT = new GlintHolder("default", null, ChatFormatting.WHITE);
    public static final GlintHolder RED = new GlintHolder("red", ResourceLocation.tryBuild(Udl.MODID, "textures/misc/glint_item_red.png"), ChatFormatting.RED);
    public static final GlintHolder YELLOW = new GlintHolder("yellow", ResourceLocation.tryBuild(Udl.MODID, "textures/misc/glint_item_yellow.png"), ChatFormatting.YELLOW);
    public static final GlintHolder BLUE = new GlintHolder("blue", ResourceLocation.tryBuild(Udl.MODID, "textures/misc/glint_item_blue.png"), ChatFormatting.BLUE);
    public static final GlintHolder ORANGE = new GlintHolder("orange", ResourceLocation.tryBuild(Udl.MODID, "textures/misc/glint_item_orange.png"), ChatFormatting.GOLD);
    public static final GlintHolder GREEN = new GlintHolder("green", ResourceLocation.tryBuild(Udl.MODID, "textures/misc/glint_item_green.png"), ChatFormatting.GREEN);
    public static final GlintHolder PURPLE = new GlintHolder("purple", ResourceLocation.tryBuild(Udl.MODID, "textures/misc/glint_item_purple.png"), ChatFormatting.DARK_PURPLE);
    public static final GlintHolder WHITE = new GlintHolder("white", ResourceLocation.tryBuild(Udl.MODID, "textures/misc/glint_item_white.png"), ChatFormatting.WHITE);
    public static final GlintHolder PINK = new GlintHolder("pink", ResourceLocation.tryBuild(Udl.MODID, "textures/misc/glint_item_pink.png"), ChatFormatting.LIGHT_PURPLE);
    public static final GlintHolder AQUA = new GlintHolder("aqua", ResourceLocation.tryBuild(Udl.MODID, "textures/misc/glint_item_aqua.png"), ChatFormatting.AQUA);

    public static ItemStack removeGlint(ItemStack stack) {
        stack.getOrCreateTag().remove(GLINT_KEY);
        stack.getOrCreateTag().remove(GLINT_ALWAYS_KEY);
        return stack;
    }

    public record GlintHolder(String id, ResourceLocation texture, ChatFormatting textColor) {
        public GlintHolder(String id, ResourceLocation texture, ChatFormatting textColor) {
            this.id = id;
            this.texture = texture;
            this.textColor = textColor;
            HOLDERS.add(this);
            BY_ID.put(this.id, this);
        }

        public ItemStack apply(ItemStack stack, boolean always) {
            stack.getOrCreateTag().putString(GLINT_KEY, this.id);
            stack.getOrCreateTag().putBoolean(GLINT_ALWAYS_KEY, always);
            return stack;
        }
    }
}
