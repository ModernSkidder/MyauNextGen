package laoqi123.ui;

import laoqi123.Myau;
import laoqi123.module.Category;
import laoqi123.ui.animations.Animation;
import laoqi123.ui.animations.DummyAnimation;
import laoqi123.ui.animations.EaseOutExpo;
import laoqi123.ui.elements.BasicElement;
import laoqi123.ui.elements.text.TextInputField;
import laoqi123.ui.pages.ModConfigPage;
import laoqi123.ui.pages.ModsPage;
import laoqi123.ui.pages.Page;
import laoqi123.ui.renderer.Icons;
import laoqi123.ui.renderer.NanoVGRenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public class ClickGui extends Screen {
    public static ClickGui INSTANCE;
    // 记住上次关闭时的页面,重新打开时回到原位置(例如 Combat/KillAura)
    private static Page lastPage;

    private static final int PAGE_ANIM_START = 224;
    private static final int PAGE_ANIM_END = 1280;
    private static final int PAGE_ANIM_OFFSET = 1056;

    private final SideBar sideBar = new SideBar();
    private final TextInputField textInputField = new TextInputField(248, 40, "Search...", false, false, 1, 12);
    private final ArrayList<Page> previousPages = new ArrayList<>();
    private final ArrayList<Page> nextPages = new ArrayList<>();
    private final BasicElement backArrow = new BasicElement(40, 40, ColorPalette.TERTIARY, true);
    private final BasicElement forwardArrow = new BasicElement(40, 40, ColorPalette.TERTIARY, true);
    private final InputHandler inputHandler = new InputHandler();
    protected Page currentPage;
    protected Page prevPage;
    private Animation pageAnimation;

    public ClickGui() {
        super(Text.empty());
        INSTANCE = this;
        if (lastPage != null && !lastPage.parents.isEmpty()) {
            currentPage = lastPage;
        } else {
            currentPage = new ModsPage(Category.COMBAT);
            currentPage.parents.add(currentPage);
        }
        sideBar.setCategoryCallback(category -> openPage(new ModsPage(category)));
        sideBar.pageOpened(currentPage.parents.get(0).getTitle());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        GuiUtils.updateDeltaTime();
        context.fill(0, 0, this.width, this.height, 0x66000000);
        if (!NanoVGRenderUtil.beginFrame()) {
            context.fill(0, 0, this.width, this.height, 0xFF151617);
            context.drawCenteredTextWithShadow(this.textRenderer, "NanoVG renderer failed to initialize", this.width / 2, this.height / 2, 0xFFFFFFFF);
            return;
        }
        long vg = NanoVGRenderUtil.vg();

        inputHandler.update(mouseX, mouseY);

        float scale = getScaleFactor();
        int x = (int) ((this.width - 1280 * scale) / 2f / scale);
        int y = (int) ((this.height - 800 * scale) / 2f / scale);
        NanoVGRenderUtil.scale(vg, scale, scale);
        inputHandler.scale(scale, scale);

        NanoVGRenderUtil.drawDropShadow(vg, x, y, 1280, 800, 64, 0, 20);
        NanoVGRenderUtil.drawRoundedRect(vg, x, y, 244, 800, Colors.GRAY_800_95, 20f);
        NanoVGRenderUtil.drawRoundedRect(vg, x + 224 - 20, y, 1056 + 20, 800, Colors.GRAY_800, 20f);
        NanoVGRenderUtil.drawLine(vg, x + 224, y + 72, x + 1280, y + 72, 1, Colors.GRAY_700);
        NanoVGRenderUtil.drawLine(vg, x + 224, y, x + 222, y + 800, 1, Colors.GRAY_700);

        NanoVGRenderUtil.drawRoundedRect(vg, x + 27, y + 18, 40, 40, Colors.PRIMARY_600, 10);
        NanoVGRenderUtil.drawCenteredText(vg, "M", x + 47, y + 44, Colors.WHITE, 20);
        NanoVGRenderUtil.drawText(vg, "MYAU " + Myau.version, x + 67, y + 40, Colors.WHITE, 15);

        textInputField.draw(vg, x + 1020, y + 16, inputHandler);
        sideBar.draw(vg, x, y, inputHandler);

        backArrow.update(x + 240, y + 16, inputHandler);
        forwardArrow.update(x + 280, y + 16, inputHandler);
        if (previousPages.isEmpty()) {
            NanoVGRenderUtil.setAlpha(vg, 0.5f);
        } else if (!backArrow.isHovered() || inputHandler.isButtonDown(0)) {
            NanoVGRenderUtil.setAlpha(vg, 0.8f);
        }
        Icons.arrowLeft(vg, x + 250, y + 26, 20, backArrow.currentColor);
        NanoVGRenderUtil.setAlpha(vg, 1f);
        if (nextPages.isEmpty()) {
            NanoVGRenderUtil.setAlpha(vg, 0.5f);
        } else if (!forwardArrow.isHovered() || inputHandler.isButtonDown(0)) {
            NanoVGRenderUtil.setAlpha(vg, 0.8f);
        }
        Icons.arrowRight(vg, x + 290, y + 26, 20, forwardArrow.currentColor);
        NanoVGRenderUtil.setAlpha(vg, 1f);

        handleHistoryMovement(backArrow.isClicked(), forwardArrow.isClicked());

        boolean transitioning = isTransitioning();
        inputHandler.setPageTransition(transitioning);
        inputHandler.setContentArea(x + 224, y + 72, 1056, 728);

        NanoVGRenderUtil.scissor(vg, x + 224, y + 72, 1056, 728);
        if (prevPage != null && pageAnimation != null) {
            float pageProgress = pageAnimation.get();
            if (!pageAnimation.isReversed()) {
                prevPage.scrollWithDraw(vg, (int) (x + pageProgress), y + 72, inputHandler);
                currentPage.scrollWithDraw(vg, (int) (x - PAGE_ANIM_OFFSET + pageProgress), y + 72, inputHandler);
            } else {
                prevPage.scrollWithDraw(vg, (int) (x - PAGE_ANIM_OFFSET + pageProgress), y + 72, inputHandler);
                currentPage.scrollWithDraw(vg, (int) (x + pageProgress), y + 72, inputHandler);
            }
            if (pageAnimation.isFinished()) prevPage = null;
        } else {
            currentPage.scrollWithDraw(vg, x + 224, y + 72, inputHandler);
        }
        NanoVGRenderUtil.resetScissor(vg);

        float breadcrumbX = x + 336;
        for (int i = 0; i < currentPage.parents.size(); i++) {
            String title = currentPage.parents.get(i).getTitle();
            float titleWidth = NanoVGRenderUtil.getTextWidth(vg, title, 24f);
            boolean hovered = inputHandler.isAreaHovered(breadcrumbX, y + 24, titleWidth, 36);
            int color = Colors.WHITE_60;
            if (i == currentPage.parents.size() - 1) color = Colors.WHITE;
            else if (hovered && !inputHandler.isButtonDown(0)) color = Colors.WHITE_80;
            NanoVGRenderUtil.drawText(vg, title, breadcrumbX, y + 38, color, 24f);
            if (i != 0) Icons.caretRight(vg, breadcrumbX - 24, y + 23, 16, color);
            if (hovered && inputHandler.isClicked()) openPage(currentPage.parents.get(i));
            breadcrumbX += titleWidth + 32;
        }

        currentPage.drawLast(vg, inputHandler);

        NanoVGRenderUtil.endFrame();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        inputHandler.addScroll(verticalAmount);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (textInputField.isToggled()) {
                textInputField.keyTyped((char) 0, keyCode);
                return true;
            }
            if (currentPage.hasFocus()) {
                currentPage.keyTyped((char) 0, keyCode);
                return true;
            }
            this.close();
            return true;
        }
        textInputField.keyTyped((char) 0, keyCode);
        currentPage.keyTyped((char) 0, keyCode);
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        textInputField.keyTyped(chr, 0);
        currentPage.keyTyped(chr, 0);
        return true;
    }

    @Override
    public void removed() {
        // 记住当前页面,下次打开 ClickGUI 时回到这里
        lastPage = currentPage;
        if (INSTANCE == this) INSTANCE = null;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public boolean isTransitioning() {
        return prevPage != null && pageAnimation != null && !pageAnimation.isFinished();
    }

    public String getSearchValue() {
        return textInputField.getInput();
    }

    public void openPage(Page page) {
        openPage(page, true);
    }

    public void openPage(Page page, boolean addToPrevious) {
        openPage(page, new EaseOutExpo(450, PAGE_ANIM_START, PAGE_ANIM_END, false), addToPrevious);
    }

    private void openPage(Page page, Animation animation, boolean addToPrevious) {
        if (page == currentPage) return;
        currentPage.finishUpAndClose();
        textInputField.setInput("");
        if (page.parents.isEmpty()) {
            page.parents.addAll(currentPage.parents);
            if (!page.isBase()) {
                boolean already = false;
                for (int i = 0; i < page.parents.size(); i++) {
                    Page parent = page.parents.get(i);
                    if (parent == page) {
                        already = true;
                        page.parents.subList(i + 1, page.parents.size()).clear();
                        break;
                    }
                }
                if (!already) page.parents.add(page);
            } else {
                page.parents.clear();
                page.parents.add(page);
            }
        }
        sideBar.pageOpened(page.parents.get(0).getTitle());
        if (addToPrevious) {
            previousPages.add(0, currentPage);
            nextPages.clear();
        }
        prevPage = currentPage;
        currentPage = page;
        this.pageAnimation = animation;
    }

    private void handleHistoryMovement(boolean back, boolean forward) {
        if (back && forward) return;
        if (back && previousPages.size() > 0) {
            nextPages.add(0, currentPage);
            openPage(previousPages.get(0), false);
            previousPages.remove(0);
        }
        if (forward && nextPages.size() > 0) {
            previousPages.add(0, currentPage);
            openPage(nextPages.get(0), new EaseOutExpo(450, PAGE_ANIM_START, PAGE_ANIM_END, true), false);
            nextPages.remove(0);
        }
    }

    public static float getScaleFactor() {
        int scW = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int scH = MinecraftClient.getInstance().getWindow().getScaledHeight();
        float scale = Math.min(scW / 1920f, scH / 1080f);
        if (scale < 1) scale = Math.min(Math.min(1f, scW / 1280f), Math.min(1f, scH / 800f));
        return (float) (Math.floor(scale / 0.05f) * 0.05f);
    }
}
