package laoqi123.ui.elements;

import laoqi123.ui.ColorPalette;
import laoqi123.ui.Colors;
import laoqi123.ui.InputHandler;
import laoqi123.ui.renderer.Icons;
import laoqi123.ui.renderer.NanoVGRenderUtil;

public class BasicButton extends BasicElement {
    protected String text;
    protected int icon1;
    protected int icon2;
    private final int alignment;
    private final float fontSize;
    private final float xSpacing;
    private final float xPadding;
    private final int iconSize;
    public float x;
    public float y;

    public static final int ALIGNMENT_LEFT = 0;
    public static final int ALIGNMENT_CENTER = 2;
    public static final int ALIGNMENT_JUSTIFIED = 3;
    public static final int SIZE_32 = 32;
    public static final int SIZE_36 = 36;

    private boolean toggleable = false;
    private Runnable runnable;

    public BasicButton(int width, int size, String text, int icon1, int icon2, int align, ColorPalette colorPalette) {
        super(width, size, colorPalette, true, size == SIZE_36 ? 10f : 8f);
        this.text = text;
        this.icon1 = icon1;
        this.icon2 = icon2;
        this.alignment = align;
        this.xSpacing = 8;
        this.xPadding = 16;
        this.iconSize = size / 2;
        this.fontSize = size / 2f - 4;
    }

    public BasicButton(int width, int size, String text, int align, ColorPalette colorPalette) {
        this(width, size, text, 0, 0, align, colorPalette);
    }

    public BasicButton(int width, int size, int icon, int align, ColorPalette colorPalette) {
        this(width, size, null, icon, 0, align, colorPalette);
    }

    @Override
    public void draw(long vg, float x, float y, InputHandler inputHandler) {
        this.x = x;
        this.y = y;
        this.update(x, y, inputHandler);
        if (disabled) NanoVGRenderUtil.setAlpha(vg, 0.5f);
        int color;
        if (colorPalette == ColorPalette.TERTIARY || colorPalette == ColorPalette.TERTIARY_DESTRUCTIVE) {
            color = currentColor;
        } else {
            NanoVGRenderUtil.drawRoundedRect(vg, x, y, this.width, this.height, currentColor, this.radius);
            color = NanoVGRenderUtil.alpha(Colors.WHITE, (int) (colorAnimation.getAlpha() * 255));
        }
        final float middle = x + width / 2f;
        final float middleYIcon = y + height / 2f - iconSize / 2f;
        final float middleYText = y + height / 2f + fontSize / 8f;
        float contentWidth = 0f;
        if (this.text != null) {
            contentWidth += NanoVGRenderUtil.getTextWidth(vg, text, fontSize);
        }
        if (alignment == ALIGNMENT_CENTER) {
            if (icon1 != 0 && icon2 == 0 && text == null) {
                drawIcon(vg, icon1, middle - iconSize / 2f, middleYIcon, iconSize, color);
            } else {
                if (icon1 != 0) contentWidth += iconSize + xSpacing;
                if (icon2 != 0) contentWidth += iconSize + xSpacing;
                if (text != null)
                    NanoVGRenderUtil.drawText(vg, text, middle - contentWidth / 2 + (icon1 == 0 ? 0 : iconSize + xSpacing), middleYText, color, fontSize);
                if (icon1 != 0) drawIcon(vg, icon1, middle - contentWidth / 2, middleYIcon, iconSize, color);
                if (icon2 != 0) drawIcon(vg, icon2, middle + contentWidth / 2 - iconSize, middleYIcon, iconSize, color);
            }
        } else if (alignment == ALIGNMENT_JUSTIFIED) {
            if (text != null)
                NanoVGRenderUtil.drawText(vg, text, middle - contentWidth / 2, middleYText, color, fontSize);
            if (icon1 != 0) drawIcon(vg, icon1, x + xPadding, middleYIcon, iconSize, color);
            if (icon2 != 0) drawIcon(vg, icon2, x + width - xPadding - iconSize, middleYIcon, iconSize, color);
        } else {
            contentWidth = xPadding;
            if (icon1 != 0) {
                drawIcon(vg, icon1, x + contentWidth, middleYIcon, iconSize, color);
                contentWidth += iconSize + xSpacing;
            }
            if (text != null) {
                NanoVGRenderUtil.drawText(vg, text, x + contentWidth, middleYText, color, fontSize);
            }
            if (icon2 != 0) drawIcon(vg, icon2, x + width - xPadding - iconSize, middleYIcon, iconSize, color);
        }
        if (disabled) NanoVGRenderUtil.setAlpha(vg, 1f);
    }

    protected void drawIcon(long vg, int icon, float x, float y, float size, int color) {
        switch (icon) {
            case 1: Icons.search(vg, x, y, size, color); break;
            case 2: Icons.arrowLeft(vg, x, y, size, color); break;
            case 3: Icons.arrowRight(vg, x, y, size, color); break;
            case 4: Icons.close(vg, x, y, size, color); break;
            case 5: Icons.check(vg, x, y, size, color); break;
            case 6: Icons.chevronUp(vg, x, y, size, color); break;
            case 7: Icons.chevronDown(vg, x, y, size, color); break;
            case 8: Icons.keystroke(vg, x, y, size, color); break;
            case 9: Icons.caretRight(vg, x, y, size, color); break;
            case 10: Icons.category(vg, laoqi123.module.Category.COMBAT, x, y, size, color); break;
            case 11: Icons.category(vg, laoqi123.module.Category.MOVEMENT, x, y, size, color); break;
            case 12: Icons.category(vg, laoqi123.module.Category.RENDER, x, y, size, color); break;
            case 13: Icons.category(vg, laoqi123.module.Category.PLAYER, x, y, size, color); break;
            case 14: Icons.category(vg, laoqi123.module.Category.MISC, x, y, size, color); break;
            default: break;
        }
    }

    @Override
    public void onClick() {
        if (disabled) return;
        if (this.runnable != null) {
            runnable.run();
        }
        if (toggleable && toggled) setColorPalette(ColorPalette.PRIMARY);
        else if (toggleable) setColorPalette(ColorPalette.SECONDARY);
    }

    @Override
    public void setToggled(boolean toggled) {
        this.toggled = toggled;
        if (toggled && toggleable) setColorPalette(ColorPalette.PRIMARY);
        else if (toggleable) setColorPalette(ColorPalette.SECONDARY);
    }

    public void setToggleable(boolean state) {
        this.toggleable = state;
    }

    public void setClickAction(Runnable runnable) {
        this.runnable = runnable;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setLeftIcon(int icon) {
        icon1 = icon;
    }

    public void setRightIcon(int icon) {
        icon2 = icon;
    }

    public boolean hasClickAction() {
        return runnable != null;
    }
}
