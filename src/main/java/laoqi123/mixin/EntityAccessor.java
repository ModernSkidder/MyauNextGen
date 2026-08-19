package laoqi123.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("inPowderSnow")
    boolean getIsInWeb();

    @Invoker("getRotationVector")
    Vec3d callGetVectorForRotation(float float1, float float2);
}
