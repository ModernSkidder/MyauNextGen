package laoqi123.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderDispatcher.class)
public interface EntityRenderDispatcherAccessor {
    @Accessor("camera")
    Camera getCamera();
}
