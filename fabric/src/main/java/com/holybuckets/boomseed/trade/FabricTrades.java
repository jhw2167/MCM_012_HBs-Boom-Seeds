package com.holybuckets.boomseed.trade;

import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;

public class FabricTrades {

    public static void register() {
        TradeOfferHelper.registerWanderingTraderOffers(1, factories -> factories.addAll(BoomSeedTrades.wanderingTrades()));
    }

}
