package laoqi123.module.modules.combat.risevelocity.impl;

import laoqi123.event.types.EventType;
import laoqi123.events.PacketEvent;
import laoqi123.module.modules.combat.risevelocity.RiseVelocityMode;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public class BounceVelocity extends RiseVelocityMode {

    @Override
    public String getName() {
        return "Bounce";
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
        double velocityY = packet.getVelocityY();
        if (velocityY > 0) {
            double y = 0.42;
            if (velocityY / 8000.0 >= 2.0) {
                y = velocityY / 8000.0;
            }
            mc.player.setVelocity(packet.getVelocityX() / 8000.0, y, packet.getVelocityZ() / 8000.0);
        }
    }
}