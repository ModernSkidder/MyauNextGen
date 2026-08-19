package laoqi123.module.modules.misc;

import laoqi123.module.Module;
import laoqi123.property.properties.BooleanProperty;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

public class AntiObbyTrap extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanProperty setAir = new BooleanProperty("set-air", true);

    public AntiObbyTrap() {
        super("AntiObbyTrap", false);
    }

    public boolean isInsideBlock(BlockView world, BlockPos blockPos) {
        BlockState blockState = world.getBlockState(blockPos);
        if (blockState.isSolid() && blockState.isFullCube(world, blockPos)) {
            Vec3d hitVec = new Vec3d(mc.player.getX(), mc.player.getY() + (double) mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
            return blockState.getCollisionShape(world, blockPos, ShapeContext.of(mc.player)).getBoundingBox().contains(hitVec);
        } else {
            return false;
        }
    }
}
