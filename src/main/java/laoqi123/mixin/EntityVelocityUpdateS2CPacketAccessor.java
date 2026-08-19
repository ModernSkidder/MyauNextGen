package laoqi123.mixin;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityVelocityUpdateS2CPacket.class)
public interface EntityVelocityUpdateS2CPacketAccessor {
    @Accessor("velocityX")
    int getMotionX();

    @Accessor("velocityX")
    void setMotionX(int motionX);

    @Accessor("velocityY")
    int getMotionY();

    @Accessor("velocityY")
    void setMotionY(int motionY);

    @Accessor("velocityZ")
    int getMotionZ();

    @Accessor("velocityZ")
    void setMotionZ(int motionZ);
}
