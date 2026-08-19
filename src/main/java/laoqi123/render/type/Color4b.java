package laoqi123.render.type;

import java.awt.Color;

public record Color4b(int r, int g, int b, int a) {
    public static final Color4b LIQUID_BOUNCE = new Color4b(0x00, 0x80, 0xFF, 0xFF);
    public static final Color4b WHITE = new Color4b(255, 255, 255, 255);
    public static final Color4b BLACK = new Color4b(0, 0, 0, 255);
    public static final Color4b RED = new Color4b(255, 0, 0, 255);
    public static final Color4b GREEN = new Color4b(0, 255, 0, 255);
    public static final Color4b BLUE = new Color4b(0, 0, 255, 255);
    public static final Color4b CYAN = new Color4b(0, 255, 255, 255);
    public static final Color4b MAGENTA = new Color4b(255, 0, 255, 255);
    public static final Color4b YELLOW = new Color4b(255, 255, 0, 255);
    public static final Color4b ORANGE = new Color4b(255, 165, 0, 255);
    public static final Color4b PURPLE = new Color4b(128, 0, 128, 255);
    public static final Color4b PINK = new Color4b(255, 192, 203, 255);
    public static final Color4b GRAY = new Color4b(128, 128, 128, 255);
    public static final Color4b LIGHT_GRAY = new Color4b(192, 192, 192, 255);
    public static final Color4b DARK_GRAY = new Color4b(64, 64, 64, 255);
    public static final Color4b TRANSPARENT = new Color4b(0, 0, 0, 0);

    public Color4b {
        r = r & 0xFF;
        g = g & 0xFF;
        b = b & 0xFF;
        a = a & 0xFF;
    }

    public Color4b(Color color) {
        this(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public Color4b(int hex, boolean hasAlpha) {
        this((hex >> 16) & 0xFF, (hex >> 8) & 0xFF, hex & 0xFF, hasAlpha ? (hex >> 24) & 0xFF : 0xFF);
    }

    public Color4b(int hex) {
        this(hex, false);
    }

    public boolean isTransparent() {
        return a <= 0;
    }

    public Color4b with(int newR, int newG, int newB, int newA) {
        return new Color4b(newR, newG, newB, newA);
    }

    public Color4b alpha(int newAlpha) {
        return with(r, g, b, newAlpha);
    }

    public int toARGB() {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public Color4b fade(float fade) {
        return fade >= 1.0f ? this : with(r, g, b, (int) (a * fade));
    }

    public Color4b darker() {
        return new Color4b(darkerChannel(r), darkerChannel(g), darkerChannel(b), a);
    }

    private static int darkerChannel(int value) {
        return Math.max(0, (int) (value * 0.7));
    }

    public Color4b interpolateTo(Color4b other, double percentage) {
        return interpolateTo(other, percentage, percentage, percentage, percentage);
    }

    public Color4b interpolateTo(Color4b other, double tR, double tG, double tB, double tA) {
        return new Color4b(
                (int) Math.max(0, Math.min(255, r + (other.r - r) * tR)),
                (int) Math.max(0, Math.min(255, g + (other.g - g) * tG)),
                (int) Math.max(0, Math.min(255, b + (other.b - b) * tB)),
                (int) Math.max(0, Math.min(255, a + (other.a - a) * tA))
        );
    }

    public Color toAwtColor() {
        return new Color(r, g, b, a);
    }

    public static Color4b fromHex(String hex) {
        String cleanHex = hex.startsWith("#") ? hex.substring(1) : hex;
        boolean hasAlpha = cleanHex.length() == 8;
        if (cleanHex.length() != 6 && !hasAlpha) {
            throw new IllegalArgumentException("Invalid hex color: " + hex);
        }
        if (hasAlpha) {
            long rgba = Long.parseLong(cleanHex, 16);
            return new Color4b((int) rgba, true);
        }
        int rgb = Integer.parseInt(cleanHex, 16);
        return new Color4b((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 0xFF);
    }

    public static Color4b ofHSB(float hue, float saturation, float brightness) {
        return ofHSB(hue, saturation, brightness, 1f);
    }

    public static Color4b ofHSB(float hue, float saturation, float brightness, float alpha) {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        return new Color4b(
                (rgb >> 16) & 0xFF,
                (rgb >> 8) & 0xFF,
                rgb & 0xFF,
                (int) (alpha * 255)
        );
    }
}
