package laoqi123.util;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

public class BlockUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static List<BlockEntity> getBlockEntities() {
        ArrayList<BlockEntity> list = new ArrayList<>();
        World world = BlockUtil.mc.world;
        if (world == null) return list;
        ClientChunkManager chunkManager = (ClientChunkManager) world.getChunkManager();
        for (long packedSection : chunkManager.getActiveSections()) {
            ChunkSectionPos sectionPos = ChunkSectionPos.from(packedSection);
            WorldChunk chunk = chunkManager.getWorldChunk(sectionPos.getSectionX(), sectionPos.getSectionZ(), false);
            if (chunk != null) {
                list.addAll(chunk.getBlockEntities().values());
            }
        }
        return list;
    }

    public static boolean isReplaceable(BlockPos blockPos) {
        return BlockUtil.isReplaceable(BlockUtil.mc.world.getBlockState(blockPos));
    }

    public static boolean isReplaceable(BlockState blockState) {
        if (!blockState.isReplaceable()) return false;
        if (!(blockState.getBlock() instanceof SnowBlock)) return true;
        VoxelShape shape = blockState.getOutlineShape(BlockUtil.mc.world, BlockPos.ORIGIN);
        return shape.isEmpty() || shape.getMax(Direction.Axis.Y) <= 0.125;
    }

    public static boolean isInteractable(BlockPos blockPos) {
        return BlockUtil.isInteractable(BlockUtil.mc.world.getBlockState(blockPos).getBlock());
    }

    public static boolean isInteractable(Block block) {
        if (block instanceof BlockWithEntity) return true;
        if (block instanceof CraftingTableBlock) return true;
        if (block instanceof AnvilBlock) return true;
        if (block instanceof BedBlock) return true;
        if (block instanceof DoorBlock) return true;
        if (block instanceof TrapdoorBlock) return true;
        if (block instanceof FenceGateBlock) return true;
        if (block instanceof FenceBlock) return true;
        if (block instanceof ButtonBlock) return true;
        if (block instanceof LeverBlock) return true;
        return block instanceof JukeboxBlock;
    }

    public static boolean isSolid(Block block) {
        if (block instanceof StairsBlock) return false;
        if (block instanceof SlabBlock) return false;
        if (block instanceof EndPortalFrameBlock) return false;
        if (block instanceof EndPortalBlock) return false;
        if (block instanceof VineBlock) return false;
        if (block instanceof PumpkinBlock) return false;
        if (block instanceof CactusBlock) return false;
        if (block instanceof PlantBlock) return false;
        if (block instanceof FallingBlock) return false;
        if (block instanceof CobwebBlock) return false;
        if (block instanceof PaneBlock) return false;
        if (block instanceof CarpetBlock) return false;
        if (block instanceof SnowBlock) return false;
        if (block instanceof FenceBlock) return false;
        if (block instanceof FenceGateBlock) return false;
        if (block instanceof WallBlock) return false;
        if (block instanceof LadderBlock) return false;
        if (block instanceof TorchBlock) return false;
        if (block instanceof RedstoneWireBlock) return false;
        if (block instanceof AbstractRedstoneGateBlock) return false;
        if (block instanceof AbstractPressurePlateBlock) return false;
        if (block instanceof TripwireBlock) return false;
        if (block instanceof TripwireHookBlock) return false;
        if (block instanceof AbstractRailBlock) return false;
        if (block instanceof SlimeBlock) return false;
        return !(block instanceof TntBlock);
    }

    public static Vec3d getHitVec(BlockPos blockPos, Direction enumFacing, float yaw, float pitch) {
        HitResult movingObjectPosition = RotationUtil.rayTrace(yaw, pitch, 4.5, 1.0f);
        if (movingObjectPosition != null) {
            if (movingObjectPosition.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) movingObjectPosition;
                if (blockHit.getBlockPos().equals(blockPos)) {
                    if (blockHit.getSide() == enumFacing) {
                        return blockHit.getPos();
                    }
                }
            }
        }
        return BlockUtil.getClickVec(blockPos, enumFacing);
    }

    public static Vec3d getClickVec(BlockPos blockPos, Direction enumFacing) {
        BlockState blockState = BlockUtil.mc.world.getBlockState(blockPos);
        VoxelShape shape = blockState.getOutlineShape(BlockUtil.mc.world, blockPos);
        double minX = shape.isEmpty() ? 0.0 : shape.getMin(Direction.Axis.X);
        double maxX = shape.isEmpty() ? 1.0 : shape.getMax(Direction.Axis.X);
        double minY = shape.isEmpty() ? 0.0 : shape.getMin(Direction.Axis.Y);
        double maxY = shape.isEmpty() ? 1.0 : shape.getMax(Direction.Axis.Y);
        double minZ = shape.isEmpty() ? 0.0 : shape.getMin(Direction.Axis.Z);
        double maxZ = shape.isEmpty() ? 1.0 : shape.getMax(Direction.Axis.Z);
        Vec3d vec3d = new Vec3d(blockPos.getX() + Math.min(Math.max(RandomUtil.nextDouble(0.0, 1.0), minX), maxX), blockPos.getY() + Math.min(Math.max(RandomUtil.nextDouble(0.0, 1.0), minY), maxY), blockPos.getZ() + Math.min(Math.max(RandomUtil.nextDouble(0.0, 1.0), minZ), maxZ));
        switch (enumFacing) {
            default: {
                return new Vec3d(vec3d.x, blockPos.getY() + minY, vec3d.z);
            }
            case UP: {
                return new Vec3d(vec3d.x, blockPos.getY() + maxY, vec3d.z);
            }
            case NORTH: {
                return new Vec3d(vec3d.x, vec3d.y, blockPos.getZ() + minZ);
            }
            case EAST: {
                return new Vec3d(blockPos.getX() + maxX, vec3d.y, vec3d.z);
            }
            case SOUTH: {
                return new Vec3d(vec3d.x, vec3d.y, blockPos.getZ() + maxZ);
            }
            case WEST:
        }
        return new Vec3d(blockPos.getX() + minX, vec3d.y, vec3d.z);
    }
}
