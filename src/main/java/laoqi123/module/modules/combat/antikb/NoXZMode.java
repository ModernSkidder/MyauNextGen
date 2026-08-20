package laoqi123.module.modules.combat.antikb;

import java.awt.Color;
import java.util.concurrent.LinkedBlockingDeque;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.impl.LoadWorldEvent;
import laoqi123.event.impl.PacketEvent;
import laoqi123.event.impl.Render2DEvent;
import laoqi123.event.impl.StrafeEvent;
import laoqi123.event.impl.TickEvent;
import laoqi123.event.types.EventType;
import laoqi123.module.Module;
import laoqi123.module.modules.combat.KillAura;
import laoqi123.module.modules.combat.Velocity;
import laoqi123.module.modules.movement.Stuck;
import laoqi123.util.ChatUtil;
import laoqi123.util.PacketUtil;
import laoqi123.util.PlayerUtil;
import laoqi123.util.RenderUtil;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.DamageTiltS2CPacket;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * NoXZ anti-knockback mode (ported from OpenNilore's NoXZMode).
 * <p>
 * Holds the server→client knockback packet (plus other packets) while the
 * player is airborne, then releases them once the player lands so the X/Z
 * portion of the knockback never carries. Optionally bursts attacks on
 * landing ("instant attack") and renders a suspension progress bar.
 */
public class NoXZMode
        extends AntiKBMode {
    public static NoXZMode INSTANCE;
    public static boolean isAttacking;
    public static boolean handlingVelocity;
    public static boolean velocityHandled;
    public static int attackCount;
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private int attackCooldown = 0;
    private Entity attackTarget = null;
    private int attacksRemaining = 0;
    private int flagCooldown = 0;
    private boolean shouldJump = false;
    private int sprintBoostCounter = 0;
    private int hitCounter = 0;
    private boolean isSuspending = false;
    private int delayTicks = 0;
    private EntityVelocityUpdateS2CPacket knockbackPacket = null;
    private final LinkedBlockingDeque<Packet<ClientPlayPacketListener>> packetQueue = new LinkedBlockingDeque();
    private float instantAttackProgress = 0.0f;
    private boolean isInstantAttacking = false;

    @Override
    public boolean isActive() {
        return this.velocityHandled;
    }

    public NoXZMode() {
        INSTANCE = this;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void onEnable() {
        this.resetAll();
    }

    @Override
    public void onDisable() {
        this.resetAll();
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE) {
            return;
        }
        if (mc.player == null || mc.world == null) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerRespawnS2CPacket
                || packet instanceof GameJoinS2CPacket) {
            this.resetAll();
            return;
        }
        if (packet instanceof PlayerPositionLookS2CPacket) {
            if (this.isSuspending) {
                this.release();
            }
            this.resetSuspension();
            if (this.parent.debugLog.getValue()) {
                ChatUtil.sendFormatted("Flag Detected");
            }
            this.flagCooldown = 2;
            return;
        }
        if (this.flagCooldown != 0) {
            return;
        }
        if (this.isSuspending) {
            // Alink 收放包: 自己的移动包与别人的移动包全部暂缓, 落地才释放
            if (packet instanceof EntityS2CPacket
                    || packet instanceof CommonPingS2CPacket
                    || packet instanceof EntityPositionS2CPacket) {
                this.packetQueue.add((Packet<ClientPlayPacketListener>) packet);
                event.setCancelled(true);
            } else if (!this.isAllowedPacket(packet)) {
                this.packetQueue.add((Packet<ClientPlayPacketListener>) packet);
                event.setCancelled(true);
            }
            return;
        }
        if (packet instanceof EntityVelocityUpdateS2CPacket motionPacket) {
            if (motionPacket.getEntityId() != mc.player.getId()) {
                return;
            }
            if (!this.canProcess()) {
                if (this.parent.debugLog.getValue()) {
                    ChatUtil.sendFormatted("Alink Wait");
                }
                this.resetAll();
                return;
            }
            double dx = -motionPacket.getVelocityX();
            double dz = -motionPacket.getVelocityZ();
            if (Math.abs(dx) > 0.01 || Math.abs(dz) > 0.01) {
                this.hitCounter = 1;
            }
            if (motionPacket.getVelocityY() > 0) {
                this.sprintBoostCounter = this.sprintBoostCounter % 100 + 100;
                if (this.sprintBoostCounter >= 100) {
                    this.shouldJump = true;
                }
                Entity target = this.getAttackTarget();
                boolean canAttack = this.isValidTarget(target) && mc.player.isSprinting();
                if (!mc.player.isOnGround()) {
                    this.enterSuspension(motionPacket);
                    event.setCancelled(true);
                } else if (canAttack) {
                    this.attackTarget = target;
                    this.attacksRemaining = this.getAttackCount(motionPacket);
                } else {
                    this.enterSuspension(motionPacket);
                    event.setCancelled(true);
                    if (this.parent.debugLog.getValue()) {
                        ChatUtil.sendFormatted("Alink Wait");
                    }
                }
            }
        }
    }

    @Override
    public void onLoadWorld(LoadWorldEvent event) {
        this.resetAll();
    }

    @Override
    public void onTick(TickEvent tickEvent) {
        if (mc.player == null) {
            return;
        }
        if (this.attackCooldown > 0) {
            --this.attackCooldown;
            if (this.attackCooldown <= 0) {
                isAttacking = false;
                attackCount = 0;
                velocityHandled = false;
            }
        }
        if (this.hitCounter > 0) {
            ++this.hitCounter;
            if (this.hitCounter > 2) {
                this.hitCounter = 0;
            }
        }
        if (mc.player.isDead() || !mc.player.isAlive() || this.shouldIgnore()) {
            this.clearTarget();
            if (this.isSuspending) {
                this.release();
            }
            if (this.isInstantAttacking) {
                this.isInstantAttacking = false;
                this.instantAttackProgress = 0.0f;
                Myau.serverTickRate = 1.0f;
            }
            return;
        }
        if (this.flagCooldown > 0) {
            --this.flagCooldown;
            this.clearTarget();
        }
        if (this.isSuspending) {
            ++this.delayTicks;
            // Alink 超时: 暂缓太久直接放弃, 放行全部暂缓包并重置
            if (this.delayTicks >= this.parent.maxDelayTicks.getValue()) {
                if (this.parent.debugLog.getValue()) {
                    ChatUtil.sendFormatted("Alink Timeout");
                }
                this.resetAll();
                return;
            }
            boolean instantAttackEnabled = this.parent.instantAttack.getValue();
            if (instantAttackEnabled && this.instantAttackProgress < 3.0f) {
                if (this.parent.tickManipulation.getValue()) {
                    // Grim 警告: 0.5x 慢放会触发 Timer/Balance 检查, 默认关闭
                    float tickRate;
                    Myau.serverTickRate = tickRate = 0.5f;
                    this.instantAttackProgress += 1.0f - tickRate;
                } else {
                    this.instantAttackProgress += 1.0f;
                }
                this.instantAttackProgress = Math.min(this.instantAttackProgress, 3.0f);
            }
            if (mc.player.isOnGround()) {
                if (this.parent.debugLog.getValue()) {
                    ChatUtil.sendFormatted("ground");
                }
                if (instantAttackEnabled) {
                    Myau.serverTickRate = 1.0f;
                }
                Entity target = this.getAttackTarget();
                boolean canAttack = this.isValidTarget(target);
                boolean sprinting = mc.player.isSprinting();
                if (canAttack && sprinting) {
                    // 放: 异步放行暂缓的服务器→客户端包(含击退包)
                    this.flushQueue();
                    this.attackTarget = target;
                    this.attacksRemaining = this.getAttackCount(this.knockbackPacket);
                    if (instantAttackEnabled && this.instantAttackProgress > 0.0f) {
                        this.attacksRemaining = (int) this.instantAttackProgress;
                        this.isSuspending = false;
                        handlingVelocity = false;
                        this.delayTicks = 0;
                        this.isInstantAttacking = true;
                        if (this.parent.tickManipulation.getValue()) {
                            // Grim 警告: 4x 爆发会触发 Timer/Balance 检查, 默认关闭
                            Myau.serverTickRate = 4.0f;
                        }
                    } else {
                        this.doAttackSequence(tickEvent);
                        this.isSuspending = false;
                        handlingVelocity = false;
                        this.delayTicks = 0;
                    }
                } else {
                    this.release();
                    if (instantAttackEnabled) {
                        this.instantAttackProgress = 0.0f;
                    }
                    if (mc.player.isSprinting()) {
                        mc.player.setSprinting(false);
                    }
                }
                return;
            }
            return;
        }
        if (this.isInstantAttacking) {
            this.instantAttackProgress -= 1.0f;
            if (this.instantAttackProgress <= 0.0f) {
                this.instantAttackProgress = 0.0f;
                this.isInstantAttacking = false;
                Myau.serverTickRate = 1.0f;
                if (this.parent.debugLog.getValue()) {
                    ChatUtil.sendFormatted("done");
                }
            }
        }
        if (this.attacksRemaining > 0 && this.attackTarget != null) {
            this.doAttackSequence(tickEvent);
        }
    }

    @Override
    public void onStrafe(StrafeEvent strafeEvent) {
        if (mc.player == null) {
            return;
        }
        if (this.hitCounter > 0) {
            strafeEvent.setForward(1.0f);
        }
        if (this.shouldJump) {
            this.shouldJump = false;
            // 不在此强置疾跑: mc.player.setSprinting(true) 会在不满足疾跑条件时
            // 强行打开疾跑标志, 触发 Grim Simulation 检查。疾跑交由 Sprint 模块/
            // 原版 updateSprintingState 维护。
        }
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        if (!this.parent.renderBar.getValue()
                || !this.parent.isEnabled()
                || (!handlingVelocity && !velocityHandled)) {
            return;
        }
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        float barWidth = 100.0f;
        float barHeight = 2.0f;
        float barX = width / 2.0f - barWidth / 2.0f;
        float barY = height / 2.0f + height * 0.10f;

        // 灰黑色背景(整条)
        RenderUtil.drawRect(barX, barY, barX + barWidth, barY + barHeight,
                new Color(30, 30, 36, 180).getRGB());
        // 青蓝色进度
        float progress = Math.min(1.0f,
                (float) this.delayTicks / Math.max(1, this.parent.maxDelayTicks.getValue()));
        if (progress > 0.0f) {
            RenderUtil.drawRect(barX, barY, barX + barWidth * progress, barY + barHeight,
                    new Color(0, 180, 255, 230).getRGB());
        }
    }

    private void enterSuspension(EntityVelocityUpdateS2CPacket packet) {
        this.isSuspending = true;
        handlingVelocity = true;
        velocityHandled = true;
        this.delayTicks = 0;
        this.knockbackPacket = packet;
        this.packetQueue.add(packet);
    }

    private boolean canProcess() {
        if (!this.parent.requireKillAura.getValue()) {
            return true;
        }
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        return killAura != null && killAura.isEnabled();
    }

    private void resetAll() {
        this.flushQueue();
        this.clearTarget();
        this.flagCooldown = 0;
        this.shouldJump = false;
        this.sprintBoostCounter = 0;
        this.hitCounter = 0;
        this.resetSuspension();
    }

    private void clearTarget() {
        this.attackTarget = null;
        this.attacksRemaining = 0;
    }

    private void resetSuspension() {
        this.isSuspending = false;
        handlingVelocity = false;
        velocityHandled = false;
        this.delayTicks = 0;
        this.knockbackPacket = null;
        this.instantAttackProgress = 0.0f;
        this.isInstantAttacking = false;
        Myau.serverTickRate = 1.0f;
    }

    private void release() {
        this.flushQueue();
        this.resetSuspension();
    }

    private boolean shouldIgnore() {
        if (mc.player == null || mc.world == null) {
            return true;
        }
        if (mc.player.isDead() || !mc.player.isAlive() || mc.player.getHealth() <= 0.0f) {
            return true;
        }
        if (mc.player.isSpectator() || mc.player.getAbilities().flying) {
            return true;
        }
        if (mc.player.isInLava() || mc.player.isOnFire() || mc.player.isTouchingWater()
                || mc.player.isClimbing() || mc.player.isSleeping()) {
            return true;
        }
        if (mc.world.getBlockState(mc.player.getBlockPos()).isOf(Blocks.COBWEB)) {
            return true;
        }
        Module stuck = Myau.moduleManager.modules.get(Stuck.class);
        return stuck != null && stuck.isEnabled();
    }

    private int getAttackCount(EntityVelocityUpdateS2CPacket motionPacket) {
        if (!this.parent.autoAttackCount.getValue() || motionPacket == null) {
            return this.parent.attackAmount.getValue();
        }
        double velocity = Math.sqrt((double) motionPacket.getVelocityX() * motionPacket.getVelocityX()
                + (double) motionPacket.getVelocityY() * motionPacket.getVelocityY());
        if (velocity < 1000.0) {
            return 0;
        }
        if (velocity < 2000.0) {
            return 3;
        }
        if (velocity < 10000.0) {
            return 4;
        }
        return 5;
    }

    private double getAABBDistance(Entity entity) {
        if (mc.player == null) {
            return Double.MAX_VALUE;
        }
        Vec3d eyePos = mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        double clampedX = Math.max(box.minX, Math.min(eyePos.x, box.maxX));
        double clampedY = Math.max(box.minY, Math.min(eyePos.y, box.maxY));
        double clampedZ = Math.max(box.minZ, Math.min(eyePos.z, box.maxZ));
        return eyePos.distanceTo(new Vec3d(clampedX, clampedY, clampedZ));
    }

    private Entity getHitResultEntity() {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY
                && ((EntityHitResult) mc.crosshairTarget).getEntity() instanceof LivingEntity hitEntity
                && hitEntity != mc.player && hitEntity.isAlive() && !hitEntity.isSpectator()) {
            return hitEntity;
        }
        return null;
    }

    private Entity getAttackTarget() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura != null && killAura.getTarget() != null) {
            return killAura.getTarget();
        }
        return this.getHitResultEntity();
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof LivingEntity livingEntity
                && (livingEntity.isDead() || livingEntity.getHealth() <= 0.0f)) {
            return false;
        }
        double maxReach = 3.7f;
        return !(this.getAABBDistance(entity) > maxReach);
    }

    private void doAttackSequence(TickEvent tickEvent) {
        if (this.attackTarget == null || !this.attackTarget.isAlive()) {
            this.clearTarget();
            return;
        }
        double maxReach = 3.7f;
        if (this.getAABBDistance(this.attackTarget) > maxReach) {
            this.clearTarget();
            return;
        }
        isAttacking = true;
        attackCount = this.attacksRemaining--;
        this.attackCooldown = 2;
        this.doAttack(this.attackTarget);
        if (this.attacksRemaining <= 0) {
            this.clearTarget();
            if (this.parent.instantAttack.getValue()) {
                if (this.parent.debugLog.getValue()) {
                    ChatUtil.sendFormatted("Attack (" + this.parent.attackAmount.getValue() + ")");
                }
            }
        }
    }

    private boolean doAttack(Entity entity) {
        if (mc.player == null || mc.interactionManager == null) {
            return false;
        }
        if (this.parent.sprintStateCheck.getValue() && !mc.player.isSprinting()) {
            if (this.parent.debugLog.getValue()) {
                ChatUtil.sendFormatted("not sprinting");
            }
            return false;
        }
        boolean wasSprinting = mc.player.isSprinting();
        if (wasSprinting) {
            mc.player.setSprinting(false);
        }
        PlayerUtil.attackEntity(entity);
        if (wasSprinting) {
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x * 0.6, velocity.y, velocity.z * 0.6);
        }
        if (!this.parent.instantAttack.getValue()) {
            if (this.parent.debugLog.getValue()) {
                ChatUtil.sendFormatted("Attack (" + this.attacksRemaining + ")");
            }
        }
        return true;
    }

    private void flushQueue() {
        Packet<ClientPlayPacketListener> packet;
        while ((packet = this.packetQueue.poll()) != null) {
            PacketUtil.receivePacket(packet);
        }
    }

    private boolean isAllowedPacket(Packet<?> packet) {
        return packet instanceof EntityVelocityUpdateS2CPacket
                || packet instanceof HealthUpdateS2CPacket
                || packet instanceof PlayerPositionLookS2CPacket
                || packet instanceof PlayerRespawnS2CPacket
                || packet instanceof GameJoinS2CPacket
                || packet instanceof PlaySoundS2CPacket
                || packet instanceof ChatMessageS2CPacket
                || packet instanceof DeathMessageS2CPacket
                || packet instanceof CloseScreenS2CPacket
                || packet instanceof DamageTiltS2CPacket
                || packet instanceof TitleS2CPacket
                || packet instanceof TeamS2CPacket
                || packet instanceof GameMessageS2CPacket
                || packet instanceof DisconnectS2CPacket
                || (packet instanceof EntityAnimationS2CPacket
                        && ((EntityAnimationS2CPacket) packet).getEntityId() != mc.player.getId());
    }

    static {
        isAttacking = false;
        handlingVelocity = false;
        velocityHandled = false;
        attackCount = 0;
    }
}
