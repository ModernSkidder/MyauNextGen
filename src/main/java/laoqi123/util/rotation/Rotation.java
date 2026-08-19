package laoqi123.util.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Rotation {
    public static final Rotation ZERO = new Rotation(0.0f, 0.0f);
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final float yaw;
    private final float pitch;
    private final boolean normalized;

    public Rotation(float yaw, float pitch) {
        this(yaw, pitch, false);
    }

    public Rotation(float yaw, float pitch, boolean normalized) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.normalized = normalized;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public boolean isNormalized() {
        return this.normalized;
    }

    public static Rotation lookingAt(Vec3d point, Vec3d from) {
        return Rotation.fromRotationVec(point.subtract(from));
    }

    public static Rotation fromRotationVec(Vec3d vec) {
        return Rotation.fromRotationVec(vec.x, vec.y, vec.z);
    }

    public static Rotation fromRotationVec(double x, double y, double z) {
        return new Rotation(
                MathHelper.wrapDegrees((float) (Math.atan2(z, x) * 180.0 / Math.PI) - 90.0f),
                MathHelper.wrapDegrees((float) (-Math.atan2(y, Math.hypot(x, z)) * 180.0 / Math.PI))
        );
    }

    public Vec3d directionVector() {
        float f = MathHelper.cos(-this.yaw * 0.017453292F - 3.1415927F);
        float f1 = MathHelper.sin(-this.yaw * 0.017453292F - 3.1415927F);
        float f2 = -MathHelper.cos(-this.pitch * 0.017453292F);
        float f3 = MathHelper.sin(-this.pitch * 0.017453292F);
        return new Vec3d(f1 * f2, f3, f * f2);
    }

    public static double gcd() {
        float f = (float) (mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2);
        return f * f * f * 8.0 * 0.15;
    }

    public Rotation normalize(Rotation baseRotation) {
        if (this.normalized) {
            return this;
        }
        double gcd = Rotation.gcd();
        RotationDelta diff = baseRotation.rotationDeltaTo(this);
        double g1 = Math.round(diff.getDeltaYaw() / gcd) * gcd;
        double g2 = Math.round(diff.getDeltaPitch() / gcd) * gcd;
        float yaw = baseRotation.getYaw() + (float) g1;
        float pitch = Math.max(-90.0f, Math.min(90.0f, baseRotation.getPitch() + (float) g2));
        return new Rotation(yaw, pitch, true);
    }

    public Rotation normalize() {
        if (Rotation.mc.player == null) {
            return this;
        }
        return this.normalize(new Rotation(Rotation.mc.player.getYaw(), Rotation.mc.player.getPitch()));
    }

    public static float angleDifference(float a, float b) {
        return MathHelper.wrapDegrees(a - b);
    }

    public RotationDelta rotationDeltaTo(Rotation other) {
        return new RotationDelta(
                Rotation.angleDifference(other.getYaw(), this.getYaw()),
                Rotation.angleDifference(other.getPitch(), this.getPitch())
        );
    }

    public float angleTo(Rotation other) {
        return Math.min(this.rotationDeltaTo(other).length(), 180.0f);
    }

    public Rotation towardsLinear(Rotation other, float horizontalFactor, float verticalFactor) {
        RotationDelta diff = this.rotationDeltaTo(other);
        float rotationDifference = diff.length();
        if (rotationDifference <= 0.001f) {
            return this;
        }
        float straightLineYaw = Math.abs(diff.getDeltaYaw() / rotationDifference) * horizontalFactor;
        float straightLinePitch = Math.abs(diff.getDeltaPitch() / rotationDifference) * verticalFactor;
        return new Rotation(
                this.yaw + MathHelper.clamp(diff.getDeltaYaw(), -straightLineYaw, straightLineYaw),
                this.pitch + MathHelper.clamp(diff.getDeltaPitch(), -straightLinePitch, straightLinePitch)
        );
    }

    public boolean approximatelyEquals(Rotation other, float tolerance) {
        return this.angleTo(other) <= tolerance;
    }

    public boolean approximatelyEquals(Rotation other) {
        return this.approximatelyEquals(other, 2.0f);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Rotation)) {
            return false;
        }
        Rotation other = (Rotation) obj;
        return Float.floatToIntBits(this.yaw) == Float.floatToIntBits(other.yaw)
                && Float.floatToIntBits(this.pitch) == Float.floatToIntBits(other.pitch);
    }

    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(this.yaw);
        result = 31 * result + Float.floatToIntBits(this.pitch);
        return result;
    }

    @Override
    public String toString() {
        return "Rotation(yaw=" + this.yaw + ", pitch=" + this.pitch + ")";
    }
}
