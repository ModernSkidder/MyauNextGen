package laoqi123.module.modules.movement;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.LivingUpdateEvent;
import laoqi123.events.MoveInputEvent;
import laoqi123.events.PacketEvent;
import laoqi123.events.StrafeEvent;
import laoqi123.events.UpdateEvent;
import laoqi123.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

public class Stasis extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private double savedMotionX;
    private double savedMotionY;
    private double savedMotionZ;

    private int tickCounter;
    private int phase; // 0 = stasis, 1 = release
    private static final int STASIS_TICKS = 45;
    private static final int RELEASE_TICKS = 1;

    public Stasis() {
        super("Stasis", false);
    }

    @Override
    public void onEnabled() {
        if (mc.player != null) {
            Vec3d velocity = mc.player.getVelocity();
            savedMotionX = velocity.x;
            savedMotionY = velocity.y;
            savedMotionZ = velocity.z;
        }
        tickCounter = 0;
        phase = 0;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) return;

        tickCounter++;

        if (phase == 0 && tickCounter >= STASIS_TICKS) {
            phase = 1;
            tickCounter = 0;
            mc.player.setVelocity(savedMotionX, savedMotionY, savedMotionZ);
        } else if (phase == 1 && tickCounter >= RELEASE_TICKS) {
            phase = 0;
            tickCounter = 0;
            Vec3d velocity = mc.player.getVelocity();
            savedMotionX = velocity.x;
            savedMotionY = velocity.y;
            savedMotionZ = velocity.z;
        }

        if (phase == 0) {
            mc.player.setVelocity(0.0, 0.0, 0.0);
        }
        if (mc.player != null && mc.player.isOnGround()) {
            this.setEnabled(false);
            return;
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled() && phase == 0) {
            PlayerInput input = mc.player.input.playerInput;
            mc.player.input.playerInput = new PlayerInput(
                    input.forward(), input.backward(), input.left(), input.right(), false, false, input.sprint()
            );
            mc.player.input.movementForward = 0.0f;
            mc.player.input.movementSideways = 0.0f;
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && phase == 0) {
            mc.player.setVelocity(0.0, 0.0, 0.0);
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled() && phase == 0) {
            event.setForward(0.0f);
            event.setStrafe(0.0f);
        }
    }

    @EventTarget
    public void onSendPacket(PacketEvent e) {
        if (!this.isEnabled()) return;
        if (e.getType() != EventType.SEND) return;
        if (!(e.getPacket() instanceof PlayerMoveC2SPacket)) return;

        if (phase == 1) return;

        if (mc.player == null || mc.player.hurtTime != 0) {
            return;
        }

        if (!(e.getPacket() instanceof PlayerMoveC2SPacket.LookAndOnGround)) {
            e.setCancelled(true);
        }
    }

    @Override
    public void onDisabled() {
        if (mc.player != null) {
            mc.player.setVelocity(savedMotionX, savedMotionY, savedMotionZ);
        }
        tickCounter = 0;
        phase = 0;
    }
}
