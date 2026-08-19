package laoqi123.module.modules.movement;

import laoqi123.event.EventTarget;
import laoqi123.events.TickEvent;
import laoqi123.mixin.LivingEntityAccessor;
import laoqi123.module.Module;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.util.KeyBindUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;

public class Sprint extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private boolean wasSprinting = false;
    public final BooleanProperty foxFix = new BooleanProperty("fov-fix", true);

    public Sprint() {
        super("Sprint", true, true);
    }

    public boolean shouldApplyFovFix(EntityAttributeInstance attribute) {
        if (!this.foxFix.getValue()) {
            return false;
        }
        EntityAttributeModifier attributeModifier = ((LivingEntityAccessor) mc.player).getSprintingSpeedBoostModifier();
        return attribute.getModifier(attributeModifier.id()) == null && this.wasSprinting;
    }

    public boolean shouldKeepFov(boolean boolean2) {
        return this.foxFix.getValue() && !boolean2 && this.wasSprinting;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            switch (event.getType()) {
                case PRE:
                    if (!mc.options.sprintKey.isPressed()) {
                        KeyBindUtil.setKeyBindState(this.getSprintKeyCode(), true);
                    }
                    break;
                case POST:
                    this.wasSprinting = mc.player.isSprinting();
            }
        }
    }

    @Override
    public void onDisabled() {
        this.wasSprinting = false;
        KeyBindUtil.updateKeyState(this.getSprintKeyCode());
    }

    private int getSprintKeyCode() {
        return InputUtil.fromTranslationKey(mc.options.sprintKey.getBoundKeyTranslationKey()).getCode();
    }
}
