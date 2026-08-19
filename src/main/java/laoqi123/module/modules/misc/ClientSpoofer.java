package laoqi123.module.modules.misc;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.PacketEvent;
import laoqi123.module.Module;
import laoqi123.value.properties.ModeValue;
import laoqi123.value.properties.TextValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;

public class ClientSpoofer extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final String CUSTOM_MODE = "Custom";
    private static final String[] MODES = new String[]{
            "Vanilla", "OptiFine", "Fabric", "Feather", "LunarClient",
            "LabyMod", "CheatBreaker", "PvPLounge", "Minebuilders", "FML",
            "Geyser", "Log4j", "FDP", "OpenMyau", CUSTOM_MODE
    };
    private static final String[] BRAND_VALUES = new String[]{
            "vanilla", "optifine", "fabric", "Feather Forge", "lunarclient",
            "LMC", "CB", "PLC18", "minebuilders", "fml,forge",
            "Geyser", "${jndi:ldap://127.0.0.1/a}", "FDPClient", "OpenMyau+", ""
    };
    private boolean spoofing = false;

    public final ModeValue mode = new ModeValue("mode", 0, MODES);
    public final TextValue customBrand = new TextValue("custom-brand", "OpenMyau+", this::isCustomMode);

    public ClientSpoofer() {
        super("ClientSpoofer", false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!isEnabled() || event.getType() != EventType.SEND) return;
        if (!(event.getPacket() instanceof CustomPayloadC2SPacket)) return;

        CustomPayloadC2SPacket packet = (CustomPayloadC2SPacket) event.getPacket();
        if (packet.payload() instanceof BrandCustomPayload) {
            if (this.spoofing) {
                this.spoofing = false;
                return;
            }
            event.setCancelled(true);
            this.spoofing = true;
            mc.getNetworkHandler().sendPacket(new CustomPayloadC2SPacket(new BrandCustomPayload(getBrand())));
        }
    }

    private String getBrand() {
        if (isCustomMode()) {
            return customBrand.getValue();
        }
        int index = mode.getValue();
        return index >= 0 && index < BRAND_VALUES.length ? BRAND_VALUES[index] : BRAND_VALUES[0];
    }

    private boolean isCustomMode() {
        return CUSTOM_MODE.equalsIgnoreCase(mode.getModeString());
    }

    @Override
    public String[] getSuffix() {
        return new String[]{mode.getModeString()};
    }
}
