package laoqi123.ui.elements.config;

import laoqi123.value.Value;
import laoqi123.ui.Colors;
import laoqi123.ui.GuiUtils;
import laoqi123.ui.InputHandler;
import laoqi123.ui.animations.Animation;
import laoqi123.ui.animations.DummyAnimation;
import laoqi123.ui.animations.EaseInOutCubic;
import laoqi123.ui.animations.EaseInOutQuart;
import laoqi123.ui.animations.EaseOutExpo;
import laoqi123.ui.dataset.Slider;
import laoqi123.ui.elements.IFocusable;
import laoqi123.ui.elements.text.NumberInputField;
import laoqi123.ui.renderer.NanoVGRenderUtil;

public class ConfigSlider extends ConfigOption implements IFocusable {
    private static final int STEP_POPUP_DURATION = 400;
    private static final int INDICATOR_POPUP_DURATION = 200;
    private static final float STEP_HEIGHT_TOTAL = 16;
    private static final float STEP_HEIGHT_HOVER = 10;
    private static final float TOUCH_TARGET_HOVER = 16;

    private final Slider slider;
    private final NumberInputField inputField;
    private final float min;
    private final float max;
    private final int step;
    private final float increment;
    private boolean dragging = false;
    private boolean mouseWasDown = false;
    private Animation stepsAnimation;
    private Animation targetAnimation;
    private Animation stepSlideAnimation;
    private float animationStart;
    private float lastSliderTarget = 1;
    private boolean animReset;
    private float lastX = -1;

