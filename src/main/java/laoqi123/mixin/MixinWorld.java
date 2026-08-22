package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.module.modules.AntiObbyTrap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockView.class)
public interface MixinWorld {
    @Inject(
            method = {"raycast(Lnet/minecraft/world/RaycastContext;)Lnet/minecraft/util/hit/BlockHitResult;"},
            at = {@At("HEAD")},
            cancellable = true
    )
    default void myauRaycast(RaycastContext context, CallbackInfoReturnable<BlockHitResult> callbackInfoReturnable) {
        if (Myau.moduleManager == null) {
            return;
        }
        AntiObbyTrap antiObbyTrap = (AntiObbyTrap) Myau.moduleManager.modules.get(AntiObbyTrap.class);
        if (!antiObbyTrap.isEnabled()) {
            return;
        }
        BlockView world = (BlockView) (Object) this;
        callbackInfoReturnable.setReturnValue(
                BlockView.<BlockHitResult, RaycastContext>raycast(
                        context.getStart(),
                        context.getEnd(),
                        context,
                        (innerContext, pos) -> {
                            BlockState blockState = world.getBlockState(pos);
                            FluidState fluidState = world.getFluidState(pos);
                            if (antiObbyTrap.isInsideBlock(world, pos)) {
                                if (antiObbyTrap.setAir.getValue() && world instanceof World) {
                                    ((World) world).setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                                }
                                blockState = Blocks.AIR.getDefaultState();
                                fluidState = Fluids.EMPTY.getDefaultState();
                            }
                            Vec3d start = innerContext.getStart();
                            Vec3d end = innerContext.getEnd();
                            VoxelShape blockShape = innerContext.getBlockShape(blockState, world, pos);
                            BlockHitResult blockHitResult = world.raycastBlock(start, end, pos, blockShape, blockState);
                            VoxelShape fluidShape = innerContext.getFluidShape(fluidState, world, pos);
                            BlockHitResult fluidHitResult = fluidShape.raycast(start, end, pos);
                            double blockDistance = blockHitResult == null
                                    ? Double.MAX_VALUE
                                    : innerContext.getStart().squaredDistanceTo(blockHitResult.getPos());
                            double fluidDistance = fluidHitResult == null
                                    ? Double.MAX_VALUE
                                    : innerContext.getStart().squaredDistanceTo(fluidHitResult.getPos());
                            return blockDistance <= fluidDistance ? blockHitResult : fluidHitResult;
                        },
                        innerContext -> {
                            Vec3d end = innerContext.getEnd();
                            Vec3d start = innerContext.getStart().subtract(end);
                            return BlockHitResult.createMissed(end, Direction.getFacing(start.x, start.y, start.z), BlockPos.ofFloored(end));
                        }
                )
        );
    }
}
