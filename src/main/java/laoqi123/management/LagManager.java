package laoqi123.management;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.PacketEvent;
import laoqi123.events.TickEvent;
import laoqi123.util.PacketUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LagManager {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final Deque<LagPacket> packetQueue;
    private int tickDelay;
    private boolean flushing;
    private Vec3d lastPosition;

    public LagManager() {
        this.packetQueue = new ConcurrentLinkedDeque<>();
        this.tickDelay = 0;
        this.flushing = false;
        this.lastPosition = new Vec3d(0.0, 0.0, 0.0);
    }

    private void flushQueue() {
        if (mc.getNetworkHandler() == null) {
            this.packetQueue.clear();
        } else {
            for (this.flushing = true; !this.packetQueue.isEmpty(); this.packetQueue.poll()) {
                LagPacket lagPacket = this.packetQueue.peek();
                if (this.tickDelay > 0 && lagPacket.delay <= this.tickDelay) {
                    break;
                }
                PacketUtil.sendPacketNoEvent(lagPacket.packet);
                if (lagPacket.packet instanceof PlayerMoveC2SPacket) {
                    PlayerMoveC2SPacket c03 = (PlayerMoveC2SPacket) lagPacket.packet;
                    if (c03.changesPosition()) {
                        this.lastPosition = new Vec3d(c03.getX(0.0), c03.getY(0.0), c03.getZ(0.0));
                    }
                }
            }
            this.flushing = false;
        }
    }

    private void incrementDelays() {
        this.packetQueue.forEach(z -> z.delay++);
    }

    public boolean handlePacket(Packet<?> packet) {
        this.flushQueue();
        if (packet instanceof KeepAliveC2SPacket || packet instanceof ChatMessageC2SPacket) {
            return false;
        } else if ((long) this.tickDelay > 0L) {
            this.packetQueue.offer(new LagPacket(packet));
            return true;
        } else {
            if (packet instanceof PlayerMoveC2SPacket) {
                PlayerMoveC2SPacket c03 = (PlayerMoveC2SPacket) packet;
                if (c03.changesPosition()) {
                    this.lastPosition = new Vec3d(c03.getX(0.0), c03.getY(0.0), c03.getZ(0.0));
                }
            }
            return false;
        }
    }

    public void setDelay(int delay) {
        this.tickDelay = delay;
    }

    public Vec3d getLastPosition() {
        return this.lastPosition;
    }

    public boolean isFlushing() {
        return this.flushing;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.POST) {
            if (mc.player != null && mc.player.isRemoved()) {
                this.setDelay(0);
            }
            this.incrementDelays();
            this.flushQueue();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof HandshakeC2SPacket
                || event.getPacket() instanceof LoginHelloC2SPacket
                || event.getPacket() instanceof QueryRequestC2SPacket
                || event.getPacket() instanceof QueryPingC2SPacket
                || event.getPacket() instanceof LoginKeyC2SPacket) {
            this.setDelay(0);
        }
    }

    public static class LagPacket {
        public final Packet<?> packet;
        public int delay;

        public LagPacket(Packet<?> packet) {
            this.packet = packet;
            this.delay = 0;
        }
    }
}
