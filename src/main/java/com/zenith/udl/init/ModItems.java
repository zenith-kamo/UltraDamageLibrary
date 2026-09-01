package com.zenith.udl.init;

import com.zenith.udl.Udl;
import com.zenith.udl.item.PocketWatchItem;
import com.zenith.udl.item.UltraDamageLibrarySwordItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Udl.MODID);

    public static final RegistryObject<Item> STARFALL_SWORD =
            ITEMS.register("ultra_damage_library_sword", UltraDamageLibrarySwordItem::new);
    public static final RegistryObject<Item> POCKET_WATCH =
            ITEMS.register("pocket_watch", () -> new PocketWatchItem(new Item.Properties()));
}
