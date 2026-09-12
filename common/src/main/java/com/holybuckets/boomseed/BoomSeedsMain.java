package com.holybuckets.boomseed;


import com.holybuckets.boomseed.config.BoomSeedsConfig;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.boomseed.entity.BoomSeedEntity;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;


public class BoomSeedsMain {
    private static boolean DEV_MODE = false;;
    private static BoomSeedsConfig CONFIG;
    public static BoomSeedsMain INSTANCE;

    public BoomSeedsMain()
    {
        super();
        INSTANCE = this;
        init();
        // LoggerProject.logInit( "001000", this.getClass().getName() ); // Uncomment if you have a logging system in place
    }

    private void init()
    {
        //Events
        EventRegistrar registrar = EventRegistrar.getInstance();
        registrar.registerOnBeforeServerStarted(this::onServerStarting);
        registrar.registerOnServerTick(TickType.ON_20_TICKS, this::on20Ticks);
    }

    private void on20Ticks(ServerTickEvent e) {
        BoomSeedEntity.on20Ticks();
    }

    private void onServerStarting(ServerStartingEvent e) {
        CONFIG = Balm.getConfig().getActiveConfig(BoomSeedsConfig.class);
        BoomSeedEntity.loadConfig(CONFIG);
        //this.DEV_MODE = CONFIG.devMode;
        this.DEV_MODE = false;
    }


}
