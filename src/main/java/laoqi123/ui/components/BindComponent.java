package laoqi123.ui.components;

import laoqi123.module.modules.GuiModule;
import laoqi123.ui.ClickGui;
import laoqi123.ui.Component;
import laoqi123.ui.Gnome;
import laoqi123.ui.dataset.BindStage;
import laoqi123.util.KeyBindUtil;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicInteger;

public class BindComponent implements Component {
    public boolean isBinding;
    private final ModuleComponent parentModule;
    private int offsetY;
    private int x;
    private int y;

    public BindComponent(ModuleComponent b, int offsetY) {
        this.parentModule = b;
        this.x = b.category.getX() + b.category.getWidth();
        this.y = b.category.getY() + b.offsetY;
        this.offsetY = offsetY;
    }

    public void draw(DrawContext context, AtomicInteger offset) {
        int rowY = this.parentModule.category.getY() + this.offsetY;
        int fontHeight = ClickGui.getFontHeight();
        ClickGui.drawString(context, "Keybind", (float) (this.parentModule.category.getX() + 16), (float) (rowY + (this.getHeight() - fontHeight) / 2), Gnome.TEXT_SECONDARY, false);

        String keyText = this.isBinding ? "Press a key..." : KeyBindUtil.getKeyName(this.parentModule.mod.getKey());
        int rightX = this.parentModule.category.getX() + this.parentModule.category.getWidth() - 16;
        int valueWidth = ClickGui.getStringWidth(context, keyText);
        ClickGui.drawString(context, keyText, (float) (rightX - valueWidth), (float) (rowY + (this.getHeight() - fontHeight) / 2), this.isBinding ? Gnome.ACCENT : Gnome.TEXT_PRIMARY, false);
    }

    public void update(int mousePosX, int mousePosY) {
        this.y = this.parentModule.category.getY() + this.offsetY;
        this.x = this.parentModule.category.getX();
    }

    public void mouseDown(int x, int y, int button) {
        if (this.isHovered(x, y) && button == 0 && this.parentModule.panelExpand) {
            this.isBinding = !this.isBinding;
        } else if (this.isBinding && this.parentModule.panelExpand) {
            int keyIndex = button - 100;
            if (button == 0) {
                this.isBinding = false;
                return;
            }
            this.parentModule.mod.setKey(keyIndex);
            this.isBinding = false;
        }
    }

    public void mouseReleased(int x, int y, int button) {
    }

    public void keyTyped(char chatTyped, int keyCode) {
        if (this.isBinding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.parentModule.mod.setKey(0);
                this.isBinding = false;
                return;
            }
            if (keyCode == GLFW.GLFW_KEY_O) {
                if (this.parentModule.mod instanceof GuiModule) {
                    this.parentModule.mod.setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
                } else {
                    this.parentModule.mod.setKey(0);
                }
                this.isBinding = false;
                return;
            }
            this.parentModule.mod.setKey(keyCode);
            this.isBinding = false;
        }
    }

    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    public boolean isHovered(int x, int y) {
        return x >= this.x && x <= this.x + this.parentModule.category.getWidth()
                && y >= this.y && y <= this.y + this.getHeight();
    }

    public int getHeight() {
        return Gnome.SETTING_ROW_HEIGHT;
    }

    public boolean isVisible() {
        return true;
    }
}