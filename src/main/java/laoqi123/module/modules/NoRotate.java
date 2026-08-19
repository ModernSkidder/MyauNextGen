package laoqi123.module.modules;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.LoadWorldEvent;
import laoqi123.events.PacketEvent;
import laoqi123.module.Module;
import laoqi123.util.PacketUtil;
import laoqi123.util.RandomUtil;
import laoqi123.util.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;

import java.util.Set;

public class NoRotate extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private boolean reset = false;

    public NoRotate() {
        super("NoRotate", false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.RECEIVE && !event.isCancelled() && mc.player != null && mc.world != null) {
            if (mc.player.getYaw() != -180.0F || mc.player.getPitch() != 0.0F) {
                if (event.getPacket() instanceof ChatMessageS2CPacket) {
                    ChatMessageS2CPacket chatPacket = (ChatMessageS2CPacket) event.getPacket();
                    String msg = chatPacket.unsignedContent() != null ? chatPacket.unsignedContent().getString() : chatPacket.body().content();
                    if (msg.contains("§e§lProtect your bed and destroy the enemy beds.") || msg.contains("§eYou will respawn in §r§c1 §r§esecond!")) {
                        this.reset = true;
                    }
                }
                if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
                    if (this.reset) {
                        this.reset = false;
                        return;
                    }
                    PlayerPositionLookS2CPacket packet = (PlayerPositionLookS2CPacket) event.getPacket();
                    event.setCancelled(true);
                    double x = packet.change().position().x;
                    double y = packet.change().position().y;
                    double z = packet.change().position().z;
                    float yaw = packet.change().yaw();
                    float pitch = packet.change().pitch();
                    Set<PositionFlag> flags = packet.relatives();
                    if (flags.contains(PositionFlag.X)) {
                        x += mc.player.getX();
                    } else {
                        mc.player.setVelocity(0.0, mc.player.getVelocity().y, mc.player.getVelocity().z);
                    }
                    if (flags.contains(PositionFlag.Y)) {
                        y += mc.player.getY();
                    } else {
                        mc.player.setVelocity(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);
                    }
                    if (flags.contains(PositionFlag.Z)) {
                        z += mc.player.getZ();
                    } else {
                        mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y, 0.0);
                    }
                    if (flags.contains(PositionFlag.X_ROT)) {
                        pitch += mc.player.getPitch();
                    }
                    if (flags.contains(PositionFlag.Y_ROT)) {
                        yaw += mc.player.getYaw();
                    }
                    mc.player
                            .updatePositionAndAngles(
                                    x,
                                    y,
                                    z,
                                    RotationUtil.quantizeAngle(mc.player.getYaw() + RandomUtil.nextFloat(-0.01F, 0.01F)),
                                    RotationUtil.quantizeAngle(mc.player.getPitch() + RandomUtil.nextFloat(-0.01F, 0.01F))
                            );
                    PacketUtil.sendPacketNoEvent(
                            new PlayerMoveC2SPacket.Full(
                                    mc.player.getX(), mc.player.getBoundingBox().minY, mc.player.getZ(), yaw % 360.0F, pitch % 360.0F, false, mc.player.horizontalCollision
                            )
                    );
                }
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.reset = false;
    }

    @Override
    public void onDisabled() {
        this.reset = false;
    }
}
