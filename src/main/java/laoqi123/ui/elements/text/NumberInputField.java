package laoqi123.ui.elements.text;

import laoqi123.ui.ColorPalette;
import laoqi123.ui.Colors;
import laoqi123.ui.InputHandler;
import laoqi123.ui.animations.ColorAnimation;
import laoqi123.ui.renderer.Icons;
import laoqi123.ui.renderer.NanoVGRenderUtil;

public class NumberInputField extends TextInputField {
    private final ColorAnimation colorTop = new ColorAnimation(ColorPalette.SECONDARY);
    private final ColorAnimation colorBottom = new ColorAnimation(ColorPalette.SECONDARY);
    private final float min;
    private final float max;
    private final float step;
    private float current;
    private boolean hoveredUp;
    private boolean pressedUp;
    private boolean hoveredDown;
    private boolean pressedDown;
    private boolean clickedUp;
    private boolean clickedDown;

    public NumberInputField(int width, int height, float defaultValue, float min, float max, float step) {
        super(width - 16, height, true, "");
        super.onlyNums = true;
        this.min = min;
        this.max = max;
        this.step = step;
        this.setCurrentValue(defaultValue);
    }

    @Override
    public void draw(long vg, float x, float y, InputHandler inputHandler) {
        super.errored = false;
        if (disabled) NanoVGRenderUtil.setAlpha(vg, 0.5f);
        NanoVGRenderUtil.drawRoundedRect(vg, x + width + 4, y, 12, 28, Colors.GRAY_500, 6f);

        boolean canUp = current < max && !disabled;
        boolean canDown = current > min && !disabled;
        hoveredUp = !disabled && inputHandler.isAreaHovered(x + width + 4, y, 12, 14);
        hoveredDown = !disabled && inputHandler.isAreaHovered(x + width + 4, y + 14, 12, 14);
        pressedUp = hoveredUp && inputHandler.isButtonDown(0);
        pressedDown = hoveredDown && inputHandler.isButtonDown(0);
        clickedUp = hoveredUp && inputHandler.isClicked();
        clickedDown = hoveredDown && inputHandler.isClicked();

        try {
            current = Float.parseFloat(input);
        } catch (NumberFormatException e) {
            super.errored = true;
        }
        if (current < min || current > max) {
            super.errored = true;
        }

        if (clickedUp && canUp) {
            current += step;
            if (current > max) current = max;
            setCurrentValue(current);
        }
        if (clickedDown && canDown) {
            current -= step;
            if (current < min) current = min;
            setCurrentValue(current);
        }

        if (!canUp) NanoVGRenderUtil.setAlpha(vg, 0.3f);
        NanoVGRenderUtil.drawRoundedRectVaried(vg, x + width + 4, y, 12, 14, colorTop.getColor(hoveredUp, pressedUp), 6f, 6f, 0f, 0f);
        Icons.chevronUp(vg, x + width + 5, y + 2, 10, Colors.WHITE_80);
        if (!canUp) NanoVGRenderUtil.setAlpha(vg, 1f);

        if (!canDown) NanoVGRenderUtil.setAlpha(vg, 0.3f);
        NanoVGRenderUtil.drawRoundedRectVaried(vg, x + width + 4, y + 14, 12, 14, colorBottom.getColor(hoveredDown, pressedDown), 0f, 0f, 6f, 6f);
        Icons.chevronDown(vg, x + width + 5, y + 15, 10, Colors.WHITE_80);
        if (!disabled) NanoVGRenderUtil.setAlpha(vg, 1f);

        super.draw(vg, x, y - 2, inputHandler);
        if (disabled) NanoVGRenderUtil.setAlpha(vg, 1f);
    }

    public float getCurrentValue() {
        return current;
    }

    public void setCurrentValue(float value) {
        if (value == (int) value) {
            this.input = String.format("%d", (int) value);
        } else if (value * 10 == (int) (value * 10)) {
            this.input = String.format("%.1f", value);
        } else {
            this.input = String.format("%.2f", value);
        }
        if (caretPos > input.length()) caretPos = input.length();
    }

    @Override
    public void onClose() {
        try {
            if (current < min) current = min;
            if (current > max) current = max;
            setCurrentValue(current);
        } catch (Exception ignored) {
        }
        super.onClose();
    }

    public boolean arrowsClicked() {
        return clickedUp || clickedDown;
    }
}
