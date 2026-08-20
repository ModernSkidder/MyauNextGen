package laoqi123.ui.elements.config;

import laoqi123.value.properties.FloatRangeValue;
import laoqi123.value.properties.IntRangeValue;
import laoqi123.ui.Colors;
import laoqi123.ui.GuiUtils;
import laoqi123.ui.InputHandler;
import laoqi123.ui.renderer.NanoVGRenderUtil;

/**
 * 双滑块范围控件,用于 IntRangeValue / FloatRangeValue(min..max)。
 */
public class ConfigRangeSlider extends ConfigOption {
    private static final int TRACK_X = 352;
    private static final int TRACK_W = 512;

    private final boolean isFloat;
    private final float boundMin;
    private final float boundMax;
    private boolean draggingMin = false;
    private boolean draggingMax = false;
    private boolean mouseWasDown = false;

    public ConfigRangeSlider(IntRangeValue property, int size) {
        super(property, size);
        this.isFloat = false;
        this.boundMin = property.getBoundMin();
        this.boundMax = property.getBoundMax();
    }

    public ConfigRangeSlider(FloatRangeValue property, int size) {
        super(property, size);
        this.isFloat = true;
        this.boundMin = property.getBoundMin();
        this.boundMax = property.getBoundMax();
    }

    private float getMin() {
        if (value instanceof IntRangeValue) return ((IntRangeValue) value).getMin();
        return ((FloatRangeValue) value).getMin();
    }

    private float getMax() {
        if (value instanceof IntRangeValue) return ((IntRangeValue) value).getMax();
        return ((FloatRangeValue) value).getMax();
    }

    private void setRange(float min, float max) {
        if (value instanceof IntRangeValue) {
            ((IntRangeValue) value).setValue(new int[]{Math.round(min), Math.round(max)});
        } else {
            ((FloatRangeValue) value).setValue(new float[]{min, max});
        }
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        if (!enabled) NanoVGRenderUtil.setAlpha(vg, 0.5f);
        NanoVGRenderUtil.drawText(vg, name, x, y + 16, nameColor, 14f);

        float min = getMin();
        float max = getMax();
        int trackLeft = x + TRACK_X;
        int trackRight = x + TRACK_X + TRACK_W;

        float minX = GuiUtils.clamp(GuiUtils.map(min, boundMin, boundMax, trackLeft, trackRight), trackLeft, trackRight);
        float maxX = GuiUtils.clamp(GuiUtils.map(max, boundMin, boundMax, trackLeft, trackRight), trackLeft, trackRight);

        boolean isMouseDown = inputHandler.isButtonDown(0);
        float mouseX = inputHandler.mouseX();

        if (hoveredOrDragging(x, y, inputHandler) && isMouseDown && !mouseWasDown) {
            float dMin = Math.abs(mouseX - minX);
            float dMax = Math.abs(mouseX - maxX);
            if (dMin <= dMax) {
                draggingMin = true;
            } else {
                draggingMax = true;
            }
        }
        if (draggingMin && !isMouseDown) {
            draggingMin = false;
        }
        if (draggingMax && !isMouseDown) {
            draggingMax = false;
        }
        mouseWasDown = isMouseDown;

        if (draggingMin) {
            float mapped = GuiUtils.mapIncrement(mouseX, trackLeft, trackRight, boundMin, boundMax, isFloat ? 0.001f : 1f);
            mapped = GuiUtils.clamp(mapped, boundMin, max);
            min = mapped;
            minX = GuiUtils.clamp(GuiUtils.map(min, boundMin, boundMax, trackLeft, trackRight), trackLeft, trackRight);
        } else if (draggingMax) {
            float mapped = GuiUtils.mapIncrement(mouseX, trackLeft, trackRight, boundMin, boundMax, isFloat ? 0.001f : 1f);
            mapped = GuiUtils.clamp(mapped, min, boundMax);
            max = mapped;
            maxX = GuiUtils.clamp(GuiUtils.map(max, boundMin, boundMax, trackLeft, trackRight), trackLeft, trackRight);
        }
        if (draggingMin || draggingMax) {
            setRange(min, max);
        }

        // 轨道
        NanoVGRenderUtil.drawRoundedRect(vg, trackLeft, y + 13, TRACK_W, 4, Colors.GRAY_300, 2f);
        // 区间高亮
        NanoVGRenderUtil.drawRoundedRect(vg, minX, y + 13 - 1, maxX - minX, 6, Colors.PRIMARY_500, 3f);

        // 两个滑块
        NanoVGRenderUtil.drawRoundedRect(vg, minX - 5, y + 3, 10, 24, Colors.WHITE, 5f);
        NanoVGRenderUtil.drawRoundedRect(vg, maxX - 5, y + 3, 10, 24, Colors.WHITE, 5f);

        // 值显示
        String text;
        if (isFloat) {
            text = String.format("%.2f .. %.2f", min, max);
        } else {
            text = String.format("%d .. %d", Math.round(min), Math.round(max));
        }
        NanoVGRenderUtil.drawText(vg, text, x + TRACK_X + TRACK_W + 24, y + 17, Colors.WHITE_80, 13f);
        NanoVGRenderUtil.setAlpha(vg, 1f);
    }

    private boolean hoveredOrDragging(int x, int y, InputHandler inputHandler) {
        return draggingMin || draggingMax || inputHandler.isAreaHovered(x + TRACK_X - 12, y, TRACK_W + 12, 32) && enabled;
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
