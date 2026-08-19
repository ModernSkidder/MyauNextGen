package laoqi123.module.modules.player;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.event.impl.HitBlockEvent;
import laoqi123.event.impl.LeftClickMouseEvent;
import laoqi123.event.impl.MoveInputEvent;
import laoqi123.event.impl.Render2DEvent;
import laoqi123.event.impl.Render3DEvent;
import laoqi123.event.impl.RightClickMouseEvent;
import laoqi123.event.impl.StrafeEvent;
import laoqi123.event.impl.SwapItemEvent;
import laoqi123.event.impl.TickEvent;
import laoqi123.event.impl.UpdateEvent;
import laoqi123.management.RotationState;
import laoqi123.module.Module;
import laoqi123.value.properties.*;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.IntValue;
import laoqi123.util.BlockUtil;
import laoqi123.util.ChatUtil;
import laoqi123.util.MoveUtil;
import laoqi123.util.PacketUtil;
import laoqi123.util.RandomUtil;
import laoqi123.util.RenderUtil;
import laoqi123.util.RotationUtil;
import laoqi123.util.player.FallingPlayer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
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

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Southside (Naven) Scaffold ported from 1.8.9 to 1.21.4.
 * Added alongside the existing SSNG Scaffold as a separate module.
 */
public class Scaffold2 extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // ===== Southside configuration =====
    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Telly", "Snap", "Normal"});
    public final BooleanValue alwaysUpdateRot = new BooleanValue("Always Update Rotation", false);
    public final IntValue placeTick = new IntValue("Place Tick", 1, 1, 5, () -> this.mode.getValue() == 0);
    public final IntValue rotTick = new IntValue("Rotation Tick", 1, 1, 5, () -> this.mode.getValue() == 0);
    public final BooleanValue itemSpoof = new BooleanValue("Spoof Item", true);
    public final BooleanValue noSwing = new BooleanValue("No Swing", false);
    public final BooleanValue eagle = new BooleanValue("Eagle", false, () -> this.mode.getValue() == 0);
    public final BooleanValue snap = new BooleanValue("Snap", false, () -> this.mode.getValue() == 0);
    public final BooleanValue noUptelly = new BooleanValue("No Up Telly", true, () -> this.mode.getValue() == 0);
    public final BooleanValue godBridge = new BooleanValue("God Bridge", false, () -> this.mode.getValue() == 2);
    public final BooleanValue smoothed = new BooleanValue("Smoothed", true, () -> this.mode.getValue() == 0);
    public final BooleanValue safeMode = new BooleanValue("Safe Mode", false, () -> this.mode.getValue() == 0 && this.smoothed.getValue());
    public final BooleanValue testOnGround = new BooleanValue("Test On Ground", false, () -> this.mode.getValue() == 0 && this.smoothed.getValue());
    public final BooleanValue fixRotation = new BooleanValue("Fix Rotation", true);
    public final BooleanValue randomSlow = new BooleanValue("Slow Up Telly", false, () -> this.mode.getValue() == 0);
    public final BooleanValue blockFly = new BooleanValue("Block Fly", false);
    public final BooleanValue abuseRotation = new BooleanValue("Abuse Rotation", true);
    public final ModeValue blockSlotMode = new ModeValue("Block Slot Mode", 0, new String[]{"Farthest", "Most Blocks"});
    public final ModeValue jumpMode = new ModeValue("Jump Mode", 1, new String[]{"Parkour", "Normal", "None"}, () -> this.mode.getValue() == 0);
    public final FloatValue safeDistance = new FloatValue("Clutch Safe Distance", 4.5F, 1.0F, 5.0F);
    public final IntValue tellyEagleTick = new IntValue("Eagle Tick", 1, 1, 5, () -> this.mode.getValue() == 0 && this.eagle.getValue());
    public final IntValue keepEagleSneakTick = new IntValue("Keep Eagle Tick", 1, 1, 5, () -> this.mode.getValue() == 0 && this.eagle.getValue());
    public final BooleanValue dbgV = new BooleanValue("Debug", false);
    public final BooleanValue keepFoV = new BooleanValue("Keep FoV", true);
    public final FloatValue fovValue = new FloatValue("Fov", 1.1F, 1.0F, 2.1F);
    public final BooleanValue mark = new BooleanValue("Mark", true);
    private final BooleanValue duplicateRotPlace = new BooleanValue("Duplicate Rot Place", true);
    private final BooleanValue interactItem = new BooleanValue("Interact Item Before Place", false);
    public final BooleanValue blockCount = new BooleanValue("Block Count", true);
    public final ModeValue blockCountStyle = new ModeValue("Block Count Style", 0, new String[]{"Retro", "Old"});
    public final IntValue blockCountOffset = new IntValue("Block Count Y Offset", 0, 0, 200);

    private static final List<Block> invalidBlocks = Arrays.asList(
            Blocks.ENCHANTING_TABLE, Blocks.CHEST, Blocks.ENDER_CHEST,
            Blocks.TRAPPED_CHEST, Blocks.ANVIL, Blocks.SAND,
            Blocks.COBWEB, Blocks.TORCH, Blocks.CRAFTING_TABLE,
            Blocks.FURNACE, Blocks.DISPENSER, Blocks.STONE_PRESSURE_PLATE,
            Blocks.NOTE_BLOCK, Blocks.DROPPER, Blocks.TNT,
            Blocks.REDSTONE_TORCH, Blocks.DAYLIGHT_DETECTOR
    );

    // ===== Southside state =====
    private SlotData slot;
    private SlotData blockSlot;
    private int oldSlot;
    private int count;
    private int lastCount = 0;
    private int startHotbarCount = 1;
    private boolean canPlace;
    private BlockData blockData;
    private BlockData lastBlockData;
    private int rotateCount = 0;
    private double posY;
    private BlockPos lastPlacePosition = null;
    private int tellyJumpTicks;
    private boolean waitingForEagleSneak;
    private Rotation lastRotation;
    private Rotation rot;
    private int placeCount = 0;
    private int ups = 0;
    private int onGroundTicks = 0;
    private int offGroundTicks = 0;
    private boolean cancelMove = false;

    public Scaffold2() {
        super("Scaffold2", false);
    }

    @Override
    public void onEnabled() {
        placeCount = 0;
        ups = 0;
        if (mc.player == null) {
            return;
        }
        lastRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        this.slot = new SlotData(mc.player.getInventory().selectedSlot, false);
        this.oldSlot = mc.player.getInventory().selectedSlot;
        this.blockSlot = null;
        startHotbarCount = Math.max(1, getBlockCountHotbar());
        blockData = null;
        lastBlockData = null;
        canPlace = true;
        lastPlacePosition = null;
        tellyJumpTicks = 0;
        waitingForEagleSneak = false;
        rot = null;
        onGroundTicks = 0;
        offGroundTicks = 0;
        cancelMove = false;
    }

    @Override
    public void onDisabled() {
        if (mc.player == null) {
            return;
        }
        mc.player.getInventory().selectedSlot = slot != null ? slot.slot() : oldSlot;
        mc.options.sneakKey.setPressed(false);
        cancelMove = false;
    }

    // ===== Southside helpers =====

    private static boolean isValid(Item item) {
        return item instanceof BlockItem
                && !invalidBlocks.contains(((BlockItem) item).getBlock())
                && BlockUtil.isSolid(((BlockItem) item).getBlock())
                && !BlockUtil.isInteractable(((BlockItem) item).getBlock());
    }

    private static boolean isFullBlock(ItemStack stack) {
        return stack != null && stack.getCount() > 0 && isValid(stack.getItem());
    }

    private int getHotbarBlockSlot() {
        if (blockSlotMode.getValue() == 1) {
            return getMostBlocksHotbarSlot();
        }
        int slot = -1;
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isFullBlock(stack)) {
                slot = i;
            }
        }
        return slot;
    }

    private int getMostBlocksHotbarSlot() {
        int selectedSlot = mc.player.getInventory().selectedSlot;
        int bestSlot = -1;
        int bestCount = -1;
        ItemStack selectedStack = mc.player.getInventory().getStack(selectedSlot);
        if (isFullBlock(selectedStack)) {
            bestSlot = selectedSlot;
            bestCount = selectedStack.getCount();
        }
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isFullBlock(stack) && stack.getCount() > bestCount) {
                bestSlot = i;
                bestCount = stack.getCount();
            }
        }
        return bestSlot;
    }

    private int getBlockCountHotbar() {
        if (mc.player == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isFullBlock(stack)) {
                count += stack.getCount();
            }
        }
        ItemStack offhand = mc.player.getOffHandStack();
        if (isFullBlock(offhand)) {
            count += offhand.getCount();
        }
        return count;
    }

    private int getBlockCountColor(int count) {
        if (count < 16) {
            return new Color(255, 80, 80).getRGB();
        }
        if (count < 32) {
            return new Color(255, 220, 80).getRGB();
        }
        return Color.WHITE.getRGB();
    }

    private BlockData getBlockData(BlockPos pos) {
        BlockData data = getPos(pos);
        if (data == null) {
            BlockPos blockPos = getBlockPos();
            if (blockPos == null) {
                return null;
            }
            Direction direction = getPlaceSide(blockPos);
            if (direction == null) {
                return null;
            }
            data = new BlockData(blockPos, direction);
        }
        if (BlockUtil.isReplaceable(data.blockPos().offset(data.facing()))) {
            return data;
        }
        return null;
    }

    private BlockData getPos(BlockPos pos) {
        if (isPosSolid(pos.add(-1, 0, 0))) {
            return new BlockData(pos.add(-1, 0, 0), Direction.EAST);
        } else if (isPosSolid(pos.add(1, 0, 0))) {
            return new BlockData(pos.add(1, 0, 0), Direction.WEST);
        } else if (isPosSolid(pos.add(0, 0, 1))) {
            return new BlockData(pos.add(0, 0, 1), Direction.NORTH);
        } else if (isPosSolid(pos.add(0, 0, -1))) {
            return new BlockData(pos.add(0, 0, -1), Direction.SOUTH);
        } else if (isPosSolid(pos.add(0, -1, 0))) {
            return new BlockData(pos.add(0, -1, 0), Direction.UP);
        }
        return null;
    }

    private BlockPos getBlockPos() {
        BlockPos playerPos = new BlockPos(
                MathHelper.floor(mc.player.getX()),
                MathHelper.floor(mc.player.getY()),
                MathHelper.floor(mc.player.getZ())
        );
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (Map.Entry<BlockPos, Block> block : searchBlocks(5).entrySet()) {
            if (isPosSolid(block.getKey())) {
                positions.add(block.getKey());
            }
        }
        positions.removeIf(pos -> pos.getY() >= playerPos.getY());
        if (positions.isEmpty()) {
            return null;
        }
        positions.sort(Comparator.comparingDouble(vec3 -> vec3.getSquaredDistance(playerPos)));
        return positions.get(0);
    }

    private Direction getPlaceSide(BlockPos blockPos) {
        List<BlockData> blockData = new ArrayList<>();
        BlockPos pos = new BlockPos(
                MathHelper.floor(mc.player.getX()),
                MathHelper.floor(mc.player.getY()),
                MathHelper.floor(mc.player.getZ())
        );
        if (isAirBlock(blockPos.east()) && !blockPos.east().equals(pos)) {
            blockData.add(new BlockData(blockPos.east(), Direction.EAST));
        }
        if (isAirBlock(blockPos.north()) && !blockPos.north().equals(pos)) {
            blockData.add(new BlockData(blockPos.north(), Direction.NORTH));
        }
        if (isAirBlock(blockPos.south()) && !blockPos.south().equals(pos)) {
            blockData.add(new BlockData(blockPos.south(), Direction.SOUTH));
        }
        if (isAirBlock(blockPos.west()) && !blockPos.west().equals(pos)) {
            blockData.add(new BlockData(blockPos.west(), Direction.WEST));
        }
        if (blockData.isEmpty()) {
            return null;
        }
        blockData.sort(Comparator.comparingDouble(vec3 -> vec3.blockPos().getSquaredDistance(pos)));
        blockData.removeIf(bd -> !BlockUtil.isReplaceable(bd.blockPos().offset(bd.facing())));
        return blockData.get(0).facing();
    }

    private boolean isAirBlock(BlockPos blockPos) {
        return BlockUtil.isReplaceable(blockPos);
    }

    private Map<BlockPos, Block> searchBlocks(int radius) {
        Map<BlockPos, Block> blocks = new HashMap<>();
        if (mc.player == null) {
            return blocks;
        }
        for (int x = radius; x >= -radius + 1; x--) {
            for (int y = radius; y >= -radius + 1; y--) {
                for (int z = radius; z >= -radius + 1; z--) {
                    BlockPos blockPos = new BlockPos(
                            mc.player.getBlockPos().getX() + x,
                            mc.player.getBlockPos().getY() + y,
                            mc.player.getBlockPos().getZ() + z
                    );
                    Block block = mc.world.getBlockState(blockPos).getBlock();
                    if (block != null) {
                        blocks.put(blockPos, block);
                    }
                }
            }
        }
        return blocks;
    }

    private boolean isPosSolid(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        if (block instanceof TrapdoorBlock
                || block instanceof DoorBlock
                || block instanceof FenceGateBlock) {
            return false;
        }
        return !BlockUtil.isReplaceable(pos) && BlockUtil.isSolid(block) && !BlockUtil.isInteractable(pos);
    }

    // ===== Rotation =====

    private static float smooth(float angle, float factor) {
        return angle * MathHelper.clamp(factor / 100.0F, 0.0F, 1.0F);
    }

    private Rotation getClosestToBlockFace(BlockData data, float yaw, float pitch) {
        if (data == null) {
            return null;
        }
        Vec3d face = getVec3(data);
        float[] rots = RotationUtil.getRotationsTo(
                face.x - mc.player.getX(),
                face.y - mc.player.getY() - mc.player.getStandingEyeHeight(),
                face.z - mc.player.getZ(),
                yaw,
                pitch
        );
        return new Rotation(rots[0], rots[1]);
    }

    private Vec3d getVec3(BlockData data) {
        BlockPos pos = data.blockPos();
        Direction face = data.facing();
        double x = pos.getX() + 0.5D + face.getOffsetX() * 0.5D;
        double y = pos.getY() + 0.5D + face.getOffsetY() * 0.5D;
        double z = pos.getZ() + 0.5D + face.getOffsetZ() * 0.5D;
        return new Vec3d(x, y, z);
    }

    private static float yawDiffDirectly(float a, float b) {
        return MathHelper.wrapDegrees(a - b);
    }

    private static float normalizeYawDiff(float a, float b) {
        return Math.abs(MathHelper.wrapDegrees(a - b));
    }

    private float getServerYaw() {
        return RotationState.isActived() && RotationState.getPriority() == 3.0F
                ? RotationState.getSmoothedYaw()
                : mc.player.getYaw();
    }

    private float getServerPitch() {
        return mc.player.getPitch();
    }

    private Rotation getBRot(boolean forceRotation) {
        Rotation rotation = blockData != null
                ? getClosestToBlockFace(blockData, getServerYaw(), getServerPitch())
                : null;
        if (rotation == null) {
            if (normalizeYawDiff(mc.player.getYaw() + 100f, getServerYaw()) < normalizeYawDiff(mc.player.getYaw() - 100f, getServerYaw())) {
                rotation = new Rotation(mc.player.getYaw() + 100f, getServerPitch());
            } else {
                rotation = new Rotation(mc.player.getYaw() - 100f, getServerPitch());
            }
        }
        if (cancelMove) {
            return getClosestToBlockFace(blockData, getServerYaw(), getServerPitch());
        }
        double diff = yawDiffDirectly(rotation.yaw, getServerYaw());
        if (mode.getValue() == 0) {
            if (mc.options.jumpKey.isPressed() && noUptelly.getValue()) {
                return rotation;
            }
            if (mc.options.jumpKey.isPressed() && randomSlow.getValue()) {
                ups++;
                if (ups % 2 == 0) {
                    return rotation;
                }
            }
            if (smoothed.getValue() && (offGroundTicks < rotTick.getValue() || safeMode.getValue())) {
                if (onGroundTicks > 0) {
                    if (safeMode.getValue() && (!testOnGround.getValue() || mc.options.jumpKey.isPressed())) {
                        switch (onGroundTicks) {
                            case 1: {
                                if (!forceRotation) {
                                    rotation.yaw = getServerYaw() + smooth((float) diff, 50.0F);
                                    rotation.pitch = 75.5f;
                                } else {
                                    rotation = getClosestToBlockFace(blockData, mc.player.getYaw(), getServerPitch());
                                }
                                break;
                            }
                            case 2: {
                                return new Rotation(mc.player.getYaw(), 75.5f);
                            }
                        }
                    } else {
                        return new Rotation(mc.player.getYaw(), 75.5f);
                    }
                } else {
                    float smoothFactor = offGroundTicks == 1 ? 80f : 50.0f;
                    smoothFactor -= (float) RandomUtil.nextDouble(0.001, 0.005);
                    rotation.yaw = getServerYaw() + smooth((float) diff, smoothFactor);
                }
            } else {
                if (snap.getValue() && mc.options.jumpKey.isPressed()) {
                    if (lastBlockData == null || offGroundTicks < rotTick.getValue()) {
                        return new Rotation(mc.player.getYaw(), 85.0F + (float) Math.random());
                    }
                } else if (offGroundTicks < rotTick.getValue()) {
                    return new Rotation(mc.player.getYaw(), 85.0F + (float) Math.random());
                }
            }
        }
        if (lastRotation != null && blockData != null && didHitBlockFace(mc.player, lastRotation.yaw, lastRotation.pitch, blockData.blockPos(), blockData.facing(), true)) {
            return lastRotation;
        }
        if (blockData != null && !alwaysUpdateRot.getValue() && offGroundTicks >= rotTick.getValue()) {
            if (!didHitBlockFace(mc.player, rotation.yaw, rotation.pitch, blockData.blockPos(), blockData.facing(), true) && offGroundTicks >= rotTick.getValue()) {
                lastRotation.yaw += (float) Math.random();
                return lastRotation;
            }
        }
        lastRotation = rotation;
        return rotation;
    }

    private static boolean didHitBlockFace(Entity player, float yaw, float pitch, BlockPos targetPos, Direction expectedFace, boolean strict) {
        if (player == null || expectedFace == null) {
            return false;
        }
        HitResult result = RotationUtil.rayTrace(yaw, pitch, mc.player.getBlockInteractionRange(), 1.0F);
        if (result == null || result.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        BlockHitResult blockHit = (BlockHitResult) result;
        return blockHit.getBlockPos().equals(targetPos) && (!strict || blockHit.getSide() == expectedFace);
    }

    private static boolean didHitBlockFace(Rotation rotation, BlockPos targetPos, Direction expectedFace, boolean strict) {
        return didHitBlockFace(mc.player, rotation.yaw, rotation.pitch, targetPos, expectedFace, strict);
    }

    private static boolean didHitBlockFace(BlockData blockData, Rotation rot) {
        return blockData == null || !didHitBlockFace(rot, blockData.blockPos(), blockData.facing(), true);
    }

    private boolean doesNotContainBlock(int down) {
        return BlockUtil.isReplaceable(mc.player.getBlockPos().down(down));
    }

    private void place() {
        if (blockData == null) {
            return;
        }
        if (rot == null) {
            return;
        }
        if (mc.interactionManager == null) {
            return;
        }
        if (!canPlace) {
            return;
        }
        if (!didHitBlockFace(mc.player, rot.yaw, rot.pitch, blockData.blockPos(), blockData.facing(), true)) {
            return;
        }
        if (!this.blockSlot.offhand()) {
            mc.player.getInventory().selectedSlot = this.blockSlot.slot();
        }
        if (interactItem.getValue()) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }
        Vec3d hitVec = BlockUtil.getHitVec(blockData.blockPos(), blockData.facing(), rot.yaw, rot.pitch);
        BlockHitResult hitResult = new BlockHitResult(hitVec, blockData.facing(), blockData.blockPos(), false);
        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        if (result != null && result != ActionResult.PASS) {
            placeCount++;
            lastPlacePosition = blockData.blockPos().offset(blockData.facing());
            if (noSwing.getValue()) {
                PacketUtil.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            } else {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private void rotationAbuse(float step, float targetYaw) {
        if (rot == null) {
            return;
        }
        double change = yawDiffDirectly(rot.yaw, targetYaw);
        int times = (int) (Math.abs(change) / step);
        float currentYaw = rot.yaw;
        for (int i = 0; i < times; i++) {
            currentYaw += smooth((float) change, step);
            rot = new Rotation(currentYaw, rot.pitch);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }
        rot = new Rotation(targetYaw, rot.pitch);
    }

    // ===== Events =====

    @EventTarget(Priority.LOWEST)
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.POST) {
            return;
        }
        if (!this.isEnabled()) {
            return;
        }
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (blockFly.getValue()) {
            // BlockFly de-sync release placeholder.
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (mc.player.isOnGround()) {
            onGroundTicks++;
            offGroundTicks = 0;
        } else {
            onGroundTicks = 0;
            offGroundTicks++;
        }

        this.blockSlot = null;

        ItemStack offhand = mc.player.getOffHandStack();
        if (isFullBlock(offhand)) {
            this.blockSlot = new SlotData(-1, true);
        }
        if (this.blockSlot == null && blockSlotMode.getValue() != 1) {
            if (isFullBlock(mc.player.getMainHandStack())) {
                this.blockSlot = new SlotData(mc.player.getInventory().selectedSlot, false);
            }
        }
        if (this.blockSlot == null) {
            int hotbarSlot = getHotbarBlockSlot();
            if (hotbarSlot != -1) {
                this.blockSlot = new SlotData(hotbarSlot, false);
            }
        }
        if (this.blockSlot == null || blockSlot.check()) {
            return;
        }

        if (mc.player.isOnGround()) {
            posY = MathHelper.floor(mc.player.getY() - 1);
        }
        if (mc.options.jumpKey.isPressed()) {
            posY = mc.player.getBlockPos().getY() - 1;
        }

        BlockPos playerBlock = new BlockPos(
                MathHelper.floor(mc.player.getX()),
                MathHelper.floor(mc.player.getY()),
                MathHelper.floor(mc.player.getZ())
        );
        BlockData possible = BlockUtil.isReplaceable(playerBlock)
                ? getBlockData(new BlockPos(playerBlock.getX(), (int) posY, playerBlock.getZ()))
                : null;
        if (possible != null) {
            blockData = possible;
        }
        lastBlockData = possible;

        if (mode.getValue() == 2) {
            canPlace = true;
        } else if (mode.getValue() == 1) {
            canPlace = doesNotContainBlock(1);
        } else {
            canPlace = offGroundTicks >= placeTick.getValue();
            if (safeMode.getValue() && testOnGround.getValue() && !canPlace && mc.options.jumpKey.isPressed()) {
                canPlace = onGroundTicks == 1;
            }
        }

        if (!this.blockSlot.offhand()) {
            mc.player.getInventory().selectedSlot = this.blockSlot.slot();
        }

        FallingPlayer fallingPlayer = new FallingPlayer(mc.player);
        boolean reachable = true;
        fallingPlayer.calculate(1);
        Vec3d nextEyePos = fallingPlayer.getEyePos();
        fallingPlayer.calculate(1);
        BlockData placement = getBlockData(new BlockPos(
                MathHelper.floor(mc.player.getX()),
                mc.player.getBlockPos().getY() - 1,
                MathHelper.floor(mc.player.getZ())
        ));
        boolean forceRotation = false;
        if (placement != null) {
            if (safeMode.getValue() && testOnGround.getValue() && onGroundTicks == 1 && mc.options.jumpKey.isPressed()) {
                forceRotation = true;
            }
            double distance = nextEyePos.distanceTo(new Vec3d(
                    placement.blockPos().getX() + 0.5D,
                    placement.blockPos().getY() + 0.5D,
                    placement.blockPos().getZ() + 0.5D
            ));
            if (distance >= safeDistance.getValue() || placement.blockPos().getY() > fallingPlayer.getY()) {
                canPlace = true;
                reachable = false;
                blockData = lastBlockData = placement;
            }
        }
        if (blockData != null) {
            Box box = new Box(
                    blockData.blockPos().getX(),
                    blockData.blockPos().getY() - 1,
                    blockData.blockPos().getZ(),
                    blockData.blockPos().getX() + 1,
                    blockData.blockPos().getY() + 1,
                    blockData.blockPos().getZ() + 1
            );
            if (blockData.blockPos().getY() > fallingPlayer.getY() && !box.contains(new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()))) {
                canPlace = true;
                reachable = false;
                posY = mc.player.getBlockPos().getY() - 1;
                blockData = lastBlockData = getBlockData(new BlockPos(
                        MathHelper.floor(mc.player.getX()),
                        (int) MathHelper.floor(posY),
                        MathHelper.floor(mc.player.getZ())
                ));
            }
        }

        if (!reachable && rotateCount < 8) {
            if (dbgV.getValue() && rotateCount == 1) {
                ChatUtil.sendFormatted("working");
            }
            cancelMove = true;
            rotateCount++;
        } else {
            rotateCount = 0;
        }

        rot = getBRot(forceRotation);
        if (rot == null) {
            return;
        }
        if (duplicateRotPlace.getValue()) {
            rot.pitch -= (float) RandomUtil.nextDouble(0.001, 0.003);
            rot.yaw -= (float) RandomUtil.nextDouble(0.0001, 0.0003);
            do {
                rot.pitch -= (float) RandomUtil.nextDouble(0.001, 0.003);
            } while (rot.pitch > 90.0F);
            if (rot.pitch < -90.0F) {
                rot.pitch = -90.0F;
            }
        }
        if (didHitBlockFace(blockData, rot)) {
            this.cancelMove = false;
            this.rotateCount = 0;
        }
        if (fixRotation.getValue()) {
            rot = new Rotation(rot.yaw, rot.pitch);
        }
        event.setRotation(rot.yaw, rot.pitch, 3);
        event.setPervRotation(rot.yaw, 3);

        if (abuseRotation.getValue()) {
            rotationAbuse(30f, rot.yaw);
        }
        place();

        if (waitingForEagleSneak) {
            tellyJumpTicks++;
            if (tellyJumpTicks == tellyEagleTick.getValue() && !mc.options.sneakKey.isPressed()) {
                mc.options.sneakKey.setPressed(true);
            }
            if (tellyJumpTicks == tellyEagleTick.getValue() + keepEagleSneakTick.getValue()) {
                mc.options.sneakKey.setPressed(false);
                waitingForEagleSneak = false;
                tellyJumpTicks = 0;
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (!this.isEnabled() || mc.player == null) {
            return;
        }
        if (this.blockSlot == null || blockSlot.check()) {
            return;
        }
        if (onGroundTicks > (smoothed.getValue() && safeMode.getValue() && !testOnGround.getValue() ? 1 : 0)
                && !mc.options.jumpKey.isPressed()
                && MoveUtil.isForwardPressed()
                && mode.getValue() == 0) {
            switch (jumpMode.getValue()) {
                case 0:
                    double yaw = Math.toRadians(mc.player.getYaw());
                    double forwardX = -Math.sin(yaw);
                    double forwardZ = Math.cos(yaw);
                    BlockPos front1 = new BlockPos(
                            (int) (mc.player.getX() + forwardX),
                            (int) (mc.player.getY() - 0.1),
                            (int) (mc.player.getZ() + forwardZ)
                    );
                    BlockPos front2 = new BlockPos(
                            (int) (mc.player.getX() + forwardX * 2),
                            (int) (mc.player.getY() - 0.1),
                            (int) (mc.player.getZ() + forwardZ * 2)
                    );
                    if (BlockUtil.isReplaceable(front1) || BlockUtil.isReplaceable(front2)) {
                        mc.player.jump();
                    }
                    break;
                case 1:
                    mc.player.jump();
                    break;
                case 2:
                    break;
            }
            if (eagle.getValue() && mode.getValue() == 0) {
                waitingForEagleSneak = true;
                tellyJumpTicks = 0;
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || mc.player == null) {
            return;
        }
        if (this.cancelMove) {
            mc.player.input.movementForward = 0.0F;
            mc.player.input.movementSideways = 0.0F;
            mc.player.setVelocity(0.0, 0.0, 0.0);
        } else if (RotationState.isActived() && RotationState.getPriority() == 3.0F && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
        }
        if (mode.getValue() == 0 && eagle.getValue()) {
            PlayerInput pi = mc.player.input.playerInput;
            mc.player.input.playerInput = new PlayerInput(pi.forward(), pi.backward(), pi.left(), pi.right(), pi.jump(), placeCount % 4 == 0, pi.sprint());
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (lastPlacePosition != null && mark.getValue() && this.isEnabled()) {
            Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
            Box box = new Box(
                    lastPlacePosition.getX() - cameraPos.x,
                    lastPlacePosition.getY() - cameraPos.y,
                    lastPlacePosition.getZ() - cameraPos.z,
                    lastPlacePosition.getX() + 1 - cameraPos.x,
                    lastPlacePosition.getY() + 1 - cameraPos.y,
                    lastPlacePosition.getZ() + 1 - cameraPos.z
            );
            RenderUtil.enableRenderState();
            RenderUtil.drawBoundingBox(box, 255, 255, 255, 150, 1.0F);
            RenderUtil.disableRenderState();
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || mc.player == null || !blockCount.getValue()) {
            return;
        }
        int newCount = Math.max(0, getBlockCountHotbar());
        if (newCount > startHotbarCount) {
            startHotbarCount = newCount;
        }
        float centerX = mc.getWindow().getScaledWidth() / 2f;
        float centerY = mc.getWindow().getScaledHeight() / 2f;
        float y = centerY + 15f + blockCountOffset.getValue();
        String text = newCount + " Blocks";
        int x = Math.round(centerX - (mc.textRenderer.getWidth(text) / 2f));
        event.getContext().drawText(mc.textRenderer, text, x, (int) y, getBlockCountColor(newCount), true);
        lastCount = newCount;
        count = newCount;
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled() && !(mc.currentScreen instanceof HandledScreen)) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled() && !(mc.currentScreen instanceof HandledScreen)) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isEnabled() && !(mc.currentScreen instanceof HandledScreen)) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (this.isEnabled()) {
            this.oldSlot = event.setSlot(this.oldSlot);
            event.setCancelled(true);
        }
    }

    public int getSlot() {
        return this.oldSlot;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{mode.getModeString()};
    }

    public record BlockData(BlockPos blockPos, Direction facing) {
    }

    private record SlotData(int slot, boolean offhand) {
        public boolean check() {
            if (mc.player == null) {
                return true;
            }
            if (offhand) {
                return !isFullBlock(mc.player.getOffHandStack());
            }
            return !isFullBlock(mc.player.getInventory().getStack(slot));
        }
    }

    private static final class Rotation {
        float yaw;
        float pitch;

        Rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}

