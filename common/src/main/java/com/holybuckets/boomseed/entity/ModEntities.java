package com.holybuckets.boomseed.entity;

import com.holybuckets.boomseed.Constants;
import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.entity.BalmEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {

    public static DeferredObject<EntityType<BoomSeedEntity>> boomSeed;
    public static DeferredObject<EntityType<BoomSeedEntity>> greatBoomSeed;

    public static void initialize(BalmEntities entities) {
        boomSeed = entities.registerEntity(id("boom_seed"), getBuilder(BoomSeedEntity::new));
        greatBoomSeed = entities.registerEntity(id("great_boom_seed"), getBuilder(BoomSeedEntity::new));
    }

    static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }

    static <T extends Entity> EntityType.Builder<T> getBuilder(EntityType.EntityFactory<T> factory) {
        return EntityType.Builder.of(factory, MobCategory.MISC)
            .sized(0.25f, 0.25f)
            .clientTrackingRange(4)
            .updateInterval(10);
    }

}
