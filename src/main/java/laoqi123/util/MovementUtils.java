package laoqi123.util;

import laoqi123.event.EventTarget;
import laoqi123.event.impl.MoveInputEvent;
import laoqi123.event.impl.PacketEvent;
import laoqi123.event.impl.TickEvent;
import laoqi123.event.impl.UpdateEvent;
import laoqi123.event.types.EventType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

/**
 * 冻结玩家移动的工具类(用于 Scaffold 的 clutch / 不可达放置场景)。
 * 与 Southside 的 MovementUtils 语义一致:取消移动时【钉住】保存的速度,
 * 而不是把速度清零 —— 清零速度会被 Grim 的 Simulation 判定为速度异常。
 */
public class MovementUtils {
    public static final MovementUtils INSTANCE = new MovementUtils();
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean cancelMove = false;
    private static Vec3d savedVelocity = Vec3d.ZERO;
    private static float savedFallDistance = 0f;
    private static int moveTicks = 0;
    private static int noMovePackets = 0;

    public static void cancelMove() {
        if (mc.player == null) return;
        if (cancelMove) return;
        cancelMove = true;
        // 只保存,不清零
        savedVelocity = mc.player.getVelocity();
        savedFallDistance = mc.player.fallDistance;
    }

    public static void resetMove() {
        cancelMove = false;
        moveTicks = 0;
        noMovePackets = 0;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.PRE) return;
        if (mc.player == null) {
            resetMove();
            return;
        }
        if (!cancelMove || moveTicks > 0) return;
        // 每 tick 恢复保存的速度:速度值保持不变,Grim 不会判定速度异常
        mc.player.setVelocity(savedVelocity);
        mc.player.fallDistance = savedFallDistance;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (cancelMove && moveTicks <= 0) {
            event.setForward(0f);
            event.setStrafe(0f);
            event.setJump(false);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.player == null) {
            resetMove();
            return;
        }
        if (!cancelMove) {
            noMovePackets = 0;
            return;
        }
        if (moveTicks > 0) {
            moveTicks--;
            if (moveTicks <= 0) resetMove();
            return;
        }
        // 连续很多 tick 没有发送位置变化包 → 开始释放冻结,避免一直卡在空中
        if (noMovePackets >= 20) {
            moveTicks = 10;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() != EventType.SEND) return;
        if (!(event.getPacket() instanceof PlayerMoveC2SPacket movePacket)) return;
        if (movePacket.changesPosition()) {
            noMovePackets = 0;
        } else {
            noMovePackets++;
        }
    }
}
