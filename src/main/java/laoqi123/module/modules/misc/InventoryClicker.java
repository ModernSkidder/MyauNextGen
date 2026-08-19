package laoqi123.module.modules.misc;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.TickEvent;
import laoqi123.module.Module;
import laoqi123.value.properties.IntValue;
import laoqi123.util.KeyBindUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

public class InventoryClicker extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final IntValue triggerTicks = new IntValue("ticks", 2, 0, 20);
    public int ticks;

    public InventoryClicker() {
        super("InventoryClicker", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{triggerTicks.getValue().toString() + " ticks"};
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && mc.player != null && event.getType() == EventType.PRE) {
            if (mc.currentScreen instanceof HandledScreen) {
                HandledScreen<?> screen = ((HandledScreen<?>) mc.currentScreen);
                final int mouseX = (int) (mc.mouse.getX() * screen.width / mc.getWindow().getFramebufferWidth());
                final int mouseY = (int) (screen.height - mc.mouse.getY() * screen.height / mc.getWindow().getFramebufferHeight() - 1);
                if (KeyBindUtil.isKeyDown(mc.options.attackKey)) {
                    ticks++;
                    if(ticks > triggerTicks.getValue())
                    {
                        screen.mouseClicked(mouseX, mouseY, 0);
                    }
                }else {
                    ticks = 0;
                }
            }
        }
    }
}
