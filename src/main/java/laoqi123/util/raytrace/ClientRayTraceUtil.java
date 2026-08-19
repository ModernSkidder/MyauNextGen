package laoqi123.util.raytrace;

import laoqi123.util.rotation.Rotation;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.ShortPlantBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

public final class ClientRayTraceUtil {
    private static final double EPSILON = 1.0E-7D;
    public static Vec3d eyePos = null;
    private static MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean didHitBlockFace(Rotation rotation, BlockPos targetPos, Direction expectedFace, boolean strict) {
        return didHitBlockFace(mc.player, rotation.getYaw(), rotation.getPitch(), targetPos, expectedFace, strict, ClientRayTraceUtil::isIgnoredBlock);
    }

    public static boolean didHitBlockFace(PlayerEntity player, float yaw, float pitch, BlockPos targetPos, Direction expectedFace, boolean strict) {
        return didHitBlockFace(player, yaw, pitch, targetPos, expectedFace, strict, ClientRayTraceUtil::isIgnoredBlock);
    }

    public static boolean didHitBlockFace(Rotation rotation, BlockPos targetPos, Direction expectedFace, boolean strict, Predicate<BlockState> ignorePredicate) {
        return didHitBlockFace(mc.player, rotation.getYaw(), rotation.getPitch(), targetPos, expectedFace, strict, ignorePredicate);
    }

    public static boolean didHitBlockFace(PlayerEntity player, float yaw, float pitch, BlockPos targetPos, Direction expectedFace, boolean strict, Predicate<BlockState> ignorePredicate) {
        if (player == null || expectedFace == null) {
            return false;
        }

        BlockHitResult result = getFacedBlock(yaw, pitch, ignorePredicate);
        if (result == null
                || targetPos.getX() != result.getBlockPos().getX()
                || targetPos.getY() != result.getBlockPos().getY()
                || targetPos.getZ() != result.getBlockPos().getZ()
                || (expectedFace != result.getSide() && strict)) {
            return false;
        }
        return true;
    }

    public static void updateEyePos() {
        if (mc.player == null) {
            return;
        }
        eyePos = mc.player.getEyePos();
    }

    @Nullable
    private static Direction getHitFace(Vec3d hitPos, Box box) {
        if (Math.abs(hitPos.x - box.minX) < EPSILON) return Direction.WEST;
        if (Math.abs(hitPos.x - box.maxX) < EPSILON) return Direction.EAST;
        if (Math.abs(hitPos.y - box.minY) < EPSILON) return Direction.DOWN;
        if (Math.abs(hitPos.y - box.maxY) < EPSILON) return Direction.UP;
        if (Math.abs(hitPos.z - box.minZ) < EPSILON) return Direction.NORTH;
        if (Math.abs(hitPos.z - box.maxZ) < EPSILON) return Direction.SOUTH;

        return null;
    }

    public static BlockHitResult getFacedBlock(float yaw, float pitch) {
        return getFacedBlock(yaw, pitch, ClientRayTraceUtil::isIgnoredBlock);
    }

