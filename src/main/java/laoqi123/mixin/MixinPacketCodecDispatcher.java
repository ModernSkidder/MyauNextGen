package laoqi123.mixin;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.network.handler.PacketCodecDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(PacketCodecDispatcher.class)
public abstract class MixinPacketCodecDispatcher {
    @Shadow
    @Final
    private Function packetIdGetter;

    @Shadow
    @Final
    private Object2IntMap typeToIndex;

    @Inject(
            method = "encode(Lio/netty/buffer/ByteBuf;Ljava/lang/Object;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void dropUnknownPacket(ByteBuf buf, Object packet, CallbackInfo callbackInfo) {
        Object packetType = this.packetIdGetter.apply(packet);
        if (packetType != null && !this.typeToIndex.containsKey(packetType)) {
            callbackInfo.cancel();
        }
    }
}
