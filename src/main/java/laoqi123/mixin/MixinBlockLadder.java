package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.module.modules.Xray;
import net.minecraft.block.BlockState;
import net.minecraft.block.LadderBlock;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {RenderLayers.class}, priority = 9999)
public abstract class MixinBlockLadder {
    @Inject(
            method = {"getBlockLayer"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private static void getBlockLayer(BlockState state, CallbackInfoReturnable<RenderLayer> callbackInfoReturnable) {
        if (Myau.moduleManager != null) {
            Xray xray = (Xray) Myau.moduleManager.modules.get(Xray.class);
            if (xray.isEnabled() && state.getBlock() instanceof LadderBlock) {
                callbackInfoReturnable.setReturnValue(RenderLayer.getTranslucent());
            }
        }
    }
}
