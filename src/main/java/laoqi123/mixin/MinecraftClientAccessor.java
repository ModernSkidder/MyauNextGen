package laoqi123.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("LOGGER")
    Logger getLogger();

    @Accessor("renderTickCounter")
    RenderTickCounter.Dynamic getTimer();

    @Accessor("itemUseCooldown")
    int getRightClickDelayTimer();

    @Accessor("itemUseCooldown")
    void setRightClickDelayTimer(int integer);

    @Accessor("attackCooldown")
    int getAttackCooldown();
}
