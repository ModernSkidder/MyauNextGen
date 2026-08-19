package laoqi123.module.modules.combat.risevelocity.impl;

import laoqi123.event.types.EventType;
import laoqi123.events.PacketEvent;
import laoqi123.events.TickEvent;
import laoqi123.module.modules.combat.risevelocity.RiseVelocityMode;
import laoqi123.property.properties.IntProperty;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public class LegitVelocity extends RiseVelocityMode {
    public final IntProperty delayTicks = new IntProperty("Delay Ticks", 5, 0, 20);

    private int pendingTicks;

    @Override
    public String getName() {
        return "Legit";
    }

    @Override
    public void onEnable() {
        this.pendingTicks = 0;
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
        if (packet.getVelocityY() > 0) {
            this.pendingTicks = this.delayTicks.getValue();
            event.setCancelled(true);
        }
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        if (this.pendingTicks > 0) {
            this.pendingTicks--;
        } else {
            this.pendingTicks = 0;
        }
    }
}