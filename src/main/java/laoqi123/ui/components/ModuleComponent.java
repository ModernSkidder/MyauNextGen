package laoqi123.ui.components;

import laoqi123.Myau;
import laoqi123.module.Module;
import laoqi123.property.Property;
import laoqi123.property.properties.*;
import laoqi123.ui.ClickGui;
import laoqi123.ui.Component;
import laoqi123.ui.Gnome;
import laoqi123.ui.dataset.impl.FloatSlider;
import laoqi123.ui.dataset.impl.IntSlider;
import laoqi123.ui.dataset.impl.PercentageSlider;
import laoqi123.util.RenderUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ModuleComponent implements Component {
    public Module mod;
    public CategoryComponent category;
    public int offsetY;
    private final ArrayList<Component> settings;
    public boolean panelExpand;
    private double switchAnim = 0.0;

    public int getTitleHeight() {
        return Gnome.ROW_HEIGHT;
    }

    public ModuleComponent(Module mod, CategoryComponent category, int offsetY) {
        this.mod = mod;
        this.category = category;
        this.offsetY = offsetY;
        this.settings = new ArrayList<>();
        this.panelExpand = false;
        int y = offsetY + this.getTitleHeight();
        if (!Myau.propertyManager.properties.get(mod.getClass()).isEmpty()) {
            for (Property<?> baseProperty : Myau.propertyManager.properties.get(mod.getClass())) {
                if (baseProperty instanceof BooleanProperty) {
                    BooleanProperty property = (BooleanProperty) baseProperty;
                    CheckBoxComponent c = new CheckBoxComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof FloatProperty) {
                    FloatProperty property = (FloatProperty) baseProperty;
                    SliderComponent c = new SliderComponent(new FloatSlider(property), this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof IntProperty) {
                    IntProperty property = (IntProperty) baseProperty;
                    SliderComponent c = new SliderComponent(new IntSlider(property), this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof PercentProperty) {
                    PercentProperty property = (PercentProperty) baseProperty;
                    SliderComponent c = new SliderComponent(new PercentageSlider(property), this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof ModeProperty) {
                    ModeProperty property = (ModeProperty) baseProperty;
                    ModeComponent c = new ModeComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof ColorProperty) {
                    ColorProperty property = (ColorProperty) baseProperty;
                    ColorSliderComponent c = new ColorSliderComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof TextProperty) {
                    TextProperty property = (TextProperty) baseProperty;
                    TextComponent c = new TextComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                }
            }
        }

        this.settings.add(new BindComponent(this, y));
    }

    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
        int y = this.offsetY + this.getTitleHeight();

        for (Component c : this.settings) {
            c.setComponentStartAt(y);
            if (c.isVisible()) {
                y += c.getHeight();
            }
        }
    }

    public void draw(DrawContext context, AtomicInteger offset) {
        int rowY = this.category.getY() + this.offsetY;
        int rowWidth = this.category.getWidth();
        int fontHeight = ClickGui.getFontHeight();

        boolean hovered = ClickGui.getLastMouseX() >= this.category.getX()
                && ClickGui.getLastMouseX() <= this.category.getX() + rowWidth
                && ClickGui.getLastMouseY() >= rowY
                && ClickGui.getLastMouseY() <= rowY + this.getTitleHeight();
        if (hovered) {
            RenderUtil.drawRect(this.category.getX(), rowY, this.category.getX() + rowWidth, rowY + this.getTitleHeight(), Gnome.HOVER);
        }

        boolean enabled = this.mod.isEnabled();
        ClickGui.drawString(context, this.mod.getName(), (float) (this.category.getX() + 16), (float) (rowY + (this.getTitleHeight() - fontHeight) / 2), enabled ? Gnome.TEXT_PRIMARY : Gnome.TEXT_SECONDARY, false);

        this.drawSwitch(context, rowY, enabled);

        String chevron = this.panelExpand ? "v" : ">";
        ClickGui.drawString(context, chevron, (float) (this.category.getX() + rowWidth - 16 - 40 - 14), (float) (rowY + (this.getTitleHeight() - fontHeight) / 2), Gnome.TEXT_DISABLED, false);

        RenderUtil.drawRect(this.category.getX(), rowY + this.getTitleHeight() - 1, this.category.getX() + rowWidth, rowY + this.getTitleHeight(), Gnome.ROW_DIVIDER);

        if (this.panelExpand && !this.settings.isEmpty()) {
            int settingY = rowY + this.getTitleHeight();
            for (Component c : this.settings) {
                if (!c.isVisible()) {
                    continue;
                }
                boolean settingHovered = ClickGui.getLastMouseX() >= this.category.getX()
                        && ClickGui.getLastMouseX() <= this.category.getX() + rowWidth
                        && ClickGui.getLastMouseY() >= settingY
                        && ClickGui.getLastMouseY() <= settingY + c.getHeight();
                if (settingHovered) {
                    RenderUtil.drawRect(this.category.getX(), settingY, this.category.getX() + rowWidth, settingY + c.getHeight(), Gnome.HOVER);
                }
                c.draw(context, offset);
                offset.incrementAndGet();
                settingY += c.getHeight();
            }
            RenderUtil.drawRect(this.category.getX(), settingY - 1, this.category.getX() + rowWidth, settingY, Gnome.ROW_DIVIDER);
        }
    }

    private void drawSwitch(DrawContext context, int rowY, boolean enabled) {
        double target = enabled ? 1.0 : 0.0;
        this.switchAnim += (target - this.switchAnim) * 0.25;
        int trackX = this.category.getX() + this.category.getWidth() - 16 - Gnome.SWITCH_WIDTH;
        int trackY = rowY + (this.getTitleHeight() - Gnome.SWITCH_HEIGHT) / 2;
        int trackColor = enabled ? Gnome.ACCENT : Gnome.SWITCH_TRACK_OFF;
        RenderUtil.drawRoundedRect(trackX, trackY, Gnome.SWITCH_WIDTH, Gnome.SWITCH_HEIGHT, Gnome.SWITCH_HEIGHT / 2.0F, trackColor);
        int knobSize = Gnome.SWITCH_HEIGHT - 4;
        int knobX = trackX + 2 + (int) (this.switchAnim * (Gnome.SWITCH_WIDTH - knobSize - 4));
        RenderUtil.drawRoundedRect(knobX, trackY + 2, knobSize, knobSize, knobSize / 2.0F, Gnome.SWITCH_KNOB);
    }

    public int getHeight() {
        if (!this.panelExpand) {
            return this.getTitleHeight();
        } else {
            int h = this.getTitleHeight();
            for (Component c : this.settings) {
                if (c.isVisible()) {
                    h += c.getHeight();
                }
            }
            return h;
        }
    }

    public void update(int mousePosX, int mousePosY) {
        if (!panelExpand) return;
        if (!this.settings.isEmpty()) {
            for (Component c : this.settings) {
                if (c.isVisible()) {
                    c.update(mousePosX, mousePosY);
                }
            }
        }
    }

    public void mouseDown(int x, int y, int button) {
        int rowY = this.category.getY() + this.offsetY;
        boolean onTitle = x >= this.category.getX() && x <= this.category.getX() + this.category.getWidth()
                && y >= rowY && y <= rowY + this.getTitleHeight();
        if (onTitle) {
            int switchX = this.category.getX() + this.category.getWidth() - 16 - Gnome.SWITCH_WIDTH;
            boolean onSwitch = x >= switchX && x <= switchX + Gnome.SWITCH_WIDTH;
            if (button == 0 && onSwitch) {
                this.mod.toggle();
            } else if (button == 0 || button == 1) {
                this.panelExpand = !this.panelExpand;
            }
            return;
        }

        if (!panelExpand) return;
        for (Component c : this.settings) {
            if (c.isVisible()) {
                c.mouseDown(x, y, button);
            }
        }
    }

    public void mouseReleased(int x, int y, int button) {
        if (!panelExpand) return;
        for (Component c : this.settings) {
            if (c.isVisible()) {
                c.mouseReleased(x, y, button);
            }
        }
    }

    public void keyTyped(char chatTyped, int keyCode) {
        if (!panelExpand) return;
        for (Component c : this.settings) {
            if (c.isVisible()) {
                c.keyTyped(chatTyped, keyCode);
            }
        }
    }

    public boolean isHovered(int x, int y) {
        return x >= this.category.getX() && x <= this.category.getX() + this.category.getWidth()
                && y >= this.category.getY() + this.offsetY
                && y <= this.category.getY() + this.offsetY + this.getHeight();
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}