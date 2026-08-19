package laoqi123.ui.elements.config;

import laoqi123.value.properties.BooleanValue;
import laoqi123.ui.ColorPalette;
import laoqi123.ui.Colors;
import laoqi123.ui.InputHandler;
import laoqi123.ui.animations.Animation;
import laoqi123.ui.animations.ColorAnimation;
import laoqi123.ui.animations.DummyAnimation;
import laoqi123.ui.animations.EaseOutBump;
import laoqi123.ui.renderer.NanoVGRenderUtil;

public class ConfigSwitch extends ConfigOption {
    private ColorAnimation color;
    private Animation animation;

    public ConfigSwitch(BooleanValue property, int size) {
        super(property, size);
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        boolean toggled = (Boolean) value.getValue();
        if (animation == null) {
            animation = new DummyAnimation(toggled ? 1 : 0);
            color = new ColorAnimation(toggled ? ColorPalette.PRIMARY : ColorPalette.SECONDARY);
        }
        float percentOn = animation.get();
        int x2 = x + 3 + (int) (percentOn * 18);
        boolean hovered = inputHandler.isAreaHovered(x, y, 42, 32);
        if (!enabled) NanoVGRenderUtil.setAlpha(vg, 0.5f);
        NanoVGRenderUtil.drawRoundedRect(vg, x, y + 4, 42, 24, color.getColor(hovered, hovered && inputHandler.isButtonDown(0)), 12f);
        NanoVGRenderUtil.drawRoundedRect(vg, x2, y + 7, 18, 18, Colors.WHITE, 9f);
        NanoVGRenderUtil.drawText(vg, name, x + 50, y + 17, nameColor, 14f);

        if (inputHandler.isAreaClicked(x, y, 42, 32) && enabled) {
            toggled = !toggled;
            value.setValue(toggled);
        }
        if (toggled == animation.isReversed()) {
            animation = new EaseOutBump(200, 0, 1, !toggled);
            color.setPalette(toggled ? ColorPalette.PRIMARY : ColorPalette.SECONDARY);
        }
        NanoVGRenderUtil.setAlpha(vg, 1f);
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
