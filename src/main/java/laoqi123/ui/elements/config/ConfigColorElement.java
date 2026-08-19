package laoqi123.ui.elements.config;

import laoqi123.property.properties.ColorProperty;
import laoqi123.ui.Colors;
import laoqi123.ui.GuiUtils;
import laoqi123.ui.InputHandler;
import laoqi123.ui.elements.BasicElement;
import laoqi123.ui.renderer.NanoVGRenderUtil;

public class ConfigColorElement extends ConfigOption {
    private final BasicElement element = new BasicElement(64, 32, false);
    private boolean open = false;
    private float popupX;
    private float popupY;
    private final boolean[] dragging = new boolean[4];
    private static final float POPUP_W = 216;
    private static final float POPUP_H = 148;

    public ConfigColorElement(ColorProperty property, int size) {
        super(property, size);
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        int x1 = size == 1 ? x : x + 512;
        int color = (Integer) property.getValue();
        NanoVGRenderUtil.drawText(vg, name, x, y + 16, nameColor, 14f);

        element.disable(!enabled);
        element.update(x1 + 416, y, inputHandler);
        NanoVGRenderUtil.drawHollowRoundRect(vg, x1 + 415, y - 1, 64, 32, Colors.GRAY_300, 12f, 2f);
        NanoVGRenderUtil.drawRoundedRect(vg, x1 + 420, y + 4, 56, 24, color, 8f);
        if (element.isClicked() && !open) {
            open = true;
            popupX = GuiUtils.clamp(inputHandler.mouseX() - 30, x + 224, x + 1056 - POPUP_W);
            popupY = GuiUtils.clamp(inputHandler.mouseY() + 12, y, y + 728 - POPUP_H);
        }
    }

    @Override
    public void drawLast(long vg, int x, InputHandler inputHandler) {
        if (!open) return;
        boolean clickedOutside = inputHandler.isClicked() && !inputHandler.isAreaHovered(popupX, popupY, POPUP_W, POPUP_H, true);
        if (clickedOutside) {
            open = false;
            return;
        }

        NanoVGRenderUtil.drawRoundedRect(vg, popupX, popupY, POPUP_W, POPUP_H, Colors.GRAY_700, 12);
        NanoVGRenderUtil.drawHollowRoundRect(vg, popupX, popupY, POPUP_W, POPUP_H, NanoVGRenderUtil.alpha(Colors.WHITE, 30), 12, 1);

        int color = (Integer) property.getValue();
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        int a = color >>> 24;

        float swatchX = popupX + 12;
        NanoVGRenderUtil.drawRoundedRect(vg, swatchX, popupY + 10, 70, 18, color, 6);
        String hex = String.format("#%08X", color);
        NanoVGRenderUtil.drawText(vg, hex, popupX + 94, popupY + 22, Colors.WHITE_80, 12);

        int[] channels = {r, g, b, a};
        String[] labels = {"R", "G", "B", "A"};
        for (int i = 0; i < 4; i++) {
            float ty = popupY + 44 + i * 24;
            NanoVGRenderUtil.drawText(vg, labels[i], popupX + 12, ty + 10, Colors.WHITE_60, 10);
            float trackX = popupX + 30;
            float trackW = POPUP_W - 64;
            float ratio = channels[i] / 255f;
            int trackColor = i == 0 ? 0xFFFF5555 : i == 1 ? 0xFF55FF55 : i == 2 ? 0xFF5555FF : 0xFFFFFFFF;
            NanoVGRenderUtil.drawRoundedRect(vg, trackX, ty + 4, trackW, 4, 0xFF494F5C, 2);
            NanoVGRenderUtil.drawRoundedRect(vg, trackX, ty + 4, trackW * ratio, 4, trackColor, 2);
            boolean hovered = inputHandler.isAreaHovered(trackX - 6, ty - 4, trackW + 12, 16, true);
            if (hovered && inputHandler.isClicked(true)) dragging[i] = true;
            if (inputHandler.isClicked() && !hovered) dragging[i] = false;
            if (dragging[i]) {
                float t = GuiUtils.clamp((inputHandler.mouseX() - trackX) / trackW, 0, 1);
                channels[i] = Math.round(t * 255);
                NanoVGRenderUtil.drawRoundedRect(vg, trackX + trackW * t - 5, ty - 1, 10, 14, Colors.WHITE, 5);
            } else {
                NanoVGRenderUtil.drawRoundedRect(vg, trackX + trackW * ratio - 5, ty - 1, 10, 14, Colors.WHITE, 5);
            }
            NanoVGRenderUtil.drawText(vg, String.valueOf(channels[i]), popupX + POPUP_W - 34, ty + 10, Colors.WHITE_80, 10);
        }

        int newColor = (channels[3] & 0xFF) << 24 | (channels[0] & 0xFF) << 16 | (channels[1] & 0xFF) << 8 | (channels[2] & 0xFF);
        if (newColor != color) {
            property.setValue(newColor);
        }
    }

    @Override
    public void finishUpAndClose() {
        open = false;
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
