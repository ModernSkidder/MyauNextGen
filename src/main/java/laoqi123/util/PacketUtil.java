package laoqi123.util;

import com.mojang.logging.LogUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PacketUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Queue<Packet<ClientPlayPacketListener>> pendingReplay = new ConcurrentLinkedQueue<>();

    public static void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(packet);
        }
    }

    public static boolean isWorldRenderPacket(Packet<?> packet) {
        return packet instanceof ChunkDataS2CPacket
                || packet instanceof ChunkDeltaUpdateS2CPacket
                || packet instanceof BlockUpdateS2CPacket
                || packet instanceof BlockEventS2CPacket
                || packet instanceof BlockBreakingProgressS2CPacket
                || packet instanceof WorldEventS2CPacket;
    }

    public static void sendPacketNoEvent(Packet<?> packet) {
        PacketUtil.sendPacket(packet);
    }

    public static void receivePacket(Packet<?> packet) {
        if (packet == null) return;
        pendingReplay.offer((Packet<ClientPlayPacketListener>) packet);
    }

    public static void drainPendingPackets() {
        if (pendingReplay.isEmpty()) return;
        Packet<ClientPlayPacketListener> packet;
        while ((packet = pendingReplay.poll()) != null) {
            try {
                if (mc.getNetworkHandler() != null) {
                    packet.apply(mc.getNetworkHandler());
                }
            } catch (Exception e) {
                LogUtils.getLogger().error("Failed to replay packet", e);
            }
        }
    }
}
