package com.holybuckets.boomseed.entity;

import com.holybuckets.boomseed.config.BoomSeemsConfig;
import com.holybuckets.boomseed.item.ModItems;
import com.holybuckets.foundation.GeneralConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.*;

public class BoomSeedEntity extends ThrowableItemProjectile {

    private static final float BOOM_RADIUS = 1.5f;
    private static final float GREAT_BOOM_RADIUS = 3.0f;
    private static final float BOOM_ENTITY_DAMAGE = 5.0f;
    private static final float GREAT_BOOM_ENTITY_DAMAGE = 14.0f;
    private static final float DROP_CHANCE = 0.25f;

    private static final Map<ResourceKey<Level>, Map<BlockPos, Float>> BLOCK_DAMAGE = new HashMap<>();
    private static final Set<ResourceLocation> BOOM_SEED_IMPERVIOUS = new HashSet<>();
    private static final Set<ResourceLocation> GREAT_BOOM_SEED_IMPERVIOUS = new HashSet<>();

    private static float weakBlockBreakChance = 0.35f;
    private static float boomSeedBlockDamage = 0.25f;
    private static float greatBoomSeedBlockDamage = 0.75f;
    private static float damageDecayRate = 0.02f;

    private boolean exploded = false;

    public static void loadConfig(BoomSeemsConfig config) {
        BOOM_SEED_IMPERVIOUS.clear();
        GREAT_BOOM_SEED_IMPERVIOUS.clear();
        for (String s : config.greatBoomSeedImperviousBlocks) {
            ResourceLocation loc = ResourceLocation.tryParse(s);
            if (loc != null) GREAT_BOOM_SEED_IMPERVIOUS.add(loc);
        }
        for (String s : config.boomSeedImperviousBlocks) {
            ResourceLocation loc = ResourceLocation.tryParse(s);
            if (loc != null) BOOM_SEED_IMPERVIOUS.add(loc);
        }
        BOOM_SEED_IMPERVIOUS.addAll(GREAT_BOOM_SEED_IMPERVIOUS);

        boomSeedBlockDamage = config.boomSeedBlockDamage;
        greatBoomSeedBlockDamage = config.greatBoomSeedBlockDamage;
        damageDecayRate = config.boomSeedDamageDecayRate;
        weakBlockBreakChance = config.weakBlockBreakChance;
    }

    public BoomSeedEntity(EntityType<? extends BoomSeedEntity> type, Level level) {
        super(type, level);
    }

    public BoomSeedEntity(EntityType<? extends BoomSeedEntity> type, Level level, LivingEntity thrower) {
        super(type, thrower, level);
    }

    public boolean isGreat() {
        return ModEntities.greatBoomSeed != null && this.getType() == ModEntities.greatBoomSeed.get();
    }

    @Override
    protected Item getDefaultItem() {
        return isGreat() ? ModItems.greatBoomSeed : ModItems.boomSeed;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) return;
        Entity target = result.getEntity();
        float damage = isGreat() ? GREAT_BOOM_ENTITY_DAMAGE : BOOM_ENTITY_DAMAGE;
        target.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
        playBoom();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide || this.exploded) return;
        this.exploded = true;
        damageBlocks();
        playBoom();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) this.discard();
    }

    private void playBoom() {
        Level level = this.level();
        float volume = isGreat() ? 4.0f : 2.0f;
        float pitch = (1.0f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f;
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, volume, pitch);
        if (level instanceof ServerLevel serverLevel) {
            if (isGreat()) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
            } else {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 2, 0.4, 0.4, 0.4, 0);
            }
        }
    }

    private void damageBlocks() {
        Level level = this.level();
        boolean great = isGreat();
        float radius = great ? GREAT_BOOM_RADIUS : BOOM_RADIUS;
        float power = great ? greatBoomSeedBlockDamage : boomSeedBlockDamage;

        BlockPos center = BlockPos.containing(this.getX(), this.getY(), this.getZ());
        int range = Mth.ceil(radius);

        //Create a random source
        Random random = new Random(GeneralConfig.getInstance().getWorldSeed());

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist > radius) continue;

                    BlockState state = level.getBlockState(pos);
                    if (!canDamage(level, pos, state, great)) continue;

                    if (!state.requiresCorrectToolForDrops()) {
                        if (random.nextFloat() < weakBlockBreakChance)
                            destroyDamagedBlock(level, pos);
                        continue;
                    }

                    applyDamage(level, pos.immutable(), state, power);
                }
            }
        }
    }

    private static boolean canDamage(Level level, BlockPos pos, BlockState state, boolean great) {
        if (state.isAir()) return false;
        if (state.getDestroySpeed(level, pos) < 0) return false;
        Set<ResourceLocation> impervious = great ? GREAT_BOOM_SEED_IMPERVIOUS : BOOM_SEED_IMPERVIOUS;
        if (impervious.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) return false;
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL) && !great) return false;
        return true;
    }

    private void applyDamage(Level level, BlockPos pos, BlockState state, float damage) {
        Map<BlockPos, Float> damageMap = BLOCK_DAMAGE.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        float previous = damageMap.getOrDefault(pos, 0.0f);
        float total = previous + damage;

        if (total < 1.0f) {
            damageMap.put(pos, total);
            sendProgress(level, pos, previous, total);
            return;
        }

        if (level.random.nextFloat() < DROP_CHANCE) {
            Block.popResource(level, pos, new ItemStack(state.getBlock()));
        }
        destroyDamagedBlock(level, pos);
    }

    private static void destroyDamagedBlock(Level level, BlockPos pos) {
        Map<BlockPos, Float> damageMap = BLOCK_DAMAGE.get(level.dimension());
        if (damageMap != null && damageMap.remove(pos) != null) clearProgress(level, pos);
        level.destroyBlock(pos, false);
    }

    private static void sendProgress(Level level, BlockPos pos, float previous, float current) {
        int before = stage(previous);
        int after = stage(current);
        if (before == after && previous > 0.0f) return;
        level.destroyBlockProgress(breakerId(pos), pos, after);
    }

    private static void clearProgress(Level level, BlockPos pos) {
        level.destroyBlockProgress(breakerId(pos), pos, -1);
    }

    public static void on20Ticks() {
        MinecraftServer server = GeneralConfig.getInstance().getServer();
        if (server == null) return;

        for (Map.Entry<ResourceKey<Level>, Map<BlockPos, Float>> entry : BLOCK_DAMAGE.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            Iterator<Map.Entry<BlockPos, Float>> it = entry.getValue().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, Float> damaged = it.next();
                if (level == null) continue;

                BlockPos pos = damaged.getKey();
                if (level.getBlockState(pos).isAir()) {
                    it.remove();
                    clearProgress(level, pos);
                    continue;
                }

                float remaining = damaged.getValue() - damageDecayRate;
                if (remaining <= 0.0f) {
                    it.remove();
                    clearProgress(level, pos);
                    continue;
                }

                if (stage(remaining) != stage(damaged.getValue())) {
                    level.destroyBlockProgress(breakerId(pos), pos, stage(remaining));
                }
                damaged.setValue(remaining);
            }
        }
    }

    private static int stage(float damage) {
        return Mth.clamp((int) (damage * 10.0f), 0, 9);
    }

    private static int breakerId(BlockPos pos) {
        return Integer.MIN_VALUE + (int) (pos.asLong() & 0x3FFFFFFF);
    }

}
