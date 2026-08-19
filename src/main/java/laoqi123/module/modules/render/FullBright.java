package laoqi123.module.modules.render;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.TickEvent;
import laoqi123.module.Module;
import laoqi123.property.properties.ModeProperty;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class FullBright extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private float prevGamma = Float.NaN;
    private boolean appliedNightVision = false;
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"GAMMA", "EFFECT"});

    public FullBright() {
        super("Fullbright", true, true);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            switch (this.mode.getValue()) {
                case 0:
                    mc.options.getGamma().setValue(1.0);
                    break;
                case 1:
                    mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 25940, 0));
            }
        }
    }

    @Override
    public void onEnabled() {
        switch (this.mode.getValue()) {
            case 0:
                this.prevGamma = mc.options.getGamma().getValue().floatValue();
                break;
            case 1:
                this.appliedNightVision = true;
        }
    }

    @Override
    public void onDisabled() {
        if (!Float.isNaN(this.prevGamma)) {
            mc.options.getGamma().setValue((double) this.prevGamma);
            this.prevGamma = Float.NaN;
        }
        if (this.appliedNightVision) {
            if (mc.player != null) {
                mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
            this.appliedNightVision = false;
        }
    }

    @Override
    public void verifyValue(String mode) {
        if (this.isEnabled()) {
            this.onDisabled();
            this.onEnabled();
        }
    }
}
