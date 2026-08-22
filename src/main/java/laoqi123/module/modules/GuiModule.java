package laoqi123.module.modules;

import laoqi123.module.Module;
import laoqi123.oneconfig.OneConfigScreens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;
import org.polyfrost.oneconfig.api.ui.v1.OneConfigUI;

public class GuiModule extends Module {

    public GuiModule() {
        super("ClickGui", false);
        setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnabled() {
        setEnabled(false);
        // Open straight onto Myau's settings page, skipping OneConfig's mod grid.
        Screen screen = OneConfigScreens.myauSettingsScreen();
        if (screen != null) {
            MinecraftClient.getInstance().setScreen(screen);
        } else {
            // OneConfig's internals moved; fall back to whatever page it picks. open()
            // only logs a warning when the UI is unavailable, so this never throws.
            OneConfigUI.open();
        }
    }
}
