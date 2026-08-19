package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.module.modules.render.ESP;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemStack.class, priority = 9999)
public abstract class MixinItemStack {
    @Inject(
            method = {"hasGlint"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void hasGlint(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (Myau.moduleManager != null) {
            ESP esp = (ESP) Myau.moduleManager.modules.get(ESP.class);
            if (esp.isEnabled() && !esp.isGlowEnabled()) {
                callbackInfoReturnable.setReturnValue(false);
            }
        }
    }
}
