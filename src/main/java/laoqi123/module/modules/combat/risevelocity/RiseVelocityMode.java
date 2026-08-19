package laoqi123.module.modules.combat.risevelocity;

import laoqi123.event.impl.MoveInputEvent;
import laoqi123.event.impl.PacketEvent;
import laoqi123.event.impl.PlayerUpdateEvent;
import laoqi123.event.impl.TickEvent;
import laoqi123.module.modules.combat.RiseVelocity;
import laoqi123.util.PacketUtil;
import laoqi123.util.TeamUtil;
import net.minecraft.block.CobwebBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;

public abstract class RiseVelocityMode {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private RiseVelocity parent;

    public abstract String getName();

    public void setParent(RiseVelocity parent) {
        this.parent = parent;
    }

    public RiseVelocity getParent() {
        return this.parent;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onPacketReceive(PacketEvent event) {
    }

    public void onTick(TickEvent event) {
    }

    public void onMoveInput(MoveInputEvent event) {
    }

    public void onPlayerUpdate(PlayerUpdateEvent event) {
    }

    protected int getTicksSinceTeleport() {
        return this.parent.getTicksSinceTeleport();
    }

    protected int getTicksSinceAttack() {
        return this.parent.getTicksSinceAttack();
    }

    protected int getJumpTicks() {
        return this.parent.getJumpTicks();
    }

    protected boolean isInWeb() {
        return mc.world != null && mc.player != null
                && mc.world.getBlockState(mc.player.getBlockPos()).getBlock() instanceof CobwebBlock;
    }

    protected void receive(Packet<?> packet) {
        PacketUtil.receivePacket(packet);
        PacketUtil.drainPendingPackets();
    }

    protected LivingEntity getClosestTarget(double range) {
        if (mc.world == null || mc.player == null) {
            return null;
        }
        LivingEntity closest = null;
        double bestDistance = range;
        for (net.minecraft.entity.Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity)) {
                continue;
            }
            PlayerEntity playerEntity = (PlayerEntity) entity;
            if (playerEntity == mc.player || !TeamUtil.isEntityLoaded(playerEntity)) {
                continue;
            }
            double distance = mc.player.distanceTo(playerEntity);
            if (distance < bestDistance) {
                bestDistance = distance;
                closest = playerEntity;
            }
        }
        return closest;
    }
}