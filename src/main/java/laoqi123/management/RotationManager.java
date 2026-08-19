package laoqi123.management;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.event.impl.Render3DEvent;
import laoqi123.event.impl.TickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class RotationManager {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private float lastUpdate;
    private float yawDelta;
    private float pitchDelta;
    private int priority;
    private boolean rotated;

    public RotationManager() {
        this.lastUpdate = Float.NaN;
        this.yawDelta = Float.NaN;
        this.pitchDelta = Float.NaN;
        this.priority = Integer.MIN_VALUE;
        this.rotated = false;
    }

    private void applyRotation(float partialTicks) {
        if (mc.player != null && !Float.isNaN(this.yawDelta) && !Float.isNaN(this.pitchDelta) && !Float.isNaN(this.lastUpdate)) {
            float yaw = this.yawDelta * (partialTicks - this.lastUpdate);
            if (yaw != 0.0F) {
                mc.player.prevYaw = mc.player.getYaw();
                mc.player.setYaw(mc.player.getYaw() + yaw);
            }
            float pitch = this.pitchDelta * (partialTicks - this.lastUpdate);
            if (pitch != 0.0F) {
                mc.player.prevPitch = mc.player.getPitch();
                mc.player.setPitch(mc.player.getPitch() + pitch);
                mc.player.setPitch(Math.clamp(mc.player.getPitch(), -90.0F, 90.0F));
            }
            this.lastUpdate = partialTicks;
        }
    }

    private void resetRotationState() {
        this.lastUpdate = Float.NaN;
        this.yawDelta = Float.NaN;
        this.pitchDelta = Float.NaN;
        this.priority = Integer.MIN_VALUE;
        this.rotated = false;
    }

    public void setRotation(float yaw, float pitch, int priority, boolean force) {
        if (this.priority <= priority) {
            this.priority = priority;
            this.yawDelta = MathHelper.wrapDegrees(yaw - mc.player.getYaw());
            this.pitchDelta = Math.clamp(pitch - mc.player.getPitch(), -90.0F, 90.0F);
            this.lastUpdate = 0.0F;
            this.rotated = force;
            this.applyRotation(0.0F);
        }
    }

    public boolean isRotated() {
        return this.rotated;
    }

    @EventTarget(Priority.HIGHEST)
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        this.applyRotation(1.0F);
        this.resetRotationState();
    }

    @EventTarget(Priority.HIGHEST)
    public void onRender3D(Render3DEvent event) {
        this.applyRotation(event.getPartialTicks());
    }
}
