package laoqi123.module.modules.combat.risevelocity.impl;

import laoqi123.Myau;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.MoveInputEvent;
import laoqi123.event.impl.PacketEvent;
import laoqi123.event.impl.PlayerUpdateEvent;
import laoqi123.event.impl.TickEvent;
import laoqi123.module.modules.combat.KillAura;
import laoqi123.module.modules.player.Scaffold;
import laoqi123.module.modules.combat.risevelocity.RiseVelocityMode;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.FloatValue;
import laoqi123.util.PacketUtil;
import laoqi123.value.properties.IntValue;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

public class GrimReduceVelocity extends RiseVelocityMode {
    public final IntValue reduceTicks = new IntValue("Reduce Ticks", 5, 0, 10);
    public final IntValue teleportDisable = new IntValue("Teleport Disable", 3, 1, 10);
    public final IntValue delayTillGround = new IntValue("Delay Till Ground", 6, 1, 20);
    public final BooleanValue extraHit = new BooleanValue("Extra Hit", true);
    public final BooleanValue distHit = new BooleanValue("Distant Hit", true);
    public final BooleanValue delayPlus = new BooleanValue("Delay Plus", true);
    public final BooleanValue rotations = new BooleanValue("Rotations", true);
    public final BooleanValue jumpReset = new BooleanValue("Jump Reset", true);
    public final FloatValue range = new FloatValue("Range", 3.0F, 1.0F, 6.0F);

    private final List<Packet<ClientPlayPacketListener>> delayedPackets = new ArrayList<>();
    private boolean dj;
    private boolean flushingPackets;
    private boolean pendingJumpReset;
    private int holdTicks;
    private int groundedTicks;

    @Override
    public String getName() {
        return "Grim Reduce";
    }

    @Override
    public void onEnable() {
        this.delayedPackets.clear();
        this.dj = false;
        this.flushingPackets = false;
        this.pendingJumpReset = false;
        this.holdTicks = 0;
        this.groundedTicks = 0;
    }

    @Override
    public void onPacketReceive(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE) {
            return;
        }
        if (this.flushingPackets || this.getTicksSinceTeleport() < this.teleportDisable.getValue() || this.isInWeb()) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (packet instanceof EntityVelocityUpdateS2CPacket velocity) {
            if (velocity.getEntityId() != mc.player.getId()) {
                return;
            }
            this.pendingJumpReset = true;
            this.delayedPackets.add((Packet<ClientPlayPacketListener>) packet);
            this.dj = true;
            event.setCancelled(true);
        } else if (packet instanceof EntityS2CPacket || packet instanceof EntityPositionS2CPacket
                || packet instanceof EntityPositionSyncS2CPacket) {
            if (this.dj) {
                this.delayedPackets.add((Packet<ClientPlayPacketListener>) packet);
                event.setCancelled(true);
            }
        } else if (packet instanceof PlayerPositionLookS2CPacket) {
            if (this.dj) {
                this.delayedPackets.add((Packet<ClientPlayPacketListener>) packet);
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        PlayerEntity player = mc.player;
        if (player == null || mc.world == null) {
            return;
        }
        if (player.isOnGround()) {
            this.groundedTicks++;
        } else {
            this.groundedTicks = 0;
        }
        if (this.dj) {
            this.holdTicks++;
            if (this.holdTicks > 25) {
                this.flush();
            }
        }

        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        boolean scaffoldEnabled = ((Scaffold) Myau.moduleManager.modules.get(Scaffold.class)).isEnabled();

        LivingEntity target;
        if (killAura != null && killAura.isEnabled() && killAura.getTarget() != null) {
            target = killAura.getTarget();
        } else {
            target = this.getClosestTarget(this.range.getValue());
        }
        if (target == null) {
            return;
        }

        if (player.hurtTime < 7 && this.rotations.getValue() && !scaffoldEnabled) {
            this.lookAt(target);
            if (player.hurtTime <= this.reduceTicks.getValue() && player.distanceTo(target) > 3.0) {
                player.setPitch((float) (90.0 - Math.random() * 5.0));
            }
        }

        if (player.age <= 20) {
            return;
        }
        if (player.hurtTime > this.reduceTicks.getValue() + 1) {
            return;
        }
        if (this.getTicksSinceTeleport() <= this.teleportDisable.getValue()) {
            return;
        }
        if (!this.extraHit.getValue()) {
            return;
        }
        if (this.distHit.getValue() && player.distanceTo(target) > 2.5) {
            PacketUtil.sendPacket(PlayerInteractEntityC2SPacket.attack(target, player.isSneaking()));
            player.swingHand(Hand.MAIN_HAND);
        }
        mc.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);
    }

    @Override
    public void onMoveInput(MoveInputEvent event) {
        if (mc.player == null) {
            return;
        }
        if (this.pendingJumpReset && this.jumpReset.getValue() && this.dj) {
            event.setJump(true);
        }
        boolean scaffoldEnabled = ((Scaffold) Myau.moduleManager.modules.get(Scaffold.class)).isEnabled();
        if (mc.player.hurtTime < 7 && this.rotations.getValue() && !scaffoldEnabled) {
            LivingEntity target = this.getClosestTarget(this.range.getValue());
            if (target != null) {
                event.setForward(1.0F);
                event.setStrafe(0.0F);
            }
        }
    }

    @Override
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.player == null) {
            return;
        }
        if (this.dj && this.shouldFlush()) {
            this.flush();
        }
        this.pendingJumpReset = false;
    }

    private boolean shouldFlush() {
        PlayerEntity player = mc.player;
        if (!this.dj) {
            return false;
        }
        if (this.delayPlus.getValue()) {
            return this.groundedTicks >= this.delayTillGround.getValue();
        }
        if (player.isOnGround()) {
            return true;
        }
        LivingEntity target = this.getClosestTarget(this.range.getValue());
        if (target == null) {
            return true;
        }
        return player.distanceTo(target) < 2.7 || this.getTicksSinceAttack() <= 1;
    }

    private void flush() {
        this.flushingPackets = true;
        for (Packet<ClientPlayPacketListener> packet : this.delayedPackets) {
            PacketUtil.receivePacket(packet);
        }
        PacketUtil.drainPendingPackets();
        this.delayedPackets.clear();
        this.flushingPackets = false;
        this.dj = false;
        this.holdTicks = 0;
        this.groundedTicks = 0;
    }

    private void lookAt(LivingEntity target) {
        PlayerEntity player = mc.player;
        double deltaX = target.getX() - player.getX();
        double deltaZ = target.getZ() - player.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0);
        double deltaY = target.getY() + target.getHeight() * 0.5 - player.getEyeY();
        float pitch = (float) Math.toDegrees(-Math.atan2(deltaY, player.distanceTo(target)));
        player.setYaw(yaw);
        player.setPitch(pitch);
    }
}