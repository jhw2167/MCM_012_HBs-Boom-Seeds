package com.holybuckets.boomseed.item;

import com.holybuckets.boomseed.entity.BoomSeedEntity;
import com.holybuckets.boomseed.entity.ModEntities;
import com.holybuckets.foundation.GeneralConfig;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Random;

public class BoomSeedItem extends Item {

    private static final int USE_COOLDOWN_TICKS = 8;
    private static final int BOOM_SEED_COUNT = 3;
    private static final float BOOM_SEED_DAMAGE_SCALE = 0.5f;

    private final boolean great;

    public BoomSeedItem(Properties properties, boolean great) {
        super(properties);
        this.great = great;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (!level.isClientSide) {
            Vec3 at = context.getClickLocation();
            BoomSeedEntity.explode(level, at, great);
            if (player != null) throwSeeds(level, player, true);
            if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        }

        if (player != null) {
            player.getCooldowns().addCooldown(this, USE_COOLDOWN_TICKS);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
            0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

        if (!level.isClientSide) {
            throwSeeds(level, player, false);
            player.getCooldowns().addCooldown(this, USE_COOLDOWN_TICKS);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) stack.shrink(1);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static final float SPREAD_DEGREES = 7.0f;
    private static Random RAND;
    private void throwSeeds(Level level, Player player, boolean cosmetic) {
        int count = great ? 1 : BOOM_SEED_COUNT;
        float scale = great ? 1.0f : BOOM_SEED_DAMAGE_SCALE;
        if(RAND==null)
            RAND = new Random(GeneralConfig.getInstance().getWorldSeed());

        for (int i = 0; i < count; i++) {
            double angle = Math.toDegrees(RAND.nextDouble(SPREAD_DEGREES));
            float xAngle = (float)(Math.sin(Math.toRadians(angle))*3);
            float yAngle = (float)(Math.cos(Math.toRadians(angle))*3);

            BoomSeedEntity seed = new BoomSeedEntity(great ? ModEntities.greatBoomSeed.get() : ModEntities.boomSeed.get(), level, player);
            seed.setDamageScale(scale);
            if (cosmetic) seed.setCosmetic();
            seed.shootFromRotation(player, player.getXRot()+xAngle, player.getYRot() + yAngle, 0.0f, 1.5f, 1.0f);
            level.addFreshEntity(seed);
        }
    }

}
