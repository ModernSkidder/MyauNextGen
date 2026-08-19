package laoqi123.module.modules.movement;

import laoqi123.module.Module;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.PercentProperty;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;

public class KeepSprint extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final PercentProperty slowdown = new PercentProperty("slowdown", 0);
    public final BooleanProperty groundOnly = new BooleanProperty("ground-only", false);
    public final BooleanProperty reachOnly = new BooleanProperty("reach-only", false);

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
