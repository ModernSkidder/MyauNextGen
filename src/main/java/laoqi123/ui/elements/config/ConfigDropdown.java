package laoqi123.ui.elements.config;

import laoqi123.property.properties.ModeProperty;
import laoqi123.ui.ColorPalette;
import laoqi123.ui.Colors;
import laoqi123.ui.GuiUtils;
import laoqi123.ui.InputHandler;
import laoqi123.ui.animations.Animation;
import laoqi123.ui.animations.ColorAnimation;
import laoqi123.ui.animations.EaseOutQuad;
import laoqi123.ui.renderer.Icons;
import laoqi123.ui.renderer.NanoVGRenderUtil;

public class ConfigDropdown extends ConfigOption {
    private final ModeProperty modeProperty;
    private final ColorAnimation backgroundColor = new ColorAnimation(ColorPalette.SECONDARY);
    private final ColorAnimation atomColor = new ColorAnimation(new ColorPalette(Colors.PRIMARY_600, Colors.PRIMARY_500, Colors.PRIMARY_500));
    private boolean opened = false;
    private Animation scrollAnimation;
    private float scrollTarget;
    private float scroll;
    private long scrollTime;
    private float x;
    private float y;

    public ConfigDropdown(ModeProperty property, int size) {
        super(property, size);
        this.modeProperty = property;
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        this.x = x;
        this.y = y;
        if (!enabled) NanoVGRenderUtil.setAlpha(vg, 0.5f);
        NanoVGRenderUtil.drawText(vg, name, x, y + 16, nameColor, 14f);

        boolean hovered = inputHandler.isAreaHovered(x + 352, y, 640, 32) && enabled;
        if (hovered && inputHandler.isClicked()) {
            opened = !opened;
            backgroundColor.setPalette(opened ? ColorPalette.PRIMARY : ColorPalette.SECONDARY);
        }
        if (opened) return;

        if (hovered && inputHandler.isButtonDown(0)) NanoVGRenderUtil.setAlpha(vg, 0.8f);
        NanoVGRenderUtil.drawRoundedRect(vg, x + 352, y, 640, 32, backgroundColor.getColor(hovered, hovered && inputHandler.isButtonDown(0)), 12);
        String selected = modeProperty.getModeString();
        NanoVGRenderUtil.drawText(vg, selected, x + 364, y + 16, Colors.WHITE_80, 14f);
        NanoVGRenderUtil.drawRoundedRect(vg, x + 964, y + 4, 24, 24, atomColor.getColor(hovered, false), 8);
        Icons.chevronDown(vg, x + 966, y + 6, 20, Colors.WHITE_80);
        NanoVGRenderUtil.setAlpha(vg, 1f);
    }

    @Override
    public void drawLast(long vg, int x, InputHandler inputHandler) {
        if (!opened) {
            finishUpAndClose();
            return;
        }
        String[] modes = modeProperty.getModes();
        int visible = Math.min(modes.length, 10);
        float panelH = visible * 32 + 8;

        boolean clickedOutside = inputHandler.isClicked() && !inputHandler.isAreaHovered(x + 352, y + 40, 640, panelH, true)
                && !inputHandler.isAreaHovered(x + 352, y, 640, 32, true);
        if (clickedOutside) {
            opened = false;
            backgroundColor.setPalette(ColorPalette.SECONDARY);
            return;
        }

        NanoVGRenderUtil.drawRoundedRect(vg, x + 352, y + 40, 640, panelH, Colors.GRAY_700, 12);
        NanoVGRenderUtil.drawHollowRoundRect(vg, x + 351, y + 39, 642, panelH + 2, NanoVGRenderUtil.alpha(Colors.WHITE, 30), 12, 1);

        float maxHeight = modes.length * 32 + 8;
        float scrollWidth = panelH;
        final float scrollBarLength = (scrollWidth / maxHeight) * scrollWidth;
        float dWheel = (float) inputHandler.getDWheel(true);
        if (dWheel != 0) {
            scrollTarget += dWheel * 0.25f;
            if (scrollTarget > 0f) scrollTarget = 0f;
            else if (scrollTarget < -maxHeight + scrollWidth) scrollTarget = -maxHeight + scrollWidth;
            scrollAnimation = new EaseOutQuad(150, scroll, scrollTarget, false);
            scrollTime = System.currentTimeMillis();
        } else if (scrollAnimation != null && scrollAnimation.isFinished()) {
            scrollAnimation = null;
        }
        scroll = scrollAnimation == null ? scrollTarget : scrollAnimation.get();

        float optionY = y + 44 + scroll;
        for (String option : modes) {
            boolean optionHovered = inputHandler.isAreaHovered(x + 352, optionY, 640, 32, true);
            int color = Colors.WHITE_80;
            if (optionHovered && inputHandler.isButtonDown(0)) {
                NanoVGRenderUtil.drawRoundedRect(vg, x + 356, optionY + 2, 632, 28, Colors.PRIMARY_700_80, 8);
            } else if (optionHovered) {
                NanoVGRenderUtil.drawRoundedRect(vg, x + 356, optionY + 2, 632, 28, Colors.PRIMARY_700, 8);
                color = Colors.WHITE;
            }
            if (optionHovered && inputHandler.isClicked(true)) {
                modeProperty.setValue(java.util.Arrays.asList(modes).indexOf(option));
                opened = false;
                backgroundColor.setPalette(ColorPalette.SECONDARY);
            }
            NanoVGRenderUtil.drawText(vg, option, x + 368, optionY + 18, color, 14);
            optionY += 32;
        }

        if (!(scrollBarLength >= scrollWidth)) {
            final float scrollBarY = (scroll / maxHeight) * (scrollWidth - 8) - 45;
            final boolean isMouseDown = inputHandler.isButtonDown(0);
            final boolean scrollHover = inputHandler.isAreaHovered(x + 988, y - scrollBarY, 12, scrollBarLength - 5, true);
            final boolean scrollTimePeriod = (System.currentTimeMillis() - scrollTime < 1000);
            boolean dragging;
            if (scrollHover && isMouseDown && !mouseWasDown) {
                yStart = inputHandler.mouseY();
                dragging = true;
            } else {
                dragging = this.dragging;
            }
            mouseWasDown = isMouseDown;
            this.dragging = dragging;
            if (dragging) {
                scrollTarget = -(inputHandler.mouseY() - yStart) * maxHeight / scrollWidth;
                if (scrollTarget > 0f) scrollTarget = 0f;
                else if (scrollTarget < -maxHeight + scrollWidth) scrollTarget = -maxHeight + scrollWidth;
                scrollAnimation = new EaseOutQuad(150, scroll, scrollTarget, false);
            }
            NanoVGRenderUtil.drawRoundedRect(vg, x + 988, y - scrollBarY, 4, scrollBarLength - 5, NanoVGRenderUtil.alpha(Colors.WHITE, scrollHover || scrollTimePeriod ? 120 : 60), 4f);
        }
    }

    @Override
    public void finishUpAndClose() {
        scroll = 0;
        scrollTarget = 0;
        scrollTime = 0;
        scrollAnimation = null;
    }

    @Override
    public int getHeight() {
        return 32;
    }

    private boolean mouseWasDown = false;
    private boolean dragging = false;
    private float yStart;
}
