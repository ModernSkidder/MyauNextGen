package laoqi123.module.modules.render.targethud.impl;

import laoqi123.module.modules.render.TargetHud2;
import laoqi123.module.modules.render.targethud.TargetHUDMode;
import laoqi123.util.RenderUtil;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class NovolineTargetHUD extends TargetHUDMode {
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));

    public NovolineTargetHUD() {
        super("Novoline");
    }

    @Override
    public void render(TargetHud2 targetHUD, TargetHud2.RenderData data, float x, float y) {
        float[] size = this.getSize(targetHUD, data);
        float width = size[0];
        float height = size[1];
        float ratio = Math.clamp(data.targetHealth() / Math.max(data.maxHealth(), 1.0F), 0.0F, 1.0F);
        float space = width - height - 4.5F;
        int accent = targetHUD.getRavenGradientColors()[0];

        RenderUtil.drawRect(x - 1.0F, y - 1.0F, x + width + 1.0F, y + height + 1.0F, new Color(29, 29, 29, 255).getRGB());
        RenderUtil.drawRect(x, y, x + width, y + height, new Color(40, 40, 40, 255).getRGB());
        targetHUD.renderPlayerHead(data.entity(), x + 0.5F, y + 0.5F, height - 1.0F);
        RenderUtil.drawRect(x + 2.0F + height, y + height - 19.5F, x + 2.0F + height + space, y + height - 8.7F, new Color(0, 0, 0, 50).getRGB());
        RenderUtil.drawRect(x + 2.0F + height, y + height - 19.5F, x + 2.0F + height + space * ratio, y + height - 8.7F, accent);

        String text = PERCENT_FORMAT.format(ratio * 100.0F) + "%";
        String name = data.entity().getDisplayName().getString();
        targetHUD.drawText(text, x + 39.0F + space / 2.0F - targetHUD.getTextWidth(text) / 2.0F, y + 19.0F, -1, true);
        targetHUD.drawText(name, x + 40.0F, y + 4.0F, -1, true);
    }

    @Override
    public float[] getSize(TargetHud2 targetHUD, TargetHud2.RenderData data) {
        if (data == null) {
            return new float[]{100.0F, 37.0F};
        }
        return new float[]{28.0F + targetHUD.getTextWidth(data.entity().getDisplayName().getString()) + 40.0F, 37.0F};
    }

}