package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.event.EventManager;
import laoqi123.event.types.EventType;
import laoqi123.events.RenderLivingEvent;
import laoqi123.module.modules.render.ESP;
import laoqi123.module.modules.render.NameTags;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        value = LivingEntityRenderer.class,
        priority = 9991
)
public abstract class MixinRendererLivingEntity {
    @Unique
    private LivingEntity _renderingEntity;

    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = {@At("HEAD")}
    )
    private void updateRenderState(LivingEntity livingEntity, LivingEntityRenderState livingEntityRenderState, float f, CallbackInfo callbackInfo) {
        this._renderingEntity = livingEntity;
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = {@At("HEAD")}
    )
    private void doRender(LivingEntityRenderState livingEntityRenderState, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo callbackInfo) {
        if (this._renderingEntity != null) {
            EventManager.call(new RenderLivingEvent(EventType.PRE, this._renderingEntity));
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = {@At("RETURN")}
    )
    private void postRender(LivingEntityRenderState livingEntityRenderState, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo callbackInfo) {
        if (this._renderingEntity != null) {
            EventManager.call(new RenderLivingEvent(EventType.POST, this._renderingEntity));
            this._renderingEntity = null;
        }
    }

    @Inject(
            method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z",
            at = {@At("HEAD")},
            cancellable = true
    )
    private void canRenderName(LivingEntity livingEntity, double d, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (Myau.moduleManager != null) {
            NameTags nameTags = (NameTags) Myau.moduleManager.modules.get(NameTags.class);
            if (nameTags.isEnabled() && nameTags.shouldRenderTags(livingEntity)) {
                callbackInfoReturnable.setReturnValue(false);
            } else {
                ESP esp = (ESP) Myau.moduleManager.modules.get(ESP.class);
                if (esp.isEnabled() && !esp.isOutlineEnabled()) {
                    callbackInfoReturnable.setReturnValue(false);
                }
            }
        }
    }
}
