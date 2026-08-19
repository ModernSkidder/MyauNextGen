package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.event.EventManager;
import laoqi123.events.HitBlockEvent;
import laoqi123.events.LeftClickMouseEvent;
import laoqi123.events.LoadWorldEvent;
import laoqi123.events.ResizeEvent;
import laoqi123.events.RightClickMouseEvent;
import laoqi123.events.SwapItemEvent;
import laoqi123.module.modules.combat.NoHitDelay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MinecraftClient.class, priority = 9999)
public abstract class MixinMinecraft {
    @Shadow
    public int attackCooldown;
    @Shadow
    public ClientPlayerInteractionManager interactionManager;

    @Inject(
            method = {"tick"},
            at = {@At("RETURN")}
    )
    private void afterWorldTick(CallbackInfo callbackInfo) {
        laoqi123.util.PacketUtil.drainPendingPackets();
    }

    @Inject(
            method = {"joinWorld"},
            at = {@At("HEAD")}
    )
    private void loadWorld(ClientWorld world, DownloadingTerrainScreen.WorldEntryReason worldEntryReason, CallbackInfo callbackInfo) {
        EventManager.call(new LoadWorldEvent());
    }

    @Inject(
            method = {"onResolutionChanged"},
            at = {@At("RETURN")}
    )
    private void updateFramebufferSize(CallbackInfo callbackInfo) {
        EventManager.call(new ResizeEvent());
    }

    @Inject(
            method = {"doAttack"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void clickMouse(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (Myau.moduleManager != null && Myau.moduleManager.modules.get(NoHitDelay.class).isEnabled()) {
            this.attackCooldown = 0;
        }
        LeftClickMouseEvent event = new LeftClickMouseEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfoReturnable.cancel();
        }
    }

    @Inject(
            method = {"doItemUse"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void rightClickMouse(CallbackInfo callbackInfo) {
        RightClickMouseEvent event = new RightClickMouseEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = {"handleBlockBreaking"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void sendClickBlockToController(boolean breaking, CallbackInfo callbackInfo) {
        HitBlockEvent event = new HitBlockEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
            this.interactionManager.cancelBlockBreaking();
        }
    }

    @Redirect(
            method = {"handleInputEvents"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I",
                    opcode = 181
            )
    )
    private void changeCurrentItem(PlayerInventory inventory, int slot) {
        SwapItemEvent event = new SwapItemEvent(slot, 0);
        EventManager.call(event);
        if (!event.isCancelled()) {
            inventory.selectedSlot = slot;
        }
    }
}
