package laoqi123.util.rotation;

import laoqi123.util.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AimUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static Vec3d firstHit(Box box, Vec3d start, Vec3d end) {
        Optional<Vec3d> hit = box.raycast(start, end);
        return hit.orElse(null);
    }

    public static boolean canSeePointFrom(Vec3d eyes, Vec3d point) {
        HitResult result = mc.world.raycast(new RaycastContext(
                eyes,
                point,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return result.getType() == HitResult.Type.MISS;
    }

    public static boolean facingEnemy(LivingEntity entity, double range, Rotation rotation) {
        return RotationUtil.rayTrace(
                entity.getBoundingBox().expand(0.1, 0.1, 0.1),
                rotation.getYaw(),
                rotation.getPitch(),
                range
        ) != null;
    }

    public static Vec3d getNearestPoint(Box box, Vec3d point) {
        return new Vec3d(
                Math.max(box.minX, Math.min(point.x, box.maxX)),
                Math.max(box.minY, Math.min(point.y, box.maxY)),
                Math.max(box.minZ, Math.min(point.z, box.maxZ))
        );
    }

    public static RotationWithVector raytraceBox(Vec3d eyes,
                                                 Box box,
                                                 double range,
                                                 double wallsRange,
                                                 RotationPreference preference,
                                                 boolean prioritizeVisible) {
        double rangeSq = range * range;
        double wallsRangeSq = wallsRange * wallsRange;

        Vec3d preferredSpot = preference.getPreferredSpotOnBox(box, eyes, range);
        Vec3d preferredSpotOnBox = AimUtil.firstHit(box, eyes, preferredSpot);
        if (preferredSpotOnBox != null) {
            double preferredSpotDistance = eyes.squaredDistanceTo(preferredSpotOnBox);
            boolean validCauseBelowWallsRange = preferredSpotDistance < wallsRangeSq;
            boolean validCauseVisible = AimUtil.canSeePointFrom(eyes, preferredSpotOnBox);
            if (validCauseBelowWallsRange || (validCauseVisible && preferredSpotDistance < rangeSq)) {
                return new RotationWithVector(Rotation.lookingAt(preferredSpot, eyes), preferredSpotOnBox);
            }
        }

        BestRotationTracker tracker = new BestRotationTracker(preference, !prioritizeVisible);
        Vec3d nearestSpot = AimUtil.getNearestPoint(box, eyes);
        if (nearestSpot != null) {
            considerSpot(tracker, preferredSpot, box, eyes, rangeSq, wallsRangeSq, nearestSpot);
        }
        List<Vec3d> points = AimUtil.projectPointsOnBox(eyes, box, 256);
        if (points != null) {
            for (Vec3d spot : points) {
                considerSpot(tracker, preferredSpot, box, eyes, rangeSq, wallsRangeSq, spot);
            }
        } else {
            double sizeX = box.getLengthX();
            double sizeY = box.getLengthY();
            double sizeZ = box.getLengthZ();
            for (double x = 0.05; x < 0.95; x += 0.1) {
                for (double y = 0.05; y < 0.95; y += 0.1) {
                    for (double z = 0.05; z < 0.95; z += 0.1) {
                        considerSpot(tracker, preferredSpot, box, eyes, rangeSq, wallsRangeSq,
                                new Vec3d(box.minX + sizeX * x, box.minY + sizeY * y, box.minZ + sizeZ * z));
                    }
                }
            }
        }
        return tracker.getBestVisible() != null ? tracker.getBestVisible() : tracker.getBestInvisible();
    }

    private static void considerSpot(BestRotationTracker tracker,
                                     Vec3d preferredSpot,
                                     Box box,
                                     Vec3d eyes,
                                     double rangeSq,
                                     double wallsRangeSq,
                                     Vec3d spot) {
        Vec3d raycastTarget = preferredSpot.subtract(eyes).multiply(2.0).add(eyes);
        Vec3d spotOnBox = AimUtil.firstHit(box, eyes, raycastTarget);
        if (spotOnBox == null) {
            return;
        }
        double distSq = eyes.squaredDistanceTo(spotOnBox);
        boolean visible = AimUtil.canSeePointFrom(eyes, spotOnBox);
        if ((!visible || distSq >= rangeSq) && distSq >= wallsRangeSq) {
            return;
        }
        Rotation rotation = Rotation.lookingAt(spot, eyes);
        tracker.considerRotation(new RotationWithVector(rotation, spot), visible);
    }

    /**
     * Simplified port of LB's projectPointsOnBox: generates a grid of points on
     * the box faces that face the observer. Returns null when the eyes are inside the box.
     */
    public static List<Vec3d> projectPointsOnBox(Vec3d eyes, Box box, int maxPoints) {
        if (box.contains(eyes)) {
            return null;
        }
        List<Vec3d> points = new ArrayList<>();
        double[] grid = new double[10];
        for (int i = 0; i < 10; i++) {
            grid[i] = 0.05 + 0.1 * i;
        }
        double sizeX = box.getLengthX();
        double sizeY = box.getLengthY();
        double sizeZ = box.getLengthZ();

        // +X / -X faces
        if (box.maxX < eyes.x) {
            for (double y : grid) {
                for (double z : grid) {
                    points.add(new Vec3d(box.maxX, box.minY + sizeY * y, box.minZ + sizeZ * z));
                }
            }
        } else if (box.minX > eyes.x) {
            for (double y : grid) {
                for (double z : grid) {
                    points.add(new Vec3d(box.minX, box.minY + sizeY * y, box.minZ + sizeZ * z));
                }
            }
        }
        // +Y / -Y faces
        if (box.maxY < eyes.y) {
            for (double x : grid) {
                for (double z : grid) {
                    points.add(new Vec3d(box.minX + sizeX * x, box.maxY, box.minZ + sizeZ * z));
                }
            }
        } else if (box.minY > eyes.y) {
            for (double x : grid) {
                for (double z : grid) {
                    points.add(new Vec3d(box.minX + sizeX * x, box.minY, box.minZ + sizeZ * z));
                }
            }
        }
        // +Z / -Z faces
        if (box.maxZ < eyes.z) {
            for (double x : grid) {
                for (double y : grid) {
                    points.add(new Vec3d(box.minX + sizeX * x, box.minY + sizeY * y, box.maxZ));
                }
            }
        } else if (box.minZ > eyes.z) {
            for (double x : grid) {
                for (double y : grid) {
                    points.add(new Vec3d(box.minX + sizeX * x, box.minY + sizeY * y, box.minZ));
                }
            }
        }

        if (points.size() > maxPoints) {
            List<Vec3d> sampled = new ArrayList<>(maxPoints);
            double step = (double) points.size() / maxPoints;
            for (int i = 0; i < maxPoints; i++) {
                sampled.add(points.get(Math.min(points.size() - 1, (int) ((i + 0.5) * step))));
            }
            return sampled;
        }
        return points;
    }

    private static class BestRotationTracker {
        private final Comparator<Rotation> comparator;
        private final boolean ignoreVisibility;
        private RotationWithVector bestVisible;
        private RotationWithVector bestInvisible;

        BestRotationTracker(Comparator<Rotation> comparator, boolean ignoreVisibility) {
            this.comparator = comparator;
            this.ignoreVisibility = ignoreVisibility;
        }

        void considerRotation(RotationWithVector rotation, boolean visible) {
            if (visible || this.ignoreVisibility) {
                if (this.isRotationBetter(this.bestVisible, rotation)) {
                    this.bestVisible = rotation;
                }
            } else {
                if (this.isRotationBetter(this.bestInvisible, rotation)) {
                    this.bestInvisible = rotation;
                }
            }
        }

        private boolean isRotationBetter(RotationWithVector base, RotationWithVector candidate) {
            return base == null || this.comparator.compare(base.getRotation(), candidate.getRotation()) > 0;
        }

        RotationWithVector getBestVisible() {
            return this.bestVisible;
        }

        RotationWithVector getBestInvisible() {
            return this.bestInvisible;
        }
    }
}
