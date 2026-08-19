package laoqi123.module.modules.render.targethud.impl;

import laoqi123.module.modules.render.TargetHud2;
import laoqi123.module.modules.render.targethud.Fonts;
import laoqi123.module.modules.render.targethud.TargetHUDMode;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.PercentProperty;
import laoqi123.util.RenderUtil;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class UnfairTargetHUD extends TargetHUDMode {
    public final PercentProperty background = new PercentProperty("Background", 65);
    public final BooleanProperty animations = new BooleanProperty("Animations", true);

    public UnfairTargetHUD() {
        super("Unfair");
    }

    @Override
    public void render(TargetHud2 targetHUD, TargetHud2.RenderData data, float x, float y) {
        float width = this.getSize(targetHUD, data)[0];
        float height = this.getSize(targetHUD, data)[1];
        int fadeAlpha = targetHUD.getFadeAlpha();
        if (fadeAlpha <= 0) {
            return;
        }
        float progress = fadeAlpha / 255.0F;
        float scale = this.getPopScale(progress);
        float centerX = x + width / 2.0F;
        float centerY = y + height / 2.0F;

        float targetHealth = TargetHud2.finiteHealth(data.targetHealth());
        float playerHealth = TargetHud2.finiteHealth(data.playerHealth());
        float maxHealth = Math.max(TargetHud2.finiteHealth(data.maxHealth()), 1.0F);
        float absorption = TargetHud2.finiteHealth(data.absorption());
        float animatedHealth = this.getAnimatedHealth(targetHUD, targetHealth);
        float animatedMaxHealth = Math.max(TargetHud2.finiteHealth(targetHUD.maxHealth), maxHealth);
        float ratio = Math.clamp(animatedHealth / animatedMaxHealth, 0.0F, 1.0F);
        float absorptionRatio = Math.clamp(absorption / maxHealth, 0.0F, 1.0F);
        float space = width - 43.0F;
        int[] colors = targetHUD.getRavenGradientColors();
        float partialTicks = TargetHud2.getPartialTicks();
        float hurtProgress = data.entity().hurtTime == 0
                ? 0.0F
                : Math.clamp((data.entity().hurtTime - partialTicks) / 10.0F, 0.0F, 1.0F);

        if (this.background.getValue() > 0) {
            int backgroundAlpha = (int) (this.background.getValue() / 100.0F * fadeAlpha);
            RenderUtil.drawRoundedRectangle(
                    this.scaleX(x, centerX, scale),
                    this.scaleY(y, centerY, scale),
                    this.scaleX(x + width, centerX, scale),
                    this.scaleY(y + height, centerY, scale),
                    this.scaleSize(7.0F, scale),
                    new Color(15, 15, 18, backgroundAlpha).getRGB()
            );
        }
        RenderUtil.drawRoundedRectangle(
                this.scaleX(x + 38.5F, centerX, scale),
                this.scaleY(y + 28.0F, centerY, scale),
                this.scaleX(x + 38.5F + space, centerX, scale),
                this.scaleY(y + 32.0F, centerY, scale),
                this.scaleSize(2.0F, scale),
                new Color(0, 0, 0, (int) (150.0F * progress)).getRGB()
        );
        if (ratio > 0.01F) {
            RenderUtil.drawRoundedGradientRect(
                    this.scaleX(x + 38.5F, centerX, scale),
                    this.scaleY(y + 28.0F, centerY, scale),
                    this.scaleX(x + 38.5F + space * ratio, centerX, scale),
                    this.scaleY(y + 32.0F, centerY, scale),
                    this.scaleSize(2.0F, scale),
                    RenderUtil.mergeAlpha(colors[0], fadeAlpha), RenderUtil.mergeAlpha(colors[0], fadeAlpha),
                    RenderUtil.mergeAlpha(colors[1], fadeAlpha), RenderUtil.mergeAlpha(colors[1], fadeAlpha)
            );
        }
        if (absorptionRatio > 0.01F) {
            RenderUtil.drawRoundedGradientRect(
                    this.scaleX(x + 38.5F, centerX, scale),
                    this.scaleY(y + 28.0F, centerY, scale),
                    this.scaleX(x + 38.5F + space * absorptionRatio, centerX, scale),
                    this.scaleY(y + 32.0F, centerY, scale),
                    this.scaleSize(2.0F, scale),
                    new Color(255, 210, 55, fadeAlpha).getRGB(), new Color(255, 210, 55, fadeAlpha).getRGB(),
                    new Color(255, 235, 110, fadeAlpha).getRGB(), new Color(255, 235, 110, fadeAlpha).getRGB()
            );
        }

        float targetHp = animatedHealth;
        float playerHp = playerHealth;
        String health = this.floorToTwoPlaces(targetHp) + "HP";
        String diff = this.diffText(playerHp, targetHealth);
        Fonts.Renderer nameFont = Fonts.interSemiBold.get(18.0F);
        Fonts.Renderer infoFont = Fonts.interSemiBold.get(13.0F);
        com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().pushMatrix();
        com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().translate(centerX, centerY, 0.0F);
        com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().scale(scale, scale, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().translate(-centerX, -centerY, 0.0F);
        nameFont.drawStringWithShadow(data.entity().getDisplayName().getString(), x + 37.0F, y + 5.0F, RenderUtil.mergeAlpha(Color.WHITE.getRGB(), fadeAlpha));
        infoFont.drawStringWithShadow(health, x + 37.0F, y + 17.0F, RenderUtil.mergeAlpha(Color.LIGHT_GRAY.getRGB(), fadeAlpha));
        infoFont.drawStringWithShadow(diff, x + 115.0F - infoFont.getStringWidth(diff), y + 17.0F, RenderUtil.mergeAlpha(Color.LIGHT_GRAY.getRGB(), fadeAlpha));
        com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().popMatrix();

        float headHurtScale = 1.0F - 0.15F * this.easeOutQuad(hurtProgress);
        int greenBlue = (int) (255.0F * (1.0F - 0.75F * hurtProgress));
        Color headColor = new Color(255, Math.clamp(greenBlue, 0, 255), Math.clamp(greenBlue, 0, 255), fadeAlpha);
        float baseHeadX = this.scaleX(x + 2.5F, centerX, scale);
        float baseHeadY = this.scaleY(y + 2.5F, centerY, scale);
        float baseHeadSize = this.scaleSize(32.0F, scale);
        float headSize = baseHeadSize * headHurtScale;
        float headX = baseHeadX + (baseHeadSize - headSize) / 2.0F;
        float headY = baseHeadY + (baseHeadSize - headSize) / 2.0F;
        float headRadius = this.scaleSize(5.0F, scale) * headHurtScale;
        RenderUtil.drawRoundedRect(
                headX,
                headY,
                headSize,
                headSize,
                headRadius,
                playerHp >= targetHealth ? new Color(0, 0, 0, 0).getRGB() : new Color(255, 0, 0, (int) (85.0F * progress)).getRGB());
        targetHUD.renderRoundedPlayerHead(
                data.entity(),
                headX,
                headY,
                headSize,
                headRadius,
                headColor.getRGB()
        );
    }

    @Override
    public float[] getSize(TargetHud2 targetHUD, TargetHud2.RenderData data) {
        return new float[]{120.0F, 37.0F};
    }

    @Override
    public boolean shouldRenderEffects(TargetHud2 targetHUD) {
        return this.background.getValue() > 0;
    }

    @Override
    public void renderMask(TargetHud2 targetHUD, TargetHud2.RenderData data, float x, float y, int color) {
        float[] size = this.getSize(targetHUD, data);
        int fadeAlpha = targetHUD.getFadeAlpha();
        if (fadeAlpha <= 0) {
            return;
        }
        float scale = this.getPopScale(fadeAlpha / 255.0F);
        float centerX = x + size[0] / 2.0F;
        float centerY = y + size[1] / 2.0F;
        RenderUtil.enableRenderState();
        RenderUtil.drawRoundedRectangle(
                this.scaleX(x, centerX, scale),
                this.scaleY(y, centerY, scale),
                this.scaleX(x + size[0], centerX, scale),
                this.scaleY(y + size[1], centerY, scale),
                this.scaleSize(7.0F, scale),
                RenderUtil.mergeAlpha(color, (color >> 24 & 255) * fadeAlpha / 255)
        );
        RenderUtil.disableRenderState();
    }

    private float getPopScale(float progress) {
        return 0.82F + this.easeOutBack(progress) * 0.18F;
    }

    private float scaleX(float value, float centerX, float scale) {
        return centerX + (value - centerX) * scale;
    }

    private float scaleY(float value, float centerY, float scale) {
        return centerY + (value - centerY) * scale;
    }

    private float scaleSize(float value, float scale) {
        return value * scale;
    }

    private float easeOutBack(float progress) {
        float t = Math.clamp(progress, 0.0F, 1.0F) - 1.0F;
        float c = 1.70158F;
        return t * t * ((c + 1.0F) * t + c) + 1.0F;
    }

    private float easeOutQuad(float progress) {
        float t = Math.clamp(progress, 0.0F, 1.0F);
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    private float getAnimatedHealth(TargetHud2 targetHUD, float fallbackHealth) {
        boolean hasAnimationState = targetHUD.maxHealth > 0.0F || targetHUD.oldHealth != 0.0F || targetHUD.newHealth != 0.0F;
        if (!this.animations.getValue() || !hasAnimationState) {
            return fallbackHealth;
        }
        float elapsedTime = (float) Math.clamp(targetHUD.animTimer.getElapsedTime(), 0L, 150L);
        return TargetHud2.finiteHealth(RenderUtil.lerpFloat(targetHUD.newHealth, targetHUD.oldHealth, elapsedTime / 150.0F));
    }

    @Override
    public boolean shouldAnimateHealth() {
        return this.animations.getValue();
    }

    private String diffText(float playerHealth, float targetHealth) {
        double diff = this.floorToTwoPlaces(TargetHud2.finiteOrDefault(playerHealth - targetHealth, 0.0F));
        if (diff > 0.0D) {
            return "+" + diff;
        }
        if (diff < 0.0D) {
            return String.valueOf(diff);
        }
        return "+0.0";
    }

    private double floorToTwoPlaces(float value) {
        return BigDecimal.valueOf(TargetHud2.finiteOrDefault(value, 0.0F)).setScale(2, RoundingMode.FLOOR).doubleValue();
    }
}