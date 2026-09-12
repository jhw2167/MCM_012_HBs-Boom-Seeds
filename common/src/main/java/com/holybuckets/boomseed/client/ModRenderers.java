package com.holybuckets.boomseed.client;

import com.holybuckets.boomseed.Constants;
import com.holybuckets.boomseed.entity.ModEntities;
import net.blay09.mods.balm.api.client.rendering.BalmRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;

public class ModRenderers {

    public static void clientInitialize(BalmRenderers renderers) {
        renderers.registerEntityRenderer(id("boom_seed"), ModEntities.boomSeed::get, (context) -> new ThrownItemRenderer<>(context));
        renderers.registerEntityRenderer(id("great_boom_seed"), ModEntities.greatBoomSeed::get, (context) -> new ThrownItemRenderer<>(context));
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }

}
