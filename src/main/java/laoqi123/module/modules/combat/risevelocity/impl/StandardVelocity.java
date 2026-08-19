package laoqi123.module.modules.combat.risevelocity.impl;

import laoqi123.event.types.EventType;
import laoqi123.event.impl.PacketEvent;
import laoqi123.module.modules.combat.risevelocity.RiseVelocityMode;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public class StandardVelocity extends RiseVelocityMode {

    @Override
    public String getName() {
        return "Standard";
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
    }
}