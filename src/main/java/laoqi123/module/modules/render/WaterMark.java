package laoqi123.module.modules.render;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.events.Render2DEvent;
import laoqi123.font.UFontRenderer;
import laoqi123.module.Module;
import laoqi123.property.properties.ModeProperty;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class WaterMark extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeProperty fontMode = new ModeProperty("FontMode", 0, new String[]{"Minecraft", "Modern"});

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

    private void drawStringWithShadow(DrawContext context, String text, float x, float y, int color) {
        UFontRenderer font = getFontRenderer();
        if (font != null) {
            font.drawStringWithShadow(text, x, y, color);
        } else {
            context.drawText(mc.textRenderer, text, (int) x, (int) y, color, true);
        }
    }

    private float getStringWidth(String text) {
        UFontRenderer font = getFontRenderer();
        if (font != null) {
            return font.getStringWidth(text);
        }
        return mc.textRenderer.getWidth(text);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) return;
        renderExhibition(event.getContext());
    }

    private void renderExhibition(DrawContext context) {
        int fps = mc.getCurrentFps();
        int ping = 0;

        if (mc.player != null && mc.world != null) {
            if (mc.getNetworkHandler() != null
                    && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) {
                ping = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
            }
        }

        String exhibitionText = "Myau";
        String restText = " NextGen";
        String fpsValue = fps + "FPS";
        String pingValue = ping + "ms";

        HUD hud = (HUD) Myau.moduleManager.getModule(HUD.class);

        float x = 2.0f;
        float y = 2.0f;

        long time = System.currentTimeMillis();
        int rainbowColor = hud != null ? hud.getColor(time).getRGB() : 0xFFFFFFFF;

        drawStringWithShadow(context, exhibitionText, x, y, rainbowColor);
        float currentX = x + getStringWidth(exhibitionText);

        drawStringWithShadow(context, restText, currentX, y, rainbowColor);
        currentX += getStringWidth(restText);

        int whiteColor = 0xFFFFFFFF;

        int grayColor = 0xFFAAAAAA;
        drawStringWithShadow(context, "[", currentX, y, grayColor);
        currentX += getStringWidth("[");

        drawStringWithShadow(context, fpsValue, currentX, y, whiteColor);
        currentX += getStringWidth(fpsValue);

        drawStringWithShadow(context, "]", currentX, y, grayColor);
        currentX += getStringWidth("]");

        String space = " ";
        drawStringWithShadow(context, space, currentX, y, whiteColor);
        currentX += getStringWidth(space);

        drawStringWithShadow(context, "[", currentX, y, grayColor);
        currentX += getStringWidth("[");

        drawStringWithShadow(context, pingValue, currentX, y, whiteColor);
        currentX += getStringWidth(pingValue);

        drawStringWithShadow(context, "]", currentX, y, grayColor);
    }
}
