package laoqi123.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.List;
import java.util.Optional;

public final class RayCastUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static RayCastResult rayCast(RotationUtil.RotationVec rotation, double distance) {
        return rayCast(rotation, distance, 0.0F);
    }

    public static boolean inView(Entity entity) {
        RotationUtil.RotationVec rotation = calculateRotationToEntity(entity);
        int renderDistance = 16 * mc.options.getViewDistance().getValue();
        if (!(entity.distanceTo(mc.player) > 100.0D) && entity instanceof PlayerEntity) {
            Box boundingBox = entity.getBoundingBox();
            for (double yOffset = 1.0D; yOffset >= -1.0D; yOffset -= 0.5D) {
                for (double xOffset = 1.0D; xOffset >= -1.0D; --xOffset) {
                    for (double zOffset = 1.0D; zOffset >= -1.0D; --zOffset) {
                        double scanX = entity.getX() + (boundingBox.maxX - boundingBox.minX) * xOffset;
                        double scanY = entity.getY() + (boundingBox.maxY - boundingBox.minY) * yOffset;
                        double scanZ = entity.getZ() + (boundingBox.maxZ - boundingBox.minZ) * zOffset;
                        RotationUtil.RotationVec scanRotation = calculateRotationTo(scanX, scanY, scanZ);
                        RayCastResult result = rayCast(scanRotation, renderDistance, 0.2F);
                        if (result != null && result.typeOfHit == RayCastResult.Type.ENTITY) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } else {
            RayCastResult result = rayCast(rotation, renderDistance, 0.3F);
            return result != null && result.typeOfHit == RayCastResult.Type.ENTITY;
        }
    }

    public static RayCastResult rayCast(RotationUtil.RotationVec rotation, double distance, float expandSize) {
        return rayCast(rotation, distance, expandSize, mc.player);
    }

    public static RayCastResult rayCast(RotationUtil.RotationVec rotation, double distance, float expandSize, Entity sourceEntity) {
        if (sourceEntity != null && mc.world != null) {
            float partialTicks = 1.0F;
            BlockHitResult blockHit = rayTraceCustom(sourceEntity, rotation.x, rotation.y, distance);
            double maxDistance = distance;
            Vec3d eyePos = sourceEntity.getEyePos();
            if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
                maxDistance = blockHit.getPos().distanceTo(eyePos);
            }

            Vec3d lookVec = getVectorForRotation(rotation.y, rotation.x);
            Vec3d endPos = eyePos.add(lookVec.x * distance, lookVec.y * distance, lookVec.z * distance);
            Entity hitEntity = null;
            Vec3d hitVec = null;
            List<Entity> entities = mc.world.getOtherEntities(sourceEntity, sourceEntity.getBoundingBox().expand(lookVec.x * distance, lookVec.y * distance, lookVec.z * distance).expand(1.0D, 1.0D, 1.0D), Entity::isCollidable);
            double currentDistance = maxDistance;

            for (Entity entity : entities) {
                float entityExpand = 0.3F + expandSize;
                Box expandedBB = entity.getBoundingBox().expand(entityExpand, entityExpand, entityExpand);
                Optional<Vec3d> entityHit = expandedBB.raycast(eyePos, endPos);
                if (expandedBB.contains(eyePos)) {
                    if (currentDistance >= 0.0D) {
                        hitEntity = entity;
                        hitVec = entityHit.orElse(eyePos);
                        currentDistance = 0.0D;
                    }
                } else if (entityHit.isPresent()) {
                    double distanceToHit = eyePos.distanceTo(entityHit.get());
                    if (distanceToHit < currentDistance || currentDistance == 0.0D) {
                        hitEntity = entity;
                        hitVec = entityHit.get();
                        currentDistance = distanceToHit;
                    }
                }
            }

            if (hitEntity != null && (currentDistance < maxDistance || blockHit == null)) {
                return new RayCastResult(hitEntity, hitVec);
            }

            if (blockHit != null) {
                return new RayCastResult(blockHit.getPos(), blockHit.getSide(), blockHit.getBlockPos());
            }
        }
        return null;
    }

    private static BlockHitResult rayTraceCustom(Entity entity, float yaw, float pitch, double distance) {
        Vec3d eyePos = entity.getEyePos();
        Vec3d lookVec = getVectorForRotation(pitch, yaw);
        Vec3d targetPos = eyePos.add(lookVec.x * distance, lookVec.y * distance, lookVec.z * distance);
        return entity.getWorld().raycast(new RaycastContext(eyePos, targetPos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, entity));
    }

    public static boolean overBlock(RotationUtil.RotationVec rotation, Direction side, net.minecraft.util.math.BlockPos pos, boolean checkSide) {
        RayCastResult hit = rayCast(rotation, 4.5D);
        if (hit != null && hit.hitVec != null) {
            return hit.getBlockPos() != null && hit.getBlockPos().equals(pos) && (!checkSide || hit.sideHit == side);
        } else {
            return false;
        }
    }

    public static RotationUtil.RotationVec calculateRotationToEntity(Entity entity) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d entityPos = new Vec3d(entity.getX(), entity.getY() + entity.getEyeHeight(entity.getPose()), entity.getZ());
        double deltaX = entityPos.x - eyePos.x;
        double deltaY = entityPos.y - eyePos.y;
        double deltaZ = entityPos.z - eyePos.z;
        double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0D / 3.141592653589793D) - 90.0F;
        float pitch = (float) (-(Math.atan2(deltaY, horizontalDist) * 180.0D / 3.141592653589793D));
        return new RotationUtil.RotationVec(yaw, pitch);
    }

    private static RotationUtil.RotationVec calculateRotationTo(double x, double y, double z) {
        Vec3d eyePos = mc.player.getEyePos();
        double deltaX = x - eyePos.x;
        double deltaY = y - eyePos.y;
        double deltaZ = z - eyePos.z;
        double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0D / 3.141592653589793D) - 90.0F;
        float pitch = (float) (-(Math.atan2(deltaY, horizontalDist) * 180.0D / 3.141592653589793D));
        return new RotationUtil.RotationVec(yaw, pitch);
    }

    private static Vec3d getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - 3.1415927F);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - 3.1415927F);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3d(f1 * f2, f3, f * f2);
    }

    public static class RayCastResult {
        public Type typeOfHit;
        public Vec3d hitVec;
        public Entity entityHit;
        public Direction sideHit;
        private net.minecraft.util.math.BlockPos blockPos;

        public RayCastResult(Vec3d hitVec, Type type) {
            this.hitVec = hitVec;
            this.typeOfHit = type;
        }

        public RayCastResult(Entity entity, Vec3d hitVec) {
            this.entityHit = entity;
            this.hitVec = hitVec;
            this.typeOfHit = Type.ENTITY;
        }

        public RayCastResult(Vec3d hitVec, Direction sideHit, net.minecraft.util.math.BlockPos blockPos) {
            this.hitVec = hitVec;
            this.sideHit = sideHit;
            this.blockPos = blockPos;
            this.typeOfHit = Type.BLOCK;
        }

        public RayCastResult(Vec3d hitVec, Direction sideHit, Type type) {
            this.hitVec = hitVec;
            this.sideHit = sideHit;
            this.typeOfHit = type;
        }

        public net.minecraft.util.math.BlockPos getBlockPos() {
            return this.blockPos;
        }

        public void setBlockPos(net.minecraft.util.math.BlockPos blockPos) {
            this.blockPos = blockPos;
        }

        public static enum Type {
            MISS,
            BLOCK,
            ENTITY;
        }
    }
}
