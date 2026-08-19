package laoqi123.ui.elements;

import laoqi123.module.Module;
import laoqi123.ui.ColorPalette;
import laoqi123.ui.Colors;
import laoqi123.ui.InputHandler;
import laoqi123.ui.animations.ColorAnimation;
import laoqi123.ui.pages.ModsPage;
import laoqi123.ui.renderer.NanoVGRenderUtil;
import laoqi123.util.KeyBindUtil;

public class ModCard extends BasicElement {
    private final Module mod;
    private final ModsPage page;
    private final ColorAnimation colorFrame = new ColorAnimation(ColorPalette.SECONDARY);
    private final ColorAnimation colorToggle;
    private boolean active;
    private boolean isHoveredMain = false;

    public ModCard(Module mod, ModsPage page) {
        super(244, 119, false);
        this.mod = mod;
        this.page = page;
        this.active = mod.isEnabled();
        toggled = active;
        colorToggle = new ColorAnimation(active ? ColorPalette.PRIMARY : ColorPalette.SECONDARY);
    }

    @Override
    public void draw(long vg, float x, float y, InputHandler inputHandler) {
        super.update(x, y, inputHandler);
        boolean transitioning = laoqi123.ui.ClickGui.INSTANCE != null && laoqi123.ui.ClickGui.INSTANCE.isTransitioning();
        if (transitioning) toggled = active;
        String name = mod.getName();
        isHoveredMain = inputHandler.isAreaHovered(x, y, width, 87);
        boolean isHoveredSecondary = inputHandler.isAreaHovered(x, y + 87, width, 32);
        NanoVGRenderUtil.drawRoundedRectVaried(vg, x, y, width, 87, colorFrame.getColor(isHoveredMain, isHoveredMain && inputHandler.isButtonDown(0)), 12f, 12f, 0f, 0f);
        NanoVGRenderUtil.drawRoundedRectVaried(vg, x, y + 87, width, 32, colorToggle.getColor(isHoveredSecondary, isHoveredSecondary && inputHandler.isButtonDown(0)), 0f, 0f, 12f, 12f);
        NanoVGRenderUtil.drawLine(vg, x, y + 86, x + width, y + 86, 2, Colors.GRAY_300);

        float nameW = NanoVGRenderUtil.getTextWidth(vg, name, 16);
        NanoVGRenderUtil.drawText(vg, name, x + (244 - nameW) / 2f, y + 49, NanoVGRenderUtil.alpha(Colors.WHITE, (int) (colorFrame.getAlpha() * 255)), 16);
        if (mod.getKey() != 0) {
            String key = KeyBindUtil.getKeyName(mod.getKey());
            float kw = NanoVGRenderUtil.getTextWidth(vg, key, 11);
            NanoVGRenderUtil.drawText(vg, key, x + (244 - kw) / 2f, y + 72, Colors.WHITE_50, 11);
        }

        NanoVGRenderUtil.drawText(vg, name, x + 12, y + 103, NanoVGRenderUtil.alpha(Colors.WHITE, (int) (colorToggle.getAlpha() * 255)), 14);
        NanoVGRenderUtil.drawRoundedRect(vg, x + width - 40, y + 96, 28, 14, active ? Colors.PRIMARY_600 : Colors.GRAY_600, 7f);
        NanoVGRenderUtil.drawCircle(vg, x + width - 40 + (active ? 21 : 7), y + 103, 5, Colors.WHITE);

        if (!transitioning && clicked && isHoveredMain) {
            toggled = active;
            page.openModule(mod);
        }
        if (!transitioning && inputHandler.isClicked(1) && isHoveredMain) {
            toggled = active;
            page.openModule(mod);
        }
        if (active != toggled) {
            active = toggled;
            colorToggle.setPalette(active ? ColorPalette.PRIMARY : ColorPalette.SECONDARY);
            mod.setEnabled(active);
        }
    }

    public Module getMod() {
        return mod;
    }

    public boolean isActive() {
        return active;
    }
}
