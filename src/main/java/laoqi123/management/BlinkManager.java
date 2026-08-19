package laoqi123.management;

import laoqi123.enums.BlinkModules;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.PacketEvent;
import laoqi123.events.TickEvent;
import laoqi123.util.PacketUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class BlinkManager {
    public static MinecraftClient mc = MinecraftClient.getInstance();
    public BlinkModules blinkModule = BlinkModules.NONE;
    public boolean blinking = false;
    public Deque<Packet<?>> blinkedPackets = new ConcurrentLinkedDeque<>();

    public boolean offerPacket(Packet<?> packet) {
        if (this.blinkModule == BlinkModules.NONE || packet instanceof KeepAliveC2SPacket || packet instanceof ChatMessageC2SPacket) {
            return false;
        } else if (this.blinkedPackets.isEmpty() && packet instanceof ClickSlotC2SPacket) {
            return false;
        } else {
            this.blinkedPackets.offer(packet);
            return true;
        }
    }

    public boolean setBlinkState(boolean state, BlinkModules module) {
        if (module == BlinkModules.NONE) {
            return false;
        }
        if (state) {
            this.blinkModule = module;
            this.blinking = true;
        } else {
            if(blinkModule != module){
                return false;
            }
            this.blinking = false;
            if (MinecraftClient.getInstance().getNetworkHandler() != null && this.blinkedPackets.isEmpty()) {
                return true;
            }
            for (Packet<?> blinkedPacket : blinkedPackets) {
                PacketUtil.sendPacketNoEvent(blinkedPacket);
            }
            this.blinkedPackets.clear();
            this.blinkModule = BlinkModules.NONE;
        }
        return true;
    }

    public BlinkModules getBlinkingModule() {
        return this.blinkModule;
    }

    public long countMovement() {
        return this.blinkedPackets.stream().filter(packet -> packet instanceof PlayerMoveC2SPacket).count();
    }

    public boolean isBlinking() {
        return blinking;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof HandshakeC2SPacket
                || event.getPacket() instanceof LoginHelloC2SPacket
                || event.getPacket() instanceof QueryRequestC2SPacket
                || event.getPacket() instanceof QueryPingC2SPacket
                || event.getPacket() instanceof LoginKeyC2SPacket) {
            this.setBlinkState(false, this.blinkModule);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.POST) {
            if (mc.player.isRemoved()) {
                this.setBlinkState(false, this.blinkModule);
            }
        }
    }
}
