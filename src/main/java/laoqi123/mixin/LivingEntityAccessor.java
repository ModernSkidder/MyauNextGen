package laoqi123.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("activeStatusEffects")
    Map<RegistryEntry<StatusEffect>, StatusEffectInstance> getActivePotionsMap();

    @Accessor("SPRINTING_SPEED_BOOST")
    EntityAttributeModifier getSprintingSpeedBoostModifier();

    @Accessor("jumpingCooldown")
    int getJumpTicks();

    @Accessor("jumpingCooldown")
    void setJumpTicks(int integer);

    @Accessor("lastAttackedTicks")
    int getLastAttackedTicks();
}
