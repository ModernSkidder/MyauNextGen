package laoqi123.module.modules.movement;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.UpdateEvent;
import laoqi123.mixin.KeyBindingAccessor;
import laoqi123.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

public class AntiAFK extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private int lastInput;

    public AntiAFK() {
        super("AntiAFK", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event){
        if(event.getType() == EventType.PRE && this.isEnabled()){
            GameOptions gameOptions = mc.options;
            if (gameOptions.jumpKey.isPressed() || gameOptions.rightKey.isPressed() || gameOptions.forwardKey.isPressed() || gameOptions.leftKey.isPressed() || gameOptions.backKey.isPressed()) {
                lastInput = 0;
            }
            lastInput++;
            if (lastInput < 20 * 10) return;
            if (mc.player.age % 5 == 0) {
                ((KeyBindingAccessor)mc.options.rightKey).setPressed(false);
                ((KeyBindingAccessor)mc.options.leftKey).setPressed(false);
                ((KeyBindingAccessor)mc.options.jumpKey).setPressed(false);
            }
            if (mc.player.age % 20 == 0) {
                if (mc.player.age % 40 == 0) {
                    ((KeyBindingAccessor)mc.options.rightKey).setPressed(true);
                } else {
                    ((KeyBindingAccessor)mc.options.leftKey).setPressed(true);
                }
            }
            if (mc.player.age % 100 == 0) {
                ((KeyBindingAccessor)mc.options.jumpKey).setPressed(true);
            }
        }
    }
}
