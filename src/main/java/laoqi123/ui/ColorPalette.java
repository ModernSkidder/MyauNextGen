package laoqi123.ui;

public class ColorPalette {
    public static final ColorPalette TRANSPARENT = new ColorPalette(Colors.TRANSPARENT, Colors.TRANSPARENT, Colors.TRANSPARENT);
    public static final ColorPalette PRIMARY = new ColorPalette(Colors.PRIMARY_600, Colors.PRIMARY_700, Colors.PRIMARY_700_80);
    public static final ColorPalette SECONDARY = new ColorPalette(Colors.GRAY_500, Colors.GRAY_400, Colors.GRAY_400_80);
    public static final ColorPalette TERTIARY = new ColorPalette(Colors.WHITE_80, Colors.WHITE, Colors.WHITE_80);
    public static final ColorPalette PRIMARY_DESTRUCTIVE = new ColorPalette(Colors.ERROR_700, Colors.ERROR_600, Colors.ERROR_600_80);
    public static final ColorPalette SECONDARY_DESTRUCTIVE = new ColorPalette(Colors.GRAY_500, Colors.ERROR_800, Colors.ERROR_800_80);
    public static final ColorPalette TERTIARY_DESTRUCTIVE = new ColorPalette(Colors.WHITE_90, Colors.ERROR_600_80, Colors.ERROR_600_80);

    private final int colorNormal;
    private final int colorHovered;
    private final int colorPressed;
    private final float[] colorNormalf;
    private final float[] colorHoveredf;
    private final float[] colorPressedf;

    public ColorPalette(int colorNormal, int colorHovered, int colorPressed) {
        this.colorNormal = colorNormal;
        this.colorHovered = colorHovered;
        this.colorPressed = colorPressed;
        this.colorNormalf = toFloats(colorNormal);
        this.colorHoveredf = toFloats(colorHovered);
        this.colorPressedf = toFloats(colorPressed);
    }

    private static float[] toFloats(int argb) {
        return new float[]{(argb >> 16 & 0xFF) / 255f, (argb >> 8 & 0xFF) / 255f, (argb & 0xFF) / 255f, (argb >>> 24) / 255f};
    }

    public int getNormalColor() {
        return colorNormal;
    }

    public int getHoveredColor() {
        return colorHovered;
    }

    public int getPressedColor() {
        return colorPressed;
    }

    public float[] getNormalColorf() {
        return colorNormalf;
    }

    public float[] getHoveredColorf() {
        return colorHoveredf;
    }

    public float[] getPressedColorf() {
        return colorPressedf;
    }
}
