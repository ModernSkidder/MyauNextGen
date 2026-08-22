package laoqi123.mixin;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import laoqi123.Myau;
import laoqi123.module.modules.Timer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderTickCounter.Dynamic.class)
public abstract class MixinRenderTickCounter {
    @Redirect(
            method = "beginRenderTick(J)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F",
                    remap = false
            )
    )
    private float modifyTimerSpeed(FloatUnaryOperator operator, float tickTime) {
        if (Myau.moduleManager == null) {
            return operator.apply(tickTime);
        }
        Timer timer = (Timer) Myau.moduleManager.modules.get(Timer.class);
        float speed = timer != null && timer.isEnabled() ? timer.speed.getValue() : 1.0F;
        if (speed <= 0.0F) {
            speed = 1.0F;
        }
        return operator.apply(tickTime) / speed;
    }
}
