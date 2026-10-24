package com.holybuckets.boomseed.worldgen;

import com.holybuckets.boomseed.Constants;
import net.blay09.mods.balm.api.world.BalmWorldGen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;

public class ModWorldGen {

    public static final TagKey<Biome> HAS_SUNJUMP_FLOWER =
        TagKey.create(Registries.BIOME, id("has_sunjump_flower"));

    public static void initialize(BalmWorldGen worldGen) {
        worldGen.addFeatureToBiomes(
            (name, biome) -> biome.is(HAS_SUNJUMP_FLOWER),
            GenerationStep.Decoration.VEGETAL_DECORATION,
            id("sunjump_flower_lone"));
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }

}
