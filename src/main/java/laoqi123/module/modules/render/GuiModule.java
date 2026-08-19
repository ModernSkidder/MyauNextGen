package laoqi123.module.modules.render;

import laoqi123.module.Module;
import laoqi123.ui.ClickGui;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class GuiModule extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public GuiModule() {
        super("ClickGui", false);
        setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnabled() {
        setEnabled(false);
        mc.setScreen(new ClickGui());
    }
}
