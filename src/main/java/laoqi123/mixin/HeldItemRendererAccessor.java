package laoqi123.mixin;

import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HeldItemRenderer.class)
public interface HeldItemRendererAccessor {
    @Accessor("equipProgressMainHand")
    float getEquippedProgress();

    @Accessor("prevEquipProgressMainHand")
    float getPrevEquippedProgress();
}
