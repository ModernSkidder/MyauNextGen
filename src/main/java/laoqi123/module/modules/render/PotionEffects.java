package laoqi123.module.modules.render;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.impl.Render2DEvent;
import laoqi123.font.UFontRenderer;
import laoqi123.module.Module;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.IntValue;
import laoqi123.value.properties.ModeValue;
import laoqi123.value.properties.PercentValue;
import laoqi123.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class PotionEffects extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public final PercentValue background = new PercentValue("Background", 50);
    public final IntValue offsetX = new IntValue("OffsetX", 5, -1000, 1000);
    public final IntValue offsetY = new IntValue("OffsetY", 80, -1000, 1000);
    public final ModeValue fontMode = new ModeValue("font-mode", 0, new String[]{"Minecraft", "Modern"});
    public final BooleanValue text = new BooleanValue("Text", true);

    private float currentHeight = 0.0f;
    private UFontRenderer modernFont;

    private UFontRenderer getModernFont() {
        if (modernFont == null) {
            try {
                modernFont = new UFontRenderer("GoogleSans-Regular", 20);
            } catch (Exception e) {
                modernFont = null;
            }
        }
        return modernFont;
    }

    private boolean isModern() {
        return this.fontMode.getValue() == 1 && getModernFont() != null;
    }

    private float getFontHeight() {
        return isModern() ? getModernFont().getHeight() : mc.textRenderer.fontHeight;
    }

    private int getStringWidth(String str) {
        return isModern() ? getModernFont().getStringWidth(str) : mc.textRenderer.getWidth(str);
    }

    private void drawString(DrawContext context, String str, float x, float y, int color, boolean shadow) {
        if (isModern()) {
            UFontRenderer fr = getModernFont();
            if (shadow) fr.drawStringWithShadow(str, x, y, color);
            else fr.drawString(str, x, y, color);
        } else {
            context.drawText(mc.textRenderer, str, (int) x, (int) y, color, shadow);
        }
    }

    public PotionEffects() {
        super("PotionEffects", false);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) return;
        if (mc.player == null || mc.world == null) return;

        Collection<StatusEffectInstance> active = mc.player.getStatusEffects();
        if (active == null || active.isEmpty()) return;

        List<StatusEffectInstance> potions = new ArrayList<>(active);
        potions.sort(Comparator.comparingInt(StatusEffectInstance::getDuration).reversed());

        float padding = 5f;
        float fontHeight = getFontHeight();
        float iconSize = 18f;
        float rowHeight = this.text.getValue() ? fontHeight + padding : iconSize + padding;

        String title = "Potions";
        float titleWidth = getStringWidth(title);

        float maxWidth = this.text.getValue() ? (titleWidth + padding * 2) : (iconSize + padding * 2 + 4f);
        float listHeight = 0f;
        float topPadding = padding;
        float bottomPadding = padding;

        for (StatusEffectInstance effect : potions) {
            float localWidth;
            if (this.text.getValue()) {
                String potionName = effect.getEffectType().value().getName().getString();
                String amp = effect.getAmplifier() > 0 ? " " + Text.translatable("enchantment.level." + (effect.getAmplifier() + 1)).getString() : "";
                String nameText = potionName + amp;
                String durationText = getDurationString(effect);
                float nameW = getStringWidth(nameText);
                float durW = getStringWidth(durationText);
                localWidth = nameW + durW + padding * 3 + iconSize + 4f;
            } else {
                localWidth = iconSize + padding * 2 + 4f;
            }

            if (localWidth > maxWidth) maxWidth = localWidth;
            listHeight += rowHeight;
        }

        float width = Math.max(maxWidth, 80f);
        float headerHeight = this.text.getValue() ? fontHeight + padding * 2 : 0f;
        float extraTop = this.text.getValue() ? 1.25f + 7.5f : 0f;
        float targetHeight = topPadding + headerHeight + extraTop + listHeight + bottomPadding;

        if (currentHeight <= 0.0f) {
            currentHeight = targetHeight;
        } else {
            float speed = 0.22f;
            currentHeight += (targetHeight - currentHeight) * speed;
        }

        float height = Math.max(headerHeight + 1f, currentHeight);

        float scaledWidth = mc.getWindow().getScaledWidth();
        float scaledHeight = mc.getWindow().getScaledHeight();
        float x = scaledWidth - width - 10 + offsetX.getValue();
        float y = 20 + offsetY.getValue();

        HUD hud = (HUD) Myau.moduleManager.getModule(HUD.class);
        int accent = hud != null ? hud.getColor(System.currentTimeMillis()).getRGB() : 0xFF80FF95;

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        int bgAlpha = (int) ((float) background.getValue() / 100f * 255);
        int bg = new Color(0, 0, 0, Math.min(255, Math.max(0, bgAlpha))).getRGB();
        RenderUtil.drawRect((int) x, (int) y, (int) (x + width), (int) (y + height), bg);

        float currentY = y + topPadding;

        if (this.text.getValue()) {
            float titleX = x + width / 2f - titleWidth / 2f;
            drawString(event.getContext(), title, titleX, currentY, 0xFFFFFFFF, true);
            currentY += fontHeight + padding * 2;

            float dividerY = currentY;
            Color dividerColor = new Color(accent);
            dividerColor = new Color(
                    Math.max(0, dividerColor.getRed() - 60),
                    Math.max(0, dividerColor.getGreen() - 60),
                    Math.max(0, dividerColor.getBlue() - 60),
                    dividerColor.getAlpha()
            );
            RenderUtil.drawRect((int) (x + 0.5f), (int) (dividerY + 1.5f), (int) (x + width - 0.5f), (int) (dividerY + 1.5f + 1.25f), dividerColor.getRGB());
            currentY = dividerY + 7.5f;
        }

        float iconX = x + padding;
        float textX = x + padding + iconSize + 4f;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        float scale = (float) mc.getWindow().getScaleFactor();
        int scissorX = (int) (x * scale);
        int scissorY = (int) ((scaledHeight - (y + height)) * scale);
        int scissorW = (int) (width * scale);
        int scissorH = (int) (height * scale);
        GL11.glScissor(scissorX, scissorY, scissorW, scissorH);

        for (StatusEffectInstance effect : potions) {
            float iconDrawY = currentY + (rowHeight - iconSize) / 2f;
            RenderUtil.renderPotionEffect(effect, (int) iconX, (int) iconDrawY);

            if (this.text.getValue()) {
                String potionName = effect.getEffectType().value().getName().getString();
                String amp = effect.getAmplifier() > 0 ? " " + Text.translatable("enchantment.level." + (effect.getAmplifier() + 1)).getString() : "";
                String nameText = potionName + amp;
                String durationText = getDurationString(effect);

                float durW = getStringWidth(durationText);
                drawString(event.getContext(), nameText, textX, currentY, 0xFFFFFFFF, true);
                drawString(event.getContext(), durationText, x + width - padding - durW, currentY, 0xFFFFFFFF, true);
            }

            currentY += rowHeight;
            if (currentY > y + height - padding) break;
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    private static String getDurationString(StatusEffectInstance effect) {
        int total = effect.getDuration() / 20;
        if (total >= 3600) {
            return String.format("%d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60);
        }
        return String.format("%d:%02d", total / 60, total % 60);
    }
}
