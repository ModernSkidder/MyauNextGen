package laoqi123.init;

import laoqi123.Myau;
import laoqi123.event.EventManager;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.KeyEvent;
import laoqi123.event.impl.TickEvent;
import laoqi123.module.Module;
import laoqi123.util.KeyBindUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.HashSet;
import java.util.Set;

public class MyauClient implements ClientModInitializer {
    private final Set<Integer> pressedKeys = new HashSet<>();

    @Override
    public void onInitializeClient() {
        new Myau();

        if (Boolean.getBoolean("laoqi123.debuggui")) {
            new DebugGuiHook();
        }

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
