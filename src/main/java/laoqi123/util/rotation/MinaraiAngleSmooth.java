package laoqi123.util.rotation;

public class MinaraiAngleSmooth extends AngleSmooth {
    private final AngleSmooth fallback;

    public MinaraiAngleSmooth(AngleSmooth fallback) {
        super("Minarai");
        this.fallback = fallback;
    }

    @Override
    public int calculateTicks(Rotation currentRotation, Rotation targetRotation) {
        return 0;
    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        return this.fallback.process(rotationTarget, currentRotation, targetRotation);
    }
}
