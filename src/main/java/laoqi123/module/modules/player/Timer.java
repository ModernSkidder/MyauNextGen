package laoqi123.module.modules.player;

import laoqi123.Myau;
import laoqi123.enums.BlinkModules;
import laoqi123.event.EventManager;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.events.*;
import laoqi123.module.Module;
import laoqi123.property.properties.FloatProperty;
import laoqi123.util.KeyBindUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.PlayerInput;

public class Timer extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final long cooldown = 100;
    private static long lastTime = 0;

    public final FloatProperty speed = new FloatProperty("Speed", 1.0F, 0.0F, 10.0F);

    private boolean lastTimerKeyPressed = false;
    private double savedMotionX;
    private double savedMotionY;
    private double savedMotionZ;

    public Timer() {
        super("Timer", false);
    }

    public static boolean canToggle() {
        return System.currentTimeMillis() - lastTime > cooldown;
    }

    @Override
    public void onEnabled() {
        if (mc.player != null) {
            savedMotionX = mc.player.getVelocity().x;
            savedMotionY = mc.player.getVelocity().y;
            savedMotionZ = mc.player.getVelocity().z;
        }
        lastTimerKeyPressed = true;

        if (speed.getValue() == 0.0F) {
            Myau.blinkManager.setBlinkState(true, BlinkModules.BLINK);
        }
    }

    @Override
    public void onDisabled() {
        lastTime = System.currentTimeMillis();

        if (speed.getValue() == 0.0F) {
            Myau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
            if (mc.player != null) {
                mc.player.setVelocity(savedMotionX, savedMotionY, savedMotionZ);
            }
        }
    }

    @EventTarget(Priority.HIGHEST)
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled() || event.getType() != EventType.PRE) {
            return;
        }

        if (speed.getValue() == 0.0F) {
            return;
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!isEnabled()) return;

        if (speed.getValue() == 0.0F) {
            mc.player.setVelocity(0.0, 0.0, 0.0);
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (!isEnabled()) return;

        if (speed.getValue() == 0.0F) {
            event.setForward(0.0f);
            event.setStrafe(0.0f);
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!isEnabled()) return;

        if (speed.getValue() == 0.0F) {
            mc.player.input.movementForward = 0.0f;
            mc.player.input.movementSideways = 0.0f;
            PlayerInput playerInput = mc.player.input.playerInput;
            mc.player.input.playerInput = new PlayerInput(playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right(), false, false, playerInput.sprint());
        }
    }

    @EventTarget(Priority.HIGHEST)
    public void onRender3D(Render3DEvent event) {
        if (!isEnabled() || speed.getValue() != 0.0F) {
            return;
        }

        int timerKey = getKey();
        boolean timerKeyPressed = timerKey != 0 && KeyBindUtil.isKeyDown(timerKey);

        if (timerKeyPressed && !lastTimerKeyPressed && canToggle()) {
            EventManager.call(new KeyEvent(timerKey));
        }
        lastTimerKeyPressed = timerKeyPressed;

        EventManager.call(new TickEvent(EventType.PRE));

        if (mc.player != null && mc.world != null) {
            UpdateEvent preEvent = new UpdateEvent(
                    EventType.PRE,
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    mc.player.getYaw(),
                    mc.player.getPitch()
            );
            EventManager.call(preEvent);

            EventManager.call(new UpdateEvent(
                    EventType.POST,
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    mc.player.getYaw(),
                    mc.player.getPitch()
            ));
        }

        EventManager.call(new TickEvent(EventType.POST));
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.format("%.1fx", speed.getValue())};
    }
}
