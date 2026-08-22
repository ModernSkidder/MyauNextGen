package laoqi123.module.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.Priority;
import laoqi123.events.Render2DEvent;
import laoqi123.events.Render3DEvent;
import laoqi123.module.Module;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.ModeProperty;
import laoqi123.util.RenderUtil;
import laoqi123.util.TeamUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.joml.Vector4d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ESP extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private boolean outline = true;
    private boolean glow = true;

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"Outlined 2D", "Glow"});
    public final BooleanProperty skeleton = new BooleanProperty("skeleton", false);
    public final BooleanProperty players = new BooleanProperty("players", true);
    public final BooleanProperty mobs = new BooleanProperty("mobs", false);
    public final BooleanProperty animals = new BooleanProperty("animals", false);
    public final BooleanProperty items = new BooleanProperty("items", false);
    public final BooleanProperty arrows = new BooleanProperty("arrows", true);
    public final BooleanProperty showHealthBar = new BooleanProperty("show-health-bar", true);
    public final ModeProperty healthBarPosition = new ModeProperty("health-bar-position", 0, new String[]{"Bottom", "Top", "Left", "Right"});

    private final Map<Entity, Vector4d> entityBoxPositions = new HashMap<>();
    private final List<Vector4d> projectedPoints = new ArrayList<>();

    public ESP() {
        super("ESP", false);
    }

    public boolean isOutlineEnabled() {
        return this.outline;
    }

    public boolean isGlowEnabled() {
        return this.glow;
    }

    private boolean shouldShowEntity(Entity entity) {
        if (entity == mc.player) return false;
        if (entity instanceof PlayerEntity && this.players.getValue()) return true;
        if (entity instanceof AnimalEntity && this.animals.getValue()) return true;
        if (entity instanceof MobEntity && this.mobs.getValue()) return true;
        if (entity instanceof ItemEntity && this.items.getValue()) return true;
        if (entity instanceof ArrowEntity || entity instanceof PotionEntity) return this.arrows.getValue();
        return false;
    }

    private boolean isInRange(Entity entity) {
        return mc.player.distanceTo(entity) * mc.player.distanceTo(entity) < 10000.0;
    }

    private Color getEntityColor(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            if (TeamUtil.isFriend(player)) {
                return Myau.friendManager.getColor();
            } else if (TeamUtil.isTarget(player)) {
                return Myau.targetManager.getColor();
            }
            return TeamUtil.getTeamColor(player, 1.0F);
        }
        return new Color(-1);
    }

    public static boolean shouldOutline(Entity entity) {
        if (Myau.moduleManager == null) return false;
        ESP esp = (ESP) Myau.moduleManager.modules.get(ESP.class);
        if (esp == null || !esp.isEnabled() || esp.mode.getValue() != 1) return false;
        if (!esp.shouldShowEntity(entity)) return false;
        return esp.isInRange(entity);
    }

    public static int getOutlineColor(Entity entity) {
        if (Myau.moduleManager == null) return entity.getTeamColorValue();
        ESP esp = (ESP) Myau.moduleManager.modules.get(ESP.class);
        if (esp != null) {
            return esp.getEntityColor(entity).getRGB();
        }
        return entity.getTeamColorValue();
    }

    @EventTarget(Priority.HIGH)
    public void onRender(Render3DEvent event) {
        if (!this.isEnabled() || mc.world == null || mc.player == null) return;
        if (this.mode.getValue() != 0) {
            this.entityBoxPositions.clear();
            return;
        }
        this.entityBoxPositions.clear();
        double scaleFactor = mc.getWindow().getScaleFactor();
        for (Entity entity : mc.world.getEntities()) {
            if (!this.shouldShowEntity(entity) || !this.isInRange(entity)) continue;
            Vector4d proj = RenderUtil.projectToScreen(entity, scaleFactor);
            if (proj == null) continue;
            if (proj.z <= 0.0 || proj.w <= 0.0) continue;
            this.entityBoxPositions.put(entity, proj);
        }
    }

    @EventTarget(Priority.HIGH)
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || this.mode.getValue() != 0 || this.entityBoxPositions.isEmpty()) return;
        Matrix4f matrix4f = event.getContext().getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (Map.Entry<Entity, Vector4d> entry : this.entityBoxPositions.entrySet()) {
            Vector4d v = entry.getValue();
            if (v.z <= 0.0 || v.w <= 0.0) continue;
            Color color = this.getEntityColor(entry.getKey());
            float x1 = (float) v.x;
            float y1 = (float) v.y;
            float x2 = x1 + (float) v.z;
            float y2 = y1 + (float) v.w;
            this.drawFilledRect2D(builder, matrix4f, x1, y1, x2, y2, color);
            if (entry.getKey() instanceof net.minecraft.entity.LivingEntity le && this.showHealthBar.getValue()) {
                this.drawHealthBar(builder, matrix4f, le, v, color);
            }
        }
        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.disableBlend();
    }

    private void drawFilledRect2D(BufferBuilder builder, Matrix4f matrix4f, float x1, float y1, float x2, float y2, Color color) {
        RenderUtil.drawQuad(builder, matrix4f, x1 - 1.0F, y1, x1 + 0.5F, y2 + 0.5F, Color.BLACK);
        RenderUtil.drawQuad(builder, matrix4f, x1 - 1.0F, y1 - 0.5F, x2 + 0.5F, y1 + 1.0F, Color.BLACK);
        RenderUtil.drawQuad(builder, matrix4f, x2 - 0.5F, y1, x2 + 0.5F, y2 + 0.5F, Color.BLACK);
        RenderUtil.drawQuad(builder, matrix4f, x1 - 1.0F, y2 - 0.5F, x2 + 0.5F, y2 + 0.5F, Color.BLACK);
        RenderUtil.drawQuad(builder, matrix4f, x1 - 0.5F, y1, x1, y2, color);
        RenderUtil.drawQuad(builder, matrix4f, x1, y2 - 0.5F, x2, y2, color);
        RenderUtil.drawQuad(builder, matrix4f, x1, y1, x2, y1 + 0.5F, color);
        RenderUtil.drawQuad(builder, matrix4f, x2 - 0.5F, y1, x2, y2, color);
    }

    private void drawHealthBar(BufferBuilder builder, Matrix4f matrix4f, net.minecraft.entity.LivingEntity entity, Vector4d v, Color color) {
        float healthFrac;
        if (entity instanceof PlayerEntity p) {
            healthFrac = Math.max(0.0F, Math.min(Math.min(p.getHealth(), p.getMaxHealth()) / p.getMaxHealth(), 1.0F));
        } else {
            healthFrac = Math.min(entity.getHealth() / entity.getMaxHealth(), 1.0F);
        }
        float x = (float) v.x;
        float y = (float) v.y;
        float right = x + (float) v.z;
        float bottom = y + (float) v.w;
        float barX;
        float barY;
        float barW;
        float barH;
        switch (this.healthBarPosition.getValue()) {
            case 1: {
                barW = (float) v.z;
                barH = 2.0F;
                barX = x;
                barY = y - 4.0F;
                break;
            }
            case 2: {
                barW = 2.0F;
                barH = (float) v.w;
                barX = x - 4.0F;
                barY = y;
                break;
            }
            case 3: {
                barW = 2.0F;
                barH = (float) v.w;
                barX = right + 2.0F;
                barY = y;
                break;
            }
            default: {
                barW = (float) v.z;
                barH = 2.0F;
                barX = x;
                barY = bottom + 2.0F;
                break;
            }
        }
        RenderUtil.drawQuad(builder, matrix4f, barX - 0.6F, barY - 0.6F, barX + barW + 0.6F, barY + barH + 0.6F, Color.BLACK);
        RenderUtil.drawQuad(builder, matrix4f, barX, barY, barX + barW, barY + barH, color.darker().darker());
        Color healthColor = this.getHealthColor(healthFrac);
        if (this.healthBarPosition.getValue() == 2 || this.healthBarPosition.getValue() == 3) {
            RenderUtil.drawQuad(builder, matrix4f, barX, barY + barH * (1.0F - healthFrac), barX + barW, barY + barH, healthColor);
        } else {
            RenderUtil.drawQuad(builder, matrix4f, barX, barY, barX + barW * healthFrac, barY + barH, healthColor);
        }
    }

    private Color getHealthColor(float fraction) {
        return Color.getHSBColor(Math.max(0.0F, fraction) / 3.0F, 1.0F, 1.0F);
    }
}