package laoqi123.util.rotation;

import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.FloatRangeValue;
import laoqi123.value.properties.FloatValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class AccelerationAngleSmooth extends AngleSmooth {
    private final FloatRangeValue yawAcceleration;
    private final FloatRangeValue pitchAcceleration;
    private final BooleanValue dynamicAccel;
    private final FloatValue coefDistance;
    private final FloatRangeValue yawCrosshairAccel;
    private final FloatRangeValue pitchCrosshairAccel;
    private final BooleanValue accelerationError;
    private final FloatValue yawAccelerationError;
    private final FloatValue pitchAccelerationError;
    private final BooleanValue constantError;
    private final FloatValue yawConstantError;
    private final FloatValue pitchConstantError;
    private final BooleanValue sigmoidDeceleration;
    private final FloatValue decelerationSteepness;
    private final FloatValue decelerationMidpoint;

    private final Supplier<Rotation> previousRotationSupplier;

    public AccelerationAngleSmooth(Supplier<Rotation> previousRotationSupplier) {
        super("Acceleration");
        this.previousRotationSupplier = previousRotationSupplier;
        this.yawAcceleration = this.register(new FloatRangeValue("YawAcceleration", 20.0f, 25.0f, 1.0f, 180.0f));
        this.pitchAcceleration = this.register(new FloatRangeValue("PitchAcceleration", 20.0f, 25.0f, 1.0f, 180.0f));
        this.dynamicAccel = this.register(new BooleanValue("DynamicAccel", false));
        this.coefDistance = this.register(new FloatValue("CoefDistance", -1.393f, -2.0f, 2.0f));
        this.yawCrosshairAccel = this.register(new FloatRangeValue("YawCrosshairAccel", 17.0f, 20.0f, 5.0f, 180.0f));
        this.pitchCrosshairAccel = this.register(new FloatRangeValue("PitchCrosshairAccel", 17.0f, 20.0f, 5.0f, 180.0f));
        this.accelerationError = this.register(new BooleanValue("AccelerationError", true));
        this.yawAccelerationError = this.register(new FloatValue("YawAccelerationError", 0.1f, 0.01f, 2.0f));
        this.pitchAccelerationError = this.register(new FloatValue("PitchAccelerationError", 0.1f, 0.01f, 2.0f));
        this.constantError = this.register(new BooleanValue("ConstantError", true));
        this.yawConstantError = this.register(new FloatValue("YawConstantError", 0.1f, 0.01f, 2.0f));
        this.pitchConstantError = this.register(new FloatValue("PitchConstantError", 0.1f, 0.01f, 2.0f));
        this.sigmoidDeceleration = this.register(new BooleanValue("SigmoidDeceleration", false));
        this.decelerationSteepness = this.register(new FloatValue("Steepness", 10.0f, 0.0f, 20.0f));
        this.decelerationMidpoint = this.register(new FloatValue("Midpoint", 0.3f, 0.0f, 1.0f));
    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        Rotation prevRotation = this.previousRotationSupplier != null ? this.previousRotationSupplier.get() : null;
        if (prevRotation == null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            prevRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        }
        RotationDelta prevDiff = prevRotation.rotationDeltaTo(currentRotation);
        RotationDelta diff = currentRotation.rotationDeltaTo(targetRotation);

        LivingEntity entity = rotationTarget != null ? rotationTarget.getEntity() : null;
        float distance = entity != null ? (float) MinecraftClient.getInstance().player.distanceTo(entity) : 0.0f;
        boolean crosshair = entity != null && AimUtil.facingEnemy(entity, Math.max(3.0, distance), currentRotation);

        float[] turnSpeed = this.computeTurnSpeed(prevDiff, diff, crosshair, distance);
        return new Rotation(
                currentRotation.getYaw() + turnSpeed[0],
                currentRotation.getPitch() + turnSpeed[1]
        );
    }

    @Override
    public int calculateTicks(Rotation currentRotation, Rotation targetRotation) {
        Rotation prevRotation = this.previousRotationSupplier != null ? this.previousRotationSupplier.get() : null;
        RotationDelta prevDiff = (prevRotation != null ? prevRotation : currentRotation).rotationDeltaTo(currentRotation);
        RotationDelta diff = currentRotation.rotationDeltaTo(targetRotation);
        int ticks = 0;
        Rotation lastRotation = currentRotation;
        do {
            float[] turnSpeed = this.computeTurnSpeed(prevDiff, diff, false, 0.0f);
            lastRotation = new Rotation(
                    lastRotation.getYaw() + turnSpeed[0],
                    lastRotation.getPitch() + turnSpeed[1]
            );
            ticks++;
        } while (!lastRotation.approximatelyEquals(targetRotation) && ticks < 80);
        return ticks;
    }

    private float[] computeTurnSpeed(RotationDelta prevDiff, RotationDelta diff, boolean crosshair, float distance) {
        float decelerationFactor = this.sigmoidDeceleration.getValue()
                ? this.computeDecelerationFactor(diff.length())
                : 1.0f;

        boolean crosshairCheck = this.dynamicAccel.getValue() && crosshair;
        float distanceFactor = this.coefDistance.getValue() * distance;

        ErrorProvider yawErrorProvider = this.getErrorProvider(true);
        ErrorProvider pitchErrorProvider = this.getErrorProvider(false);

        float[] yawAccelRange = crosshairCheck
                ? new float[]{this.yawCrosshairAccel.random(), this.yawCrosshairAccel.random()}
                : new float[]{this.yawAcceleration.random() + distanceFactor, this.yawAcceleration.random() + distanceFactor};
        float[] pitchAccelRange = crosshairCheck
                ? new float[]{this.pitchCrosshairAccel.random(), this.pitchCrosshairAccel.random()}
                : new float[]{this.pitchAcceleration.random() + distanceFactor, this.pitchAcceleration.random() + distanceFactor};

        float yawAccel = calculateAcceleration(
                diff.getDeltaYaw(),
                prevDiff.getDeltaYaw(),
                yawAccelRange[0],
                yawAccelRange[1],
                decelerationFactor
        );
        float pitchAccel = calculateAcceleration(
                diff.getDeltaPitch(),
                prevDiff.getDeltaPitch(),
                pitchAccelRange[0],
                pitchAccelRange[1],
                decelerationFactor
        );

        return new float[]{
                prevDiff.getDeltaYaw() + yawAccel + yawErrorProvider.getError(yawAccel),
                prevDiff.getDeltaPitch() + pitchAccel + pitchErrorProvider.getError(pitchAccel)
        };
    }

    private ErrorProvider getErrorProvider(boolean yaw) {
        float accelError = 0.0f;
        float constError = 0.0f;
        if (yaw ? this.accelerationError.getValue() : this.accelerationError.getValue()) {
            accelError = yaw ? this.yawAccelerationError.getValue() : this.pitchAccelerationError.getValue();
        }
        if (yaw ? this.constantError.getValue() : this.constantError.getValue()) {
            constError = yaw ? this.yawConstantError.getValue() : this.pitchConstantError.getValue();
        }
        return new ErrorProvider(accelError, constError);
    }

    private static float calculateAcceleration(float yawDiff, float prevYawDiff, float min, float max, float decelerationFactor) {
        return MathHelper.clamp(Rotation.angleDifference(yawDiff, prevYawDiff), min, max) * decelerationFactor;
    }

    private float computeDecelerationFactor(float rotationDifference) {
        float scaled = rotationDifference / 120.0f;
        float sigmoid = (float) (1.0 / (1.0 + Math.exp(-this.decelerationSteepness.getValue() * (scaled - this.decelerationMidpoint.getValue()))));
        return MathHelper.clamp(sigmoid, 0.0f, 1.0f);
    }

    private static class ErrorProvider {
        private final float accelErrorMin;
        private final float accelErrorMax;
        private final float constErrorMin;
        private final float constErrorMax;

        ErrorProvider(float accelError, float constError) {
            this.accelErrorMin = -accelError;
            this.accelErrorMax = accelError;
            this.constErrorMin = -constError;
            this.constErrorMax = constError;
        }

        float getError(float acceleration) {
            float currentAccelerationError = randomInRange(this.accelErrorMin, this.accelErrorMax);
            float currentConstantError = randomInRange(this.constErrorMin, this.constErrorMax);
            return acceleration * currentAccelerationError + currentConstantError;
        }

        private static float randomInRange(float min, float max) {
            if (max <= min) {
                return min;
            }
            return ThreadLocalRandom.current().nextFloat() * (max - min) + min;
        }
    }
}
