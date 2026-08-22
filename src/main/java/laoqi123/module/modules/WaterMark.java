package laoqi123.module.modules;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.events.Render2DEvent;
import laoqi123.font.UFontRenderer;
import laoqi123.module.Module;
import laoqi123.oneconfig.Glass;
import laoqi123.property.properties.IntProperty;
import laoqi123.property.properties.ModeProperty;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class WaterMark extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeProperty fontMode = new ModeProperty("FontMode", 0, new String[]{"Minecraft", "Modern"});

    /**
     * Screen position, owned by Myau's config but driven by OneConfig's HUD Designer.
     * Hidden from the settings list because the Designer is how they are meant to be
     * changed.
     *
     * <p>{@code -1} means "not placed yet" and resolves to the top-right corner on first
     * render, clear of the HUD module's ArrayList which occupies the top-left.
     */
    public final IntProperty posX = new IntProperty("position-x", -1, -1, 10000, () -> false);
    public final IntProperty posY = new IntProperty("position-y", -1, -1, 10000, () -> false);

    private UFontRenderer modernFont;
    private boolean modernFontLoaded = false;

    public WaterMark() {
        super("WaterMark", false, false);
    }

    private UFontRenderer getFontRenderer() {
        if (fontMode.getValue() == 1) { // Modern
            if (!modernFontLoaded) {
                try {
                    modernFont = new UFontRenderer("GoogleSans-Regular", 18);
                } catch (Exception e) {
                    modernFont = null;
                }
                modernFontLoaded = true;
            }
            if (modernFont != null) {
                return modernFont;
            }
        }
        return null;
    }

    private float getStringWidth(String text) {
        UFontRenderer font = getFontRenderer();
        if (font != null) {
            return font.getStringWidth(text);
        }
        return mc.textRenderer.getWidth(text);
    }

    /**
     * Drawn with {@code DrawContext} only when the Compose HUD is unavailable.
     *
     * <p>{@code laoqi123.oneconfig.huds.WaterMarkComposeHud} normally renders this
     * overlay inside OneConfig's Skia scene, where the frosted background lives. This
     * path stays as the fallback for when OneConfig's UI cannot start, so the watermark
     * never silently disappears.
     */
    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) return;
        if (laoqi123.oneconfig.huds.WaterMarkComposeHud.Companion.isActive()) return;
        resolveDefaultPosition();
        renderAt(event.getContext(), posX.getValue(), posY.getValue());
    }

    /**
     * Place the watermark in the top-right on first use. The default cannot be a fixed
     * number because it depends on the panel's measured width and the window size.
     */
    private void resolveDefaultPosition() {
        if (posX.getValue() >= 0 && posY.getValue() >= 0) {
            return;
        }
        int margin = 4;
        posX.setValue(Math.max(0,
                Math.round(mc.getWindow().getScaledWidth() - measureWidth() - margin)));
        posY.setValue(margin);
    }

    /**
     * Drawn as a OneConfig HUD: 50% black with 4px corners and 4px padding, the client
     * name in the accent colour and the readouts in muted secondary text.
     */
    public void renderAt(DrawContext context, float x, float y) {
        String name = "Myau NextGen";
        String stats = mc.getCurrentFps() + " FPS  " + ping() + " ms";

        float nameWidth = getStringWidth(name);
        float statsWidth = getStringWidth(stats);
        float textHeight = lineHeight();

        float width = Glass.PAD + nameWidth + SEPARATOR + statsWidth + Glass.PAD;
        float height = Glass.PAD + textHeight + Glass.PAD;

        Glass.panel(x, y, width, height);

        float textY = y + (height - textHeight) / 2.0F;
        drawString(context, name, x + Glass.PAD, textY, accentColor());
        drawString(context, stats, x + Glass.PAD + nameWidth + SEPARATOR, textY,
                Glass.TEXT_SECONDARY);
    }

    /** Gap between the client name and the stats block. */
    private static final float SEPARATOR = 10.0F;

    private int ping() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            return 0;
        }
        var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    private int accentColor() {
        HUD hud = (HUD) Myau.moduleManager.getModule(HUD.class);
        return hud != null ? hud.getColor(System.currentTimeMillis()).getRGB() : Glass.TEXT;
    }

    private void drawString(DrawContext context, String text, float x, float y, int color) {
        UFontRenderer font = getFontRenderer();
        if (font != null) {
            font.drawString(text, x, y, color);
        } else {
            context.drawText(mc.textRenderer, text, (int) x, (int) y, color, false);
        }
    }

    private float lineHeight() {
        UFontRenderer font = getFontRenderer();
        return font != null ? font.getHeight() : mc.textRenderer.fontHeight;
    }

    /** Panel width, used to size the HUD Designer's drag box. */
    public float measureWidth() {
        String stats = mc.getCurrentFps() + " FPS  " + ping() + " ms";
        return Glass.PAD + getStringWidth("Myau NextGen") + SEPARATOR
                + getStringWidth(stats) + Glass.PAD;
    }

    /** Panel height, used to size the HUD Designer's drag box. */
    public float measureHeight() {
        return Glass.PAD + lineHeight() + Glass.PAD;
    }
}
