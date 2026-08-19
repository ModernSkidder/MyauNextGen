package laoqi123.util.rotation;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;

public interface RotationPreference extends Comparator<Rotation> {
    Vec3d getPreferredSpot(Vec3d eyesPos, double range);

    default Vec3d getPreferredSpotOnBox(Box box, Vec3d eyesPos, double range) {
        return this.getPreferredSpot(eyesPos, range);
    }
}
