package laoqi123.module.modules.movement;

import laoqi123.module.Module;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.PercentValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;

public class KeepSprint extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final PercentValue slowdown = new PercentValue("slowdown", 0);
    public final BooleanValue groundOnly = new BooleanValue("ground-only", false);
    public final BooleanValue reachOnly = new BooleanValue("reach-only", false);

    public KeepSprint() {
        super("KeepSprint", false);
    }

    public boolean shouldKeepSprint() {
        if (this.groundOnly.getValue() && !mc.player.isOnGround()) {
            return false;
        } else {
            HitResult target = mc.crosshairTarget;
            Entity viewEntity = mc.getCameraEntity();
            return !this.reachOnly.getValue()
                    || target != null && viewEntity != null && target.getPos().distanceTo(viewEntity.getCameraPosVec(1.0F)) > 3.0;
        }
    }
}
