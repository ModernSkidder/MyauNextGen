package laoqi123.module.modules.combat.risevelocity.impl;

import laoqi123.event.types.EventType;
import laoqi123.events.PacketEvent;
import laoqi123.events.TickEvent;
import laoqi123.module.modules.combat.risevelocity.RiseVelocityMode;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.IntProperty;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

import java.util.ArrayList;
import java.util.List;

public class DelayVelocity extends RiseVelocityMode {
    public final IntProperty delayTicks = new IntProperty("Delay Ticks", 5, 0, 20);
    public final BooleanProperty limitDelay = new BooleanProperty("Limit Delay", true);
    public final BooleanProperty onDrop = new BooleanProperty("On Drop", false);

    private final List<Packet<ClientPlayPacketListener>> delayedPackets = new ArrayList<>();
    private int ticks;

    @Override
    public String getName() {
        return "Delay";
    }

    @Override
    public void onEnable() {
        this.delayedPackets.clear();
        this.ticks = 0;
    }

    @Override
    public void onPacketReceive(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE) {
            return;
        }
        if (!(event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet)) {
            return;
        }
        if (packet.getEntityId() != mc.player.getId()) {
            return;
        }
        if (this.getTicksSinceTeleport() < 3 || this.isInWeb()) {
            return;
        }
        event.setCancelled(true);
        this.delayedPackets.add((Packet<ClientPlayPacketListener>) event.getPacket());
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        if (this.onDrop.getValue() && mc.player.isOnGround() && !this.delayedPackets.isEmpty()) {
            this.flush();
        }
        if (!this.limitDelay.getValue()) {
            return;
        }
        this.ticks++;
        if (this.ticks >= this.delayTicks.getValue()) {
            this.ticks = 0;
            this.flush();
        }
    }

    private void flush() {
        for (Packet<ClientPlayPacketListener> packet : this.delayedPackets) {
            this.receive(packet);
        }
        this.delayedPackets.clear();
    }
}