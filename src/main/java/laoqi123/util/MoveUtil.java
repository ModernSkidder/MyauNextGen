package laoqi123.util;

import laoqi123.Myau;
import laoqi123.management.RotationState;
import laoqi123.module.modules.TargetStrafe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class MoveUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean isForwardPressed() {
        if (MoveUtil.mc.options.forwardKey.isPressed() != MoveUtil.mc.options.backKey.isPressed())
            return true;
        return MoveUtil.mc.options.leftKey.isPressed() != MoveUtil.mc.options.rightKey.isPressed();
    }

    public static int getForwardValue() {
        int forwardValue = 0;
        if (MoveUtil.mc.options.forwardKey.isPressed()) {
            ++forwardValue;
        }
        if (MoveUtil.mc.options.backKey.isPressed()) {
            --forwardValue;
        }
        return forwardValue;
    }

    public static int getLeftValue() {
        int leftValue = 0;
        if (MoveUtil.mc.options.leftKey.isPressed()) {
            ++leftValue;
        }
        if (MoveUtil.mc.options.rightKey.isPressed()) {
            --leftValue;
        }
        return leftValue;
    }

    public static float getMoveYaw() {
        return MoveUtil.adjustYaw(RotationState.isActived() ? RotationState.getSmoothedYaw() : MoveUtil.mc.player.getYaw(), MoveUtil.mc.player.input.movementForward, MoveUtil.mc.player.input.movementSideways);
    }

    public static float adjustYaw(float yaw, float forward, float strafe) {
        TargetStrafe targetStrafe = (TargetStrafe) Myau.moduleManager.modules.get(TargetStrafe.class);
        if (targetStrafe.isEnabled()) {
            if (!Float.isNaN(targetStrafe.getTargetYaw())) {
                return targetStrafe.getTargetYaw();
            }
        }
        if (forward < 0.0f) {
            yaw += 180.0f;
        }
        if (strafe != 0.0f) {
            float multiplier = forward == 0.0f ? 1.0f : 0.5f * Math.signum(forward);
            yaw += -90.0f * multiplier * Math.signum(strafe);
        }
        return MathHelper.wrapDegrees(yaw);
    }

    public static float getDirectionYaw() {
        if (MoveUtil.getSpeed() == 0.0) {
            return MathHelper.wrapDegrees(MoveUtil.mc.player.getYaw());
        }
        return MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(MoveUtil.mc.player.getVelocity().z, MoveUtil.mc.player.getVelocity().x)) - 90.0f);
    }

    public static double getBaseMoveSpeed() {
        double baseSpeed = 0.28015;
        if (MoveUtil.getSpeedTime() > 0) {
            baseSpeed = 0.28015 * (1.0 + 0.15 * (double) MoveUtil.getSpeedLevel());
        }
        return baseSpeed;
    }

    public static double getBaseJumpHigh(int speedLevel) {
        double jumpHeight = 0.452;
        if (speedLevel == 1) {
            jumpHeight = 0.49720000000000003;
        } else if (speedLevel >= 2) {
            jumpHeight *= 1.2;
        }
        return jumpHeight;
    }

    public static double getJumpMotion() {
        int speedLevel = 0;
        if (MoveUtil.getSpeedTime() > 0) {
            speedLevel = MoveUtil.getSpeedLevel();
        }
        return MoveUtil.getBaseJumpHigh(speedLevel);
    }

    public static double getSpeed() {
        return MoveUtil.getSpeed(MoveUtil.mc.player.getVelocity().x, MoveUtil.mc.player.getVelocity().z);
    }

    public static boolean isMoving() {
        return mc.player.input.movementForward != 0.0F
                || mc.player.input.movementSideways != 0.0F
                || Math.abs(mc.player.getVelocity().x) > 0.01
                || Math.abs(mc.player.getVelocity().z) > 0.01;
    }

    public static double getSpeed(double motionX, double motionZ) {
        return Math.hypot(motionX, motionZ);
    }

    public static void setSpeed(double speed) {
        MoveUtil.setSpeed(speed, MoveUtil.getDirectionYaw());
    }

    public static void setSpeed(double speed, float yaw) {
        MoveUtil.mc.player.setVelocity(-Math.sin(Math.toRadians(yaw)) * speed, MoveUtil.mc.player.getVelocity().y, Math.cos(Math.toRadians(yaw)) * speed);
    }

    public static void addSpeed(double speed, float yaw) {
        MoveUtil.mc.player.setVelocity(
                MoveUtil.mc.player.getVelocity().x + -Math.sin(Math.toRadians(yaw)) * speed,
                MoveUtil.mc.player.getVelocity().y,
                MoveUtil.mc.player.getVelocity().z + Math.cos(Math.toRadians(yaw)) * speed);
    }

    public static int getSpeedLevel() {
        int speedLevel = 0;
        if (MoveUtil.mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            speedLevel = (MoveUtil.mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1);
        }
        return speedLevel;
    }

    public static int getSpeedTime() {
        if (MoveUtil.mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            return MoveUtil.mc.player.getStatusEffect(StatusEffects.SPEED).getDuration();
        }
        return 0;
    }

    public static float getAllowedHorizontalDistance() {
        float slipperiness = MoveUtil.mc.world.getBlockState(new BlockPos(MathHelper.floor(MoveUtil.mc.player.getX()), MathHelper.floor(MoveUtil.mc.player.getBoundingBox().minY) - 1, MathHelper.floor(MoveUtil.mc.player.getZ()))).getBlock().getSlipperiness() * 0.91f;
        return MoveUtil.mc.player.getMovementSpeed() * (0.16277136f / (slipperiness * slipperiness * slipperiness));
    }

    public static double[] predictMovement() {
        float strafeInput = (float) MoveUtil.getLeftValue() * 0.98f;
        float forwardInput = (float) MoveUtil.getForwardValue() * 0.98f;
        float inputMagnitude = strafeInput * strafeInput + forwardInput * forwardInput;
        if (inputMagnitude >= 1.0E-4f) {
            inputMagnitude = MathHelper.sqrt(inputMagnitude);
            if (inputMagnitude < 1.0f) {
                inputMagnitude = 1.0f;
            }
            inputMagnitude = MoveUtil.getAllowedHorizontalDistance() / inputMagnitude;
            float sinYaw = MathHelper.sin(MoveUtil.mc.player.getYaw() * (float) Math.PI / 180.0f);
            float cosYaw = MathHelper.cos(MoveUtil.mc.player.getYaw() * (float) Math.PI / 180.0f);
            strafeInput *= inputMagnitude;
            forwardInput *= inputMagnitude;
            return new double[]{strafeInput * cosYaw - forwardInput * sinYaw, forwardInput * cosYaw + strafeInput * sinYaw};
        }
        return new double[]{0.0, 0.0};
    }

    public static void fixStrafe(float targetYaw) {
        float angle = MathHelper.wrapDegrees(MoveUtil.adjustYaw(MoveUtil.mc.player.getYaw(), MoveUtil.getForwardValue(), MoveUtil.getLeftValue()) - targetYaw + 22.5f);
        switch ((int) (angle + 180.0f) / 45 % 8) {
            case 0: {
                MoveUtil.mc.player.input.movementForward = -1.0f;
                MoveUtil.mc.player.input.movementSideways = 0.0f;
                break;
            }
            case 1: {
                MoveUtil.mc.player.input.movementForward = -1.0f;
                MoveUtil.mc.player.input.movementSideways = 1.0f;
                break;
            }
            case 2: {
                MoveUtil.mc.player.input.movementForward = 0.0f;
                MoveUtil.mc.player.input.movementSideways = 1.0f;
                break;
            }
            case 3: {
                MoveUtil.mc.player.input.movementForward = 1.0f;
                MoveUtil.mc.player.input.movementSideways = 1.0f;
                break;
            }
            case 4: {
                MoveUtil.mc.player.input.movementForward = 1.0f;
                MoveUtil.mc.player.input.movementSideways = 0.0f;
                break;
            }
            case 5: {
                MoveUtil.mc.player.input.movementForward = 1.0f;
                MoveUtil.mc.player.input.movementSideways = -1.0f;
                break;
            }
            case 6: {
                MoveUtil.mc.player.input.movementForward = 0.0f;
                MoveUtil.mc.player.input.movementSideways = -1.0f;
                break;
            }
            case 7: {
                MoveUtil.mc.player.input.movementForward = -1.0f;
                MoveUtil.mc.player.input.movementSideways = -1.0f;
                break;
            }
        }
        if (MoveUtil.mc.player.input.playerInput.sneak()) {
            MoveUtil.mc.player.input.movementForward *= 0.3f;
            MoveUtil.mc.player.input.movementSideways *= 0.3f;
        }
    }
}
