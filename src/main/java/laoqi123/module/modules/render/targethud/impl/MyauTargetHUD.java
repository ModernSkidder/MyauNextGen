package laoqi123.module.modules.render.targethud.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import laoqi123.Myau;
import laoqi123.enums.ChatColors;
import laoqi123.module.modules.render.HUD;
import laoqi123.module.modules.render.TargetHud2;
import laoqi123.module.modules.render.targethud.TargetHUDMode;
import laoqi123.value.properties.*;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.ModeValue;
import laoqi123.util.ColorUtil;
import laoqi123.util.RenderUtil;
import laoqi123.util.TeamUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.Color;

public class MyauTargetHUD extends TargetHUDMode {
    public final ModeValue color = new ModeValue("Color", 0, new String[]{"Default", "Hud"});
    public final FloatValue scale = new FloatValue("Scale", 1.0F, 0.5F, 1.5F);
    public final PercentValue background = new PercentValue("Background", 25);
    public final BooleanValue head = new BooleanValue("Head", true);
    public final BooleanValue indicator = new BooleanValue("Indicator", true);
    public final BooleanValue outline = new BooleanValue("Outline", false);
    public final BooleanValue animations = new BooleanValue("Animations", true);
    public final BooleanValue shadow = new BooleanValue("Shadow", true);

    public MyauTargetHUD() {
        super("Myau");
    }

    @Override
    public void render(TargetHud2 targetHUD, TargetHud2.RenderData data, float x, float y) {
        float elapsedTime = (float) Math.clamp(targetHUD.animTimer.getElapsedTime(), 0L, 150L);
        float lerpedHealthRatio = Math.clamp(RenderUtil.lerpFloat(targetHUD.newHealth, targetHUD.oldHealth, elapsedTime / 150.0F) / targetHUD.maxHealth, 0.0F, 1.0F);
        Color targetColor = this.getTargetColor(data.entity());
        Color healthBarColor = this.color.getValue() == 0 ? ColorUtil.getHealthBlend(lerpedHealthRatio) : targetColor;
        float healthDeltaRatio = Math.clamp((data.playerHealth() - data.targetHealth() + 1.0F) / 2.0F, 0.0F, 1.0F);
        Color healthDeltaColor = ColorUtil.getHealthBlend(healthDeltaRatio);
        UnfairText text = this.buildText(targetHUD, data);
        float headIconOffset = this.getHeadIconOffset(targetHUD);
        float barTotalWidth = this.getBarWidth(targetHUD, text, headIconOffset);
        float posX = x / this.scale.getValue();
        float posY = y / this.scale.getValue();

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().scale(this.scale.getValue(), this.scale.getValue(), 1.0F);
        RenderSystem.getModelViewStack().translate(posX, posY, 0.0F);
        RenderUtil.enableRenderState();
        int backgroundColor = new Color(0.0F, 0.0F, 0.0F, this.background.getValue() / 100.0F).getRGB();
        int outlineColor = this.outline.getValue() ? targetColor.getRGB() : new Color(0, 0, 0, 0).getRGB();
        RenderUtil.drawOutlineRect(0.0F, 0.0F, barTotalWidth, 27.0F, 1.5F, backgroundColor, outlineColor);
        RenderUtil.drawRect(headIconOffset + 2.0F, 22.0F, barTotalWidth - 2.0F, 25.0F, ColorUtil.darker(healthBarColor, 0.2F).getRGB());
        RenderUtil.drawRect(headIconOffset + 2.0F, 22.0F, headIconOffset + 2.0F + lerpedHealthRatio * (barTotalWidth - 2.0F - headIconOffset - 2.0F), 25.0F, healthBarColor.getRGB());
        RenderUtil.disableRenderState();
        targetHUD.drawText(text.targetName, headIconOffset + 2.0F, 2.0F, -1, this.shadow.getValue());
        targetHUD.drawText(text.health, headIconOffset + 2.0F, 12.0F, -1, this.shadow.getValue());
        if (this.indicator.getValue()) {
            targetHUD.drawText(text.status, barTotalWidth - 2.0F - text.statusWidth, 2.0F, healthDeltaColor.getRGB(), this.shadow.getValue());
            targetHUD.drawText(text.healthDiff, barTotalWidth - 2.0F - text.healthDiffWidth, 12.0F, ColorUtil.darker(healthDeltaColor, 0.8F).getRGB(), this.shadow.getValue());
        }
        if (this.head.getValue() && targetHUD.headTexture != null) {
            targetHUD.renderPlayerHead(data.entity(), 2.0F, 2.0F, 23.0F);
        }
        RenderSystem.getModelViewStack().popMatrix();
    }

