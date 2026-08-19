package laoqi123.module.modules.render;

import laoqi123.Myau;
import laoqi123.enums.ChatColors;
import laoqi123.event.EventTarget;
import laoqi123.events.Render2DEvent;
import laoqi123.events.Render3DEvent;
import laoqi123.module.Module;
import laoqi123.util.RenderUtil;
import laoqi123.util.RotationUtil;
import laoqi123.util.TeamUtil;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.PercentProperty;
import laoqi123.property.properties.ModeProperty;
import laoqi123.property.properties.IntProperty;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.stream.Collectors;

public class Tracers extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final ModeProperty colorMode = new ModeProperty("color", 0, new String[]{"DEFAULT", "TEAMS", "HUD"});
    public final BooleanProperty drawLines = new BooleanProperty("lines", true);
    public final BooleanProperty drawArrows = new BooleanProperty("arrows", false);
    public final PercentProperty opacity = new PercentProperty("opacity", 100);
    public final IntProperty distance = new IntProperty("distance", 512, 0, 512);
    public final BooleanProperty showPlayers = new BooleanProperty("players", true);
    public final BooleanProperty showFriends = new BooleanProperty("friends", true);
    public final BooleanProperty showEnemies = new BooleanProperty("enemies", true);
    public final BooleanProperty showBots = new BooleanProperty("bots", false);

    private boolean shouldRender(PlayerEntity entityPlayer) {
        if (entityPlayer.deathTime > 0) {
            return false;
        } else if (mc.getCameraEntity().distanceTo(entityPlayer) > (float) this.distance.getValue()) {
            return false;
        } else if (entityPlayer != mc.player && entityPlayer != mc.getCameraEntity()) {
            if (TeamUtil.isBot(entityPlayer)) {
                return this.showBots.getValue();
            } else if (TeamUtil.isFriend(entityPlayer)) {
                return this.showFriends.getValue();
            } else {
                return TeamUtil.isTarget(entityPlayer) ? this.showEnemies.getValue() : this.showPlayers.getValue();
            }
        } else {
            return false;
        }
    }

    private Color getEntityColor(PlayerEntity entityPlayer, float alpha) {
        if (TeamUtil.isFriend(entityPlayer)) {
            Color color = Myau.friendManager.getColor();
            return new Color((float) color.getRed() / 255.0F, (float) color.getGreen() / 255.0F, (float) color.getBlue() / 255.0F, alpha);
        } else if (TeamUtil.isTarget(entityPlayer)) {
            Color color = Myau.targetManager.getColor();
            return new Color((float) color.getRed() / 255.0F, (float) color.getGreen() / 255.0F, (float) color.getBlue() / 255.0F, alpha);
        } else {
            switch (this.colorMode.getValue()) {
                case 0:
                    return TeamUtil.getTeamColor(entityPlayer, alpha);
                case 1:
                    int teamColor = TeamUtil.isSameTeam(entityPlayer) ? ChatColors.BLUE.toAwtColor() : ChatColors.RED.toAwtColor();
                    return new Color(teamColor & Color.WHITE.getRGB() | (int) (alpha * 255.0F) << 24, true);
                case 2:
                    int color = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis()).getRGB();
                    return new Color(color & Color.WHITE.getRGB() | (int) (alpha * 255.0F) << 24, true);
                default:
                    return new Color(1.0F, 1.0F, 1.0F, alpha);
            }
        }
    }

    public Tracers() {
        super("Tracers", false);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && this.drawLines.getValue()) {
            RenderUtil.enableRenderState();
            Vec3d position;
            float partialTicks = mc.getRenderTickCounter().getTickDelta(true);
            if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
                position = new Vec3d(0.0, 0.0, 1.0)
                        .rotateX(
                                (float) (
                                        -Math.toRadians(
                                                RenderUtil.lerpFloat(
                                                        mc.getCameraEntity().getPitch(),
                                                        mc.getCameraEntity().prevPitch,
                                                        partialTicks
                                                )
                                        )
                                )
                        )
                        .rotateY(
                                (float) (
                                        -Math.toRadians(
                                                RenderUtil.lerpFloat(
                                                        mc.getCameraEntity().getYaw(),
                                                        mc.getCameraEntity().prevYaw,
                                                        partialTicks
                                                )
                                        )
                                )
                        );
            } else {
                position = new Vec3d(0.0, 0.0, 0.0)
                        .rotateX((float) (-Math.toRadians(mc.gameRenderer.getCamera().getPitch())))
                        .rotateY((float) (-Math.toRadians(mc.gameRenderer.getCamera().getYaw())));
            }
            position = new Vec3d(position.x, position.y + (double) mc.getCameraEntity().getStandingEyeHeight(), position.z);
            for (PlayerEntity player : TeamUtil.getLoadedEntitiesSorted().stream().filter(entity -> entity instanceof PlayerEntity && this.shouldRender((PlayerEntity) entity)).map(PlayerEntity.class::cast).collect(Collectors.toList())) {
                Color color = this.getEntityColor(player, (float) this.opacity.getValue() / 100.0F);
                double x = RenderUtil.lerpDouble(player.getX(), player.prevX, event.getPartialTicks());
                double y = RenderUtil.lerpDouble(player.getY(), player.prevY, event.getPartialTicks()) - (player.isSneaking() ? 0.125 : 0.0);
                double z = RenderUtil.lerpDouble(player.getZ(), player.prevZ, event.getPartialTicks());
                RenderUtil.drawLine3D(
                        position,
                        x,
                        y + (double) player.getStandingEyeHeight(),
                        z,
                        (float) color.getRed() / 255.0F,
                        (float) color.getGreen() / 255.0F,
                        (float) color.getBlue() / 255.0F,
                        (float) color.getAlpha() / 255.0F,
                        1.5F
                );
            }
            RenderUtil.disableRenderState();
        }
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled() && this.drawArrows.getValue()) {
            for (PlayerEntity player : TeamUtil.getLoadedEntitiesSorted().stream().filter(entity -> entity instanceof PlayerEntity && this.shouldRender((PlayerEntity) entity)).map(PlayerEntity.class::cast).collect(Collectors.toList())) {
                float yawBetween = RotationUtil.getYawBetween(
                        RenderUtil.lerpDouble(mc.player.getX(), mc.player.prevX, event.getPartialTicks()),
                        RenderUtil.lerpDouble(mc.player.getZ(), mc.player.prevZ, event.getPartialTicks()),
                        RenderUtil.lerpDouble(player.getX(), player.prevX, event.getPartialTicks()),
                        RenderUtil.lerpDouble(player.getZ(), player.prevZ, event.getPartialTicks())
                );
                if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
                    yawBetween += 180.0F;
                }
                float arrowDirX = (float) Math.sin(Math.toRadians(yawBetween));
                float arrowDirY = (float) Math.cos(Math.toRadians(yawBetween)) * -1.0F;
                float opacity = this.opacity.getValue().floatValue() / 100.0F;
                yawBetween = Math.abs(MathHelper.wrapDegrees(yawBetween));
                if (yawBetween < 30.0F) {
                    opacity = 0.0F;
                } else if (yawBetween < 60.0F) {
                    opacity *= (yawBetween - 30.0F) / 30.0F;
                }
                HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
                float scale = hud.scale.getValue();
                RenderUtil.enableRenderState();
                RenderUtil.drawTriangle(
                        (float) mc.getWindow().getScaledWidth() / 2.0F + scale * (55.0F * arrowDirX + 1.0F),
                        (float) mc.getWindow().getScaledHeight() / 2.0F + scale * (55.0F * arrowDirY + 1.0F),
                        (float) (Math.atan2(arrowDirY, arrowDirX) + Math.PI),
                        10.0F * scale,
                        this.getEntityColor(player, opacity).getRGB()
                );
                RenderUtil.disableRenderState();
            }
        }
    }
}
