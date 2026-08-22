package laoqi123.module.modules;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.events.TickEvent;
import laoqi123.mixin.LivingEntityAccessor;
import laoqi123.module.Module;
import laoqi123.property.properties.IntProperty;
import net.minecraft.client.MinecraftClient;

public class NoJumpDelay extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final IntProperty delay = new IntProperty("delay", 3, 0, 8);

    public NoJumpDelay() {
        super("NoJumpDelay", false);
    }

    @EventTarget(Priority.HIGHEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            ((LivingEntityAccessor) mc.player)
                    .setJumpTicks(Math.min(((LivingEntityAccessor) mc.player).getJumpTicks(), this.delay.getValue() + 1));
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.delay.getValue().toString()};
    }
}
