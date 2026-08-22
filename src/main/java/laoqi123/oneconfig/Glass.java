package laoqi123.oneconfig;

import laoqi123.font.UFontRenderer;
import laoqi123.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Draws Myau's overlays exactly the way OneConfig draws its own HUDs.
 *
 * <p>The spec is taken from {@code Hud.kt}'s defaults rather than from OneConfig's
 * in-window UI: a HUD is a 50% black rounded rectangle with 4px corners, 4px padding
 * and white text. {@code hudBackground()} is literally just
 * {@code background(PolyColor(bgColor, bgChroma, bgChromaSpeed), bgRadius)} - there is
 * no border, no vignette and no drop shadow.
 *
 * <p>There is deliberately no background blur. OneConfig's frosted panels come from
 * Skia ({@code BlurRenderer} downsamples the render target and runs an ImageFilter blur
 * onto its own Compose surface), which only exists inside a Compose scene. Minecraft's
 * {@code GameRenderer.renderBlur()} is a whole-screen pass that cannot be clipped to a
 * HUD rect, so using it here bleeds blur across the screen. OneConfig's own HUDs are
 * not blurred either.
 */
public final class Glass {

    private Glass() {
    }

    // ------------------------------------------------------ OneConfig HUD defaults

    /** {@code Hud.bgColor} default: 50% black. */
    public static final int BG = 0x80000000;

    /** {@code Hud.bgRadius} default. */
    public static final float RADIUS = 4.0F;

    /** {@code TextHud} padding default, applied on all four sides. */
    public static final float PAD = 4.0F;

    /** {@code Hud.textColor} default. */
    public static final int TEXT = 0xFFFFFFFF;

    /** Muted text, matching the theme's {@code textColorSecondary}. */
    public static final int TEXT_SECONDARY = 0xFF757883;

    /** How long a full chroma hue cycle takes, matching {@code CHROMA_CYCLE_SECONDS}. */
    private static final double CHROMA_CYCLE_SECONDS = 10.0;

    // -------------------------------------------------------------------- typography

    /**
     * OneConfig's UI font. Its theme uses Poppins ({@code UITypography}), which is what
     * makes its text read as thin and modern; Minecraft's bitmap font looks heavy and
     * pixelated next to it.
     *
     * <p>Loaded lazily on first use because {@link UFontRenderer} builds a glyph atlas,
     * which needs a render thread. A failure leaves this null and everything falls back
     * to the vanilla font renderer.
     */
    private static UFontRenderer font;
    private static boolean fontLoaded;

    private static UFontRenderer font() {
        if (!fontLoaded) {
            fontLoaded = true;
            try {
                font = new UFontRenderer("Poppins-Regular", FONT_SIZE);
            } catch (Throwable t) {
                font = null;
            }
        }
        return font;
    }

    /**
     * Poppins is rendered at 2x and drawn at half scale, which is how the existing
     * {@link UFontRenderer} users get a crisp result on a HiDPI atlas.
     */
    private static final int FONT_SIZE = 18;

    // ------------------------------------------------------------------ primitives

    /** A HUD panel: 50% black, 4px corners. */
    public static void panel(float x, float y, float width, float height) {
        panel(x, y, width, height, BG, RADIUS);
    }

    public static void panel(float x, float y, float width, float height, int color) {
        panel(x, y, width, height, color, RADIUS);
    }

    public static void panel(float x, float y, float width, float height, int color, float radius) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        RenderUtil.drawRoundedRect(x, y, width, height, radius, color);
    }

    /**
     * A progress bar: a dim track with a filled portion, both pill-shaped. Used for
     * health bars and similar readouts.
     *
     * @param progress 0..1, clamped
     */
    public static void bar(float x, float y, float width, float height, int color, float progress) {
        float radius = Math.min(height / 2.0F, RADIUS);
        RenderUtil.drawRoundedRect(x, y, width, height, radius, BG);
        float filled = width * clamp01(progress);
        if (filled > 0.0F) {
            RenderUtil.drawRoundedRect(x, y, Math.max(filled, height), height, radius, color);
        }
    }

    // ------------------------------------------------------------------------ text

    /** Draw text in OneConfig's HUD style: Poppins, white, no shadow. */
    public static void text(DrawContext context, String value, float x, float y) {
        text(context, value, x, y, TEXT);
    }

    public static void text(DrawContext context, String value, float x, float y, int color) {
        UFontRenderer poppins = font();
        if (poppins != null) {
            poppins.drawString(value, x, y, color);
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        context.drawText(mc.textRenderer, value, (int) x, (int) y, color, false);
    }

    public static int textWidth(String value) {
        UFontRenderer poppins = font();
        return poppins != null
                ? poppins.getStringWidth(value)
                : MinecraftClient.getInstance().textRenderer.getWidth(value);
    }

    public static int textHeight() {
        UFontRenderer poppins = font();
        return poppins != null
                ? poppins.getHeight()
                : MinecraftClient.getInstance().textRenderer.fontHeight;
    }

    // --------------------------------------------------------------------- colours

    /** Scale an ARGB colour's alpha by {@code factor}. */
    public static int withAlpha(int argb, float factor) {
        int alpha = Math.round(((argb >>> 24) & 0xFF) * clamp01(factor));
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    /** Replace a colour's alpha with an absolute 0..255 value. */
    public static int alpha(int argb, int value) {
        return ((value & 0xFF) << 24) | (argb & 0xFFFFFF);
    }

    /**
     * The chroma cycle OneConfig uses when a colour has chroma enabled: the colour's own
     * hue advances one full turn every {@value #CHROMA_CYCLE_SECONDS} seconds, keeping
     * its saturation, value and alpha.
     *
     * @param speed 1.0 is OneConfig's default rate
     */
    public static int chroma(int argb, float speed) {
        int a = (argb >>> 24) & 0xFF;
        float[] hsb = java.awt.Color.RGBtoHSB(
                (argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, null);
        double elapsed = System.nanoTime() / 1.0E9;
        double hue = (hsb[0] + elapsed * Math.max(0.0F, speed) / CHROMA_CYCLE_SECONDS) % 1.0;
        int rgb = java.awt.Color.HSBtoRGB((float) hue, hsb[1], hsb[2]);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
