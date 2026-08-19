package laoqi123.module.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import laoqi123.Myau;
import laoqi123.enums.ChatColors;
import laoqi123.event.EventTarget;
import laoqi123.event.impl.Render3DEvent;
import laoqi123.module.Module;
import laoqi123.value.properties.*;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.ModeValue;
import laoqi123.util.ColorUtil;
import laoqi123.util.RenderUtil;
import laoqi123.util.TeamUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4fStack;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class NameTags extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final DecimalFormat healthFormatter = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    public final FloatValue scale = new FloatValue("scale", 1.0F, 0.5F, 2.0F);
    public final BooleanValue autoScale = new BooleanValue("auto-scale", true);
    public final PercentValue backgroundOpacity = new PercentValue("background", 25);
    public final BooleanValue shadow = new BooleanValue("shadow", true);
    public final ModeValue distanceMode = new ModeValue("distance", 0, new String[]{"NONE", "DEFAULT", "VAPE"});
    public final ModeValue healthMode = new ModeValue("health", 2, new String[]{"NONE", "HP", "HEARTS", "TAB"});
    public final BooleanValue armor = new BooleanValue("armor", true);
    public final BooleanValue effects = new BooleanValue("effects", true);
    public final BooleanValue players = new BooleanValue("players", true);
    public final BooleanValue friends = new BooleanValue("friends", true);
    public final BooleanValue enemies = new BooleanValue("enemies", true);
    public final BooleanValue bossees = new BooleanValue("bosses", false);
    public final BooleanValue mobs = new BooleanValue("mobs", false);
    public final BooleanValue creepers = new BooleanValue("creepers", false);
    public final BooleanValue endermans = new BooleanValue("endermen", false);
    public final BooleanValue blazes = new BooleanValue("blazes", false);
    public final BooleanValue animals = new BooleanValue("animals", false);
    public final BooleanValue self = new BooleanValue("self", false);
    public final BooleanValue bots = new BooleanValue("bots", false);

    public NameTags() {
        super("NameTags", false);
    }

    public boolean shouldRenderTags(LivingEntity entityLivingBase) {
        if (entityLivingBase.deathTime > 0) {
            return false;
        } else if (mc.getCameraEntity().distanceTo(entityLivingBase) > 512.0F) {
            return false;
        } else if (entityLivingBase instanceof PlayerEntity) {
            if (entityLivingBase != mc.player && entityLivingBase != mc.getCameraEntity()) {
                if (TeamUtil.isBot((PlayerEntity) entityLivingBase)) {
                    return this.bots.getValue();
                } else if (TeamUtil.isFriend((PlayerEntity) entityLivingBase)) {
                    return this.friends.getValue();
                } else {
                    return TeamUtil.isTarget((PlayerEntity) entityLivingBase) ? this.enemies.getValue() : this.players.getValue();
                }
            } else {
                return this.self.getValue() && !mc.options.getPerspective().isFirstPerson();
            }
        } else if (entityLivingBase instanceof EnderDragonEntity || entityLivingBase instanceof WitherEntity) {
            return !entityLivingBase.isInvisible() && this.bossees.getValue();
        } else if (!(entityLivingBase instanceof MobEntity) && !(entityLivingBase instanceof SlimeEntity)) {
            return (entityLivingBase instanceof AnimalEntity
                    || entityLivingBase instanceof BatEntity
                    || entityLivingBase instanceof SquidEntity
                    || entityLivingBase instanceof VillagerEntity) && this.animals.getValue();
        } else if (entityLivingBase instanceof CreeperEntity) {
            return this.creepers.getValue();
        } else if (entityLivingBase instanceof EndermanEntity) {
            return this.endermans.getValue();
        } else {
            return entityLivingBase instanceof BlazeEntity ? this.blazes.getValue() : this.mobs.getValue();
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled()) {
            for (Entity entity : TeamUtil.getLoadedEntitiesSorted()) {
                if (entity instanceof LivingEntity
                        && this.shouldRenderTags((LivingEntity) entity)
                        && RenderUtil.isInViewFrustum(entity.getBoundingBox(), 10.0)) {
                    String teamName = TeamUtil.stripName(entity);
                    String strippedName = Formatting.strip(teamName);
                    if (strippedName != null && !strippedName.isBlank()) {
                        double x = RenderUtil.lerpDouble(entity.getX(), entity.prevX, event.getPartialTicks()) - mc.gameRenderer.getCamera().getPos().x;
                        double y = RenderUtil.lerpDouble(entity.getY(), entity.prevY, event.getPartialTicks()) - mc.gameRenderer.getCamera().getPos().y + entity.getStandingEyeHeight();
                        double z = RenderUtil.lerpDouble(entity.getZ(), entity.prevZ, event.getPartialTicks()) - mc.gameRenderer.getCamera().getPos().z;
                        double distance = mc.getCameraEntity().distanceTo(entity);
                        Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
                        matrixStack.pushMatrix();
                        matrixStack.translate((float) x, (float) (y + (entity.isInSneakingPose() ? 0.225 : 0.4)), (float) z);
                        matrixStack.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
                        float view = mc.options.getPerspective().isFrontView() ? -1.0F : 1.0F;
                        matrixStack.rotate(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch() * view));
                        double scale = Math.pow(Math.min(Math.max(this.autoScale.getValue() ? distance : 0.0, 6.0), 128.0), 0.75) * 0.0075;
                        matrixStack.scale((float) (-scale * this.scale.getValue()), (float) (-scale * this.scale.getValue()), 1.0F);
                        String distanceText = "";
                        switch (this.distanceMode.getValue()) {
                            case 1:
                                distanceText = String.format("&7%dm&r ", (int) distance);
                                break;
                            case 2:
                                distanceText = String.format("&a[&f%d&a]&r ", (int) distance);
                        }
                        float health = ((LivingEntity) entity).getHealth();
                        float absorption = ((LivingEntity) entity).getAbsorptionAmount();
                        float max = ((LivingEntity) entity).getMaxHealth();
                        float percent = Math.min(Math.max((health + absorption) / max, 0.0F), 1.0F);
                        String healText = "";
                        switch (this.healthMode.getValue()) {
                            case 1:
                                healText = String.format(" %d%s", (int) health, absorption > 0.0F ? String.format(" &6%d&r", (int) absorption) : "&r");
                                break;
                            case 2:
                                healText = String.format(
                                        " %s%s",
                                        healthFormatter.format((double) health / 2.0),
                                        absorption > 0.0F ? String.format(" &6%s&r", healthFormatter.format((double) absorption / 2.0)) : "&r"
                                );
                                break;
                            case 3:
                                if (entity instanceof PlayerEntity) {
                                    Scoreboard scoreboard = mc.world.getScoreboard();
                                    if (scoreboard != null) {
                                        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
                                        if (objective != null) {
                                            ReadableScoreboardScore score = scoreboard.getScore((PlayerEntity) entity, objective);
                                            if (score != null) {
                                                healText = String.format(" &e%d&r", score.getScore());
                                            }
                                        }
                                    }
                                }
                        }
                        String color = ChatColors.formatColor(String.format("%s&f%s&r%s", distanceText, teamName, healText));
                        int width = mc.textRenderer.getWidth(color);
                        if (this.backgroundOpacity.getValue() > 0) {
                            Color textColor = !entity.isInSneakingPose() && !entity.isInvisible()
                                    ? new Color(0.0F, 0.0F, 0.0F, (float) this.backgroundOpacity.getValue() / 100.0F)
                                    : new Color(0.33F, 0.0F, 0.33F, (float) this.backgroundOpacity.getValue() / 100.0F);
                            RenderUtil.enableRenderState();
                            RenderUtil.drawRect(
                                    (float) (-width) / 2.0F - 1.0F,
                                    (float) (-mc.textRenderer.fontHeight) - 1.0F,
                                    (float) width / 2.0F + (this.shadow.getValue() ? 1.0F : 0.0F),
                                    this.shadow.getValue() ? 0.0F : -1.0F,
                                    textColor.getRGB()
                            );
                            RenderUtil.disableRenderState();
                        }
                        BufferAllocator allocator = new BufferAllocator(256);
                        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
                        mc.textRenderer.draw(
                                color,
                                (float) (-width) / 2.0F,
                                (float) (-mc.textRenderer.fontHeight),
                                ColorUtil.getHealthBlend(percent).getRGB(),
                                this.shadow.getValue(),
                                RenderSystem.getModelViewMatrix(),
                                immediate,
                                TextRenderer.TextLayerType.SEE_THROUGH,
                                0,
                                0xF000F0
                        );
                        immediate.draw();
                        if (entity instanceof PlayerEntity) {
                            int height = mc.textRenderer.fontHeight + 2;
                            if (this.armor.getValue()) {
                                ArrayList<ItemStack> renderingItems = new ArrayList<>();
                                for (int i = 4; i >= 0; i--) {
                                    ItemStack itemStack;
                                    if (i == 0) {
                                        itemStack = ((PlayerEntity) entity).getInventory().getMainHandStack();
                                    } else {
                                        itemStack = ((PlayerEntity) entity).getInventory().armor.get(i - 1);
                                    }
                                    if (!itemStack.isEmpty()) {
                                        renderingItems.add(itemStack);
                                    }
                                }
                                if (!renderingItems.isEmpty()) {
                                    int offset = renderingItems.size() * -8;
                                    for (int i = 0; i < renderingItems.size(); i++) {
                                        RenderUtil.renderItemInGUI(renderingItems.get(i), offset + i * 16, -height - 16);
                                    }
                                    height += 16;
                                }
                            }
                            if (this.effects.getValue()) {
                                List<StatusEffectInstance> effects = ((PlayerEntity) entity)
                                        .getStatusEffects()
                                        .stream()
                                        .filter(StatusEffectInstance::shouldShowIcon)
                                        .collect(Collectors.toList());
                                if (!effects.isEmpty()) {
                                    matrixStack.pushMatrix();
                                    matrixStack.scale(0.5F, 0.5F, 1.0F);
                                    int offset = effects.size() * -9;
                                    for (int i = 0; i < effects.size(); i++) {
                                        RenderUtil.renderPotionEffect(effects.get(i), offset + i * 18, -(height * 2) - 18);
                                    }
                                    matrixStack.popMatrix();
                                }
                            }
                            if (TeamUtil.isFriend((PlayerEntity) entity)) {
                                RenderUtil.enableRenderState();
                                float x1 = (float) (-width) / 2.0F - 1.0F;
                                float y1 = (float) (-mc.textRenderer.fontHeight) - 1.0F;
                                float x2 = (float) width / 2.0F + 1.0F;
                                float y2 = this.shadow.getValue() ? 0.0F : -1.0F;
                                int friendColor = Myau.friendManager.getColor().getRGB();
                                RenderUtil.drawOutlineRect(x1, y1, x2, y2, 1.5F, 0, friendColor);
                                RenderUtil.disableRenderState();
                            } else if (TeamUtil.isTarget((PlayerEntity) entity)) {
                                RenderUtil.enableRenderState();
                                float x1 = (float) (-width) / 2.0F - 1.0F;
                                float y1 = (float) (-mc.textRenderer.fontHeight) - 1.0F;
                                float x2 = (float) width / 2.0F + 1.0F;
                                float y2 = this.shadow.getValue() ? 0.0F : -1.0F;
                                int targetColor = Myau.targetManager.getColor().getRGB();
                                RenderUtil.drawOutlineRect(x1, y1, x2, y2, 1.5F, 0, targetColor);
                                RenderUtil.disableRenderState();
                            }
                        }
                        matrixStack.popMatrix();
                    }
                }
            }
        }
    }
}
