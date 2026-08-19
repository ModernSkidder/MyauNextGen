package laoqi123.ui.components;

import laoqi123.property.properties.BooleanProperty;
import laoqi123.ui.ClickGui;
import laoqi123.ui.Component;
import laoqi123.ui.Gnome;
import laoqi123.util.RenderUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.concurrent.atomic.AtomicInteger;

public class CheckBoxComponent implements Component {
    private final BooleanProperty property;
    private final ModuleComponent module;
    private int offsetY;
    private int x;
    private int y;
    private double switchAnim = 0.0;

    public CheckBoxComponent(BooleanProperty property, ModuleComponent parentModule, int offsetY) {
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

        boolean enabled = this.property.getValue();
        double target = enabled ? 1.0 : 0.0;
        this.switchAnim += (target - this.switchAnim) * 0.25;
        int trackX = this.module.category.getX() + this.module.category.getWidth() - 16 - Gnome.SWITCH_WIDTH;
        int trackY = rowY + (this.getHeight() - Gnome.SWITCH_HEIGHT) / 2;
        RenderUtil.drawRoundedRect(trackX, trackY, Gnome.SWITCH_WIDTH, Gnome.SWITCH_HEIGHT, Gnome.SWITCH_HEIGHT / 2.0F, enabled ? Gnome.ACCENT : Gnome.SWITCH_TRACK_OFF);
        int knobSize = Gnome.SWITCH_HEIGHT - 4;
        int knobX = trackX + 2 + (int) (this.switchAnim * (Gnome.SWITCH_WIDTH - knobSize - 4));
        RenderUtil.drawRoundedRect(knobX, trackY + 2, knobSize, knobSize, knobSize / 2.0F, Gnome.SWITCH_KNOB);
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
            this.property.setValue(!this.property.getValue());
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