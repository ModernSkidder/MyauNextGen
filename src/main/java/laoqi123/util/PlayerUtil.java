package laoqi123.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
public class PlayerUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean isJumping() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.options.jumpKey);
    }

    public static boolean isSneaking() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.options.sneakKey);
    }

    public static boolean isMovingLeft() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.options.leftKey);
    }

    public static boolean isMovingRight() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.options.rightKey);
    }

    public static boolean isAttacking() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.options.attackKey);
    }

    public static boolean isUsingItem() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.options.useKey);
    }

    public static boolean canFly(float fallThreshold) {
        if (!mc.player.getAbilities().allowFlying && !mc.player.getAbilities().invulnerable) {
            StatusEffectInstance jumpEffect = mc.player.getStatusEffect(StatusEffects.JUMP_BOOST);
            float jumpBoost = jumpEffect != null ? (float) (jumpEffect.getAmplifier() + 1) : 0.0F;
            float fallDistance = mc.player.fallDistance;
            if (mc.player.getVelocity().y < -0.67 || !isAirBelow()) {
                fallDistance -= (float) mc.player.getVelocity().y;
            }
            return MathHelper.ceil(fallDistance - fallThreshold - jumpBoost) > 0;
        } else {
            return false;
        }
    }

    public static boolean canFly(int checkHeight) {
        if (!mc.player.getAbilities().allowFlying && !mc.player.getAbilities().invulnerable) {
            int playerY = MathHelper.floor(mc.player.getY());
            for (int offset = 0; offset <= checkHeight; ++offset) {
                int currentY = playerY - offset;
                if (currentY < 0) {
                    break;
                }
                Block block = mc.world.getBlockState(new BlockPos(MathHelper.floor(mc.player.getX()), currentY, MathHelper.floor(mc.player.getZ()))).getBlock();
                if (block != Blocks.AIR) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public static boolean isInWater() {
        return checkInWater(mc.player.getBoundingBox().expand(-1.0E-6, 0.0, -1.0E-6));
    }

    public static boolean checkInWater(Box boundingBox) {
        if (!mc.player.isTouchingWater() && !mc.player.isInLava()) {
            int minY = MathHelper.floor(boundingBox.minY);
            if (minY < 0) {
                return true;
            } else {
                int minX = MathHelper.floor(boundingBox.minX);
                int maxX = MathHelper.floor(boundingBox.maxX + 1.0);
                int minZ = MathHelper.floor(boundingBox.minZ);
                int maxZ = MathHelper.floor(boundingBox.maxZ + 1.0);
                for (int x = minX; x < maxX; ++x) {
                    for (int z = minZ; z < maxZ; ++z) {
                        for (int y = minY; y >= 0; --y) {
                            if (!BlockUtil.isReplaceable(new BlockPos(x, y, z))) {
                                return false;
                            }
                        }
                    }
                }
                return true;
            }
        } else {
            return false;
        }
    }

    public static boolean canMove(double x, double z) {
        return PlayerUtil.canMove(x, z, -1.0);
    }

    public static boolean canMove(double x, double z, double y) {
        Box boundingBox = PlayerUtil.mc.player.getBoundingBox().offset(x, y, z);
        return PlayerUtil.mc.world.isSpaceEmpty(boundingBox);
    }

    public static boolean isAirBelow() {
        Box axisAlignedBB = PlayerUtil.mc.player.getBoundingBox().offset(0.0, -1.0, 0.0);
        return !PlayerUtil.mc.world.isSpaceEmpty(axisAlignedBB);
    }

    public static boolean isAirAbove() {
        Box axisAlignedBB = PlayerUtil.mc.player.getBoundingBox().offset(0.0, 1.0, 0.0);
        return !PlayerUtil.mc.world.isSpaceEmpty(axisAlignedBB);
    }

    public static boolean canReach(BlockPos blockPos, double reach) {
        return PlayerUtil.isBlockWithinReach(blockPos, PlayerUtil.mc.player.getX(), PlayerUtil.mc.player.getY() + (double) PlayerUtil.mc.player.getEyeHeight(PlayerUtil.mc.player.getPose()), PlayerUtil.mc.player.getZ(), reach);
    }

    public static boolean isBlockWithinReach(BlockPos blockPos, double x, double y, double z, double reach) {
        return blockPos.getSquaredDistance(x, y, z) < Math.pow(reach, 2.0);
    }

    public static void attackEntity(Entity target) {
        if (mc.interactionManager != null) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        }
    }

    public static boolean isMoving() {
        return mc.player != null
                && (mc.player.input.movementForward != 0.0F || mc.player.input.movementSideways != 0.0F);
    }

    public static int getMoveSpeedEffectAmplifier() {
        if (mc.player == null) {
            return 0;
        }
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            return mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
        }
        return 0;
    }
}
