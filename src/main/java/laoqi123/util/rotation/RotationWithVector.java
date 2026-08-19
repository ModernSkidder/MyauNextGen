package laoqi123.util.rotation;

import net.minecraft.util.math.Vec3d;

public class RotationWithVector {
    private final Rotation rotation;
    private final Vec3d vec;

    public RotationWithVector(Rotation rotation, Vec3d vec) {
        this.rotation = rotation;
        this.vec = vec;
    }

    public Rotation getRotation() {
        return this.rotation;
    }

    public Vec3d getVec() {
        return this.vec;
    }
}
