package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.module.modules.movement.Jesus;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity {
    @Inject(
            method = {"isPushedByFluids()Z"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void isPushedByFluids(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if ((Object) this instanceof ClientPlayerEntity && Myau.moduleManager != null) {
            Jesus jesus = (Jesus) Myau.moduleManager.modules.get(Jesus.class);
            if (jesus.isEnabled() && jesus.noPush.getValue()) {
                callbackInfoReturnable.setReturnValue(false);
            }
        }
    }
}
