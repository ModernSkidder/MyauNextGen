package laoqi123.init;

import laoqi123.Myau;
import laoqi123.event.EventManager;
import laoqi123.event.types.EventType;
import laoqi123.events.KeyEvent;
import laoqi123.events.TickEvent;
import laoqi123.module.Module;
import laoqi123.oneconfig.MyauOneConfig;
import laoqi123.util.KeyBindUtil;
import laoqi123.web.SplashLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public class MyauClient implements ClientModInitializer {
    private final Set<Integer> pressedKeys = new HashSet<>();

    @Override
    public void onInitializeClient() {
        // Standalone loading window: hide the game window and show the splash while
        // the engine (mods, MCEF/Chromium) initializes. The game window is shown
        // again on the first rendered frame via MixinMinecraft.
        //
        // Guard: AWT/Swing cannot start in a headless environment (gradle
        // runClient forces java.awt.headless=true). In that case we do NOT hide
        // the game window and do NOT show the splash - otherwise the user would
        // see nothing but a black screen.
        boolean splashAvailable = !java.awt.GraphicsEnvironment.isHeadless();
        if (splashAvailable) {
            MinecraftClient mcc = MinecraftClient.getInstance();
            Window win = mcc.getWindow();
            try {
                GLFW.glfwHideWindow(win.getHandle());
            } catch (Throwable ignored) {
            }
            SplashLoader.start("MYAU  NEXTGEN");
            SplashLoader.setText("Loading modules...");
            SplashLoader.setProgress(10);
        }

        new Myau();

        // Publish every module and setting into the OneConfig UI. Must run after
        // new Myau() so the module and property managers are populated.
        if (splashAvailable) {
            SplashLoader.setText("Building settings menu...");
            SplashLoader.setProgress(60);
        }
        MyauOneConfig.init();

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) return;
            EventManager.call(new TickEvent(EventType.PRE));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null || mc.player == null) return;

            EventManager.call(new TickEvent(EventType.POST));
            if (mc.currentScreen != null) return;

            Set<Integer> currentlyPressed = new HashSet<>();
            for (Module module : Myau.moduleManager.modules.values()) {
                int key = module.getKey();
                if (key == 0 || currentlyPressed.contains(key)) continue;
                if (KeyBindUtil.isKeyDown(key)) {
                    currentlyPressed.add(key);
                    if (!pressedKeys.contains(key)) {
                        EventManager.call(new KeyEvent(key));
                    }
                }
            }
            pressedKeys.retainAll(currentlyPressed);
            for (int key : currentlyPressed) pressedKeys.add(key);
        });
    }
}
