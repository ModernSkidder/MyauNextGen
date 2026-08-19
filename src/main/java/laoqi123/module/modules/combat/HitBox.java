package laoqi123.module.modules.combat;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.event.impl.LeftClickMouseEvent;
import laoqi123.event.impl.Render3DEvent;
import laoqi123.event.impl.TickEvent;
import laoqi123.module.Module;
import laoqi123.value.properties.*;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.ModeValue;
import laoqi123.util.RenderUtil;
import laoqi123.util.TeamUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class HitBox extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private Entity targetEntity = null;
    private Vec3d targetHitVec = null;
    public final FloatValue multiplier = new FloatValue("multiplier", 1.2F, 1.0F, 5.0F);
    public final ModeValue showHitbox = new ModeValue("show-hitbox", 0, new String[]{"NONE", "PLAYERS", "MOBS", "ANIMALS", "ALL"});
    public final ColorValue color = new ColorValue("color", new Color(255, 255, 255).getRGB(), () -> this.showHitbox.getValue() != 0);
    public final BooleanValue teams = new BooleanValue("teams", true, () -> this.showHitbox.getValue() == 1 || this.showHitbox.getValue() == 4);
    public final BooleanValue botCheck = new BooleanValue("bot-check", true, () -> this.showHitbox.getValue() == 1 || this.showHitbox.getValue() == 4);

    public HitBox() {
        super("HitBox", false);
    }

    public static float getExpansion(Entity entity) {
        HitBox hitBox = (HitBox) Myau.moduleManager.modules.get(HitBox.class);
        if (hitBox != null && hitBox.isEnabled() && entity instanceof LivingEntity) {
            return hitBox.multiplier.getValue();
        }
        return 1.0F;
    }

    private void calculateMouseOver(float partialTicks) {
        if (mc.getCameraEntity() != null && mc.world != null) {
            mc.targetedEntity = null;
            Entity pointedEntity = null;
            double reach = 3.0;
            HitResult blockHit = mc.getCameraEntity().raycast(reach, partialTicks, false);
            double distance = reach;
            Vec3d eyePos = mc.getCameraEntity().getCameraPosVec(partialTicks);
            if (blockHit.getType() != HitResult.Type.MISS) {
                distance = blockHit.getPos().distanceTo(eyePos);
            }
            Vec3d lookVec = mc.getCameraEntity().getRotationVec(partialTicks);
            Vec3d reachVec = eyePos.add(lookVec.x * reach, lookVec.y * reach, lookVec.z * reach);
            Vec3d hitVec = null;
            float expansion = 1.0F;
            List<Entity> entities = mc.world.getOtherEntities(
                    mc.getCameraEntity(),
                    mc.getCameraEntity()
                            .getBoundingBox()
                            .offset(lookVec.x * reach, lookVec.y * reach, lookVec.z * reach)
                            .expand(expansion, expansion, expansion),
                    e -> true
            );
            double closestDistance = distance;
            for (Entity entity : entities) {
                if (entity.canHit()) {
                    float collisionSize = (float) ((double) 0.1F * getExpansion(entity));
                    Box expandedBox = entity.getBoundingBox().expand(collisionSize, collisionSize, collisionSize);
                    Optional<Vec3d> intercept = expandedBox.raycast(eyePos, reachVec);
                    if (expandedBox.contains(eyePos)) {
                        if (0.0 < closestDistance || closestDistance == 0.0) {
                            pointedEntity = entity;
                            hitVec = intercept.orElse(eyePos);
                            closestDistance = 0.0;
                        }
                    } else if (intercept.isPresent()) {
                        double interceptDistance = eyePos.distanceTo(intercept.get());
                        if (interceptDistance < closestDistance || closestDistance == 0.0) {
                            if (entity == mc.getCameraEntity().getVehicle() && !entity.canHit()) {
                                if (closestDistance == 0.0) {
                                    pointedEntity = entity;
                                    hitVec = intercept.get();
                                }
                            } else {
                                pointedEntity = entity;
                                hitVec = intercept.get();
                                closestDistance = interceptDistance;
                            }
                        }
                    }
                }
            }
            if (pointedEntity != null && (closestDistance < distance || blockHit.getType() == HitResult.Type.MISS)) {
                this.targetEntity = pointedEntity;
                this.targetHitVec = hitVec;
                if (pointedEntity instanceof LivingEntity || pointedEntity instanceof ItemFrameEntity) {
                    mc.targetedEntity = pointedEntity;
                }
            }
        }
    }

    private boolean shouldShowEntity(LivingEntity entity) {
        if (entity == mc.player) {
            return false;
        }
        if (entity.deathTime > 0 || entity instanceof ArmorStandEntity || entity.isInvisible()) {
            return false;
        }
        if (mc.getCameraEntity().distanceTo(entity) > 128.0F) {
            return false;
        }
        if (!RenderUtil.isInViewFrustum(entity.getBoundingBox(), 0.1F)) {
            return false;
        }
        switch (this.showHitbox.getValue()) {
            case 0:
                return false;
            case 1:
                if (entity instanceof PlayerEntity) {
                    PlayerEntity player = (PlayerEntity) entity;
                    if (TeamUtil.isFriend(player)) {
                        return false;
                    }
                    if (this.teams.getValue() && TeamUtil.isSameTeam(player)) {
                        return false;
                    }
                    if (this.botCheck.getValue() && TeamUtil.isBot(player)) {
                        return false;
                    }
                    return true;
                }
                return false;
            case 2:
                if (entity instanceof EnderDragonEntity || entity instanceof WitherEntity) {
                    return true;
                }
                if (entity instanceof HostileEntity || entity instanceof SlimeEntity) {
                    return !(entity instanceof SilverfishEntity);
                }
                return false;
            case 3:
                return entity instanceof AnimalEntity
                        || entity instanceof BatEntity
                        || entity instanceof SquidEntity
                        || entity instanceof VillagerEntity
                        || entity instanceof IronGolemEntity;
            case 4:
                if (entity instanceof PlayerEntity) {
                    PlayerEntity player = (PlayerEntity) entity;
                    if (TeamUtil.isFriend(player)) {
                        return false;
                    }
                    if (this.teams.getValue() && TeamUtil.isSameTeam(player)) {
                        return false;
                    }
                    if (this.botCheck.getValue() && TeamUtil.isBot(player)) {
                        return false;
                    }
                }
                return true;
            default:
                return false;
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            this.calculateMouseOver(1.0F);
        }
    }

    @EventTarget(Priority.HIGH)
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled() && !event.isCancelled() && this.targetEntity != null) {
            mc.crosshairTarget = new EntityHitResult(this.targetEntity, this.targetHitVec);
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled() && this.showHitbox.getValue() != 0) {
            List<LivingEntity> entities = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                    .filter(entity -> entity instanceof LivingEntity)
                    .map(entity -> (LivingEntity) entity)
                    .filter(this::shouldShowEntity)
                    .collect(Collectors.toList());
            if (!entities.isEmpty()) {
                RenderUtil.enableRenderState();
                Color renderColor = new Color(this.color.getValue());
                for (LivingEntity entity : entities) {
                    float collisionSize = (float) ((double) 0.1F * this.multiplier.getValue());
                    Box expandedBox = entity.getBoundingBox().expand(collisionSize, collisionSize, collisionSize);
                    double lerpX = RenderUtil.lerpDouble(entity.getX(), entity.prevX, event.getPartialTicks());
                    double lerpY = RenderUtil.lerpDouble(entity.getY(), entity.prevY, event.getPartialTicks());
                    double lerpZ = RenderUtil.lerpDouble(entity.getZ(), entity.prevZ, event.getPartialTicks());
                    Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
                    Box offsetBox = new Box(
                            expandedBox.minX - entity.getX() + (lerpX - cameraPos.x),
                            expandedBox.minY - entity.getY() + (lerpY - cameraPos.y),
                            expandedBox.minZ - entity.getZ() + (lerpZ - cameraPos.z),
                            expandedBox.maxX - entity.getX() + (lerpX - cameraPos.x),
                            expandedBox.maxY - entity.getY() + (lerpY - cameraPos.y),
                            expandedBox.maxZ - entity.getZ() + (lerpZ - cameraPos.z)
                    );
                    RenderUtil.drawBoundingBox(offsetBox, renderColor.getRed(), renderColor.getGreen(), renderColor.getBlue(), 150, 1.5F);
                }
                RenderUtil.disableRenderState();
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.format("%.1fx", this.multiplier.getValue())};
    }
}
