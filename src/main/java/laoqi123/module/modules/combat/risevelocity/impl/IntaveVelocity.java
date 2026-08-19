package laoqi123.module.modules.combat.risevelocity.impl;

import laoqi123.event.types.EventType;
import laoqi123.event.impl.MoveInputEvent;
import laoqi123.event.impl.PacketEvent;
import laoqi123.module.modules.combat.risevelocity.RiseVelocityMode;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.IntValue;
import laoqi123.util.PacketUtil;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

import java.util.ArrayList;
import java.util.List;

public class IntaveVelocity extends RiseVelocityMode {
    public final IntValue startTicks = new IntValue("Start Ticks", 2, 0, 4);
    public final BooleanValue alwaysDrain = new BooleanValue("Always Drain", false);
    public final BooleanValue waitForLowTicks = new BooleanValue("Wait For Low Ticks", false);

    private final List<Packet<ClientPlayPacketListener>> delayedPackets = new ArrayList<>();
    private int currentTicks;

    @Override
    public String getName() {
        return "Intave";
    }

    @Override
    public void onEnable() {
        this.delayedPackets.clear();
        this.currentTicks = 0;
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

        if (this.alwaysDrain.getValue()) {
            this.sendDelayed();
            return;
        }
        if (mc.player.isOnGround()) {
            this.sendDelayed();
            return;
        }
        if (!this.waitForLowTicks.getValue()) {
            this.sendDelayed();
            return;
        }
        this.currentTicks++;
        if (this.currentTicks >= this.startTicks.getValue()) {
            this.sendDelayed();
        }
    }

    @Override
    public void onMoveInput(MoveInputEvent event) {
        if (mc.player == null) {
            return;
        }
        if (!this.delayedPackets.isEmpty() && mc.player.isOnGround()) {
            event.setJump(true);
        }
    }

    private void sendDelayed() {
        for (Packet<ClientPlayPacketListener> packet : this.delayedPackets) {
            PacketUtil.receivePacket(packet);
        }
        PacketUtil.drainPendingPackets();
        this.delayedPackets.clear();
        this.currentTicks = 0;
    }
}