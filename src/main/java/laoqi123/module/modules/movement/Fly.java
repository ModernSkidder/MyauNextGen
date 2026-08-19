package laoqi123.module.modules.movement;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.StrafeEvent;
import laoqi123.events.UpdateEvent;
import laoqi123.module.Module;
import laoqi123.property.properties.FloatProperty;
import laoqi123.util.KeyBindUtil;
import laoqi123.util.MoveUtil;
import net.minecraft.client.MinecraftClient;

public class Fly extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private double verticalMotion = 0.0;
    public final FloatProperty hSpeed = new FloatProperty("horizontal-speed", 1.0F, 0.0F, 100.0F);
    public final FloatProperty vSpeed = new FloatProperty("vertical-speed", 1.0F, 0.0F, 100.0F);

    public Fly() {
        super("Fly", false);
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled()) {
            if (mc.player.getY() % 1.0 != 0.0) {
                mc.player.setVelocity(mc.player.getVelocity().x, this.verticalMotion, mc.player.getVelocity().z);
            }
            MoveUtil.setSpeed(0.0);
            event.setFriction((float) MoveUtil.getBaseMoveSpeed() * this.hSpeed.getValue());
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            this.verticalMotion = 0.0;
            if (mc.currentScreen == null) {
                if (KeyBindUtil.isKeyDown(mc.options.jumpKey)) {
                    this.verticalMotion = this.verticalMotion + this.vSpeed.getValue().doubleValue() * 0.42F;
                }
                if (KeyBindUtil.isKeyDown(mc.options.sneakKey)) {
                    this.verticalMotion = this.verticalMotion - this.vSpeed.getValue().doubleValue() * 0.42F;
                }
                mc.options.sneakKey.setPressed(false);
            }
        }
    }

    @Override
    public void onDisabled() {
        if (mc.player != null) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);
        }
        MoveUtil.setSpeed(0.0);
        mc.options.sneakKey.setPressed(KeyBindUtil.isKeyDown(mc.options.sneakKey));
    }
}
