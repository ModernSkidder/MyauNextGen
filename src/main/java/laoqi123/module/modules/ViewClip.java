package laoqi123.module.modules;

import laoqi123.module.Module;
import net.minecraft.client.MinecraftClient;

public class ViewClip extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public ViewClip() {
        super("ViewClip", false);
    }

    @Override
    public void onEnabled() {
        if (mc.world != null) {
            mc.worldRenderer.reload();
        }
    }

    @Override
    public void onDisabled() {
        if (mc.world != null) {
            mc.worldRenderer.reload();
        }
    }
}
