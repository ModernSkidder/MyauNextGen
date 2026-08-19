package laoqi123.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityItemUseAccessor {
    @Accessor("activeItemStack")
    ItemStack getItemInUse();

    @Accessor("activeItemStack")
    void setItemInUse(ItemStack itemStack);

    @Accessor("itemUseTimeLeft")
    int getItemInUseCount();

    @Accessor("itemUseTimeLeft")
    void setItemInUseCount(int integer);
}
