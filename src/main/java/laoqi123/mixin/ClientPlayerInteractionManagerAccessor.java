package laoqi123.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Accessor("currentBreakingProgress")
    float getCurBlockDamageMP();

    @Accessor("currentBreakingProgress")
    void setCurBlockDamageMP(float float1);

    @Accessor("blockBreakingCooldown")
    int getBlockHitDelay();

    @Accessor("blockBreakingCooldown")
    void setBlockHitDelay(int integer);

    @Accessor("breakingBlock")
    boolean getIsHittingBlock();

    @Accessor("lastSelectedSlot")
    int getCurrentPlayerItem();

    @Accessor("lastSelectedSlot")
    void setCurrentPlayerItem(int integer);

    @Invoker("syncSelectedSlot")
    void callSyncCurrentPlayItem();
}
