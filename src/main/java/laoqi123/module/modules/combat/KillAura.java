package laoqi123.module.modules.combat;

import com.google.common.base.CaseFormat;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import laoqi123.Myau;
import laoqi123.enums.BlinkModules;
import laoqi123.event.EventManager;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.event.impl.*;
import laoqi123.management.RotationState;
import laoqi123.mixin.ClientPlayerInteractionManagerAccessor;
import laoqi123.mixin.EntityRenderDispatcherAccessor;
import laoqi123.module.Module;
import laoqi123.module.modules.combat.killaura.KillAuraAutoBlock;
import laoqi123.module.modules.combat.killaura.KillAuraFailSwing;
import laoqi123.module.modules.combat.killaura.KillAuraRotation;
import laoqi123.module.modules.combat.killaura.KillAuraTargetSelect;
import laoqi123.module.modules.misc.BedNuker;
import laoqi123.module.modules.player.AutoBlockIn;
import laoqi123.module.modules.player.AutoHeal;
import laoqi123.module.modules.player.Scaffold;
import laoqi123.module.modules.render.HUD;
import laoqi123.value.Value;
import laoqi123.value.properties.*;
import laoqi123.util.*;
import laoqi123.util.clicking.Clicker;
import laoqi123.util.config.Configurable;
import laoqi123.util.rotation.MovementCorrection;
import laoqi123.util.rotation.Rotation;
import laoqi123.util.rotation.RotationWithVector;
import laoqi123.util.config.PropertyProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.joml.Matrix4fStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.stream.StreamSupport;

public class KillAura extends Module implements PropertyProvider {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final KillAuraAutoBlock autoBlock;
    public final KillAuraTargetSelect targetSelect;
    public final KillAuraFailSwing failSwing;
    private final Configurable rootConfig = new Configurable("KillAura");

    public final FloatValue swingRange;
    public final FloatValue attackRange;
    public final IntValue fov;
    public final IntValue minCPS;
    public final IntValue maxCPS;
    public final IntValue switchDelay;
    public final BooleanValue keepSprint;
    public final Clicker clicker;
    public final KillAuraRotation rotations;
    public final BooleanValue throughWalls;
    public final BooleanValue requirePress;
    public final BooleanValue allowMining;
    public final BooleanValue weaponsOnly;
    public final BooleanValue allowTools;
    public final BooleanValue inventoryCheck;
    public final BooleanValue lowTimerCheck;
    public final BooleanValue botCheck;
    public final BooleanValue players;
    public final BooleanValue bosses;
    public final BooleanValue mobs;
    public final BooleanValue animals;
    public final BooleanValue golems;
    public final BooleanValue silverfish;
    public final BooleanValue teams;
    public final BooleanValue shark;
    public ModeValue showTarget;

    private final TimerUtil timer = new TimerUtil();
    public AttackData target = null;
    private int switchTick = 0;
    private boolean hitRegistered = false;

