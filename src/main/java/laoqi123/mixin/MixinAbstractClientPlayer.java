package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.module.modules.Scaffold;
import laoqi123.module.modules.Sprint;
import laoqi123.util.player.PlayerUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractClientPlayerEntity.class, priority = 9999)
public abstract class MixinAbstractClientPlayer extends MixinEntityPlayer {
    @Redirect(
            method = {"getFovMultiplier"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getAttributeValue(Lnet/minecraft/registry/entry/RegistryEntry;)D"
            )
    )
    private double getFovMultiplier(AbstractClientPlayerEntity livingEntity, RegistryEntry<EntityAttribute> registryEntry) {
        double attributeValue = livingEntity.getAttributeValue(registryEntry);
        if (livingEntity instanceof ClientPlayerEntity && Myau.moduleManager != null) {
            Sprint sprint = (Sprint) Myau.moduleManager.modules.get(Sprint.class);
            return sprint.isEnabled() && sprint.shouldApplyFovFix(livingEntity.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED))
                    ? attributeValue * 1.300000011920929
                    : attributeValue;
        }
        return attributeValue;
    }

    @Inject(method = "getFovMultiplier", at = @At("RETURN"), cancellable = true)
    private void scaffoldKeepFov(boolean changingFov, float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (Myau.moduleManager == null) {
            return;
        }
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled() && scaffold.keepFoV.getValue() && PlayerUtils.isMoving()) {
            cir.setReturnValue(scaffold.fovValue.getValue() + PlayerUtils.getMoveSpeedEffectAmplifier() * 0.13F);
        }
    }
}
