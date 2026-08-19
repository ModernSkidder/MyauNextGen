package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.event.EventManager;
import laoqi123.events.KnockbackEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Entity.class, priority = 9999)
public abstract class MixinEntity {
    @Inject(method = "setVelocity(DDD)V", at = @At("HEAD"), cancellable = true)
    private void setVelocity(double x, double y, double z, CallbackInfo ci) {
        if ((Object) this instanceof ClientPlayerEntity) {
            KnockbackEvent event = new KnockbackEvent(x, y, z);
            EventManager.call(event);
            if (event.isCancelled()) {
                ci.cancel();
                ((Entity) (Object) this).setVelocity(new Vec3d(event.getX(), event.getY(), event.getZ()));
            }
        }
    }

    @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true)
    private void setAngles(CallbackInfo ci) {
        if ((Object) this instanceof ClientPlayerEntity && Myau.rotationManager != null && Myau.rotationManager.isRotated()) {
            ci.cancel();
        }
    }
}
