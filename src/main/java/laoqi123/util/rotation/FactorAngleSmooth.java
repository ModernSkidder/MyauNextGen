package laoqi123.util.rotation;

public abstract class FactorAngleSmooth extends AngleSmooth {
    public FactorAngleSmooth(String name) {
        super(name);
    }

    /**
     * @return horizontal speed, vertical speed
     */
    public abstract float[] calculateFactors(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation);

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        float[] factors = this.calculateFactors(rotationTarget, currentRotation, targetRotation);
        return currentRotation.towardsLinear(targetRotation, factors[0], factors[1]);
    }

    @Override
    public int calculateTicks(Rotation currentRotation, Rotation targetRotation) {
        Rotation current = currentRotation;
        int ticks = -1;
        do {
            float[] factors = this.calculateFactors(null, current, targetRotation);
            current = current.towardsLinear(targetRotation, factors[0], factors[1]);
            ticks++;
        } while (!current.approximatelyEquals(targetRotation) && ticks < 80);
        return ticks;
    }
}
