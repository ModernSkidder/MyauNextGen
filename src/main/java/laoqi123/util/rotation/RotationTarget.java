package laoqi123.util.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;

import java.util.List;

public class RotationTarget {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private Rotation rotation;
    private LivingEntity entity;
    private final List<RotationProcessor> processors;
    private final int ticksUntilReset;
    private final float resetThreshold;
    private final boolean considerInventory;
    private final MovementCorrection movementCorrection;

    public RotationTarget(Rotation rotation,
                          LivingEntity entity,
                          List<RotationProcessor> processors,
                          int ticksUntilReset,
                          float resetThreshold,
                          boolean considerInventory,
                          MovementCorrection movementCorrection) {
        this.rotation = rotation;
        this.entity = entity;
        this.processors = processors;
        this.ticksUntilReset = ticksUntilReset;
        this.resetThreshold = resetThreshold;
        this.considerInventory = considerInventory;
        this.movementCorrection = movementCorrection;
    }

    public Rotation towards(Rotation currentRotation, boolean isResetting) {
        if (isResetting) {
            this.entity = null;
        }
        Rotation target = isResetting ? new Rotation(mc.player.getYaw(), mc.player.getPitch()) : this.rotation;
        if (this.processors.isEmpty()) {
            return target;
        }
        Rotation processed = target;
        for (RotationProcessor processor : this.processors) {
            processed = processor.process(this, currentRotation, processed);
        }
        return processed;
    }

    public Rotation getRotation() {
        return this.rotation;
    }

    public void setRotation(Rotation rotation) {
        this.rotation = rotation;
    }

    public LivingEntity getEntity() {
        return this.entity;
    }

    public int getTicksUntilReset() {
        return this.ticksUntilReset;
    }

    public float getResetThreshold() {
        return this.resetThreshold;
    }

    public boolean isConsiderInventory() {
        return this.considerInventory;
    }

    public MovementCorrection getMovementCorrection() {
        return this.movementCorrection;
    }
}
