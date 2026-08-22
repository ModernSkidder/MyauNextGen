package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.module.modules.AntiDebuff;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BackgroundRenderer.class, priority = 9999)
public abstract class MixinBackgroundRenderer {
    @Inject(
            method = "getFogModifier",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void getFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<?> callbackInfoReturnable) {
        if (Myau.moduleManager != null && entity instanceof LivingEntity livingEntity && livingEntity.hasStatusEffect(StatusEffects.BLINDNESS)) {
            AntiDebuff antiDebuff = (AntiDebuff) Myau.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.blindness.getValue()) {
                callbackInfoReturnable.setReturnValue(null);
            }
        }
    }
}
