package laoqi123.module.modules.misc;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.PacketEvent;
import laoqi123.module.Module;
import laoqi123.util.ChatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.math.Vec3d;

public class LightningTracker extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private String getDirection(double playerX, double playerZ, double lightningX, double lightningZ) {
        double threshold = Math.sqrt(2.0) - 1.0;
        double xDiff = lightningX - playerX;
        double yDiff = lightningZ - playerZ;
        if (Math.abs(xDiff) > Math.abs(yDiff)) {
            if (Math.abs(yDiff / xDiff) <= threshold) {
                return xDiff > 0.0 ? "E" : "W";
            } else if (xDiff > 0.0) {
                return yDiff > 0.0 ? "SE" : "NE";
            } else {
                return yDiff > 0.0 ? "SW" : "NW";
            }
        } else if (Math.abs(yDiff) > 0.0) {
            if (Math.abs(xDiff / yDiff) <= threshold) {
                return yDiff > 0.0 ? "S" : "N";
            } else if (yDiff > 0.0) {
                return xDiff > 0.0 ? "SE" : "SW";
            } else {
                return xDiff > 0.0 ? "NE" : "NW";
            }
        } else {
            return "?";
        }
    }

    public LightningTracker() {
        super("LightningTracker", false, true);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.RECEIVE && event.getPacket() instanceof EntitySpawnS2CPacket) {
            EntitySpawnS2CPacket packet = (EntitySpawnS2CPacket) event.getPacket();
            if (packet.getEntityType() == EntityType.LIGHTNING_BOLT) {
                double x = packet.getX();
                double y = packet.getY();
                double z = packet.getZ();
                double distance = mc.player.getPos().distanceTo(new Vec3d(x, y, z));
                String direction = this.getDirection(mc.player.getX(), mc.player.getZ(), x, z);
                ChatUtil.sendFormatted(
                        String.format(
                                "&8[&e%s&8] &7X: &f&l%d&r &7Y: &f&l%d&r &7Z: &f&l%d&r &7D: &6&l%d&r &6%s&r",
                                this.getName(),
                                (int) x,
                                (int) y,
                                (int) z,
                                (int) distance,
                                direction
                        )
                );
            }
        }
    }
}
