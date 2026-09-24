package com.holybuckets.boomseed.entity;

import com.holybuckets.boomseed.config.BoomSeedsConfig;
import com.holybuckets.boomseed.item.ModItems;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;

import java.util.*;
import java.util.function.Predicate;

public class BoomSeedEntity extends ThrowableItemProjectile {

    private static final int GREAT_BOOM_RANGE = 1;
    private static final float BOOM_ENTITY_DAMAGE = 3.0f;
    private static final float GREAT_BOOM_ENTITY_DAMAGE = 20.0f;
    private static final float DROP_CHANCE = 0.25f;

    private static final Map<ResourceKey<Level>, Map<BlockPos, Float>> BLOCK_DAMAGE = new HashMap<>();
    private static final Set<Block> BOOM_SEED_IMPERVIOUS = new HashSet<>();
    private static final Set<Block> GREAT_BOOM_SEED_IMPERVIOUS = new HashSet<>();

    private static float weakBlockBreakChance = 0.35f;
    private static float boomSeedBlockDamage = 0.2f;
    private static float greatBoomSeedBlockDamage = 0.75f;
    private static float damageDecayRate = 0.035f;

    private boolean exploded = false;
    private boolean cosmetic = false;
    private static Random randomSource;

    private static Block toBlock(String loc) {
        return HBUtil.BlockUtil.blockNameToBlock(loc);
    }

    public static void loadConfig(BoomSeedsConfig config) {
        BOOM_SEED_IMPERVIOUS.clear();
        GREAT_BOOM_SEED_IMPERVIOUS.clear();
        for (String s : config.greatBoomSeedImperviousBlocks) {
            GREAT_BOOM_SEED_IMPERVIOUS.add(toBlock(s));
        }
        for (String s : config.boomSeedImperviousBlocks) {
            BOOM_SEED_IMPERVIOUS.add(toBlock(s));
        }
        BOOM_SEED_IMPERVIOUS.addAll(GREAT_BOOM_SEED_IMPERVIOUS);

        boomSeedBlockDamage = config.boomSeedBlockDamage;
        greatBoomSeedBlockDamage = config.greatBoomSeedBlockDamage;
        damageDecayRate = config.boomSeedDamageDecayRate;
        weakBlockBreakChance = config.weakBlockBreakChance;

        randomSource = new Random(GeneralConfig.getInstance().getWorldSeed());
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
        return isGreat() ? ModItems.greatBoomSeed : ModItems.singleBoomSeed;
    }

    public void setCosmetic() {
        this.cosmetic = true;
    }


    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide || this.cosmetic || this.exploded) return;
        this.exploded = true;
        Entity target = result.getEntity();
        float damage = (isGreat() ? GREAT_BOOM_ENTITY_DAMAGE : BOOM_ENTITY_DAMAGE);
        target.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
        playBoom(this.level(), this.position(), isGreat());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide || this.cosmetic || this.exploded) return;
        this.exploded = true;
        explode(this.level(), this.position(), isGreat(), false);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) this.discard();
    }


    private static int boomCount=0;
    public static void explode(Level level, Vec3 at, boolean great, boolean forceSound) {
        if (level.isClientSide) return;
        damageBlocks(level, at, great);
        if(great || forceSound || (boomCount++>2)) {
            boomCount=0;
            playBoom(level, at, great);
        }
    }

    private static void playBoom(Level level, Vec3 at, boolean great) {
        float volume = great ? 4.0f : 2.0f;
        float pitch = (1.0f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f;
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, volume, pitch);
        if (level instanceof ServerLevel serverLevel) {
            if (great) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            } else {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 2, 0.4, 0.4, 0.4, 0);
            }
        }
    }

    private static void damageBlocks(Level level, Vec3 at, boolean great) {
        float power = (great ? greatBoomSeedBlockDamage : boomSeedBlockDamage);

        BlockPos center = BlockPos.containing(at.x, at.y, at.z);

        for (BlockPos pos: blastArea(center, at, great))
        {
            BlockState state = level.getBlockState(pos);
            if (!canDamage(level, pos, state, great)) continue;

            if (!state.requiresCorrectToolForDrops()) {
                if (randomSource.nextFloat() < weakBlockBreakChance)
                    destroyDamagedBlock(level, pos);
                continue;
            }

            applyDamage(level, pos.immutable(), state, power);
        }
    }

    private static Set<BlockPos> blastArea(BlockPos center, Vec3 hitAt, boolean great)
    {
        Set<BlockPos> blocks = new HashSet<>();


        //BlockPos frontFace = getface(center, hitAt, 1);
        //BlockPos backFace = getface(center, hitAt, -1);
        BlockPos hitFace = center;

        //the sample pos needs to be closer to the center of the hit face than the center of the front face, so we can use that to filter out blocks that are too far away
        Predicate<Vec3> isCloseFalloff = (posCenter) -> {
            return posCenter.distanceToSqr(hitFace.getCenter()) <= randomSource.nextDouble()+1.5d;
        };

        for (int x = -GREAT_BOOM_RANGE; x <= GREAT_BOOM_RANGE; x++) {
            for (int y = -GREAT_BOOM_RANGE; y <= GREAT_BOOM_RANGE; y++) {
                for (int z = -GREAT_BOOM_RANGE; z <= GREAT_BOOM_RANGE; z++) {
                    if(great || isCloseFalloff.test(hitFace.offset(x, y, z).getCenter()))
                        blocks.add(hitFace.offset(x, y, z));
                }
            }
        }
        blocks.add(center);
        return blocks;
    }

        private static BlockPos getface(BlockPos pos, Vec3 at, int dir) {
            return new BlockPos((int) (pos.getX()+dir*0.1d), (int) (pos.getY()+dir*0.1d), (int) (pos.getZ()+dir*0.1d));
        }

    private static boolean canDamage(Level level, BlockPos pos, BlockState state, boolean great) {
        if (state.isAir()) return false;
        if (state.getDestroySpeed(level, pos) < 0) return false;
        Set<Block> impervious = great ? GREAT_BOOM_SEED_IMPERVIOUS : BOOM_SEED_IMPERVIOUS;
        if (impervious.contains(state.getBlock())) return false;
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL) && !great) return false;
        return true;
    }

    private static void applyDamage(Level level, BlockPos pos, BlockState state, float damage) {
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
        int hash = pos.getX() * 31 + pos.getY();
        hash = hash * 31 + pos.getZ();
        hash ^= hash >>> 16;
        return hash | Integer.MIN_VALUE;
    }

}