    public static BlockHitResult getFacedBlock(float yaw, float pitch, Predicate<BlockState> ignorePredicate) {
        final MinecraftClient client = MinecraftClient.getInstance();
        final double reachDistance = client.player.getBlockInteractionRange();
        if (yaw == 0.0f && pitch == 0.0f) {
            return null;
        }

        Vec3d startPos = eyePos;
        Vec3d direction = Vec3d.fromPolar(pitch, yaw);
        Vec3d endPos = startPos.add(direction.multiply(reachDistance));
        if (direction.x == 0) direction = new Vec3d(EPSILON, direction.y, direction.z);
        if (direction.y == 0) direction = new Vec3d(direction.x, EPSILON, direction.z);
        if (direction.z == 0) direction = new Vec3d(direction.x, direction.y, EPSILON);

        BlockPos currentPos = BlockPos.ofFloored(startPos);
        int stepX = (int) Math.signum(direction.x);
        int stepY = (int) Math.signum(direction.y);
        int stepZ = (int) Math.signum(direction.z);

        double nextBoundaryX = (stepX > 0) ? currentPos.getX() + 1 : currentPos.getX();
        double nextBoundaryY = (stepY > 0) ? currentPos.getY() + 1 : currentPos.getY();
        double nextBoundaryZ = (stepZ > 0) ? currentPos.getZ() + 1 : currentPos.getZ();

        double tMaxX = (nextBoundaryX - startPos.x) / direction.x;
        double tMaxY = (nextBoundaryY - startPos.y) / direction.y;
        double tMaxZ = (nextBoundaryZ - startPos.z) / direction.z;

        double tDeltaX = stepX / direction.x;
        double tDeltaY = stepY / direction.y;
        double tDeltaZ = stepZ / direction.z;

        final var world = mc.player.getWorld();
        Box box;
        while (startPos.distanceTo(currentPos.toCenterPos()) <= reachDistance) {
            if (!world.isAir(currentPos)) {
                BlockState state = world.getBlockState(currentPos);
                if (!ignorePredicate.test(state)) {
                    VoxelShape shape;
                    FluidState fluidState = world.getFluidState(currentPos);
                    if (fluidState != null && fluidState.isStill() && fluidState.getFluid() == Fluids.WATER) {
                        shape = VoxelShapes.fullCube();
                    } else {
                        shape = state.getCollisionShape(world, currentPos, ShapeContext.of(client.player));
                    }

                    if (!shape.isEmpty()) {
                        for (Box localBox : shape.getBoundingBoxes()) {
                            box = localBox.offset(currentPos);
                            Optional<Vec3d> intercept = box.raycast(startPos, endPos);

                            if (intercept.isPresent()) {
                                Vec3d hitVec = intercept.get();
                                Direction side = getHitFaceFromBox(hitVec, box);
                                boolean isInside = box.contains(startPos);
                                return new BlockHitResult(hitVec, side, currentPos, isInside);
                            }
                        }
                    }
                }
            }
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    currentPos = currentPos.add(stepX, 0, 0);
                    tMaxX += tDeltaX;
                } else {
                    currentPos = currentPos.add(0, 0, stepZ);
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    currentPos = currentPos.add(0, stepY, 0);
                    tMaxY += tDeltaY;
                } else {
                    currentPos = currentPos.add(0, 0, stepZ);
                    tMaxZ += tDeltaZ;
                }
            }
        }
        return null;
    }

    public static boolean isIgnoredBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof PlantBlock
                || block instanceof SnowBlock
                || block instanceof AirBlock
                || block instanceof ShortPlantBlock
                || block instanceof FluidBlock;
    }

    private static Direction getHitFaceFromBox(Vec3d hit, Box box) {
        final double eps = 1e-7;

        if (Math.abs(hit.x - box.minX) <= eps) return Direction.WEST;
        if (Math.abs(hit.x - box.maxX) <= eps) return Direction.EAST;
        if (Math.abs(hit.y - box.minY) <= eps) return Direction.DOWN;
        if (Math.abs(hit.y - box.maxY) <= eps) return Direction.UP;
        if (Math.abs(hit.z - box.minZ) <= eps) return Direction.NORTH;
        if (Math.abs(hit.z - box.maxZ) <= eps) return Direction.SOUTH;

        double dxMin = Math.abs(hit.x - box.minX);
        double dxMax = Math.abs(hit.x - box.maxX);
        double dyMin = Math.abs(hit.y - box.minY);
        double dyMax = Math.abs(hit.y - box.maxY);
        double dzMin = Math.abs(hit.z - box.minZ);
        double dzMax = Math.abs(hit.z - box.maxZ);

        double m = dxMin; Direction d = Direction.WEST;
        if (dxMax < m) { m = dxMax; d = Direction.EAST; }
        if (dyMin < m) { m = dyMin; d = Direction.DOWN; }
        if (dyMax < m) { m = dyMax; d = Direction.UP; }
        if (dzMin < m) { m = dzMin; d = Direction.NORTH; }
        if (dzMax < m) { /* m = dzMax; */ d = Direction.SOUTH; }

        return d;
    }

    public static BlockHitResult getFacedContainerBlock(float yaw, float pitch) {
        final MinecraftClient client = MinecraftClient.getInstance();
        final double reachDistance = client.player.getBlockInteractionRange();
        Vec3d startPos = eyePos;
        Vec3d direction = Vec3d.fromPolar(pitch, yaw);
        Vec3d endPos = startPos.add(direction.multiply(reachDistance));

        if (direction.x == 0) direction = new Vec3d(EPSILON, direction.y, direction.z);
        if (direction.y == 0) direction = new Vec3d(direction.x, EPSILON, direction.z);
        if (direction.z == 0) direction = new Vec3d(direction.x, direction.y, EPSILON);

        BlockPos currentPos = BlockPos.ofFloored(startPos);
        int stepX = (int) Math.signum(direction.x);
        int stepY = (int) Math.signum(direction.y);
        int stepZ = (int) Math.signum(direction.z);

        double nextBoundaryX = (stepX > 0) ? currentPos.getX() + 1 : currentPos.getX();
        double nextBoundaryY = (stepY > 0) ? currentPos.getY() + 1 : currentPos.getY();
        double nextBoundaryZ = (stepZ > 0) ? currentPos.getZ() + 1 : currentPos.getZ();

        double tMaxX = (nextBoundaryX - startPos.x) / direction.x;
        double tMaxY = (nextBoundaryY - startPos.y) / direction.y;
        double tMaxZ = (nextBoundaryZ - startPos.z) / direction.z;

        double tDeltaX = stepX / direction.x;
        double tDeltaY = stepY / direction.y;
        double tDeltaZ = stepZ / direction.z;

        while (startPos.distanceTo(currentPos.toCenterPos()) <= reachDistance) {
            if (!mc.player.getWorld().isAir(currentPos) && isContainerBlock(mc.player.getWorld().getBlockState(currentPos))) {
                Box currentBox = new Box(currentPos);
                Optional<Vec3d> intercept = currentBox.raycast(startPos, endPos);

                if (intercept.isPresent()) {
                    Vec3d hitVec = intercept.get();
                    Direction side = getHitFace(hitVec, currentBox);
                    boolean isInside = currentBox.contains(startPos);
                    return new BlockHitResult(hitVec, side, currentPos, isInside);
                }
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    currentPos = currentPos.add(stepX, 0, 0);
                    tMaxX += tDeltaX;
                } else {
                    currentPos = currentPos.add(0, 0, stepZ);
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    currentPos = currentPos.add(0, stepY, 0);
                    tMaxY += tDeltaY;
                } else {
                    currentPos = currentPos.add(0, 0, stepZ);
                    tMaxZ += tDeltaZ;
                }
            }
        }
        return null;
    }

    private static boolean isContainerBlock(BlockState state) {
        return state.getBlock() instanceof ChestBlock
                || state.getBlock() instanceof EnderChestBlock
                || state.getBlock() instanceof ShulkerBoxBlock;
    }

    public static double getDistance(float yaw, float pitch, Entity target) {
        Vec3d dir = Vec3d.fromPolar(pitch, yaw).normalize();
        Box targetBox = target.getBoundingBox();
        return intersectRayAabb(eyePos, dir, targetBox);
    }

    public static boolean didHitEntity(float yaw, float pitch, double range, Entity target) {
        return overBox(yaw, pitch, range, target.getBoundingBox());
    }

    public static boolean overBox(float yawDeg, float pitchDeg, double range, Box box) {
        if (box == null) {
            return false;
        }

        Vec3d start = eyePos;
        World world = MinecraftClient.getInstance().world;
        Vec3d dir = Vec3d.fromPolar(pitchDeg, yawDeg).normalize();
        if (dir.lengthSquared() < 1e-12) return false;
        double tBox = intersectRayAabb(start, dir, box);
        if (!Double.isFinite(tBox) || tBox < 0.0 || tBox > range) {
            return false;
        }
        if (isOccludedBefore(world, start, dir, tBox, range, mc.player)) {
            return false;
        }
        return true;
    }

    private static boolean isOccludedBefore(World world, Vec3d origin, Vec3d dir, double tStop, double range, Entity viewer) {
        int x = MathHelper.floor(origin.x);
        int y = MathHelper.floor(origin.y);
        int z = MathHelper.floor(origin.z);

        int stepX = dir.x > 0 ? 1 : (dir.x < 0 ? -1 : 0);
        int stepY = dir.y > 0 ? 1 : (dir.y < 0 ? -1 : 0);
        int stepZ = dir.z > 0 ? 1 : (dir.z < 0 ? -1 : 0);

        double tMaxX = nextBoundaryT(origin.x, dir.x, x, stepX);
        double tMaxY = nextBoundaryT(origin.y, dir.y, y, stepY);
        double tMaxZ = nextBoundaryT(origin.z, dir.z, z, stepZ);

        double tDeltaX = stepX != 0 ? 1.0 / Math.abs(dir.x) : Double.POSITIVE_INFINITY;
        double tDeltaY = stepY != 0 ? 1.0 / Math.abs(dir.y) : Double.POSITIVE_INFINITY;
        double tDeltaZ = stepZ != 0 ? 1.0 / Math.abs(dir.z) : Double.POSITIVE_INFINITY;
        double tLimit = Math.min(tStop, range);
        if (blockOccludesHere(world, origin, dir, new BlockPos(x, y, z), tStop, viewer)) {
            return true;
        }
        while (true) {
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    if (tMaxX > tLimit) break;
                    x += stepX;
                    if (blockOccludesHere(world, origin, dir, new BlockPos(x, y, z), tStop, viewer)) {
                        return true;
                    }
                    tMaxX += tDeltaX;
                } else {
                    if (tMaxZ > tLimit) break;
                    z += stepZ;
                    if (blockOccludesHere(world, origin, dir, new BlockPos(x, y, z), tStop, viewer)) {
                        return true;
                    }
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    if (tMaxY > tLimit) break;
                    y += stepY;
                    if (blockOccludesHere(world, origin, dir, new BlockPos(x, y, z), tStop, viewer)) {
                        return true;
                    }
                    tMaxY += tDeltaY;
                } else {
                    if (tMaxZ > tLimit) break;
                    z += stepZ;
                    if (blockOccludesHere(world, origin, dir, new BlockPos(x, y, z), tStop, viewer)) {
                        return true;
                    }
                    tMaxZ += tDeltaZ;
                }
            }
        }
        return false;
    }

    private static double nextBoundaryT(double o, double d, int cell, int step) {
        if (step == 0) return Double.POSITIVE_INFINITY;
        double boundary = step > 0 ? (cell + 1) : cell;
        return (boundary - o) / d;
    }

    private static boolean blockOccludesHere(World world, Vec3d origin, Vec3d dir, BlockPos pos, double tEntity, Entity viewer) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) return false;
        VoxelShape shape = state.getOutlineShape(world, pos, viewer != null ? ShapeContext.of(viewer) : ShapeContext.absent());
        if (shape.isEmpty()) return false;
        for (Box local : shape.getBoundingBoxes()) {
            Box box = local.offset(pos);
            double tBox = intersectRayAabb(origin, dir, box);
            if (Double.isFinite(tBox) && tBox >= 0.0 && tBox < tEntity) {
                return true;
            }
        }
        return false;
    }

    public static double intersectRayAabb(Vec3d o, Vec3d d, Box b) {
        double tMin = 0.0;
        double tMax = Double.POSITIVE_INFINITY;

        if (!axisSlab(o.x, d.x, b.minX, b.maxX, Holder.tmp)) return Double.POSITIVE_INFINITY;
        tMin = Math.max(tMin, Holder.tmp[0]); tMax = Math.min(tMax, Holder.tmp[1]);
        if (tMax < tMin) return Double.POSITIVE_INFINITY;

        if (!axisSlab(o.y, d.y, b.minY, b.maxY, Holder.tmp)) return Double.POSITIVE_INFINITY;
        tMin = Math.max(tMin, Holder.tmp[0]); tMax = Math.min(tMax, Holder.tmp[1]);
        if (tMax < tMin) return Double.POSITIVE_INFINITY;

        if (!axisSlab(o.z, d.z, b.minZ, b.maxZ, Holder.tmp)) return Double.POSITIVE_INFINITY;
        tMin = Math.max(tMin, Holder.tmp[0]); tMax = Math.min(tMax, Holder.tmp[1]);
        if (tMax < tMin) return Double.POSITIVE_INFINITY;

        return tMin;
    }

    private static boolean axisSlab(double o, double d, double min, double max, double[] out) {
        if (Math.abs(d) < 1e-12) {
            if (o < min || o > max) return false;
            out[0] = Double.NEGATIVE_INFINITY;
            out[1] = Double.POSITIVE_INFINITY;
            return true;
        } else {
            double inv = 1.0 / d;
            double t1 = (min - o) * inv;
            double t2 = (max - o) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            out[0] = t1;
            out[1] = t2;
            return true;
        }
    }

    private static final class Holder {
        static final double[] tmp = new double[2];
    }
}