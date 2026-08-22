package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.module.modules.Chams;
import laoqi123.module.modules.ViewClip;
import laoqi123.module.modules.Xray;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {ChunkOcclusionDataBuilder.class}, priority = 9999)
public abstract class MixinVisGraph {
    @Inject(
            method = {"markClosed"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void markClosed(BlockPos blockPos, CallbackInfo callbackInfo) {
        if (this.isOcclusionBypassActive()) {
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = {"build"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void build(CallbackInfoReturnable<ChunkOcclusionData> callbackInfoReturnable) {
        if (this.isOcclusionBypassActive()) {
            ChunkOcclusionData chunkOcclusionData = new ChunkOcclusionData();
            chunkOcclusionData.fill(true);
            callbackInfoReturnable.setReturnValue(chunkOcclusionData);
        }
    }

    private boolean isOcclusionBypassActive() {
        return Myau.moduleManager != null
                && (Myau.moduleManager.modules.get(Chams.class).isEnabled()
                        || Myau.moduleManager.modules.get(ViewClip.class).isEnabled()
                        || Myau.moduleManager.modules.get(Xray.class).isEnabled());
    }
}
