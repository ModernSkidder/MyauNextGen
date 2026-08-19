package laoqi123.module.modules;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.events.MoveInputEvent;
import laoqi123.events.UpdateEvent;
import laoqi123.management.RotationState;
import laoqi123.module.Module;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.util.ItemUtil;
import laoqi123.util.MoveUtil;
import laoqi123.util.PacketUtil;
import laoqi123.util.TeamUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EggItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SnowballItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;

public class AutoProjectiles extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final FloatProperty range = new FloatProperty("Range", 8.0F, 3.0F, 15.0F);
    public final IntProperty amount = new IntProperty("Amount", 1, 1, 10);
    public final BooleanProperty prediction = new BooleanProperty("Prediction", true);
    public final BooleanProperty teams = new BooleanProperty("Teams", true);
    public final BooleanProperty weaponOnly = new BooleanProperty("WeaponOnly", true);

    private LivingEntity target = null;
    private int lastSlot = -1;
    private long lastThrowTime = 0L;
    private int throwState = 0;
    private int throwsRemaining = 0;
    private boolean hasRotated = false;
    private SmartPredictor smartPredictor = new SmartPredictor();

    public AutoProjectiles() {
        super("AutoProjectiles", false);
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == mc.player || entity.deathTime > 0) {
            return false;
        }
        if (!(entity instanceof OtherClientPlayerEntity)) {
            return false;
        }
        double distance = mc.player.distanceTo(entity);
        if (distance > this.range.getValue()) {
            return false;
        }
        PlayerEntity player = (PlayerEntity) entity;
        if (TeamUtil.isFriend(player)) {
            return false;
        }
        return !this.teams.getValue() || !TeamUtil.isSameTeam(player);
    }

    private LivingEntity getTarget() {
        ArrayList<LivingEntity> targets = new ArrayList<>();
        for (Object obj : mc.world.getEntities()) {
            if (obj instanceof LivingEntity) {
                LivingEntity entity = (LivingEntity) obj;
                if (isValidTarget(entity)) {
                    targets.add(entity);
                }
            }
        }
        if (targets.isEmpty()) {
            return null;
        }
        targets.sort(Comparator.comparingDouble(entity -> mc.player.distanceTo(entity)));

        LivingEntity newTarget = targets.get(0);
        if (this.target != newTarget) {
            this.smartPredictor = new SmartPredictor();
        }

        return newTarget;
    }

    private boolean hasProjectile() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isProjectile(stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean isProjectile(ItemStack stack) {
        if (stack == null) return false;
        Item item = stack.getItem();
        return item instanceof SnowballItem || item instanceof EggItem;
    }

    private int getProjectileSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isProjectile(stack)) {
                return i;
            }
        }
        return -1;
    }

    private Vec3d predictPosition(LivingEntity target) {
        long currentTime = System.currentTimeMillis();
        smartPredictor.addPosition(new Vec3d(target.getX(), target.getY(), target.getZ()), currentTime);

        if (!this.prediction.getValue()) {
            return new Vec3d(target.getX(), target.getY() + target.getStandingEyeHeight(), target.getZ());
        }

        double rawPing = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
        double networkDelay = rawPing / 1000.0;

        double clientProcessingDelay = 0.02;
        double serverProcessingDelay = 0.01;
        double packetDelay = networkDelay * 0.5;

        double distance = mc.player.distanceTo(target);
        final double PROJECTILE_SPEED = 20.0;
        final double GRAVITY = 0.03;

        double horizontalDistance = Math.sqrt(
                Math.pow(target.getX() - mc.player.getX(), 2) +
                        Math.pow(target.getZ() - mc.player.getZ(), 2)
        );
        double verticalDistance = (target.getY() + target.getStandingEyeHeight()) - (mc.player.getY() + mc.player.getStandingEyeHeight());

        double horizontalTime = horizontalDistance / PROJECTILE_SPEED;
        double verticalTime = calculateVerticalFlightTime(verticalDistance, PROJECTILE_SPEED, GRAVITY);
        double actualFlightTime = Math.max(horizontalTime, verticalTime);

        double totalDelayCompensation = networkDelay + clientProcessingDelay + serverProcessingDelay + packetDelay;

        if (rawPing > 100) {
            totalDelayCompensation += (rawPing - 100) / 1000.0 * 0.8;
        }

        double basePredictionTime = actualFlightTime + totalDelayCompensation;

        Vec3d velocity = smartPredictor.getCurrentVelocity();
        double targetSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        if (targetSpeed > 0.2) {
            basePredictionTime += targetSpeed * 0.1;
        }

        double distanceFactor = Math.min(1.2, distance / 10.0);
        double finalPredictionTime = basePredictionTime * distanceFactor;

        Vec3d predictedPos = smartPredictor.predictNextPosition(finalPredictionTime);

        return new Vec3d(predictedPos.x, predictedPos.y + target.getStandingEyeHeight(), predictedPos.z);
    }

    private double calculateVerticalFlightTime(double verticalDistance, double initialSpeed, double gravity) {
        double verticalComponent = initialSpeed * 0.2;

        if (verticalDistance >= 0) {
            double discriminant = verticalComponent * verticalComponent + 2 * gravity * verticalDistance;
            if (discriminant < 0) return 0;
            return (verticalComponent + Math.sqrt(discriminant)) / gravity;
        } else {
            double discriminant = verticalComponent * verticalComponent - 2 * gravity * verticalDistance;
            if (discriminant < 0) return 0;
            return (Math.sqrt(discriminant) - verticalComponent) / gravity;
        }
    }

    private long calculateSmartDelay() {
        if (target == null) return 800L;

        double distance = mc.player.distanceTo(target);

        if (distance <= 3.5) {
            return 0L;
        } else if (distance <= 3.8) {
            return 20L;
        } else if (distance <= 4.0) {
            return 70L;
        } else if (distance <= 4.5) {
            return 100L;
        } else if (distance <= 5.0) {
            return 200L;
        } else if (distance <= 10.0) {
            return 500L;
        } else {
            return 800L;
        }
    }

    private float[] getRotationsToPosition(Vec3d position) {
        double deltaX = position.x - mc.player.getX();
        double deltaY = position.y - mc.player.getY() - mc.player.getStandingEyeHeight();
        double deltaZ = position.z - mc.player.getZ();
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(deltaY, horizontalDistance) * 180.0 / Math.PI);
        return new float[]{yaw, pitch};
    }

    private void switchToProjectile() {
        int projectileSlot = this.getProjectileSlot();
        if (projectileSlot != -1) {
            this.lastSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = projectileSlot;
        }
    }

    private void switchBack() {
        if (this.lastSlot != -1) {
            mc.player.getInventory().selectedSlot = lastSlot;
            this.lastSlot = -1;
        }
    }

    private void throwProjectile() {
        int projectileSlot = this.getProjectileSlot();
        if (projectileSlot != -1) {
            ItemStack projectileStack = mc.player.getInventory().getStack(projectileSlot);
            if (isProjectile(projectileStack)) {
                PacketUtil.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, 0.0F, 0.0F));
            }
        }
    }

    @EventTarget(Priority.HIGH)
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }

        if (weaponOnly.getValue() && !ItemUtil.isHoldingSword()) {
            if (this.throwState != 0 || this.lastSlot != -1) {
                this.switchBack();
            }
            this.target = null;
            this.throwState = 0;
            this.throwsRemaining = 0;
            this.hasRotated = false;
            return;
        }

        if (!this.hasProjectile()) {
            this.target = null;
            this.throwState = 0;
            this.throwsRemaining = 0;
            this.hasRotated = false;
            this.switchBack();
            return;
        }

        if (this.throwState == 0) {
            this.target = this.getTarget();
            if (this.target == null) {
                return;
            }

            KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
            if (killAura != null && killAura.isEnabled()) {
                double distance = mc.player.distanceTo(this.target);
                if (distance <= killAura.attackRange.getValue()) {
                    return;
                }
            }

            if (System.currentTimeMillis() - this.lastThrowTime < this.calculateSmartDelay()) {
                return;
            }

            this.throwsRemaining = this.amount.getValue();
            this.throwState = 1;
            this.hasRotated = false;
        }

        if (this.throwState == 1) {
            this.switchToProjectile();
            this.throwState = 2;
        } else if (this.throwState == 2) {
            if (this.throwsRemaining > 0) {
                Vec3d predictedPos = this.predictPosition(this.target);
                float[] rotations = this.getRotationsToPosition(predictedPos);

                event.setRotation(rotations[0], rotations[1], 2);
                event.setPervRotation(rotations[0], 2);
                this.hasRotated = true;
                this.throwState = 3;
            } else {
                this.throwState = 4;
            }
        } else if (this.throwState == 3) {
            this.throwProjectile();
            this.throwsRemaining--;

            if (this.throwsRemaining > 0) {
                this.throwState = 2;
            } else {
                this.throwState = 4;
            }
        } else if (this.throwState == 4) {
            this.switchBack();
            this.target = null;
            this.throwState = 0;
            this.hasRotated = false;
            this.lastThrowTime = System.currentTimeMillis();
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (weaponOnly.getValue() && !ItemUtil.isHoldingSword()) {
            return;
        }
        if (this.hasRotated && RotationState.isActived() && RotationState.getPriority() == 2.0F && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
        }
    }

    @Override
    public void onEnabled() {
        this.target = null;
        this.lastSlot = -1;
        this.lastThrowTime = 0L;
        this.throwState = 0;
        this.throwsRemaining = 0;
        this.hasRotated = false;
    }

    @Override
    public void onDisabled() {
        this.switchBack();
        this.target = null;
        this.throwState = 0;
        this.throwsRemaining = 0;
        this.hasRotated = false;
    }

    private static class SmartPredictor {
        private final Vec3d[] positions = new Vec3d[20];
        private final long[] timestamps = new long[20];
        private final double[] movementPatterns = new double[4];
        private int index = 0;
        private double strafeFrequency = 0.0;
        private double jumpFrequency = 0.0;
        private long lastDirectionChange = 0L;
        private Vec3d lastDirection = new Vec3d(0, 0, 0);
        private boolean isStrafing = false;
        private boolean isJumping = false;

        public void addPosition(Vec3d pos, long time) {
            positions[index] = pos;
            timestamps[index] = time;

            if (index > 0) {
                analyzeMovementPattern();
            }

            index = (index + 1) % positions.length;
        }

        private void analyzeMovementPattern() {
            if (index < 2) return;

            int currentIdx = index;
            int prevIdx = (index - 1 + positions.length) % positions.length;

            Vec3d currentPos = positions[currentIdx];
            Vec3d prevPos = positions[prevIdx];

            if (currentPos == null || prevPos == null) return;

            Vec3d movement = new Vec3d(
                    currentPos.x - prevPos.x,
                    currentPos.y - prevPos.y,
                    currentPos.z - prevPos.z
            );

            if (Math.abs(movement.x) > 0.01) {
                if (movement.x > 0) movementPatterns[0] += 0.1;
                else movementPatterns[1] += 0.1;
            }

            if (Math.abs(movement.z) > 0.01) {
                if (movement.z > 0) movementPatterns[2] += 0.1;
                else movementPatterns[3] += 0.1;
            }

            for (int i = 0; i < movementPatterns.length; i++) {
                movementPatterns[i] *= 0.95;
            }

            Vec3d currentDirection = normalizeMovement(movement);
            if (lastDirection.length() > 0) {
                double dotProduct = lastDirection.x * currentDirection.x +
                        lastDirection.z * currentDirection.z;
                if (dotProduct < 0.3) {
                    lastDirectionChange = timestamps[currentIdx];
                    strafeFrequency = Math.min(1.0, strafeFrequency + 0.2);
                    isStrafing = true;
                }
            }
            lastDirection = currentDirection;

            if (movement.y > 0.1) {
                jumpFrequency = Math.min(1.0, jumpFrequency + 0.15);
                isJumping = true;
            } else {
                jumpFrequency *= 0.9;
                isJumping = false;
            }

            if (System.currentTimeMillis() - lastDirectionChange > 500) {
                isStrafing = false;
                strafeFrequency *= 0.8;
            }
        }

        private Vec3d normalizeMovement(Vec3d movement) {
            double length = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
            if (length < 0.001) return new Vec3d(0, 0, 0);
            return new Vec3d(movement.x / length, 0, movement.z / length);
        }

        public Vec3d predictNextPosition(double predictionTime) {
            if (index < 3) return positions[(index - 1 + positions.length) % positions.length];

            Vec3d currentPos = positions[(index - 1 + positions.length) % positions.length];
            Vec3d velocity = getCurrentVelocity();
            Vec3d acceleration = getCurrentAcceleration();

            Vec3d basePredict = new Vec3d(
                    currentPos.x + velocity.x * predictionTime + 0.5 * acceleration.x * predictionTime * predictionTime,
                    currentPos.y + velocity.y * predictionTime + 0.5 * acceleration.y * predictionTime * predictionTime,
                    currentPos.z + velocity.z * predictionTime + 0.5 * acceleration.z * predictionTime * predictionTime
            );

            Vec3d behaviorPredict = predictBehaviorChange(currentPos, velocity, predictionTime);

            double baseWeight = Math.max(0.3, 1.0 - strafeFrequency);
            double behaviorWeight = strafeFrequency;

            return new Vec3d(
                    basePredict.x * baseWeight + behaviorPredict.x * behaviorWeight,
                    basePredict.y * baseWeight + behaviorPredict.y * behaviorWeight,
                    basePredict.z * baseWeight + behaviorPredict.z * behaviorWeight
            );
        }

        private Vec3d predictBehaviorChange(Vec3d currentPos, Vec3d velocity, double predictionTime) {
            Vec3d predicted = currentPos;
            double reactionTime = 0.3;
            if (isStrafing && predictionTime > reactionTime) {
                double timeSinceLastChange = (System.currentTimeMillis() - lastDirectionChange) / 1000.0;
                if (timeSinceLastChange > 0.8 && Math.random() < strafeFrequency) {
                    Vec3d oppositeVel = new Vec3d(-velocity.x * 0.8, velocity.y, -velocity.z * 0.8);
                    predicted = new Vec3d(
                            currentPos.x + oppositeVel.x * (predictionTime - reactionTime),
                            currentPos.y + oppositeVel.y * (predictionTime - reactionTime),
                            currentPos.z + oppositeVel.z * (predictionTime - reactionTime)
                    );
                } else {
                    Vec3d continuedVel = new Vec3d(velocity.x * 0.9, velocity.y, velocity.z * 0.9);
                    predicted = new Vec3d(
                            currentPos.x + continuedVel.x * predictionTime,
                            currentPos.y + continuedVel.y * predictionTime,
                            currentPos.z + continuedVel.z * predictionTime
                    );
                }
            } else {
                double totalPattern = movementPatterns[0] + movementPatterns[1] + movementPatterns[2] + movementPatterns[3];
                if (totalPattern > 0) {
                    double xTendency = (movementPatterns[0] - movementPatterns[1]) / totalPattern;
                    double zTendency = (movementPatterns[2] - movementPatterns[3]) / totalPattern;

                    Vec3d tendencyVel = new Vec3d(
                            velocity.x + xTendency * 0.5,
                            velocity.y + (isJumping ? jumpFrequency * 0.3 : 0),
                            velocity.z + zTendency * 0.5
                    );

                    predicted = new Vec3d(
                            currentPos.x + tendencyVel.x * predictionTime,
                            currentPos.y + tendencyVel.y * predictionTime,
                            currentPos.z + tendencyVel.z * predictionTime
                    );
                }
            }

            return predicted;
        }

        private Vec3d getCurrentVelocity() {
            if (index < 2) return new Vec3d(0, 0, 0);

            int currentIdx = (index - 1 + positions.length) % positions.length;
            int prevIdx = (index - 2 + positions.length) % positions.length;

            if (positions[currentIdx] == null || positions[prevIdx] == null) {
                return new Vec3d(0, 0, 0);
            }

            long timeDiff = timestamps[currentIdx] - timestamps[prevIdx];
            if (timeDiff <= 0) return new Vec3d(0, 0, 0);

            double deltaX = positions[currentIdx].x - positions[prevIdx].x;
            double deltaY = positions[currentIdx].y - positions[prevIdx].y;
            double deltaZ = positions[currentIdx].z - positions[prevIdx].z;

            double timeInSeconds = timeDiff / 1000.0;
            return new Vec3d(deltaX / timeInSeconds, deltaY / timeInSeconds, deltaZ / timeInSeconds);
        }

        private Vec3d getCurrentAcceleration() {
            if (index < 3) return new Vec3d(0, 0, 0);

            Vec3d vel1 = getVelocityBetween((index - 1 + positions.length) % positions.length,
                    (index - 2 + positions.length) % positions.length);
            Vec3d vel2 = getVelocityBetween((index - 2 + positions.length) % positions.length,
                    (index - 3 + positions.length) % positions.length);

            int currentIdx = (index - 1 + positions.length) % positions.length;
            int prevIdx = (index - 2 + positions.length) % positions.length;

            long timeDiff = timestamps[currentIdx] - timestamps[prevIdx];
            if (timeDiff <= 0) return new Vec3d(0, 0, 0);

            double timeInSeconds = timeDiff / 1000.0;
            return new Vec3d(
                    (vel1.x - vel2.x) / timeInSeconds,
                    (vel1.y - vel2.y) / timeInSeconds,
                    (vel1.z - vel2.z) / timeInSeconds
            );
        }

        private Vec3d getVelocityBetween(int idx1, int idx2) {
            if (positions[idx1] == null || positions[idx2] == null) {
                return new Vec3d(0, 0, 0);
            }

            long timeDiff = timestamps[idx1] - timestamps[idx2];
            if (timeDiff <= 0) return new Vec3d(0, 0, 0);

            double deltaX = positions[idx1].x - positions[idx2].x;
            double deltaY = positions[idx1].y - positions[idx2].y;
            double deltaZ = positions[idx1].z - positions[idx2].z;

            double timeInSeconds = timeDiff / 1000.0;
            return new Vec3d(deltaX / timeInSeconds, deltaY / timeInSeconds, deltaZ / timeInSeconds);
        }
    }
}
