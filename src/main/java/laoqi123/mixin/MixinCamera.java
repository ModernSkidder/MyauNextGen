package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.module.modules.render.ViewClip;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Camera.class, priority = 9999)
public abstract class MixinCamera {
    @Invoker("clipToSpace")
    protected abstract float invokeClipToSpace(float f);

    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"
            )
    )
    private float viewClip(Camera camera, float f) {
        if (Myau.moduleManager != null && Myau.moduleManager.modules.get(ViewClip.class).isEnabled()) {
            return f;
        }
        return this.invokeClipToSpace(f);
    }
}
