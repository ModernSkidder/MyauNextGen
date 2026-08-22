package laoqi123.module.modules.risevelocity.impl;

import laoqi123.event.types.EventType;
import laoqi123.events.PacketEvent;
import laoqi123.events.TickEvent;
import laoqi123.module.modules.risevelocity.RiseVelocityMode;
import laoqi123.property.properties.IntProperty;
import laoqi123.util.PacketUtil;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TickVelocity extends RiseVelocityMode {
    public final IntProperty tickDelay = new IntProperty("Tick Delay", 3, 0, 8);

    private final Queue<Packet<ClientPlayPacketListener>> queue = new ConcurrentLinkedQueue<>();
    private int queueTicks;

    @Override
    public String getName() {
        return "Tick";
    }

    @Override
    public void onEnable() {
        this.queue.clear();
        this.queueTicks = 0;
    }

    @Override
    public void onPacketReceive(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE) {
            return;
        }
        Packet<?> packet = event.getPacket();
        boolean hold = packet instanceof EntityVelocityUpdateS2CPacket velocity
                && velocity.getEntityId() == mc.player.getId()
                && this.getTicksSinceTeleport() >= 3 && !this.isInWeb();
        hold |= packet instanceof EntityS2CPacket || packet instanceof EntityPositionS2CPacket
                || packet instanceof EntityPositionSyncS2CPacket;
        if (!hold) {
            return;
        }
        event.setCancelled(true);
        this.queue.offer((Packet<ClientPlayPacketListener>) packet);
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        if (this.queue.isEmpty()) {
            return;
        }
        this.queueTicks++;
        if (this.queueTicks < this.tickDelay.getValue()) {
            return;
        }
        this.queueTicks = 0;
        Packet<ClientPlayPacketListener> packet;
        while ((packet = this.queue.poll()) != null) {
            PacketUtil.receivePacket(packet);
        }
        PacketUtil.drainPendingPackets();
    }
}