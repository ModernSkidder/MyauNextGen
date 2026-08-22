package laoqi123.module.modules;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.LoadWorldEvent;
import laoqi123.events.PacketEvent;
import laoqi123.events.Render3DEvent;
import laoqi123.mixin.MinecraftClientAccessor;
import laoqi123.module.Module;
import laoqi123.property.properties.*;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.ModeProperty;
import laoqi123.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.CopyOnWriteArraySet;

public class Xray extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final LinkedHashSet<Block> xrayBlocks;
    private static final LinkedHashSet<Vec3i> caveOffsetsSmall;
    private static final LinkedHashSet<Vec3i> caveOffsetsLarge;
    public final CopyOnWriteArraySet<BlockPos> trackedBlocks = new CopyOnWriteArraySet<>();
    public final CopyOnWriteArraySet<BlockPos> pendingBlocks = new CopyOnWriteArraySet<>();
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"SOFT", "FULL"});
    public final PercentProperty opacity = new PercentProperty("opacity", 50);
    public final IntProperty range = new IntProperty("range", 64, 16, 512);
    public final BooleanProperty cavesOnly = new BooleanProperty("caves-only", true);
    public final IntProperty caveRadius = new IntProperty("caves-radius", 2, 1, 2);
    public final BooleanProperty diamonds = new BooleanProperty("diamonds", true);
    public final BooleanProperty diamondTracers = new BooleanProperty("diamonds-tracers", true);
    public final BooleanProperty gold = new BooleanProperty("gold", true);
    public final BooleanProperty goldTracers = new BooleanProperty("gold-tracers", true);
    public final BooleanProperty iron = new BooleanProperty("iron", false);
    public final BooleanProperty ironTracers = new BooleanProperty("iron-tracers", false);
    public final BooleanProperty coal = new BooleanProperty("coal", false);
    public final BooleanProperty coalTracers = new BooleanProperty("coal-tracers", false);
    public final BooleanProperty redstone = new BooleanProperty("redstone", false);
    public final BooleanProperty redStoneTracers = new BooleanProperty("redstone-tracers", false);
    public final BooleanProperty lapis = new BooleanProperty("lapis", false);
    public final BooleanProperty lapisTracers = new BooleanProperty("lapis-tracers", false);
    public final BooleanProperty emeralds = new BooleanProperty("emeralds", false);
    public final BooleanProperty emeraldsTracers = new BooleanProperty("emeralds-tracers", false);
    public final BooleanProperty spawners = new BooleanProperty("spawners", false);
    public final BooleanProperty spawnerTracers = new BooleanProperty("spawners-tracers", false);
    public final BooleanProperty canes = new BooleanProperty("canes", false);
    public final BooleanProperty canesTracers = new BooleanProperty("canes-tracers", false);
    public final BooleanProperty warts = new BooleanProperty("warts", false);
    public final BooleanProperty wartsTracers = new BooleanProperty("warts-tracers", false);

    private void renderOreHighlight(BlockPos blockPos, BlockState state, Vec3d viewVector) {
        if (mc.player != null
                && mc.player.squaredDistanceTo((double) blockPos.getX() + 0.5, (double) blockPos.getY() + 0.5, (double) blockPos.getZ() + 0.5)
                        <= (double) this.range.getValue() * (double) this.range.getValue()) {
            Color color = this.getOreColor(state);
            RenderUtil.drawBlockBoundingBox(blockPos, 1.0, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 1.5F);
            if (this.shouldDrawTracer(state)) {
                RenderUtil.drawLine3D(
                        viewVector,
                        (double) blockPos.getX() + 0.5,
                        (double) blockPos.getY() + 0.5,
                        (double) blockPos.getZ() + 0.5,
                        (float) color.getRed() / 255.0F,
                        (float) color.getGreen() / 255.0F,
                        (float) color.getBlue() / 255.0F,
                        1.0F,
                        1.5F
                );
            }
        }
    }

    private Color getOreColor(BlockState state) {
        if (state.isOf(Blocks.GOLD_ORE)) {
            return new Color(16777045);
        }
        if (state.isOf(Blocks.IRON_ORE)) {
            return new Color(16777215);
        }
        if (state.isOf(Blocks.COAL_ORE)) {
            return new Color(0);
        }
        if (state.isOf(Blocks.LAPIS_ORE)) {
            return new Color(5592575);
        }
        if (state.isOf(Blocks.SPAWNER)) {
            return new Color(16733695);
        }
        if (state.isOf(Blocks.DIAMOND_ORE)) {
            return new Color(5636095);
        }
        if (state.isOf(Blocks.REDSTONE_ORE) || state.isOf(Blocks.DEEPSLATE_REDSTONE_ORE)) {
            return new Color(16733525);
        }
        if (state.isOf(Blocks.SUGAR_CANE)) {
            return new Color(11206570);
        }
        if (state.isOf(Blocks.NETHER_WART)) {
            return new Color(11141120);
        }
        if (state.isOf(Blocks.EMERALD_ORE)) {
            return new Color(5635925);
        }
        return new Color(-1);
    }

    private boolean shouldDrawTracer(BlockState state) {
        if (state.isOf(Blocks.GOLD_ORE)) {
            return this.goldTracers.getValue();
        }
        if (state.isOf(Blocks.IRON_ORE)) {
            return this.ironTracers.getValue();
        }
        if (state.isOf(Blocks.COAL_ORE)) {
            return this.coalTracers.getValue();
        }
        if (state.isOf(Blocks.LAPIS_ORE)) {
            return this.lapisTracers.getValue();
        }
        if (state.isOf(Blocks.SPAWNER)) {
            return this.spawnerTracers.getValue();
        }
        if (state.isOf(Blocks.DIAMOND_ORE)) {
            return this.diamondTracers.getValue();
        }
        if (state.isOf(Blocks.REDSTONE_ORE) || state.isOf(Blocks.DEEPSLATE_REDSTONE_ORE)) {
            return this.redStoneTracers.getValue();
        }
        if (state.isOf(Blocks.SUGAR_CANE)) {
            return this.canesTracers.getValue();
        }
        if (state.isOf(Blocks.NETHER_WART)) {
            return this.wartsTracers.getValue();
        }
        if (state.isOf(Blocks.EMERALD_ORE)) {
            return this.emeraldsTracers.getValue();
        }
        return false;
    }

    private boolean isValidCaveBlock(BlockState state) {
        return state.getBlock() instanceof SpawnerBlock || !state.isOpaque() || state.emitsRedstonePower();
    }

    private boolean isValidCaveBlock(BlockPos pos) {
        if (mc.world == null || !mc.world.isChunkLoaded(pos)) {
            return false;
        }
        BlockState state = mc.world.getBlockState(pos);
        return state.getBlock() instanceof SpawnerBlock || !state.isFullCube(mc.world, pos) || !state.isOpaque() || state.emitsRedstonePower();
    }

    public Xray() {
        super("Xray", false);
    }

    public boolean shouldRenderSide(Block block) {
        return xrayBlocks.contains(block);
    }

    public boolean isXrayBlock(BlockState state) {
        if (state.isOf(Blocks.GOLD_ORE)) {
            return this.gold.getValue();
        }
        if (state.isOf(Blocks.IRON_ORE)) {
            return this.iron.getValue();
        }
        if (state.isOf(Blocks.COAL_ORE)) {
            return this.coal.getValue();
        }
        if (state.isOf(Blocks.LAPIS_ORE)) {
            return this.lapis.getValue();
        }
        if (state.isOf(Blocks.SPAWNER)) {
            return this.spawners.getValue();
        }
        if (state.isOf(Blocks.DIAMOND_ORE)) {
            return this.diamonds.getValue();
        }
        if (state.isOf(Blocks.REDSTONE_ORE) || state.isOf(Blocks.DEEPSLATE_REDSTONE_ORE)) {
            return this.redstone.getValue();
        }
        if (state.isOf(Blocks.SUGAR_CANE)) {
            return this.canes.getValue();
        }
        if (state.isOf(Blocks.NETHER_WART)) {
            return this.warts.getValue();
        }
        if (state.isOf(Blocks.EMERALD_ORE)) {
            return this.emeralds.getValue();
        }
        return false;
    }

    public boolean checkBlock(BlockState state) {
        return !this.cavesOnly.getValue() || this.isValidCaveBlock(state);
    }

    public boolean checkBlock(BlockPos blockPos) {
        if (!this.cavesOnly.getValue()) {
            return true;
        } else {
            if (this.caveRadius.getValue() >= 2) {
                for (Vec3i vec3i : caveOffsetsLarge) {
                    if (this.isValidCaveBlock(blockPos.add(vec3i))) {
                        return true;
                    }
                }
            } else {
                for (Vec3i vec3i : caveOffsetsSmall) {
                    if (this.isValidCaveBlock(blockPos.add(vec3i))) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled()) {
            if (mc.player == null || mc.world == null) {
                return;
            }
            Entity entity = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
            float tickDelta = ((MinecraftClientAccessor) mc).getTimer().getTickDelta(false);
            Vec3d vec3;
            if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
                vec3 = new Vec3d(0.0, 0.0, 1.0)
                        .rotateX((float) (-Math.toRadians(RenderUtil.lerpFloat(entity.getPitch(), entity.prevPitch, tickDelta))))
                        .rotateY((float) (-Math.toRadians(RenderUtil.lerpFloat(entity.getYaw(), entity.prevYaw, tickDelta))));
            } else {
                vec3 = new Vec3d(0.0, 0.0, 0.0)
                        .rotateX((float) (-Math.toRadians(RenderUtil.lerpFloat(entity.getPitch(), entity.prevPitch, tickDelta))))
                        .rotateY((float) (-Math.toRadians(RenderUtil.lerpFloat(entity.getYaw(), entity.prevYaw, tickDelta))));
            }
            vec3 = new Vec3d(vec3.x, vec3.y + (double) entity.getEyeHeight(entity.getPose()), vec3.z);
            RenderUtil.enableRenderState();
            for (BlockPos blockPos : this.trackedBlocks) {
                if (this.pendingBlocks.contains(blockPos)) {
                    this.trackedBlocks.remove(blockPos);
                } else {
                    BlockState state = mc.world.getBlockState(blockPos);
                    if (this.isXrayBlock(state)) {
                        this.renderOreHighlight(blockPos, state, vec3);
                    } else {
                        this.trackedBlocks.remove(blockPos);
                    }
                }
            }
            for (BlockPos blockPos : this.pendingBlocks) {
                BlockState state = mc.world.getBlockState(blockPos);
                if (this.isXrayBlock(state)) {
                    this.renderOreHighlight(blockPos, state, vec3);
                } else {
                    this.pendingBlocks.remove(blockPos);
                }
            }
            RenderUtil.disableRenderState();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE) {
            if (event.getPacket() instanceof ChunkDeltaUpdateS2CPacket) {
                ChunkDeltaUpdateS2CPacket packet = (ChunkDeltaUpdateS2CPacket) event.getPacket();
                packet.visitUpdates((blockPos, blockState) -> {
                    if (this.isXrayBlock(blockState)) {
                        this.pendingBlocks.add(blockPos.toImmutable());
                    }
                });
            } else if (event.getPacket() instanceof BlockUpdateS2CPacket) {
                BlockUpdateS2CPacket packet = (BlockUpdateS2CPacket) event.getPacket();
                if (this.isXrayBlock(packet.getState())) {
                    this.pendingBlocks.add(packet.getPos());
                }
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.trackedBlocks.clear();
        this.pendingBlocks.clear();
    }

    @Override
    public void onEnabled() {
        if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }

    @Override
    public void onDisabled() {
        if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }

    @Override
    public void verifyValue(String mode) {
        this.trackedBlocks.clear();
        this.pendingBlocks.clear();
        if (this.isEnabled() && mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }

    static {
        xrayBlocks = new LinkedHashSet<>(
                Arrays.asList(
                        Blocks.DIAMOND_ORE,
                        Blocks.GOLD_ORE,
                        Blocks.IRON_ORE,
                        Blocks.COAL_ORE,
                        Blocks.REDSTONE_ORE,
                        Blocks.DEEPSLATE_REDSTONE_ORE,
                        Blocks.LAPIS_ORE,
                        Blocks.EMERALD_ORE,
                        Blocks.SPAWNER,
                        Blocks.SUGAR_CANE,
                        Blocks.NETHER_WART
                )
        );
        caveOffsetsSmall = new LinkedHashSet<>(
                Arrays.asList(new Vec3i(0, -1, 0), new Vec3i(1, 0, 0), new Vec3i(0, 0, -1), new Vec3i(0, 0, 1), new Vec3i(-1, 0, 0), new Vec3i(0, 1, 0))
        );
        caveOffsetsLarge = new LinkedHashSet<>(
                Arrays.asList(
                        new Vec3i(0, -2, 0),
                        new Vec3i(1, -1, 0),
                        new Vec3i(0, -1, -1),
                        new Vec3i(0, -1, 0),
                        new Vec3i(0, -1, 1),
                        new Vec3i(-1, -1, 0),
                        new Vec3i(2, 0, 0),
                        new Vec3i(0, 0, 2),
                        new Vec3i(0, 0, -2),
                        new Vec3i(-2, 0, 0),
                        new Vec3i(1, 0, -1),
                        new Vec3i(1, 0, 0),
                        new Vec3i(1, 0, 1),
                        new Vec3i(0, 0, -1),
                        new Vec3i(0, 0, 1),
                        new Vec3i(-1, 0, -1),
                        new Vec3i(-1, 0, 0),
                        new Vec3i(-1, 0, 1),
                        new Vec3i(1, 1, 0),
                        new Vec3i(0, 1, -1),
                        new Vec3i(0, 1, 0),
                        new Vec3i(0, 1, 1),
                        new Vec3i(-1, 1, 0),
                        new Vec3i(0, 2, 0)
                )
        );
    }
}
