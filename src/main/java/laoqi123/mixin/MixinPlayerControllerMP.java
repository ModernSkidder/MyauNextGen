package laoqi123.mixin;

import laoqi123.event.EventManager;
import laoqi123.event.impl.AttackEvent;
import laoqi123.event.impl.CancelUseEvent;
import laoqi123.event.impl.WindowClickEvent;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayerInteractionManager.class, priority = 9999)
public abstract class MixinPlayerControllerMP {

    @Inject(
            method = "attackEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;syncSelectedSlot()V"))
    private void attackEntity(
            PlayerEntity player, Entity target, CallbackInfo callbackInfo
    ) {
        EventManager.call(new AttackEvent(target));
    }

    @Inject(
            method = {"clickSlot"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void clickSlot(
            int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo callbackInfo
    ) {
        WindowClickEvent event = new WindowClickEvent(syncId, slotId, button, actionType.ordinal());
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = {"stopUsingItem"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void stopUsingItem(PlayerEntity player, CallbackInfo callbackInfo) {
        CancelUseEvent event = new CancelUseEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }
}
