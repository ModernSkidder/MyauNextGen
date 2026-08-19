package laoqi123.module.modules;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.events.KeyEvent;
import laoqi123.module.Module;
import laoqi123.util.ChatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class MCF extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public MCF() {
        super("MCF", false, true);
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (this.isEnabled() && event.getKey() == -98) {
            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY && ((EntityHitResult) mc.crosshairTarget).getEntity() instanceof PlayerEntity) {
                String hitName = ((EntityHitResult) mc.crosshairTarget).getEntity().getName().getString();
                if (!Myau.friendManager.isFriend(hitName)) {
                    Myau.friendManager.add(hitName);
                    ChatUtil.sendFormatted(String.format("%sAdded &o%s&r to your friend list&r", Myau.clientName, hitName));
                } else {
                    Myau.friendManager.remove(hitName);
                    ChatUtil.sendFormatted(String.format("%sRemoved &o%s&r from your friend list&r", Myau.clientName, hitName));
                }
            }
        }
    }
}
