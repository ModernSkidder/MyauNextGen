package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.module.modules.Xray;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class MixinBlock {
    @Inject(
            method = {"shouldDrawSide(Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;)Z"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private static void shouldDrawSide(
            BlockState state, BlockState otherState, Direction side, CallbackInfoReturnable<Boolean> callbackInfoReturnable
    ) {
        if (Myau.moduleManager != null) {
            Xray xray = (Xray) Myau.moduleManager.modules.get(Xray.class);
            if (xray.isEnabled() && xray.mode.getValue() == 1 && xray.shouldRenderSide(state.getBlock()) && xray.checkBlock(otherState)) {
                callbackInfoReturnable.setReturnValue(true);
            }
        }
    }
}
