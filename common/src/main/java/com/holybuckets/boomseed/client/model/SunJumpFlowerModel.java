package com.holybuckets.boomseed.client.model;

import com.holybuckets.boomseed.Constants;
import com.holybuckets.boomseed.block.be.SunJumpFlowerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SunJumpFlowerModel extends GeoModel<SunJumpFlowerBlockEntity> {

    @Override
    public ResourceLocation getModelResource(SunJumpFlowerBlockEntity animatable) {
        return new ResourceLocation(Constants.MOD_ID, "geo/sunjump_flower.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SunJumpFlowerBlockEntity animatable) {
        return new ResourceLocation(Constants.MOD_ID, "textures/block/sunjump_flower.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SunJumpFlowerBlockEntity animatable) {
        return new ResourceLocation(Constants.MOD_ID, "animations/sunjump_flower.animation.json");
    }

}
