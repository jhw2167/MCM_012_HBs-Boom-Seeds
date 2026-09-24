package com.holybuckets.boomseed.config;

import com.holybuckets.boomseed.Constants;
import net.blay09.mods.balm.api.config.reflection.Comment;
import net.blay09.mods.balm.api.config.reflection.Config;
import net.blay09.mods.balm.api.config.reflection.NestedType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Config(Constants.MOD_ID)
public class BoomSeedsConfig {

    @NestedType(String.class)
    @Comment("Block ids that cannot be damaged by Boom Seeds, e.g. minecraft:obsidian")
    public List<String> boomSeedImperviousBlocks = new ArrayList<>(Arrays.asList("minecraft:deepslate",
    "minecraft:deepslate"));

    @NestedType(String.class)
    @Comment("Block ids that cannot be damaged by Great Boom Seeds. These are added to boomSeedImperviousBlocks at runtime")
    public List<String> greatBoomSeedImperviousBlocks = new ArrayList<>(Arrays.asList(
        "minecraft:ancient_debris",
        "minecraft:obsidian",
        "minecraft:crying_obsidian",
        "minecraft:diamond_ore",
        "minecraft:deepslate_diamond_ore",
        "minecraft:iron_block",
        "minecraft:iron_bars",
        "minecraft:iron_door",
        "minecraft:iron_trapdoor",
        "minecraft:chain",
        "minecraft:heavy_weighted_pressure_plate",
        "minecraft:light_weighted_pressure_plate",
        "minecraft:anvil",
        "minecraft:chipped_anvil",
        "minecraft:damaged_anvil",
        "minecraft:hopper",
        "minecraft:cauldron",
        "minecraft:lantern",
        "minecraft:soul_lantern" ));

    @Comment("Portion of a block's hardness damaged by a single Boom Seed explosion")
    public float boomSeedBlockDamage = 0.25f;

    @Comment("Portion of a block's hardness damaged by a single Great Boom Seed explosion")
    public float greatBoomSeedBlockDamage = 3f;

    @Comment("Chance a block that needs no tool is destroyed outright by an explosion that reaches it")
    public float weakBlockBreakChance = 0.35f;

    @Comment("Damage to blocks decays by this float every 1 second")
    public float boomSeedDamageDecayRate = 0.20f;

}
