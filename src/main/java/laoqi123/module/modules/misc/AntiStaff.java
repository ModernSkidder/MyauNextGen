package laoqi123.module.modules.misc;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.PacketEvent;
import laoqi123.module.Module;
import laoqi123.util.ChatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class AntiStaff extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final String STAFF_LIST_B64 = "QuermeaQnOaXoOmHj+Wfn+mbqizkuInlm73mnYAs56yZ5qmZLE1lbmdDaGVuMzg4NCxBbmRyZXdrcmlzdCxGaWE5LOaeq+iQp+ael+eEtiznu7/osYbkuYPjgZXjgpMs5oqW6Z+z5Li25bCP5YyqLOaKlumfs19hd2Hpqazljp8sTW5hbUxlb18s5Lit5LqM5bCR5bm0REws5p6V5LiK5Lmm5Li25aGR5pyb5pyILElhbU1vbGluY2VuXywsQ29GdV9fLOaWl+aImOiDnOS9myzlj6rnjqnmlqXlgJks5p6V5LiK5Lmm5Li26Zuq5aScLGFpeXVraSxDYW5keUFwb3N0bGUsY2h1bnlpMSzmtYHlvbHlj6rkvJrlmKTlmKTlmKQscXRlc2RmXzY3NCxxeHRtbGM5OSxTa3lmb3ks56We5Z2R5LmL6YCXLOWco+S4iuiNo+iAgDIzMyzlsI/lhpvlkJvkuLblpKnkvb/kuYvnv7ws5p6V5LiK5Lmm5Li25YKy5a+SLF93aW5uZXJfLFNreV9ZdWFueGlhbw==";

    private static final List<String> STAFF_LIST = new ArrayList<>();

    static {
        String decoded = new String(Base64.getDecoder().decode(STAFF_LIST_B64), StandardCharsets.UTF_8);
        for (String name : decoded.split(",")) {
            STAFF_LIST.add(name);
        }
    }

    public AntiStaff() {
        super("AntiStaff", false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.RECEIVE) return;
        if (event.getPacket() instanceof PlayerListS2CPacket packet) {
            if (!packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) return;
            for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
                if (entry.profile() != null) {
                    String name = entry.profile().getName();
                    if (name != null && !name.isEmpty() && STAFF_LIST.contains(name)) {
                        this.exitGame();
                        return;
                    }
                }
                if (entry.displayName() != null) {
                    String display = entry.displayName().getString();
                    if (!display.isEmpty() && STAFF_LIST.contains(display)) {
                        this.exitGame();
                        return;
                    }
                }
            }
        } else if (event.getPacket() instanceof EntitySpawnS2CPacket packet) {
            if (packet.getUuid() == null || mc.world == null) return;
            if (packet.getEntityType() != EntityType.PLAYER) return;
            net.minecraft.entity.Entity entity = mc.world.getEntityById(packet.getEntityId());
            if (entity != null) {
                String name = entity.getName().getString();
                if (!name.isEmpty() && STAFF_LIST.contains(name)) {
                    this.exitGame();
                }
            }
        }
    }

    private void exitGame() {
        ChatUtil.sendFormatted("&cStaff detected!");
        if (mc.player != null && mc.player.networkHandler != null) {
            mc.player.networkHandler.sendCommand("hub");
        }
    }
}
