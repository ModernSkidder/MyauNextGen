package laoqi123.ui.components;

import laoqi123.module.Module;
import laoqi123.ui.ClickGui;
import laoqi123.ui.Component;
import laoqi123.ui.Gnome;
import laoqi123.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CategoryComponent {
    public ArrayList<Component> modulesInCategory = new ArrayList<>();
    public String categoryName;
    private int x;
    private int y;
    private final List<ModuleComponent> allModules = new ArrayList<>();
    private final List<ModuleComponent> visibleModules = new ArrayList<>();
    private int scroll = 0;
    private double animScroll = 0;
    private int contentHeight = 0;

    public CategoryComponent(String category, List<Module> modules) {
        this.categoryName = category;
        for (Module mod : modules) {
            ModuleComponent b = new ModuleComponent(mod, this, 0);
            this.modulesInCategory.add(b);
            this.allModules.add(b);
            this.visibleModules.add(b);
        }
    }

    public ArrayList<Component> getModules() {
        return this.modulesInCategory;
    }

    public void setX(int n) {
        this.x = n;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isOpened() {
        return true;
    }

    public void setOpened(boolean on) {
    }

    public void setSearchQuery(String query) {
        this.visibleModules.clear();
        if (query == null || query.isEmpty()) {
            this.visibleModules.addAll(this.allModules);
            return;
        }
        String lower = query.toLowerCase();
        for (ModuleComponent module : this.allModules) {
            if (module.mod.getName().toLowerCase().contains(lower)) {
                this.visibleModules.add(module);
            }
        }
        int maxScroll = Math.max(0, this.contentHeight - this.getMaxHeight());
        if (this.scroll > maxScroll) {
            this.scroll = maxScroll;
        }
    }

    public void render(DrawContext context) {
        int cardX = this.x;
        int titleHeight = 28;
        int cardY = this.y + titleHeight + 4;
        int cardWidth = Math.min(Gnome.CARD_WIDTH, MinecraftClient.getInstance().currentScreen.width - this.x - Gnome.CONTENT_PAD);
        int fontHeight = ClickGui.getFontHeight();

        String title = this.categoryName.toUpperCase();
        ClickGui.drawString(context, title, (float) cardX, (float) (this.y + (titleHeight - fontHeight) / 2), Gnome.TEXT_PRIMARY, false);

        this.updateLayout();
        int maxScroll = Math.max(0, this.contentHeight - this.getMaxHeight());
        if (this.scroll > maxScroll) {
            this.scroll = maxScroll;
        }
        if (this.animScroll > maxScroll) {
            this.animScroll = maxScroll;
        }
        this.animScroll += (this.scroll - this.animScroll) * 0.2;

        int cardHeight = Math.min(this.contentHeight, this.getMaxHeight()) + 24;
        RenderUtil.drawRoundedRect(cardX, cardY, cardWidth, cardHeight, 12, Gnome.CARD_BG);

        int rowStart = cardY + 12;
        int panelTop = rowStart;
        context.enableScissor(cardX, panelTop, cardX + cardWidth, panelTop + cardHeight - 12);
        for (ModuleComponent module : this.visibleModules) {
            module.draw(context, new AtomicInteger(0));
        }
        context.disableScissor();

        if (this.contentHeight > this.getMaxHeight()) {
            int barHeight = Math.max(24, (int) ((float) this.getMaxHeight() * this.getMaxHeight() / this.contentHeight));
            int barY = panelTop + (int) (this.animScroll * (this.getMaxHeight() - barHeight) / Math.max(1, this.contentHeight - this.getMaxHeight()));
            RenderUtil.drawRoundedRect(cardX + cardWidth - 6, barY, 4, barHeight, 2, Gnome.TEXT_DISABLED);
        }

        if (this.visibleModules.isEmpty()) {
            ClickGui.drawString(context, "No results", (float) (cardX + 16), (float) (rowStart + 8), Gnome.TEXT_DISABLED, false);
        }
    }

    public void mouseClicked(int x, int y, int button) {
        for (Component c : this.visibleModules) {
            c.mouseDown(x, y, button);
        }
    }

    public void mouseReleasedAll(int x, int y, int button) {
        for (Component c : this.visibleModules) {
            c.mouseReleased(x, y, button);
        }
    }

    private void updateLayout() {
        int yOffset = 0;
        for (ModuleComponent module : this.visibleModules) {
            module.setComponentStartAt(Gnome.CARD_CONTENT_OFFSET + yOffset - (int) this.animScroll);
            yOffset += module.getHeight();
        }
        this.contentHeight = yOffset;
    }

    public void updateAllOffsets() {
        this.updateLayout();
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return Math.min(Gnome.CARD_WIDTH, MinecraftClient.getInstance().getWindow().getScaledWidth() - this.x - Gnome.CONTENT_PAD);
    }

    public int getMaxHeight() {
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        return Math.max(100, screenHeight - this.y - 28 - Gnome.CONTENT_PAD);
    }

    public void resetScroll() {
        this.scroll = 0;
        this.animScroll = 0;
    }

    public String getName() {
        return categoryName;
    }

    public void onScroll(int mouseX, int mouseY, int scrollAmount) {
        if (this.contentHeight <= this.getMaxHeight()) return;
        this.scroll -= scrollAmount * 12;
        this.scroll = Math.max(0, Math.min(this.scroll, this.contentHeight - this.getMaxHeight()));
        this.updateLayout();
    }
}