package laoqi123.module.modules;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.TickEvent;
import laoqi123.module.Module;
import laoqi123.property.properties.ModeProperty;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.lang.reflect.Field;

public class FullBright extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final double GAMMA_TARGET = 1000.0; // far beyond the [0,1] slider clamp -> full bright
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
                    setGammaRaw(GAMMA_TARGET);
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
                setGammaRaw(GAMMA_TARGET);
                break;
            case 1:
                this.appliedNightVision = true;
        }
    }

    @Override
    public void onDisabled() {
        if (!Float.isNaN(this.prevGamma)) {
            // prevGamma is within [0,1], so the public setter is safe here
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

    /**
     * Writes the gamma value directly into the SimpleOption backing field, bypassing the
     * DoubleSliderCallbacks [0,1] clamp that would otherwise throw
     * "Illegal option value" for values like 1000.0.
     */
    private void setGammaRaw(double value) {
        try {
            SimpleOption<Double> gamma = mc.options.getGamma();
            // Locate the backing value field by matching the current (clamped) value,
            // which is mapping-agnostic (works on both Yarn-dev and obfuscated builds).
            for (Field f : gamma.getClass().getDeclaredFields()) {
                if (!f.getType().equals(Object.class) && !f.getType().equals(Double.class)) continue;
                f.setAccessible(true);
                Object cur = f.get(gamma);
                if (cur instanceof Double && Math.abs((Double) cur - gamma.getValue()) < 1e-6) {
                    f.set(gamma, value);
                    return;
                }
            }
            // Fallback: try a few known field names across mappings.
            for (String name : new String[]{"value", "h", "field_23120", "c"}) {
                try {
                    Field f = gamma.getClass().getDeclaredField(name);
                    f.setAccessible(true);
                    f.set(gamma, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                }
            }
            // Last resort: clamped public setter.
            gamma.setValue(Math.max(0.0, Math.min(1.0, value)));
        } catch (Throwable t) {
            try {
                mc.options.getGamma().setValue(Math.max(0.0, Math.min(1.0, value)));
            } catch (Throwable ignored) {
            }
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
