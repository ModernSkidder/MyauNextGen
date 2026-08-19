package laoqi123.ui.pages;

import laoqi123.Myau;
import laoqi123.module.Category;
import laoqi123.module.Module;
import laoqi123.ui.ClickGui;
import laoqi123.ui.Colors;
import laoqi123.ui.InputHandler;
import laoqi123.ui.elements.ModCard;
import laoqi123.ui.renderer.NanoVGRenderUtil;

import java.util.ArrayList;

public class ModsPage extends Page {
    private final Category category;
    private final ArrayList<ModCard> modCards = new ArrayList<>();
    private int size;

    public ModsPage(Category category) {
        super(category.getName());
        this.category = category;
        for (Module mod : Myau.moduleManager.getModulesInCategory(category)) {
            modCards.add(new ModCard(mod, this));
        }
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        String filter = ClickGui.INSTANCE == null ? "" : ClickGui.INSTANCE.getSearchValue().toLowerCase().trim();
        int iX = x + 16;
        int iY = y + 16;
        for (ModCard modCard : modCards) {
            if (filter.isEmpty() || modCard.getMod().getName().toLowerCase().contains(filter)) {
                if (iY + 135 >= y - scroll && iY <= y + 728 - scroll) modCard.draw(vg, iX, iY, inputHandler);
                iX += 260;
                if (iX > x + 796) {
                    iX = x + 16;
                    iY += 135;
                }
            }
        }
        size = iY - y + 135;
        if (iX == x + 16 && iY == y + 16) {
            NanoVGRenderUtil.drawText(vg, "Looks like there is nothing here. Try another category?", x + 16, y + 72, Colors.WHITE_60, 14f);
        }
    }

    public void openModule(Module mod) {
        if (ClickGui.INSTANCE != null) ClickGui.INSTANCE.openPage(new ModConfigPage(mod));
    }

    public Category getCategory() {
        return category;
    }

    @Override
    public int getMaxScrollHeight() {
        return size;
    }

    @Override
    public boolean isBase() {
        return true;
    }
}
