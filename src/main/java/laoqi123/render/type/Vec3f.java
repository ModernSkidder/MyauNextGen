package laoqi123.render.type;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public record Vec3f(float x, float y, float z) {
    public static final Vec3f ZERO = new Vec3f(0f, 0f, 0f);

    public Vec3f(double x, double y, double z) {
        this((float) x, (float) y, (float) z);
    }

    public Vec3f(Vec3d vec) {
        this(vec.x, vec.y, vec.z);
    }

    public Vec3f(Vec3i vec) {
        this(vec.getX(), vec.getY(), vec.getZ());
    }

    public Vec3f add(float addX, float addY, float addZ) {
        return new Vec3f(x + addX, y + addY, z + addZ);
    }

    public Vec3f add(Vec3f other) {
        return add(other.x, other.y, other.z);
    }

    public Vec3f sub(Vec3f other) {
        return new Vec3f(x - other.x, y - other.y, z - other.z);
    }

    public Vec3f mul(float scale) {
        return new Vec3f(x * scale, y * scale, z * scale);
    }

    public Vec3f rotatePitch(float pitch) {
        float f = (float) Math.cos(pitch);
        float f1 = (float) Math.sin(pitch);
        float d0 = x;
        float d1 = y * f + z * f1;
        float d2 = z * f - y * f1;
        return new Vec3f(d0, d1, d2);
    }

    public Vec3f rotateYaw(float yaw) {
        float f = (float) Math.cos(yaw);
        float f1 = (float) Math.sin(yaw);
        float d0 = x * f + z * f1;
        float d1 = y;
        float d2 = z * f - x * f1;
        return new Vec3f(d0, d1, d2);
    }

    public Vec3d toVec3d() {
        return new Vec3d(x, y, z);
    }
}
