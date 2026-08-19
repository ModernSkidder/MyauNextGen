package laoqi123.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen {
    private static final Identifier MYAU_BACKGROUND = Identifier.of("myaunextgen", "textures/gui/background.png");
    private static final int BACKGROUND_WIDTH = 1920;
    private static final int BACKGROUND_HEIGHT = 1080;

    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true)
    private void myauRenderBackground(DrawContext context, float delta, CallbackInfo ci) {
        int i = context.getScaledWindowWidth();
        int j = context.getScaledWindowHeight();
        context.drawTexture(
            RenderLayer::getGuiTextured,
            MYAU_BACKGROUND,
            0,
            0,
            0.0F,
            0.0F,
            i,
            j,
            BACKGROUND_WIDTH,
            BACKGROUND_HEIGHT,
            BACKGROUND_WIDTH,
            BACKGROUND_HEIGHT
        );
        ci.cancel();
    }
}
