package com.holybuckets.boomseed.block.be;

import com.holybuckets.boomseed.block.SunJumpFlowerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SunJumpFlowerBlockEntity extends BlockEntity implements GeoBlockEntity {

    public static final String IDLE_CONTROLLER = "idle";
    public static final String ACTION_CONTROLLER = "action";
    public static final String POP_OUT_TRIGGER = "pop_out";
    public static final String POP_IN_TRIGGER = "pop_in";

    public static final int ARM_TICKS = 10;
    public static final int SPRUNG_TICKS = 20;
    public static final int RESET_TICKS = 34;

    private static final int TRANSITION_TICKS = 5;
    private static final String PHASE_KEY = "Phase";
    private static final String PHASE_TICK_KEY = "PhaseTick";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation IDLE_UP = RawAnimation.begin().thenLoop("idle_up");
    private static final RawAnimation POP_OUT = RawAnimation.begin().thenPlay("pop_out");
    private static final RawAnimation POP_IN = RawAnimation.begin().thenPlay("pop_in");

    public enum Phase { READY, ARMING, SPRUNG, RESETTING }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Phase phase = Phase.READY;
    private int phaseTick = 0;

    public SunJumpFlowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.sunjumpFlower.get(), pos, state);
    }

    public Phase getPhase() {
        return phase;
    }

    public boolean isReady() {
        return phase == Phase.READY;
    }

    public void arm() {
        if (phase != Phase.READY) return;
        setPhase(Phase.ARMING);
        triggerAnim(ACTION_CONTROLLER, POP_OUT_TRIGGER);
    }

    private void setPhase(Phase next) {
        this.phase = next;
        this.phaseTick = 0;
        setChanged();
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        phaseTick++;

        switch (phase) {
            case READY -> {
                if (SunJumpFlowerBlock.findLaunchTarget(level, pos) != null) arm();
            }
            case ARMING -> {
                if (phaseTick < ARM_TICKS) return;
                SunJumpFlowerBlock.launch(level, pos, state);
                setPhase(Phase.SPRUNG);
            }
            case SPRUNG -> {
                if (phaseTick < SPRUNG_TICKS) return;
                triggerAnim(ACTION_CONTROLLER, POP_IN_TRIGGER);
                SunJumpFlowerBlock.setSprung(level, pos, state, false);
                setPhase(Phase.RESETTING);
            }
            case RESETTING -> {
                if (phaseTick < RESET_TICKS) return;
                setPhase(Phase.READY);
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, IDLE_CONTROLLER, TRANSITION_TICKS, this::idle));
        controllers.add(new AnimationController<>(this, ACTION_CONTROLLER, TRANSITION_TICKS, state -> PlayState.STOP)
            .triggerableAnim(POP_OUT_TRIGGER, POP_OUT)
            .triggerableAnim(POP_IN_TRIGGER, POP_IN));
    }

    private PlayState idle(AnimationState<SunJumpFlowerBlockEntity> state) {
        boolean sprung = getBlockState().hasProperty(SunJumpFlowerBlock.SPRUNG)
            && getBlockState().getValue(SunJumpFlowerBlock.SPRUNG);
        return state.setAndContinue(sprung ? IDLE_UP : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(PHASE_KEY, phase.ordinal());
        tag.putInt(PHASE_TICK_KEY, phaseTick);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        int ordinal = tag.getInt(PHASE_KEY);
        phase = ordinal >= 0 && ordinal < Phase.values().length ? Phase.values()[ordinal] : Phase.READY;
        phaseTick = tag.getInt(PHASE_TICK_KEY);
    }

}
