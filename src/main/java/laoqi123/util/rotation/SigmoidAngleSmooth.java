package laoqi123.util.rotation;

import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.FloatRangeProperty;

public class SigmoidAngleSmooth extends FactorAngleSmooth {
    private final FloatRangeProperty horizontalTurnSpeed;
    private final FloatRangeProperty verticalTurnSpeed;
    private final FloatProperty steepness;
    private final FloatProperty midpoint;

    public SigmoidAngleSmooth() {
        super("Sigmoid");
        this.horizontalTurnSpeed = this.register(new FloatRangeProperty("HorizontalTurnSpeed", 180.0f, 180.0f, 0.0f, 180.0f));
        this.verticalTurnSpeed = this.register(new FloatRangeProperty("VerticalTurnSpeed", 180.0f, 180.0f, 0.0f, 180.0f));
        this.steepness = this.register(new FloatProperty("Steepness", 10.0f, 0.0f, 20.0f));
        this.midpoint = this.register(new FloatProperty("Midpoint", 0.3f, 0.0f, 1.0f));
    }

    @Override
    public float[] calculateFactors(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        float rotationDifference = currentRotation.angleTo(targetRotation);
        float h = rotationTarget != null ? this.horizontalTurnSpeed.random() : this.horizontalTurnSpeed.getMin();
        float v = rotationTarget != null ? this.verticalTurnSpeed.random() : this.verticalTurnSpeed.getMin();
        return new float[]{
                computeFactor(rotationDifference, h),
                computeFactor(rotationDifference, v)
        };
    }

    private float computeFactor(float rotationDifference, float speed) {
        float scaled = rotationDifference / 120.0f;
        float sigmoid = (float) (1.0 / (1.0 + Math.exp(-this.steepness.getValue() * (scaled - this.midpoint.getValue()))));
        float interpolatedSpeed = sigmoid * speed;
        return Math.max(0.0f, Math.min(180.0f, interpolatedSpeed));
    }
}
