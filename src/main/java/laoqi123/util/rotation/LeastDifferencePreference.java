package laoqi123.util.rotation;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class LeastDifferencePreference implements RotationPreference {
    private final Rotation baseRotation;
    private final Vec3d basePoint;

    public LeastDifferencePreference(Rotation baseRotation, Vec3d basePoint) {
        this.baseRotation = baseRotation;
        this.basePoint = basePoint;
    }

    public static LeastDifferencePreference leastDifferenceToLastPoint(Vec3d eyes, Vec3d point) {
        return new LeastDifferencePreference(Rotation.lookingAt(point, eyes), point);
    }

    @Override
    public Vec3d getPreferredSpot(Vec3d eyesPos, double range) {
        if (this.basePoint != null) {
            return this.basePoint;
        }
        Rotation rotation = this.baseRotation;
        return eyesPos.add(rotation.directionVector().multiply(range));
    }

    @Override
    public Vec3d getPreferredSpotOnBox(Box box, Vec3d eyesPos, double range) {
        if (this.basePoint != null) {
            return this.basePoint;
        }
        Vec3d preferred = this.getPreferredSpot(eyesPos, range);
        if (box.contains(preferred)) {
            return preferred;
        }
        Vec3d spotOnBox = AimUtil.firstHit(box, eyesPos, preferred);
        if (spotOnBox != null && eyesPos.squaredDistanceTo(spotOnBox) <= range * range) {
            return spotOnBox;
        }
        return preferred;
    }

    @Override
    public int compare(Rotation o1, Rotation o2) {
        return Float.compare(this.baseRotation.angleTo(o1), this.baseRotation.angleTo(o2));
    }
}
