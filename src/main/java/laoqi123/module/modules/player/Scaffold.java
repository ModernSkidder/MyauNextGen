package laoqi123.module.modules.player;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.MoveInputEvent;
import laoqi123.event.impl.Render2DEvent;
import laoqi123.event.impl.Render3DEvent;
import laoqi123.event.impl.TickEvent;
import laoqi123.event.impl.UpdateEvent;
import laoqi123.module.Module;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.FloatValue;
import laoqi123.value.properties.IntValue;
import laoqi123.value.properties.ModeValue;
import laoqi123.util.BlockData;
import laoqi123.util.InventoryUtil;
import laoqi123.util.MovementUtils;
import laoqi123.util.PacketUtil;
import laoqi123.util.RandomUtils;
import laoqi123.util.RenderUtil;
import laoqi123.util.RotationUtils;
import laoqi123.util.SlotUtils;
import laoqi123.util.player.FallingPlayer;
import laoqi123.util.player.PlayerUtils;
import laoqi123.util.raytrace.ClientRayTraceUtil;
import laoqi123.util.rotation.Rotation;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scaffold extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Telly", "Snap", "Normal"});
    /**
     * 视觉修复: 开启后 mixin 不再把 renderPitch/renderYaw 往服务器旋转上收敛/外推,
     * 手部模型完全跟随自然视角(不抽搐)。逻辑(服务器旋转、lastYaw/lastPitch 跟踪)完全不变。
     */
    public final BooleanValue moduleFix = new BooleanValue("Module Fix", true);
    public final BooleanValue alwaysUpdateRot = new BooleanValue("Always Update Rotation", false);
    public final IntValue placeTick = new IntValue("PlaceTick", 1, 1, 5, () -> mode.getValue() == 0);
    public final IntValue rotTick = new IntValue("RotationTick", 1, 1, 5);
    public final BooleanValue noSwing = new BooleanValue("No Swing", false);
    public final BooleanValue eagle = new BooleanValue("Eagle", false);
    public final BooleanValue snap = new BooleanValue("Snap", false);
    public final BooleanValue noUptelly = new BooleanValue("No Uptelly", true);
    public final BooleanValue godBridge = new BooleanValue("GodBridge", false, () -> mode.getValue() == 2);
    public final BooleanValue smoothed = new BooleanValue("Heypixel UpTelly", true, () -> mode.getValue() == 0);
    public final BooleanValue safeMode = new BooleanValue("Safe Mode", false, () -> mode.getValue() == 0 && smoothed.getValue());
    public final BooleanValue testOnGround = new BooleanValue("Test OnGround", false);
    public final BooleanValue fixRotation = new BooleanValue("Fix Rotation", true);
    public final BooleanValue randomSlow = new BooleanValue("SlowUpTelly", false);
    public final BooleanValue spoofItem = new BooleanValue("Spoof Item", true);
    public final BooleanValue keepFoV = new BooleanValue("Keep FoV", true);
    public final FloatValue fovValue = new FloatValue("Fov", 1.1f, 1.0f, 2.1f, () -> keepFoV.getValue());
    public final ModeValue blockSlotMode = new ModeValue("Block Slot Mode", 0, new String[]{"Farthest", "Most Blocks"});
    public final ModeValue jumpMode = new ModeValue("Jump Mode", 0, new String[]{"Normal", "Parkour", "None"});
    public final FloatValue safeDistance = new FloatValue("Clutch Safe Distance", 4.5f, 1f, 5f);
    public final IntValue tellyEagleTick = new IntValue("EagleTick", 1, 1, 5, () -> eagle.getValue());
    public final IntValue keepEagleSneakTick = new IntValue("KeepEagleTick", 1, 1, 5, () -> eagle.getValue());
    public final BooleanValue mark = new BooleanValue("Mark", true);
    public final BooleanValue blockCount = new BooleanValue("BlockCount", true);
    public final ModeValue blockCountStyle = new ModeValue("BlockCount Style", 0, new String[]{"Retro", "Old"}, () -> blockCount.getValue());
    public final IntValue blockCountOffset = new IntValue("BlockCount Y Offset", 0, 0, 200, () -> blockCount.getValue());

    public static final List<Block> invalidBlocks = Arrays.asList(
            Blocks.ENCHANTING_TABLE, Blocks.OAK_SIGN, Blocks.CHEST, Blocks.ENDER_CHEST,
            Blocks.TRAPPED_CHEST, Blocks.ANVIL, Blocks.SAND, Blocks.COBWEB, Blocks.TORCH,
            Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.WATER_CAULDRON, Blocks.DISPENSER,
            Blocks.STONE_PRESSURE_PLATE, Blocks.BAMBOO_PRESSURE_PLATE, Blocks.NOTE_BLOCK,
            Blocks.DROPPER, Blocks.TNT, Blocks.REDSTONE_TORCH, Blocks.DAYLIGHT_DETECTOR,
            Blocks.OAK_SIGN, Blocks.BIRCH_SIGN, Blocks.SPRUCE_SIGN, Blocks.JUNGLE_SIGN,
            Blocks.ACACIA_SIGN, Blocks.DARK_OAK_SIGN, Blocks.MANGROVE_SIGN, Blocks.CHERRY_SIGN,
            Blocks.BAMBOO_SIGN, Blocks.CRIMSON_SIGN, Blocks.WARPED_SIGN,
            Blocks.OAK_HANGING_SIGN, Blocks.BIRCH_HANGING_SIGN, Blocks.SPRUCE_HANGING_SIGN, Blocks.JUNGLE_HANGING_SIGN,
            Blocks.ACACIA_HANGING_SIGN, Blocks.DARK_OAK_HANGING_SIGN, Blocks.MANGROVE_HANGING_SIGN, Blocks.CHERRY_HANGING_SIGN,
            Blocks.BAMBOO_HANGING_SIGN, Blocks.CRIMSON_HANGING_SIGN, Blocks.WARPED_HANGING_SIGN);

    private SlotData slot;
    private SlotData blockSlot;
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
    private int oldSlot;
    private int placeCount = 0;
    private int ups = 0;
    private float serverYaw;
    private float serverPitch;
    private float playerYaw;

    public Scaffold() {
        super("Scaffold", false);
    }

    /**
     * 当前是否应让旋转 mixin 跳过对渲染态字段(renderPitch/renderYaw)的干预。
     * 仅当 Scaffold 开启且 "Module Fix" 选项打开时为 true;此时 mixin 只保留逻辑
     * (lastYaw/lastPitch 跟踪服务器旋转),渲染完全跟随自然视角,手部不抽搐。
     */
    public static boolean isModuleFixActive() {
        if (Myau.moduleManager == null) {
            return false;
        }
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        return scaffold != null && scaffold.isEnabled() && scaffold.moduleFix.getValue();
    }

    @Override
    public void onEnabled() {
        placeCount = 0;
        ups = 0;
        if (mc.player == null) return;
        lastRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        serverYaw = mc.player.getYaw();
        serverPitch = mc.player.getPitch();
        this.slot = new SlotData(mc.player.getInventory().selectedSlot, Hand.MAIN_HAND);
        this.oldSlot = mc.player.getInventory().selectedSlot;
        this.blockSlot = null;
        startHotbarCount = Math.max(1, getBlockCountHotbar());
        blockData = null;
        canPlace = true;
        lastPlacePosition = null;
        tellyJumpTicks = 0;
        waitingForEagleSneak = false;
        rot = null;
        ClientRayTraceUtil.updateEyePos();
    }

    @Override
    public void onDisabled() {
        if (mc.player == null) return;
        mc.player.getInventory().selectedSlot = oldSlot;
        mc.options.sneakKey.setPressed(false);
        MovementUtils.resetMove();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.PRE || !isEnabled() || mc.player == null) return;
        // 捕获玩家自然朝向(此刻旋转还没被 mixin 应用,getYaw() 仍是真实视角)
        this.playerYaw = mc.player.getYaw();
        ClientRayTraceUtil.updateEyePos();

        this.blockSlot = null;
        if (InventoryUtil.isFullBlock(mc.player.getOffHandStack()) && isValid(mc.player.getOffHandStack().getItem())) {
            this.blockSlot = new SlotData(SlotUtils.OFFHAND, Hand.OFF_HAND);
        }
        if (blockSlot == null && blockSlotMode.getValue() != 1) {
            if (InventoryUtil.isFullBlock(mc.player.getMainHandStack()) && isValid(mc.player.getMainHandStack().getItem())) {
                this.blockSlot = new SlotData(mc.player.getInventory().selectedSlot, Hand.MAIN_HAND);
            }
        }
        if (blockSlot == null) {
            int hotbarSlot = getHotbarBlockSlot();
            if (hotbarSlot != -1) {
                this.blockSlot = new SlotData(hotbarSlot, Hand.MAIN_HAND);
            }
        }
        if (this.blockSlot == null || blockSlot.check()) return;

        if (mc.player.isOnGround()) {
            posY = Math.floor(mc.player.getY() - 1);
        }
        if (mc.options.jumpKey.isPressed()) {
            posY = mc.player.getBlockY() - 1;
        }
        BlockData possible = ClientRayTraceUtil.isIgnoredBlock(mc.world.getBlockState(
                new BlockPos((int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ())))) ?
                getBlockData(new BlockPos((int) Math.floor(mc.player.getX()), (int) posY, (int) Math.floor(mc.player.getZ()))) : null;
        if (possible != null) {
            blockData = possible;
        }
        lastBlockData = possible;

        if (mode.getValue() == 2) {
            canPlace = true;
        } else if (mode.getValue() == 1) {
            canPlace = doesNotContainBlock(1);
        } else {
            canPlace = PlayerUtils.offGroundTicks >= placeTick.getValue();
            if (safeMode.getValue() && testOnGround.getValue() && !canPlace && mc.options.jumpKey.isPressed()) {
                canPlace = PlayerUtils.onGroundTicks == 1;
            }
        }

        if (this.blockSlot.hand() == Hand.MAIN_HAND) {
            mc.player.getInventory().selectedSlot = this.blockSlot.slot();
        }
        FallingPlayer fallingPlayer = new FallingPlayer(mc.player);
        boolean reachable = true;
        fallingPlayer.calculate(1);
        Vec3d nextEyePos = fallingPlayer.getEyePos();
        fallingPlayer.calculate(1);
        BlockData placement = getBlockData(new BlockPos((int) Math.floor(mc.player.getX()), mc.player.getBlockY() - 1, (int) Math.floor(mc.player.getZ())));
        boolean forceRotation = false;
        if (placement != null) {
            if (safeMode.getValue() && testOnGround.getValue() && PlayerUtils.onGroundTicks == 1 && mc.options.jumpKey.isPressed()) {
                forceRotation = true;
            }
            double distance = nextEyePos.distanceTo(placement.pos().toCenterPos());
            if (distance >= safeDistance.getValue() || placement.pos().getY() > fallingPlayer.getY()) {
                canPlace = true;
                reachable = false;
                blockData = lastBlockData = placement;
            }
        }
        if (blockData != null) {
            Box box = new Box(this.blockData.pos())
                    .withMinY(this.blockData.pos().getY() - 1)
                    .withMaxY(this.blockData.pos().getY() + 1);
            if (blockData.pos().getY() > fallingPlayer.getY() && !box.contains(mc.player.getPos())) {
                canPlace = true;
                reachable = false;
                posY = mc.player.getBlockY() - 1;
                blockData = lastBlockData = getBlockData(new BlockPos((int) Math.floor(mc.player.getX()), (int) Math.floor(posY), (int) Math.floor(mc.player.getZ())));
            }
        }
        if (!reachable && rotateCount < 8) {
            MovementUtils.cancelMove();
            rotateCount++;
        } else {
            rotateCount = 0;
        }

        rot = getBRot(forceRotation);
        if (rot == null) {
            rot = new Rotation(serverYaw, 75.5f);
        }
        float rotYaw = rot.getYaw();
        float rotPitch = rot.getPitch();
        rotPitch -= RandomUtils.generateRandomFloat(0.001f, 0.003f);
        rotYaw -= RandomUtils.generateRandomFloat(0.0001f, 0.0003f);
        do {
            rotPitch -= RandomUtils.generateRandomFloat(0.001f, 0.003f);
        } while (rotPitch > 90f);
        if (rotPitch < -90f) {
            rotPitch = -90f;
        }
        rot = new Rotation(rotYaw, rotPitch);

        if (didHitBlockFace(blockData, rot)) {
            MovementUtils.resetMove();
            rotateCount = 0;
        }
        if (fixRotation.getValue()) {
            rot = rot.normalize();
        }
        serverYaw = rot.getYaw();
        serverPitch = rot.getPitch();
        event.setRotation(serverYaw, serverPitch, 10);
        // 关键:把旋转喂给 MoveFix(setPervRotation → RotationState.smoothYaw),
        // MixinEntityLivingBase 会用 smoothYaw 让移动跟随旋转,与上报的包 yaw 一致,
        // 否则移动永远按自然朝向而包带着旋转 → Grim Simulation 一直判定异常
        event.setPervRotation(serverYaw, 10);

        if (blockData == null) return;
        if (mc.player.isSpectator()) {
            setEnabled(false);
            return;
        }
        place();
        if (mode.getValue() == 0) {
            return;
        }
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

    private Rotation getBRot(boolean forceRotation) {
        Rotation rotation = blockData != null ? RotationUtils.getClosestToBlockFace(blockData.pos(), blockData.facing(), serverYaw, serverPitch) : null;
        if (rotation == null) {
            if (RotationUtils.normalizeYawDiff(mc.player.getYaw() + 100f, serverYaw) < RotationUtils.normalizeYawDiff(mc.player.getYaw() - 100f, serverYaw)) {
                rotation = new Rotation(mc.player.getYaw() + 100f, serverPitch);
            } else {
                rotation = new Rotation(mc.player.getYaw() - 100f, serverPitch);
            }
        }
        if (MovementUtils.cancelMove) {
            rotation = RotationUtils.getClosestToBlockFace(blockData.pos(), blockData.facing(), serverYaw, serverPitch);
            if (rotation == null) {
                rotation = new Rotation(serverYaw, 75.5f);
            }
        }
        float diff = RotationUtils.yawDiffDirectly(rotation.getYaw(), serverYaw);
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
            if (smoothed.getValue() && (PlayerUtils.offGroundTicks < rotTick.getValue() || safeMode.getValue())) {
                if (PlayerUtils.onGroundTicks > 0) {
                    if (safeMode.getValue() && (!testOnGround.getValue() || mc.options.jumpKey.isPressed())) {
                        switch (PlayerUtils.onGroundTicks) {
                            case 1: {
                                if (!forceRotation) {
                                    rotation = new Rotation(serverYaw + RotationUtils.smooth(diff, diff / 2f), 75.5f);
                                } else {
                                    Rotation forced = RotationUtils.getClosestToBlockFace(blockData.pos(), blockData.facing(), mc.player.getYaw(), serverPitch);
                                    rotation = forced != null ? forced : new Rotation(serverYaw + RotationUtils.smooth(diff, diff / 2f), 75.5f);
                                }
                                ((laoqi123.mixin.LivingEntityAccessor) mc.player).setJumpTicks(2);
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
                    float smooth = PlayerUtils.offGroundTicks == 1 ? 80f : 50.0f;
                    smooth -= RandomUtils.generateRandomFloat(0.001f, 0.005f);
                    rotation = new Rotation(serverYaw + RotationUtils.smooth(diff, smooth), rotation.getPitch());
                }
            } else {
                if (snap.getValue() && mc.options.jumpKey.isPressed()) {
                    if (lastBlockData == null || PlayerUtils.offGroundTicks < rotTick.getValue()) {
                        return new Rotation(mc.player.getYaw(), 85.0f + (float) Math.random());
                    }
                } else {
                    if (PlayerUtils.offGroundTicks < rotTick.getValue()) {
                        return new Rotation(mc.player.getYaw(), 85.0f + (float) Math.random());
                    }
                }
            }
        }
        if (lastRotation != null && blockData != null && ClientRayTraceUtil.didHitBlockFace(mc.player, lastRotation.getYaw(), lastRotation.getPitch(), blockData.pos(), blockData.facing(), true)) {
            return lastRotation;
        }
        if (blockData != null && !alwaysUpdateRot.getValue() && PlayerUtils.offGroundTicks >= rotTick.getValue()) {
            if (!ClientRayTraceUtil.didHitBlockFace(mc.player, rotation.getYaw(), rotation.getPitch(), blockData.pos(), blockData.facing(), true) && PlayerUtils.offGroundTicks >= rotTick.getValue()) {
                float lastYaw = lastRotation != null ? lastRotation.getYaw() : rotation.getYaw();
                lastYaw += (float) Math.random();
                return new Rotation(lastYaw, lastRotation != null ? lastRotation.getPitch() : rotation.getPitch());
            }
        }
        lastRotation = rotation;
        return rotation;
    }

    private void place() {
        if (blockData == null || mc.interactionManager == null || !canPlace || rot == null) {
            return;
        }
        BlockHitResult block = ClientRayTraceUtil.getFacedBlock(rot.getYaw(), rot.getPitch());
        if (!ClientRayTraceUtil.didHitBlockFace(mc.player, rot.getYaw(), rot.getPitch(), blockData.pos(), blockData.facing(), true)) {
            return;
        }
        if (this.blockSlot.hand() == Hand.MAIN_HAND) {
            mc.player.getInventory().selectedSlot = this.blockSlot.slot();
        }
        ActionResult result = mc.interactionManager.interactBlock(mc.player, this.blockSlot.hand(), block);
        if (result == ActionResult.SUCCESS) {
            placeCount++;
            lastPlacePosition = blockData.pos().offset(blockData.facing());
            if (PlayerUtils.lastPitchDiff > 0.0d) {
                PlayerUtils.lastPlacePitchDiff = PlayerUtils.lastPitchDiff;
            }
            if (noSwing.getValue()) {
                PacketUtil.sendPacket(new HandSwingC2SPacket(this.blockSlot.hand()));
            } else {
                mc.player.swingHand(this.blockSlot.hand());
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.POST || !isEnabled() || mc.player == null) return;
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
    public void onMoveInput(MoveInputEvent event) {
        if (!isEnabled() || mc.player == null || blockSlot == null || blockSlot.check()) return;
        // 移动输入重写(Southside StrafeFix 同款):
        // 保持玩家想要的移动方向(按自然朝向计算的 forward/strafe),把输入改写成
        // "在上报 yaw(serverYaw) 下产生相同方向"的值 —— 这样回头自救时玩家继续向前移动,
        // 而上报 yaw 与重写后的输入对 Grim 依然自洽(不会往回走,也不会爆 Simulation)
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        if (!MovementUtils.cancelMove && (forward != 0.0f || strafe != 0.0f)) {
            float yawDiff = Math.abs(MathHelper.wrapDegrees(serverYaw - this.playerYaw));
            if (yawDiff > 1.0f) {
                double intended = Scaffold.getDirection(this.playerYaw, forward, strafe);
                float bestForward = 0f;
                float bestStrafe = 0f;
                float bestDiff = Float.MAX_VALUE;
                for (float pf = -1f; pf <= 1f; pf += 1f) {
                    for (float ps = -1f; ps <= 1f; ps += 1f) {
                        if (pf == 0f && ps == 0f) continue;
                        double predicted = Scaffold.getDirection(serverYaw, pf, ps);
                        float diff = Math.abs(MathHelper.wrapDegrees(
                                (float) Math.toDegrees(predicted) - (float) Math.toDegrees(intended)));
                        if (diff < bestDiff) {
                            bestDiff = diff;
                            bestForward = pf;
                            bestStrafe = ps;
                        }
                    }
                }
                event.setForward(bestForward);
                event.setStrafe(bestStrafe);
            }
        }
        if (PlayerUtils.onGroundTicks > (smoothed.getValue() && safeMode.getValue() && !testOnGround.getValue() ? 1 : 0)
                && !mc.options.jumpKey.isPressed() && PlayerUtils.isMoving() && mode.getValue() == 0) {
            switch (jumpMode.getValue()) {
                case 2: {
                    break;
                }
                case 0: {
                    event.setJump(true);
                    break;
                }
                case 1: {
                    double yaw = Math.toRadians(mc.player.getYaw());
                    double forwardX = -Math.sin(yaw);
                    double forwardZ = Math.cos(yaw);
                    BlockPos frontPos1 = new BlockPos((int) (mc.player.getX() + forwardX), (int) (mc.player.getY() - 0.1), (int) (mc.player.getZ() + forwardZ));
                    BlockPos frontPos2 = new BlockPos((int) (mc.player.getX() + forwardX * 2), (int) (mc.player.getY() - 0.1), (int) (mc.player.getZ() + forwardZ * 2));
                    if (mc.world.getBlockState(frontPos1).getBlock() instanceof AirBlock
                            || mc.world.getBlockState(frontPos2).getBlock() instanceof AirBlock) {
                        event.setJump(true);
                    }
                    break;
                }
            }
            if (eagle.getValue()) {
                waitingForEagleSneak = true;
                tellyJumpTicks = 0;
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (lastPlacePosition != null && mark.getValue()) {
            Box box = new Box(lastPlacePosition);
            RenderUtil.drawBoundingBox(box, 255, 255, 255, 150, 2.0f);
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!isEnabled() || mc.player == null || !blockCount.getValue()) return;
        int newCount = Math.max(0, getBlockCountHotbar());
        if (newCount > startHotbarCount) {
            startHotbarCount = newCount;
        }
        float centerX = mc.getWindow().getScaledWidth() / 2f;
        float centerY = mc.getWindow().getScaledHeight() / 2f;
        float y = centerY + 15f + blockCountOffset.getValue();
        if (blockCountStyle.getValue() == 1) {
            String text = newCount + " Blocks";
            int x = Math.round(centerX - (mc.textRenderer.getWidth(text) / 2f));
            event.getContext().drawText(mc.textRenderer, text, x, Math.round(y), getBlockCountColor(newCount), true);
            lastCount = newCount;
            count = newCount;
            return;
        }
        ItemStack displayStack = getHeldBlockStack();
        String countText = String.valueOf(newCount);
        String label = "Blocks";
        float textWidth = mc.textRenderer.getWidth(countText) + 3f + mc.textRenderer.getWidth(label);
        float x = centerX - (textWidth + (displayStack.isEmpty() ? 0f : 22f)) / 2f;
        float baseY = y + 4f;
        if (!displayStack.isEmpty()) {
            event.getContext().drawItem(displayStack, Math.round(x), Math.round(baseY));
            x += 18f;
        }
        event.getContext().drawText(mc.textRenderer, label, Math.round(x), Math.round(baseY + 1), getBlockCountColor(newCount), true);
        x += mc.textRenderer.getWidth(label) + 3f;
        event.getContext().drawText(mc.textRenderer, countText, Math.round(x), Math.round(baseY + 1), 0xFFFFFFFF, true);
        lastCount = newCount;
        count = newCount;
    }

    private static boolean didHitBlockFace(BlockData blockData, Rotation rot) {
        return blockData == null || !ClientRayTraceUtil.didHitBlockFace(rot, blockData.pos(), blockData.facing(), true);
    }

    /**
     * 由 yaw + 输入(forward/strafe)计算移动方向角(弧度),与 Southside StrafeFix.getDirection 一致。
     */
    private static double getDirection(float yaw, double forward, double strafe) {
        if (forward < 0.0) {
            yaw += 180.0f;
        }
        float f = 1.0f;
        if (forward < 0.0) {
            f = -0.5f;
        } else if (forward > 0.0) {
            f = 0.5f;
        }
        if (strafe > 0.0) {
            yaw -= 90.0f * f;
        }
        if (strafe < 0.0) {
            yaw += 90.0f * f;
        }
        return Math.toRadians(yaw);
    }

    public boolean doesNotContainBlock(int down) {
        return mc.world.getBlockState(PlayerUtils.blockRelativeToPlayer(0, -down, 0)).isTransparent();
    }

    private boolean isValid(final Item item) {
        return item instanceof BlockItem && !invalidBlocks.contains(((BlockItem) item).getBlock()) && SlotUtils.isGoodForBridging(item);
    }

    private int getHotbarBlockSlot() {
        if (blockSlotMode.getValue() == 1) {
            return getMostBlocksHotbarSlot();
        }
        int slot = -1;
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (InventoryUtil.isFullBlock(stack) && isValid(stack.getItem())) {
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
        if (InventoryUtil.isFullBlock(selectedStack) && isValid(selectedStack.getItem())) {
            bestSlot = selectedSlot;
            bestCount = selectedStack.getCount();
        }
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (InventoryUtil.isFullBlock(stack) && isValid(stack.getItem()) && stack.getCount() > bestCount) {
                bestSlot = i;
                bestCount = stack.getCount();
            }
        }
        return bestSlot;
    }

    private ItemStack getHeldBlockStack() {
        if (mc.player == null) return ItemStack.EMPTY;
        ItemStack main = mc.player.getMainHandStack();
        if (isDisplayBlock(main)) return main;
        ItemStack offhand = mc.player.getOffHandStack();
        if (isDisplayBlock(offhand)) return offhand;
        return ItemStack.EMPTY;
    }

    private boolean isDisplayBlock(ItemStack stack) {
        return stack != null && !stack.isEmpty() && isValid(stack.getItem());
    }

    private int getBlockCountColor(int count) {
        if (count < 16) {
            return 0xFFFF5050;
        }
        if (count < 32) {
            return 0xFFFFDC50;
        }
        return 0xFFFFFFFF;
    }

    public int getBlockCountInventory() {
        int blockCount = 0;
        for (int i = 9; i < 45; ++i) {
            if (mc.player == null) return -1;
            if (mc.player.getInventory().getStack(i).isStackable()) {
                ItemStack is = mc.player.getInventory().getStack(i);
                if (is.getItem() instanceof BlockItem block) {
                    if (isValid(block)) {
                        blockCount += is.getCount();
                    }
                }
            }
        }
        return blockCount;
    }

    public int getBlockCountHotbar() {
        if (mc.player == null) return 0;
        int blockCount = 0;
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                if (isValid(blockItem)) {
                    blockCount += stack.getCount();
                }
            }
        }
        ItemStack offhandStack = mc.player.getOffHandStack();
        if (!offhandStack.isEmpty() && offhandStack.getItem() instanceof BlockItem offhandBlock) {
            if (isValid(offhandBlock)) {
                blockCount += offhandStack.getCount();
            }
        }
        return blockCount;
    }

    private BlockData getBlockData(BlockPos pos) {
        BlockData data;
        if (getPos(pos) == null) {
            BlockPos blockPos = getBlockPos();
            if (blockPos == null) return null;
            Direction direction = getPlaceSide(blockPos);
            if (direction == null) return null;
            data = new BlockData(blockPos, direction);
        } else {
            data = getPos(pos);
        }
        if (ClientRayTraceUtil.isIgnoredBlock(mc.world.getBlockState(data.pos().offset(data.facing())))) {
            return data;
        }
        return null;
    }

    private Direction getPlaceSide(BlockPos blockPos) {
        List<BlockData> blockData = new ArrayList<>();
        BlockPos pos = new BlockPos((int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ()));

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
        if (blockData.isEmpty()) return null;

        blockData.sort(Comparator.comparingDouble(vec3 -> vec3.pos().getSquaredDistance(pos)));
        blockData.removeIf(blockData1 -> !ClientRayTraceUtil.isIgnoredBlock(mc.world.getBlockState(blockData1.pos().offset(blockData1.facing()))));
        return blockData.getFirst().facing();
    }

    private BlockPos getBlockPos() {
        BlockPos playerPos = new BlockPos((int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ()));
        ArrayList<BlockPos> positions = new ArrayList<>();
        Map<BlockPos, Block> searchBlock = searchBlocks(5);
        for (Map.Entry<BlockPos, Block> block : searchBlock.entrySet()) {
            if (isPosSolid(block.getKey())) {
                positions.add(block.getKey());
            }
        }
        positions.removeIf(pos -> pos.getY() >= playerPos.getY());
        if (positions.isEmpty()) return null;
        positions.sort(Comparator.comparingDouble(vec3 -> vec3.getSquaredDistance(playerPos)));
        return positions.getFirst();
    }

    public boolean isAirBlock(BlockPos blockPos) {
        return ClientRayTraceUtil.isIgnoredBlock(mc.world.getBlockState(blockPos));
    }

    public Block getBlock(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock();
    }

    public Map<BlockPos, Block> searchBlocks(int radius) {
        Map<BlockPos, Block> blocks = new HashMap<>();
        if (mc.player == null) {
            return blocks;
        }
        for (int x = radius; x >= -radius + 1; x--) {
            for (int y = radius; y >= -radius + 1; y--) {
                for (int z = radius; z >= -radius + 1; z--) {
                    BlockPos blockPos = new BlockPos(mc.player.getBlockX() + x, mc.player.getBlockY() + y, mc.player.getBlockZ() + z);
                    Block block = getBlock(blockPos);
                    if (block != null) {
                        blocks.put(blockPos, block);
                    }
                }
            }
        }
        return blocks;
    }

    public BlockData getPos(BlockPos pos) {
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

    public boolean isPosSolid(BlockPos pos) {
        final Block block = mc.world.getBlockState(pos).getBlock();
        if (block instanceof TrapdoorBlock || block instanceof DoorBlock || block instanceof FenceGateBlock) {
            return false;
        }
        return !Arrays.asList(
                Blocks.ANVIL,
                Blocks.AIR,
                Blocks.WATER,
                Blocks.FIRE,
                Blocks.LAVA,
                Blocks.SKELETON_SKULL,
                Blocks.OAK_SIGN,
                Blocks.TRAPPED_CHEST,
                Blocks.CHEST,
                Blocks.ENCHANTING_TABLE,
                Blocks.ENDER_CHEST,
                Blocks.CRAFTING_TABLE,
                Blocks.DAYLIGHT_DETECTOR,
                Blocks.COBWEB,
                Blocks.SHORT_GRASS,
                Blocks.FLOWER_POT,
                Blocks.CHORUS_FLOWER,
                Blocks.SUNFLOWER,
                Blocks.CORNFLOWER,
                Blocks.TORCHFLOWER,
                Blocks.OAK_BUTTON,
                Blocks.ACACIA_BUTTON,
                Blocks.BIRCH_BUTTON,
                Blocks.CRIMSON_BUTTON,
                Blocks.CHERRY_BUTTON,
                Blocks.DARK_OAK_BUTTON,
                Blocks.JUNGLE_BUTTON,
                Blocks.STONE_BUTTON,
                Blocks.WARPED_BUTTON,
                Blocks.SPRUCE_BUTTON,
                Blocks.NOTE_BLOCK,
                Blocks.PLAYER_HEAD
        ).contains(block) && !ClientRayTraceUtil.isIgnoredBlock(mc.world.getBlockState(pos));
    }

    public int getSlot() {
        if (blockSlot != null && blockSlot.hand() == Hand.MAIN_HAND && blockSlot.slot() >= 0 && blockSlot.slot() <= 8) {
            return blockSlot.slot();
        }
        return oldSlot;
    }

    private record SlotData(int slot, Hand hand) {
        public boolean check() {
            if (mc.player == null) {
                return false;
            }
            if (hand.equals(Hand.OFF_HAND)) {
                var stack = mc.player.getOffHandStack();
                return stack.isEmpty() || !(stack.getItem() instanceof BlockItem);
            }
            return mc.player.getInventory().getStack(slot).isEmpty() || !(mc.player.getInventory().getStack(slot).getItem() instanceof BlockItem);
        }
    }
}
