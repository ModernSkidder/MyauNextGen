package laoqi123.module.modules.combat;

import laoqi123.Myau;
import laoqi123.event.EventManager;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.event.impl.*;
import laoqi123.mixin.EntityAccessor;
import laoqi123.module.Module;
import laoqi123.module.modules.movement.LongJump;
import laoqi123.module.modules.player.Scaffold;
import laoqi123.module.modules.player.Timer;
import laoqi123.value.properties.*;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.IntValue;
import laoqi123.util.ChatUtil;
import laoqi123.util.KeyBindUtil;
import laoqi123.util.MoveUtil;
import laoqi123.util.PacketUtil;
import laoqi123.util.RayCastUtil;
import laoqi123.util.RotationUtil;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.DamageTiltS2CPacket;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Velocity extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private int chanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean jumpFlag = false;

    private int rotatoTickCounter = 0;
    private double knockbackX = 0;
    private double knockbackZ = 0;
    private float[] targetRotation = null;
    private int reduceTick = -1;
    private boolean pressed = false;
    private boolean hasReceivedVelocity = false;
    private int ticksSinceVelocity = -1;
    public static boolean extraAttacked = false;
    public static boolean velocityAttacked = false;

    private boolean ShouldJump = false;

    private int slapReduceTicks = 0;
    private int slapAnInt = 0;
    private boolean slot = false;
    private boolean attack = false;
    private boolean swing = false;
    private boolean block = false;
    private boolean inventory = false;
    private boolean dig = false;

    private int attackCooldown = 0;
    private int attackCount = 0;
    private Entity attackTarget = null;
    private int attacksRemaining = 0;
    private int flagCooldown = 0;
    private boolean shouldJump = false;
    private int sprintBoostCounter = 0;
    private int hitCounter = 0;
    private boolean isSuspending = false;
    private int suspendTicks = 0;
    private boolean pendingRelease = false;
    private boolean grimSuspending = false;
    private int grimSuspendTicks = 0;
    private boolean grimKnockback = false;
    private boolean polarKb = false;
    private double polarSb = 0;
    private boolean delayActive = false;
    private int delayChanceCounter = 0;
    private int delayTickCount = 0;
    private EntityVelocityUpdateS2CPacket knockbackPacket = null;
    private final Deque<Packet<?>> packetQueue = new ConcurrentLinkedDeque<>();
    private final Deque<PlayerMoveC2SPacket> movePacketQueue = new ConcurrentLinkedDeque<>();
    private boolean isFlushing = false;
    private float instantAttackProgress = 0.0F;
    private boolean isInstantAttacking = false;
    private boolean shouldFlushMotion = false;

    public final ModeValue mode = new ModeValue("mode", 0, new String[]{"Vanilla", "Jump", "Hypixel", "Slap_Attack", "NoXZ", "GrimReduce", "Polar", "Delay"});
    public final ModeValue polarMode = new ModeValue("Polar Mode", 0, new String[]{"Reduce", "Cancel10%"}, () -> mode.getValue() == 6);

    public final IntValue delayTicks = new IntValue("delay-ticks", 3, 1, 20, () -> mode.getValue() == 7);
    public final PercentValue delayChance = new PercentValue("delay-chance", 100, () -> mode.getValue() == 7);

    public final IntValue attackAmount = new IntValue("Attack amount", 5, 1, 20, () -> mode.getValue() == 4);
    public final BooleanValue instantAttack = new BooleanValue("Instant Attack", false, () -> mode.getValue() == 4);
    public final BooleanValue sprintStateCheck = new BooleanValue("Sprint state check", true, () -> mode.getValue() == 4);

    public final PercentValue chance = new PercentValue("chance", 100, () -> mode.getValue() <= 1 || mode.getValue() == 7);
    public final PercentValue horizontal = new PercentValue("horizontal", 0, () -> mode.getValue() <= 1 || mode.getValue() == 7);
    public final PercentValue vertical = new PercentValue("vertical", 100, () -> mode.getValue() <= 1 || mode.getValue() == 7);
    public final PercentValue explosionHorizontal = new PercentValue("explosions-horizontal", 100, () -> mode.getValue() <= 1 || mode.getValue() == 7);
    public final PercentValue explosionVertical = new PercentValue("explosions-vertical", 100, () -> mode.getValue() <= 1 || mode.getValue() == 7);

    public final BooleanValue reduce = new BooleanValue("reduce", true, () -> mode.getValue() == 2);
    public final IntValue attackTimes = new IntValue("attack-times", 1, 1, 5, () -> mode.getValue() == 2 && reduce.getValue());
    private final BooleanValue onlySprinting = new BooleanValue("only-sprinting", true, () -> mode.getValue() == 2 && reduce.getValue());
    private final BooleanValue reduceWhenCanAttack = new BooleanValue("reduce-when-can-attack", true, () -> mode.getValue() == 2 && reduce.getValue());
    public final BooleanValue hypixelJump = new BooleanValue("jump", true, () -> mode.getValue() == 2);
    public final BooleanValue rotate = new BooleanValue("rotate", false, () -> mode.getValue() == 2);
    public final IntValue rotateTick = new IntValue("rotate-ticks", 3, 1, 12, () -> mode.getValue() == 2 && rotate.getValue());

    public final BooleanValue slapReduce = new BooleanValue("reduce", true, () -> mode.getValue() == 3);
    public final BooleanValue tickExactEnable = new BooleanValue("tickExact", true, () -> mode.getValue() == 3);
    public final IntValue tick500 = new IntValue("500", 3, 0, 20, () -> mode.getValue() == 3);
    public final IntValue tick1000 = new IntValue("1000", 4, 0, 20, () -> mode.getValue() == 3);
    public final IntValue tick2000 = new IntValue("2000", 4, 0, 20, () -> mode.getValue() == 3);
    public final IntValue tick3000 = new IntValue("3000", 5, 0, 20, () -> mode.getValue() == 3);
    public final IntValue tick4000 = new IntValue("4000", 6, 0, 20, () -> mode.getValue() == 3);
    public final IntValue tick5000 = new IntValue("5000", 6, 0, 20, () -> mode.getValue() == 3);
    public final IntValue tick6000 = new IntValue("6000", 7, 0, 20, () -> mode.getValue() == 3);
    public final IntValue tick7000 = new IntValue("7000", 7, 0, 20, () -> mode.getValue() == 3);
    public final IntValue tick8000 = new IntValue("8000", 8, 0, 20, () -> mode.getValue() == 3);
    public final IntValue tick9000 = new IntValue("9000", 8, 0, 20, () -> mode.getValue() == 3);
    public final IntValue tick10000 = new IntValue("10000", 9, 0, 20, () -> mode.getValue() == 3);

    public final BooleanValue fakeCheck = new BooleanValue("fake-check", true);
    public final BooleanValue debugLog = new BooleanValue("debug-log", false);
    public final BooleanValue timer = new BooleanValue("Timer", false);

    public final IntValue maxAirTicks = new IntValue("Max Air Ticks", 12, 4, 20, () -> mode.getValue() == 5);
    public final IntValue reach = new IntValue("Reach", 3, 2, 4, () -> mode.getValue() == 5);

    private long lastTimerAttackTime = -1L;
    private long timerEnableAt = -1L;
    private boolean timerEnabledByVelocity = false;
    private boolean timerJumpPending = false;
    private float savedTimerSpeed = 1.0F;

    public Velocity() {
        super("Velocity", false);
    }

    private boolean isInLiquidOrWeb() {
        return mc.player.isTouchingWater() || mc.player.isInLava() || ((EntityAccessor) mc.player).getIsInWeb();
    }

    private boolean canDelay() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        return mc.player.isOnGround() && (!killAura.isEnabled() || !killAura.shouldAutoBlock());
    }

    private void setJumpInput(boolean jump) {
        PlayerInput input = mc.player.input.playerInput;
        mc.player.input.playerInput = new PlayerInput(
                input.forward(), input.backward(), input.left(), input.right(), jump, input.sneak(), input.sprint()
        );
    }

    private int getKeyCode(KeyBinding keyBinding) {
        InputUtil.Key key = InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey());
        int code = key.getCode();
        return key.getCategory() == InputUtil.Type.MOUSE ? code - 100 : code;
    }

    private void performAttack(LivingEntity target) {
        EventManager.call(new AttackEvent(target));
        mc.player.swingHand(Hand.MAIN_HAND);
        PacketUtil.sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        mc.player.setVelocity(mc.player.getVelocity().x * 0.6, mc.player.getVelocity().y, mc.player.getVelocity().z * 0.6);
        mc.player.setSprinting(false);
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            this.pendingExplosion = false;
            this.allowNext = true;
            return;
        }

        if (this.mode.getValue() == 6 && this.polarMode.getValue() == 0 && this.polarKb && !this.isInLiquidOrWeb()) {
            event.setX(event.getX() * 0.5);
            event.setZ(event.getZ() * 0.5);
        }

        if (!this.allowNext || !this.fakeCheck.getValue()) {
            this.allowNext = true;
            if (this.pendingExplosion) {
                if (this.mode.getValue() <= 1 || this.mode.getValue() == 7) {
                    this.pendingExplosion = false;
                    if (this.explosionHorizontal.getValue() > 0) {
                        event.setX(event.getX() * (double) this.explosionHorizontal.getValue() / 100.0);
                        event.setZ(event.getZ() * (double) this.explosionHorizontal.getValue() / 100.0);
                    } else {
                        event.setX(mc.player.getVelocity().x);
                        event.setZ(mc.player.getVelocity().z);
                    }
                    if (this.explosionVertical.getValue() > 0) {
                        event.setY(event.getY() * (double) this.explosionVertical.getValue() / 100.0);
                    } else {
                        event.setY(mc.player.getVelocity().y);
                    }
                }
            } else {
                if (this.mode.getValue() <= 1 || this.mode.getValue() == 7) {
                    this.chanceCounter = (this.chanceCounter % 100) + this.chance.getValue();
                    if (this.chanceCounter >= 100) {
                        if (this.mode.getValue() == 1 || this.mode.getValue() == 7) {
                            this.jumpFlag = event.getY() > 0.0;
                        }

                        if (this.horizontal.getValue() > 0) {
                            event.setX(event.getX() * (double) this.horizontal.getValue() / 100.0);
                            event.setZ(event.getZ() * (double) this.horizontal.getValue() / 100.0);
                        } else {
                            event.setX(mc.player.getVelocity().x);
                            event.setZ(mc.player.getVelocity().z);
                        }
                        if (this.vertical.getValue() > 0) {
                            event.setY(event.getY() * (double) this.vertical.getValue() / 100.0);
                        } else {
                            event.setY(mc.player.getVelocity().y);
                        }
                    }
                } else if (this.mode.getValue() == 2) {
                    if (this.rotate.getValue() && event.getY() > 0.0) {
                        this.knockbackX = event.getX();
                        this.knockbackZ = event.getZ();
                        if (Math.abs(this.knockbackX) > 0.01 || Math.abs(this.knockbackZ) > 0.01) {
                            this.rotatoTickCounter = 1;
                        }
                    }
                    this.ticksSinceVelocity = 0;
                    this.hasReceivedVelocity = true;
                }
            }
        }
    }

    @EventTarget
    public void onTickDelay(TickEvent event) {
        if (!this.isEnabled() || this.mode.getValue() != 7) return;
        if (event.getType() != EventType.PRE) return;
        if (mc.player == null || mc.world == null) return;

        if (!this.delayActive) return;

        this.delayTickCount++;
        if (this.canDelay() || this.isInLiquidOrWeb() || this.delayTickCount >= this.delayTicks.getValue()) {
            this.releaseDelay();
        }
    }

    private void releaseDelay() {
        this.applyKnockbackPacketNoXZ();
        this.delayActive = false;
        this.delayTickCount = 0;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) return;

        if (this.ticksSinceVelocity >= 0) {
            this.ticksSinceVelocity++;
        }
        if (this.ticksSinceVelocity >= 10) {
            this.ticksSinceVelocity = -1;
            this.ShouldJump = false;
        }
        this.handleJumpReset();
    }

    private void handleJumpReset() {
        if (!this.ShouldJump) return;

        Scaffold scaffold = (Scaffold) Myau.moduleManager.getModule(Scaffold.class);
        if (mc.player == null || mc.currentScreen instanceof InventoryScreen || scaffold.isEnabled()) return;
        if (this.ticksSinceVelocity >= 0) {
            if (this.ticksSinceVelocity <= 2 && mc.player.isOnGround()) {
                KeyBindUtil.setKeyBindState(this.getKeyCode(mc.options.jumpKey), true);
            }
        }
        if (this.ticksSinceVelocity >= 4 && this.ticksSinceVelocity <= 9) {
            KeyBindUtil.setKeyBindState(this.getKeyCode(mc.options.jumpKey), this.pressed);
        }
    }

    @EventTarget
    public void onTimerTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) return;

        if (!this.timer.getValue()) {
            this.disableVelocityTimer();
            return;
        }
        if (mc.player == null || mc.world == null) return;

        Timer timerModule = (Timer) Myau.moduleManager.getModule(Timer.class);
        if (timerModule == null) {
            this.timerEnabledByVelocity = false;
            this.timerEnableAt = -1L;
            return;
        }

        long now = System.currentTimeMillis();
        if (!this.timerEnabledByVelocity) {
            if (this.timerEnableAt != -1L && now >= this.timerEnableAt) {
                this.savedTimerSpeed = timerModule.speed.getValue();
                timerModule.speed.setValue(0.2F);
                timerModule.setEnabled(true);
                this.timerEnabledByVelocity = true;
            }
        } else if (this.lastTimerAttackTime == -1L || now - this.lastTimerAttackTime >= 1500L) {
            timerModule.speed.setValue(this.savedTimerSpeed);
            timerModule.setEnabled(false);
            this.timerEnabledByVelocity = false;
            this.timerEnableAt = -1L;
        }
    }

    private void disableVelocityTimer() {
        if (this.timerEnabledByVelocity) {
            Timer timerModule = (Timer) Myau.moduleManager.getModule(Timer.class);
            if (timerModule != null) {
                timerModule.speed.setValue(this.savedTimerSpeed);
                if (timerModule.isEnabled()) {
                    timerModule.setEnabled(false);
                }
            }
            this.timerEnabledByVelocity = false;
        }
        this.lastTimerAttackTime = -1L;
        this.timerEnableAt = -1L;
        this.timerJumpPending = false;
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        boolean isMode1 = this.mode.getValue() == 1;
        boolean isMode2Jump = this.mode.getValue() == 2 && this.hypixelJump.getValue();

        if (this.isEnabled() && this.timer.getValue() && this.timerJumpPending) {
            this.timerJumpPending = false;
            this.setJumpInput(true);
        }

        if (this.isEnabled() && this.mode.getValue() == 4 && this.shouldJump) {
            this.shouldJump = false;
            if (mc.player.isOnGround() && mc.player.isSprinting()
                    && !mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) && !this.isInLiquidOrWeb()) {
                this.setJumpInput(true);
            }
        }

        if (this.isEnabled() && this.jumpFlag && (isMode1 || isMode2Jump || this.mode.getValue() == 7)) {
            this.jumpFlag = false;
            if (mc.player.isOnGround() && mc.player.isSprinting() && !mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) && !this.isInLiquidOrWeb()) {
                this.setJumpInput(true);
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) return;

        if (this.mode.getValue() == 2) {
            if (event.getType() == EventType.PRE) {
                if (this.reduce.getValue()) {
                    if (this.velocityAttacked) {
                        KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
                        if (killAura.getTarget() != null && killAura.isEnabled()) {
                            this.performAttack(killAura.getTarget());
                        }
                        velocityAttacked = false;
                    }

                    if (this.hasReceivedVelocity) {
                        if (this.reduceTick >= this.attackTimes.getValue()) {
                            this.reduceTick = 0;
                            this.hasReceivedVelocity = false;
                        }
                        KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
                        if (killAura.getTarget() != null) {
                            if (mc.player.isSprinting() || !this.onlySprinting.getValue()) {
                                if (!this.reduceWhenCanAttack.getValue()
                                        || (killAura.getAutoBlock().getBlockTick() == 0 && killAura.getAutoBlock().mode.getValue() == 4)
                                        || (killAura.getAutoBlock().mode.getValue() == 3 && killAura.getAutoBlock().getBlockTick() == 0)) {
                                    this.performAttack(killAura.getTarget());
                                }
                            }
                        }
                        this.reduceTick++;
                    }
                }

                int maxTick = this.rotateTick.getValue();
                if (this.rotatoTickCounter > 0 && this.rotatoTickCounter <= maxTick) {
                    if (this.rotatoTickCounter == 1) {
                        double deltaX = -this.knockbackX;
                        double deltaZ = -this.knockbackZ;
                        this.targetRotation = RotationUtil.getRotationsTo(deltaX, 0, deltaZ, event.getYaw(), event.getPitch());
                    }
                    if (this.targetRotation != null) {
                        event.setRotation(this.targetRotation[0], this.targetRotation[1], 2);
                        event.setPervRotation(this.targetRotation[0], 2);
                    }
                }
            } else if (event.getType() == EventType.POST) {
                int maxTick = this.rotateTick.getValue();
                if (this.rotatoTickCounter > 0 && this.rotatoTickCounter <= maxTick) {
                    this.rotatoTickCounter++;
                    if (this.rotatoTickCounter > maxTick) {
                        this.rotatoTickCounter = 0;
                        this.targetRotation = null;
                        this.knockbackX = 0;
                        this.knockbackZ = 0;
                    }
                }
            }
        }

        if (this.mode.getValue() == 3 && this.slapReduce.getValue() && event.getType() == EventType.PRE) {
            if (this.slapReduceTicks > 0) {
                this.slapReduceTicks--;
                KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
                if (killAura != null && killAura.isEnabled() && killAura.getTarget() != null) {
                    LivingEntity target = killAura.getTarget();
                    if (!((EntityAccessor) mc.player).getIsInWeb() && mc.player.isSprinting() && MoveUtil.isMoving() && target != mc.player && !this.badPackets()) {
                        this.performAttack(target);
                        this.slapAnInt++;
                        if (this.debugLog.getValue()) {
                            ChatUtil.sendFormatted(Myau.clientName + "Attack reduce " + this.slapAnInt);
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onUpdateGrimReduce(UpdateEvent event) {
        if (!this.isEnabled() || this.mode.getValue() != 5) return;
        if (event.getType() != EventType.PRE) return;
        if (mc.player == null || mc.world == null) {
            this.resetGrimReduce();
            return;
        }

        if (this.grimSuspending) {
            this.grimSuspendTicks++;
            boolean timeout = this.grimSuspendTicks >= this.maxAirTicks.getValue();
            if (mc.player.isOnGround() || timeout) {
                boolean grounded = mc.player.isOnGround();
                Entity target = this.getAttackTargetNoXZ();
                boolean canReduce = grounded && mc.player.isSprinting() && this.isValidTargetGrimReduce(target) && !this.badPackets();

                this.releaseGrimReduce();

                if (canReduce) {
                    this.doReduceGrimReduce(target);
                } else if (grounded && mc.player.isSprinting()) {
                    mc.player.setSprinting(false);
                }
            }
            return;
        }

        if (this.grimKnockback) {
            this.grimKnockback = false;
            if (this.badPackets() || this.isBlockedState()) return;
            if (!mc.player.isSprinting()) return;
            Entity target = this.getAttackTargetNoXZ();
            if (this.isValidTargetGrimReduce(target)) {
                this.doReduceGrimReduce(target);
            }
        }
    }

    @EventTarget
    public void onStrafeGrimReduce(StrafeEvent event) {
        if (!this.isEnabled() || this.mode.getValue() != 5) return;
        if (mc.player == null) return;
        if (this.grimSuspending) {
            event.setForward(1.0F);
            event.setStrafe(0.0F);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.isCancelled()) return;

        if (this.mode.getValue() == 3 && event.getType() == EventType.SEND) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof UpdateSelectedSlotC2SPacket) {
                this.slot = true;
            } else if (packet instanceof HandSwingC2SPacket) {
                this.swing = true;
            } else if (packet instanceof PlayerInteractEntityC2SPacket) {
                ((PlayerInteractEntityC2SPacket) packet).handle(new PlayerInteractEntityC2SPacket.Handler() {
                    @Override
                    public void interact(Hand hand) {
                    }

                    @Override
                    public void interactAt(Hand hand, Vec3d pos) {
                    }

                    @Override
                    public void attack() {
                        Velocity.this.attack = true;
                    }
                });
            } else if (packet instanceof PlayerInteractBlockC2SPacket) {
                this.block = true;
            } else if (packet instanceof PlayerInteractItemC2SPacket) {
                this.block = true;
            } else if (packet instanceof PlayerActionC2SPacket) {
                this.block = true;
                this.dig = true;
            } else if (packet instanceof CloseHandledScreenC2SPacket
                    || packet instanceof ClickSlotC2SPacket
                    || (packet instanceof ClientCommandC2SPacket &&
                            ((ClientCommandC2SPacket) packet).getMode() == ClientCommandC2SPacket.Mode.OPEN_INVENTORY)) {
                this.inventory = true;
            } else if (packet instanceof PlayerMoveC2SPacket) {
                this.resetBadPackets();
            }
        }

        if (event.getType() == EventType.RECEIVE) {
            if (this.mode.getValue() == 0 || this.mode.getValue() == 1) {
                if (event.getPacket() instanceof ExplosionS2CPacket) {
                    ExplosionS2CPacket packet = (ExplosionS2CPacket) event.getPacket();
                    if (packet.playerKnockback().isPresent()) {
                        Vec3d kb = packet.playerKnockback().get();
                        if (kb.x != 0.0 || kb.y != 0.0 || kb.z != 0.0) {
                            this.pendingExplosion = true;
                            if (this.explosionHorizontal.getValue() == 0 || this.explosionVertical.getValue() == 0) {
                                event.setCancelled(true);
                            }
                            if (this.debugLog.getValue() && mc.player != null) {
                                ChatUtil.sendFormatted(
                                        String.format(
                                                "%sExplosion (&otick: %d, x: %.2f, y: %.2f, z: %.2f&r)&r",
                                                Myau.clientName,
                                                mc.player.age,
                                                mc.player.getVelocity().x + kb.x,
                                                mc.player.getVelocity().y + kb.y,
                                                mc.player.getVelocity().z + kb.z
                                        )
                                );
                            }
                        }
                    }
                }
            }

            if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket) {
                EntityVelocityUpdateS2CPacket packet = (EntityVelocityUpdateS2CPacket) event.getPacket();
                if (mc.player != null && packet.getEntityId() == mc.player.getId()) {

                    if (this.mode.getValue() == 5) {
                        if (this.grimSuspending) return;
                        if (!this.isPlayerKnockback()) return;
                        if (this.isBlockedState()) return;
                        if (!mc.player.isOnGround()) {
                            this.grimSuspending = true;
                            this.grimSuspendTicks = 0;
                            this.knockbackPacket = packet;
                            event.setCancelled(true);
                        } else {
                            this.grimKnockback = true;
                        }
                    }

                    if (this.mode.getValue() == 6) {
                        this.polarKb = true;
                        if (this.polarMode.getValue() == 1) {
                            RayCastUtil.RayCastResult result = RayCastUtil.rayCast(
                                    new RotationUtil.RotationVec(mc.player.getYaw(), mc.player.getPitch()), 2.9F);
                            KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
                            LivingEntity target = killAura != null ? killAura.getTarget() : null;
                            if (target != null
                                    && result != null && result.typeOfHit == RayCastUtil.RayCastResult.Type.ENTITY
                                    && result.entityHit instanceof PlayerEntity
                                    && RotationUtil.distanceToEntity(target) > 1
                                    && this.polarSb < 1) {
                                event.setCancelled(true);
                                this.polarSb++;
                            } else {
                                this.polarSb = Math.max(0, this.polarSb - 0.1);
                            }
                        }
                    }

                    if (this.mode.getValue() == 7) {
                        LongJump longJump = (LongJump) Myau.moduleManager.modules.get(LongJump.class);
                        if (!this.delayActive
                                && !this.canDelay()
                                && !this.isInLiquidOrWeb()
                                && !this.pendingExplosion
                                && (!this.allowNext || !this.fakeCheck.getValue())
                                && (longJump == null || !longJump.isEnabled() || !longJump.canStartJump())) {
                            this.delayChanceCounter = this.delayChanceCounter % 100 + this.delayChance.getValue();
                            if (this.delayChanceCounter >= 100) {
                                this.delayActive = true;
                                this.delayTickCount = 0;
                                this.knockbackPacket = packet;
                                event.setCancelled(true);
                                return;
                            }
                        }
                    }

                    if (this.mode.getValue() == 2) {
                        this.hasReceivedVelocity = true;
                        this.ticksSinceVelocity = 0;
                        this.jumpFlag = packet.getVelocityY() > 0;
                        this.pressed = mc.options.jumpKey.isPressed();
                        this.ShouldJump = true;
                    }

                    if (this.mode.getValue() == 3 && this.slapReduce.getValue()) {
                        this.slapReduceTicks = this.calculateSlapTicks(
                                (int) (packet.getVelocityX() * 8000.0), (int) (packet.getVelocityZ() * 8000.0)
                        );
                        if (this.debugLog.getValue()) {
                            ChatUtil.sendFormatted(Myau.clientName + "Attack reduceTicks: " + this.slapReduceTicks);
                        }
                    }
                    if (this.timer.getValue()) {
                        this.lastTimerAttackTime = System.currentTimeMillis();
                        if (mc.player.isOnGround()) {
                            this.timerJumpPending = true;
                        }
                        this.timerEnableAt = this.lastTimerAttackTime + 300L;
                    }
                    if (this.debugLog.getValue()) {
                        ChatUtil.sendFormatted(
                                String.format(
                                        "%sVelocity (&otick: %d, x: %.2f, y: %.2f, z: %.2f&r)&r",
                                        Myau.clientName,
                                        mc.player.age,
                                        packet.getVelocityX(),
                                        packet.getVelocityY(),
                                        packet.getVelocityZ()
                                )
                        );
                    }
                }
            }

            if (event.getPacket() instanceof EntityStatusS2CPacket) {
                EntityStatusS2CPacket packet = (EntityStatusS2CPacket) event.getPacket();
                net.minecraft.client.world.ClientWorld world = mc.world;
                if (world != null && mc.player != null) {
                    Entity entity = packet.getEntity(world);
                    if (entity != null && entity.equals(mc.player) && packet.getStatus() == 2) {
                        this.allowNext = false;
                    }
                }
            }
        }
    }

    private boolean isPlayerKnockback() {
        if (mc.player == null || mc.world == null) {
            return false;
        }
        double radius = this.reach.getValue() + 2.0;
        double radiusSq = radius * radius;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;
            if (mc.player.squaredDistanceTo(player) <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlockedState() {
        if (mc.player == null) {
            return false;
        }
        return mc.player.isClimbing() || this.isInLiquidOrWeb() || this.isOnFireBlock();
    }

    private boolean isOnFireBlock() {
        if (mc.player == null || mc.world == null) {
            return false;
        }
        double x = mc.player.getX();
        double z = mc.player.getZ();
        return mc.world.getBlockState(new BlockPos((int) x, (int) mc.player.getY(), (int) z)).isOf(Blocks.FIRE)
                || mc.world.getBlockState(new BlockPos((int) x, (int) (mc.player.getY() - 0.2), (int) z)).isOf(Blocks.FIRE);
    }

    private boolean isValidTargetGrimReduce(Entity entity) {
        return entity != null && entity.isAlive() && entity != mc.player;
    }

    private void doReduceGrimReduce(Entity target) {
        if (mc.player == null || target == null || mc.interactionManager == null) return;
        if (!(target instanceof LivingEntity)) return;
        mc.interactionManager.attackEntity(mc.player, (LivingEntity) target);
        mc.player.swingHand(Hand.MAIN_HAND);
        Vec3d velocity = mc.player.getVelocity();
        mc.player.setVelocity(velocity.x * 0.6, velocity.y, velocity.z * 0.6);
        mc.player.setSprinting(false);
    }

    private void releaseGrimReduce() {
        this.applyKnockbackPacketNoXZ();
        this.grimSuspending = false;
        this.grimSuspendTicks = 0;
    }

    private void resetGrimReduce() {
        this.grimSuspending = false;
        this.grimSuspendTicks = 0;
        this.grimKnockback = false;
    }

    private void resetPolar() {
        this.polarKb = false;
        this.polarSb = 0;
    }

    private void resetDelay() {
        this.delayActive = false;
        this.delayChanceCounter = 0;
        this.delayTickCount = 0;
    }

    private int calculateSlapTicks(int motionX, int motionZ) {
        double kb = Math.hypot(motionX, motionZ);
        if (!tickExactEnable.getValue()) {
            double ticks = 6.43153527E-4 * kb + 2.9419087136;
            int result = (int) Math.round(ticks);
            if (result < 1) result = 1;
            if (result > 10) result = 10;
            return result;
        }
        if (kb <= 500) return tick500.getValue();
        if (kb <= 1000) return tick1000.getValue();
        if (kb <= 2000) return tick2000.getValue();
        if (kb <= 3000) return tick3000.getValue();
        if (kb <= 4000) return tick4000.getValue();
        if (kb <= 5000) return tick5000.getValue();
        if (kb <= 6000) return tick6000.getValue();
        if (kb <= 7000) return tick7000.getValue();
        if (kb <= 8000) return tick8000.getValue();
        if (kb <= 9000) return tick9000.getValue();
        return tick10000.getValue();
    }

    private boolean badPackets() {
        return this.badPackets(false, false, false, false, false, false);
    }

    private boolean badPackets(boolean p1, boolean p2, boolean p3, boolean p4, boolean p5, boolean p6) {
        if (this.slot && !p1) return true;
        if (this.attack && !p2) return true;
        if (this.swing && !p3) return true;
        if (this.block && !p4) return true;
        if (this.inventory && !p5) return true;
        if (this.dig && !p6) return true;
        return false;
    }

    private void resetBadPackets() {
        this.slot = false;
        this.swing = false;
        this.attack = false;
        this.block = false;
        this.inventory = false;
        this.dig = false;
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.onDisabled();
    }

    @Override
    public void onEnabled() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.rotatoTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0;
        this.knockbackZ = 0;
        this.reduceTick = -1;
        this.hasReceivedVelocity = false;
        this.ticksSinceVelocity = -1;
        extraAttacked = false;
        velocityAttacked = false;
        this.jumpFlag = false;
        this.ShouldJump = false;
        this.slapReduceTicks = 0;
        this.slapAnInt = 0;
        this.resetBadPackets();
        this.resetAllNoXZ();
        this.disableVelocityTimer();
    }

    @Override
    public void onDisabled() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.hasReceivedVelocity = false;
        this.rotatoTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0;
        this.knockbackZ = 0;
        this.reduceTick = -1;
        this.ticksSinceVelocity = -1;
        extraAttacked = false;
        velocityAttacked = false;
        this.jumpFlag = false;
        this.ShouldJump = false;
        this.slapReduceTicks = 0;
        this.slapAnInt = 0;
        this.resetBadPackets();
        this.resetAllNoXZ();
        this.disableVelocityTimer();
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacketNoXZ(PacketEvent event) {
        if (!this.isEnabled() || this.mode.getValue() != 4) return;
        if (event.isCancelled()) return;
        if (this.isFlushing) return;
        if (mc.player == null || mc.world == null) return;

        if (event.getType() == EventType.RECEIVE) {
            if (this.shouldIgnoreNoXZ()) return;
            Packet<?> packet = event.getPacket();

            if (packet instanceof PlayerPositionLookS2CPacket) {
                if (this.isSuspending) {
                    this.pendingRelease = true;
                }
                this.flagCooldown = 2;
                this.knockbackPacket = null;
                return;
            }

            if (this.flagCooldown != 0) return;

            if (this.isSuspending) {
                if (!this.isAllowedPacketNoXZ(packet)) {
                    this.packetQueue.add(packet);
                    event.setCancelled(true);
                }
                return;
            }

            if (packet instanceof EntityVelocityUpdateS2CPacket motionPacket) {
                if (motionPacket.getEntityId() != mc.player.getId()) return;
                double dx = -motionPacket.getVelocityX();
                double dz = -motionPacket.getVelocityZ();
                if (Math.abs(dx) > 0.01 || Math.abs(dz) > 0.01) {
                    this.hitCounter = 1;
                }
                if (motionPacket.getVelocityY() > 0) {
                    Entity target;
                    this.sprintBoostCounter = this.sprintBoostCounter % 100 + 100;
                    if (this.sprintBoostCounter >= 100) {
                        this.shouldJump = true;
                    }
                    boolean canAttack = this.isValidTargetNoXZ(target = this.getAttackTargetNoXZ()) && mc.player.isSprinting();
                    if (!mc.player.isOnGround()) {
                        this.isSuspending = true;
                        this.suspendTicks = 0;
                        this.knockbackPacket = motionPacket;
                        event.setCancelled(true);
                    } else if (canAttack) {
                        this.attackTarget = target;
                        this.attacksRemaining = this.attackAmount.getValue();
                    } else {
                        this.isSuspending = true;
                        this.suspendTicks = 0;
                        this.knockbackPacket = motionPacket;
                        event.setCancelled(true);
                    }
                }
            }
        } else if (event.getType() == EventType.SEND) {
            if (this.isSuspending && event.getPacket() instanceof PlayerMoveC2SPacket movePacket) {
                this.movePacketQueue.add(movePacket);
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onTickNoXZ(TickEvent event) {
        if (!this.isEnabled() || this.mode.getValue() != 4) return;
        if (event.getType() != EventType.PRE) return;
        if (mc.player == null || mc.world == null) return;

        if (this.pendingRelease) {
            this.pendingRelease = false;
            this.releaseNoXZ();
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
            if (this.attackCooldown <= 0) {
                this.attackCount = 0;
            }
        }

        if (this.hitCounter > 0) {
            this.hitCounter++;
            if (this.hitCounter > 2) {
                this.hitCounter = 0;
            }
        }

        if (mc.player.isDead() || this.shouldIgnoreNoXZ()) {
            this.clearTargetNoXZ();
            if (this.isSuspending) {
                this.releaseNoXZ();
            }
            if (this.isInstantAttacking) {
                this.isInstantAttacking = false;
                this.instantAttackProgress = 0.0F;
            }
            return;
        }

        if (this.flagCooldown > 0) {
            this.flagCooldown--;
            this.clearTargetNoXZ();
        }

        if (this.isSuspending) {
            this.suspendTicks++;
            boolean instantAttackEnabled = this.instantAttack.getValue();
            if (instantAttackEnabled && this.instantAttackProgress < 3.0F) {
                this.instantAttackProgress += 0.5F;
                this.instantAttackProgress = Math.min(this.instantAttackProgress, 3.0F);
            }
            boolean onGround = mc.player.isOnGround();
            boolean isTimeout = this.suspendTicks >= 12;
            if (onGround || isTimeout) {
                Entity target = this.getAttackTargetNoXZ();
                boolean canAttack = this.isValidTargetNoXZ(target);
                boolean sprinting = mc.player.isSprinting();
                if (onGround && canAttack && sprinting) {
                    this.isFlushing = true;
                    this.attackTarget = target;
                    this.attacksRemaining = this.attackAmount.getValue();
                    this.sendMovePacketsNoXZ();
                    this.applyKnockbackPacketNoXZ();
                    if (instantAttackEnabled && this.instantAttackProgress > 0.0F) {
                        this.attacksRemaining = (int) this.instantAttackProgress;
                        this.scheduleMotionFlushNoXZ();
                        this.isSuspending = false;
                        this.suspendTicks = 0;
                        this.isFlushing = false;
                        this.isInstantAttacking = true;
                    } else {
                        this.doAttackSequenceNoXZ();
                        this.scheduleMotionFlushNoXZ();
                        this.isSuspending = false;
                        this.suspendTicks = 0;
                        this.isFlushing = false;
                    }
                } else {
                    this.releaseNoXZ();
                    this.instantAttackProgress = 0.0F;
                    if (onGround && mc.player.isSprinting()) {
                        mc.player.setSprinting(false);
                    }
                }
                return;
            }
            return;
        }

        if (this.isInstantAttacking) {
            this.instantAttackProgress -= 1.0F;
            if (this.instantAttackProgress <= 0.0F) {
                this.instantAttackProgress = 0.0F;
                this.isInstantAttacking = false;
            }
        }

        if (this.attacksRemaining > 0 && this.attackTarget != null) {
            this.doAttackSequenceNoXZ();
        }
    }

    @EventTarget
    public void onUpdateNoXZ(UpdateEvent event) {
        if (!this.isEnabled() || this.mode.getValue() != 4) return;
        if (event.getType() == EventType.POST && this.shouldFlushMotion) {
            Packet<?> packet;
            while ((packet = this.packetQueue.poll()) != null) {
                PacketUtil.receivePacket(packet);
            }
            this.shouldFlushMotion = false;
        }
    }

    @EventTarget
    public void onStrafeNoXZ(StrafeEvent event) {
        if (!this.isEnabled() || this.mode.getValue() != 4) return;
        if (mc.player == null) return;
        if (this.hitCounter > 0) {
            event.setForward(1.0F);
        }
    }

    private boolean shouldIgnoreNoXZ() {
        if (mc.player == null || mc.world == null) return true;
        if (mc.player.isDead() || mc.player.getHealth() <= 0.0F) return true;
        if (mc.player.isSpectator() || mc.player.getAbilities().flying) return true;
        if (mc.player.isInLava() || mc.player.isOnFire()
                || mc.player.isTouchingWater() || mc.player.isClimbing() || mc.player.isSleeping()) return true;
        return mc.world.getBlockState(mc.player.getBlockPos()).isOf(net.minecraft.block.Blocks.COBWEB);
    }

    private double getAABBDistanceNoXZ(Entity entity) {
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

    private Entity getAttackTargetNoXZ() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.getTarget() != null) {
            return killAura.getTarget();
        }
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            return ((EntityHitResult) mc.crosshairTarget).getEntity();
        }
        return null;
    }

    private boolean isValidTargetNoXZ(Entity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) {
            return false;
        }
        return this.getAABBDistanceNoXZ(entity) <= 3.7;
    }

    private void doAttackSequenceNoXZ() {
        if (this.attackTarget == null || !this.attackTarget.isAlive()) {
            this.clearTargetNoXZ();
            return;
        }
        if (this.getAABBDistanceNoXZ(this.attackTarget) > 3.7) {
            this.clearTargetNoXZ();
            return;
        }
        this.attackCount = this.attacksRemaining--;
        this.attackCooldown = 2;
        this.doAttackNoXZ(this.attackTarget);
        if (this.attacksRemaining <= 0) {
            this.clearTargetNoXZ();
        }
    }

    private boolean doAttackNoXZ(Entity entity) {
        if (mc.player == null || mc.interactionManager == null) {
            return false;
        }
        if (this.sprintStateCheck.getValue() && !mc.player.isSprinting()) {
            return false;
        }
        boolean wasSprinting = mc.player.isSprinting();
        if (wasSprinting) {
            mc.player.setSprinting(false);
        }
        mc.interactionManager.attackEntity(mc.player, entity);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (wasSprinting) {
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x * 0.6, velocity.y, velocity.z * 0.6);
        }
        return true;
    }

    private void sendMovePacketsNoXZ() {
        PlayerMoveC2SPacket movePacket;
        while ((movePacket = this.movePacketQueue.poll()) != null) {
            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(movePacket);
            }
        }
    }

    private void applyKnockbackPacketNoXZ() {
        if (this.knockbackPacket != null && mc.getNetworkHandler() != null) {
            try {
                ((Packet<ClientPlayPacketListener>) this.knockbackPacket).apply(mc.getNetworkHandler());
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            this.knockbackPacket = null;
        }
    }

    private void scheduleMotionFlushNoXZ() {
        this.shouldFlushMotion = true;
    }

    private boolean isAllowedPacketNoXZ(Packet<?> packet) {
        return packet instanceof EntityVelocityUpdateS2CPacket
                || packet instanceof HealthUpdateS2CPacket
                || packet instanceof PlayerPositionLookS2CPacket
                || packet instanceof PlaySoundS2CPacket
                || packet instanceof ChatMessageS2CPacket
                || packet instanceof DeathMessageS2CPacket
                || packet instanceof CloseScreenS2CPacket
                || packet instanceof DamageTiltS2CPacket
                || packet instanceof TitleS2CPacket
                || packet instanceof TeamS2CPacket
                || packet instanceof GameMessageS2CPacket
                || packet instanceof DisconnectS2CPacket
                || (packet instanceof EntityAnimationS2CPacket && ((EntityAnimationS2CPacket) packet).getEntityId() != mc.player.getId());
    }

    private void releaseNoXZ() {
        this.isFlushing = true;
        this.sendMovePacketsNoXZ();
        this.applyKnockbackPacketNoXZ();
        this.scheduleMotionFlushNoXZ();
        this.isFlushing = false;
        this.isSuspending = false;
        this.suspendTicks = 0;
        this.instantAttackProgress = 0.0F;
        this.isInstantAttacking = false;
    }

    private void clearTargetNoXZ() {
        this.attackTarget = null;
        this.attacksRemaining = 0;
    }

    private void resetSuspensionNoXZ() {
        this.isSuspending = false;
        this.suspendTicks = 0;
        this.knockbackPacket = null;
        this.packetQueue.clear();
        this.movePacketQueue.clear();
        this.isFlushing = false;
        this.instantAttackProgress = 0.0F;
        this.isInstantAttacking = false;
        this.shouldFlushMotion = false;
        this.pendingRelease = false;
    }

    public void resetAllNoXZ() {
        this.clearTargetNoXZ();
        this.flagCooldown = 0;
        this.shouldJump = false;
        this.sprintBoostCounter = 0;
        this.hitCounter = 0;
        this.attackCooldown = 0;
        this.resetSuspensionNoXZ();
        this.resetGrimReduce();
        this.resetPolar();
        this.resetDelay();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
