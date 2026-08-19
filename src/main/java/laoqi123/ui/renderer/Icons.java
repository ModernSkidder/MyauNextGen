package laoqi123.ui.renderer;

import laoqi123.module.Category;
import org.lwjgl.nanovg.NanoVG;

public class Icons {

    private Icons() {
    }

    public static void search(long vg, float x, float y, float size, int color) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgCircle(vg, x + size * 0.42f, y + size * 0.42f, size * 0.22f);
        NanoVGRenderUtil.setStroke(vg, color);
        NanoVG.nvgStrokeWidth(vg, Math.max(1.5f, size * 0.09f));
        NanoVG.nvgLineCap(vg, NanoVG.NVG_ROUND);
        NanoVG.nvgStroke(vg);
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgMoveTo(vg, x + size * 0.58f, y + size * 0.58f);
        NanoVG.nvgLineTo(vg, x + size * 0.86f, y + size * 0.86f);
        NanoVG.nvgStroke(vg);
    }

    public static void arrowLeft(long vg, float x, float y, float size, int color) {
        chevron(vg, x, y, size, color, 180f);
    }

    public static void arrowRight(long vg, float x, float y, float size, int color) {
        chevron(vg, x, y, size, color, 0f);
    }

    public static void caretRight(long vg, float x, float y, float size, int color) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgMoveTo(vg, x, y);
        NanoVG.nvgLineTo(vg, x + size * 0.6f, y + size / 2f);
        NanoVG.nvgLineTo(vg, x, y + size);
        NanoVGRenderUtil.setStroke(vg, color);
        NanoVG.nvgStrokeWidth(vg, Math.max(1.5f, size * 0.14f));
        NanoVG.nvgLineCap(vg, NanoVG.NVG_ROUND);
        NanoVG.nvgLineJoin(vg, NanoVG.NVG_ROUND);
        NanoVG.nvgStroke(vg);
    }

    public static void chevronUp(long vg, float x, float y, float size, int color) {
        chevron(vg, x, y, size, color, -90f);
    }

    public static void chevronDown(long vg, float x, float y, float size, int color) {
        chevron(vg, x, y, size, color, 90f);
    }

    private static void chevron(long vg, float x, float y, float size, int color, float rotation) {
        NanoVG.nvgSave(vg);
        NanoVG.nvgTranslate(vg, x + size / 2f, y + size / 2f);
        NanoVG.nvgRotate(vg, (float) Math.toRadians(rotation));
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgMoveTo(vg, -size * 0.28f, -size * 0.16f);
        NanoVG.nvgLineTo(vg, size * 0.28f, -size * 0.16f);
        NanoVG.nvgLineTo(vg, 0, size * 0.3f);
        NanoVG.nvgClosePath(vg);
        NanoVGRenderUtil.setFill(vg, color);
        NanoVG.nvgFill(vg);
        NanoVG.nvgRestore(vg);
    }

    public static void close(long vg, float x, float y, float size, int color) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgMoveTo(vg, x + size * 0.22f, y + size * 0.22f);
        NanoVG.nvgLineTo(vg, x + size * 0.78f, y + size * 0.78f);
        NanoVG.nvgMoveTo(vg, x + size * 0.78f, y + size * 0.22f);
        NanoVG.nvgLineTo(vg, x + size * 0.22f, y + size * 0.78f);
        NanoVGRenderUtil.setStroke(vg, color);
        NanoVG.nvgStrokeWidth(vg, Math.max(1.5f, size * 0.12f));
        NanoVG.nvgLineCap(vg, NanoVG.NVG_ROUND);
        NanoVG.nvgStroke(vg);
    }

    public static void check(long vg, float x, float y, float size, int color) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgMoveTo(vg, x + size * 0.18f, y + size * 0.52f);
        NanoVG.nvgLineTo(vg, x + size * 0.42f, y + size * 0.76f);
        NanoVG.nvgLineTo(vg, x + size * 0.84f, y + size * 0.26f);
        NanoVGRenderUtil.setStroke(vg, color);
        NanoVG.nvgStrokeWidth(vg, Math.max(1.8f, size * 0.14f));
        NanoVG.nvgLineCap(vg, NanoVG.NVG_ROUND);
        NanoVG.nvgLineJoin(vg, NanoVG.NVG_ROUND);
        NanoVG.nvgStroke(vg);
    }

    public static void keystroke(long vg, float x, float y, float size, int color) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRoundedRect(vg, x + size * 0.06f, y + size * 0.3f, size * 0.88f, size * 0.4f, size * 0.12f);
        NanoVGRenderUtil.setStroke(vg, color);
        NanoVG.nvgStrokeWidth(vg, Math.max(1.5f, size * 0.09f));
        NanoVG.nvgStroke(vg);
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgCircle(vg, x + size * 0.5f, y + size * 0.5f, size * 0.1f);
        NanoVGRenderUtil.setFill(vg, color);
        NanoVG.nvgFill(vg);
    }

    public static void category(long vg, Category category, float x, float y, float size, int color) {
        switch (category) {
            case COMBAT:
                combat(vg, x, y, size, color);
                break;
            case MOVEMENT:
                movement(vg, x, y, size, color);
                break;
            case RENDER:
                render(vg, x, y, size, color);
                break;
            case PLAYER:
                player(vg, x, y, size, color);
                break;
            default:
                misc(vg, x, y, size, color);
                break;
        }
    }

    private static void combat(long vg, float x, float y, float size, int color) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgMoveTo(vg, x + size * 0.2f, y + size * 0.15f);
        NanoVG.nvgLineTo(vg, x + size * 0.85f, y + size * 0.8f);
        NanoVG.nvgMoveTo(vg, x + size * 0.85f, y + size * 0.15f);
        NanoVG.nvgLineTo(vg, x + size * 0.2f, y + size * 0.8f);
        NanoVGRenderUtil.setStroke(vg, color);
        NanoVG.nvgStrokeWidth(vg, Math.max(1.5f, size * 0.1f));
        NanoVG.nvgLineCap(vg, NanoVG.NVG_ROUND);
        NanoVG.nvgStroke(vg);
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgMoveTo(vg, x + size * 0.12f, y + size * 0.08f);
        NanoVG.nvgLineTo(vg, x + size * 0.3f, y + size * 0.08f);
        NanoVG.nvgMoveTo(vg, x + size * 0.72f, y + size * 0.88f);
        NanoVG.nvgLineTo(vg, x + size * 0.9f, y + size * 0.88f);
        NanoVG.nvgStroke(vg);
    }

    private static void movement(long vg, float x, float y, float size, int color) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgMoveTo(vg, x + size * 0.22f, y + size * 0.78f);
        NanoVG.nvgLineTo(vg, x + size * 0.78f, y + size * 0.22f);
        NanoVG.nvgLineTo(vg, x + size * 0.78f, y + size * 0.5f);
        NanoVG.nvgMoveTo(vg, x + size * 0.78f, y + size * 0.22f);
        NanoVG.nvgLineTo(vg, x + size * 0.5f, y + size * 0.22f);
        NanoVGRenderUtil.setStroke(vg, color);
        NanoVG.nvgStrokeWidth(vg, Math.max(1.5f, size * 0.1f));
        NanoVG.nvgLineCap(vg, NanoVG.NVG_ROUND);
        NanoVG.nvgLineJoin(vg, NanoVG.NVG_ROUND);
        NanoVG.nvgStroke(vg);
    }

    private static void render(long vg, float x, float y, float size, int color) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgEllipse(vg, x + size / 2f, y + size / 2f, size * 0.44f, size * 0.28f);
        NanoVGRenderUtil.setStroke(vg, color);
        NanoVG.nvgStrokeWidth(vg, Math.max(1.5f, size * 0.09f));
        NanoVG.nvgStroke(vg);
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgCircle(vg, x + size / 2f, y + size / 2f, size * 0.12f);
        NanoVGRenderUtil.setFill(vg, color);
        NanoVG.nvgFill(vg);
    }

    private static void player(long vg, float x, float y, float size, int color) {
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgCircle(vg, x + size / 2f, y + size * 0.3f, size * 0.17f);
        NanoVGRenderUtil.setFill(vg, color);
        NanoVG.nvgFill(vg);
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRoundedRect(vg, x + size * 0.22f, y + size * 0.5f, size * 0.56f, size * 0.42f, size * 0.2f);
        NanoVGRenderUtil.setFill(vg, color);
        NanoVG.nvgFill(vg);
    }

    private static void misc(long vg, float x, float y, float size, int color) {
        float startX = x + size * 0.16f;
        float gap = size * 0.34f;
        for (int i = 0; i < 3; i++) {
            float cx = startX + i * gap;
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgMoveTo(vg, cx, y + size * 0.15f);
            NanoVG.nvgLineTo(vg, cx, y + size * 0.85f);
            NanoVGRenderUtil.setStroke(vg, color);
            NanoVG.nvgStrokeWidth(vg, Math.max(1.2f, size * 0.08f));
            NanoVG.nvgLineCap(vg, NanoVG.NVG_ROUND);
            NanoVG.nvgStroke(vg);
        }
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgCircle(vg, startX, y + size * 0.32f, size * 0.1f);
        NanoVGRenderUtil.setFill(vg, color);
        NanoVG.nvgFill(vg);
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgCircle(vg, startX + gap, y + size * 0.68f, size * 0.1f);
        NanoVGRenderUtil.setFill(vg, color);
        NanoVG.nvgFill(vg);
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgCircle(vg, startX + gap * 2, y + size * 0.5f, size * 0.1f);
        NanoVGRenderUtil.setFill(vg, color);
        NanoVG.nvgFill(vg);
    }
}
