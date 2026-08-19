package laoqi123.util;

import laoqi123.util.raytrace.ClientRayTraceUtil;
import laoqi123.util.rotation.Rotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static float normalizeYawDiff(float yaw, float target) {
        return Math.abs(MathHelper.wrapDegrees(yaw - target));
    }

    public static float yawDiffDirectly(float yaw, float target) {
        return MathHelper.wrapDegrees(yaw - target);
    }

    public static float smooth(float diff, float smooth) {
        if (smooth == 0.0f) {
            return 0.0f;
        }
        return Math.signum(diff) * Math.min(Math.abs(diff), Math.abs(smooth));
    }

    public static Rotation getClosestToBlockFace(BlockPos pos, Direction face, float yaw, float pitch) {
        if (pos == null || face == null || mc.player == null) {
            return null;
        }
        Vec3d eye = ClientRayTraceUtil.eyePos != null ? ClientRayTraceUtil.eyePos : mc.player.getEyePos();

        double[] axis1 = {0.25, 0.5, 0.75};
        double[] axis2 = {0.25, 0.5, 0.75};

        Rotation best = null;
        double bestDiff = Double.MAX_VALUE;
        for (double a : axis1) {
            for (double b : axis2) {
                double px;
                double py;
                double pz;
                switch (face) {
                    case UP -> {
                        px = pos.getX() + a;
                        py = pos.getY() + 1.0;
                        pz = pos.getZ() + b;
                    }
                    case DOWN -> {
                        px = pos.getX() + a;
                        py = pos.getY();
                        pz = pos.getZ() + b;
                    }
                    case EAST -> {
                        px = pos.getX() + 1.0;
                        py = pos.getY() + a;
                        pz = pos.getZ() + b;
                    }
                    case WEST -> {
                        px = pos.getX();
                        py = pos.getY() + a;
                        pz = pos.getZ() + b;
                    }
                    case SOUTH -> {
                        px = pos.getX() + a;
                        py = pos.getY() + b;
                        pz = pos.getZ() + 1.0;
                    }
                    default -> {
                        px = pos.getX() + a;
                        py = pos.getY() + b;
                        pz = pos.getZ();
                    }
                }
                Rotation rot = new Rotation(
                        MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(pz - eye.z, px - eye.x)) - 90.0f),
                        MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(py - eye.y, Math.hypot(px - eye.x, pz - eye.z))))
                );
                if (!ClientRayTraceUtil.didHitBlockFace(rot, pos, face, true)) {
                    continue;
                }
                float yawDiff = Math.abs(MathHelper.wrapDegrees(rot.getYaw() - yaw));
                float pitchDiff = Math.abs(rot.getPitch() - pitch);
                double diff = yawDiff + pitchDiff;
                if (diff < bestDiff) {
                    bestDiff = diff;
                    best = rot;
                }
            }
        }
        return best;
    }
}