package laoqi123.ui.animations;

import laoqi123.ui.ColorPalette;

public class ColorAnimation {
    private ColorPalette palette;
    private final int duration;
    private int prevState = 0;
    private Animation redAnimation;
    private Animation greenAnimation;
    private Animation blueAnimation;
    private Animation alphaAnimation;

    public ColorAnimation(ColorPalette palette, int duration) {
        this.palette = palette;
        this.duration = duration;
        redAnimation = new DummyAnimation(palette.getNormalColorf()[0]);
        greenAnimation = new DummyAnimation(palette.getNormalColorf()[1]);
        blueAnimation = new DummyAnimation(palette.getNormalColorf()[2]);
        alphaAnimation = new DummyAnimation(palette.getNormalColorf()[3]);
    }

    public ColorAnimation(ColorPalette palette) {
        this(palette, 100);
    }

    public int getColor(boolean hovered, boolean pressed) {
        int state = pressed ? 2 : hovered ? 1 : 0;
        if (state != prevState) {
            float[] newColors = pressed ? palette.getPressedColorf() : hovered ? palette.getHoveredColorf() : palette.getNormalColorf();
            redAnimation = new EaseInOutQuad(duration, redAnimation.get(), newColors[0], false);
            greenAnimation = new EaseInOutQuad(duration, greenAnimation.get(), newColors[1], false);
            blueAnimation = new EaseInOutQuad(duration, blueAnimation.get(), newColors[2], false);
            alphaAnimation = new EaseInOutQuad(duration, alphaAnimation.get(), newColors[3], false);
            prevState = state;
        }
        return ((int) (alphaAnimation.get() * 255) << 24) | ((int) (redAnimation.get() * 255) << 16) | ((int) (greenAnimation.get() * 255) << 8) | ((int) (blueAnimation.get() * 255));
    }

    public float getAlpha() {
        return alphaAnimation.get(0);
    }

    public void setPalette(ColorPalette palette) {
        if (this.palette == palette) return;
        this.palette = palette;
        prevState = 3;
    }

    public void setColors(float[] colors) {
        redAnimation = new DummyAnimation(colors[0]);
        greenAnimation = new DummyAnimation(colors[1]);
        blueAnimation = new DummyAnimation(colors[2]);
        alphaAnimation = new DummyAnimation(colors[3]);
    }
}
