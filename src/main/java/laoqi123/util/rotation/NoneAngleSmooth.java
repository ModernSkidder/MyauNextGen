package laoqi123.util.rotation;

public class NoneAngleSmooth extends AngleSmooth {
    public NoneAngleSmooth() {
        super("None");
    }

    @Override
    public int calculateTicks(Rotation currentRotation, Rotation targetRotation) {
        return 0;
    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        return currentRotation;
    }
}
