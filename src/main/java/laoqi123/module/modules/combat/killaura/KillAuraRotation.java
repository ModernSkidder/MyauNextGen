package laoqi123.module.modules.combat.killaura;

import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.property.properties.ModeProperty;
import laoqi123.util.config.ChoiceConfigurable;
import laoqi123.util.config.Configurable;
import laoqi123.util.rotation.AccelerationAngleSmooth;
import laoqi123.util.rotation.AimUtil;
import laoqi123.util.rotation.AngleSmooth;
import laoqi123.util.rotation.FailRotationProcessor;
import laoqi123.util.rotation.InterpolationAngleSmooth;
import laoqi123.util.rotation.LeastDifferencePreference;
import laoqi123.util.rotation.LinearAngleSmooth;
import laoqi123.util.rotation.MinaraiAngleSmooth;
import laoqi123.util.rotation.MovementCorrection;
import laoqi123.util.rotation.Rotation;
import laoqi123.util.rotation.RotationPreference;
import laoqi123.util.rotation.RotationProcessor;
import laoqi123.util.rotation.RotationTarget;
import laoqi123.util.rotation.RotationWithVector;
import laoqi123.util.rotation.ShortStopRotationProcessor;
import laoqi123.util.rotation.SigmoidAngleSmooth;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class KillAuraRotation extends Configurable {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public final ChoiceConfigurable angleSmooth;
    public final ShortStopRotationProcessor shortStop;
    public final FailRotationProcessor fail;
    public final ModeProperty movementCorrection;
    public final FloatProperty resetThreshold;
    public final IntProperty ticksUntilReset;
    public final ModeProperty rotationTiming;

    private Rotation currentRotation;
    private Rotation previousRotation;
    private RotationTarget previousRotationTarget;
    private int inactiveTicks;

    public KillAuraRotation() {
        super("Rotations");

        LinearAngleSmooth linear = new LinearAngleSmooth();
        SigmoidAngleSmooth sigmoid = new SigmoidAngleSmooth();
        InterpolationAngleSmooth interpolation = new InterpolationAngleSmooth(this::getPreviousRotationTarget);
        AccelerationAngleSmooth acceleration = new AccelerationAngleSmooth(this::getPreviousRotation);
        MinaraiAngleSmooth minarai = new MinaraiAngleSmooth(interpolation);
        this.angleSmooth = new ChoiceConfigurable("AngleSmooth", 0,
                linear, sigmoid, interpolation, acceleration, minarai);
        this.addChild(this.angleSmooth);

        this.shortStop = new ShortStopRotationProcessor();
        this.addChild(this.shortStop);
        this.fail = new FailRotationProcessor(this::getPreviousRotation);
        this.addChild(this.fail);

        this.movementCorrection = this.register(new ModeProperty("MovementCorrection", 2,
                new String[]{"Off", "Strict", "Silent", "ChangeLook"}));
        this.resetThreshold = this.register(new FloatProperty("ResetThreshold", 2.0f, 1.0f, 180.0f));
        this.ticksUntilReset = this.register(new IntProperty("TicksUntilReset", 5, 1, 30));
        this.rotationTiming = this.register(new ModeProperty("RotationTiming", 0,
                new String[]{"Normal", "Snap", "OnTick"}));
    }

    public AngleSmooth getActiveAngleSmooth() {
        return (AngleSmooth) this.angleSmooth.getActiveChoice();
    }

    public MovementCorrection getMovementCorrection() {
        return MovementCorrection.values()[this.movementCorrection.getValue()];
    }

    public int getRotationTiming() {
        return this.rotationTiming.getValue();
    }

    public int calculateTicks(Rotation current, Rotation target) {
        return this.getActiveAngleSmooth().calculateTicks(current, target);
    }

    public Rotation getCurrentRotation() {
        return this.currentRotation;
    }

    public Rotation getPreviousRotation() {
        return this.previousRotation;
    }

    public RotationTarget getPreviousRotationTarget() {
        return this.previousRotationTarget;
    }

    public void tick() {
        this.fail.tick();
    }

    public void reset() {
        this.currentRotation = null;
        this.previousRotation = null;
        this.previousRotationTarget = null;
        this.inactiveTicks = 0;
    }

    public RotationTarget buildTarget(Rotation rotation, LivingEntity entity) {
        List<RotationProcessor> processors = new ArrayList<>();
        processors.add(this.getActiveAngleSmooth());
        if (this.fail.running()) {
            processors.add(this.fail);
        }
        if (this.shortStop.running()) {
            processors.add(this.shortStop);
        }
        return new RotationTarget(
                rotation,
                entity,
                processors,
                this.ticksUntilReset.getValue(),
                this.resetThreshold.getValue(),
                false,
                this.getMovementCorrection()
        );
    }

    public RotationWithVector findRotation(LivingEntity entity, double range, boolean throughWalls) {
        Vec3d eyes = mc.player.getEyePos();
        double margin = entity.getTargetingMargin();
        Box box = entity.getBoundingBox().expand(margin, margin, margin);

        Vec3d aimPoint;
        List<Vec3d> projected = AimUtil.projectPointsOnBox(eyes, box, 256);
        if (projected != null) {
            Vec3d best = null;
            double bestDist = Double.MAX_VALUE;
            for (Vec3d p : projected) {
                double dist = eyes.squaredDistanceTo(p);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = p;
                }
            }
            aimPoint = best != null ? best : box.getCenter();
        } else {
            aimPoint = box.getCenter();
        }

        RotationPreference preference = LeastDifferencePreference.leastDifferenceToLastPoint(eyes, aimPoint);
        RotationWithVector rotation = AimUtil.raytraceBox(eyes, box, range, 0.0, preference, true);
        if (rotation == null && throughWalls) {
            rotation = AimUtil.raytraceBox(eyes, box, range, range, preference, true);
        }
        return rotation;
    }

    /**
     * Computes the next rotation to apply.
     *
     * @param targetRotation the aim rotation (null when there is nothing to aim at)
     * @param entity         the aim entity (null when nothing to aim at)
     * @param active         whether we have a valid aim target this tick
     * @return the next rotation, or null when the engine wants the player's rotation back
     */
    public Rotation update(Rotation targetRotation, LivingEntity entity, boolean active) {
        Rotation playerRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        if (active) {
            this.inactiveTicks = 0;
            Rotation base = this.currentRotation != null ? this.currentRotation : playerRotation;
            RotationTarget target = this.buildTarget(targetRotation, entity);
            Rotation next = target.towards(base, false);
            next = next.normalize(base);
            this.previousRotation = this.currentRotation;
            this.currentRotation = next;
            this.previousRotationTarget = target;
            return next;
        }
        if (this.currentRotation == null) {
            return null;
        }
        this.inactiveTicks++;
        RotationTarget target = this.buildTarget(playerRotation, null);
        Rotation next = target.towards(this.currentRotation, true);
        next = next.normalize(this.currentRotation);
        if (this.inactiveTicks >= this.ticksUntilReset.getValue()
                || next.angleTo(playerRotation) <= this.resetThreshold.getValue()) {
            this.currentRotation = null;
            this.previousRotation = null;
            this.previousRotationTarget = null;
            this.inactiveTicks = 0;
            return null;
        }
        this.previousRotation = this.currentRotation;
        this.currentRotation = next;
        this.previousRotationTarget = target;
        return next;
    }
}
