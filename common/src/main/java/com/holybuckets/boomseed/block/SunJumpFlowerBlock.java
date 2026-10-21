package com.holybuckets.boomseed.block;

import com.holybuckets.boomseed.block.be.ModBlockEntities;
import com.holybuckets.boomseed.block.be.SunJumpFlowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SunJumpFlowerBlock extends FlowerBlock implements EntityBlock, BonemealableBlock {

    public static final BooleanProperty SPRUNG = BooleanProperty.create("sprung");

    private static final int LAUNCH_BLOCKS = 8;
    private static final double VANILLA_JUMP_VELOCITY = 0.42;
    private static final double VANILLA_JUMP_HEIGHT = 1.25;
    private static final double LAUNCH_VELOCITY = VANILLA_JUMP_VELOCITY * Math.sqrt(LAUNCH_BLOCKS / VANILLA_JUMP_HEIGHT);
    private static final int STEW_EFFECT_SECONDS = 5;
    private static final double LAUNCH_RANGE = 1.5;

    public SunJumpFlowerBlock(Properties properties) {
        super(MobEffects.JUMP, STEW_EFFECT_SECONDS, properties);
        registerDefaultState(defaultBlockState().setValue(SPRUNG, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SPRUNG);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SunJumpFlowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (tickLevel, pos, tickState, be) -> {
            if (be instanceof SunJumpFlowerBlockEntity flower) flower.serverTick(tickLevel, pos, tickState);
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        entity.resetFallDistance();
    }

    @Nullable
    public static Player findLaunchTarget(Level level, BlockPos pos) {
        if (!level.canSeeSky(pos)) return null;
        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, LAUNCH_RANGE, false);
        if (player == null || player.isSpectator()) return null;
        return player;
    }

    public static void setSprung(Level level, BlockPos pos, BlockState ignored, boolean sprung) {
        BlockState current = level.getBlockState(pos);
        if (!current.hasProperty(SPRUNG) || current.getValue(SPRUNG) == sprung) return;
        level.setBlock(pos, current.setValue(SPRUNG, sprung), UPDATE_CLIENTS);
    }

    public static void launch(Level level, BlockPos pos, BlockState state) {
        setSprung(level, pos, state, true);

        Player player = findLaunchTarget(level, pos);
        level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 1.0f, 1.2f);
        if (player == null) return;

        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, LAUNCH_VELOCITY, motion.z);
        player.resetFallDistance();
        player.hurtMarked = true;

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient) {
        return !spreadTargets(level, pos, state).isEmpty();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        List<BlockPos> targets = spreadTargets(level, pos, state);
        if (targets.isEmpty()) return;
        BlockPos target = targets.get(random.nextInt(targets.size()));
        level.setBlock(target, defaultBlockState(), UPDATE_ALL);
    }

    private List<BlockPos> spreadTargets(LevelReader level, BlockPos pos, BlockState state) {
        List<BlockPos> targets = new ArrayList<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos target = pos.relative(direction);
            if (!level.getBlockState(target).isAir()) continue;
            if (!defaultBlockState().canSurvive(level, target)) continue;
            targets.add(target);
        }
        return targets;
    }

}
