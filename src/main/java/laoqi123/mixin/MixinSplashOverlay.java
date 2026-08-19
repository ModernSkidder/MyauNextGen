package laoqi123.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ReloadableTexture;
import net.minecraft.client.texture.TextureContents;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public abstract class MixinSplashOverlay {
    private static final Identifier MYAU_LOADING = Identifier.of("myaunextgen", "textures/gui/loading.png");
    private static final int LOADING_WIDTH = 1920;
    private static final int LOADING_HEIGHT = 1080;

    private static InputStream loadingLogoStream() throws IOException {
        InputStream inputStream = MixinSplashOverlay.class.getClassLoader().getResourceAsStream("assets/myaunextgen/textures/gui/loading.png");
        if (inputStream == null) {
            throw new IOException("assets/myaunextgen/textures/gui/loading.png not found");
        }
        return inputStream;
    }

    @Inject(method = "init", at = @At("HEAD"))
    private static void myauInit(TextureManager textureManager, CallbackInfo ci) {
        textureManager.registerTexture(MYAU_LOADING, new ReloadableTexture(MYAU_LOADING) {
            @Override
            public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
                try (InputStream inputStream = loadingLogoStream()) {
                    return new TextureContents(NativeImage.read(inputStream), new TextureResourceMetadata(true, true));
                }
            }
        });
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void myauDrawLoading(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int i = context.getScaledWindowWidth();
        int j = context.getScaledWindowHeight();
        context.drawTexture(
            RenderLayer::getGuiTextured,
            MYAU_LOADING,
            0,
            0,
            0.0F,
            0.0F,
            i,
            j,
            LOADING_WIDTH,
            LOADING_HEIGHT,
            LOADING_WIDTH,
            LOADING_HEIGHT
        );
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_clear(I)V"))
    private static void myauNoClear(int i) {
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(Lnet/minecraft/client/render/RenderLayer;IIIII)V"))
    private void myauNoBackgroundFill(DrawContext context, RenderLayer renderLayer, int x1, int y1, int x2, int y2, int color) {
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIIIIII)V"))
    private void myauNoMojangLogo(DrawContext context, java.util.function.Function<Identifier, RenderLayer> renderLayers, Identifier sprite, int x, int y, float u, float v, int width, int height, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
    }
}
