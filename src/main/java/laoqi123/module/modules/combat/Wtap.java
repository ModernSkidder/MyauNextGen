package laoqi123.module.modules.combat;

import laoqi123.event.EventTarget;
import laoqi123.event.types.Priority;
import laoqi123.events.AttackEvent;
import laoqi123.events.MoveInputEvent;
import laoqi123.module.Module;
import laoqi123.util.TimerUtil;
import laoqi123.property.properties.FloatProperty;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;

public class Wtap extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final TimerUtil timer = new TimerUtil();
    private boolean active = false;
    private boolean stopForward = false;
    private long delayTicks = 0L;
    private long durationTicks = 0L;
    public final FloatProperty delay = new FloatProperty("delay", 5.5F, 0.0F, 10.0F);
    public final FloatProperty duration = new FloatProperty("duration", 1.5F, 1.0F, 5.0F);

    private boolean canTrigger() {
        return !(mc.player.input.movementForward < 0.8F)
                && !mc.player.horizontalCollision
                && (!((float) mc.player.getHungerManager().getFoodLevel() <= 6.0F) || mc.player.getAbilities().allowFlying) && (mc.player.isSprinting()
                || !mc.player.isUsingItem() && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS) && mc.options.sprintKey.isPressed());
    }

    public Wtap() {
        super("WTap", false);
    }

    @EventTarget(Priority.LOWEST)
    public void onMoveInput(MoveInputEvent event) {
        if (this.active) {
            if (!this.stopForward && !this.canTrigger()) {
                this.active = false;
                while (this.delayTicks > 0L) {
                    this.delayTicks -= 50L;
                }
                while (this.durationTicks > 0L) {
                    this.durationTicks -= 50L;
                }
            } else if (this.delayTicks > 0L) {
                this.delayTicks -= 50L;
            } else {
                if (this.durationTicks > 0L) {
                    this.durationTicks -= 50L;
                    this.stopForward = true;
                    mc.player.input.movementForward = 0.0F;
                }
                if (this.durationTicks <= 0L) {
                    this.active = false;
                }
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isEnabled() && !event.isCancelled() && !this.active && this.timer.hasTimeElapsed(500L) && mc.player.isSprinting()) {
            this.timer.reset();
            this.active = true;
            this.stopForward = false;
            this.delayTicks = this.delayTicks + (long) (50.0F * this.delay.getValue());
            this.durationTicks = this.durationTicks + (long) (50.0F * this.duration.getValue());
        }
    }
}
