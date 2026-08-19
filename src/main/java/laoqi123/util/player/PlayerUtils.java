package laoqi123.util.player;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.UpdateEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;

public class PlayerUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static int onGroundTicks = 0;
    public static int offGroundTicks = 0;
    public static double lastPitchDiff = 0.0d;
    public static double lastPlacePitchDiff = 0.0d;
    public static float lastAppliedPitch = 0.0f;

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.PRE || mc.player == null) {
            return;
        }
        if (mc.player.isOnGround()) {
            onGroundTicks++;
            offGroundTicks = 0;
        } else {
            offGroundTicks++;
            onGroundTicks = 0;
        }
    }

    public static boolean isMoving() {
        return mc.player != null
                && (mc.player.input.movementForward != 0.0f || mc.player.input.movementSideways != 0.0f);
    }

    public static BlockPos blockRelativeToPlayer(int x, int y, int z) {
        if (mc.player == null) {
            return BlockPos.ORIGIN;
        }
        return BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ()).add(x, y, z);
    }

    public static int getMoveSpeedEffectAmplifier() {
        if (mc.player == null) {
            return 0;
        }
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            return mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
        }
        return 0;
    }
}