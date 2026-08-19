package laoqi123.module.modules.movement;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.event.impl.MoveInputEvent;
import laoqi123.event.impl.TickEvent;
import laoqi123.module.Module;
import laoqi123.util.ItemUtil;
import laoqi123.util.MoveUtil;
import laoqi123.util.PlayerUtil;
import laoqi123.util.RandomUtil;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.IntValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.PlayerInput;

import java.util.Objects;

public class Eagle extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private int sneakDelay = 0;
    public final IntValue minDelay = new IntValue("min-delay", 2, 0, 10);
    public final IntValue maxDelay = new IntValue("max-delay", 3, 0, 10);
    public final BooleanValue directionCheck = new BooleanValue("direction-check", true);
    public final BooleanValue jumpCheck = new BooleanValue("jump-check", true);
    public final BooleanValue pitchCheck = new BooleanValue("pitch-check", true);
    public final BooleanValue blocksOnly = new BooleanValue("blocks-only", true);
    public final BooleanValue sneakOnly = new BooleanValue("sneaking-only", false);

    private boolean isSneakingInput() {
        return mc.player.input.playerInput.sneak();
    }

    private void setSneakingInput(boolean sneaking) {
        PlayerInput input = mc.player.input.playerInput;
        mc.player.input.playerInput = new PlayerInput(
                input.forward(), input.backward(), input.left(), input.right(), input.jump(), sneaking, input.sprint()
        );
    }

    private boolean canMoveSafely() {
        double[] offset = MoveUtil.predictMovement();
        return PlayerUtil.canMove(mc.player.getVelocity().x + offset[0], mc.player.getVelocity().z + offset[1]);
    }

    private boolean shouldSneak() {
        if (this.directionCheck.getValue() && mc.options.forwardKey.isPressed()) {
            return false;
        } else if (this.jumpCheck.getValue() && mc.options.jumpKey.isPressed()) {
            return false;
        } else if (this.pitchCheck.getValue() && mc.player.getPitch() < 69.0F) {
            return false;
        } else if (sneakOnly.getValue() && !mc.options.sneakKey.isPressed()) {
            return false;
        } else {
            return (!this.blocksOnly.getValue() || ItemUtil.isHoldingBlock()) && mc.player.isOnGround();
        }
    }

    public Eagle() {
        super("Eagle", false);
    }

    @EventTarget(Priority.LOWEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.sneakDelay > 0) {
                this.sneakDelay--;
            }
            if (this.sneakDelay == 0 && this.canMoveSafely()) {
                this.sneakDelay = RandomUtil.nextInt(this.minDelay.getValue(), this.maxDelay.getValue() + 1);
            }
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled() && mc.currentScreen == null) {

            if (sneakOnly.getValue() && mc.options.sneakKey.isPressed() && shouldSneak()) {
                setSneakingInput(false);
                mc.player.input.movementForward /= 0.3F;
                mc.player.input.movementSideways /= 0.3F;
            }

            if (!isSneakingInput()) {
                if (this.shouldSneak() && (this.sneakDelay > 0 || this.canMoveSafely())) {
                    setSneakingInput(true);
                    mc.player.input.movementSideways *= 0.3F;
                    mc.player.input.movementForward *= 0.3F;
                }
            }
        }
    }

    @Override
    public void onDisabled() {
        this.sneakDelay = 0;
    }

    @Override
    public void verifyValue(String name) {
        switch (name) {
            case "min-delay":
                if (this.minDelay.getValue() > this.maxDelay.getValue()) {
                    this.maxDelay.setValue(this.minDelay.getValue());
                }
                break;
            case "max-delay":
                if (this.minDelay.getValue() > this.maxDelay.getValue()) {
                    this.minDelay.setValue(this.maxDelay.getValue());
                }
        }
    }

    @Override
    public String[] getSuffix() {
        return Objects.equals(this.minDelay.getValue(), this.maxDelay.getValue())
                ? new String[]{this.minDelay.getValue().toString()}
                : new String[]{String.format("%d-%d", this.minDelay.getValue(), this.maxDelay.getValue())};
    }
}
