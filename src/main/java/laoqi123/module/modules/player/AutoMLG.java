package laoqi123.module.modules.player;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.TickEvent;
import laoqi123.module.Module;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.util.RandomUtil;
import laoqi123.util.RotationUtil;
import laoqi123.util.rotation.Rotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class AutoMLG extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final FloatProperty triggerDistanceSetting = new FloatProperty("Fall distance", 3.0F, 1.0F, 10.0F);
    private final IntProperty predictTicksSetting = new IntProperty("Predict Ticks", 2, 1, 5);
    private final BooleanProperty solidCheckSetting = new BooleanProperty("Solid check", true);
    private final BooleanProperty recoverySetting = new BooleanProperty("Recorvey", true);
    public Rotation targetRotation = null;
    private float accumulatedFall;
    private double lastY;
    private Integer slotToRestore;
    private boolean waterPlaced;
    private boolean recoveryActive;
    private int recoveryDelay;
    private int recoveryCountdown;
    private Integer waterBucketSlot;
    private BlockPos placedWaterPos;
    private boolean readyToPlace;
    private int postPlaceCooldown;
    private int postActionCooldown;
    private int extraCooldown;
    private boolean lookActive;
    private int lookStage;
    private boolean pendingUse;
    private boolean pendingPlace;
    private int lookHoldTicks;
    private float lookCurrentYaw;
    private float lookCurrentPitch;
    private float lookTargetYaw;
    private float lookTargetPitch;
    private float lookRestoreYaw;
    private float lookRestorePitch;

    public AutoMLG() {
        super("AutoMLG", false);
    }

    @Override
    public void onEnabled() {
        this.resetState();
        this.accumulatedFall = 0.0F;
        this.lastY = mc.player != null ? mc.player.getY() : 0.0;
    }

    @Override
    public void onDisabled() {
        this.resetState();
        this.accumulatedFall = 0.0F;
    }

    private void resetState() {
        this.slotToRestore = null;
        this.waterPlaced = false;
        this.recoveryActive = false;
        this.recoveryDelay = 0;
        this.recoveryCountdown = 0;
        this.waterBucketSlot = null;
        this.placedWaterPos = null;
        this.readyToPlace = false;
        this.postPlaceCooldown = 0;
        this.postActionCooldown = 0;
        this.extraCooldown = 0;
        this.lookActive = false;
        this.lookStage = 0;
        this.pendingUse = false;
        this.pendingPlace = false;
        this.lookHoldTicks = 0;
    }

    public boolean isInCooldown() {
        return this.postPlaceCooldown > 0;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (event.getType() != EventType.PRE) {
            return;
        }
        if (this.lookActive) {
            this.tickLook();
            if (this.lookActive) {
                return;
            }
        }
        if (this.pendingUse) {
            this.doUseItemNow();
            return;
        }
        double deltaY;
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (mc.player.isGliding()) {
            return;
        }
        if (mc.player.isOnGround() || mc.player.getAbilities().flying || mc.player.isTouchingWater() || mc.player.isTouchingWaterOrRain() || mc.player.isInLava()) {
            this.accumulatedFall = 0.0F;
        } else {
            deltaY = mc.player.getY() - this.lastY;
            if (deltaY < 0.0) {
                this.accumulatedFall -= (float) deltaY;
            }
        }
        this.lastY = mc.player.getY();
        if (this.postPlaceCooldown > 0) {
            --this.postPlaceCooldown;
        }
        if (this.postActionCooldown > 0) {
            --this.postActionCooldown;
        }
        if (this.extraCooldown > 0) {
            --this.extraCooldown;
        }
        if (this.slotToRestore != null) {
            mc.player.getInventory().selectedSlot = this.slotToRestore;
            this.slotToRestore = null;
        }
        if (mc.player.isOnGround() || this.accumulatedFall <= 0.0F) {
            this.waterPlaced = false;
            this.readyToPlace = false;
        }
        if (this.recoveryActive) {
            if (this.recoveryDelay > 0) {
                --this.recoveryDelay;
                return;
            }
            if (this.recoveryCountdown-- <= 0) {
                this.recoveryActive = false;
                return;
            }
            if (this.waterBucketSlot == null) {
                this.waterBucketSlot = this.findItemInHotbar(Items.BUCKET);
                if (this.waterBucketSlot == null) {
                    this.recoveryActive = false;
                    return;
                }
            }
            if (mc.player.getInventory().getStack(this.waterBucketSlot).getItem() == Items.WATER_BUCKET) {
                this.recoveryActive = false;
                this.waterBucketSlot = null;
                this.placedWaterPos = null;
                this.postPlaceCooldown = Math.max(this.postPlaceCooldown, 1);
                return;
            }
            if (this.placedWaterPos == null || !this.isWaterSource(this.placedWaterPos)) {
                this.recoveryActive = false;
                this.waterBucketSlot = null;
                this.placedWaterPos = null;
                return;
            }
            Rotation recoveryRotation = this.rotationToBlock(this.placedWaterPos);
            BlockHitResult recoveryHit = this.raycastFluid(recoveryRotation, 4.5);
            if (recoveryHit.getType() == HitResult.Type.MISS || !recoveryHit.getBlockPos().equals(this.placedWaterPos)) {
                this.recoveryActive = false;
                this.waterBucketSlot = null;
                this.placedWaterPos = null;
                return;
            }
            this.setTargetRotation(recoveryRotation);
            this.selectSlot(this.waterBucketSlot);
            this.useItem(recoveryRotation);
            return;
        }
        if (!this.waterPlaced
                && !this.recoveryActive
                && this.placedWaterPos == null
                && this.postPlaceCooldown == 0
                && this.postActionCooldown == 0
                && this.accumulatedFall <= 0.5F
                && this.findItemInHotbar(Items.WATER_BUCKET) < 0) {
            int slot = this.findItemInHotbar(Items.BUCKET);
            if (slot >= 0) {
                BlockPos bucketPos = this.findBucketPos();
                if (bucketPos != null) {
                    Rotation rotation = this.rotationToBlock(bucketPos);
                    BlockHitResult hit = this.raycastFluid(rotation, 4.5);
                    if (hit.getType() != HitResult.Type.MISS && hit.getBlockPos().equals(bucketPos)) {
                        this.setTargetRotation(rotation);
                        this.selectSlot(slot);
                        this.useItem(rotation);
                        this.postActionCooldown = 8;
                        this.postPlaceCooldown = Math.max(this.postPlaceCooldown, 1);
                        return;
                    }
                }
            }
        }
        if (this.waterPlaced && !this.readyToPlace && mc.player.getVelocity().y < 0.0) {
            deltaY = this.distanceToGround(2.5);
            if (deltaY > 0.0 && deltaY <= 1.05) {
                this.readyToPlace = true;
            }
        }
        if (this.waterPlaced || this.pendingPlace) {
            return;
        }
        if (this.accumulatedFall < (float) this.triggerDistanceSetting.getValue()) {
            return;
        }
        int slot = this.findItemInHotbar(Items.WATER_BUCKET);
        if (slot < 0) {
            return;
        }
        int ticksLeft = this.ticksUntilGround();
        if (ticksLeft <= (int) this.predictTicksSetting.getValue() + this.getLookDownTicks()) {
            if ((boolean) this.solidCheckSetting.getValue() && !this.hasSolidBelow(BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ()))) {
                return;
            }
            Rotation rotation = new Rotation(mc.player.getYaw(), 90.0F);
            BlockHitResult hit = this.raycastSolid(rotation, 5.0);
            if (hit.getType() == HitResult.Type.MISS) {
                return;
            }
            this.placeWaterBucket(slot, true);
        }
    }

    private int getLookDownTicks() {
        float pitchDiff = 90.0F - mc.player.getPitch();
        if (pitchDiff < 0.0F) {
            pitchDiff = 0.0F;
        }
        int lookTicks = (int) Math.ceil((double) (pitchDiff / 30.0F));
        return lookTicks + 1;
    }

    private int ticksUntilGround() {
        if (mc.player.getVelocity().y >= 0.0) {
            return 999;
        }
        double distance = this.distanceToGround(30.0);
        if (distance == Double.POSITIVE_INFINITY) {
            return 999;
        }
        double simulatedDrop = 0.0;
        double simulatedVelocity = mc.player.getVelocity().y;
        for (int i = 1; i <= 20; ++i) {
            simulatedDrop += simulatedVelocity;
            simulatedVelocity = (simulatedVelocity - 0.08) * 0.98;
            if (Math.abs(simulatedDrop) >= distance) {
                return i;
            }
        }
        return 999;
    }

    private void useItem(Rotation rotation) {
        if (mc.interactionManager == null || mc.player == null) {
            return;
        }
        if (rotation == null) {
            return;
        }
        this.lookCurrentYaw = mc.player.getYaw();
        this.lookCurrentPitch = mc.player.getPitch();
        this.lookRestoreYaw = this.lookCurrentYaw;
        this.lookRestorePitch = this.lookCurrentPitch;
        this.lookTargetYaw = rotation.getYaw() + RandomUtil.nextFloat(-3.0F, 3.0F);
        this.lookTargetPitch = Math.clamp(rotation.getPitch() + RandomUtil.nextFloat(-2.0F, 2.0F), -90.0F, 90.0F);
        this.lookStage = 1;
        this.lookHoldTicks = RandomUtil.nextInt(0, 2);
        this.pendingUse = true;
        this.lookActive = true;
    }

    private void tickLook() {
        if (mc.player.getYaw() != this.lookCurrentYaw || mc.player.getPitch() != this.lookCurrentPitch) {
            this.lookActive = false;
            this.lookStage = 0;
            this.pendingUse = false;
            return;
        }
        if (this.lookStage == 1) {
            if (this.moveLookTowards(this.lookTargetYaw, this.lookTargetPitch)) {
                if (this.pendingUse) {
                    this.pendingUse = false;
                    this.doUseItemNow();
                }
                this.lookStage = 2;
            }
        } else if (this.lookStage == 2) {
            if (--this.lookHoldTicks <= 0) {
                this.lookStage = 3;
            }
        } else if (this.lookStage == 3) {
            if (this.moveLookTowards(this.lookRestoreYaw, this.lookRestorePitch)) {
                this.lookActive = false;
                this.lookStage = 0;
            }
        }
    }

    private boolean moveLookTowards(float targetYaw, float targetPitch) {
        float step = RandomUtil.nextFloat(30.0F, 42.0F);
        float yawDiff = MathHelper.wrapDegrees(targetYaw - this.lookCurrentYaw);
        float pitchDiff = targetPitch - this.lookCurrentPitch;
        boolean reached = Math.abs(yawDiff) <= step && Math.abs(pitchDiff) <= step;
        if (reached) {
            this.lookCurrentYaw = targetYaw;
            this.lookCurrentPitch = targetPitch;
        } else {
            this.lookCurrentYaw += Math.clamp(yawDiff, -step, step);
            this.lookCurrentPitch += Math.clamp(pitchDiff, -step, step);
        }
        mc.player.setYaw(this.lookCurrentYaw);
        mc.player.setPitch(this.lookCurrentPitch);
        return reached;
    }

    private void doUseItemNow() {
        if (mc.interactionManager != null && mc.player != null) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        if (this.slotToRestore != null) {
            mc.player.getInventory().selectedSlot = this.slotToRestore;
            this.slotToRestore = null;
        }
        if (this.pendingPlace) {
            this.pendingPlace = false;
            this.waterPlaced = true;
            this.recoveryActive = (boolean) this.recoverySetting.getValue();
            this.recoveryDelay = 3;
            this.recoveryCountdown = this.recoveryActive ? 2 : 0;
            this.waterBucketSlot = null;
        }
    }

    private BlockPos findBucketPos() {
        BlockPos playerPos = BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        BlockPos closestPos = null;
        double closestDistSq = Double.POSITIVE_INFINITY;
        for (int dy = -1; dy <= 1; ++dy) {
            for (int dx = -4; dx <= 4; ++dx) {
                for (int dz = -4; dz <= 4; ++dz) {
                    BlockPos candidatePos = playerPos.add(dx, dy, dz);
                    if (!this.isWaterSource(candidatePos)) {
                        continue;
                    }
                    double distSq = mc.player.getPos().squaredDistanceTo(
                            (double) candidatePos.getX() + 0.5, (double) candidatePos.getY() + 0.5, (double) candidatePos.getZ() + 0.5
                    );
                    if (distSq >= closestDistSq) {
                        continue;
                    }
                    Rotation rotation = this.rotationToBlock(candidatePos);
                    BlockHitResult hit = this.raycastFluid(rotation, 4.5);
                    if (hit.getType() == HitResult.Type.MISS || !hit.getBlockPos().equals(candidatePos)) {
                        continue;
                    }
                    closestPos = candidatePos;
                    closestDistSq = distSq;
                }
            }
        }
        return closestPos;
    }

    private void setTargetRotation(Rotation rotation) {
        this.targetRotation = rotation;
    }

    private void selectSlot(int slot) {
        this.slotToRestore = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;
    }

    private void placeWaterBucket(int slot, boolean markPlaced) {
        Rotation rotation = new Rotation(mc.player.getYaw(), 90.0F);
        this.setTargetRotation(rotation);
        this.selectSlot(slot);
        this.pendingPlace = markPlaced;
        this.useItem(rotation);
        this.placedWaterPos = this.getPlacementBlockPos(rotation);
        this.lookStage = 1;
        this.lookActive = true;
    }

    private BlockPos getPlacementBlockPos(Rotation rotation) {
        BlockHitResult hit = this.raycastSolid(rotation, 4.5);
        if (hit.getType() == HitResult.Type.MISS) {
            return null;
        }
        return hit.getBlockPos().offset(hit.getSide());
    }

    private BlockHitResult raycastSolid(Rotation rotation, double range) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d direction = RotationUtil.getVectorForRotation(rotation.getPitch(), rotation.getYaw());
        Vec3d endPos = eyePos.add(direction.multiply(range));
        return mc.world.raycast(new RaycastContext(eyePos, endPos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
    }

    private BlockHitResult raycastFluid(Rotation rotation, double range) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d direction = RotationUtil.getVectorForRotation(rotation.getPitch(), rotation.getYaw());
        Vec3d endPos = eyePos.add(direction.multiply(range));
        return mc.world.raycast(new RaycastContext(eyePos, endPos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.SOURCE_ONLY, mc.player));
    }

    private boolean isWaterSource(BlockPos blockPos) {
        net.minecraft.fluid.FluidState fluidState = mc.world.getFluidState(blockPos);
        return fluidState.getFluid() == Fluids.WATER && fluidState.isStill();
    }

    private boolean hasSolidBelow(BlockPos blockPos) {
        return this.isSolidNonMenu(blockPos.down()) || this.isSolidNonMenu(blockPos.down(2));
    }

    private boolean isSolidNonMenu(BlockPos blockPos) {
        net.minecraft.block.BlockState blockState = mc.world.getBlockState(blockPos);
        boolean hasCollision = !blockState.getCollisionShape(mc.world, blockPos).isEmpty();
        boolean noMenu = blockState.createScreenHandlerFactory(mc.world, blockPos) == null;
        return hasCollision && noMenu;
    }

    private double distanceToGround(double maxDist) {
        Vec3d startPos = new Vec3d(mc.player.getX(), mc.player.getBoundingBox().minY, mc.player.getZ());
        Vec3d endPos = startPos.add(0.0, -maxDist, 0.0);
        BlockHitResult hit = mc.world.raycast(new RaycastContext(startPos, endPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        if (hit.getType() == HitResult.Type.MISS) {
            return Double.POSITIVE_INFINITY;
        }
        return startPos.y - hit.getPos().y;
    }

    private int findItemInHotbar(Item item) {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private Rotation rotationToBlock(BlockPos blockPos) {
        float[] rotations = RotationUtil.getRotationsTo(
                (double) blockPos.getX() + 0.5 - mc.player.getX(),
                (double) blockPos.getY() + 0.5 - mc.player.getY() - (double) mc.player.getStandingEyeHeight(),
                (double) blockPos.getZ() + 0.5 - mc.player.getZ(),
                mc.player.getYaw(), mc.player.getPitch()
        );
        return new Rotation(rotations[0], rotations[1]);
    }
}