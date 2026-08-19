package laoqi123.module.modules;

import laoqi123.event.EventTarget;
import laoqi123.events.PacketEvent;
import laoqi123.module.Module;
import laoqi123.util.ChatUtil;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class FlagDetector extends Module {
    public FlagDetector() {
        super("FlagDetector", false, true);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()) return;

        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            ChatUtil.sendFormatted("&7[&cFlagDetector&7] &fServer flag detected (Lagback)!");
        }
    }
}