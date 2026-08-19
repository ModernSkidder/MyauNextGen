package laoqi123.ui.components;

import laoqi123.property.properties.ModeProperty;
import laoqi123.ui.ClickGui;
import laoqi123.ui.Component;
import laoqi123.ui.Gnome;
import net.minecraft.client.gui.DrawContext;

import java.util.concurrent.atomic.AtomicInteger;

public class ModeComponent implements Component {
    private final ModeProperty property;
    private final ModuleComponent parentModule;
    private int x;
    private int y;
    private int offsetY;

    public ModeComponent(ModeProperty desc, ModuleComponent parentModule, int offsetY) {
        this.property = desc;
        this.parentModule = parentModule;
        this.x = parentModule.category.getX() + parentModule.category.getWidth();
        this.y = parentModule.category.getY() + parentModule.offsetY;
        this.offsetY = offsetY;
    }

    public void draw(DrawContext context, AtomicInteger offset) {
        int rowY = this.parentModule.category.getY() + this.offsetY;
        int fontHeight = ClickGui.getFontHeight();
        ClickGui.drawString(context, this.property.getName().replace("-", " "), (float) (this.parentModule.category.getX() + 16), (float) (rowY + (this.getHeight() - fontHeight) / 2), Gnome.TEXT_SECONDARY, false);

        String mode = this.property.getModeString();
        mode = mode.replace("_", " ").toUpperCase();
        int rightX = this.parentModule.category.getX() + this.parentModule.category.getWidth() - 16;
        int modeWidth = ClickGui.getStringWidth(context, mode + " v");
        ClickGui.drawString(context, mode + " v", (float) (rightX - modeWidth), (float) (rowY + (this.getHeight() - fontHeight) / 2), Gnome.ACCENT, false);
    }

    public void update(int mousePosX, int mousePosY) {
        this.y = this.parentModule.category.getY() + this.offsetY;
        this.x = this.parentModule.category.getX();
    }

    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return Gnome.SETTING_ROW_HEIGHT;
    }

    public void mouseDown(int x, int y, int button) {
        if (isHovered(x, y)) {
            if (button == 0) {
                this.property.nextMode();
            } else if (button == 1) {
                this.property.previousMode();
            }
        }
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
    }

    private boolean isHovered(int x, int y) {
        return x >= this.x && x <= this.x + this.parentModule.category.getWidth()
                && y >= this.y && y <= this.y + this.getHeight();
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }
}