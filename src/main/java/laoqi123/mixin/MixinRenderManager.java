package laoqi123.mixin;

import laoqi123.management.RotationState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRenderDispatcher.class, priority = 9999)
public abstract class MixinRenderManager {
    @Unique
    private float _prevRenderYawOffset;
    @Unique
    private float _renderYawOffset;
    @Unique
    private float _prevRotationYawHead;
    @Unique
    private float _rotationYawHead;
    @Unique
    private float _prevRotationPitch;
    @Unique
    private float _rotationPitch;

    @Inject(
            method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = {@At("HEAD")}
    )
    private void renderEntityStatic(Entity entity, double double2, double double3, double double4, float float5, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo callbackInfo) {
        if (entity instanceof ClientPlayerEntity && RotationState.isRotated(1)) {
            ClientPlayerEntity entityPlayerSP = (ClientPlayerEntity) entity;
            this._prevRenderYawOffset = entityPlayerSP.prevBodyYaw;
            this._renderYawOffset = entityPlayerSP.bodyYaw;
            this._prevRotationYawHead = entityPlayerSP.prevHeadYaw;
            this._rotationYawHead = entityPlayerSP.headYaw;
            this._prevRotationPitch = entityPlayerSP.prevPitch;
            this._rotationPitch = entityPlayerSP.getPitch();
            entityPlayerSP.prevBodyYaw = RotationState.getPrevRenderYawOffset();
            entityPlayerSP.bodyYaw = RotationState.getRenderYawOffset();
            entityPlayerSP.prevHeadYaw = RotationState.getPrevRotationYawHead();
            entityPlayerSP.headYaw = RotationState.getRotationYawHead();
            entityPlayerSP.prevPitch = RotationState.getPrevRotationPitch();
            entityPlayerSP.setPitch(RotationState.getRotationPitch());
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = {@At("RETURN")}
    )
    private void renderEntityStaticPost(Entity entity, double double2, double double3, double double4, float float5, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo callbackInfo) {
        if (entity instanceof ClientPlayerEntity && RotationState.isRotated(1)) {
            ClientPlayerEntity entityPlayerSP = (ClientPlayerEntity) entity;
            entityPlayerSP.prevBodyYaw = this._prevRenderYawOffset;
            entityPlayerSP.bodyYaw = this._renderYawOffset;
            entityPlayerSP.prevHeadYaw = this._prevRotationYawHead;
            entityPlayerSP.headYaw = this._rotationYawHead;
            entityPlayerSP.prevPitch = this._prevRotationPitch;
            entityPlayerSP.setPitch(this._rotationPitch);
        }
    }
}
