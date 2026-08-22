package laoqi123.mixin;

import laoqi123.module.modules.ESP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = WorldRenderer.class, priority = 9999)
public abstract class MixinWorldRenderer {
    @Redirect(
            method = "renderEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;hasOutline(Lnet/minecraft/entity/Entity;)Z")
    )
    private boolean renderEntitiesHasOutline(MinecraftClient client, Entity entity) {
        return ESP.shouldOutline(entity) || client.hasOutline(entity);
    }

    @Redirect(
            method = "getEntitiesToRender",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;hasOutline(Lnet/minecraft/entity/Entity;)Z")
    )
    private boolean getEntitiesToRenderHasOutline(MinecraftClient client, Entity entity) {
        return ESP.shouldOutline(entity) || client.hasOutline(entity);
    }

    @Redirect(
            method = "renderEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I")
    )
    private int renderEntitiesOutlineColor(Entity entity) {
        return ESP.getOutlineColor(entity);
    }
}