    public KillAura() {
        super("KillAura", false);
        this.targetSelect = new KillAuraTargetSelect();
        this.autoBlock = new KillAuraAutoBlock(this);
        this.failSwing = new KillAuraFailSwing();
        this.rootConfig.setRunningOverride(this::isEnabled);
        this.rootConfig.addChild(this.targetSelect);
        this.rootConfig.addChild(this.autoBlock);
        this.rootConfig.addChild(this.failSwing);

        this.swingRange = new FloatValue("Swing Range", 3.5F, 3.0F, 6.0F);
        this.attackRange = new FloatValue("Attack Range", 3.0F, 3.0F, 6.0F);
        this.fov = new IntValue("Fov", 360, 30, 360);
        this.minCPS = new IntValue("Min Aps", 14, 1, 20);
        this.maxCPS = new IntValue("Max Aps", 14, 1, 20);
        this.keepSprint = new BooleanValue("KeepSprint", true);
        this.clicker = new Clicker("KillAuraClicker", this.minCPS, this.maxCPS);
        this.rootConfig.addChild(this.clicker);
        this.switchDelay = new IntValue("Switch Delay", 150, 0, 1000);
        this.rotations = new KillAuraRotation();
        this.rootConfig.addChild(this.rotations);
        this.throughWalls = new BooleanValue("Through Walls", true);
        this.requirePress = new BooleanValue("Require Press", false);
        this.allowMining = new BooleanValue("Allow Mining", false);
        this.weaponsOnly = new BooleanValue("Weapons Only", false);
        this.allowTools = new BooleanValue("Allow Tools", false, this.weaponsOnly::getValue);
        this.inventoryCheck = new BooleanValue("Inventory Check", true);
        this.lowTimerCheck = new BooleanValue("Low Timer Check", true);
        this.botCheck = new BooleanValue("Bot Check", true);
        this.players = new BooleanValue("Players", true);
        this.bosses = new BooleanValue("Bosses", false);
        this.mobs = new BooleanValue("Mobs", false);
        this.animals = new BooleanValue("Animals", false);
        this.golems = new BooleanValue("Golems", false);
        this.silverfish = new BooleanValue("Silverfish", false);
        this.teams = new BooleanValue("Teams", true);
        this.shark = new BooleanValue("Shark", false);
        this.showTarget = new ModeValue("Show Target", 0, new String[]{"None", "Default", "Hud", "Scan"});
    }

    public FloatValue scanThickness = new FloatValue("ScanThickness", 0.6F, 0.1F, 2.5F, () -> showTarget.getValue() == 3);

    public long getAttackDelayMS() {
        float remainingTicks = (1.0F - mc.player.getAttackCooldownProgress(0.0F)) / mc.player.getAttackCooldownProgressPerTick();
        return Math.max(0L, Math.round(remainingTicks) * 50L);
    }

