package laoqi123.render.utils;

import laoqi123.render.type.Color4b;

import java.awt.Color;

public class ColorUtils {
    public static final int[] hexColors = new int[16];

    static {
        for (int i = 0; i < 16; i++) {
            int baseColor = ((i >> 3) & 1) * 85;
            int red = ((i >> 2) & 1) * 170 + baseColor + (i == 6 ? 85 : 0);
            int green = ((i >> 1) & 1) * 170 + baseColor;
            int blue = (i & 1) * 170 + baseColor;
            hexColors[i] = (red & 255) << 16 | (green & 255) << 8 | (blue & 255);
        }
    }

    public static Color4b rainbow() {
        return rainbow(1f);
    }

    public static Color4b rainbow(float alpha) {
        return Color4b.ofHSB(
                (float) (System.nanoTime() / 10_000_000_000.0) % 1.0F,
                1f,
                1f,
                alpha
        );
    }

    public static Color4b shiftHue(Color4b color4b, int shift) {
        float[] hsb = Color.RGBtoHSB(color4b.r(), color4b.g(), color4b.b(), null);
        return Color4b.ofHSB(
                (hsb[0] + shift / 360f) % 1f,
                hsb[1],
                hsb[2],
                color4b.a() / 255f
        );
    }

    public static Color4b interpolateHue(Color4b primaryColor, Color4b otherColor, float percentageOther) {
        float[] hsb1 = Color.RGBtoHSB(primaryColor.r(), primaryColor.g(), primaryColor.b(), null);
        float[] hsb2 = Color.RGBtoHSB(otherColor.r(), otherColor.g(), otherColor.b(), null);

        float h = hsb1[0] + (hsb2[0] - hsb1[0]) * percentageOther;
        float s = hsb1[1] + (hsb2[1] - hsb1[1]) * percentageOther;
        float v = hsb1[2] + (hsb2[2] - hsb1[2]) * percentageOther;
        float alpha = primaryColor.a() + (otherColor.a() - primaryColor.a()) * percentageOther;

        int rgb = Color.HSBtoRGB(h, s, v);
        return new Color4b(
                (rgb >> 16) & 0xFF,
                (rgb >> 8) & 0xFF,
                rgb & 0xFF,
                (int) alpha
        );
    }
}
