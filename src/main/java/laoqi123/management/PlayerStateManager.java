package laoqi123.management;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class PlayerStateManager {
    public boolean attacking = false;
    public boolean digging = false;
    public boolean placing = false;
    public boolean swapping = false;
    public boolean swinging = false;

    public void handlePacket(Packet<?> packet) {
        if (packet instanceof PlayerInteractEntityC2SPacket) {
            this.attacking = true;
        }
        if (packet instanceof PlayerActionC2SPacket) {
            this.digging = true;
        }
        if (packet instanceof PlayerInteractBlockC2SPacket) {
            this.placing = true;
        }
        if (packet instanceof UpdateSelectedSlotC2SPacket) {
            this.swapping = true;
        }
        if (packet instanceof HandSwingC2SPacket) {
            this.swinging = true;
        }
        if (packet instanceof PlayerMoveC2SPacket) {
            this.attacking = false;
            this.digging = false;
            this.placing = false;
            this.swapping = false;
            this.swinging = false;
        }
    }
}
