package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.event.EventManager;
import laoqi123.event.impl.SafeWalkEvent;
import laoqi123.module.modules.movement.KeepSprint;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = PlayerEntity.class, priority = 9999)
public abstract class MixinEntityPlayer extends MixinEntityLivingBase {
    @ModifyConstant(method = "attack", constant = @Constant(doubleValue = 0.6))
    private double attack(double speed) {
        if (Myau.moduleManager == null) {
            return speed;
        } else {
            KeepSprint keepSprint = (KeepSprint) Myau.moduleManager.modules.get(KeepSprint.class);
            return keepSprint.isEnabled() && keepSprint.shouldKeepSprint()
                    ? speed + (1.0 - speed) * (1.0 - keepSprint.slowdown.getValue().doubleValue() / 100.0)
                    : speed;
        }
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V"))
    private void setSprinnt(PlayerEntity playerEntity, boolean boolean2) {
        if (Myau.moduleManager != null) {
            KeepSprint keepSprint = (KeepSprint) Myau.moduleManager.modules.get(KeepSprint.class);
            if (!keepSprint.isEnabled() || !keepSprint.shouldKeepSprint()) {
                playerEntity.setSprinting(boolean2);
            }
        }
    }

    @Redirect(
            method = "adjustMovementForSneaking",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;clipAtLedge()Z")
    )
    private boolean adjustMovementForSneaking(PlayerEntity playerEntity) {
        boolean clipAtLedge = playerEntity.isSneaking();
        if (playerEntity instanceof ClientPlayerEntity) {
            SafeWalkEvent event = new SafeWalkEvent(clipAtLedge);
            EventManager.call(event);
            return event.isSafeWalk();
        }
        return clipAtLedge;
    }
}
