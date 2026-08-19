package laoqi123.util.rotation;

import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.IntRangeProperty;
import net.minecraft.util.math.MathHelper;

import java.util.function.Supplier;

public class InterpolationAngleSmooth extends FactorAngleSmooth {
    private final IntRangeProperty horizontalSpeed;
    private final IntRangeProperty verticalSpeed;
    private final IntRangeProperty directionChangeFactor;
    private final FloatProperty midpoint;
    private final Supplier<RotationTarget> previousTargetSupplier;

    public InterpolationAngleSmooth(Supplier<RotationTarget> previousTargetSupplier) {
        this(previousTargetSupplier, 80, 85, 20, 25, 95, 100);
    }

    public InterpolationAngleSmooth(Supplier<RotationTarget> previousTargetSupplier,
                                    int hMin, int hMax, int vMin, int vMax,
                                    int dcMin, int dcMax) {
        super("Interpolation");
        this.previousTargetSupplier = previousTargetSupplier;
        this.horizontalSpeed = this.register(new IntRangeProperty("HorizontalSpeed", hMin, hMax, 1, 100));
        this.verticalSpeed = this.register(new IntRangeProperty("VerticalSpeed", vMin, vMax, 1, 100));
        this.directionChangeFactor = this.register(new IntRangeProperty("DirectionChangeFactor", dcMin, dcMax, 0, 100));
        this.midpoint = this.register(new FloatProperty("Midpoint", 0.35f, 0.0f, 1.0f));
    }

    @Override
    public float[] calculateFactors(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        RotationDelta diff = currentRotation.rotationDeltaTo(targetRotation);
        float yawDiff = Math.abs(diff.getDeltaYaw());
        float pitchDiff = Math.abs(diff.getDeltaPitch());

        RotationTarget previousTarget = this.previousTargetSupplier != null ? this.previousTargetSupplier.get() : null;
        double directionChange = rotationTarget != null && previousTarget != null && rotationTarget.getRotation() != null
                ? MathHelper.clamp((double) rotationTarget.getRotation().angleTo(previousTarget.getRotation()), 0.0, 1.0)
                * (this.directionChangeFactor.random() / 100.0)
                : 0.0;

        float hSpeed = (rotationTarget != null ? this.horizontalSpeed.random() : this.horizontalSpeed.getMin()) / 100.0f;
        float vSpeed = (rotationTarget != null ? this.verticalSpeed.random() : this.verticalSpeed.getMin()) / 100.0f;

        float horizontalFactor = calculateFactor(yawDiff, hSpeed, (float) directionChange);
        float verticalFactor = calculateFactor(pitchDiff, vSpeed, (float) directionChange);
        return new float[]{horizontalFactor * yawDiff, verticalFactor * pitchDiff};
    }

    private float calculateFactor(float diff, float turnSpeed, float directionChange) {
        float t = MathHelper.clamp(diff / 180.0f, 0.0f, 1.0f);
        float bezierSpeed = bezier(0.05f, 1.0f, 1.0f - t);
        float sigmoidSpeed = sigmoid(t);
        return t > this.midpoint.getValue()
                ? bezierSpeed * turnSpeed
                : sigmoidSpeed * MathHelper.clamp(turnSpeed + directionChange, 0.0f, 1.0f);
    }

    private static float sigmoid(float t) {
        return (float) (1.0 / (1.0 + Math.exp(-0.5 * (t - 0.3))));
    }

    private static float bezier(float start, float end, float t) {
        float b = 1.0f - t;
        return b * b * start + 2.0f * b * t * 1.0f + t * t * end;
    }
}
