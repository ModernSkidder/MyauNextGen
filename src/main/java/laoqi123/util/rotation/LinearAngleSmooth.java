package laoqi123.util.rotation;

import laoqi123.value.properties.FloatRangeValue;

public class LinearAngleSmooth extends FactorAngleSmooth {
    private final FloatRangeValue horizontalTurnSpeed;
    private final FloatRangeValue verticalTurnSpeed;

    public LinearAngleSmooth() {
        this(180.0f, 180.0f, 180.0f, 180.0f);
    }

    public LinearAngleSmooth(float hMin, float hMax, float vMin, float vMax) {
        super("Linear");
        this.horizontalTurnSpeed = this.register(new FloatRangeValue("HorizontalTurnSpeed", hMin, hMax, 0.0f, 180.0f));
        this.verticalTurnSpeed = this.register(new FloatRangeValue("VerticalTurnSpeed", vMin, vMax, 0.0f, 180.0f));
    }

    @Override
    public float[] calculateFactors(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        if (rotationTarget != null) {
            return new float[]{this.horizontalTurnSpeed.random(), this.verticalTurnSpeed.random()};
        }
        return new float[]{this.horizontalTurnSpeed.getMin(), this.verticalTurnSpeed.getMin()};
    }
}
