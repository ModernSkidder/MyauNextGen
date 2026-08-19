package laoqi123.module.modules.movement;

import com.google.common.base.CaseFormat;
import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.event.impl.*;
import laoqi123.management.RotationState;
import laoqi123.mixin.ClientPlayerInteractionManagerAccessor;
import laoqi123.module.Module;
import laoqi123.util.*;
import laoqi123.value.properties.FloatValue;
import laoqi123.value.properties.ModeValue;
import laoqi123.value.properties.PercentValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.FireChargeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;

public class LongJump extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final TimerUtil fireballTimer = new TimerUtil();
    private final TimerUtil jumpTimer = new TimerUtil();
    private boolean isJumping = false;
    private int tickCounter = 0;
    private int jumpModeStage = 0;
    private boolean readyToUseFireball = false;
    private boolean fireballLaunched = false;
    private int savedHotbarSlot = -1;
    public final ModeValue mode = new ModeValue("mode", 0, new String[]{"FIREBALL", "FIREBALL_MANUAL", "FIREBALL_HIGH", "FIREBALL_FLAT"});
    public final FloatValue motion = new FloatValue("motion", 1.0F, 1.0F, 20.0F);
    public final FloatValue speedMotion = new FloatValue("speed-motion", 1.0F, 1.0F, 20.0F);
    public final PercentValue strafe = new PercentValue("strafe", 0);

    private int findFireballInHotbar() {
        if (mc.player == null) {
            return -1;
        } else {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack != null && stack.getItem() instanceof FireChargeItem) {
                    return i;
                }
            }
            return -1;
        }
    }

    private double getMotionFactor() {
        return MoveUtil.getSpeedLevel() > 0
                ? (double) this.speedMotion.getValue()
                : (double) this.motion.getValue();
    }

    public LongJump() {
        super("LongJump", false);
    }

    public boolean isAutoMode() {
        return this.mode.getValue() == 0 || this.mode.getValue() == 2 || this.mode.getValue() == 3;
    }

    public boolean isManualMode() {
        return this.mode.getValue() == 1;
    }

    public boolean isLongJumpMode() {
        return this.isAutoMode() || this.isManualMode();
    }

    public boolean canStartJump() {
        return !this.fireballTimer.hasTimeElapsed(1000L) && !this.isJumping;
    }

    public boolean isJumping() {
        return this.isJumping;
    }

    @EventTarget(Priority.HIGHEST)
    public void onKnockback(KnockbackEvent event) {
        if (this.isEnabled() && !event.isCancelled()) {
            if ((this.isManualMode() || this.isAutoMode()) && this.canStartJump()) {
                event.setCancelled(true);
                this.isJumping = true;
                this.tickCounter = 0;
            }
        }
    }

    @EventTarget(Priority.HIGHEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            switch (event.getType()) {
                case PRE:
                    if (this.isAutoMode() && !this.fireballLaunched && this.readyToUseFireball) {
                        int slot = this.findFireballInHotbar();
                        if (slot != -1) {
                            this.savedHotbarSlot = mc.player.getInventory().selectedSlot;
                            mc.player.getInventory().selectedSlot = slot;
                            ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).callSyncCurrentPlayItem();
                            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                            this.fireballTimer.reset();
                            this.fireballLaunched = true;
                        }
                    }
                    break;
                case POST:
                    if (this.savedHotbarSlot != -1) {
                        mc.player.getInventory().selectedSlot = this.savedHotbarSlot;
                        this.savedHotbarSlot = -1;
                    }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.isLongJumpMode() && this.isJumping) {
                this.tickCounter++;
                if (this.tickCounter == 1) {
                    switch (this.mode.getValue()) {
                        case 0:
                        case 1:
                            this.jumpModeStage = 0;
                            break;
                        case 2:
                            this.jumpModeStage = 1;
                            break;
                        case 3:
                            this.jumpModeStage = MoveUtil.isForwardPressed() ? 2 : 1;
                    }
                }
                if (this.tickCounter == 2 && MoveUtil.isForwardPressed()) {
                    MoveUtil.setSpeed(MoveUtil.getSpeed() * this.getMotionFactor());
                }
                if (this.tickCounter >= 1 && this.tickCounter <= 30) {
                    switch (this.jumpModeStage) {
                        case 1:
                            if (this.tickCounter == 1) {
                                mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y * 0.75, mc.player.getVelocity().z);
                            } else {
                                double motion = mc.player.getVelocity().y / 0.98F + 0.055;
                                if (motion > 0.0) {
                                    mc.player.setVelocity(mc.player.getVelocity().x, motion, mc.player.getVelocity().z);
                                }
                            }
                            break;
                        case 2:
                            if (this.tickCounter == 1) {
                                mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y * 0.75, mc.player.getVelocity().z);
                            } else {
                                mc.player.setVelocity(mc.player.getVelocity().x, 0.01 + (double) this.tickCounter * 0.003, mc.player.getVelocity().z);
                            }
                    }
                }
                if (this.tickCounter >= 30) {
                    this.isJumping = false;
                    this.tickCounter = 0;
                    this.jumpModeStage = 0;
                    if (this.isAutoMode()) {
                        this.setEnabled(false);
                    }
                    return;
                }
            }
            if (this.isAutoMode() && !this.isJumping) {
                if (this.jumpTimer.hasTimeElapsed(1500L)) {
                    this.setEnabled(false);
                    return;
                }
                this.readyToUseFireball = true;
                float yaw = RotationUtil.quantizeAngle(mc.player.getYaw() - 180.0F - RandomUtil.nextFloat(0.0F, 1.0F));
                float pitch = RotationUtil.quantizeAngle(89.0F + RandomUtil.nextFloat(-0.25F, 0.25F));
                event.setRotation(yaw, pitch, 4);
                event.setPervRotation(yaw, 4);
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            if (RotationState.isActived()
                    && RotationState.getPriority() == 4.0F
                    && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled()) {
            if (this.isLongJumpMode()
                    && this.isJumping
                    && this.tickCounter >= 5
                    && this.tickCounter <= 30
                    && this.strafe.getValue() > 0) {
                double speed = MoveUtil.getSpeed();
                MoveUtil.setSpeed(speed * (double) ((float) (100 - this.strafe.getValue()) / 100.0F), MoveUtil.getDirectionYaw());
                MoveUtil.addSpeed(
                        speed * (double) ((float) this.strafe.getValue() / 100.0F), MoveUtil.getMoveYaw()
                );
                MoveUtil.setSpeed(speed);
            }
        }
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        InputUtil.Key useKey = InputUtil.fromTranslationKey(mc.options.useKey.getBoundKeyTranslationKey());
        int useKeyCode = useKey.getCode();
        if (useKey.getCategory() == InputUtil.Type.MOUSE) {
            useKeyCode -= 100;
        }
        if (event.getKey() == useKeyCode) {
            ItemStack stack = mc.player.getMainHandStack();
            if (stack != null && stack.getItem() instanceof FireChargeItem) {
                this.fireballTimer.reset();
            }
        }
    }

    @EventTarget(Priority.HIGH)
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
                this.isJumping = false;
                this.tickCounter = 0;
                this.jumpModeStage = 0;
                if (this.isAutoMode()) {
                    this.setEnabled(false);
                }
            }
        }
    }

    @Override
    public void onEnabled() {
        this.jumpTimer.reset();
        if (this.isAutoMode() && this.findFireballInHotbar() == -1) {
            this.setEnabled(false);
            ChatUtil.sendFormatted(String.format("%s%s: &cNo fireball found in your hotbar!&r", Myau.clientName, this.getName()));
        }
    }

    @Override
    public void onDisabled() {
        this.isJumping = false;
        this.tickCounter = 0;
        this.jumpModeStage = 0;
        this.readyToUseFireball = false;
        this.fireballLaunched = false;
    }

    @Override
    public String[] getSuffix() {
        String mode = this.mode.getModeString();
        return mode.contains("FIREBALL") ? new String[]{"Fireball"} : new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, mode)};
    }
}
