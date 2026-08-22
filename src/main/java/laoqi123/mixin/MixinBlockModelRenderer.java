package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.module.modules.BedESP;
import laoqi123.module.modules.Xray;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockModelRenderer.class, priority = 9999)
public abstract class MixinBlockModelRenderer {
    @Inject(
            method = "render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;JI)V",
            at = {@At("HEAD")}
    )
    private void renderBlock(
            BlockRenderView world,
            BakedModel model,
            BlockState state,
            BlockPos pos,
            MatrixStack matrices,
            VertexConsumer vertexConsumer,
            boolean cull,
            Random random,
            long seed,
            int overlay,
            CallbackInfo callbackInfo
    ) {
        if (Myau.moduleManager != null) {
            BedESP bedESP = (BedESP) Myau.moduleManager.modules.get(BedESP.class);
            if (bedESP.isEnabled() && state.getBlock() instanceof BedBlock && state.get(BedBlock.PART) == BedPart.HEAD) {
                bedESP.beds.add(pos.toImmutable());
            }
            Xray Xray = (Xray) Myau.moduleManager.modules.get(Xray.class);
            if (Xray.isEnabled() && Xray.isXrayBlock(state)) {
                if (Xray.checkBlock(pos)) {
                    Xray.trackedBlocks.add(pos.toImmutable());
                } else {
                    Xray.trackedBlocks.remove(pos);
                }
            }
        }
    }
}
