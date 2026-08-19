package laoqi123.util.rotation;

public class RotationDelta {
    private final float deltaYaw;
    private final float deltaPitch;

    public RotationDelta(float deltaYaw, float deltaPitch) {
        this.deltaYaw = deltaYaw;
        this.deltaPitch = deltaPitch;
    }

    public float getDeltaYaw() {
        return this.deltaYaw;
    }

    public float getDeltaPitch() {
        return this.deltaPitch;
    }

    public float length() {
        return (float) Math.hypot(this.deltaYaw, this.deltaPitch);
    }
}
