package laoqi123.ui.components;

import laoqi123.ui.ClickGui;
import laoqi123.ui.Component;
import laoqi123.ui.Gnome;
import laoqi123.ui.callback.GuiInput;
import laoqi123.ui.dataset.Slider;
import laoqi123.util.RenderUtil;
import net.minecraft.client.gui.DrawContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;

public class SliderComponent implements Component {
    private final Slider slider;
    private final ModuleComponent parentModule;
    private int offsetY;
    private int x;
    private int y;
    private boolean dragging = false;

    public SliderComponent(Slider slider, ModuleComponent parentModule, int offsetY) {
        this.slider = slider;
        this.parentModule = parentModule;
        this.x = parentModule.category.getX() + parentModule.category.getWidth();
        this.y = parentModule.category.getY() + parentModule.offsetY;
        this.offsetY = offsetY;
    }

    public void draw(DrawContext context, AtomicInteger offset) {
        int rowY = this.parentModule.category.getY() + this.offsetY;
        int fontHeight = ClickGui.getFontHeight();
        ClickGui.drawString(context, this.slider.getName().replace("-", " "), (float) (this.parentModule.category.getX() + 16), (float) (rowY + (this.getHeight() - fontHeight) / 2), Gnome.TEXT_SECONDARY, false);

        int rightX = this.parentModule.category.getX() + this.parentModule.category.getWidth() - 16;
        int trackWidth = 110;
        int trackX = rightX - trackWidth - 4 - ClickGui.getStringWidth(context, this.slider.getValueString()) - 4;
        int trackY = rowY + (this.getHeight() - 4) / 2;
        int trackColor = Gnome.ACCENT;

        double ratio = (this.slider.getInput() - this.slider.getMin()) / Math.max(1.0E-6, this.slider.getMax() - this.slider.getMin());
        ratio = Math.max(0.0, Math.min(1.0, ratio));

        RenderUtil.drawRoundedRect(trackX, trackY, trackWidth, 4, 2, Gnome.TRACK_BG);
        RenderUtil.drawRoundedRect(trackX, trackY, (float) (trackWidth * ratio), 4, 2, trackColor);
        int knobX = trackX + (int) (trackWidth * ratio);
        RenderUtil.drawRoundedRect(knobX - 5, trackY - 3, 10, 10, 5, Gnome.TEXT_PRIMARY);

        ClickGui.drawString(context, this.slider.getValueString(), (float) (rightX - ClickGui.getStringWidth(context, this.slider.getValueString())), (float) (rowY + (this.getHeight() - fontHeight) / 2), Gnome.TEXT_PRIMARY, false);
    }

    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return Gnome.SETTING_ROW_HEIGHT;
    }

    public void update(int mousePosX, int mousePosY) {
        this.y = this.parentModule.category.getY() + this.offsetY;
        this.x = this.parentModule.category.getX();

        if (this.dragging) {
            int trackX = this.x + this.parentModule.category.getWidth() - 16 - 4 - ClickGui.getStringWidth(null, this.slider.getValueString()) - 4 - 110;
            int trackWidth = 110;
            double d = Math.min(trackWidth, Math.max(0, mousePosX - trackX));
            if (trackWidth == 0) {
                this.slider.setValue(this.slider.getMin());
            } else {
                double rawValue = d / (double) trackWidth
                        * (this.slider.getMax() - this.slider.getMin())
                        + this.slider.getMin();

                double increment = this.slider.getIncrement();
                if (increment > 0) {
                    rawValue = Math.round(rawValue / increment) * increment;
                }
                double n = roundToPrecision(rawValue, 2);
                n = Math.max(this.slider.getMin(), Math.min(this.slider.getMax(), n));
                this.slider.setValue(n);
            }
        }
    }

    private static double roundToPrecision(double v, int precision) {
        if (precision < 0) {
            return 0.0D;
        } else {
            BigDecimal bd = new BigDecimal(v);
            bd = bd.setScale(precision, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }
    }

    public void mouseDown(int x, int y, int button) {
        if (this.isValueHovered(x, y) && button == 0 && this.parentModule.panelExpand) {
            GuiInput.prompt(slider.getName().replace("-", " "), slider.getValueString(), slider::setValueString, ClickGui.getInstance());
            return;
        }
        if (this.isTrackHovered(x, y) && button == 0 && this.parentModule.panelExpand) {
            this.dragging = true;
        }
    }

    public void mouseReleased(int x, int y, int button) {
        this.dragging = false;
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
    }

    private boolean isValueHovered(int x, int y) {
        return x >= this.x && x <= this.x + this.parentModule.category.getWidth()
                && y >= this.y && y <= this.y + this.getHeight();
    }

    private boolean isTrackHovered(int x, int y) {
        int trackX = this.x + this.parentModule.category.getWidth() - 16 - 4 - ClickGui.getStringWidth(null, this.slider.getValueString()) - 4 - 110;
        return x >= trackX && x <= trackX + 110
                && y >= this.y && y <= this.y + this.getHeight();
    }

    @Override
    public boolean isVisible() {
        return slider.isVisible();
    }
}