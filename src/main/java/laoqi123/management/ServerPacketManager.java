package laoqi123.management;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.PacketEvent;
import laoqi123.events.TickEvent;
import laoqi123.util.PacketUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ServerPacketManager {
    public static boolean deSyncing = false;
    public static int deSyncTick = 0;
    private static final Deque<Packet<?>> heldPackets = new ConcurrentLinkedDeque<>();

    public static void setup() {
        deSyncing = true;
        deSyncTick = 0;
    }

    public static void reset(boolean fullReset) {
        deSyncing = false;
        deSyncTick = 0;
        if (fullReset) {
            heldPackets.clear();
        }
    }

    public static void releaseTick(boolean unsafe) {
        deSyncing = false;
        deSyncTick = 0;
        flush();
    }

    private static void flush() {
        Packet<?> packet;
        while ((packet = heldPackets.poll()) != null) {
            PacketUtil.sendPacketNoEvent(packet);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && deSyncing && event.getPacket() instanceof PlayerMoveC2SPacket) {
            heldPackets.offer(event.getPacket());
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.POST && deSyncing) {
            deSyncTick++;
        }
    }
}