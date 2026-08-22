package laoqi123.module.modules;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.events.Render2DEvent;
import laoqi123.font.UFontRenderer;
import laoqi123.module.Module;
import laoqi123.oneconfig.Glass;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.property.properties.ModeProperty;
import laoqi123.property.properties.PercentProperty;
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

    public final PercentProperty background = new PercentProperty("Background", 50);
    public final IntProperty offsetX = new IntProperty("OffsetX", 5, -1000, 1000);
    public final IntProperty offsetY = new IntProperty("OffsetY", 80, -1000, 1000);
    public final ModeProperty fontMode = new ModeProperty("font-mode", 0, new String[]{"Minecraft", "Modern"});
    public final BooleanProperty text = new BooleanProperty("Text", true);

    private float currentHeight = 0.0f;
    private UFontRenderer modernFont;

    /** Last drawn size, published so the HUD Designer can size its drag box. */
    private float lastWidth = 0.0f;
    private float lastHeight = 0.0f;

    public float getLastWidth() {
        return lastWidth;
    }

    public float getLastHeight() {
        return lastHeight;
    }

    /**
     * Screen X for the given content width. The overlay is anchored to the right edge,
     * so the stored offset counts leftwards from there.
     */
    public float screenX(float width) {
        return mc.getWindow().getScaledWidth() - width - 10 + offsetX.getValue();
    }

    public float screenY() {
        return 20 + offsetY.getValue();
    }

    /** Move the overlay to an absolute screen position, inverting {@link #screenX}. */
    public void setScreenPos(float x, float y) {
        offsetX.setValue(Math.round(x - (mc.getWindow().getScaledWidth() - lastWidth - 10)));
        offsetY.setValue(Math.round(y - 20));
    }

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
        // The Compose HUD normally draws this overlay inside OneConfig's Skia scene, where
        // the frosted background lives. This path remains the fallback for when OneConfig's
        // UI cannot start, so the list never silently disappears.
        if (laoqi123.oneconfig.huds.PotionEffectsComposeHud.isActive()) return;
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

        lastWidth = width;
        lastHeight = height;

        float scaledWidth = mc.getWindow().getScaledWidth();
        float scaledHeight = mc.getWindow().getScaledHeight();
        float x = screenX(width);
        float y = screenY();

        HUD hud = (HUD) Myau.moduleManager.getModule(HUD.class);
        int accent = hud != null ? hud.getColor(System.currentTimeMillis()).getRGB() : 0xFF80FF95;

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        // OneConfig HUD background. The Background slider scales the 50% black fill's
        // alpha, so 50 is the OneConfig default and 100 is opaque black.
        int fill = Glass.alpha(Glass.BG, Math.round(background.getValue() / 100.0F * 255.0F));
        Glass.panel(x, y, width, height, fill);

        float currentY = y + topPadding;

        if (this.text.getValue()) {
            float titleX = x + width / 2f - titleWidth / 2f;
            drawString(event.getContext(), title, titleX, currentY, Glass.TEXT, false);
            currentY += fontHeight + padding * 2;

            // Accent divider, inset so it clears the rounded corners.
            float dividerY = currentY + 1.5f;
            RenderUtil.drawRect(x + Glass.PAD, dividerY, x + width - Glass.PAD, dividerY + 1.0f,
                    Glass.withAlpha(accent, 0.45F));
            currentY = dividerY + 6.0f;
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
                drawString(event.getContext(), nameText, textX, currentY, Glass.TEXT, false);
                drawString(event.getContext(), durationText, x + width - padding - durW, currentY,
                        Glass.TEXT_SECONDARY, false);
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
