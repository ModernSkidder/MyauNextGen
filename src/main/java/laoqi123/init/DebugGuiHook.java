package laoqi123.init;

import laoqi123.Myau;
import laoqi123.event.EventManager;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.TickEvent;
import laoqi123.ui.ClickGui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;

import java.io.File;

public class DebugGuiHook {
    private int ticks = 0;
    private boolean opened = false;
    private int shots = 0;

    public DebugGuiHook() {
        EventManager.register(this);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        ticks++;
        if (!opened && ticks > 100) {
            opened = true;
            mc.setScreen(new ClickGui());
        }
        if (opened && ticks > 120 && shots < 3) {
            shots++;
            try {
                NativeImage image = ScreenshotRecorder.takeScreenshot(mc.getFramebuffer());
                image.writeTo(new File("C:\\Users\\37672\\AppData\\Local\\Temp\\opencode\\clickgui-" + shots + ".png"));
                laoqi123.font.GlyphCache.debugLast.debugDumpGpuTexture("C:\\Users\\37672\\AppData\\Local\\Temp\\opencode\\gpu-" + shots + ".png");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (opened && ticks > 300) {
            EventManager.unregister(this);
            mc.setScreen(null);
        }
    }
}
