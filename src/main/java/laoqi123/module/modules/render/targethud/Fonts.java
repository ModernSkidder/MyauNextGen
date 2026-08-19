package laoqi123.module.modules.render.targethud;

import com.mojang.blaze3d.systems.RenderSystem;
import laoqi123.util.RenderUtil;
import net.minecraft.client.MinecraftClient;

public class Fonts {
    public static final Registry interSemiBold = new Registry();
    public static final Registry exhi = new Registry();

    public static class Registry {
        public Renderer get(float size) {
            return new Renderer(size);
        }
    }

    public static class Renderer {
        private static final float DEFAULT_SIZE = 9.0F;
        private final float size;

        private Renderer(float size) {
            this.size = size;
        }

        public int getStringWidth(String text) {
            float scale = this.size / DEFAULT_SIZE;
            return (int) (MinecraftClient.getInstance().textRenderer.getWidth(text) * scale);
        }

        public void drawStringWithShadow(String text, float x, float y, int color) {
            this.drawString(text, x, y, color, true);
        }

        public void drawString(String text, float x, float y, int color) {
            this.drawString(text, x, y, color, false);
        }

        public void drawString(String text, float x, float y, int color, boolean shadow) {
            float scale = this.size / DEFAULT_SIZE;
            if (Math.abs(scale - 1.0F) < 0.01F) {
                RenderUtil.drawGuiText(text, x, y, color, shadow);
                return;
            }
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().translate(x, y, 0.0F);
            RenderSystem.getModelViewStack().scale(scale, scale, 1.0F);
            RenderUtil.drawGuiText(text, 0.0F, 0.0F, color, shadow);
            RenderSystem.getModelViewStack().popMatrix();
        }
    }
}