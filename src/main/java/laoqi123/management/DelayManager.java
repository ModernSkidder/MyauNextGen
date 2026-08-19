package laoqi123.management;

import laoqi123.enums.DelayModules;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.PacketEvent;
import laoqi123.event.impl.TickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class DelayManager {
    public static MinecraftClient mc = MinecraftClient.getInstance();
    public DelayModules delayModule = DelayModules.NONE;
    public long delay = 0L;
    public Deque<Packet<ClientPlayPacketListener>> delayedPacket = new ConcurrentLinkedDeque<>();

    public boolean shouldDelay(Packet<?> packet) {
        if (this.delayModule == DelayModules.NONE) {
            return false;
        } else if (packet instanceof KeepAliveS2CPacket) {
            return false;
        } else if (!(packet instanceof GameJoinS2CPacket) && !(packet instanceof PlayerRespawnS2CPacket)) {
            if (packet instanceof EntityStatusS2CPacket) {
                EntityStatusS2CPacket s19 = (EntityStatusS2CPacket) packet;
                if (mc.world != null) {
                    Entity entity = s19.getEntity(mc.world);
                    if (entity != null && (!entity.equals(mc.player) || s19.getStatus() != 2)) {
                        return false;
                    }
                }
            }
            this.delayedPacket.offer((Packet<ClientPlayPacketListener>) packet);
            return true;
        } else {
            this.setDelayState(false, this.delayModule);
            return false;
        }
    }

    public boolean setDelayState(boolean state, DelayModules delayModule) {
        if (state) {
            this.delay = 0;
            this.delayModule = delayModule;
        } else {
            this.delayModule = DelayModules.NONE;
            if (MinecraftClient.getInstance().getNetworkHandler() != null && this.delayedPacket.isEmpty()) {
                return true;
            }
            while (true) {
                Packet<ClientPlayPacketListener> packet = this.delayedPacket.poll();
                if (packet == null) {
                    this.delayedPacket.clear();
                    break;
                }
                laoqi123.util.PacketUtil.receivePacket(packet);
            }
        }
        return this.delayModule != DelayModules.NONE;
    }

    public DelayModules getDelayModule() {
        return this.delayModule;
    }

    public void delay(DelayModules modules) {
        this.delayModule = modules;
    }

    public long getDelay() {
        return this.delay;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof HandshakeC2SPacket
                || event.getPacket() instanceof LoginHelloC2SPacket
                || event.getPacket() instanceof QueryRequestC2SPacket
                || event.getPacket() instanceof QueryPingC2SPacket
                || event.getPacket() instanceof LoginKeyC2SPacket) {
            this.setDelayState(false, this.delayModule);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.POST) {
            if (mc.player != null && mc.player.isRemoved()) {
                this.setDelayState(false, this.delayModule);
            }
            if (this.delayModule != DelayModules.NONE) {
                this.delay++;
            }
        }
    }
}
