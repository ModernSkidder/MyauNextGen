package laoqi123.module.modules;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.*;
import laoqi123.module.Module;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.util.PacketUtil;
import laoqi123.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BackTrack extends Module {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final IntProperty trackMs = new IntProperty("TrackMS", 200, 1, 1000);
    private final FloatProperty maxDistance = new FloatProperty("MaxTrackRange", 6.0F, 3.1F, 6.0F);
    private final IntProperty maxTick = new IntProperty("MaxTick", 10, 0, 30);
    private final BooleanProperty renderRealPos = new BooleanProperty("RenderRealPos", true);
    private final BooleanProperty smart = new BooleanProperty("Smart", true);
    private final BooleanProperty onlyHighSpeed = new BooleanProperty("Only On Target High Speed", false);
    private final FloatProperty highSpeedThreshold = new FloatProperty("HighSpeed Threshold", 0.2F, 0.01F, 1.0F, onlyHighSpeed::getValue);

    private final Queue<TimedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private final List<Packet<?>> skipPackets = new ArrayList<>();
    private final Deque<Vec3d> positionHistory = new ConcurrentLinkedDeque<>();
    private final Deque<Vec3d> recentPositions = new ConcurrentLinkedDeque<>();

    private Vec3d realTargetPos;
    private Vec3d lastRealTargetPos;
    private PlayerEntity target;
    private int attackTicks;

    public BackTrack() {
        super("BackTrack", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{trackMs.getValue() + "ms"};
    }

    @Override
    public void onEnabled() {
        clearAll();
    }

    @Override
    public void onDisabled() {
        releaseAll();
        clearAll();
    }

    private void clearAll() {
        packetQueue.clear();
        skipPackets.clear();
        positionHistory.clear();
        recentPositions.clear();
        realTargetPos = null;
        lastRealTargetPos = null;
        target = null;
        attackTicks = 0;
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (!isEnabled()) return;

        Entity entity = e.getTarget();
        if (!(entity instanceof PlayerEntity)) return;

        PlayerEntity player = (PlayerEntity) entity;

        if (onlyHighSpeed.getValue()) {
            double dx = player.getX() - player.prevX;
            double dy = player.getY() - player.prevY;
            double dz = player.getZ() - player.prevZ;
            double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (speed < highSpeedThreshold.getValue()) {
                return;
            }
        }

        if (target != null && player.getId() == target.getId()) {
            attackTicks = 0;
            return;
        }

        target = player;
        realTargetPos = player.getPos();
        lastRealTargetPos = realTargetPos;

        positionHistory.clear();
        recentPositions.clear();
        positionHistory.add(realTargetPos);
        recentPositions.add(realTargetPos);

        attackTicks = 0;
    }

    @EventTarget
    public void onTick(TickEvent e) {
        if (!isEnabled() || e.getType() == EventType.POST) return;

        if (target != null) {
            attackTicks++;
        }

        updateTargetLogic();
        processPacketQueue();

        if (packetQueue.isEmpty() && target != null) {
            realTargetPos = target.getPos();
        }
    }

    private void updateTargetLogic() {
        if (target == null || realTargetPos == null) return;

        try {
            Vec3d currentPos = realTargetPos;
            recentPositions.addLast(currentPos);
            if (recentPositions.size() > 5) {
                recentPositions.removeFirst();
            }

            if (recentPositions.size() == 5) {
                Vec3d oldestPos = recentPositions.getFirst();
                if (oldestPos.distanceTo(currentPos) > 5.0) {
                    resetAndRelease();
                    return;
                }
            }

            positionHistory.addLast(currentPos);
            if (positionHistory.size() > 10) {
                positionHistory.removeFirst();
            }

            boolean tooFar = realTargetPos.distanceTo(mc.player.getPos()) > maxDistance.getValue();
            boolean tickExpired = attackTicks > maxTick.getValue();
            if (tickExpired || tooFar) {
                resetAndRelease();
                return;
            }

            if (smart.getValue() && !positionHistory.isEmpty()) {
                Vec3d firstHistory = positionHistory.getFirst();
                double distReal = realTargetPos.distanceTo(mc.player.getPos());
                double distHistory = firstHistory.distanceTo(mc.player.getPos());
                if (distReal <= distHistory) {
                    resetAndRelease();
                    return;
                }
            }

            lastRealTargetPos = realTargetPos;
        } catch (Exception ex) {
            resetAndRelease();
        }
    }

    private void processPacketQueue() {
        long maxDelay = trackMs.getValue();

        while (!packetQueue.isEmpty()) {
            TimedPacket timedPacket = packetQueue.peek();
            if (timedPacket == null) break;

            if (timedPacket.elapsed(maxDelay)) {
                packetQueue.poll();
                Packet<?> packet = timedPacket.getPacket();
                skipPackets.add(packet);
                PacketUtil.receivePacket(packet);
            } else {
                break;
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!isEnabled() || target == null || realTargetPos == null || lastRealTargetPos == null)
            return;
        if (!renderRealPos.getValue())
            return;

        float size = target.getTargetingMargin();
        double width = target.getWidth() / 2.0 + size;
        double height = target.getHeight() + size;

        Vec3d smoothed = getSmoothedPosition(event.getPartialTicks());
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        Box aabb = new Box(
                smoothed.x - width, smoothed.y, smoothed.z - width,
                smoothed.x + width, smoothed.y + height, smoothed.z + width
        ).offset(
                -cameraPos.x,
                -cameraPos.y,
                -cameraPos.z
        );

        RenderUtil.drawFilledBox(aabb, 255, 255, 255);
    }

    private Vec3d getSmoothedPosition(float partialTicks) {
        if (positionHistory.isEmpty()) {
            return new Vec3d(
                    lastRealTargetPos.x + (realTargetPos.x - lastRealTargetPos.x) * partialTicks,
                    lastRealTargetPos.y + (realTargetPos.y - lastRealTargetPos.y) * partialTicks,
                    lastRealTargetPos.z + (realTargetPos.z - lastRealTargetPos.z) * partialTicks
            );
        }

        double totalWeight = 0;
        double x = 0, y = 0, z = 0;

        Object[] history = positionHistory.toArray();
        int size = history.length;
        for (int i = 0; i < size; i++) {
            double weight = (i + 1) / (double) size;
            Vec3d pos = (Vec3d) history[i];
            x += pos.x * weight;
            y += pos.y * weight;
            z += pos.z * weight;
            totalWeight += weight;
        }

        double currentWeight = 3;
        x += realTargetPos.x * currentWeight;
        y += realTargetPos.y * currentWeight;
        z += realTargetPos.z * currentWeight;
        totalWeight += currentWeight;

        return new Vec3d(x / totalWeight, y / totalWeight, z / totalWeight);
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (!isEnabled() || e.getType() == EventType.SEND) return;

        Packet<?> packet = e.getPacket();
        if (skipPackets.contains(packet)) {
            skipPackets.remove(packet);
            return;
        }

        if (target == null) return;

        boolean shouldIntercept = false;

        if (packet instanceof EntityPositionS2CPacket) {
            EntityPositionS2CPacket wrapper = (EntityPositionS2CPacket) packet;
            Entity entity = mc.world.getEntityById(wrapper.entityId());
            if (entity != null && entity.getId() == target.getId()) {
                Set<PositionFlag> flags = wrapper.relatives();
                Vec3d change = wrapper.change().position();
                realTargetPos = new Vec3d(
                        flags.contains(PositionFlag.X) ? realTargetPos.x + change.x : change.x,
                        flags.contains(PositionFlag.Y) ? realTargetPos.y + change.y : change.y,
                        flags.contains(PositionFlag.Z) ? realTargetPos.z + change.z : change.z
                );
                shouldIntercept = true;
            }
        } else if (packet instanceof EntityPositionSyncS2CPacket) {
            EntityPositionSyncS2CPacket wrapper = (EntityPositionSyncS2CPacket) packet;
            if (wrapper.id() == target.getId()) {
                realTargetPos = wrapper.values().position();
                shouldIntercept = true;
            }
        } else if (packet instanceof EntitiesDestroyS2CPacket) {
            EntitiesDestroyS2CPacket wrapper = (EntitiesDestroyS2CPacket) packet;
            for (int id : wrapper.getEntityIds()) {
                if (id == target.getId()) {
                    resetAndRelease();
                    return;
                }
            }
        }

        if (shouldIntercept) {
            packetQueue.add(new TimedPacket(packet));
            e.setCancelled(true);
        }
    }

    private void resetAndRelease() {
        target = null;
        realTargetPos = null;
        lastRealTargetPos = null;
        positionHistory.clear();
        recentPositions.clear();
        releaseAll();
    }

    private void releaseAll() {
        while (!packetQueue.isEmpty()) {
            TimedPacket tp = packetQueue.poll();
            if (tp != null) {
                Packet<?> packet = tp.getPacket();
                skipPackets.add(packet);
                PacketUtil.receivePacket(packet);
            }
        }
    }

    private static class TimedPacket {
        private final Packet<?> packet;
        private final long time;

        public TimedPacket(Packet<?> packet) {
            this.packet = packet;
            this.time = System.currentTimeMillis();
        }

        public Packet<?> getPacket() {
            return packet;
        }

        public boolean elapsed(long delayMs) {
            return System.currentTimeMillis() - time >= delayMs;
        }
    }
}
