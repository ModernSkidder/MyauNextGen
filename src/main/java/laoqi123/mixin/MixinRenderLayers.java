package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.module.modules.render.Xray;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderLayers.class)
public abstract class MixinRenderLayers {
    @Inject(
            method = {"getBlockLayer(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/render/RenderLayer;"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private static void getBlockLayer(BlockState state, CallbackInfoReturnable<RenderLayer> callbackInfoReturnable) {
        if (Myau.moduleManager != null) {
            Xray xray = (Xray) Myau.moduleManager.modules.get(Xray.class);
            if (xray.isEnabled() && (!xray.shouldRenderSide(state.getBlock()) || xray.mode.getValue() == 0 && !xray.isXrayBlock(state))) {
                callbackInfoReturnable.setReturnValue(RenderLayer.getTranslucent());
            }
        }
    }
}
