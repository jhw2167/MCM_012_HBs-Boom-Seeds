package com.holybuckets.boomseed.trade;

import com.holybuckets.boomseed.item.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.List;

public class BoomSeedTrades {

    private static final int PORK_COST = 4;
    private static final int PORK_TRADE_SEEDS = 16;
    private static final int COPPER_COST = 6;
    private static final int COPPER_TRADE_SEEDS = 32;

    public static List<VillagerTrades.ItemListing> wanderingTrades() {
        List<VillagerTrades.ItemListing> trades = new ArrayList<>();
        trades.add(new SimpleListing(new ItemStack(Items.PORKCHOP, PORK_COST), new ItemStack(ModItems.boomSeed, PORK_TRADE_SEEDS), 8, 1));
        trades.add(new SimpleListing(new ItemStack(Items.COPPER_INGOT, COPPER_COST), new ItemStack(ModItems.boomSeed, COPPER_TRADE_SEEDS), 6, 1));
        return trades;
    }

    private record SimpleListing(ItemStack cost, ItemStack result, int maxUses, int xp) implements VillagerTrades.ItemListing {

        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            return new MerchantOffer(cost.copy(), result.copy(), maxUses, xp, 0.05f);
        }
    }

}
