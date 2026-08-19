package laoqi123.util.rotation;

import laoqi123.value.properties.*;
import laoqi123.value.properties.BooleanValue;
import laoqi123.util.config.ToggleableConfigurable;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class FailRotationProcessor extends ToggleableConfigurable implements RotationProcessor {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final IntValue rate;
    private final FloatValue failFactor;
    private final FloatRangeValue strengthHorizontal;
    private final FloatRangeValue strengthVertical;
    private final IntRangeValue transitionInDuration;
    private final BooleanValue restrictToSneak;
    private final Supplier<Rotation> previousRotationSupplier;

    private int ticksElapsed;
    private int currentTransitionInDuration;
    private Rotation shiftRotation = new Rotation(0.0f, 0.0f);

    public FailRotationProcessor(Supplier<Rotation> previousRotationSupplier) {
        super("Fail", false);
        this.previousRotationSupplier = previousRotationSupplier;
        this.rate = this.register(new IntValue("Rate", 3, 1, 100));
        this.failFactor = this.register(new FloatValue("Factor", 0.04f, 0.01f, 0.99f));
        this.strengthHorizontal = this.register(new FloatRangeValue("StrengthHorizontal", 5.0f, 10.0f, 1.0f, 90.0f));
        this.strengthVertical = this.register(new FloatRangeValue("StrengthVertical", 0.0f, 2.0f, 0.0f, 90.0f));
        this.transitionInDuration = this.register(new IntRangeValue("TransitionInDuration", 1, 4, 0, 20));
        this.restrictToSneak = this.register(new BooleanValue("RestrictToSneak", false));
    }

    public void tick() {
        if (this.running() && !this.isInFailState()) {
            if (this.rate.getValue() > ThreadLocalRandom.current().nextInt(1, 101)) {
                this.currentTransitionInDuration = this.transitionInDuration.random();
                float yawShift = ThreadLocalRandom.current().nextBoolean()
                        ? this.strengthHorizontal.random()
                        : -this.strengthHorizontal.random();
                float pitchShift = ThreadLocalRandom.current().nextBoolean()
                        ? this.strengthVertical.random()
                        : -this.strengthVertical.random();
                this.shiftRotation = new Rotation(yawShift, pitchShift);
                this.ticksElapsed = 0;
            } else {
                this.ticksElapsed++;
            }
        }
    }

    public boolean isInFailState() {
        return this.running() && this.ticksElapsed < this.currentTransitionInDuration;
    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        if (this.running() && this.isInFailState()) {
            Rotation shift = this.shiftRotation;
            if (this.restrictToSneak.getValue() && !mc.player.isSneaking()) {
                shift = new Rotation(0.0f, 0.0f);
            }
            Rotation prevRotation = this.previousRotationSupplier != null ? this.previousRotationSupplier.get() : null;
            Rotation serverRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
            if (prevRotation != null) {
                float deltaYaw = (prevRotation.getYaw() - serverRotation.getYaw()) * this.failFactor.getValue();
                float deltaPitch = (prevRotation.getPitch() - serverRotation.getPitch()) * this.failFactor.getValue();
                return new Rotation(
                        targetRotation.getYaw() + deltaYaw + shift.getYaw(),
                        targetRotation.getPitch() + deltaPitch + shift.getPitch()
                );
            }
        }
        return targetRotation;
    }
}
