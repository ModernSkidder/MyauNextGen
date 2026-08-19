package laoqi123.ui.renderer;

import laoqi123.ui.Colors;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class NanoVGRenderUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static long vg;
    private static int fontSans = -1;
    private static boolean failed = false;
    private static ByteBuffer fontData;
    private static final NVGColor color = NVGColor.calloc();
    private static final NVGColor color2 = NVGColor.calloc();
    private static final org.lwjgl.nanovg.NVGPaint paint = org.lwjgl.nanovg.NVGPaint.create();
    private static final FloatBuffer bounds = BufferUtils.createFloatBuffer(4);

    private NanoVGRenderUtil() {
    }

    public static long vg() {
        ensure();
        return vg;
    }

    private static void ensure() {
        if (vg != 0 || failed) return;
        try {
            vg = NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS | NanoVGGL3.NVG_STENCIL_STROKES);
            if (vg == 0) {
                failed = true;
                return;
            }
            loadFonts();
        } catch (Throwable t) {
            failed = true;
            vg = 0;
        }
    }

    private static void loadFonts() {
        try (InputStream is = NanoVGRenderUtil.class.getResourceAsStream("/assets/myaunextgen/font/GoogleSans-Regular.ttf")) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                fontData = ByteBuffer.allocateDirect(bytes.length);
                fontData.put(bytes).flip();
                fontSans = NanoVG.nvgCreateFontMem(vg, "sans", fontData, false);
            }
        } catch (Throwable ignored) {
        }
        if (fontSans < 0) fontSans = 0;
    }

    public static boolean beginFrame() {
        ensure();
        if (vg == 0) return false;
        int fbW = mc.getWindow().getFramebufferWidth();
        int fbH = mc.getWindow().getFramebufferHeight();
        int scW = mc.getWindow().getScaledWidth();
        int scH = mc.getWindow().getScaledHeight();
        float dpr = (float) fbW / Math.max(1, scW);
        NanoVG.nvgBeginFrame(vg, scW, scH, dpr);
        NanoVG.nvgResetTransform(vg);
        NanoVG.nvgResetScissor(vg);
        return true;
    }

    public static void endFrame() {
        if (vg == 0) return;
        NanoVG.nvgEndFrame(vg);
    }

    public static void setAlpha(long vg, float alpha) {
        NanoVG.nvgGlobalAlpha(vg, alpha);
    }

    public static void scale(long vg, float x, float y) {
        NanoVG.nvgScale(vg, x, y);
    }

    public static void translate(long vg, float x, float y) {
        NanoVG.nvgTranslate(vg, x, y);
    }

    public static void rotate(long vg, float angle) {
        NanoVG.nvgRotate(vg, angle);
    }

    public static void resetTransform(long vg) {
        NanoVG.nvgResetTransform(vg);
    }

    public static void save(long vg) {
        NanoVG.nvgSave(vg);
    }

    public static void restore(long vg) {
        NanoVG.nvgRestore(vg);
    }

    public static void scissor(long vg, float x, float y, float w, float h) {
        NanoVG.nvgScissor(vg, x, y, w, h);
    }

    public static void resetScissor(long vg) {
        NanoVG.nvgResetScissor(vg);
    }

    public static void setFill(long vg, int argb) {
        setColor(argb);
        NanoVG.nvgFillColor(vg, color);
    }

    public static void setStroke(long vg, int argb) {
        setColor(argb);
        NanoVG.nvgStrokeColor(vg, color);
    }

    private static void setColor(int argb) {
        color.r((argb >> 16 & 0xFF) / 255f).g((argb >> 8 & 0xFF) / 255f).b((argb & 0xFF) / 255f).a((argb >>> 24) / 255f);
    }

    public static void rgba(NVGColor c, float r, float g, float b, float a) {
        c.r(r).g(g).b(b).a(a);
    }

    public static void drawRect(long vg, float x, float y, float w, float h, int argb) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRect(vg, x, y, w, h);
        setFill(vg, argb);
        NanoVG.nvgFill(vg);
    }

    public static void drawRoundedRect(long vg, float x, float y, float w, float h, int argb, float radius) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRoundedRect(vg, x, y, w, h, radius);
        setFill(vg, argb);
        NanoVG.nvgFill(vg);
    }

    public static void drawRoundedRectVaried(long vg, float x, float y, float w, float h, int argb, float rTL, float rTR, float rBR, float rBL) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRoundedRectVarying(vg, x, y, w, h, rTL, rTR, rBR, rBL);
        setFill(vg, argb);
        NanoVG.nvgFill(vg);
    }

    public static void drawHollowRoundRect(long vg, float x, float y, float w, float h, int argb, float radius, float thickness) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRoundedRect(vg, x, y, w, h, radius);
        setStroke(vg, argb);
        NanoVG.nvgStrokeWidth(vg, thickness);
        NanoVG.nvgStroke(vg);
    }

    public static void drawCircle(long vg, float x, float y, float radius, int argb) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgCircle(vg, x, y, radius);
        setFill(vg, argb);
        NanoVG.nvgFill(vg);
    }

    public static void drawHollowCircle(long vg, float x, float y, float radius, int argb, float thickness) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgCircle(vg, x, y, radius);
        setStroke(vg, argb);
        NanoVG.nvgStrokeWidth(vg, thickness);
        NanoVG.nvgStroke(vg);
    }

    public static void drawEllipse(long vg, float x, float y, float rx, float ry, int argb) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgEllipse(vg, x, y, rx, ry);
        setFill(vg, argb);
        NanoVG.nvgFill(vg);
    }

    public static void drawLine(long vg, float x1, float y1, float x2, float y2, float width, int argb) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgMoveTo(vg, x1, y1);
        NanoVG.nvgLineTo(vg, x2, y2);
        setStroke(vg, argb);
        NanoVG.nvgStrokeWidth(vg, width);
        NanoVG.nvgLineCap(vg, NanoVG.NVG_ROUND);
        NanoVG.nvgStroke(vg);
    }

    public static void drawText(long vg, String text, float x, float y, int argb, float size) {
        NanoVG.nvgFontFace(vg, "sans");
        NanoVG.nvgFontSize(vg, size);
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_BASELINE);
        setFill(vg, argb);
        NanoVG.nvgText(vg, x, y, text);
    }

    public static void drawCenteredText(long vg, String text, float x, float y, int argb, float size) {
        NanoVG.nvgFontFace(vg, "sans");
        NanoVG.nvgFontSize(vg, size);
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_BASELINE);
        setFill(vg, argb);
        NanoVG.nvgText(vg, x, y, text);
    }

    public static float getTextWidth(long vg, String text, float size) {
        NanoVG.nvgFontFace(vg, "sans");
        NanoVG.nvgFontSize(vg, size);
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_BASELINE);
        bounds.clear();
        NanoVG.nvgTextBounds(vg, 0, 0, text, bounds);
        return bounds.get(2) - bounds.get(0);
    }

    public static void drawDropShadow(long vg, float x, float y, float w, float h, float blur, float spread, float cornerRadius) {
        float f = blur * 2;
        rgba(color, 0f, 0f, 0f, 0.6f);
        rgba(color2, 0f, 0f, 0f, 0f);
        NanoVG.nvgBoxGradient(vg, x, y + 2, w, h, cornerRadius + blur, f, color, color2, paint);
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRect(vg, x - blur, y - blur, w + blur * 2, h + blur * 2);
        NanoVG.nvgRoundedRect(vg, x, y, w, h, cornerRadius);
        NanoVG.nvgPathWinding(vg, NanoVG.NVG_HOLE);
        NanoVG.nvgFillPaint(vg, paint);
        NanoVG.nvgFill(vg);
    }

    public static int alpha(int color, int alpha) {
        return color & 0x00FFFFFF | (alpha & 0xFF) << 24;
    }
}
