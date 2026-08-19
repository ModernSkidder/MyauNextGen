package laoqi123.module.modules;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.events.LoadWorldEvent;
import laoqi123.events.PacketEvent;
import laoqi123.events.UpdateEvent;
import laoqi123.module.Module;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.util.ItemUtil;
import laoqi123.util.PacketUtil;
import laoqi123.util.RandomUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KnockbackDelay extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final IntProperty airDelay = new IntProperty("AirDelay", 90, 0, 1000);
    private final IntProperty groundDelay = new IntProperty("GroundDelay", 0, 0, 1000);
    private final IntProperty chance = new IntProperty("Chance", 100, 0, 100);
    private final BooleanProperty realtimeDamage = new BooleanProperty("RealtimeDamage", true);
    private final BooleanProperty requireTarget = new BooleanProperty("RequireTarget", false);
    private final BooleanProperty onlySwords = new BooleanProperty("OnlySwords", false);

    private final Queue<TimedPacket> packets = new ConcurrentLinkedQueue<>();
    private boolean blink;

    public KnockbackDelay() {
        super("KnockbackDelay", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{airDelay.getValue() + " - " + groundDelay.getValue()};
    }

    @Override
    public void onDisabled() {
        reset();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.PRE) return;
        if (mc.player == null || mc.world == null) return;
        if (mc.isInSingleplayer() || mc.player.age < 20) return;

        if (mc.currentScreen != null) {
            reset();
            return;
        }

        if (!shouldActivate()) {
            reset();
            return;
        }

        int delay = mc.player.isOnGround() ? groundDelay.getValue() : airDelay.getValue();

        if (!packets.isEmpty()) {
            handle(delay);
        }

        if (mc.player != null && mc.player.hurtTime > 0) {
            blink = true;
        } else if (packets.isEmpty()) {
            blink = false;
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        reset();
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE) return;
        if (mc.player == null || mc.world == null) return;
        if (mc.isInSingleplayer() || mc.player.age < 20 || event.isCancelled()) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof PlayerRespawnS2CPacket) return;
        if (packet instanceof WorldTimeUpdateS2CPacket) return;
        if (packet instanceof HealthUpdateS2CPacket) return;
        if (packet instanceof EntitiesDestroyS2CPacket) return;
        if (packet instanceof ChatMessageS2CPacket) return;
        if (packet instanceof BlockBreakingProgressS2CPacket) return;
        if (packet instanceof ScreenHandlerSlotUpdateS2CPacket) return;
        if (packet instanceof net.minecraft.network.packet.s2c.common.DisconnectS2CPacket) return;

        if (packet instanceof GameStateChangeS2CPacket) {
            GameStateChangeS2CPacket.Reason reason = ((GameStateChangeS2CPacket) packet).getReason();
            if (reason == GameStateChangeS2CPacket.RAIN_STARTED
                    || reason == GameStateChangeS2CPacket.RAIN_STOPPED
                    || reason == GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED
                    || reason == GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED) return;
        }

        if (packet instanceof EntitySpawnS2CPacket) {
            if (((EntitySpawnS2CPacket) packet).getEntityType() == EntityType.LIGHTNING_BOLT) return;
        }

        if (packet instanceof PlaySoundS2CPacket) {
            if ("ambient.weather.thunder".equalsIgnoreCase(((PlaySoundS2CPacket) packet).getSound().value().id().getPath())) return;
        }

        if (realtimeDamage.getValue() && packet instanceof EntityStatusS2CPacket) {
            EntityStatusS2CPacket statusPacket = (EntityStatusS2CPacket) packet;
            net.minecraft.client.world.ClientWorld world = mc.world;
            if (statusPacket.getStatus() == 2 && world != null && statusPacket.getEntity(world) == mc.player) {
                return;
            }
        }

        if (blink) {
            event.setCancelled(true);
            packets.add(new TimedPacket(packet, System.currentTimeMillis()));
        }
    }

    private boolean shouldActivate() {
        if (RandomUtil.nextInt(0, 100) > chance.getValue()) return false;

        if (requireTarget.getValue() && findTarget() == null) return false;

        if (onlySwords.getValue() && !ItemUtil.isHoldingSword()) return false;

        return true;
    }

    private void reset() {
        if (!blink) return;
        blink = false;
        flush();
    }

    private void handle(int delay) {
        while (!packets.isEmpty()) {
            TimedPacket wrapper = packets.peek();
            if (wrapper != null && wrapper.elapsed(delay)) {
                packets.poll();
                PacketUtil.receivePacket(wrapper.packet);
            } else {
                break;
            }
        }
    }

    private void flush() {
        TimedPacket wrapper;
        while ((wrapper = packets.poll()) != null) {
            PacketUtil.receivePacket(wrapper.packet);
        }
    }

    private static class TimedPacket {
        private final Packet<?> packet;
        private final long time;

        public TimedPacket(Packet<?> packet, long time) {
            this.packet = packet;
            this.time = time;
        }

        public boolean elapsed(int delayMs) {
            return System.currentTimeMillis() - time >= delayMs;
        }
    }

    public Entity findTarget() {
        KillAura ka = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (ka != null && ka.isEnabled() && ka.getTarget() != null) {
            return ka.getTarget();
        }

        if (mc.targetedEntity != null) return mc.targetedEntity;

        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            return ((EntityHitResult) mc.crosshairTarget).getEntity();
        }

        return null;
    }
}
