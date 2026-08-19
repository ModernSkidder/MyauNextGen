package laoqi123.module.modules;

import laoqi123.enums.ChatColors;
import laoqi123.event.EventTarget;
import laoqi123.events.Render2DEvent;
import laoqi123.module.Module;
import laoqi123.util.RenderUtil;
import laoqi123.util.RotationUtil;
import laoqi123.util.TeamUtil;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.FloatProperty;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.awt.*;
import java.util.stream.Collectors;

public class Indicators extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final FloatProperty scale = new FloatProperty("scale", 1.0f, 0.5f, 1.5f);
    public final FloatProperty offset = new FloatProperty("offset", 50.0f, 0.0f, 255.0f);
    public final BooleanProperty directionCheck = new BooleanProperty("direction-check", true);
    public final BooleanProperty fireballs = new BooleanProperty("fireballs", true);
    public final BooleanProperty pearls = new BooleanProperty("pearls", true);
    public final BooleanProperty arrows = new BooleanProperty("arrows", true);
    public final BooleanProperty egg = new BooleanProperty("egg", true);
    public final BooleanProperty snowball = new BooleanProperty("snowball", true);

    private boolean shouldRender(Entity entity) {
        double d = (entity.getX() - entity.prevX) * (mc.player.getX() - entity.getX()) + (entity.getY() - entity.prevY) * (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()) - entity.getY() - entity.getHeight() / 2.0) + (entity.getZ() - entity.prevZ) * (mc.player.getZ() - entity.getZ());
        if (d == 0.0) {
            return false;
        }
        if (d < 0.0) {
            if (this.directionCheck.getValue()) {
                return false;
            }
        }
        if (this.fireballs.getValue() && entity instanceof FireballEntity) return true;
        if (this.pearls.getValue() && entity instanceof EnderPearlEntity) return true;
        if (this.arrows.getValue() && entity instanceof ArrowEntity) return true;
        if (this.egg.getValue() && entity instanceof EggEntity) return true;
        if (this.snowball.getValue() && entity instanceof SnowballEntity) return true;
        return false;
    }

    private Item getIndicatorItem(Entity entity) {
        if (entity instanceof FireballEntity) {
            return Items.FIRE_CHARGE;
        }
        if (entity instanceof EnderPearlEntity) {
            return Items.ENDER_PEARL;
        }
        if (entity instanceof ArrowEntity) {
            return Items.ARROW;
        }
        if (entity instanceof EggEntity) {
            return Items.EGG;
        }
        if (entity instanceof SnowballEntity) {
            return Items.SNOWBALL;
        }
        return Items.AIR;
    }

    private Color getIndicatorColor(Entity entity) {
        if (entity instanceof FireballEntity) {
            return new Color(12676363);
        }
        if (entity instanceof EnderPearlEntity) {
            return new Color(2458740);
        }
        if (entity instanceof ArrowEntity) {
            return new Color(0x969696);
        }
        return new Color(-1);
    }

    public Indicators() {
        super("Indicators", false, true);
    }

    @EventTarget
    public void onRender(Render2DEvent render2DEvent) {
        if (!this.isEnabled()) {
            return;
        }
        for (Entity entity : TeamUtil.getLoadedEntitiesSorted().stream().filter(this::shouldRender).collect(Collectors.toList())) {
            float offset = 10.0f + this.offset.getValue();
            float yawBetween = RotationUtil.getYawBetween(RenderUtil.lerpDouble(mc.player.getX(), mc.player.prevX, render2DEvent.getPartialTicks()), RenderUtil.lerpDouble(mc.player.getZ(), mc.player.prevZ, render2DEvent.getPartialTicks()), RenderUtil.lerpDouble(entity.getX(), entity.prevX, render2DEvent.getPartialTicks()), RenderUtil.lerpDouble(entity.getZ(), entity.prevZ, render2DEvent.getPartialTicks()));
            if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
                yawBetween += 180.0f;
            }
            float x = (float) Math.sin(Math.toRadians(yawBetween));
            float z = (float) Math.cos(Math.toRadians(yawBetween)) * -1.0f;
            float scaleValue = this.scale.getValue();
            float centerX = (float) mc.getWindow().getScaledWidth() / 2.0f;
            float centerY = (float) mc.getWindow().getScaledHeight() / 2.0f;
            RenderUtil.renderItemInGUI(new ItemStack(this.getIndicatorItem(entity)), (int) (centerX + (offset + 0.0f) * x * scaleValue - 8.0f * scaleValue), (int) (centerY + (offset + 0.0f) * z * scaleValue - 8.0f * scaleValue));
            String string = String.format("%dm", (int) mc.player.distanceTo(entity));
            int textColor = ChatColors.GRAY.toAwtColor() & 0xFFFFFF | 0xBF000000;
            render2DEvent.getContext().drawText(mc.textRenderer, string, (int) (centerX + (offset + 0.0f) * x * scaleValue - mc.textRenderer.getWidth(string) / 2.0f * scaleValue + 1.0f), (int) (centerY + (offset + 0.0f) * z * scaleValue + 1.0f), textColor, true);
            RenderUtil.enableRenderState();
            RenderUtil.drawArrow(centerX + (offset + 15.0f) * x * scaleValue + 1.0f, centerY + (offset + 15.0f) * z * scaleValue + 1.0f, (float) (Math.atan2(z, x) + Math.PI), 7.5f * scaleValue, 1.5f, this.getIndicatorColor(entity).getRGB());
            RenderUtil.disableRenderState();
        }
    }
}
