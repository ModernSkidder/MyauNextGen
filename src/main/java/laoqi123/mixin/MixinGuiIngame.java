package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.event.EventManager;
import laoqi123.events.Render2DEvent;
import laoqi123.module.modules.player.AutoBlockIn;
import laoqi123.module.modules.misc.NickHider;
import laoqi123.module.modules.player.Scaffold;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InGameHud.class, priority = 9999)
public abstract class MixinGuiIngame {
    @Redirect(
            method = {"tick()V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerInventory;getMainHandStack()Lnet/minecraft/item/ItemStack;"
            )
    )
    private ItemStack updateTick(PlayerInventory inventoryPlayer) {
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled() && scaffold.spoofItem.getValue()) {
            int slot = scaffold.getSlot();
            if (slot >= 0) {
                return inventoryPlayer.getStack(slot);
            }
        }
        AutoBlockIn autoBlockIn = (AutoBlockIn) Myau.moduleManager.modules.get(AutoBlockIn.class);
        if (autoBlockIn.itemSpoof.getValue() && autoBlockIn.isEnabled()) {
            int slot = autoBlockIn.getSlot();
            if (slot >= 0) {
                return inventoryPlayer.getStack(slot);
            }
        }
        return inventoryPlayer.getMainHandStack();
    }

    @Inject(
            method = {"render"},
            at = {@At("RETURN")}
    )
    private void renderGameOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo callbackInfo) {
        EventManager.call(new Render2DEvent(context, tickCounter.getTickDelta(true)));
    }

    @Redirect(
            method = {"renderExperienceBar"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;experienceProgress:F"
            )
    )
    private float renderExperience(ClientPlayerEntity entityPlayerSP) {
        if (Myau.moduleManager == null) {
            return entityPlayerSP.experienceProgress;
        } else {
            NickHider event = (NickHider) Myau.moduleManager.modules.get(NickHider.class);
            return event.isEnabled() && event.level.getValue() ? 0.0F : entityPlayerSP.experienceProgress;
        }
    }

    @Redirect(
            method = {"renderExperienceLevel"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;experienceLevel:I"
            )
    )
    private int renderExperienceLevel(ClientPlayerEntity entityPlayerSP) {
        if (Myau.moduleManager == null) {
            return entityPlayerSP.experienceLevel;
        } else {
            NickHider event = (NickHider) Myau.moduleManager.modules.get(NickHider.class);
            return event.isEnabled() && event.level.getValue() ? 0 : entityPlayerSP.experienceLevel;
        }
    }
}
