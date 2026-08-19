package laoqi123.util.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class FallingPlayer {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private double x;
    private double y;
    private double z;
    private Vec3d motion;
    private Vec3d eyePos;
    private float yaw;
    private float strafe;
    private float forward;
    private float jumpMovementFactor;
    private float movementSpeed;
    private boolean onGround;

    public FallingPlayer(PlayerEntity player) {
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        this.motion = player.getVelocity();
        this.yaw = mc.player.getYaw();
        this.strafe = mc.player.input.movementSideways;
        this.forward = mc.player.input.movementForward;
        this.movementSpeed = player.getMovementSpeed();
        this.onGround = player.isOnGround();
        this.jumpMovementFactor = player.isSprinting() ? 0.026f : 0.02f;
        this.eyePos = mc.player.getEyePos();
    }

    private void calculateForTick() {
        float dragX = 0.91f;
        float dragZ = dragX;
        float dragY = 0.98f;
        float acceleration = this.jumpMovementFactor;

        updateVelocity(acceleration, new Vec3d(this.strafe, 0, this.forward));
        this.x += motion.x;
        this.y += motion.y;
        this.z += motion.z;
        updateGroundState();
        double gravity = 0.08D;
        this.motion = this.motion.add(0, -gravity, 0);
        this.eyePos = new Vec3d(this.x, this.y + mc.player.getStandingEyeHeight(), this.z);
        this.motion = new Vec3d(
                this.motion.x * dragX,
                this.motion.y * dragY,
                this.motion.z * dragZ
        );
    }

    private void updateGroundState() {
        Vec3d center = new Vec3d(x, y, z);
        Vec3d down = center.add(0, -0.2, 0);

        BlockHitResult result = rayTraceHit(center, down);
        if (result != null && result.getType() == HitResult.Type.BLOCK && result.getSide() == Direction.UP) {
            this.onGround = true;
        } else {
            this.onGround = false;
        }
    }

    private void updateVelocity(float speed, Vec3d input) {
        double lengthSquared = input.lengthSquared();
        if (lengthSquared < 1.0E-7D) {
            return;
        }

        Vec3d normalizedInput = (lengthSquared > 1.0D ? input.normalize() : input).multiply(speed);

        float f = MathHelper.sin(this.yaw * ((float) Math.PI / 180F));
        float g = MathHelper.cos(this.yaw * ((float) Math.PI / 180F));
        double inputX = normalizedInput.x * (double) g - normalizedInput.z * (double) f;
        double inputZ = normalizedInput.z * (double) g + normalizedInput.x * (double) f;

        this.motion = this.motion.add(inputX, 0, inputZ);
    }

    public void calculate(int ticks) {
        for (int i = 0; i < ticks; i++) {
            calculateForTick();
        }
    }

    public BlockPos findCollision(int ticks) {
        float w = mc.player != null ? mc.player.getWidth() / 2f : 0.3f;
        for (int i = 0; i < ticks; i++) {
            Vec3d start = new Vec3d(x, y, z);
            calculateForTick();
            Vec3d end = new Vec3d(x, y, z);

            BlockPos raytracedBlock;
            if ((raytracedBlock = rayTrace(start, end)) != null) return raytracedBlock;
            if ((raytracedBlock = rayTrace(start.add(w, 0, w), end.add(w, 0, w))) != null) return raytracedBlock;
            if ((raytracedBlock = rayTrace(start.add(-w, 0, w), end.add(-w, 0, w))) != null) return raytracedBlock;
            if ((raytracedBlock = rayTrace(start.add(w, 0, -w), end.add(w, 0, -w))) != null) return raytracedBlock;
            if ((raytracedBlock = rayTrace(start.add(-w, 0, -w), end.add(-w, 0, -w))) != null) return raytracedBlock;
            if ((raytracedBlock = rayTrace(start.add(w, 0, 0), end.add(w, 0, 0))) != null) return raytracedBlock;
            if ((raytracedBlock = rayTrace(start.add(-w, 0, 0), end.add(-w, 0, 0))) != null) return raytracedBlock;
            if ((raytracedBlock = rayTrace(start.add(0, 0, w), end.add(0, 0, w))) != null) return raytracedBlock;
            if ((raytracedBlock = rayTrace(start.add(0, 0, -w), end.add(0, 0, -w))) != null) return raytracedBlock;
        }
        return null;
    }

    private BlockHitResult rayTraceHit(Vec3d start, Vec3d end) {
        double distance = start.distanceTo(end);
        if (distance < 1.0E-7D) {
            return null;
        }
        HitResult result = mc.world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player
        ));
        return result instanceof BlockHitResult ? (BlockHitResult) result : null;
    }

    private BlockPos rayTrace(Vec3d start, Vec3d end) {
        BlockHitResult result = rayTraceHit(start, end);
        if (result != null && result.getType() == HitResult.Type.BLOCK && result.getSide() == Direction.UP) {
            return result.getBlockPos();
        }
        return null;
    }

    public Vec3d getEyePos() {
        return eyePos.add(0);
    }

    public Vec3d getPos() {
        return new Vec3d(x, y, z);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Vec3d getMotion() {
        return motion;
    }

    public float getYaw() {
        return yaw;
    }

    public float getStrafe() {
        return strafe;
    }

    public float getForward() {
        return forward;
    }

    public float getJumpMovementFactor() {
        return jumpMovementFactor;
    }

    public float getMovementSpeed() {
        return movementSpeed;
    }

    public boolean isOnGround() {
        return onGround;
    }
}