    private void sendCriticalPackets() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        boolean horizontalCollision = mc.player.horizontalCollision;
        PacketUtil.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.0E-10, z, false, horizontalCollision));
        PacketUtil.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, horizontalCollision));
    }

    private boolean performAttack(float yaw, float pitch) {
        if (!Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
            if (this.autoBlock.isPlayerBlocking() && this.autoBlock.mode.getValue() != 1) {
                return false;
            } else if (mc.world.getTickManager().getTickRate() < 20.0F && lowTimerCheck.getValue()) {
                return false;
            } else {
                if (RotationUtil.rayTrace(this.target.getBox(), yaw, pitch, this.attackRange.getValue()) == null) {
                    if (this.failSwing.isEnabled()
                            && RotationUtil.distanceToEntity(this.target.getEntity())
                            <= (double) this.attackRange.getValue() + this.failSwing.getCurrentAdditionalRange()) {
                        this.failSwing.recordFailedHit(this.target.getEntity());
                    }
                    return false;
                } else {
                    if (this.shark.getValue() && !mc.player.isOnGround()) {
                        this.sendCriticalPackets();
                    }
                    AttackEvent event = new AttackEvent(this.target.getEntity());
                    EventManager.call(event);
                    ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).callSyncCurrentPlayItem();
                    if (this.keepSprint.getValue()) {
                        PacketUtil.sendPacket(PlayerInteractEntityC2SPacket.attack(this.target.getEntity(), mc.player.isSneaking()));
                        mc.player.swingHand(Hand.MAIN_HAND);
                    } else if (mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
                        PlayerUtil.attackEntity(this.target.getEntity());
                    }
                    this.hitRegistered = true;
                    return true;
                }
            }
        } else {
            return false;
        }
    }

    private boolean canAttack() {
        if (this.inventoryCheck.getValue() && mc.currentScreen != null) {
            return false;
        } else if (!(Boolean) this.weaponsOnly.getValue()
                || ItemUtil.hasRawUnbreakingEnchant()
                || this.allowTools.getValue() && ItemUtil.isHoldingTool()) {
            if (((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getIsHittingBlock()) {
                return false;
            } else if ((ItemUtil.isEating() || ItemUtil.isUsingBow()) && PlayerUtil.isUsingItem()) {
                return false;
            } else {
                AutoHeal autoHeal = (AutoHeal) Myau.moduleManager.getModule(AutoHeal.class);
                if (autoHeal.isEnabled() && autoHeal.isSwitching()) {
                    return false;
                } else {
                    BedNuker bedNuker = (BedNuker) Myau.moduleManager.getModule(BedNuker.class);
                    AutoBlockIn autoBlockIn = (AutoBlockIn) Myau.moduleManager.getModule(AutoBlockIn.class);
                    if (bedNuker.isEnabled() && bedNuker.isReady()) {
                        return false;
                    } else if (Myau.moduleManager.getModule(Scaffold.class).isEnabled()) {
                        return false;
                    } else if (autoBlockIn.isEnabled()) {
                        return false;
                    } else if (this.requirePress.getValue()) {
                        return PlayerUtil.isAttacking();
                    } else {
                        return !this.allowMining.getValue() || mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK || !PlayerUtil.isAttacking();
                    }
                }
            }
        } else {
            return false;
        }
    }

    private boolean canAutoBlock() {
        return this.autoBlock.canAutoBlock();
    }

    public boolean hasValidTarget() {
        return StreamSupport.stream(mc.world
                .getEntities()
                .spliterator(), false)
                .anyMatch(
                        entity -> entity instanceof LivingEntity
                                && this.isValidTarget((LivingEntity) entity)
                                && this.isInBlockRange((LivingEntity) entity)
                );
    }

    private boolean isValidTarget(LivingEntity entityLivingBase) {
        if (!TeamUtil.isEntityLoaded(entityLivingBase)) {
            return false;
        } else if (entityLivingBase != mc.player && entityLivingBase != mc.player.getVehicle()) {
            if (entityLivingBase == mc.getCameraEntity() || entityLivingBase == mc.getCameraEntity().getVehicle()) {
                return false;
            } else if (entityLivingBase.deathTime > 0) {
                return false;
            } else if (RotationUtil.angleToEntity(entityLivingBase) > this.fov.getValue().floatValue()) {
                return false;
            } else if (!this.throughWalls.getValue() && !RotationUtil.hasVisiblePoint(entityLivingBase.getBoundingBox().expand(entityLivingBase.getTargetingMargin(), entityLivingBase.getTargetingMargin(), entityLivingBase.getTargetingMargin()))) {
                return false;
            } else if (entityLivingBase instanceof AbstractClientPlayerEntity) {
                if (!this.players.getValue()) {
                    return false;
                } else if (TeamUtil.isFriend((PlayerEntity) entityLivingBase)) {
                    return false;
                } else {
                    return (!this.teams.getValue() || !Teams.isKillAuraTeam((PlayerEntity) entityLivingBase)) && (!this.botCheck.getValue() || !TeamUtil.isBot((PlayerEntity) entityLivingBase));
                }
            } else if (entityLivingBase instanceof EnderDragonEntity || entityLivingBase instanceof WitherEntity) {
                return this.bosses.getValue();
            } else if (!(entityLivingBase instanceof HostileEntity) && !(entityLivingBase instanceof SlimeEntity)) {
                if (entityLivingBase instanceof AnimalEntity
                        || entityLivingBase instanceof BatEntity
                        || entityLivingBase instanceof SquidEntity
                        || entityLivingBase instanceof VillagerEntity) {
                    return this.animals.getValue();
                } else if (!(entityLivingBase instanceof IronGolemEntity)) {
                    return false;
                } else {
                    return this.golems.getValue() && (!this.teams.getValue() || !TeamUtil.hasTeamColor(entityLivingBase));
                }
            } else if (!(entityLivingBase instanceof SilverfishEntity)) {
                return this.mobs.getValue();
            } else {
                return this.silverfish.getValue() && (!this.teams.getValue() || !TeamUtil.hasTeamColor(entityLivingBase));
            }
        } else {
            return false;
        }
    }

    private boolean isInRange(LivingEntity entityLivingBase) {
        return this.isInBlockRange(entityLivingBase) || this.isInSwingRange(entityLivingBase) || this.isInAttackRange(entityLivingBase);
    }

    private boolean isInBlockRange(LivingEntity entityLivingBase) {
        return RotationUtil.distanceToEntity(entityLivingBase) <= (double) this.autoBlock.range.getValue();
    }

    private boolean isInSwingRange(LivingEntity entityLivingBase) {
        return RotationUtil.distanceToEntity(entityLivingBase) <= (double) this.swingRange.getValue();
    }

    private boolean isBoxInSwingRange(Box axisAlignedBB) {
        return RotationUtil.distanceToBox(axisAlignedBB) <= (double) this.swingRange.getValue();
    }

    private boolean isInAttackRange(LivingEntity entityLivingBase) {
        return RotationUtil.distanceToEntity(entityLivingBase) <= (double) this.attackRange.getValue();
    }

    private boolean isBoxInAttackRange(Box axisAlignedBB) {
        return RotationUtil.distanceToBox(axisAlignedBB) <= (double) this.attackRange.getValue();
    }

    private boolean isPlayerTarget(LivingEntity entityLivingBase) {
        return entityLivingBase instanceof PlayerEntity && TeamUtil.isTarget((PlayerEntity) entityLivingBase);
    }

    public LivingEntity getTarget() {
        return this.target != null ? this.target.getEntity() : null;
    }

    public java.util.List<LivingEntity> getTargets() {
        java.util.List<LivingEntity> result = new ArrayList<>();
        if (this.target != null && TeamUtil.isEntityLoaded(this.target.getEntity())) {
            result.add(this.target.getEntity());
        }
        if (this.targetSelect.mode.getValue() == 1) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof LivingEntity) {
                    LivingEntity e = (LivingEntity) entity;
                    if (isValidTarget(e) && isInRange(e) && !result.contains(e)) {
                        result.add(e);
                    }
                }
            }
        }
        return result;
    }

    public boolean isAttackAllowed() {
        Scaffold scaffold = (Scaffold) Myau.moduleManager.getModule(Scaffold.class);
        if (scaffold.isEnabled()) {
            return false;
        } else if (!this.weaponsOnly.getValue()
                || ItemUtil.hasRawUnbreakingEnchant()
                || this.allowTools.getValue() && ItemUtil.isHoldingTool()) {
            return !this.requirePress.getValue() || KeyBindUtil.isKeyDown(mc.options.attackKey);
        } else {
            return false;
        }
    }

    public boolean shouldAutoBlock() {
        return this.autoBlock.shouldAutoBlock();
    }

    public boolean isBlocking() {
        return this.autoBlock.isBlocking();
    }

    public boolean isPlayerBlocking() {
        return this.autoBlock.isPlayerBlocking();
    }

    @EventTarget(Priority.LOW)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            this.clicker.tick();
            boolean attack = this.target != null && this.canAttack();
            boolean inAttackRange = attack;
            KillAuraAutoBlock.BlockResult blockResult = this.autoBlock.updateBlocking(this, attack);
            attack = blockResult.attack;
            boolean swap = blockResult.swap;
            boolean blocked = blockResult.blocked;
            if (this.target != null) {
                RotationWithVector rotationData = this.rotations.findRotation(this.target.getEntity(), this.swingRange.getValue(), this.throughWalls.getValue());
                boolean active = rotationData != null;
                Rotation next = this.rotations.update(active ? rotationData.getRotation() : null, this.target.getEntity(), active);
                if (next != null) {
                    event.setRotation(next.getYaw(), next.getPitch(), 1);
                    MovementCorrection correction = this.rotations.getMovementCorrection();
                    if (correction != MovementCorrection.OFF) {
                        event.setPervRotation(next.getYaw(), 1);
                    }
                    if (correction == MovementCorrection.CHANGE_LOOK) {
                        Myau.rotationManager.setRotation(next.getYaw(), next.getPitch(), 1, true);
                    }
                }
            } else {
                this.rotations.update(null, null, false);
            }
            if (inAttackRange && this.isBoxInSwingRange(this.target.getBox())) {
                boolean attacked = false;
                if (attack) {
                    attacked = this.clicker.click(() -> this.performAttack(event.getNewYaw(), event.getNewPitch()));
                }
                if (swap) {
                    if (attacked) {
                        this.autoBlock.interactAttack(event.getNewYaw(), event.getNewPitch(), this.target);
                    } else {
                        if (!this.autoBlock.isPostBlock()) this.autoBlock.sendUseItem();
                    }
                }
            }
            if (blocked) {
                Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                Myau.blinkManager.setBlinkState(true, BlinkModules.AUTO_BLOCK);
            }
        }
        if (event.getType() == EventType.POST && this.isEnabled()) {
            this.autoBlock.handlePostTick();
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            switch (event.getType()) {
                case PRE:
                    this.rotations.tick();
                    if (this.target == null
                            || !this.isValidTarget(this.target.getEntity())
                            || !this.isBoxInAttackRange(this.target.getBox())
                            || !this.isBoxInSwingRange(this.target.getBox())
                            || this.timer.hasTimeElapsed(this.switchDelay.getValue().longValue())) {
                        this.timer.reset();
                        ArrayList<LivingEntity> targets = new ArrayList<>();
                        for (Entity entity : mc.world.getEntities()) {
                            if (entity instanceof LivingEntity
                                    && this.isValidTarget((LivingEntity) entity)
                                    && this.isInRange((LivingEntity) entity)) {
                                targets.add((LivingEntity) entity);
                            }
                        }
                        if (targets.isEmpty()) {
                            this.target = null;
                        } else {
                            if (targets.stream().anyMatch(this::isInSwingRange)) {
                                targets.removeIf(entityLivingBase -> !this.isInSwingRange(entityLivingBase));
                            }
                            if (targets.stream().anyMatch(this::isInAttackRange)) {
                                targets.removeIf(entityLivingBase -> !this.isInAttackRange(entityLivingBase));
                            }
                            if (targets.stream().anyMatch(this::isPlayerTarget)) {
                                targets.removeIf(entityLivingBase -> !this.isPlayerTarget(entityLivingBase));
                            }
                            targets.sort(
                                    (entityLivingBase1, entityLivingBase2) -> {
                                        int sortBase = 0;
                                        switch (this.targetSelect.sort.getValue()) {
                                            case 1:
                                                sortBase = Float.compare(TeamUtil.getHealthScore(entityLivingBase1), TeamUtil.getHealthScore(entityLivingBase2));
                                                break;
                                            case 2:
                                                sortBase = Integer.compare(entityLivingBase1.hurtTime, entityLivingBase2.hurtTime);
                                                break;
                                            case 3:
                                                sortBase = Float.compare(
                                                        RotationUtil.angleToEntity(entityLivingBase1),
                                                        RotationUtil.angleToEntity(entityLivingBase2)
                                                );
                                        }
                                        return sortBase != 0
                                                ? sortBase
                                                : Double.compare(RotationUtil.distanceToEntity(entityLivingBase1), RotationUtil.distanceToEntity(entityLivingBase2));
                                    }
                            );
                            if (this.targetSelect.mode.getValue() == 1 && this.hitRegistered) {
                                this.hitRegistered = false;
                                this.switchTick++;
                            }
                            if (this.targetSelect.mode.getValue() == 0 || this.switchTick >= targets.size()) {
                                this.switchTick = 0;
                            }
                            this.target = new AttackData(targets.get(this.switchTick));
                        }
                    }
                    if (this.target != null) {
                        this.target = new AttackData(this.target.getEntity());
                    }
                    break;
                case POST:
                    if (this.isPlayerBlocking() && !mc.player.isBlocking()) {
                        mc.player.setCurrentHand(Hand.MAIN_HAND);
                    }
            }
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && !event.isCancelled()) {
            if (event.getPacket() instanceof PlayerActionC2SPacket) {
                PlayerActionC2SPacket packet = (PlayerActionC2SPacket) event.getPacket();
                if (packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                    this.autoBlock.onReleaseUseItem();
                }
            }
            if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket) {
                this.autoBlock.onSlotChangePacket();
            }
        }
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (this.isEnabled()) {
            if (this.rotations.getMovementCorrection() == MovementCorrection.STRICT
                    && RotationState.isActived()
                    && RotationState.getPriority() == 1.0F
                    && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        this.failSwing.renderFailedHits();
        if (this.isEnabled() && target != null) {
            if (this.showTarget.getValue() != 0
                    && TeamUtil.isEntityLoaded(this.target.getEntity())
                    && this.isAttackAllowed() && this.showTarget.getValue() != 3) {
                Color color = new Color(-1);
                switch (this.showTarget.getValue()) {
                    case 1:
                        if (this.target.getEntity().hurtTime > 0) {
                            color = new Color(16733525);
                        } else {
                            color = new Color(5635925);
                        }
                        break;
                    case 2:
                        color = ((HUD) Myau.moduleManager.getModule(HUD.class)).getColor(System.currentTimeMillis());
                }
                RenderUtil.enableRenderState();
                RenderUtil.drawEntityBox(this.target.getEntity(), color.getRed(), color.getGreen(), color.getBlue());
                RenderUtil.disableRenderState();
            }
            if (this.showTarget.getValue() == 3) {
                renderScan(event);
            }
        }
    }

    public static Vec3d interpolate(Vec3d previousVec, Vec3d currentVec, float progress) {
        return new Vec3d(
                previousVec.x + (currentVec.x - previousVec.x) * progress,
                previousVec.y + (currentVec.y - previousVec.y) * progress,
                previousVec.z + (currentVec.z - previousVec.z) * progress
        );
    }

    private void renderScan(Render3DEvent event) {
        if (target == null) return;

        double renderPosX = ((EntityRenderDispatcherAccessor) mc.getEntityRenderDispatcher()).getCamera().getPos().x;
        double renderPosY = ((EntityRenderDispatcherAccessor) mc.getEntityRenderDispatcher()).getCamera().getPos().y;
        double renderPosZ = ((EntityRenderDispatcherAccessor) mc.getEntityRenderDispatcher()).getCamera().getPos().z;
        Vec3d interpolated = interpolate(
                new Vec3d(target.entity.prevX, target.entity.prevY, target.entity.prevZ),
                target.entity.getPos(),
                event.getPartialTicks()
        );

        double height = target.entity.getHeight();
        long time = System.currentTimeMillis();
        double rawAngle = time / 300.0;
        double offset = (Math.sin(rawAngle) + 1) / 2.0 * height;

        double thicknessScale = 1.0 - Math.abs(Math.sin(rawAngle));
        double minScale = 0.15;
        thicknessScale = minScale + (1.0 - minScale) * thicknessScale;

        double x = interpolated.x - renderPosX;
        double y = interpolated.y + offset - renderPosY;
        double z = interpolated.z - renderPosZ;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableCull();

        float radius = 0.6f;
        double baseThickness = scanThickness.getValue();
        double thickness = baseThickness * thicknessScale;
        double halfThick = thickness / 2.0;
        double bottomY = -halfThick;

        int slices = 60;

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.translate((float) x, (float) y, (float) z);

        Tessellator tessellator = Tessellator.getInstance();
        for (int i = 0; i < slices; i++) {
            double angle1 = Math.toRadians((i / (double) slices) * 360.0);
            double angle2 = Math.toRadians(((i + 1) / (double) slices) * 360.0);

            double x1 = Math.sin(angle1) * radius;
            double z1 = Math.cos(angle1) * radius;
            double x2 = Math.sin(angle2) * radius;
            double z2 = Math.cos(angle2) * radius;

            Color col1 = ((HUD) Myau.moduleManager.getModule(HUD.class)).getColor((int) (i * 360.0 / slices * 10));
            Color col2 = ((HUD) Myau.moduleManager.getModule(HUD.class)).getColor((int) ((i + 1) * 360.0 / slices * 10));
            int r1 = col1.getRed();
            int g1 = col1.getGreen();
            int b1 = col1.getBlue();
            int r2 = col2.getRed();
            int g2 = col2.getGreen();
            int b2 = col2.getBlue();

            int alphaBottom = 12;
            int alphaTop = 178;

            BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            bufferBuilder.vertex((float) x1, (float) bottomY, (float) z1).color(r1, g1, b1, alphaBottom);
            bufferBuilder.vertex((float) x1, (float) halfThick, (float) z1).color(r1, g1, b1, alphaTop);
            bufferBuilder.vertex((float) x2, (float) bottomY, (float) z2).color(r2, g2, b2, alphaBottom);
            bufferBuilder.vertex((float) x2, (float) halfThick, (float) z2).color(r2, g2, b2, alphaTop);
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        }

        modelViewStack.popMatrix();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.autoBlock.isBlocking()) {
            event.setCancelled(true);
        } else {
            if (this.isEnabled() && this.target != null && this.canAttack()) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.autoBlock.isBlocking()) {
            event.setCancelled(true);
        } else {
            if (this.isEnabled() && this.target != null && this.canAttack()) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.autoBlock.isBlocking()) {
            event.setCancelled(true);
        } else {
            if (this.isEnabled() && this.target != null && this.canAttack()) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onCancelUse(CancelUseEvent event) {
        if (this.autoBlock.isBlocking()) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onEnabled() {
        this.target = null;
        this.switchTick = 0;
        this.hitRegistered = false;
        this.clicker.reset();
        this.autoBlock.reset();
        this.failSwing.reset();
        this.rotations.reset();
    }

    @Override
    public void onDisabled() {
        Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
        this.autoBlock.reset();
        this.failSwing.reset();
    }

    @Override
    public void verifyValue(String mode) {
        if (!this.autoBlock.mode.getName().equals(mode) && !this.autoBlock.aps.getName().equals(mode)) {
            if (this.swingRange.getName().equals(mode)) {
                if (this.swingRange.getValue() < this.attackRange.getValue()) {
                    this.attackRange.setValue(this.swingRange.getValue());
                }
            } else if (this.attackRange.getName().equals(mode)) {
                if (this.swingRange.getValue() < this.attackRange.getValue()) {
                    this.swingRange.setValue(this.attackRange.getValue());
                }
            } else if (this.minCPS.getName().equals(mode)) {
                if (this.minCPS.getValue() > this.maxCPS.getValue()) {
                    this.maxCPS.setValue(this.minCPS.getValue());
                }
            } else {
                if (this.maxCPS.getName().equals(mode) && this.minCPS.getValue() > this.maxCPS.getValue()) {
                    this.minCPS.setValue(this.maxCPS.getValue());
                }
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.targetSelect.mode.getModeString())};
    }

    public KillAuraAutoBlock getAutoBlock() {
        return this.autoBlock;
    }

    public KillAuraFailSwing getFailSwing() {
        return this.failSwing;
    }

    @Override
    public java.util.List<Value<?>> getAdditionalProperties() {
        return this.rootConfig.collectProperties();
    }

    public static class AttackData {
        private final LivingEntity entity;
        private final Box box;
        private final double x;
        private final double y;
        private final double z;

        public AttackData(LivingEntity entityLivingBase) {
            this.entity = entityLivingBase;
            double collisionBorderSize = entityLivingBase.getTargetingMargin();
            this.box = entityLivingBase.getBoundingBox().expand(collisionBorderSize, collisionBorderSize, collisionBorderSize);
            this.x = entityLivingBase.getX();
            this.y = entityLivingBase.getY();
            this.z = entityLivingBase.getZ();
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public Box getBox() {
            return this.box;
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }

        public double getZ() {
            return this.z;
        }
    }
}
