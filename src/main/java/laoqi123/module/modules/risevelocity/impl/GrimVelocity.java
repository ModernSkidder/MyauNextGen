package laoqi123.module.modules.risevelocity.impl;

import laoqi123.event.types.EventType;
import laoqi123.events.MoveInputEvent;
import laoqi123.events.PacketEvent;
import laoqi123.events.TickEvent;
import laoqi123.module.modules.risevelocity.RiseVelocityMode;
import laoqi123.util.PacketUtil;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class GrimVelocity extends RiseVelocityMode {

    private final List<Packet<ClientPlayPacketListener>> heldPackets = new ArrayList<>();
    private boolean movementFrozen;
    private Vec3d motionSaved;
    private boolean dj;
    private boolean flushingPackets;
    private int holdTicks;
    private int reachedTicks;

    @Override
    public String getName() {
        return "Grim";
    }

    @Override
    public void onEnable() {
        this.heldPackets.clear();
        this.movementFrozen = false;
        this.motionSaved = null;
        this.dj = false;
        this.flushingPackets = false;
        this.holdTicks = 0;
        this.reachedTicks = 0;
    }

    @Override
    public void onPacketReceive(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE) {
            return;
        }
        if (this.flushingPackets || this.getTicksSinceTeleport() < 3 || this.isInWeb()) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (packet instanceof EntityVelocityUpdateS2CPacket velocity) {
            if (velocity.getEntityId() != mc.player.getId()) {
                return;
            }
            event.setCancelled(true);
            if (!mc.player.isOnGround()) {
                this.heldPackets.add((Packet<ClientPlayPacketListener>) packet);
                this.dj = true;
                if (this.holdTicks > 25) {
                    this.flush();
                }
            } else {
                this.movementFrozen = true;
                if (this.reachedTicks >= 3) {
                    this.flush();
                }
            }
        } else if (packet instanceof EntityS2CPacket || packet instanceof EntityPositionS2CPacket
                || packet instanceof EntityPositionSyncS2CPacket) {
            if (this.dj) {
                this.heldPackets.add((Packet<ClientPlayPacketListener>) packet);
                event.setCancelled(true);
            }
        } else if (packet instanceof PlayerPositionLookS2CPacket) {
            this.reachedTicks++;
            if (this.dj) {
                this.heldPackets.add((Packet<ClientPlayPacketListener>) packet);
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        if (mc.player == null) {
            return;
        }
        if (this.dj) {
            this.holdTicks++;
            if (this.holdTicks > 25) {
                this.flush();
            }
        }
        boolean active = this.getTicksSinceTeleport() >= 7 && !this.isInWeb();
        if (active || !mc.player.isOnGround()) {
            if (this.movementFrozen) {
                mc.player.setVelocity(0.0, 0.0, 0.0);
            }
        } else {
            if (!this.heldPackets.isEmpty() || this.movementFrozen || this.dj) {
                if (!this.heldPackets.isEmpty() || this.movementFrozen) {
                    this.flush();
                } else {
                    this.dj = false;
                }
            }
        }
    }

    @Override
    public void onMoveInput(MoveInputEvent event) {
        if (mc.player == null) {
            return;
        }
        if (this.getTicksSinceTeleport() >= 7 && !this.isInWeb()) {
            if (this.getJumpTicks() > 3 && mc.player.isOnGround() && this.dj) {
                mc.player.jump();
                this.movementFrozen = true;
                this.dj = false;
                this.flush();
            }
            if (this.movementFrozen) {
                if (this.motionSaved == null) {
                    this.motionSaved = mc.player.getVelocity();
                }
                mc.player.setVelocity(0.0, 0.0, 0.0);
            } else if (this.motionSaved != null) {
                mc.player.setVelocity(this.motionSaved);
                this.motionSaved = null;
            }
        } else if (this.motionSaved != null) {
            this.motionSaved = null;
        }
    }

    private void flush() {
        this.flushingPackets = true;
        for (Packet<ClientPlayPacketListener> packet : this.heldPackets) {
            PacketUtil.receivePacket(packet);
        }
        PacketUtil.drainPendingPackets();
        this.heldPackets.clear();
        this.flushingPackets = false;
        this.reachedTicks = 0;
        this.holdTicks = 0;
        this.dj = false;
        this.movementFrozen = false;
        this.motionSaved = null;
    }
}