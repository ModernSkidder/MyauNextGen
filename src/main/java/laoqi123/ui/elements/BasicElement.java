package laoqi123.ui.elements;

import laoqi123.ui.ColorPalette;
import laoqi123.ui.InputHandler;
import laoqi123.ui.animations.ColorAnimation;
import laoqi123.ui.renderer.NanoVGRenderUtil;

public class BasicElement {
    protected int width;
    protected int height;
    protected ColorPalette colorPalette;
    protected int hitBoxX;
    protected int hitBoxY;
    protected boolean hoverFx;
    protected boolean hovered = false;
    protected boolean pressed = false;
    protected boolean clicked = false;
    protected boolean toggled = false;
    protected boolean disabled = false;
    public int currentColor;
    protected final float radius;
    private boolean block = false;
    protected ColorAnimation colorAnimation;

    public BasicElement(int width, int height, ColorPalette colorPalette, boolean hoverFx) {
        this(width, height, colorPalette, hoverFx, 12f);
    }

    public BasicElement(int width, int height, ColorPalette colorPalette, boolean hoverFx, float radius) {
        this.height = height;
        this.width = width;
        this.colorPalette = colorPalette;
        this.hoverFx = hoverFx;
        this.radius = radius;
        this.colorAnimation = new ColorAnimation(colorPalette);
    }

    public BasicElement(int width, int height, boolean hoverFx) {
        this(width, height, ColorPalette.TRANSPARENT, hoverFx, 12f);
    }

    public void draw(long vg, float x, float y, InputHandler inputHandler) {
        this.update(x, y, inputHandler);
        NanoVGRenderUtil.drawRoundedRect(vg, x, y, width, height, currentColor, radius);
    }

    public void update(float x, float y, InputHandler inputHandler) {
        if (disabled) {
            hovered = false;
            pressed = false;
            clicked = false;
        } else {
            hovered = inputHandler.isAreaHovered(x - hitBoxX, y - hitBoxY, width + hitBoxX, height + hitBoxY);
            pressed = hovered && inputHandler.isButtonDown(0);
            clicked = inputHandler.isClicked(block) && hovered;
            if (clicked) {
                toggled = !toggled;
                onClick();
            }
        }
        if (hoverFx) currentColor = colorAnimation.getColor(hovered, pressed);
        else currentColor = colorAnimation.getColor(false, false);
    }

    public void ignoreBlockedTouches(boolean state) {
        block = state;
    }

    public void onClick() {
    }

    public void setCustomHitbox(int x, int y) {
        hitBoxX = x;
        hitBoxY = y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setColorPalette(ColorPalette colorPalette) {
        if (this.colorPalette == ColorPalette.TERTIARY || this.colorPalette == ColorPalette.TERTIARY_DESTRUCTIVE)
            this.colorAnimation.setColors(colorPalette.getNormalColorf());
        this.colorPalette = colorPalette;
        this.colorAnimation.setPalette(colorPalette);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean isPressed() {
        return pressed;
    }

    public boolean isClicked() {
        return clicked;
    }

    public boolean isToggled() {
        return toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void disable(boolean state) {
        disabled = state;
    }
}
