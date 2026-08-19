package laoqi123.module.modules.player;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.PacketEvent;
import laoqi123.events.UpdateEvent;
import laoqi123.module.Module;
import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.property.properties.ModeProperty;
import laoqi123.util.PacketUtil;
import laoqi123.util.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;

import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.StreamSupport;

public class FakeLag extends Module {

    private static final MinecraftClient mc = MinecraftClient.getInstance();



    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"Normal", "Dynamic"});
    public final IntProperty delay = new IntProperty("delay-ms", 200, 50, 5000, () -> mode.getValue() == 0);
    public final FloatProperty range = new FloatProperty("range", 4.0F, 1.0F, 10.0F, () -> mode.getValue() == 1);
    public final IntProperty minDelay = new IntProperty("min-delay-ms", 100, 50, 3000, () -> mode.getValue() == 1);
    public final IntProperty maxDelay = new IntProperty("max-delay-ms", 400, 100, 5000, () -> mode.getValue() == 1);

    private final ConcurrentLinkedQueue<PacketData> packetQueue = new ConcurrentLinkedQueue<>();
    private boolean isDispatching = false;
    private double nearestEnemyDistance = Double.MAX_VALUE;

    public FakeLag() {
        super("FakeLag", false);
    }

    @Override
    public void onEnabled() {
        packetQueue.clear();
        this.isDispatching = false;
        this.nearestEnemyDistance = Double.MAX_VALUE;
    }

    @Override
    public void onDisabled() {
        this.isDispatching = true;
        while (!packetQueue.isEmpty()) {
            PacketUtil.sendPacket(packetQueue.poll().packet);
        }
        this.isDispatching = false;
        this.nearestEnemyDistance = Double.MAX_VALUE;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.enabled) return;
        if (event.getType() != EventType.SEND) return;
        if (this.isDispatching) return;
        if (mc.player == null || mc.world == null) return;

        Packet<?> packet = event.getPacket();

        if (mode.getValue() == 0) {
            event.setCancelled(true);
            packetQueue.add(new PacketData(packet, System.currentTimeMillis(), this.delay.getValue()));
        } else {
            if (nearestEnemyDistance > this.range.getValue()) {
            } else {
                event.setCancelled(true);
                long randomDelay = minDelay.getValue() + (long)(Math.random() * (maxDelay.getValue() - minDelay.getValue() + 1));
                packetQueue.add(new PacketData(packet, System.currentTimeMillis(), randomDelay));
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.PRE) return;
        if (mc.player == null) return;

        if (mode.getValue() == 1) {
            LivingEntity nearest = findNearestEnemy();
            if (nearest != null) {
                nearestEnemyDistance = RotationUtil.distanceToEntity(nearest);
            } else {
                nearestEnemyDistance = Double.MAX_VALUE;
            }

            if (nearestEnemyDistance > this.range.getValue()) {
                while (!packetQueue.isEmpty()) {
                    this.isDispatching = true;
                    PacketUtil.sendPacket(packetQueue.poll().packet);
                    this.isDispatching = false;
                }
                return;
            }
        }

        long currentTime = System.currentTimeMillis();
        while (!packetQueue.isEmpty()) {
            PacketData data = packetQueue.peek();
            if (currentTime - data.timestamp >= data.delayMs) {
                packetQueue.poll();
                this.isDispatching = true;
                PacketUtil.sendPacket(data.packet);
                this.isDispatching = false;
            } else {
                break;
            }
        }
    }

    private LivingEntity findNearestEnemy() {
        if (mc.world == null) return null;
        return StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .filter(e -> e != mc.player && e.isAlive() && !(e instanceof PlayerEntity && ((PlayerEntity) e).isSleeping()))
                .min(Comparator.comparingDouble(RotationUtil::distanceToEntity))
                .orElse(null);
    }

    private static class PacketData {
        private final Packet<?> packet;
        private final long timestamp;
        private final long delayMs;

        public PacketData(Packet<?> packet, long timestamp, long delayMs) {
            this.packet = packet;
            this.timestamp = timestamp;
            this.delayMs = delayMs;
        }
    }
}
