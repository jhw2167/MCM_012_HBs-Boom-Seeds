package com.holybuckets.boomseed.item;


import com.holybuckets.boomseed.Constants;
import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.item.BalmItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ModItems {
    public static DeferredObject<CreativeModeTab> creativeModeTab;
    public static Item boomSeed;
    public static Item greatBoomSeed;

    public static void initialize(BalmItems items) {
        items.registerItem(() -> boomSeed = new BoomSeedItem(items.itemProperties().stacksTo(64), false), id("boom_seed"), id(Constants.MOD_ID));
        items.registerItem(() -> greatBoomSeed = new BoomSeedItem(items.itemProperties().stacksTo(16), true), id("great_boom_seed"), id(Constants.MOD_ID));
        creativeModeTab = items.registerCreativeModeTab(id(Constants.MOD_ID), () -> new ItemStack(boomSeed));
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }

}
