package laoqi123.module.modules;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.TickEvent;
import laoqi123.mixin.ClientPlayerInteractionManagerAccessor;
import laoqi123.module.Module;
import laoqi123.property.properties.IntProperty;
import laoqi123.property.properties.PercentProperty;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;

public class SpeedMine extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final PercentProperty speed = new PercentProperty("speed", 15);
    public final IntProperty delay = new IntProperty("delay", 0, 0, 4);

    public SpeedMine() {
        super("SpeedMine", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (!mc.interactionManager.getCurrentGameMode().isCreative()) {
                if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                    ((ClientPlayerInteractionManagerAccessor) mc.interactionManager)
                            .setBlockHitDelay(Math.min(((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getBlockHitDelay(), this.delay.getValue() + 1));
                    if (((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getIsHittingBlock()) {
                        float curBlockDamageMP = ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getCurBlockDamageMP();
                        float damage = 0.3F * (this.speed.getValue().floatValue() / 100.0F);
                        if (curBlockDamageMP < damage) {
                            ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).setCurBlockDamageMP(damage);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.format("%d%%", this.speed.getValue())};
    }
}
