package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.module.modules.misc.AntiObfuscate;
import laoqi123.module.modules.misc.NickHider;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = TextRenderer.class, priority = 9999)
public abstract class MixinFontRenderer {
    @ModifyVariable(
            method = "draw(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private String renderString(String string) {
        if (Myau.moduleManager == null) {
            return string;
        }
        AntiObfuscate antiObfuscate = (AntiObfuscate) Myau.moduleManager.modules.get(AntiObfuscate.class);
        if (antiObfuscate.isEnabled()) {
            string = antiObfuscate.stripObfuscated(string);
        }
        NickHider nickHider = (NickHider) Myau.moduleManager.modules.get(NickHider.class);
        if (nickHider.isEnabled()) {
            string = nickHider.replaceNick(string);
        }
        return string;
    }

    @ModifyVariable(
            method = "draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private Text drawText(Text text) {
        if (Myau.moduleManager == null) {
            return text;
        }
        AntiObfuscate antiObfuscate = (AntiObfuscate) Myau.moduleManager.modules.get(AntiObfuscate.class);
        String string = text.getString();
        if (antiObfuscate.isEnabled()) {
            string = antiObfuscate.stripObfuscated(string);
        }
        NickHider nickHider = (NickHider) Myau.moduleManager.modules.get(NickHider.class);
        if (nickHider.isEnabled()) {
            string = nickHider.replaceNick(string);
        }
        return string.equals(text.getString()) ? text : Text.literal(string).setStyle(text.getStyle());
    }

    @ModifyVariable(
            method = "getWidth(Lnet/minecraft/text/StringVisitable;)I",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private StringVisitable getWidth(StringVisitable stringVisitable) {
        if (Myau.moduleManager == null) {
            return stringVisitable;
        }
        AntiObfuscate antiObfuscate = (AntiObfuscate) Myau.moduleManager.modules.get(AntiObfuscate.class);
        String string = stringVisitable.getString();
        if (antiObfuscate.isEnabled()) {
            string = antiObfuscate.stripObfuscated(string);
        }
        NickHider nickHider = (NickHider) Myau.moduleManager.modules.get(NickHider.class);
        if (nickHider.isEnabled()) {
            string = nickHider.replaceNick(string);
        }
        return string.equals(stringVisitable.getString()) ? stringVisitable : StringVisitable.plain(string);
    }

    @ModifyVariable(
            method = "getWidth(Ljava/lang/String;)I",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private String getStringWidth(String string) {
        if (Myau.moduleManager == null) {
            return string;
        }
        AntiObfuscate antiObfuscate = (AntiObfuscate) Myau.moduleManager.modules.get(AntiObfuscate.class);
        if (antiObfuscate.isEnabled()) {
            string = antiObfuscate.stripObfuscated(string);
        }
        NickHider nickHider = (NickHider) Myau.moduleManager.modules.get(NickHider.class);
        return nickHider.isEnabled() ? nickHider.replaceNick(string) : string;
    }
}
