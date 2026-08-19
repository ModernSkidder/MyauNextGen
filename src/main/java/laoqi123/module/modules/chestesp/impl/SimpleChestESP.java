package laoqi123.module.modules.chestesp.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import laoqi123.event.types.EventType;
import laoqi123.events.LoadWorldEvent;
import laoqi123.events.PacketEvent;
import laoqi123.events.Render3DEvent;
import laoqi123.module.modules.chestesp.ChestESPMode;
import laoqi123.util.BlockUtil;
import laoqi123.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimpleChestESP extends ChestESPMode {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Color CHEST_COLOR = new Color(0, 255, 0);
    private static final Color OPENED_CHEST_COLOR = new Color(255, 0, 0);

    private final List<BlockPos> openedChestPositions = new CopyOnWriteArrayList<>();
    private final List<Box> renderBoundingBoxes = new CopyOnWriteArrayList<>();

    @Override
    public void onLoadWorld(LoadWorldEvent event) {
        this.openedChestPositions.clear();
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE) return;
        if (!(event.getPacket() instanceof BlockEventS2CPacket packet)) return;
        if (packet.getType() != 1 || packet.getData() != 1) return;
        if (mc.world == null) return;
        Block block = packet.getBlock();
        if (block != Blocks.CHEST && block != Blocks.TRAPPED_CHEST) return;
        this.addOpenedChest(packet.getPos());
    }

    private void addOpenedChest(BlockPos pos) {
        if (this.openedChestPositions.contains(pos)) return;
        this.openedChestPositions.add(pos);
        if (mc.world == null) return;
        Block block = mc.world.getBlockState(pos).getBlock();
        if (block instanceof ChestBlock) {
            for (Direction facing : Direction.Type.HORIZONTAL) {
                BlockPos neighbor = pos.offset(facing);
                if (mc.world.getBlockState(neighbor).getBlock() == block && !this.openedChestPositions.contains(neighbor)) {
                    this.openedChestPositions.add(neighbor);
                }
            }
        }
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (!this.parent.isEnabled() || mc.world == null) return;

        this.renderBoundingBoxes.clear();
        for (BlockEntity blockEntity : BlockUtil.getBlockEntities()) {
            if (!(blockEntity instanceof ChestBlockEntity chestBlockEntity)) continue;
            Box aabb = this.getChestAabb(chestBlockEntity);
            if (aabb != null) {
                this.renderBoundingBoxes.add(aabb);
            }
        }

        RenderUtil.enableRenderState();
        for (Box aabb : this.renderBoundingBoxes) {
            BlockPos blockPos = BlockPos.ofFloored(aabb.minX, aabb.minY, aabb.minZ);
            Color color = this.openedChestPositions.contains(blockPos) ? OPENED_CHEST_COLOR : CHEST_COLOR;
            this.drawFilledBoxAlpha(aabb, color.getRed(), color.getGreen(), color.getBlue(), 64);
        }
        RenderUtil.disableRenderState();
    }

    private Box getChestAabb(ChestBlockEntity chestBlockEntity) {
        if (mc.world == null) return null;
        BlockPos pos = chestBlockEntity.getPos();
        BlockState state = null;
        if (mc.world.getBlockState(pos).getBlock() instanceof ChestBlock) {
            state = mc.world.getBlockState(pos);
        }
        if (state == null || !state.contains(ChestBlock.CHEST_TYPE)) {
            return null;
        }
        ChestType chestType = state.get(ChestBlock.CHEST_TYPE);
        if (chestType == ChestType.LEFT) {
            return null;
        }
        Box aabb = new Box(pos);
        if (chestType != ChestType.SINGLE) {
            Block block = state.getBlock();
            for (Direction facing : Direction.Type.HORIZONTAL) {
                BlockPos neighbor = pos.offset(facing);
                if (mc.world.getBlockState(neighbor).getBlock() == block) {
                    aabb = aabb.union(new Box(neighbor));
                }
            }
        }
        return aabb;
    }

    private void drawFilledBoxAlpha(Box bb, int red, int green, int blue, int alpha) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        VertexRendering.drawFilledBox(new MatrixStack(), bufferBuilder, bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, red, green, blue, alpha);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
}