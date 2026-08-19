package laoqi123.module.modules.risevelocity.impl;

import laoqi123.event.types.EventType;
import laoqi123.events.PacketEvent;
import laoqi123.events.TickEvent;
import laoqi123.module.modules.risevelocity.RiseVelocityMode;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.property.properties.ModeProperty;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public class GroundVelocity extends RiseVelocityMode {
    public final BooleanProperty holding = new BooleanProperty("Holding", false);
    public final ModeProperty moveStrafe = new ModeProperty("Move Strafe", 0, new String[]{"Off", "Random"});
    public final IntProperty maxTicks = new IntProperty("Max Ticks", 4, 1, 6);
    public final IntProperty minTicks = new IntProperty("Min Ticks", 2, 1, 5);

    private int lastsTicks;

    @Override
    public String getName() {
        return "Ground";
    }

    @Override
    public void onEnable() {
        this.lastsTicks = 0;
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
        if (this.getTicksSinceTeleport() < 5 || this.isInWeb()) {
            return;
        }
        event.setCancelled(true);
        this.lastsTicks++;
        if (this.lastsTicks >= this.minTicks.getValue() && this.lastsTicks < this.maxTicks.getValue()) {
            if (this.moveStrafe.getValue() == 1 && this.lastsTicks > 1) {
                mc.player.setVelocity(
                        (Math.random() - 0.5) * 0.15,
                        packet.getVelocityY() / 8000.0,
                        (Math.random() - 0.5) * 0.15
                );
            } else {
                mc.player.setVelocity(
                        packet.getVelocityX() / 8000.0,
                        packet.getVelocityY() / 8000.0,
                        packet.getVelocityZ() / 8000.0
                );
            }
        }
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        if (!this.holding.getValue()) {
            this.lastsTicks = 0;
        }
    }
}