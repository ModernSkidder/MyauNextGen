package laoqi123.ui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import laoqi123.enums.ChatColors;
import laoqi123.property.properties.ColorProperty;
import laoqi123.ui.ClickGui;
import laoqi123.ui.Component;
import laoqi123.util.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ColorSliderComponent implements Component {

    private final ModuleComponent parentModule;
    private final ColorProperty property;
    private int offsetY;
    private boolean draggingHue, draggingSat, draggingBri;
    private float hue, saturation, brightness;

    public ColorSliderComponent(ColorProperty property, ModuleComponent parentModule, int offsetY) {
        this.parentModule = parentModule;
        this.offsetY = offsetY;
        this.property = property;

        Color c = new Color(property.getValue());
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
    }

    @Override
    public void draw(DrawContext context, java.util.concurrent.atomic.AtomicInteger offset) {
        int x = parentModule.category.getX() + 16;
        int y = parentModule.category.getY() + offsetY;
        int width = parentModule.category.getWidth() - 32;
        int fontHeight = ClickGui.getFontHeight();
        ClickGui.drawStringWithShadow(context, property.getName().replace("-", " "), (float) x, (float) (this.parentModule.category.getY() + this.offsetY + (this.getHeight() - fontHeight) / 2), -1);
        if (!draggingHue && !draggingSat && !draggingBri) {
            Color color = new Color(property.getValue());
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
            hue = hsb[0];
            saturation = hsb[1];
            brightness = hsb[2];
        }
        int colorPreviewSize = 6;
        int colorPreviewX = parentModule.category.getX() + parentModule.category.getWidth() - 16 - colorPreviewSize;
        int colorPreviewY = y + 3;
        int previewColor = Color.HSBtoRGB(hue, saturation, brightness);
        RenderUtil.drawRect(colorPreviewX, colorPreviewY, colorPreviewX + colorPreviewSize, colorPreviewY + colorPreviewSize, previewColor);
        int baseY = y + fontHeight + 4;
        int satY = baseY + 4 + 2;
        int briY = satY + 4 + 2;
        drawHueBar(x, baseY, width);
        drawPointer(x, baseY, width, hue);
        drawGradientRect(x, satY, x + width, satY + 4, Color.WHITE.getRGB(), Color.getHSBColor(hue, 1f, 1f).getRGB());
        drawPointer(x, satY, width, saturation);
        drawGradientRect(x, briY, x + width, briY + 4, Color.BLACK.getRGB(), Color.getHSBColor(hue, saturation, 1f).getRGB());
        drawPointer(x, briY, width, brightness);
    }

    private void drawHueBar(int x, int y, int width) {
        for (int i = 0; i < width; i++) {
            float hue = (float) i / (float) width;
            int color = Color.HSBtoRGB(hue, 1f, 1f);
            RenderUtil.drawRect(x + i, y, x + i + 1, y + 4, color);
        }
    }

    private void drawPointer(int x, int y, int width, float value) {
        int posX = x + (int) (width * value);
        RenderUtil.drawRect(posX - 1, y, posX, y + 4, new Color(0, 0, 0, 200).getRGB());
    }

    @Override
    public void update(int mouseX, int mouseY) {
        int baseX = parentModule.category.getX() + 16;
        int width = parentModule.category.getWidth() - 32;
        boolean changed = false;

        if (draggingHue) {
            hue = getSliderValue(mouseX, baseX, width);
            changed = true;
        }
        if (draggingSat) {
            saturation = getSliderValue(mouseX, baseX, width);
            changed = true;
        }
        if (draggingBri) {
            brightness = getSliderValue(mouseX, baseX, width);
            changed = true;
        }

        if (changed) {
            int signed = Color.HSBtoRGB(hue, saturation, brightness);
            property.setValue(new Color(signed).getRGB());
        }
    }

    private float getSliderValue(int mouseX, int startX, int width) {
        double d = Math.min(width, Math.max(0, mouseX - startX));
        return (float) roundToPrecision(d / width, 3);
    }

    private static double roundToPrecision(double v, int precision) {
        BigDecimal bd = new BigDecimal(v);
        bd = bd.setScale(precision, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (button != 0 || !parentModule.panelExpand) return;
        int baseY = parentModule.category.getY() + offsetY + ClickGui.getFontHeight() + 4;
        if (isHovered(mouseX, mouseY, baseY)) draggingHue = true;
        else if (isHovered(mouseX, mouseY, baseY + 4 + 2)) draggingSat = true;
        else if (isHovered(mouseX, mouseY, baseY + (4 + 2) * 2)) draggingBri = true;
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
        draggingHue = draggingSat = draggingBri = false;
    }

    private boolean isHovered(int mx, int my, int sliderY) {
        int startX = parentModule.category.getX() + 16;
        int endX = startX + parentModule.category.getWidth() - 32;
        return mx >= startX && mx <= endX && my >= sliderY && my <= sliderY + 4;
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return ClickGui.getFontHeight() + 20;
    }

    private void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        float sa = (float) (startColor >> 24 & 255) / 255.0F;
        float sr = (float) (startColor >> 16 & 255) / 255.0F;
        float sg = (float) (startColor >> 8 & 255) / 255.0F;
        float sb = (float) (startColor & 255) / 255.0F;
        float ea = (float) (endColor >> 24 & 255) / 255.0F;
        float er = (float) (endColor >> 16 & 255) / 255.0F;
        float eg = (float) (endColor >> 8 & 255) / 255.0F;
        float eb = (float) (endColor & 255) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(right, top, 0).color(er, eg, eb, ea);
        buffer.vertex(left, top, 0).color(sr, sg, sb, sa);
        buffer.vertex(left, bottom, 0).color(sr, sg, sb, sa);
        buffer.vertex(right, bottom, 0).color(er, eg, eb, ea);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }
}