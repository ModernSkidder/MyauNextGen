package laoqi123.module.modules.player;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.LoadWorldEvent;
import laoqi123.event.impl.MoveInputEvent;
import laoqi123.event.impl.PacketEvent;
import laoqi123.event.impl.UpdateEvent;
import laoqi123.module.Module;
import laoqi123.module.modules.combat.KillAura;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.FloatValue;
import laoqi123.value.properties.ModeValue;
import laoqi123.util.BlockUtil;
import laoqi123.util.MoveUtil;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ChestAura extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final DecimalFormat df = new DecimalFormat("0.0");

    public final FloatValue range = new FloatValue("Range", 4.0f, 1.0f, 6.0f);
    public final BooleanValue throughWalls = new BooleanValue("Through Walls", true);
    public final ModeValue moveFix = new ModeValue("Move Fix", 1, new String[]{"None", "Silent", "Strict"});

    private final List<BlockPos> openedChests = new ArrayList<>();
    private ChestBlockEntity targetChest;
    private float[] rotations;
    private boolean isRotating;
    private boolean scaffoldWasEnabled = false;

    public ChestAura() {
        super("ChestAura", false);
    }

    @Override
    public void onEnabled() {
        Scaffold scaffold = (Scaffold) Myau.moduleManager.getModule(Scaffold.class);
        if (scaffold != null && scaffold.isEnabled()) {
            scaffoldWasEnabled = true;
            scaffold.setEnabled(false);
        }
        openedChests.clear();
    }

    @Override
    public void onDisabled() {
        if (scaffoldWasEnabled) {
            Scaffold scaffold = (Scaffold) Myau.moduleManager.getModule(Scaffold.class);
            if (scaffold != null) {
                scaffold.setEnabled(true);
            }
            scaffoldWasEnabled = false;
        }
        targetChest = null;
        isRotating = false;
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        openedChests.clear();
        scaffoldWasEnabled = false;
    }

    private void addOpenedChest(BlockPos pos) {
        if (!openedChests.contains(pos)) {
            openedChests.add(pos);
        }
        Block block = mc.world.getBlockState(pos).getBlock();
        if (block instanceof ChestBlock) {
            for (Direction facing : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                BlockPos neighbor = pos.offset(facing);
                if (mc.world.getBlockState(neighbor).getBlock() == block) {
                    if (!openedChests.contains(neighbor)) {
                        openedChests.add(neighbor);
                    }
                }
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{df.format(range.getValue())};
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!isEnabled()) return;
        if (event.getPacket() instanceof BlockEventS2CPacket) {
            BlockEventS2CPacket packet = (BlockEventS2CPacket) event.getPacket();
            if (packet.getData() == 1) {
                addOpenedChest(packet.getPos());
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled()) return;
        if (event.getType() != EventType.PRE) return;

        KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.getTarget() != null) {
            targetChest = null;
            isRotating = false;
            return;
        }

        if (mc.currentScreen instanceof HandledScreen) {
            targetChest = null;
            isRotating = false;
            return;
        }

        for (BlockEntity blockEntity : BlockUtil.getBlockEntities()) {
            if (blockEntity instanceof ChestBlockEntity) {
                ChestBlockEntity chest = (ChestBlockEntity) blockEntity;
                if (ChestBlockEntity.getPlayersLookingInChestCount(mc.world, chest.getPos()) > 0) {
                    addOpenedChest(chest.getPos());
                }
            }
        }

        targetChest = getClosestChest();
        isRotating = false;

        if (targetChest != null) {
            double x = targetChest.getPos().getX() + 0.5 - mc.player.getX();
            double y = targetChest.getPos().getY() + 0.5 - mc.player.getY() - mc.player.getStandingEyeHeight();
            double z = targetChest.getPos().getZ() + 0.5 - mc.player.getZ();
            double dist = Math.sqrt(x * x + z * z);

            float yaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI) - 90.0f;
            float pitch = (float) -(Math.atan2(y, dist) * 180.0 / Math.PI);

            rotations = new float[]{yaw, pitch};

            event.setRotation(rotations[0], rotations[1], 1);
            mc.player.setHeadYaw(rotations[0]);
            mc.player.setBodyYaw(rotations[0]);
            isRotating = true;

            if (this.moveFix.getValue() != 0) {
                event.setPervRotation(rotations[0], 1);
            }

            BlockPos pos = targetChest.getPos();
            ActionResult result = mc.interactionManager.interactBlock(
                    mc.player,
                    Hand.MAIN_HAND,
                    new BlockHitResult(
                            new Vec3d(pos.getX(), pos.getY(), pos.getZ()),
                            Direction.UP,
                            pos,
                            false
                    )
            );
            if (result != ActionResult.PASS) {
                mc.player.swingHand(Hand.MAIN_HAND);
                addOpenedChest(pos);
            }
        }
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (!isEnabled()) return;

        KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.getTarget() != null) return;

        if (isRotating && targetChest != null) {
            if (this.moveFix.getValue() == 1 && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(rotations[0]);
            }
        }
    }

    private ChestBlockEntity getClosestChest() {
        List<ChestBlockEntity> chests = StreamSupport.stream(BlockUtil.getBlockEntities().spliterator(), false)
                .filter(e -> e instanceof ChestBlockEntity)
                .map(e -> (ChestBlockEntity) e)
                .filter(e -> !openedChests.contains(e.getPos()))
                .filter(e -> mc.player.squaredDistanceTo(
                        e.getPos().getX() + 0.5,
                        e.getPos().getY() + 0.5,
                        e.getPos().getZ() + 0.5) <= range.getValue() * range.getValue())
                .filter(e -> throughWalls.getValue() || mc.player.canSee(
                        new ItemEntity(mc.world, e.getPos().getX(), e.getPos().getY(), e.getPos().getZ(), new ItemStack(Items.STICK))))
                .sorted(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(
                        e.getPos().getX() + 0.5,
                        e.getPos().getY() + 0.5,
                        e.getPos().getZ() + 0.5)))
                .collect(Collectors.toList());

        return chests.isEmpty() ? null : chests.get(0);
    }
}
