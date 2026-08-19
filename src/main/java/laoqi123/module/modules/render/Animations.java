package laoqi123.module.modules.render;

import laoqi123.config.AnimationConfig;
import laoqi123.config.AnimationMode;
import laoqi123.module.Module;
import laoqi123.value.properties.FloatValue;
import laoqi123.value.properties.IntValue;
import laoqi123.value.properties.ModeValue;

/**
 * Animations Module
 * Original logic by syuto/animations-1.6, integrated into Uzi
 */
public class Animations extends Module {

    private static final String[] MODES = new String[]{"VANILLA", "EXHIBITION", "ETB", "SIGMA", "DORTWARE", "PLAIN",
            "SPIN", "AVATAR", "SWONG", "SWANG", "SWANK", "STYLES",
            "NUDGE", "PUNCH", "JIGSAW", "SLIDE",
            "Swing", "Old", "Push", "Dash", "Slash", "Scale", "Swonk", "Stella",
            "Small", "Edit", "Rhys", "Stab", "Float", "Remix", "Xiv", "Winter",
            "Yamato", "SlideSwing", "SmallPush", "Reverse", "Invent", "Leaked",
            "Aqua", "Astro", "Fadeaway", "Astolfo", "AstolfoSpin", "Moon",
            "MoonPush", "Smooth", "Tap1", "Tap2", "Sigma3", "Sigma4",
            "1.8", "Slide", "Swank", "Swang", "Avatar", "Jigsaw"};

    public final ModeValue mode = new ModeValue("Mode", 0, MODES);
    public final ModeValue render = new ModeValue("Render", 1, new String[]{"BLOCKING", "ALWAYS"});

    public final IntValue scale = new IntValue("Scale", 100, 50, 150);
    public final FloatValue itemSize = new FloatValue("Item-Size", 0.0F, -0.5F, 0.5F);
    public final FloatValue blockPosX = new FloatValue("BlockPos-X", 0.0F, -1.0F, 1.0F);
    public final FloatValue blockPosY = new FloatValue("BlockPos-Y", 0.0F, -1.0F, 1.0F);
    public final FloatValue blockPosZ = new FloatValue("BlockPos-Z", 0.0F, -1.0F, 1.0F);
    public final IntValue swingSpeed = new IntValue("SwingSpeed", 0, 0, 100);

    public Animations() {
        super("Animations", true, false);
    }

    @Override
    public void onEnabled() {
        syncConfig();
    }

    @Override
    public void onDisabled() {
        AnimationConfig.setEnabled(false);
    }

    private void syncConfig() {
        AnimationConfig.setEnabled(true);
        AnimationMode[] modes = AnimationMode.values();
        if (mode.getValue() < modes.length) {
            AnimationConfig.setMode(modes[mode.getValue()]);
        }
        AnimationConfig.setRenderMode(render.getValue());
        AnimationConfig.setScale(scale.getValue());
        AnimationConfig.setItemSize(itemSize.getValue());
        AnimationConfig.setBlockPosX(blockPosX.getValue());
        AnimationConfig.setBlockPosY(blockPosY.getValue());
        AnimationConfig.setBlockPosZ(blockPosZ.getValue());
        AnimationConfig.setSwingSpeed(swingSpeed.getValue());
    }

    public void onUpdate() {
        if (this.isEnabled()) {
            syncConfig();
        }
    }

    @Override
    public String[] getSuffix() {
        String modeName = mode.getModeString();
        return new String[]{modeName.isEmpty() ? "?" : modeName};
    }
}