    @Override
    public float[] getSize(TargetHud2 targetHUD, TargetHud2.RenderData data) {
        if (data == null) {
            return new float[]{120.0F, 36.0F};
        }
        UnfairText text = this.buildText(targetHUD, data);
        float headIconOffset = this.getHeadIconOffset(targetHUD);
        return new float[]{this.getBarWidth(targetHUD, text, headIconOffset) * this.scale.getValue(), 27.0F * this.scale.getValue()};
    }

    private float getHeadIconOffset(TargetHud2 targetHUD) {
        return this.head.getValue() && targetHUD.headTexture != null ? 25.0F : 0.0F;
    }

    private float getBarWidth(TargetHud2 targetHUD, UnfairText text, float headIconOffset) {
        float barContentWidth = Math.max(
                text.targetNameWidth + (this.indicator.getValue() ? 2.0F + text.statusWidth + 2.0F : 0.0F),
                text.healthWidth + (this.indicator.getValue() ? 2.0F + text.healthDiffWidth + 2.0F : 0.0F)
        );
        return Math.max(headIconOffset + 70.0F, headIconOffset + 2.0F + barContentWidth + 2.0F);
    }

    private UnfairText buildText(TargetHud2 targetHUD, TargetHud2.RenderData data) {
        String targetNameText = ChatColors.formatColor(String.format("&r%s&r", TeamUtil.stripName(data.entity())));
        String healthText = ChatColors.formatColor(
                String.format("&r&f%s%sHP&r", TargetHud2.HEALTH_FORMAT.format(data.targetHealth()), data.absorption() > 0.0F ? "&6" : "&c")
        );
        String statusText = ChatColors.formatColor(String.format("&r&l%s&r", data.targetHealth() == data.playerHealth() ? "D" : (data.targetHealth() < data.playerHealth() ? "W" : "L")));
        String healthDiffText = ChatColors.formatColor(
                String.format("&r%s&r", data.targetHealth() == data.playerHealth() ? "0.0" : TargetHud2.DIFF_FORMAT.format(data.playerHealth() - data.targetHealth()))
        );
        return new UnfairText(targetHUD, targetNameText, healthText, statusText, healthDiffText);
    }

    @Override
    public boolean shouldAnimateHealth() {
        return this.animations.getValue();
    }

    private Color getTargetColor(LivingEntity entityLivingBase) {
        if (entityLivingBase instanceof PlayerEntity player) {
            if (TeamUtil.isFriend(player)) {
                return Myau.friendManager.getColor();
            }
            if (TeamUtil.isTarget(player)) {
                return Myau.targetManager.getColor();
            }
        }
        switch (this.color.getValue()) {
            case 0:
                if (!(entityLivingBase instanceof PlayerEntity)) {
                    return new Color(-1);
                }
                return TeamUtil.getTeamColor((PlayerEntity) entityLivingBase, 1.0F);
            case 1:
                int rgb = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis()).getRGB();
                return new Color(rgb);
            default:
                return new Color(-1);
        }
    }

    private static class UnfairText {
        private final String targetName;
        private final String health;
        private final String status;
        private final String healthDiff;
        private final int targetNameWidth;
        private final int healthWidth;
        private final int statusWidth;
        private final int healthDiffWidth;

        private UnfairText(TargetHud2 targetHUD, String targetName, String health, String status, String healthDiff) {
            this.targetName = targetName;
            this.health = health;
            this.status = status;
            this.healthDiff = healthDiff;
            this.targetNameWidth = targetHUD.getTextWidth(targetName);
            this.healthWidth = targetHUD.getTextWidth(health);
            this.statusWidth = targetHUD.getTextWidth(status);
            this.healthDiffWidth = targetHUD.getTextWidth(healthDiff);
        }
    }
}