package laoqi123.module.modules.player;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.event.impl.HitBlockEvent;
import laoqi123.event.impl.LeftClickMouseEvent;
import laoqi123.event.impl.LivingUpdateEvent;
import laoqi123.event.impl.MoveInputEvent;
import laoqi123.event.impl.Render2DEvent;
import laoqi123.event.impl.RightClickMouseEvent;
import laoqi123.event.impl.SafeWalkEvent;
import laoqi123.event.impl.StrafeEvent;
import laoqi123.event.impl.SwapItemEvent;
import laoqi123.event.impl.UpdateEvent;
import laoqi123.management.RotationState;
import laoqi123.module.Module;
import laoqi123.module.modules.misc.BedNuker;
import laoqi123.module.modules.movement.LongJump;
import laoqi123.module.modules.render.HUD;
import laoqi123.value.properties.*;
import laoqi123.value.properties.BooleanValue;
import laoqi123.util.BlockData;
import laoqi123.util.BlockUtil;
import laoqi123.util.ItemUtil;
import laoqi123.util.MoveUtil;
import laoqi123.util.PacketUtil;
import laoqi123.util.PlayerUtil;
import laoqi123.util.RandomUtil;
import laoqi123.util.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.GameMode;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;

public class Scaffold extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final double[] placeOffsets = new double[]{
            0.03125,
            0.09375,
            0.15625,
            0.21875,
            0.28125,
            0.34375,
            0.40625,
            0.46875,
            0.53125,
            0.59375,
            0.65625,
            0.71875,
            0.78125,
            0.84375,
            0.90625,
            0.96875
    };

    private int rotationTick = 0;
    private int lastSlot = -1;
    private int blockCount = -1;
    private float yaw = -180.0F;
    private float pitch = 0.0F;
    private boolean canRotate = false;
    private int towerTick = 0;
    private int towerDelay = 0;
    private int stage = 0;
    private int startY = 256;
    private boolean shouldKeepY = false;
    private boolean towering = false;
    private Direction targetFacing = null;
    private boolean canplace = false;
    private boolean onairplace = false;
    private boolean pendingFlatDelay = false;
    private int currentFlatTick = 0;
    private boolean wasInAir = false;
    private boolean isSnapping = false;
    private int snapCooldown = 0;
    private int lastRotationTick = 0;
    private int rotationZeroCount = 0;
    private boolean hasPlacedThisJump = false;


    public final ModeValue rotationMode = new ModeValue("rotations", 1, new String[]{"NONE", "DEFAULT", "BACKWARDS", "SIDEWAYS", "OFFSET", "SNAP", "HYPIXEL"});
    public final ModeValue moveFix = new ModeValue("move-fix", 2, new String[]{"NONE", "SILENT"});
    public final ModeValue sprintMode = new ModeValue("sprint", 0, new String[]{"NONE", "VANILLA"});
    public final PercentValue groundMotion = new PercentValue("ground-motion", 100);
    public final PercentValue airMotion = new PercentValue("air-motion", 100);
    public final PercentValue speedMotion = new PercentValue("speed-motion", 100);
    public final BooleanValue keepYonPress = new BooleanValue("keep-y-on-press", false, () -> this.keepY.getValue() != 0);
    public final BooleanValue multiplace = new BooleanValue("multi-place", true);
    public final BooleanValue safeWalk = new BooleanValue("safe-walk", true);
    public final BooleanValue swing = new BooleanValue("swing", true);
    public final BooleanValue itemSpoof = new BooleanValue("item-spoof", false);
    public final BooleanValue blockCounter = new BooleanValue("block-counter", true);
    public final BooleanValue candiffplace = new BooleanValue("candiffplace", true);
    public final IntValue towerFlatTicks = new IntValue("delay-ticks", 4, 0, 20);
    public final IntValue AngleDiff = new IntValue("angle-diff", 50, 0, 180, candiffplace::getValue);
    public final FloatValue tellyStartRotMinSpeed = new FloatValue("telly-start-rotation-min-speed", 80.0F, 1.0F, 180.0F, () -> this.keepY.getValue() == 3 || this.keepY.getValue() == 4);
    public final FloatValue tellyStartRotMaxSpeed = new FloatValue("telly-start-rotation-max-speed", 85.0F, 1.0F, 180.0F, () -> this.keepY.getValue() == 3 || this.keepY.getValue() == 4);
    public final FloatValue tellyNormalRotMinSpeed = new FloatValue("telly-normal-rotation-min-speed", 30.0F, 1.0F, 180.0F, () -> this.keepY.getValue() == 3 || this.keepY.getValue() == 4);
    public final FloatValue tellyNormalRotMaxSpeed = new FloatValue("telly-normal-rotation-max-speed", 35.0F, 1.0F, 180.0F, () -> this.keepY.getValue() == 3 || this.keepY.getValue() == 4);
    public final ModeValue rotationSpeed = new ModeValue("rotation-speed", 4, new String[]{"1TICK", "2TICK", "3TICK", "4TICK", "NORMAL"});
    public final ModeValue tower = new ModeValue("tower", 0, new String[]{"NONE", "VANILLA", "EXTRA", "TELLY", "HYPIXEL"});
    public final ModeValue keepY = new ModeValue("keep-y", 0, new String[]{"NONE", "VANILLA", "EXTRA", "TELLY", "EXTRATELLY"});
    public final BooleanValue spoofItem = new BooleanValue("Spoof Item", true);
    public final BooleanValue keepFoV = new BooleanValue("Keep FoV", true);
    public final FloatValue fovValue = new FloatValue("Fov", 1.1F, 1.0F, 2.1F);



    private boolean isSnapDisabled() {
        boolean isKeepYEnabled = this.keepY.getValue() != 0;
        boolean isKeepYActive = isKeepYEnabled && (!this.keepYonPress.getValue() || PlayerUtil.isUsingItem());
        boolean isJumping = PlayerUtil.isJumping();

        return isKeepYActive || isJumping;
    }

    private boolean shouldStopSprint() {
        if (this.isTowering()) {
            return false;
        } else {
            if (this.rotationMode.getValue() == 5 && !this.isSnapDisabled()) {
                return false;
            }
            boolean stage = this.keepY.getValue() == 1 || this.keepY.getValue() == 2 || this.keepY.getValue() == 4;
            return (!stage || this.stage <= 0) && this.sprintMode.getValue() == 0;
        }
    }

    private boolean canPlace() {
        BedNuker bedNuker = (BedNuker) Myau.moduleManager.modules.get(BedNuker.class);
        if (bedNuker.isEnabled() && bedNuker.isReady()) {
            return false;
        } else {
            LongJump longJump = (LongJump) Myau.moduleManager.modules.get(LongJump.class);
            return !longJump.isEnabled() || !longJump.isAutoMode() || longJump.isJumping();
        }
    }

    private Direction getBestFacing(BlockPos blockPos1, BlockPos blockPos3) {
        double offset = 0.0;
        Direction direction = null;
        for (Direction facing : Direction.values()) {
            if (facing != Direction.DOWN) {
                BlockPos pos = blockPos1.offset(facing);
                if (pos.getY() <= blockPos3.getY()) {
                    double distance = pos.getSquaredDistance((double) blockPos3.getX() + 0.5, (double) blockPos3.getY() + 0.5, (double) blockPos3.getZ() + 0.5);
                    if (direction == null || distance < offset || distance == offset && facing == Direction.UP) {
                        offset = distance;
                        direction = facing;
                    }
                }
            }
        }
        return direction;
    }

    private BlockData getBlockData() {
        int startY = MathHelper.floor(mc.player.getY());

        int targetX, targetZ;
        boolean useSnap = this.rotationMode.getValue() == 5 && !this.isSnapDisabled();

        if (useSnap) {
            targetX = MathHelper.floor(mc.player.getX() + mc.player.getVelocity().x * 1);
            targetZ = MathHelper.floor(mc.player.getZ() + mc.player.getVelocity().z * 1);
        } else {
            targetX = MathHelper.floor(mc.player.getX());
            targetZ = MathHelper.floor(mc.player.getZ());
        }

        BlockPos targetPos = new BlockPos(
                targetX,
                (this.stage != 0 && !this.shouldKeepY ? Math.min(startY, this.startY) : startY) - 1,
                targetZ
        );
        if (!BlockUtil.isReplaceable(targetPos)) {
            return null;
        } else {
            double reach = mc.player.getBlockInteractionRange();
            ArrayList<BlockPos> positions = new ArrayList<>();
            for (int x = -4; x <= 4; x++) {
                for (int y = -4; y <= 0; y++) {
                    for (int z = -4; z <= 4; z++) {
                        BlockPos pos = targetPos.add(x, y, z);
                        if (!BlockUtil.isReplaceable(pos)
                                && !BlockUtil.isInteractable(pos)
                                && !(
                                mc.player.squaredDistanceTo((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5)
                                        > reach * reach
                        )
                                && (this.stage == 0 || this.shouldKeepY || pos.getY() < this.startY)) {
                            for (Direction facing : Direction.values()) {
                                if (facing != Direction.DOWN) {
                                    BlockPos blockPos = pos.offset(facing);
                                    if (BlockUtil.isReplaceable(blockPos)) {
                                        positions.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (positions.isEmpty()) {
                return null;
            } else {
                positions.sort(
                        Comparator.comparingDouble(
                                o -> o.getSquaredDistance((double) targetPos.getX() + 0.5, (double) targetPos.getY() + 0.5, (double) targetPos.getZ() + 0.5)
                        )
                );
                BlockPos blockPos = positions.get(0);
                Direction facing = this.getBestFacing(blockPos, targetPos);
                return facing == null ? null : new BlockData(blockPos, facing);
            }
        }
    }

    private void place(BlockPos blockPos, Direction direction, Vec3d vec3d) {
        if (mc.interactionManager == null) {
            return;
        }
        if (ItemUtil.isHoldingBlock() && this.blockCount > 0) {
            ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(vec3d, direction, blockPos, false));
            if (result.isAccepted()) {
                if (mc.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
                    this.blockCount--;
                }
                if (this.swing.getValue()) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                } else {
                    PacketUtil.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                }
                if (!mc.player.isOnGround()) {
                    this.hasPlacedThisJump = true;
                }
            }
        }
    }

    private Direction yawToFacing(float yaw) {
        if (yaw < -135.0F || yaw > 135.0F) {
            return Direction.NORTH;
        } else if (yaw < -45.0F) {
            return Direction.EAST;
        } else {
            return yaw < 45.0F ? Direction.SOUTH : Direction.WEST;
        }
    }

    private double distanceToEdge(Direction direction) {
        switch (direction) {
            case NORTH:
                return mc.player.getZ() - Math.floor(mc.player.getZ());
            case EAST:
                return Math.ceil(mc.player.getX()) - mc.player.getX();
            case SOUTH:
                return Math.ceil(mc.player.getZ()) - mc.player.getZ();
            case WEST:
            default:
                return mc.player.getX() - Math.floor(mc.player.getX());
        }
    }

    private float getSpeed() {
        if (!mc.player.isOnGround()) {
            return (float) this.airMotion.getValue() / 100.0F;
        } else {
            return MoveUtil.getSpeedLevel() > 0
                    ? (float) this.speedMotion.getValue() / 100.0F
                    : (float) this.groundMotion.getValue() / 100.0F;
        }
    }

    private double getRandomOffset() {
        return 0.2155 - RandomUtil.nextDouble(1.0E-4, 9.0E-4);
    }

    private float getCurrentYaw() {
        return MoveUtil.adjustYaw(
                mc.player.getYaw(), (float) MoveUtil.getForwardValue(), (float) MoveUtil.getLeftValue()
        );
    }

    private boolean isDiagonal(float yaw) {
        float absYaw = Math.abs(yaw % 90.0F);
        return absYaw > 20.0F && absYaw < 70.0F;
    }

    public boolean isTowering() {
        if (this.currentFlatTick > 0) {
            return false;
        }
        if (mc.player.isOnGround() && MoveUtil.isForwardPressed() && !PlayerUtil.isAirAbove()) {
            boolean keepY = this.keepY.getValue() == 3 || this.keepY.getValue() == 4;
            boolean tower = this.tower.getValue() == 3 || this.tower.getValue() == 4;
            return keepY && this.stage > 0 || tower && PlayerUtil.isJumping();
        } else {
            return false;
        }
    }

    private boolean isNearEdge(double lookAhead) {
        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        float moveYaw = MoveUtil.getMoveYaw();
        double dirX = -Math.sin(moveYaw * Math.PI / 180.0);
        double dirZ =  Math.cos(moveYaw * Math.PI / 180.0);
        int floorY = MathHelper.floor(py - 1.0);

        for (double dist = 0.2; dist <= lookAhead; dist += 0.3) {
            int checkX = MathHelper.floor(px + dirX * dist);
            int checkZ = MathHelper.floor(pz + dirZ * dist);
            BlockPos below = new BlockPos(checkX, floorY, checkZ);
            Block block = mc.world.getBlockState(below).getBlock();
            if (!BlockUtil.isSolid(block) && !BlockUtil.isInteractable(block)) {
                return true;
            }
        }
        return false;
    }

    public Scaffold() {
        super("Scaffold", false);
    }

    public int getSlot() {
        return this.lastSlot;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.rotationMode.getModeString()};
    }

    @EventTarget(Priority.HIGH)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.player.isOnGround()) {
                if (this.wasInAir && this.hasPlacedThisJump) {
                    this.rotationZeroCount++;

                    if (this.rotationZeroCount >= 2) {
                        this.currentFlatTick = this.towerFlatTicks.getValue();
                        this.rotationZeroCount = 0;
                    }
                }
                this.hasPlacedThisJump = false;
                this.wasInAir = false;
            } else {
                this.wasInAir = true;
            }

            if (this.currentFlatTick > 0) {
                this.currentFlatTick--;
            }
            if ((!this.onairplace) && mc.player.isOnGround()){
                this.onairplace = true;
            }
            if (this.rotationTick > 0) {
                this.rotationTick--;
            }
            if (this.snapCooldown > 0) {
                this.snapCooldown--;
            }
            if (mc.player.isOnGround()) {
                if (this.stage > 0) {
                    this.stage--;
                }
                if (this.stage < 0) {
                    this.stage++;
                }
                if (this.stage == 0
                        && this.keepY.getValue() != 0
                        && (!this.keepYonPress.getValue() || PlayerUtil.isUsingItem())
                        && !PlayerUtil.isJumping()) {
                    this.stage = 1;
                }
                this.startY = this.shouldKeepY ? this.startY : MathHelper.floor(mc.player.getY());
                this.shouldKeepY = false;
                this.towering = false;
            }
            if (this.canPlace()) {
                ItemStack stack = mc.player.getMainHandStack();
                int count = ItemUtil.isBlock(stack) ? stack.getCount() : 0;
                this.blockCount = Math.min(this.blockCount, count);
                if (this.blockCount <= 0) {
                    int slot = mc.player.getInventory().selectedSlot;
                    if (this.blockCount == 0) {
                        slot--;
                    }
                    for (int i = slot; i > slot - 9; i--) {
                        int hotbarSlot = (i % 9 + 9) % 9;
                        ItemStack candidate = mc.player.getInventory().getStack(hotbarSlot);
                        if (ItemUtil.isBlock(candidate)) {
                            mc.player.getInventory().selectedSlot = hotbarSlot;
                            this.blockCount = candidate.getCount();
                            break;
                        }
                    }
                }

                float currentYaw = this.getCurrentYaw();
                float yawDiffTo180 = RotationUtil.wrapAngleDiff(currentYaw - 180.0F, event.getYaw());
                float diagonalYaw = this.isDiagonal(currentYaw)
                        ? yawDiffTo180
                        : RotationUtil.wrapAngleDiff(currentYaw - 135.0F * ((currentYaw + 180.0F) % 90.0F < 45.0F ? 1.0F : -1.0F), event.getYaw());

                boolean useSnap = this.rotationMode.getValue() == 5 && !this.isSnapDisabled();
                int effectiveMode = useSnap ? 5 : (this.rotationMode.getValue() == 5 ? 3 : this.rotationMode.getValue());

                if (useSnap) {
                    BlockData data = this.getBlockData();
                    if (data != null && this.snapCooldown == 0) {
                        this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                        this.pitch = RotationUtil.quantizeAngle(85.0F);
                        this.canRotate = true;
                        this.isSnapping = true;
                    } else {
                        this.yaw = RotationUtil.quantizeAngle(mc.player.getYaw());
                        this.pitch = RotationUtil.quantizeAngle(mc.player.getPitch());
                        this.canRotate = true;
                        this.isSnapping = false;
                    }
                } else if (!this.canRotate) {
                    switch (effectiveMode) {
                        case 1:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                            } else {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                            }
                            break;
                        case 2:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(yawDiffTo180);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                            } else {
                                this.yaw = RotationUtil.quantizeAngle(yawDiffTo180);
                            }
                            break;
                        case 3:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                            } else {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                            }
                            break;
                        case 4:
                            float roundedYaw = Math.round(currentYaw / 45.0f) * 45.0f;
                            this.yaw = RotationUtil.quantizeAngle(roundedYaw);
                            if (this.pitch == 0.0F || !this.canRotate) {
                                this.pitch = RotationUtil.quantizeAngle(79.3F);
                            }
                            break;
                        case 5:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                            } else {
                                float targetYaw = this.isDiagonal(currentYaw) ? diagonalYaw : yawDiffTo180;
                                float yawDiff = MathHelper.wrapDegrees(targetYaw - this.yaw);
                                float pitchDiff = MathHelper.wrapDegrees(85.0F - this.pitch);
                                float yawTolerance = this.rotationTick >= 2
                                        ? RandomUtil.nextFloat(tellyStartRotMinSpeed.getValue(), tellyStartRotMaxSpeed.getValue())
                                        : RandomUtil.nextFloat(tellyNormalRotMinSpeed.getValue(), tellyNormalRotMaxSpeed.getValue());
                                float pitchTolerance = this.rotationTick >= 2
                                        ? RandomUtil.nextFloat(tellyStartRotMinSpeed.getValue(), tellyStartRotMaxSpeed.getValue())
                                        : RandomUtil.nextFloat(tellyNormalRotMinSpeed.getValue(), tellyNormalRotMaxSpeed.getValue());
                                this.yaw = RotationUtil.quantizeAngle(this.yaw + RotationUtil.clampAngle(yawDiff, yawTolerance));
                                this.pitch = RotationUtil.quantizeAngle(this.pitch + RotationUtil.clampAngle(pitchDiff, pitchTolerance));
                            }
                            break;
                    }
                }

                BlockData blockData = this.getBlockData();
                Vec3d hitVec = null;
                if (blockData != null) {
                    if (useSnap && !this.isSnapping) {
                    } else {
                        double[] x = placeOffsets;
                        double[] y = placeOffsets;
                        double[] z = placeOffsets;
                        switch (blockData.facing()) {
                            case NORTH: z = new double[]{0.0}; break;
                            case EAST:  x = new double[]{1.0}; break;
                            case SOUTH: z = new double[]{1.0}; break;
                            case WEST:  x = new double[]{0.0}; break;
                            case DOWN:  y = new double[]{0.0}; break;
                            case UP:    y = new double[]{1.0}; break;
                        }
                        float bestYaw = -180.0F;
                        float bestPitch = 0.0F;
                        float bestDiff = 0.0F;
                        for (double dx : x) {
                            for (double dy : y) {
                                for (double dz : z) {
                                    double relX = (double) blockData.pos().getX() + dx - mc.player.getX();
                                    double relY = (double) blockData.pos().getY() + dy - mc.player.getY() - (double) mc.player.getEyeHeight(mc.player.getPose());
                                    double relZ = (double) blockData.pos().getZ() + dz - mc.player.getZ();
                                    float baseYaw = RotationUtil.wrapAngleDiff(this.yaw, event.getYaw());
                                    float[] rotations = RotationUtil.getRotationsTo(relX, relY, relZ, baseYaw, this.pitch);
                                    HitResult mop = RotationUtil.rayTrace(rotations[0], rotations[1], mc.player.getBlockInteractionRange(), 1.0F);
                                    if (mop != null
                                            && mop.getType() == HitResult.Type.BLOCK
                                            && ((BlockHitResult) mop).getBlockPos().equals(blockData.pos())
                                            && ((BlockHitResult) mop).getSide() == blockData.facing()) {
                                        float totalDiff = Math.abs(rotations[0] - baseYaw) + Math.abs(rotations[1] - this.pitch);
                                        if (bestYaw == -180.0F && bestPitch == 0.0F || totalDiff < bestDiff) {
                                            bestYaw = rotations[0];
                                            bestPitch = rotations[1];
                                            bestDiff = totalDiff;
                                            hitVec = mop.getPos();
                                        }
                                    }
                                }
                            }
                        }
                        if (bestYaw != -180.0F || bestPitch != 0.0F) {
                            this.yaw = bestYaw;
                            this.pitch = bestPitch;
                            this.canRotate = true;
                        }
                    }
                }

                boolean isTellyTowering = this.isTowering() && this.tower.getValue() == 3;
                if (this.canRotate && MoveUtil.isForwardPressed() && Math.abs(MathHelper.wrapDegrees(yawDiffTo180 - this.yaw)) < 90.0F) {
                    if (!(isTellyTowering && effectiveMode == 3)) {
                        switch (effectiveMode) {
                            case 2:
                                this.yaw = RotationUtil.quantizeAngle(yawDiffTo180);
                                break;
                            case 3:
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                                break;
                        }
                    }
                }

                if (effectiveMode != 0) {
                    boolean keepYActive = (this.keepY.getValue() != 0) && (this.stage > 0 || this.shouldKeepY);
                    float targetYaw = this.yaw;
                    float targetPitch = this.pitch;
                    if (this.towering && !useSnap
                            && (mc.player.getVelocity().y > 0.0 || mc.player.getY() > (double)(this.startY + 1))) {
                        float yawDiff = MathHelper.wrapDegrees(this.yaw - event.getYaw());
                        float tolerance = this.rotationTick >= 2
                                ? RandomUtil.nextFloat(tellyStartRotMinSpeed.getValue(), tellyStartRotMaxSpeed.getValue())
                                : RandomUtil.nextFloat(tellyNormalRotMinSpeed.getValue(), tellyNormalRotMaxSpeed.getValue());
                        if (Math.abs(yawDiff) > tolerance) {
                            float clampedYaw = RotationUtil.clampAngle(yawDiff, tolerance);
                            targetYaw = RotationUtil.quantizeAngle(event.getYaw() + clampedYaw);
                            if (!keepYActive && !useSnap
                                    && (mc.player.getVelocity().y > 0.0 || mc.player.getY() > (double)(this.startY + 1)) && candiffplace.getValue()){
                                this.rotationTick = 0;
                            } else {
                                this.rotationTick = 1;
                            }
                        }
                    }

                    if (useSnap) {
                        float diffYaw = MathHelper.wrapDegrees(targetYaw - event.getYaw());
                        float diffPitch = targetPitch - event.getPitch();
                        float maxSnapDelta = 100.0F;
                        if (Math.abs(diffYaw) > maxSnapDelta) {
                            targetYaw = event.getYaw() + Math.copySign(maxSnapDelta, diffYaw);
                        }
                        if (Math.abs(diffPitch) > maxSnapDelta) {
                            targetPitch = event.getPitch() + Math.copySign(maxSnapDelta, diffPitch);
                        }
                    } else if (this.rotationSpeed.getValue() < 5) {
                        int speedTicks = this.rotationSpeed.getValue() + 1;
                        float diffYaw = MathHelper.wrapDegrees(targetYaw - event.getYaw());
                        float diffPitch = targetPitch - event.getPitch();
                        float maxYawDelta = 360.0F / speedTicks;
                        float maxPitchDelta = 360.0F / speedTicks;
                        if (Math.abs(diffYaw) > maxYawDelta) {
                            targetYaw = event.getYaw() + Math.copySign(maxYawDelta, diffYaw);
                        }
                        if (Math.abs(diffPitch) > maxPitchDelta) {
                            targetPitch = event.getPitch() + Math.copySign(maxPitchDelta, diffPitch);
                        }
                    }

                    if (this.isTowering() && !useSnap) {
                        float optimalYaw = this.yaw;
                        float optimalPitch = this.pitch;
                        float yawDelta = MathHelper.wrapDegrees(mc.player.getYaw() - event.getYaw());
                        targetYaw = RotationUtil.quantizeAngle(event.getYaw() + yawDelta * RandomUtil.nextFloat(0.98F, 0.99F));
                        targetPitch = RotationUtil.quantizeAngle(RandomUtil.nextFloat(25.0F, 70.0F));
                        this.towering = true;
                        if (this.tower.getValue() == 4) {
                            double yaw1 = Math.toRadians(optimalYaw);
                            double pitch1 = Math.toRadians(optimalPitch);
                            double yaw2 = Math.toRadians(targetYaw);
                            double pitch2 = Math.toRadians(targetPitch);
                            double dx1 = -Math.sin(yaw1) * Math.cos(pitch1);
                            double dy1 = -Math.sin(pitch1);
                            double dz1 =  Math.cos(yaw1) * Math.cos(pitch1);
                            double dx2 = -Math.sin(yaw2) * Math.cos(pitch2);
                            double dy2 = -Math.sin(pitch2);
                            double dz2 =  Math.cos(yaw2) * Math.cos(pitch2);
                            double dot = dx1 * dx2 + dy1 * dy2 + dz1 * dz2;
                            dot = Math.max(-1.0, Math.min(1.0, dot));
                            double angleDiffDeg = Math.toDegrees(Math.acos(dot));
                            double maxDiff = (double) this.AngleDiff.getValue();
                            this.canplace = angleDiffDeg < maxDiff;
                            if (angleDiffDeg < maxDiff) {
                                this.rotationTick = 3;
                            }
                        }
                    }
                    event.setRotation(targetYaw, targetPitch, 3);
                    if (this.moveFix.getValue() == 1) {
                        event.setPervRotation(targetYaw, 3);
                    }
                }

                boolean canSnapPlace = true;
                if (useSnap) {
                    boolean isMoving = Math.abs(mc.player.getVelocity().x) > 0.02 || Math.abs(mc.player.getVelocity().z) > 0.02;
                    boolean nearEdge = isNearEdge(1.0);
                    boolean isRunningToEdge = isMoving && (PlayerUtil.isAirBelow() || nearEdge);

                    if (this.isSnapping) {
                        float absoluteYawDiff = Math.abs(MathHelper.wrapDegrees(this.yaw - event.getYaw()));
                        if (absoluteYawDiff > 45.0F && !isRunningToEdge) {
                            canSnapPlace = false;
                        }
                    } else {
                        if (PlayerUtil.isAirBelow()) {
                            this.isSnapping = true;
                            this.snapCooldown = 0;
                            canSnapPlace = true;
                        } else {
                            canSnapPlace = false;
                        }
                    }
                }

                if ((blockData != null && hitVec != null && this.rotationTick <= 0 && canSnapPlace)) {
                    this.place(blockData.pos(), blockData.facing(), hitVec);

                    if (useSnap) {
                        this.snapCooldown = 1;
                        this.isSnapping = false;
                    }

                    if (this.multiplace.getValue() && !useSnap) {
                        for (int i = 0; i < 2; i++) {
                            blockData = this.getBlockData();
                            if (blockData == null) {
                                break;
                            }
                            HitResult mop = RotationUtil.rayTrace(this.yaw, this.pitch, mc.player.getBlockInteractionRange(), 1.0F);
                            if (mop != null
                                    && mop.getType() == HitResult.Type.BLOCK
                                    && ((BlockHitResult) mop).getBlockPos().equals(blockData.pos())
                                    && ((BlockHitResult) mop).getSide() == blockData.facing()) {
                                this.place(blockData.pos(), blockData.facing(), mop.getPos());
                            } else {
                                hitVec = BlockUtil.getClickVec(blockData.pos(), blockData.facing());
                                double dx = hitVec.x - mc.player.getX();
                                double dy = hitVec.y - mc.player.getY() - (double) mc.player.getEyeHeight(mc.player.getPose());
                                double dz = hitVec.z - mc.player.getZ();
                                float[] rotations = RotationUtil.getRotationsTo(dx, dy, dz, event.getYaw(), event.getPitch());
                                if (!(Math.abs(rotations[0] - this.yaw) < 120.0F) || !(Math.abs(rotations[1] - this.pitch) < 60.0F)) {
                                    break;
                                }
                                mop = RotationUtil.rayTrace(rotations[0], rotations[1], mc.player.getBlockInteractionRange(), 1.0F);
                                if (mop == null
                                        || mop.getType() != HitResult.Type.BLOCK
                                        || !((BlockHitResult) mop).getBlockPos().equals(blockData.pos())
                                        || ((BlockHitResult) mop).getSide() != blockData.facing()) {
                                    break;
                                }
                                this.place(blockData.pos(), blockData.facing(), mop.getPos());
                            }
                        }
                    }
                }
                if (this.targetFacing != null && canSnapPlace) {
                    if ((this.rotationTick <= 0) ||(this.rotationTick <= 1 && this.tower.getValue() == 4 && this.canplace && this.onairplace && candiffplace.getValue())) {
                        int playerBlockX = MathHelper.floor(mc.player.getX());
                        int playerBlockY = MathHelper.floor(mc.player.getY());
                        int playerBlockZ = MathHelper.floor(mc.player.getZ());
                        BlockPos belowPlayer = new BlockPos(playerBlockX, playerBlockY - 1, playerBlockZ);
                        hitVec = BlockUtil.getHitVec(belowPlayer, this.targetFacing, this.yaw, this.pitch);
                        this.place(belowPlayer, this.targetFacing, hitVec);
                        if (useSnap) {
                            this.snapCooldown = 3;
                            this.isSnapping = false;
                        }
                    }
                    this.targetFacing = null;
                } else if ((this.keepY.getValue() == 2 || this.keepY.getValue() == 4) && this.stage > 0 && !mc.player.isOnGround() && canSnapPlace) {
                    int nextBlockY = MathHelper.floor(mc.player.getY() + mc.player.getVelocity().y);
                    if (nextBlockY <= this.startY && mc.player.getY() > (double) (this.startY + 1)) {
                        this.shouldKeepY = true;
                        blockData = this.getBlockData();
                        if ((blockData != null && this.rotationTick <= 0)||(blockData != null && this.rotationTick <= 1 && this.tower.getValue() == 4 && this.canplace && this.onairplace && candiffplace.getValue())) {
                            hitVec = BlockUtil.getHitVec(blockData.pos(), blockData.facing(), this.yaw, this.pitch);
                            this.place(blockData.pos(), blockData.facing(), hitVec);
                            if (useSnap) {
                                this.snapCooldown = 3;
                                this.isSnapping = false;
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled()) {
            if (this.currentFlatTick > 0) {
                this.towerTick = 0;
                this.towerDelay = 0;
                return;
            }
            if (!mc.player.horizontalCollision
                    && mc.player.hurtTime <= 5
                    && !mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)
                    && PlayerUtil.isJumping()
                    && ItemUtil.isHoldingBlock()) {
                int yState = (int) (mc.player.getY() % 1.0 * 100.0);
                switch (this.tower.getValue()) {
                    case 1:
                        switch (this.towerTick) {
                            case 0:
                                if (mc.player.isOnGround()) {
                                    this.towerTick = 1;
                                    mc.player.setVelocity(mc.player.getVelocity().x, -0.0784000015258789, mc.player.getVelocity().z);
                                }
                                return;
                            case 1:
                                if (yState == 0 && PlayerUtil.isAirBelow()) {
                                    this.startY = MathHelper.floor(mc.player.getY());
                                    this.towerTick = 2;
                                    mc.player.setVelocity(mc.player.getVelocity().x, 0.42F, mc.player.getVelocity().z);
                                    if (MoveUtil.isForwardPressed()) {
                                        MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                                    } else {
                                        MoveUtil.setSpeed(0.0);
                                        event.setForward(0.0F);
                                        event.setStrafe(0.0F);
                                    }
                                    return;
                                } else {
                                    this.towerTick = 0;
                                    return;
                                }
                            case 2:
                                this.towerTick = 3;
                                mc.player.setVelocity(mc.player.getVelocity().x, 0.75 - mc.player.getY() % 1.0, mc.player.getVelocity().z);
                                return;
                            case 3:
                                this.towerTick = 1;
                                mc.player.setVelocity(mc.player.getVelocity().x, 1.0 - mc.player.getY() % 1.0, mc.player.getVelocity().z);
                                return;
                            default:
                                this.towerTick = 0;
                                return;
                        }
                    case 2:
                        switch (this.towerTick) {
                            case 0:
                                if (mc.player.isOnGround()) {
                                    this.towerTick = 1;
                                    mc.player.setVelocity(mc.player.getVelocity().x, -0.0784000015258789, mc.player.getVelocity().z);
                                }
                                return;
                            case 1:
                                if (yState == 0 && PlayerUtil.isAirBelow()) {
                                    this.startY = MathHelper.floor(mc.player.getY());
                                    if (!MoveUtil.isForwardPressed()) {
                                        this.towerDelay = 2;
                                        MoveUtil.setSpeed(0.0);
                                        event.setForward(0.0F);
                                        event.setStrafe(0.0F);
                                        Direction facing = this.yawToFacing(MathHelper.wrapDegrees(this.yaw - 180.0F));
                                        double distance = this.distanceToEdge(facing);
                                        if (distance > 0.1) {
                                            if (mc.player.isOnGround()) {
                                                Vec3i directionVec = facing.getVector();
                                                double offset = Math.min(this.getRandomOffset(), distance - 0.05);
                                                double jitter = RandomUtil.nextDouble(0.02, 0.03);
                                                Box nextBox = mc.player
                                                        .getBoundingBox()
                                                        .offset((double) directionVec.getX() * (offset - jitter), 0.0, (double) directionVec.getZ() * (offset - jitter));
                                                if (mc.world.isSpaceEmpty(nextBox)) {
                                                    mc.player.setVelocity(mc.player.getVelocity().x, -0.0784000015258789, mc.player.getVelocity().z);
                                                    mc.player
                                                            .setPosition(nextBox.minX + (nextBox.maxX - nextBox.minX) / 2.0, nextBox.minY, nextBox.minZ + (nextBox.maxZ - nextBox.minZ) / 2.0);
                                                }
                                                return;
                                            }
                                        } else {
                                            this.towerTick = 2;
                                            this.targetFacing = facing;
                                            mc.player.setVelocity(mc.player.getVelocity().x, 0.42F, mc.player.getVelocity().z);
                                        }
                                        return;
                                    } else {
                                        this.towerTick = 2;
                                        this.towerDelay++;
                                        mc.player.setVelocity(mc.player.getVelocity().x, 0.42F, mc.player.getVelocity().z);
                                        MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                                        return;
                                    }
                                } else {
                                    this.towerTick = 0;
                                    this.towerDelay = 0;
                                    return;
                                }
                            case 2:
                                this.towerTick = 3;
                                mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y - RandomUtil.nextDouble(0.00101, 0.00109), mc.player.getVelocity().z);
                                return;
                            case 3:
                                if (this.towerDelay >= 4) {
                                    this.towerTick = 4;
                                    this.towerDelay = 0;
                                } else {
                                    this.towerTick = 1;
                                    mc.player.setVelocity(mc.player.getVelocity().x, 1.0 - mc.player.getY() % 1.0, mc.player.getVelocity().z);
                                }
                                return;
                            case 4:
                                this.towerTick = 5;
                                return;
                            case 5:
                                if (!PlayerUtil.isAirBelow()) {
                                    this.towerTick = 0;
                                } else {
                                    this.towerTick = 1;
                                    double motionY = mc.player.getVelocity().y;
                                    motionY -= 0.08;
                                    motionY *= 0.98F;
                                    motionY -= 0.08;
                                    motionY *= 0.98F;
                                    mc.player.setVelocity(mc.player.getVelocity().x, motionY, mc.player.getVelocity().z);
                                }
                                return;
                            default:
                                this.towerTick = 0;
                                this.towerDelay = 0;
                                return;
                        }
                    case 4:
                        if (mc.player.getVelocity().x == 0 && mc.player.getVelocity().z == 0) {
                            if (yState == 0 && PlayerUtil.isAirBelow() && mc.player.isOnGround()) {
                                this.startY = MathHelper.floor(mc.player.getY());
                                mc.player.setVelocity(mc.player.getVelocity().x, 0.42F, mc.player.getVelocity().z);
                            }
                            if (!mc.player.isOnGround() && mc.player.getVelocity().y < 0.0) {
                                mc.player.setVelocity(mc.player.getVelocity().x, -0.3F, mc.player.getVelocity().z);
                            }
                            this.towerTick = 0;
                            this.towerDelay = 0;
                            return;
                        } else {
                            break;
                        }
                    default:
                        this.towerTick = 0;
                        this.towerDelay = 0;
                }
            } else {
                this.towerTick = 0;
                this.towerDelay = 0;
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            if (this.moveFix.getValue() == 1
                    && RotationState.isActived()
                    && RotationState.getPriority() == 3.0F
                    && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }
            if (mc.player.isOnGround() && this.stage > 0 && MoveUtil.isForwardPressed()) {
                PlayerInput pi = mc.player.input.playerInput;
                mc.player.input.playerInput = new PlayerInput(pi.forward(), pi.backward(), pi.left(), pi.right(), true, pi.sneak(), pi.sprint());
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled()) {
            float speed = this.getSpeed();
            if (speed != 1.0F) {
                if (mc.player.input.movementForward != 0.0F && mc.player.input.movementSideways != 0.0F) {
                    mc.player.input.movementForward = mc.player.input.movementForward * (1.0F / (float) Math.sqrt(2.0));
                    mc.player.input.movementSideways = mc.player.input.movementSideways * (1.0F / (float) Math.sqrt(2.0));
                }
                mc.player.input.movementForward *= speed;
                mc.player.input.movementSideways *= speed;
            }
            if (this.shouldStopSprint()) {
                mc.player.setSprinting(false);
            }
        }
    }

    @EventTarget
    public void onSafeWalk(SafeWalkEvent event) {
        if (this.isEnabled() && this.safeWalk.getValue()) {
            if (mc.player.isOnGround() && mc.player.getVelocity().y <= 0.0 && PlayerUtil.canMove(mc.player.getVelocity().x, mc.player.getVelocity().z, -1.0)) {
                event.setSafeWalk(true);
            }
        }
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled()) {
            if (this.blockCounter.getValue()) {
                int count = 0;
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (stack != null && stack.getCount() > 0) {
                        Item item = stack.getItem();
                        if (item instanceof BlockItem) {
                            Block block = ((BlockItem) item).getBlock();
                            if (!BlockUtil.isInteractable(block) && BlockUtil.isSolid(block)) {
                                count += stack.getCount();
                            }
                        }
                    }
                }
                HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
                float scale = hud.scale.getValue();
                String text = String.format("%d block%s left", count, count != 1 ? "s" : "");
                float x = ((float) mc.getWindow().getScaledWidth() / 2.0F + (float) mc.textRenderer.fontHeight * 1.5F) / scale;
                float y = (float) mc.getWindow().getScaledHeight() / 2.0F / scale - (float) mc.textRenderer.fontHeight / 2.0F + 1.0F;
                int color = (count > 0 ? Color.WHITE.getRGB() : new Color(255, 85, 85).getRGB()) | -1090519040;
                event.getContext().drawText(mc.textRenderer, text, (int) x, (int) y, color, hud.shadow.getValue());
            }
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (this.isEnabled()) {
            this.lastSlot = event.setSlot(this.lastSlot);
            event.setCancelled(true);
        }
    }

    @Override
    public void onEnabled() {
        if (mc.player != null) {
            this.lastSlot = mc.player.getInventory().selectedSlot;
        } else {
            this.lastSlot = -1;
        }
        this.blockCount = -1;
        this.rotationTick = 0;
        this.yaw = -180.0F;
        this.pitch = 0.0F;
        this.canRotate = false;
        this.towerTick = 0;
        this.towerDelay = 0;
        this.towering = false;
        this.snapCooldown = 0;
        this.isSnapping = false;
        this.pendingFlatDelay = false;
        this.hasPlacedThisJump = false;
        if (mc.player != null && !mc.player.isOnGround()) {
            this.onairplace = false;
        }
        if (mc.player != null && mc.player.isOnGround()) {
            this.onairplace = true;
        }
        this.lastRotationTick = 0;
        this.rotationZeroCount = 0;
        this.currentFlatTick = 0;

    }

    @Override
    public void onDisabled() {
        if (mc.player != null && this.lastSlot != -1) {
            mc.player.getInventory().selectedSlot = this.lastSlot;
        }
        this.onairplace = false;
    }
}
