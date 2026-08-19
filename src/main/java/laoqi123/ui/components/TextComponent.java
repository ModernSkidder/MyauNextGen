package laoqi123.ui.components;

import laoqi123.property.properties.TextProperty;
import laoqi123.ui.ClickGui;
import laoqi123.ui.Component;
import laoqi123.ui.Gnome;
import laoqi123.ui.callback.GuiInput;
import net.minecraft.client.gui.DrawContext;

import java.util.concurrent.atomic.AtomicInteger;

public class TextComponent implements Component {
    private final TextProperty property;
    private final ModuleComponent module;
    private int offsetY;
    private int x;
    private int y;

    public TextComponent(TextProperty property, ModuleComponent parentModule, int offsetY) {
        this.property = property;
        this.module = parentModule;
        this.x = parentModule.category.getX() + parentModule.category.getWidth();
        this.y = parentModule.category.getY() + parentModule.offsetY;
        this.offsetY = offsetY;
    }

    public void draw(DrawContext context, AtomicInteger offset) {
        int rowY = this.module.category.getY() + this.offsetY;
        int fontHeight = ClickGui.getFontHeight();
        ClickGui.drawString(context, this.property.getName().replace("-", " "), (float) (this.module.category.getX() + 16), (float) (rowY + (this.getHeight() - fontHeight) / 2), Gnome.TEXT_SECONDARY, false);

        String value = this.property.getValue();
        int rightX = this.module.category.getX() + this.module.category.getWidth() - 16;
        int valueWidth = ClickGui.getStringWidth(context, value);
        ClickGui.drawString(context, value, (float) (rightX - valueWidth), (float) (rowY + (this.getHeight() - fontHeight) / 2), Gnome.TEXT_PRIMARY, false);
    }

    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return Gnome.SETTING_ROW_HEIGHT;
    }

    public void update(int mousePosX, int mousePosY) {
        this.y = this.module.category.getY() + this.offsetY;
        this.x = this.module.category.getX();
    }

    public void mouseDown(int x, int y, int button) {
        if (this.isHovered(x, y) && button == 0 && this.module.panelExpand) {
            GuiInput.prompt(property.getName().replace("-", " "), property.getValue(), property::setValue, ClickGui.getInstance());
        }
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
    }

    public boolean isHovered(int x, int y) {
        return x >= this.x && x <= this.x + this.module.category.getWidth()
                && y >= this.y && y <= this.y + this.getHeight();
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }
}