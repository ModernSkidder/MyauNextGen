package laoqi123.render;

import com.mojang.blaze3d.systems.RenderSystem;
import laoqi123.render.type.Color4b;
import laoqi123.render.type.Vec3f;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class LBRenderUtil {
    private static final int CIRCLE_RES = 40;

    private static final List<Vector3f> circlePoints = new ArrayList<>(CIRCLE_RES + 1);

    static {
        for (int i = 0; i <= CIRCLE_RES; i++) {
            double theta = Math.PI * 2f * i / CIRCLE_RES;
            circlePoints.add(new Vector3f((float) Math.cos(theta), 0f, (float) Math.sin(theta)));
        }
    }

    public static void enableWorldRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.SrcFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DstFactor.ZERO);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
    }

    public static void disableWorldRenderState() {
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void drawLine(Vec3f p1, Vec3f p2, int argb) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(1.5F);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(p1.x(), p1.y(), p1.z()).color(argb);
        bufferBuilder.vertex(p2.x(), p2.y(), p2.z()).color(argb);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.lineWidth(2.0F);
    }

    public static void drawLines(int argb, Vec3f... lines) {
        if (lines.length == 0) return;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(1.5F);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        for (Vec3f line : lines) {
            bufferBuilder.vertex(line.x(), line.y(), line.z()).color(argb);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.lineWidth(2.0F);
    }

    public static void drawLineStrip(int argb, Vec3f... positions) {
        if (positions.length == 0) return;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(1.5F);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (Vec3f position : positions) {
            bufferBuilder.vertex(position.x(), position.y(), position.z()).color(argb);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.lineWidth(2.0F);
    }

    private static void drawBoxVertices(Box box, BoxVertexIterator iterator, int argb, int verticesToUse) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        boolean check = (verticesToUse & 0xFFFFFF) != 0xFFFFFF;
        BufferBuilder bufferBuilder;
        if (iterator == BoxVertexIterator.OUTLINE) {
            RenderSystem.lineWidth(1.5F);
            bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        } else {
            bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        }
        iterator.forEachVertex(box, (index, x, y, z) -> {
            if (check && (verticesToUse & (1 << index)) == 0) return;
            bufferBuilder.vertex((float) x, (float) y, (float) z).color(argb);
        });
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        if (iterator == BoxVertexIterator.OUTLINE) {
            RenderSystem.lineWidth(2.0F);
        }
    }

    public static void drawBoxOutlined(Box box, Color4b color) {
        if (color.isTransparent()) return;
        drawBoxVertices(box, BoxVertexIterator.OUTLINE, color.toARGB(), -1);
    }

    public static void drawBox(Box box, Color4b faceColor, Color4b outlineColor) {
        if (faceColor != null && !faceColor.isTransparent()) {
            drawBoxVertices(box, BoxVertexIterator.FACE, faceColor.toARGB(), -1);
        }
        if (outlineColor != null && !outlineColor.isTransparent()) {
            drawBoxVertices(box, BoxVertexIterator.OUTLINE, outlineColor.toARGB(), -1);
        }
    }

    public static void drawBoxSide(Box box, Direction side, Color4b faceColor, Color4b outlineColor) {
        if (faceColor != null && !faceColor.isTransparent()) {
            drawBoxVertices(box, BoxVertexIterator.FACE, faceColor.toARGB(), BoxVertexIterator.FACE.sideMask(side));
        }
        if (outlineColor != null && !outlineColor.isTransparent()) {
            drawBoxVertices(box, BoxVertexIterator.OUTLINE, outlineColor.toARGB(), BoxVertexIterator.OUTLINE.sideMask(side));
        }
    }

    public static void drawBoxSides(Box box, Iterable<Direction> sides, Color4b faceColor, Color4b outlineColor) {
        int faceMask = 0;
        int outlineMask = 0;
        for (Direction side : sides) {
            faceMask |= BoxVertexIterator.FACE.sideMask(side);
            outlineMask |= BoxVertexIterator.OUTLINE.sideMask(side);
        }
        if (faceColor != null && !faceColor.isTransparent()) {
            drawBoxVertices(box, BoxVertexIterator.FACE, faceColor.toARGB(), faceMask);
        }
        if (outlineColor != null && !outlineColor.isTransparent()) {
            drawBoxVertices(box, BoxVertexIterator.OUTLINE, outlineColor.toARGB(), outlineMask);
        }
    }

    public static void drawPlane(float sizeX, float sizeZ, Color4b fillColor, Color4b outlineColor) {
        if (fillColor != null && !fillColor.isTransparent()) {
            int argb = fillColor.toARGB();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            bufferBuilder.vertex(0f, 0f, 0f).color(argb);
            bufferBuilder.vertex(0f, 0f, sizeZ).color(argb);
            bufferBuilder.vertex(sizeX, 0f, sizeZ).color(argb);
            bufferBuilder.vertex(sizeX, 0f, 0f).color(argb);
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        }
        if (outlineColor != null && !outlineColor.isTransparent()) {
            int argb = outlineColor.toARGB();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.lineWidth(1.5F);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
            bufferBuilder.vertex(0f, 0f, 0f).color(argb);
            bufferBuilder.vertex(0f, 0f, sizeZ).color(argb);
            bufferBuilder.vertex(0f, 0f, sizeZ).color(argb);
            bufferBuilder.vertex(sizeX, 0f, sizeZ).color(argb);
            bufferBuilder.vertex(sizeX, 0f, sizeZ).color(argb);
            bufferBuilder.vertex(sizeX, 0f, 0f).color(argb);
            bufferBuilder.vertex(sizeX, 0f, 0f).color(argb);
            bufferBuilder.vertex(0f, 0f, 0f).color(argb);
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            RenderSystem.lineWidth(2.0F);
        }
    }

    public static void drawCircleOutline(float radius, Color4b color4b) {
        int argb = color4b.toARGB();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(1.5F);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (Vector3f point : circlePoints) {
            bufferBuilder.vertex(point.x * radius, point.y, point.z * radius).color(argb);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.lineWidth(2.0F);
    }

    public static void drawGradientCircle(float outerRadius, float innerRadius, Color4b outerColor4b, Color4b innerColor4b) {
        drawGradientCircle(outerRadius, innerRadius, outerColor4b, innerColor4b, new Vector3f());
    }

    public static void drawGradientCircle(float outerRadius, float innerRadius, Color4b outerColor4b, Color4b innerColor4b, Vector3f innerOffset) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        Vector3f innerP = new Vector3f();
        Vector3f outerP = new Vector3f();
        for (Vector3f p : circlePoints) {
            outerP.set(p).mul(outerRadius);
            innerP.set(p).mul(innerRadius).add(innerOffset);
            bufferBuilder.vertex(outerP.x, outerP.y, outerP.z).color(outerColor4b.toARGB());
            bufferBuilder.vertex(innerP.x, innerP.y, innerP.z).color(innerColor4b.toARGB());
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public static void drawGradientSides(double height, Color4b baseColor, Color4b topColor, Box box) {
        if (height == 0.0) {
            return;
        }
        drawGradientQuad(
                new Vec3f(box.minX, 0.0, box.minZ),
                new Vec3f(box.minX, height, box.minZ),
                new Vec3f(box.maxX, height, box.minZ),
                new Vec3f(box.maxX, 0.0, box.minZ),
                baseColor, topColor
        );
        drawGradientQuad(
                new Vec3f(box.maxX, 0.0, box.minZ),
                new Vec3f(box.maxX, height, box.minZ),
                new Vec3f(box.maxX, height, box.maxZ),
                new Vec3f(box.maxX, 0.0, box.maxZ),
                baseColor, topColor
        );
        drawGradientQuad(
                new Vec3f(box.maxX, 0.0, box.maxZ),
                new Vec3f(box.maxX, height, box.maxZ),
                new Vec3f(box.minX, height, box.maxZ),
                new Vec3f(box.minX, 0.0, box.maxZ),
                baseColor, topColor
        );
        drawGradientQuad(
                new Vec3f(box.minX, 0.0, box.maxZ),
                new Vec3f(box.minX, height, box.maxZ),
                new Vec3f(box.minX, height, box.minZ),
                new Vec3f(box.minX, 0.0, box.minZ),
                baseColor, topColor
        );
    }

    private static void drawGradientQuad(Vec3f p1, Vec3f p2, Vec3f p3, Vec3f p4, Color4b baseColor, Color4b topColor) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(p1.x(), p1.y(), p1.z()).color(baseColor.toARGB());
        bufferBuilder.vertex(p2.x(), p2.y(), p2.z()).color(topColor.toARGB());
        bufferBuilder.vertex(p3.x(), p3.y(), p3.z()).color(topColor.toARGB());
        bufferBuilder.vertex(p4.x(), p4.y(), p4.z()).color(baseColor.toARGB());
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public static Vec3f relativeToCamera(Vec3f pos) {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        var cameraPos = mc.gameRenderer.getCamera().getPos();
        return new Vec3f(
                pos.x() - (float) cameraPos.x,
                pos.y() - (float) cameraPos.y,
                pos.z() - (float) cameraPos.z
        );
    }
}
