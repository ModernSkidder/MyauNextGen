package laoqi123.util;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import laoqi123.enums.ChatColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4d;
import org.joml.Vector4f;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class RenderUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static Frustum cameraFrustum;
    private static IntBuffer viewportBuffer;
    private static FloatBuffer modelViewBuffer;
    private static FloatBuffer projectionBuffer;
    private static FloatBuffer vectorBuffer;
    private static Map<Identifier, EnchantmentData> enchantmentMap;

    static {
        RenderUtil.cameraFrustum = new Frustum(new Matrix4f(), new Matrix4f());
        RenderUtil.viewportBuffer = null;
        RenderUtil.modelViewBuffer = null;
        RenderUtil.projectionBuffer = null;
        RenderUtil.vectorBuffer = null;
        RenderUtil.enchantmentMap = new EnchantmentMap();
    }

    private static ChatColors getColorForLevel(int currentLevel, int maxLevel) {
        if (currentLevel > maxLevel) {
            return ChatColors.LIGHT_PURPLE;
        }
        if (currentLevel == maxLevel) {
            return ChatColors.RED;
        }
        switch (currentLevel) {
            case 1: {
                return ChatColors.AQUA;
            }
            case 2: {
                return ChatColors.GREEN;
            }
            case 3: {
                return ChatColors.YELLOW;
            }
            case 4: {
                return ChatColors.GOLD;
            }
        }
        return ChatColors.GRAY;
    }

    public static void drawOutlinedString(String text, float x, float y) {
        String string2 = text.replaceAll("(?i)§[\\da-f]", "");
        BufferAllocator allocator = new BufferAllocator(256);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        mc.textRenderer.drawWithOutline(Text.literal(string2).asOrderedText(), x, y, -1, 0xFF000000, RenderSystem.getModelViewMatrix(), immediate, 0xF000F0);
        immediate.draw();
    }

    public static void renderEnchantmentText(ItemStack itemStack, float x, float y, float scale) {
        ItemEnchantmentsComponent itemEnchantmentsComponent = itemStack.getEnchantments();
        if (itemEnchantmentsComponent != null && !itemEnchantmentsComponent.isEmpty()) {
            int i = 0;
            for (Entry<RegistryEntry<Enchantment>, Integer> entry : itemEnchantmentsComponent.getEnchantmentEntries()) {
                EnchantmentData enchantmentData = enchantmentMap.get(entry.getKey().getKey().get().getValue());
                if (enchantmentData == null) {
                    continue;
                }
                int level = entry.getValue();
                ChatColors chatColors = RenderUtil.getColorForLevel(level, enchantmentData.maxLevel);
                RenderUtil.drawOutlinedString(ChatColors.formatColor(String.format("&r%s%s%d&r", enchantmentData.shortName, chatColors, level)), x * (1.0f / scale), (y + (float) i * 4.0f) * (1.0f / scale));
                i++;
            }
        }
    }

    public static void renderItemInGUI(ItemStack itemStack, int x, int y) {
        BufferAllocator allocator = new BufferAllocator(2048);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        DrawContext context = new DrawContext(mc, immediate);
        context.drawItem(itemStack, x, y);
        context.drawStackOverlay(mc.textRenderer, itemStack, x, y);
        context.draw();
        RenderUtil.renderEnchantmentText(itemStack, x, y, 0.5f);
    }

    public static void renderPotionEffect(StatusEffectInstance potionEffect, int x, int y) {
        Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(potionEffect.getEffectType());
        BufferAllocator allocator = new BufferAllocator(256);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        DrawContext context = new DrawContext(mc, immediate);
        context.drawSpriteStretched(RenderLayer::getGuiTextured, sprite, x, y, 18, 18, 0xFFFFFFFF);
        context.draw();
    }

    public static void drawRect(float x1, float y1, float x2, float y2, int color) {
        if (color == 0) {
            return;
        }
        RenderUtil.setColor(color);
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        bufferBuilder.vertex(x1, y1, 0.0F);
        bufferBuilder.vertex(x1, y2, 0.0F);
        bufferBuilder.vertex(x2, y2, 0.0F);
        bufferBuilder.vertex(x2, y1, 0.0F);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawRect3D(float x1, float y1, float x2, float y2, int color) {
        if (color == 0) {
            return;
        }
        RenderUtil.setColor(color);
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        for (int i = 0; i < 2; ++i) {
            bufferBuilder.vertex(x1, y1, 0.0F);
            bufferBuilder.vertex(x1, y2, 0.0F);
            bufferBuilder.vertex(x2, y2, 0.0F);
            bufferBuilder.vertex(x2, y1, 0.0F);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawOutlineRect(float x1, float y1, float x2, float y2, float lineWidth, int backgroundColor, int lineColor) {
        RenderUtil.drawRect(0.0f, 0.0f, x2, 27.0f, backgroundColor);
        if (lineColor == 0) {
            return;
        }
        RenderUtil.setColor(lineColor);
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION);
        bufferBuilder.vertex(x1, y1, 0.0F);
        bufferBuilder.vertex(x1, y2, 0.0F);
        bufferBuilder.vertex(x2, y2, 0.0F);
        bufferBuilder.vertex(x2, y1, 0.0F);
        bufferBuilder.vertex(x1, y1, 0.0F);
        bufferBuilder.vertex(x2, y1, 0.0F);
        bufferBuilder.vertex(x1, y2, 0.0F);
        bufferBuilder.vertex(x2, y2, 0.0F);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.lineWidth(2.0f);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        RenderUtil.setColor(color);
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION);
        bufferBuilder.vertex(x1, y1, 0.0F);
        bufferBuilder.vertex(x2, y2, 0.0F);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.lineWidth(2.0f);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawLine3D(Vec3d start, double endX, double endY, double endZ, float red, float green, float blue, float alpha, float lineWidth) {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        RenderSystem.setShaderColor(red, green, blue, alpha);
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION);
        bufferBuilder.vertex((float) start.x, (float) start.y, (float) start.z);
        bufferBuilder.vertex((float) (endX - cameraPos.x), (float) (endY - cameraPos.y), (float) (endZ - cameraPos.z));
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.lineWidth(2.0f);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawArrow(float centerX, float centerY, float angle, float length, float lineWidth, int color) {
        float f6 = angle + (float) Math.toRadians(45.0);
        float f7 = angle - (float) Math.toRadians(45.0);
        RenderUtil.setColor(color);
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION);
        bufferBuilder.vertex(centerX, centerY, 0.0F);
        bufferBuilder.vertex(centerX + length * (float) Math.cos(f6), centerY + length * (float) Math.sin(f6), 0.0F);
        bufferBuilder.vertex(centerX, centerY, 0.0F);
        bufferBuilder.vertex(centerX + length * (float) Math.cos(f7), centerY + length * (float) Math.sin(f7), 0.0F);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.lineWidth(2.0f);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawTriangle(float centerX, float centerY, float angle, float length, int color) {
        float f5 = angle + (float) Math.toRadians(26.25);
        float f6 = angle - (float) Math.toRadians(26.25);
        RenderUtil.setColor(color);
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION);
        bufferBuilder.vertex(centerX, centerY, 0.0F);
        bufferBuilder.vertex(centerX + length * (float) Math.cos(f5), centerY + length * (float) Math.sin(f5), 0.0F);
        bufferBuilder.vertex(centerX + length * (float) Math.cos(f6), centerY + length * (float) Math.sin(f6), 0.0F);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawFramebuffer(Framebuffer framebuffer) {
        framebuffer.draw(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
    }

    public static void fillCircle(double x, double y, double radius, int segments, int color) {
        RenderUtil.setColor(color);
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION);
        bufferBuilder.vertex((float) x, (float) y, 0.0F);
        for (int i = 0; i <= segments; i++) {
            double angle = i * (Math.PI * 2.0 / segments);
            double px = x + Math.cos(angle) * radius;
            double py = y + Math.sin(angle) * radius;
            bufferBuilder.vertex((float) px, (float) py, 0.0F);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawCircle(double centerX, double centerY, double centerZ, double radius, int segments, int color) {
        RenderUtil.setColor(color);
        RenderSystem.lineWidth(3.0f);
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION);
        for (int i = 0; i <= segments; ++i) {
            double d5 = (double) i * (Math.PI * 2 / (double) segments);
            bufferBuilder.vertex((float) (centerX + Math.cos(d5) * radius), (float) centerY, (float) (centerZ + Math.sin(d5) * radius));
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.lineWidth(2.0f);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawEntityCircle(Entity entity, double radius, int segments, int color) {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        float partialTicks = mc.getRenderTickCounter().getTickDelta(true);
        double d2 = RenderUtil.lerpDouble(entity.getX(), entity.prevX, partialTicks) - cameraPos.x;
        double d3 = RenderUtil.lerpDouble(entity.getY(), entity.prevY, partialTicks) - cameraPos.y;
        double d4 = RenderUtil.lerpDouble(entity.getZ(), entity.prevZ, partialTicks) - cameraPos.z;
        RenderUtil.drawCircle(d2, d3, d4, radius, segments, color);
    }

    public static void drawFilledBox(Box axisAlignedBB, int red, int green, int blue) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        VertexRendering.drawFilledBox(new MatrixStack(), bufferBuilder, axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ, axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ, red, green, blue, 63);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public static void drawBoundingBox(Box axisAlignedBB, int red, int green, int blue, int alpha, float lineWidth) {
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        VertexRendering.drawBox(new MatrixStack(), bufferBuilder, axisAlignedBB, red, green, blue, alpha);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.lineWidth(2.0f);
    }

    public static void drawEntityBox(Entity entity, int red, int green, int blue) {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        float partialTicks = mc.getRenderTickCounter().getTickDelta(true);
        double d2 = RenderUtil.lerpDouble(entity.getX(), entity.prevX, partialTicks);
        double d3 = RenderUtil.lerpDouble(entity.getY(), entity.prevY, partialTicks);
        double d4 = RenderUtil.lerpDouble(entity.getZ(), entity.prevZ, partialTicks);
        RenderUtil.drawFilledBox(entity.getBoundingBox().expand(0.1, 0.1, 0.1).offset(d2 - entity.getX(), d3 - entity.getY(), d4 - entity.getZ()).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z), red, green, blue);
    }

    public static void drawEntityBoundingBox(Entity entity, int red, int green, int blue, int alpha, float lineWidth, double expand) {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        float partialTicks = mc.getRenderTickCounter().getTickDelta(true);
        double d2 = RenderUtil.lerpDouble(entity.getX(), entity.prevX, partialTicks);
        double d3 = RenderUtil.lerpDouble(entity.getY(), entity.prevY, partialTicks);
        double d4 = RenderUtil.lerpDouble(entity.getZ(), entity.prevZ, partialTicks);
        RenderUtil.drawBoundingBox(entity.getBoundingBox().expand(expand, expand, expand).offset(d2 - entity.getX(), d3 - entity.getY(), d4 - entity.getZ()).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z), red, green, blue, alpha, lineWidth);
    }

    public static void drawBlockBox(BlockPos blockPos, double height, int red, int green, int blue) {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        double x = blockPos.getX() - cameraPos.x;
        double y = blockPos.getY() - cameraPos.y;
        double z = blockPos.getZ() - cameraPos.z;
        RenderUtil.drawFilledBox(new Box(x, y, z, x + 1.0, y + height, z + 1.0), red, green, blue);
    }

    public static void drawBlockBoundingBox(BlockPos blockPos, double height, int red, int green, int blue, int alpha, float lineWidth) {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        double x = blockPos.getX() - cameraPos.x;
        double y = blockPos.getY() - cameraPos.y;
        double z = blockPos.getZ() - cameraPos.z;
        RenderUtil.drawBoundingBox(new Box(x, y, z, x + 1.0, y + height, z + 1.0), red, green, blue, alpha, lineWidth);
    }

    public static void drawCornerESP(PlayerEntity entity, float red, float green, float blue) {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        float partialTicks = mc.getRenderTickCounter().getTickDelta(true);
        float x = (float) (RenderUtil.lerpDouble(entity.getX(), entity.prevX, partialTicks) - cameraPos.x);
        float y = (float) (RenderUtil.lerpDouble(entity.getY(), entity.prevY, partialTicks) - cameraPos.y);
        float z = (float) (RenderUtil.lerpDouble(entity.getZ(), entity.prevZ, partialTicks) - cameraPos.z);
        MatrixStack matrices = new MatrixStack();
        matrices.translate(x, y + entity.getHeight() / 2.0F, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
        matrices.scale(-0.098F, -0.098F, 0.098F);
        float width = (float) (26.6 * entity.getWidth() / 2.0);
        float height = 12.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        RenderUtil.drawRectInMatrix(matrices, width, height - 1.0F, width - 4.0F, height);
        RenderUtil.drawRectInMatrix(matrices, -width, height - 1.0F, -width + 4.0F, height);
        RenderUtil.drawRectInMatrix(matrices, -width, height, -width + 1.0F, height - 4.0F);
        RenderUtil.drawRectInMatrix(matrices, width, height, width - 1.0F, height - 4.0F);
        RenderUtil.drawRectInMatrix(matrices, width, -height, width - 4.0F, -height + 1.0F);
        RenderUtil.drawRectInMatrix(matrices, -width, -height, -width + 4.0F, -height + 1.0F);
        RenderUtil.drawRectInMatrix(matrices, -width, -height + 1.0F, -width + 1.0F, -height + 4.0F);
        RenderUtil.drawRectInMatrix(matrices, width, -height + 1.0F, width - 1.0F, -height + 4.0F);
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        RenderUtil.drawRectInMatrix(matrices, width, height, width - 4.0F, height + 0.2F);
        RenderUtil.drawRectInMatrix(matrices, -width, height, -width + 4.0F, height + 0.2F);
        RenderUtil.drawRectInMatrix(matrices, -width - 0.2F, height + 0.2F, -width, height - 4.0F);
        RenderUtil.drawRectInMatrix(matrices, width + 0.2F, height + 0.2F, width, height - 4.0F);
        RenderUtil.drawRectInMatrix(matrices, width + 0.2F, -height, width - 4.0F, -height - 0.2F);
        RenderUtil.drawRectInMatrix(matrices, -width - 0.2F, -height, -width + 4.0F, -height - 0.2F);
        RenderUtil.drawRectInMatrix(matrices, -width - 0.2F, -height, -width, -height + 4.0F);
        RenderUtil.drawRectInMatrix(matrices, width + 0.2F, -height, width, -height + 4.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawFake2DESP(PlayerEntity entity, float red, float green, float blue) {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        float partialTicks = mc.getRenderTickCounter().getTickDelta(true);
        float x = (float) (RenderUtil.lerpDouble(entity.getX(), entity.prevX, partialTicks) - cameraPos.x);
        float y = (float) (RenderUtil.lerpDouble(entity.getY(), entity.prevY, partialTicks) - cameraPos.y);
        float z = (float) (RenderUtil.lerpDouble(entity.getZ(), entity.prevZ, partialTicks) - cameraPos.z);
        MatrixStack matrices = new MatrixStack();
        matrices.translate(x, y + entity.getHeight() / 2.0F, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
        matrices.scale(-0.1F, -0.1F, 0.1F);
        float width = (float) (23.3 * entity.getWidth() / 2.0);
        float height = 12.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        RenderUtil.drawRectInMatrix(matrices, width, height, -width, height + 0.4F);
        RenderUtil.drawRectInMatrix(matrices, width, -height, -width, -height + 0.4F);
        RenderUtil.drawRectInMatrix(matrices, width, -height + 0.4F, width - 0.4F, height + 0.4F);
        RenderUtil.drawRectInMatrix(matrices, -width, -height + 0.4F, -width + 0.4F, height + 0.4F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void draw3DRect(float x1, float y1, float x2, float y2) {
        RenderUtil.drawRectInMatrix(new MatrixStack(), x1, y1, x2, y2);
    }

    private static void drawRectInMatrix(MatrixStack matrices, float x1, float y1, float x2, float y2) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        MatrixStack.Entry entry = matrices.peek();
        bufferBuilder.vertex(entry, x2, y1, 0.0F);
        bufferBuilder.vertex(entry, x1, y1, 0.0F);
        bufferBuilder.vertex(entry, x1, y2, 0.0F);
        bufferBuilder.vertex(entry, x2, y2, 0.0F);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public static Vector4d projectToScreen(Entity entity, double screenScale) {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        float partialTicks = mc.getRenderTickCounter().getTickDelta(true);
        double d3 = RenderUtil.lerpDouble(entity.getX(), entity.prevX, partialTicks);
        double d4 = RenderUtil.lerpDouble(entity.getY(), entity.prevY, partialTicks);
        double d5 = RenderUtil.lerpDouble(entity.getZ(), entity.prevZ, partialTicks);
        Box axisAlignedBB = entity.getBoundingBox().expand(0.1, 0.1, 0.1).offset(d3 - entity.getX(), d4 - entity.getY(), d5 - entity.getZ());
        return RenderUtil.projectToScreen(axisAlignedBB, screenScale);
    }

    public static Vector4d projectToScreen(Box axisAlignedBB, double screenScale) {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        Vector4d vector4d = null;
        Matrix4f modelView = RenderSystem.getModelViewMatrix();
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        int viewportWidth = mc.getWindow().getFramebufferWidth();
        int viewportHeight = mc.getWindow().getFramebufferHeight();
        double[] xs = {axisAlignedBB.minX, axisAlignedBB.minX, axisAlignedBB.maxX, axisAlignedBB.maxX, axisAlignedBB.minX, axisAlignedBB.minX, axisAlignedBB.maxX, axisAlignedBB.maxX};
        double[] ys = {axisAlignedBB.minY, axisAlignedBB.maxY, axisAlignedBB.minY, axisAlignedBB.maxY, axisAlignedBB.minY, axisAlignedBB.maxY, axisAlignedBB.minY, axisAlignedBB.maxY};
        double[] zs = {axisAlignedBB.minZ, axisAlignedBB.minZ, axisAlignedBB.minZ, axisAlignedBB.minZ, axisAlignedBB.maxZ, axisAlignedBB.maxZ, axisAlignedBB.maxZ, axisAlignedBB.maxZ};
        for (int i = 0; i < 8; i++) {
            Vector4f vector4f = new Vector4f((float) (xs[i] - cameraPos.x), (float) (ys[i] - cameraPos.y), (float) (zs[i] - cameraPos.z), 1.0F);
            vector4f.mul(modelView);
            vector4f.mulProject(projection);
            if (vector4f.w != 0.0F) {
                vector4f.mul(1.0F / vector4f.w);
            }
            double sx = (vector4f.x * 0.5F + 0.5F) * viewportWidth / screenScale;
            double sy = (0.5F - vector4f.y * 0.5F) * viewportHeight / screenScale;
            double sz = vector4f.z;
            if (sz < 0.0 || sz >= 1.0) {
                continue;
            }
            if (vector4d == null) {
                vector4d = new Vector4d(sx, sy, sz, 0.0);
            }
            vector4d.x = Math.min(sx, vector4d.x);
            vector4d.y = Math.min(sy, vector4d.y);
            vector4d.z = Math.max(sx, vector4d.z);
            vector4d.w = Math.max(sy, vector4d.w);
        }
        return vector4d;
    }

    public static boolean isInViewFrustum(Box axisAlignedBB, double expand) {
        cameraFrustum = new Frustum(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
        return cameraFrustum.isVisible(axisAlignedBB.expand(expand, expand, expand));
    }

    public static void enableRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
    }

    public static void disableRenderState() {
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void setColor(int argb) {
        float f = (float) (argb >> 24 & 0xFF) / 255.0f;
        float f2 = (float) (argb >> 16 & 0xFF) / 255.0f;
        float f3 = (float) (argb >> 8 & 0xFF) / 255.0f;
        float f4 = (float) (argb & 0xFF) / 255.0f;
        RenderSystem.setShaderColor(f2, f3, f4, f);
    }

    public static float lerpFloat(float current, float previous, float t) {
        return previous + (current - previous) * t;
    }

    public static double lerpDouble(double current, double previous, double t) {
        return previous + (current - previous) * t;
    }

    public static final class EnchantmentData {
        public final String shortName;
        public final int maxLevel;

        public EnchantmentData(String shortName, int maxLevel) {
            this.shortName = shortName;
            this.maxLevel = maxLevel;
        }
    }

    static final class EnchantmentMap extends HashMap<Identifier, EnchantmentData> {
        EnchantmentMap() {
            this.put(Identifier.of("protection"), new EnchantmentData("Pr", 4));
            this.put(Identifier.of("fire_protection"), new EnchantmentData("Fp", 4));
            this.put(Identifier.of("fire_aspect"), new EnchantmentData("Ff", 4));
            this.put(Identifier.of("blast_protection"), new EnchantmentData("Bp", 4));
            this.put(Identifier.of("projectile_protection"), new EnchantmentData("Pp", 4));
            this.put(Identifier.of("respiration"), new EnchantmentData("Re", 3));
            this.put(Identifier.of("aqua_affinity"), new EnchantmentData("Aq", 1));
            this.put(Identifier.of("thorns"), new EnchantmentData("Th", 3));
            this.put(Identifier.of("depth_strider"), new EnchantmentData("Ds", 3));
            this.put(Identifier.of("sharpness"), new EnchantmentData("Sh", 5));
            this.put(Identifier.of("smite"), new EnchantmentData("Sm", 5));
            this.put(Identifier.of("bane_of_arthropods"), new EnchantmentData("BoA", 5));
            this.put(Identifier.of("knockback"), new EnchantmentData("Kb", 2));
            this.put(Identifier.of("looting"), new EnchantmentData("Lo", 3));
            this.put(Identifier.of("efficiency"), new EnchantmentData("Ef", 5));
            this.put(Identifier.of("silk_touch"), new EnchantmentData("St", 1));
            this.put(Identifier.of("unbreaking"), new EnchantmentData("Ub", 3));
            this.put(Identifier.of("fortune"), new EnchantmentData("Fo", 3));
            this.put(Identifier.of("power"), new EnchantmentData("Po", 5));
            this.put(Identifier.of("punch"), new EnchantmentData("Pu", 2));
            this.put(Identifier.of("flame"), new EnchantmentData("Fl", 1));
            this.put(Identifier.of("infinity"), new EnchantmentData("Inf", 1));
            this.put(Identifier.of("luck_of_the_sea"), new EnchantmentData("LoS", 3));
            this.put(Identifier.of("lure"), new EnchantmentData("Lu", 3));
        }
    }

    public static Framebuffer createFrameBuffer(Framebuffer framebuffer, boolean depth) {
        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();
        if (framebuffer == null || framebuffer.textureWidth != width || framebuffer.textureHeight != height) {
            if (framebuffer != null) {
                framebuffer.delete();
            }
            framebuffer = new SimpleFramebuffer(width, height, depth);
            framebuffer.setTexFilter(GlConst.GL_LINEAR);
        }
        return framebuffer;
    }

    public static void setAlphaLimit(float limit) {
    }

    public static void bindTexture(int texture) {
        GlStateManager._bindTexture(texture);
    }

    public static void drawQuads() {
        float width = mc.getWindow().getFramebufferWidth();
        float height = mc.getWindow().getFramebufferHeight();
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        bufferBuilder.vertex(0.0F, 0.0F, 0.0F);
        bufferBuilder.vertex(0.0F, height, 0.0F);
        bufferBuilder.vertex(width, height, 0.0F);
        bufferBuilder.vertex(width, 0.0F, 0.0F);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public static void drawQuad(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float x2, float y2, Color color) {
        builder.vertex(matrix, x1, y2, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        builder.vertex(matrix, x2, y2, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        builder.vertex(matrix, x2, y1, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        builder.vertex(matrix, x1, y1, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public static void drawRect(double left, double top, double right, double bottom, int color) {
        if (left < right) {
            double i = left;
            left = right;
            right = i;
        }
        if (top < bottom) {
            double j = top;
            top = bottom;
            bottom = j;
        }
        RenderUtil.setColor(color);
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        bufferBuilder.vertex((float) left, (float) bottom, 0.0F);
        bufferBuilder.vertex((float) right, (float) bottom, 0.0F);
        bufferBuilder.vertex((float) right, (float) top, 0.0F);
        bufferBuilder.vertex((float) left, (float) top, 0.0F);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        RenderUtil.drawRoundedRect((double) x, (double) y, (double) width, (double) height, (double) radius, color, true, true, true, true);
    }

    public static void drawRoundedRect(double x, double y, double width, double height, double radius, int color, boolean roundTopLeft, boolean roundTopRight, boolean roundBottomLeft, boolean roundBottomRight) {
        if (width <= 0.0D || height <= 0.0D || (color >>> 24) == 0) {
            return;
        }
        radius = Math.max(0.0D, Math.min(radius, Math.min(width, height) / 2.0D));
        if (radius <= 0.0D || !(roundTopLeft || roundTopRight || roundBottomLeft || roundBottomRight)) {
            RenderUtil.drawRect((float) x, (float) y, (float) (x + width), (float) (y + height), color);
            return;
        }
        RenderUtil.enableRenderState();
        RenderUtil.setColor(color);
        RenderUtil.drawQuadNoState(x + radius, y, x + width - radius, y + height);
        RenderUtil.drawQuadNoState(x, y + radius, x + radius, y + height - radius);
        RenderUtil.drawQuadNoState(x + width - radius, y + radius, x + width, y + height - radius);
        if (roundTopLeft) {
            RenderUtil.drawCornerFan(x + radius, y + radius, radius, 180.0D, 270.0D);
        } else {
            RenderUtil.drawQuadNoState(x, y, x + radius, y + radius);
        }
        if (roundTopRight) {
            RenderUtil.drawCornerFan(x + width - radius, y + radius, radius, 270.0D, 360.0D);
        } else {
            RenderUtil.drawQuadNoState(x + width - radius, y, x + width, y + radius);
        }
        if (roundBottomRight) {
            RenderUtil.drawCornerFan(x + width - radius, y + height - radius, radius, 0.0D, 90.0D);
        } else {
            RenderUtil.drawQuadNoState(x + width - radius, y + height - radius, x + width, y + height);
        }
        if (roundBottomLeft) {
            RenderUtil.drawCornerFan(x + radius, y + height - radius, radius, 90.0D, 180.0D);
        } else {
            RenderUtil.drawQuadNoState(x, y + height - radius, x + radius, y + height);
        }
        // setColor() leaves a global shader tint behind. Without restoring it every later
        // draw (text, item icons, the whole inventory screen) is multiplied by this
        // colour, which turned everything black.
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderUtil.disableRenderState();
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color, boolean roundTopLeft, boolean roundTopRight, boolean roundBottomLeft, boolean roundBottomRight) {
        RenderUtil.drawRoundedRect((double) x, (double) y, (double) width, (double) height, (double) radius, color, roundTopLeft, roundTopRight, roundBottomLeft, roundBottomRight);
    }

    public static void drawRoundedRectOutline(float x, float y, float width, float height, float radius, float lineWidth, int color, boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight) {
        radius = Math.min(radius, Math.min(width, height) / 2.0f);
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        RenderSystem.setShaderColor(r, g, b, a);
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION);
        float firstX = 0.0F;
        float firstY = 0.0F;
        boolean first = true;
        if (topLeft) {
            for (int i = 180; i <= 270; i += 3) {
                double rad = Math.toRadians(i);
                float px = (float) (x + radius + Math.cos(rad) * radius);
                float py = (float) (y + radius + Math.sin(rad) * radius);
                if (first) {
                    firstX = px;
                    firstY = py;
                    first = false;
                }
                bufferBuilder.vertex(px, py, 0.0F);
            }
        } else {
            bufferBuilder.vertex(x, y, 0.0F);
            if (first) {
                firstX = x;
                firstY = y;
                first = false;
            }
        }
        if (topRight) {
            for (int i = 270; i <= 360; i += 3) {
                double rad = Math.toRadians(i);
                float px = (float) (x + width - radius + Math.cos(rad) * radius);
                float py = (float) (y + radius + Math.sin(rad) * radius);
                if (first) {
                    firstX = px;
                    firstY = py;
                    first = false;
                }
                bufferBuilder.vertex(px, py, 0.0F);
            }
        } else {
            bufferBuilder.vertex(x + width, y, 0.0F);
            if (first) {
                firstX = x + width;
                firstY = y;
                first = false;
            }
        }
        if (bottomRight) {
            for (int i = 0; i <= 90; i += 3) {
                double rad = Math.toRadians(i);
                float px = (float) (x + width - radius + Math.cos(rad) * radius);
                float py = (float) (y + height - radius + Math.sin(rad) * radius);
                if (first) {
                    firstX = px;
                    firstY = py;
                    first = false;
                }
                bufferBuilder.vertex(px, py, 0.0F);
            }
        } else {
            bufferBuilder.vertex(x + width, y + height, 0.0F);
            if (first) {
                firstX = x + width;
                firstY = y + height;
                first = false;
            }
        }
        if (bottomLeft) {
            for (int i = 90; i <= 180; i += 3) {
                double rad = Math.toRadians(i);
                float px = (float) (x + radius + Math.cos(rad) * radius);
                float py = (float) (y + height - radius + Math.sin(rad) * radius);
                if (first) {
                    firstX = px;
                    firstY = py;
                    first = false;
                }
                bufferBuilder.vertex(px, py, 0.0F);
            }
        } else {
            bufferBuilder.vertex(x, y + height, 0.0F);
            if (first) {
                firstX = x;
                firstY = y + height;
                first = false;
            }
        }
        bufferBuilder.vertex(firstX, firstY, 0.0F);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.lineWidth(2.0f);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawQuadNoState(double x1, double y1, double x2, double y2) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        bufferBuilder.vertex((float) x1, (float) y1, 0.0F);
        bufferBuilder.vertex((float) x2, (float) y1, 0.0F);
        bufferBuilder.vertex((float) x2, (float) y2, 0.0F);
        bufferBuilder.vertex((float) x1, (float) y2, 0.0F);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    private static void drawCornerFan(double centerX, double centerY, double radius, double start, double end) {
        int segments = Math.max(12, (int) Math.ceil(radius * 3.0D));
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION);
        bufferBuilder.vertex((float) centerX, (float) centerY, 0.0F);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(start + (end - start) * i / segments);
            bufferBuilder.vertex((float) (centerX + Math.cos(angle) * radius), (float) (centerY + Math.sin(angle) * radius), 0.0F);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public static int mergeAlpha(int color, int alpha) {
        if (alpha < 0) {
            alpha = 0;
        }
        if (alpha > 255) {
            alpha = 255;
        }
        return color & 0x00FFFFFF | alpha << 24;
    }

    public static int darkenColor(int color, int amount) {
        int r = Math.max(0, (color >> 16 & 255) - amount);
        int g = Math.max(0, (color >> 8 & 255) - amount);
        int b = Math.max(0, (color & 255) - amount);
        return color & 0xFF000000 | r << 16 | g << 8 | b;
    }

    public static int lerpColor(int color1, int color2, float fraction) {
        fraction = Math.min(Math.max(fraction, 0.0F), 1.0F);
        int a1 = color1 >> 24 & 255;
        int a2 = color2 >> 24 & 255;
        int r1 = color1 >> 16 & 255;
        int r2 = color2 >> 16 & 255;
        int g1 = color1 >> 8 & 255;
        int g2 = color2 >> 8 & 255;
        int b1 = color1 & 255;
        int b2 = color2 & 255;
        return (a1 + (int) ((float) (a2 - a1) * fraction)) << 24
                | (r1 + (int) ((float) (r2 - r1) * fraction)) << 16
                | (g1 + (int) ((float) (g2 - g1) * fraction)) << 8
                | b1 + (int) ((float) (b2 - b1) * fraction);
    }

    public static void drawRoundedRectangle(float x1, float y1, float x2, float y2, float radius, int color) {
        float width = Math.max(0.0F, x2 - x1);
        float height = Math.max(0.0F, y2 - y1);
        RenderUtil.drawRoundedRect(x1, y1, width, height, Math.max(0.0F, radius), color);
    }

    public static void drawGradientRect(float x1, float y1, float x2, float y2, int topLeft, int topRight, int bottomLeft, int bottomRight) {
        if (Math.abs(x2 - x1) < 0.5F || Math.abs(y2 - y1) < 0.5F) {
            return;
        }
        RenderUtil.enableRenderState();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder gradient = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        gradient.vertex(x1, y1, 0.0F).color(topLeft >> 16 & 255, topLeft >> 8 & 255, topLeft & 255, topLeft >> 24 & 255);
        gradient.vertex(x1, y2, 0.0F).color(bottomLeft >> 16 & 255, bottomLeft >> 8 & 255, bottomLeft & 255, bottomLeft >> 24 & 255);
        gradient.vertex(x2, y2, 0.0F).color(bottomRight >> 16 & 255, bottomRight >> 8 & 255, bottomRight & 255, bottomRight >> 24 & 255);
        gradient.vertex(x2, y1, 0.0F).color(topRight >> 16 & 255, topRight >> 8 & 255, topRight & 255, topRight >> 24 & 255);
        BufferRenderer.drawWithGlobalProgram(gradient.end());
        RenderUtil.disableRenderState();
    }

    public static void drawRoundedGradientRect(float x1, float y1, float x2, float y2, float radius, int topLeft, int topRight, int bottomLeft, int bottomRight) {
        float width = x2 - x1;
        float height = y2 - y1;
        radius = Math.max(0.0F, Math.min(radius, Math.min(width, height) / 2.0F));
        if (radius <= 0.0F) {
            RenderUtil.drawGradientRect(x1, y1, x2, y2, topLeft, topRight, bottomLeft, bottomRight);
            return;
        }
        RenderUtil.enableRenderState();
        float horizontal = height > 0.0F ? radius / height : 0.0F;
        RenderUtil.drawGradientRect(x1 + radius, y1, x2 - radius, y2, RenderUtil.lerpColor(topLeft, bottomLeft, horizontal), RenderUtil.lerpColor(topRight, bottomRight, horizontal), RenderUtil.lerpColor(bottomLeft, topLeft, horizontal), RenderUtil.lerpColor(bottomRight, topRight, horizontal));
        int leftTop = RenderUtil.lerpColor(topLeft, bottomLeft, horizontal);
        int leftBottom = RenderUtil.lerpColor(bottomLeft, topLeft, horizontal);
        RenderUtil.drawGradientRect(x1, y1 + radius, x1 + radius, y2 - radius, topLeft, leftTop, leftBottom, bottomLeft);
        int rightTop = RenderUtil.lerpColor(topRight, bottomRight, horizontal);
        int rightBottom = RenderUtil.lerpColor(bottomRight, topRight, horizontal);
        RenderUtil.drawGradientRect(x2 - radius, y1 + radius, x2, y2 - radius, topRight, rightTop, rightBottom, bottomRight);
        float vertical = width > 0.0F ? radius / width : 0.0F;
        int topLeftCorner = RenderUtil.lerpColor(topLeft, topRight, vertical);
        int topRightCorner = RenderUtil.lerpColor(topRight, topLeft, vertical);
        RenderUtil.drawGradientRect(x1 + radius, y1, x2 - radius, y1 + radius, topLeft, topRight, topRightCorner, topLeftCorner);
        int bottomLeftCorner = RenderUtil.lerpColor(bottomLeft, bottomRight, vertical);
        int bottomRightCorner = RenderUtil.lerpColor(bottomRight, bottomLeft, vertical);
        RenderUtil.drawGradientRect(x1 + radius, y2 - radius, x2 - radius, y2, bottomLeft, bottomRight, bottomRightCorner, bottomLeftCorner);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderUtil.drawGradientCornerFan(x1 + radius, y1 + radius, radius, 180.0F, 270.0F, leftTop, topLeft);
        RenderUtil.drawGradientCornerFan(x2 - radius, y1 + radius, radius, 270.0F, 360.0F, topRight, rightTop);
        RenderUtil.drawGradientCornerFan(x2 - radius, y2 - radius, radius, 0.0F, 90.0F, bottomRight, rightBottom);
        RenderUtil.drawGradientCornerFan(x1 + radius, y2 - radius, radius, 90.0F, 180.0F, bottomLeft, leftBottom);
        RenderUtil.disableRenderState();
    }

    private static void drawGradientCornerFan(double centerX, double centerY, double radius, double start, double end, int startColor, int endColor) {
        int segments = Math.max(12, (int) Math.ceil(radius * 3.0D));
        BufferBuilder fan = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(start + (end - start) * i / segments);
            int color = RenderUtil.lerpColor(startColor, endColor, (float) i / (float) segments);
            fan.vertex((float) (centerX + Math.cos(angle) * radius), (float) (centerY + Math.sin(angle) * radius), 0.0F)
                    .color(color >> 16 & 255, color >> 8 & 255, color & 255, color >> 24 & 255);
        }
        BufferRenderer.drawWithGlobalProgram(fan.end());
    }

    public static void drawRoundedGradientOutlinedRectangle(float x1, float y1, float x2, float y2, float radius, int background, int gradient1, int gradient2) {
        RenderUtil.drawRoundedGradientRect(x1, y1, x2, y2, radius, gradient1, gradient1, gradient2, gradient2);
        RenderUtil.drawRoundedRectangle(x1 + 1.5F, y1 + 1.5F, x2 - 1.5F, y2 - 1.5F, Math.max(0.0F, radius - 1.5F), background);
    }

    public static void drawSkeetRect(float x, float y, float width, float height) {
        RenderUtil.drawRoundedRect(x, y, width, height, 3.0F, new Color(0, 0, 0, 90).getRGB());
        RenderUtil.drawRoundedRectOutline(x, y, width, height, 3.0F, 1.0F, new Color(200, 200, 200, 110).getRGB(), true, true, true, true);
        RenderUtil.drawLine(x + 3.0F, y + 0.5F, x + width - 3.0F, y + 0.5F, 1.0F, new Color(255, 255, 255, 140).getRGB());
    }

    public static void drawTexturedRect(net.minecraft.util.Identifier texture, float x, float y, float width, float height, float u0, float v0, float u1, float v1, int rgba) {
        RenderSystem.enableBlend();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
        RenderUtil.setColor(rgba);
        BufferBuilder direct = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        direct.vertex(x, y + height, 0.0F).texture(u0, v1).color(255, 255, 255, 255);
        direct.vertex(x + width, y + height, 0.0F).texture(u1, v1).color(255, 255, 255, 255);
        direct.vertex(x + width, y, 0.0F).texture(u1, v0).color(255, 255, 255, 255);
        direct.vertex(x, y, 0.0F).texture(u0, v0).color(255, 255, 255, 255);
        BufferRenderer.drawWithGlobalProgram(direct.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    public static void drawGuiText(String text, float x, float y, int color, boolean shadow) {
        RenderSystem.disableScissor();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(true);
        BufferAllocator allocator = new BufferAllocator(256);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        mc.textRenderer.draw(text, x, y, color, shadow, RenderSystem.getModelViewMatrix(), immediate, net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
        immediate.draw();
    }
}