    public ConfigSlider(Value<?> value, Slider slider, int size) {
        super(value, size);
        this.slider = slider;
        this.min = (float) slider.getMin();
        this.max = (float) slider.getMax();
        this.step = (int) slider.getIncrement() == 0 ? 0 : Math.max(1, (int) Math.round((max - min) / slider.getIncrement()));
        this.increment = (float) slider.getIncrement();
        this.inputField = new NumberInputField(84, 32, 0, min, max, (float) slider.getIncrement());
        this.stepsAnimation = new DummyAnimation(0);
        this.targetAnimation = new DummyAnimation(0);
        this.stepSlideAnimation = new DummyAnimation(1);
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        int xCoordinate = 0;
        float value = 0;
        boolean hovered = inputHandler.isAreaHovered(x + 352, y, 512, 32) && enabled;

        inputField.disable(!enabled);
        if (!enabled) NanoVGRenderUtil.setAlpha(vg, 0.5f);

        boolean isMouseDown = inputHandler.isButtonDown(0);
        if (hovered && isMouseDown && !mouseWasDown) dragging = true;
        boolean startedDragging = !mouseWasDown && isMouseDown;
        mouseWasDown = isMouseDown;
        if (dragging) {
            xCoordinate = (int) GuiUtils.clamp(inputHandler.mouseX(), x + 352, x + 864);
            if (step > 0) xCoordinate = getStepCoordinate(xCoordinate, x);
            value = (float) GuiUtils.mapIncrement(xCoordinate, x + 352, x + 864, min, max, increment);
        } else if (inputField.isToggled() || inputField.arrowsClicked()) {
            value = inputField.getCurrentValue();
            xCoordinate = (int) GuiUtils.clamp(GuiUtils.map(value, min, max, x + 352, x + 864), x + 352, x + 864);
        }
        if ((dragging && inputHandler.isClicked()) || inputField.isToggled() || inputField.arrowsClicked()) {
            dragging = false;
            if (step > 0) {
                xCoordinate = getStepCoordinate(xCoordinate, x);
                value = (float) GuiUtils.mapIncrement(xCoordinate, x + 352, x + 864, min, max, increment);
            }
            setValue(value);
        }

        float stepPercent = stepsAnimation.get();
        float targetPercent = targetAnimation.get();
        if (enabled) {
            if (dragging && startedDragging) {
                stepsAnimation = new EaseOutExpo(STEP_POPUP_DURATION, stepPercent, 1, false);
                targetAnimation = new EaseOutExpo(INDICATOR_POPUP_DURATION, targetPercent, 0.6f, false);
                animReset = true;
            } else if (!dragging && hovered) {
                if (targetAnimation.getEnd() != 1) {
                    stepsAnimation = new EaseOutExpo(STEP_POPUP_DURATION, stepPercent, STEP_HEIGHT_HOVER / STEP_HEIGHT_TOTAL, false);
                    targetAnimation = new EaseInOutQuart(INDICATOR_POPUP_DURATION, targetPercent, 1, false);
                    animReset = true;
                }
            } else if (!dragging && animReset) {
                stepsAnimation = new EaseOutExpo(STEP_POPUP_DURATION, stepPercent, 0, false);
                targetAnimation = new EaseOutExpo(INDICATOR_POPUP_DURATION, targetPercent, 0, false);
                animReset = false;
            }
        }

        if (!dragging && !inputField.isToggled()) {
            value = (float) slider.getInput();
            xCoordinate = (int) GuiUtils.clamp(GuiUtils.map(value, min, max, x + 352, x + 864), x + 352, x + 864);
        }
        if (!inputField.isToggled()) {
            inputField.setCurrentValue(value);
        }

        if (stepSlideAnimation.isFinished() && lastSliderTarget != -1 && lastSliderTarget != xCoordinate && (!dragging || startedDragging || step > 0) && lastX == x) {
            animationStart = lastSliderTarget;
            stepSlideAnimation = new EaseInOutCubic(300, 0f, 1f, false);
        }
        float progress = stepSlideAnimation.get();
        lastSliderTarget = xCoordinate;
        lastX = x;
        xCoordinate = (int) (xCoordinate * progress + animationStart * (1 - progress));

        float radius = 4;
        if (step > 0) {
            radius *= 1 - (Math.min(stepPercent, STEP_HEIGHT_HOVER / STEP_HEIGHT_TOTAL) * STEP_HEIGHT_TOTAL / STEP_HEIGHT_HOVER);
        }

        NanoVGRenderUtil.drawText(vg, name, x, y + 17, nameColor, 14f);
        NanoVGRenderUtil.drawRoundedRect(vg, x + 352, y + 13, 512, 4, Colors.GRAY_300, radius);
        NanoVGRenderUtil.drawRoundedRect(vg, x + 352, y + 13 - 1, xCoordinate - x - 352, 6, Colors.PRIMARY_500, 4f);

        if (step > 0 && stepPercent > 0.05f) {
            float stepOffset = stepPercent * 16;
            for (float i = x + 354; i <= x + 864; i += 512 / ((max - min) / step)) {
                int color = xCoordinate > i - 2 ? Colors.PRIMARY_500 : Colors.GRAY_300;
                NanoVGRenderUtil.drawRoundedRect(vg, i - 2, y + 16 - 1 - (stepOffset / 2f), 4, stepOffset, color, 2f);
            }
        }

        NanoVGRenderUtil.drawRoundedRect(vg, xCoordinate - 12, y + 4, 24, 24, Colors.WHITE, 12f);
        if (targetPercent > 0.02f) {
            float size = TOUCH_TARGET_HOVER * targetPercent;
            NanoVGRenderUtil.drawRoundedRect(vg, xCoordinate - size / 2, y + 16 - size / 2, size, size, NanoVGRenderUtil.alpha(Colors.PRIMARY_500, (int) (120 * targetPercent)), 12f);
        }

        inputField.draw(vg, x + 892, y, inputHandler);
        NanoVGRenderUtil.setAlpha(vg, 1f);
    }

    private int getStepCoordinate(int xCoordinate, int x) {
        Integer nearest = null;
        float stride = 512 / ((max - min) / step);
        for (float i = x + 352; i <= x + 864; i += stride) {
            if (nearest == null || Math.abs(xCoordinate - i) < Math.abs(xCoordinate - nearest))
                nearest = (int) i;
        }
        return nearest == null ? 0 : nearest;
    }

    private void setValue(float value) {
        slider.setValue(value);
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        inputField.keyTyped(key, keyCode);
    }

    @Override
    public void finishUpAndClose() {
        inputField.onClose();
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public boolean hasFocus() {
        return inputField.isToggled();
    }
}
