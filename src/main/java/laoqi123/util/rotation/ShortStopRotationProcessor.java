package laoqi123.util.rotation;

import laoqi123.property.properties.IntProperty;
import laoqi123.property.properties.IntRangeProperty;
import laoqi123.util.config.ToggleableConfigurable;

import java.util.concurrent.ThreadLocalRandom;

public class ShortStopRotationProcessor extends ToggleableConfigurable implements RotationProcessor {
    private final IntProperty rate;
    private final IntRangeProperty stopDuration;
    private int ticksElapsed;
    private int currentTransitionInDuration;

    public ShortStopRotationProcessor() {
        super("ShortStop", false);
        this.rate = this.register(new IntProperty("Rate", 3, 1, 25));
        this.stopDuration = this.register(new IntRangeProperty("Duration", 1, 2, 1, 5));
        this.currentTransitionInDuration = this.stopDuration.random();
    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        if (!this.running()) {
            return targetRotation;
        }
        if (this.rate.getValue() > ThreadLocalRandom.current().nextInt(0, 101)) {
            this.currentTransitionInDuration = this.stopDuration.random();
            this.ticksElapsed = 0;
        }
        if (this.ticksElapsed < this.currentTransitionInDuration) {
            this.ticksElapsed++;
            return currentRotation.towardsLinear(targetRotation, randomFactor(), randomFactor());
        }
        return targetRotation;
    }

    private static float randomFactor() {
        return ThreadLocalRandom.current().nextFloat() * 0.1f;
    }
}
