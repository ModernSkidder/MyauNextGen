package laoqi123.util.clicking;

import laoqi123.mixin.LivingEntityAccessor;
import laoqi123.value.properties.FloatRangeValue;
import laoqi123.util.config.Configurable;
import net.minecraft.client.MinecraftClient;

public class ItemCooldown extends Configurable {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public final FloatRangeValue minimumCooldown;
    private float nextCooldown;

    public ItemCooldown() {
        super("ItemCooldown");
        this.minimumCooldown = this.register(new FloatRangeValue("Minimum", 1.0f, 1.0f, 0.0f, 2.0f));
        this.nextCooldown = this.randomCooldown();
    }

    public boolean isCooldownPassed(int ticks) {
        return this.cooldownProgress(ticks) >= this.nextCooldown;
    }

    public float cooldownProgress(int baseTime) {
        return (float) (((LivingEntityAccessor) mc.player).getLastAttackedTicks() + baseTime) * mc.player.getAttackCooldownProgressPerTick();
    }

    public void newCooldown() {
        this.nextCooldown = this.randomCooldown();
    }

    private float randomCooldown() {
        return this.minimumCooldown.random();
    }
}
