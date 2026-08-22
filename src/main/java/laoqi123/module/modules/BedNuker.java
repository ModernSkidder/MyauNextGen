package laoqi123.module.modules;

import com.google.common.base.CaseFormat;
import laoqi123.Myau;
import laoqi123.enums.ChatColors;
import laoqi123.enums.DelayModules;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.events.HitBlockEvent;
import laoqi123.events.KnockbackEvent;
import laoqi123.events.LeftClickMouseEvent;
import laoqi123.events.LoadWorldEvent;
import laoqi123.events.MoveInputEvent;
import laoqi123.events.PacketEvent;
import laoqi123.events.PlayerUpdateEvent;
import laoqi123.events.Render2DEvent;
import laoqi123.events.Render3DEvent;
import laoqi123.events.RightClickMouseEvent;
import laoqi123.events.SwapItemEvent;
import laoqi123.events.TickEvent;
import laoqi123.events.UpdateEvent;
import laoqi123.management.RotationState;
import laoqi123.mixin.ClientPlayerInteractionManagerAccessor;
import laoqi123.module.Module;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.ModeProperty;
import laoqi123.property.properties.PercentProperty;
import laoqi123.util.BlockUtil;
import laoqi123.util.ColorUtil;
import laoqi123.util.ItemUtil;
import laoqi123.util.MoveUtil;
import laoqi123.util.PacketUtil;
import laoqi123.util.PlayerUtil;
import laoqi123.util.RenderUtil;
import laoqi123.util.RotationUtil;
import laoqi123.util.TimerUtil;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BedNuker extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final TimerUtil timer = new TimerUtil();
    private final ArrayList<BlockPos> bedWhitelist = new ArrayList<BlockPos>();
    private final Color colorRed = new Color(ChatColors.RED.toAwtColor());
    private final Color colorYellow = new Color(ChatColors.YELLOW.toAwtColor());
    private final Color colorGreen = new Color(ChatColors.GREEN.toAwtColor());
    private BlockPos targetBed = null;
    private int breakStage = 0;
    private int tickCounter = 0;
    private float breakProgress = 0.0F;
    private boolean isBed = false;
    private int savedSlot = -1;
    private boolean readyToBreak = false;
    private boolean breaking = false;
    private boolean waitingForStart = false;
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"LEGIT", "SWAP"});
    public final FloatProperty range = new FloatProperty("range", 4.5F, 3.0F, 6.0F);
    public final PercentProperty speed = new PercentProperty("speed", 0);
    public final BooleanProperty groundSpeed = new BooleanProperty("ground-spoof", false);
    public final ModeProperty ignoreVelocity = new ModeProperty("ignore-velocity", 0, new String[]{"NONE", "CANCEL", "DELAY"});
    public final BooleanProperty surroundings = new BooleanProperty("surroundings", true);
    public final BooleanProperty toolCheck = new BooleanProperty("tool-check", true);
    public final BooleanProperty whiteList = new BooleanProperty("whitelist", true);
    public final BooleanProperty swing = new BooleanProperty("swing", true);
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[]{"NONE", "SILENT", "STRICT"});
    public final ModeProperty showTarget = new ModeProperty("show-target", 1, new String[]{"NONE", "DEFAULT", "HUD"});
    public final ModeProperty showProgress = new ModeProperty("show-progress", 1, new String[]{"NONE", "DEFAULT", "HUD"});

    private void resetBreaking() {
        if (this.targetBed != null) {
            mc.world.setBlockBreakingInfo(mc.player.getId(), this.targetBed, -1);
        }
        this.targetBed = null;
        this.breakStage = 0;
        this.tickCounter = 0;
        this.breakProgress = 0.0F;
        this.isBed = false;
        this.readyToBreak = false;
        this.breaking = false;
    }

    private float calcProgress() {
        if (this.targetBed == null) {
            return 0.0F;
        } else {
            float progress = this.breakProgress;
            if (this.groundSpeed.getValue()) {
                int slot = ItemUtil.findInventorySlot(mc.player.getInventory().selectedSlot, mc.world.getBlockState(this.targetBed).getBlock());
                progress = (float) this.tickCounter * this.getBreakDelta(mc.world.getBlockState(this.targetBed), this.targetBed, slot, true);
            }
            return Math.min(1.0F, progress / (1.0F - 0.3F * ((float) this.speed.getValue().intValue() / 100.0F)));
        }
    }

    private void restoreSlot() {
        if (this.savedSlot != -1) {
            mc.player.getInventory().selectedSlot = this.savedSlot;
            this.syncHeldItem();
            this.savedSlot = -1;
        }
    }

    private void syncHeldItem() {
        int currentPlayerItem = ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getCurrentPlayerItem();
        if (mc.player.getInventory().selectedSlot != currentPlayerItem) {
            mc.player.stopUsingItem();
        }
        ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).callSyncCurrentPlayItem();
    }

    private boolean hasProperTool(Block block) {
        if (!block.getDefaultState().isToolRequired()) {
            return true;
        } else {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (!stack.isEmpty() && stack.getItem() instanceof PickaxeItem) {
                    return true;
                }
            }
            return false;
        }
    }

    private Direction getHitFacing(BlockPos blockPos) {
        double x = (double) blockPos.getX() + 0.5 - mc.player.getX();
        double y = (double) blockPos.getY() + 0.25 - mc.player.getY() - (double) mc.player.getStandingEyeHeight();
        double z = (double) blockPos.getZ() + 0.5 - mc.player.getZ();
        float[] rotations = RotationUtil.getRotationsTo(x, y, z, mc.player.getYaw(), mc.player.getPitch());
        HitResult mop = RotationUtil.rayTrace(rotations[0], rotations[1], 8.0, 1.0F);
        return mop instanceof BlockHitResult ? ((BlockHitResult) mop).getSide() : Direction.UP;
    }

    private boolean hasAquaAffinity() {
        RegistryEntry<Enchantment> entry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.AQUA_AFFINITY.getValue()).orElse(null);
        return entry != null && EnchantmentHelper.getEquipmentLevel(entry, mc.player) > 0;
    }

    private float getDigSpeed(BlockState blockState, int slot, boolean boolean5) {
        ItemStack item = mc.player.getInventory().getStack(slot);
        float digSpeed = item.isEmpty() ? 1.0F : item.getMiningSpeedMultiplier(blockState);
        if (digSpeed > 1.0F) {
            RegistryEntry<Enchantment> entry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.EFFICIENCY.getValue()).orElse(null);
            int enchantmentLevel = entry == null ? 0 : EnchantmentHelper.getLevel(entry, item);
            if (enchantmentLevel > 0) {
                digSpeed += (float) (enchantmentLevel * enchantmentLevel + 1);
            }
        }
        if (mc.player.hasStatusEffect(StatusEffects.HASTE)) {
            digSpeed *= 1.0F + (float) (mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1) * 0.2F;
        }
        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0:
                    digSpeed *= 0.3F;
                    break;
                case 1:
                    digSpeed *= 0.09F;
                    break;
                case 2:
                    digSpeed *= 0.0027F;
                    break;
                default:
                    digSpeed *= 8.1E-4F;
            }
        }
        if (mc.player.isTouchingWater() && !this.hasAquaAffinity()) {
            digSpeed /= 5.0F;
        }
        if (!boolean5) {
            digSpeed /= 5.0F;
        }
        return digSpeed;
    }

    boolean canHarvest(Block block, int slot) {
        if (!block.getDefaultState().isToolRequired()) {
            return true;
        } else {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            return !stack.isEmpty() && stack.isSuitableFor(block.getDefaultState());
        }
    }

    private float getBreakDelta(BlockState blockState, BlockPos blockPos, int slot, boolean boolean5) {
        Block block = blockState.getBlock();
        float hardness = block.getHardness();
        float boost = this.canHarvest(block, slot) ? 30.0F : 100.0F;
        return hardness < 0.0F ? 0.0F : this.getDigSpeed(blockState, slot, boolean5) / hardness / boost;
    }

    private float calcBlockStrength(BlockPos blockPos) {
        BlockState blockState = mc.world.getBlockState(blockPos);
        int slot = ItemUtil.findInventorySlot(mc.player.getInventory().selectedSlot, blockState.getBlock());
        return this.getBreakDelta(blockState, blockPos, slot, mc.player.isOnGround());
    }

    private BlockPos validateBedPlacement(BlockPos bedPosition) {
        BlockState blockState = mc.world.getBlockState(bedPosition);
        if (blockState.getBlock() instanceof BedBlock) {
            ArrayList<BlockPos> pos = new ArrayList<>();
            BedPart partType = blockState.get(BedBlock.PART);
            Direction facing = blockState.get(BedBlock.FACING);
            for (BlockPos blockPos : Arrays.asList(bedPosition, bedPosition.offset(partType == BedPart.HEAD ? facing.getOpposite() : facing))) {
                for (Direction enumFacing : Arrays.asList(Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
                    Block block = mc.world.getBlockState(blockPos.offset(enumFacing)).getBlock();
                    if (BlockUtil.isReplaceable(block.getDefaultState())) {
                        return null;
                    }
                    if (!(block instanceof BedBlock)) {
                        pos.add(blockPos.offset(enumFacing));
                    }
                }
            }
            if (!pos.isEmpty()) {
                pos.sort(
                        (blockPos1, blockPos2) -> {
                            int o = Float.compare(this.calcBlockStrength(blockPos2), this.calcBlockStrength(blockPos1));
                            return o != 0
                                    ? o
                                    : Double.compare(
                                    blockPos1.toCenterPos().squaredDistanceTo(mc.player.getX(), mc.player.getY() + (double) mc.player.getStandingEyeHeight(), mc.player.getZ()),
                                    blockPos2.toCenterPos().squaredDistanceTo(mc.player.getX(), mc.player.getY() + (double) mc.player.getStandingEyeHeight(), mc.player.getZ())
                            );
                        }
                );
                return pos.get(0);
            }
        }
        return null;
    }

    private BlockPos findNearestBed() {
        return this.findTargetBed(mc.player.getX(), mc.player.getY() + (double) mc.player.getStandingEyeHeight(), mc.player.getZ());
    }

    private BlockPos findTargetBed(double x, double y, double z) {
        ArrayList<BlockPos> targets = new ArrayList<>();
        int sX = MathHelper.floor(x);
        int sY = MathHelper.floor(y);
        int sZ = MathHelper.floor(z);
        for (int i = sX - 6; i <= sX + 6; i++) {
            for (int j = sY - 6; j <= sY + 6; j++) {
                for (int k = sZ - 6; k <= sZ + 6; k++) {
                    BlockPos newPos = new BlockPos(i, j, k);
                    if (!(Boolean) this.whiteList.getValue() || !this.bedWhitelist.contains(newPos)) {
                        Block block = mc.world.getBlockState(newPos).getBlock();
                        if (block instanceof BedBlock
                                && PlayerUtil.isBlockWithinReach(newPos, x, y, z, this.range.getValue().doubleValue())) {
                            targets.add(newPos);
                        }
                    }
                }
            }
        }
        if (targets.isEmpty()) {
            return null;
        } else {
            targets.sort(
                    Comparator.comparingDouble(
                            blockPos -> blockPos.toCenterPos().squaredDistanceTo(mc.player.getX(), mc.player.getY() + (double) mc.player.getStandingEyeHeight(), mc.player.getZ())
                    )
            );
            for (BlockPos blockPos : targets) {
                if (this.surroundings.getValue()) {
                    BlockPos pos = this.validateBedPlacement(blockPos);
                    if (pos != null) {
                        Block block = mc.world.getBlockState(pos).getBlock();
                        if (this.toolCheck.getValue() && !this.hasProperTool(block)) {
                            continue;
                        }
                        return pos;
                    }
                }
                return blockPos;
            }
            return null;
        }
    }

    private void doSwing() {
        if (this.swing.getValue()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            PacketUtil.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
    }

    private Color getProgressColor(int mode) {
        switch (mode) {
            case 1:
                float progress = this.calcProgress();
                if (progress <= 0.5F) {
                    return ColorUtil.interpolate(progress / 0.5F, this.colorRed, this.colorYellow);
                }
                return ColorUtil.interpolate((progress - 0.5F) / 0.5F, this.colorYellow, this.colorGreen);
            case 2:
                return ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
            default:
                return new Color(-1);
        }
    }

    public BedNuker() {
        super("BedNuker", false);
    }

    public boolean isReady() {
        return this.targetBed != null && this.readyToBreak;
    }

    public boolean isBreaking() {
        return this.targetBed != null && this.breaking;
    }

    @EventTarget(Priority.HIGH)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            AutoBlockIn autoBlockIn = (AutoBlockIn) Myau.moduleManager.modules.get(AutoBlockIn.class);
            if (autoBlockIn.isEnabled()) return;
            if (this.targetBed != null) {
                if (mc.world.isAir(this.targetBed) || !PlayerUtil.canReach(this.targetBed, this.range.getValue().doubleValue())) {
                    this.restoreSlot();
                    this.resetBreaking();
                } else if (!this.isBed) {
                    BlockPos nearestBed = this.findNearestBed();
                    if (nearestBed != null && mc.world.getBlockState(nearestBed).getBlock() instanceof BedBlock) {
                        this.resetBreaking();
                    }
                }
            }
            if (this.targetBed != null) {
                int slot = ItemUtil.findInventorySlot(mc.player.getInventory().selectedSlot, mc.world.getBlockState(this.targetBed).getBlock());
                if (this.mode.getValue() == 0 && this.savedSlot == -1) {
                    this.savedSlot = mc.player.getInventory().selectedSlot;
                    mc.player.getInventory().selectedSlot = slot;
                    this.syncHeldItem();
                }
                switch (this.breakStage) {
                    case 0:
                        if (!mc.player.isUsingItem()) {
                            this.doSwing();
                            PacketUtil.sendPacket(
                                    new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, this.targetBed, this.getHitFacing(this.targetBed))
                            );
                            this.doSwing();
                            mc.particleManager.addBlockBreakingParticles(this.targetBed, this.getHitFacing(this.targetBed));
                            this.breakStage = 1;
                        }
                        break;
                    case 1:
                        if (this.mode.getValue() == 1) {
                            this.readyToBreak = false;
                        }
                        this.breaking = true;
                        this.tickCounter++;
                        this.breakProgress = this.breakProgress
                                + this.getBreakDelta(mc.world.getBlockState(this.targetBed), this.targetBed, slot, mc.player.isOnGround());
                        float tick = (float) this.tickCounter;
                        BlockState blockState = mc.world.getBlockState(this.targetBed);
                        boolean canBreak = mc.player.isOnGround() && this.groundSpeed.getValue();
                        BlockPos target = this.targetBed;
                        float delta = tick * this.getBreakDelta(blockState, target, slot, canBreak);
                        mc.particleManager.addBlockBreakingParticles(this.targetBed, this.getHitFacing(this.targetBed));
                        if (this.breakProgress >= 1.0F - 0.3F * ((float) this.speed.getValue().intValue() / 100.0F)
                                || delta >= 1.0F - 0.3F * ((float) this.speed.getValue().intValue() / 100.0F)) {
                            if (this.mode.getValue() == 1) {
                                this.readyToBreak = true;
                                this.savedSlot = mc.player.getInventory().selectedSlot;
                                mc.player.getInventory().selectedSlot = slot;
                                this.syncHeldItem();
                                if (mc.player.isUsingItem()) {
                                    this.savedSlot = mc.player.getInventory().selectedSlot;
                                    mc.player.getInventory().selectedSlot = (mc.player.getInventory().selectedSlot + 1) % 9;
                                    this.syncHeldItem();
                                }
                            }
                            this.breaking = false;
                            PacketUtil.sendPacket(
                                    new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, this.targetBed, this.getHitFacing(this.targetBed))
                            );
                            this.doSwing();
                            BlockState blockState_ = mc.world.getBlockState(this.targetBed);
                            Block block = blockState_.getBlock();
                            if (!blockState_.isAir()) {
                                mc.world.syncWorldEvent(2001, this.targetBed, Block.getRawIdFromState(blockState_));
                                mc.world.removeBlock(this.targetBed, false);
                            }
                            if (block instanceof BedBlock) {
                                this.timer.reset();
                            }
                            this.breakStage = 2;
                        }
                        break;
                    case 2:
                        this.restoreSlot();
                        this.resetBreaking();
                }
                if (this.targetBed != null) {
                    return;
                }
            }
            if (mc.player.getAbilities().allowModifyWorld && this.timer.hasTimeElapsed(500)) {
                this.targetBed = this.findNearestBed();
                this.breakStage = 0;
                this.tickCounter = 0;
                this.breakProgress = 0.0F;
                this.isBed = this.targetBed != null && mc.world.getBlockState(this.targetBed).getBlock() instanceof BedBlock;
                this.restoreSlot();
                if (this.targetBed != null) {
                    this.readyToBreak = true;
                }
            }
            if (this.targetBed == null) {
                Myau.delayManager.setDelayState(false, DelayModules.BED_NUKER);
            }
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            AutoBlockIn autoBlockIn = (AutoBlockIn) Myau.moduleManager.modules.get(AutoBlockIn.class);
            if (autoBlockIn.isEnabled()) return;
            if (this.isReady()) {
                double x = (double) this.targetBed.getX() + 0.5 - mc.player.getX();
                double y = (double) this.targetBed.getY() + 0.5 - mc.player.getY() - (double) mc.player.getStandingEyeHeight();
                double z = (double) this.targetBed.getZ() + 0.5 - mc.player.getZ();
                float[] rotations = RotationUtil.getRotationsTo(x, y, z, event.getYaw(), event.getPitch());
                event.setRotation(rotations[0], rotations[1], 5);
                event.setPervRotation(this.moveFix.getValue() != 0 ? rotations[0] : mc.player.getYaw(), 5);
            }
        }
    }

    @EventTarget
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.isEnabled()) {
            if (this.isBreaking()
                    && !Myau.playerStateManager.attacking
                    && !Myau.playerStateManager.digging
                    && !Myau.playerStateManager.placing
                    && !Myau.playerStateManager.swinging) {
                this.doSwing();
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            if (this.moveFix.getValue() == 1
                    && RotationState.isActived()
                    && RotationState.getPriority() == 5.0F
                    && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }
        }
    }

    @EventTarget(Priority.HIGH)
    public void onKnockback(KnockbackEvent event) {
        if (this.isEnabled() && !event.isCancelled() && !(event.getY() <= 0.0)) {
            if (this.ignoreVelocity.getValue() == 1 && this.targetBed != null) {
                event.setCancelled(true);
                event.setX(mc.player.getVelocity().x);
                event.setY(mc.player.getVelocity().y);
                event.setZ(mc.player.getVelocity().z);
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled()) {
            if (this.targetBed != null && (!this.isBed || !this.surroundings.getValue())) {
                if (this.showProgress.getValue() != 0) {
                    HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
                    float scale = hud.scale.getValue();
                    String text = String.format("%d%%", (int) (this.calcProgress() * 100.0F));
                    MatrixStack matrices = event.getContext().getMatrices();
                    matrices.push();
                    matrices.scale(scale, scale, 1.0F);
                    int width = mc.textRenderer.getWidth(text);
                    event.getContext().drawText(
                            mc.textRenderer,
                            text,
                            (int) ((float) mc.getWindow().getScaledWidth() / 2.0F / scale - (float) width / 2.0F),
                            (int) ((float) mc.getWindow().getScaledHeight() / 5.0F * 2.0F / scale),
                            this.getProgressColor(this.showProgress.getValue()).getRGB() & 16777215 | -1090519040,
                            hud.shadow.getValue()
                    );
                    matrices.pop();
                }
            }
        }
    }

    @EventTarget(Priority.LOW)
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && this.targetBed != null && !mc.world.isAir(this.targetBed)) {
            mc.world.setBlockBreakingInfo(mc.player.getId(), this.targetBed, (int) (this.calcProgress() * 10.0F) - 1);
            if (this.showTarget.getValue() != 0) {
                BedESP bedESP = (BedESP) Myau.moduleManager.modules.get(BedESP.class);
                Color color = this.getProgressColor(this.showTarget.getValue());
                RenderUtil.enableRenderState();
                BlockPos target = this.targetBed;
                double newHeight = this.isBed ? bedESP.getHeight() : 1.0;
                int r = color.getRed();
                int g = color.getBlue();
                int b = color.getGreen();
                RenderUtil.drawBlockBox(target, newHeight, r, b, g);
                RenderUtil.disableRenderState();
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.waitingForStart = false;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!event.isCancelled()) {
            if (event.getPacket() instanceof ChatMessageS2CPacket) {
                ChatMessageS2CPacket packet = (ChatMessageS2CPacket) event.getPacket();
                String text = null;
                if (packet.unsignedContent() != null) {
                    text = packet.unsignedContent().getString();
                } else if (packet.body() != null && packet.body().content() != null) {
                    text = packet.body().content();
                }
                if (text != null && (text.contains("搂e搂lProtect your bed and destroy the enemy bed") || text.contains("搂e搂lDestroy the enemy bed and then eliminate them"))) {
                    this.waitingForStart = true;
                }
            }
            if (event.getPacket() instanceof PlayerPositionLookS2CPacket && this.waitingForStart) {
                this.waitingForStart = false;
                this.bedWhitelist.clear();
                this.scheduler.schedule(() -> {
                    ClientPlayerEntity player = mc.player;
                    if (player == null) {
                        return;
                    }
                    int sX = MathHelper.floor(player.getX());
                    int sY = MathHelper.floor(player.getY() + (double) player.getStandingEyeHeight());
                    int sZ = MathHelper.floor(player.getZ());
                    for (int i = sX - 25; i <= sX + 25; i++) {
                        for (int j = sY - 25; j <= sY + 25; j++) {
                            for (int k = sZ - 25; k <= sZ + 25; k++) {
                                BlockPos blockPos = new BlockPos(i, j, k);
                                Block block = mc.world.getBlockState(blockPos).getBlock();
                                if (block instanceof BedBlock) {
                                    this.bedWhitelist.add(blockPos);
                                }
                            }
                        }
                    }
                }, 1L, TimeUnit.SECONDS);
            }
            if (this.isEnabled() && this.targetBed != null && this.ignoreVelocity.getValue() == 2 && Myau.delayManager.getDelayModule() != DelayModules.BED_NUKER) {
                if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket) {
                    EntityVelocityUpdateS2CPacket packet = (EntityVelocityUpdateS2CPacket) event.getPacket();
                    if (packet.getEntityId() == mc.player.getId() && packet.getVelocityY() > 0) {
                        Myau.delayManager.delay(DelayModules.BED_NUKER);
                        Myau.delayManager.delayedPacket.offer(packet);
                        event.setCancelled(true);
                    }
                }
                if (event.getPacket() instanceof ExplosionS2CPacket) {
                    ExplosionS2CPacket explosion = (ExplosionS2CPacket) event.getPacket();
                    if (explosion.playerKnockback().map(v -> v.x != 0.0 || v.y != 0.0 || v.z != 0.0).orElse(false)) {
                        Myau.delayManager.delay(DelayModules.BED_NUKER);
                        Myau.delayManager.delayedPacket.offer(explosion);
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled()) {
            if (this.isReady() || this.targetBed != null && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            if (this.isReady()) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isEnabled()) {
            if (this.isReady() || this.targetBed != null && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (this.isEnabled()) {
            if (this.savedSlot != -1) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onDisabled() {
        this.resetBreaking();
        this.savedSlot = -1;
        Myau.delayManager.setDelayState(false, DelayModules.BED_NUKER);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
