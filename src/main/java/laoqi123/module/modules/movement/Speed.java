package laoqi123.module.modules.movement;

import com.google.common.base.CaseFormat;
import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.events.*;
import laoqi123.management.RotationState;
import laoqi123.mixin.EntityAccessor;
import laoqi123.module.Module;
import laoqi123.module.modules.player.Scaffold;
import laoqi123.module.modules.combat.KillAura;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.property.properties.ModeProperty;
import laoqi123.property.properties.PercentProperty;
import laoqi123.util.MoveUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.PlayerInput;

public class Speed extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"TimerBalance", "Normal"});
    public final FloatProperty timerBoostMultiplier = new FloatProperty("TimerBoostMultiplier", 0.5f, 0.1f, 1f, () -> this.mode.getValue() == 0);
    public final IntProperty lowTimerTicks = new IntProperty("LowTimerTicks", 6, 1, 10, () -> this.mode.getValue() == 0);
    public final BooleanProperty rotation = new BooleanProperty("Rotation", false, () -> this.mode.getValue() == 0);
    public final FloatProperty multiplier = new FloatProperty("Multiplier", 1.0F, 0.0F, 10.0F, () -> this.mode.getValue() == 1);
    public final FloatProperty friction = new FloatProperty("Friction", 1.0F, 0.0F, 10.0F, () -> this.mode.getValue() == 1);
    public final PercentProperty strafe = new PercentProperty("Strafe", 0, () -> this.mode.getValue() == 1);

    public Speed() {
        super("Speed", false);
    }

    private int ticks = 0;
    private float yaw = 0f;
    private YawOffsetMode yawOffsetMode = YawOffsetMode.AIR;

    public enum YawOffsetMode {
        GROUND("Ground"),
        AIR("Air"),
        CONSTANT("Constant");

        private final String tag;

        YawOffsetMode(String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }
    }

    private boolean finished = false;

    private void computeGroundYawOffset(PlayerEntity player) {
        if (player.isOnGround()) {
            yaw = getYawOffsetFromKeys();
        } else {
            yaw = 0f;
        }
    }

    private void computeAirYawOffset(PlayerEntity player) {
        if (!player.isOnGround()
                && mc.options.forwardKey.isPressed()
                && !mc.options.leftKey.isPressed()
                && !mc.options.rightKey.isPressed()) {
            yaw = -45f;
        } else {
            yaw = 0f;
        }
    }

    private void computeConstantYawOffset(PlayerEntity player) {
        yaw = getYawOffsetFromKeys();
    }

    private float getYawOffsetFromKeys() {
        KeyBinding forward = mc.options.forwardKey;
        KeyBinding back = mc.options.backKey;
        KeyBinding left = mc.options.leftKey;
        KeyBinding right = mc.options.rightKey;

        if (forward.isPressed() && left.isPressed()) return 45f;
        if (forward.isPressed() && right.isPressed()) return -45f;
        if (back.isPressed() && left.isPressed()) return 135f;
        if (back.isPressed() && right.isPressed()) return -135f;
        if (back.isPressed()) return 180f;
        if (left.isPressed()) return 90f;
        if (right.isPressed()) return -90f;
        return 0f;
    }

    private boolean canBoost() {
        Scaffold scaffold = (Scaffold) Myau.moduleManager.getModule(Scaffold.class);
        return !scaffold.isEnabled() && MoveUtil.isForwardPressed()
                && mc.player.getHungerManager().getFoodLevel() > 6
                && !mc.player.isSneaking()
                && !mc.player.isTouchingWater()
                && !mc.player.isInLava()
                && !((EntityAccessor) mc.player).getIsInWeb();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (isEnabled() && this.mode.getValue() == 0 && event.getType() == EventType.PRE) {
            if (canBoost()) {
                if (!mc.player.isOnGround()) {
                    if (ticks < lowTimerTicks.getValue() && !finished && mc.player.getVelocity().y < 0) {
                        ticks++;
                        if (ticks == lowTimerTicks.getValue()) {
                            finished = true;
                        }
                    }
                    if (finished) {
                        if (ticks > 0) {
                            ticks--;
                            if (ticks == 0) {
                                finished = false;
                            }
                        }
                    }
                } else {
                    finished = false;
                    ticks = 0;
                }
            } else {
                finished = false;
                ticks = 0;
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (isEnabled() && this.mode.getValue() == 0 && event.getType() == EventType.PRE && rotation.getValue()) {
            if (canBoost() && !Myau.moduleManager.getModule(KillAura.class).isEnabled()) {
                switch (yawOffsetMode) {
                    case GROUND:
                        computeGroundYawOffset(mc.player);
                        break;
                    case AIR:
                        computeAirYawOffset(mc.player);
                        break;
                    case CONSTANT:
                        computeConstantYawOffset(mc.player);
                        break;
                }
                event.setRotation(mc.player.getYaw() - yaw, mc.player.getPitch(), 2);
                event.setPervRotation(mc.player.getYaw() - yaw, 2);
            }
        }
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (this.isEnabled() && this.mode.getValue() == 0 && rotation.getValue() && canBoost() && !Myau.moduleManager.getModule(KillAura.class).isEnabled()) {
            if (RotationState.isActived() && RotationState.getPriority() == 2.0F && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }
        }
    }

    @EventTarget(Priority.LOW)
    public void onStrafe(StrafeEvent event) {
        if (this.mode.getValue() == 1) {
            if (this.isEnabled() && this.canBoost()) {
                if (mc.player.isOnGround()) {
                    mc.player.setVelocity(mc.player.getVelocity().x, 0.42F, mc.player.getVelocity().z);
                    MoveUtil.setSpeed(
                            MoveUtil.getJumpMotion() * (double) this.multiplier.getValue(),
                            MoveUtil.getMoveYaw()
                    );
                } else {
                    if (this.friction.getValue() != 1.0F) {
                        event.setFriction(event.getFriction() * this.friction.getValue());
                    }
                    if (this.strafe.getValue() > 0) {
                        double speed = MoveUtil.getSpeed();
                        MoveUtil.setSpeed(speed * (double) ((float) (100 - this.strafe.getValue()) / 100.0F), MoveUtil.getDirectionYaw());
                        MoveUtil.addSpeed(
                                speed * (double) ((float) this.strafe.getValue() / 100.0F), MoveUtil.getMoveYaw()
                        );
                        MoveUtil.setSpeed(speed);
                    }
                }
            }
        }
    }

    @Override
    public void onDisabled() {
        finished = false;
        ticks = 0;
    }

    @EventTarget(Priority.LOW)
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.mode.getValue() == 1) {
            if (this.isEnabled() && this.canBoost()) {
                PlayerInput playerInput = mc.player.input.playerInput;
                mc.player.input.playerInput = new PlayerInput(playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right(), false, playerInput.sneak(), playerInput.sprint());
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, mode.getModeString())};
    }
}
