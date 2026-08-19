package laoqi123.ui.pages;

import laoqi123.ui.ClickGui;
import laoqi123.ui.ColorPalette;
import laoqi123.ui.Colors;
import laoqi123.ui.InputHandler;
import laoqi123.ui.animations.Animation;
import laoqi123.ui.animations.ColorAnimation;
import laoqi123.ui.animations.EaseOutQuad;
import laoqi123.ui.renderer.NanoVGRenderUtil;
import org.lwjgl.nanovg.NanoVG;

import java.util.ArrayList;
import java.util.Objects;

public abstract class Page {
    protected final String title;
    protected Animation scrollAnimation;
    private final ColorAnimation colorAnimation = new ColorAnimation(new ColorPalette(Colors.TRANSPARENT, Colors.GRAY_400_60, Colors.GRAY_400_60), 200);
    protected float scrollTarget;
    private long scrollTime;
    private boolean mouseWasDown;
    private boolean dragging;
    private float yStart;
    protected float scroll;
    public final ArrayList<Page> parents = new ArrayList<>();
    private String previousSearch = "";

    public Page(String title) {
        this.title = title;
    }

    public abstract void draw(long vg, int x, int y, InputHandler inputHandler);

    public void drawLast(long vg, InputHandler inputHandler) {
    }

    public int drawStatic(long vg, int x, int y, InputHandler inputHandler) {
        return 0;
    }

    public void finishUpAndClose() {
        scroll = 0;
        scrollTarget = 0;
        scrollTime = 0;
        scrollAnimation = null;
    }

    public void scrollWithDraw(long vg, int x, int y, InputHandler inputHandler) {
        int maxScroll = getMaxScrollHeight();
        int scissorOffset = drawStatic(vg, x, y, inputHandler);
        if (ClickGui.INSTANCE != null && !Objects.equals(previousSearch, ClickGui.INSTANCE.getSearchValue())) {
            previousSearch = ClickGui.INSTANCE.getSearchValue();
            finishUpAndClose();
        }
        scroll = scrollAnimation == null ? scrollTarget : scrollAnimation.get();
        final float scrollBarLength = (728f / maxScroll) * 728f;
        NanoVG.nvgSave(vg);
        NanoVG.nvgIntersectScissor(vg, x, y + scissorOffset, 1056, 728 - scissorOffset);
        float dWheel = (float) inputHandler.getDWheel();
        if (dWheel != 0) {
            scrollTarget += dWheel;
            if (scrollTarget > 0f) scrollTarget = 0f;
            else if (scrollTarget < -maxScroll + 728) scrollTarget = -maxScroll + 728;
            scrollAnimation = new EaseOutQuad(150, scroll, scrollTarget, false);
            scrollTime = System.currentTimeMillis();
        } else if (scrollAnimation != null && scrollAnimation.isFinished()) {
            scrollAnimation = null;
        }
        if (maxScroll <= 728) {
            draw(vg, x, y, inputHandler);
            NanoVG.nvgRestore(vg);
            return;
        }
        draw(vg, x, (int) (y + scroll), inputHandler);
        if (dragging && inputHandler.isClicked(true)) dragging = false;
        NanoVG.nvgRestore(vg);
        if (!(scrollBarLength > 727f)) {
            final float scrollBarY = (scroll / maxScroll) * 720f;
            final boolean isMouseDown = inputHandler.isButtonDown(0);
            final boolean scrollHover = inputHandler.isAreaHovered(x + 1042, (int) (y - scrollBarY), 12, (int) scrollBarLength);
            final boolean scrollTimePeriod = (System.currentTimeMillis() - scrollTime < 1000);
            if (scrollHover && isMouseDown && !mouseWasDown) {
                yStart = inputHandler.mouseY();
                dragging = true;
            }
            mouseWasDown = isMouseDown;
            if (dragging) {
                scrollTarget = -(inputHandler.mouseY() - yStart) * maxScroll / 728f;
                if (scrollTarget > 0f) scrollTarget = 0f;
                else if (scrollTarget < -maxScroll + 728) scrollTarget = -maxScroll + 728;
                scrollAnimation = new EaseOutQuad(150, scroll, scrollTarget, false);
            }
            NanoVGRenderUtil.drawRoundedRect(vg, x + 1048, y - scrollBarY, 4, scrollBarLength, colorAnimation.getColor(scrollHover || scrollTimePeriod, dragging), 4f);
        }
    }

    public String getTitle() {
        return title;
    }

    public void keyTyped(char key, int keyCode) {
    }

    public boolean hasFocus() {
        return false;
    }

    public boolean isBase() {
        return false;
    }

    public int getMaxScrollHeight() {
        return 728;
    }
}
