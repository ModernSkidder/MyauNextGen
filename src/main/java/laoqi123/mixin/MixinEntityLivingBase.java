package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.event.EventManager;
import laoqi123.events.StrafeEvent;
import laoqi123.management.RotationState;
import laoqi123.module.modules.movement.Jesus;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LivingEntity.class, priority = 9999)
public abstract class MixinEntityLivingBase extends MixinEntity {
    @Redirect(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float jump(LivingEntity entity) {
        return (Object) this instanceof ClientPlayerEntity && RotationState.isActived()
                ? RotationState.getSmoothedYaw()
                : entity.getYaw();
    }

    @Redirect(
            method = "applyMovementInput",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;updateVelocity(FLnet/minecraft/util/math/Vec3d;)V")
    )
    private void applyMovementInput(LivingEntity entity, float speed, Vec3d movementInput) {
        if ((Object) this instanceof ClientPlayerEntity) {
            StrafeEvent event = new StrafeEvent((float) movementInput.x, (float) movementInput.z, speed);
            EventManager.call(event);
            Vec3d input = new Vec3d(event.getStrafe(), movementInput.y, event.getForward());
            boolean actived = RotationState.isActived();
            float yaw = ((Entity) (Object) this).getYaw();
            if (actived) {
                ((Entity) (Object) this).setYaw(RotationState.getSmoothedYaw());
            }
            entity.updateVelocity(event.getFriction(), input);
            if (actived) {
                ((Entity) (Object) this).setYaw(yaw);
            }
        } else {
            entity.updateVelocity(speed, movementInput);
        }
    }

    @ModifyVariable(method = "travelInFluid", name = "h", at = @At("STORE"), ordinal = 0)
    private float travelInFluid(float f) {
        if ((Object) this instanceof ClientPlayerEntity && f == (float) ((LivingEntity) (Object) this).getAttributeValue(EntityAttributes.WATER_MOVEMENT_EFFICIENCY)) {
            if (Myau.moduleManager == null) {
                return f;
            }
            Jesus jesus = (Jesus) Myau.moduleManager.modules.get(Jesus.class);
            if (jesus.isEnabled() && (!jesus.groundOnly.getValue() || ((Entity) (Object) this).isOnGround())) {
                return Math.max(f, jesus.speed.getValue());
            }
        }
        return f;
    }
}
