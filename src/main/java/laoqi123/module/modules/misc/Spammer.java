package laoqi123.module.modules.misc;

import laoqi123.event.EventTarget;
import laoqi123.event.impl.Render2DEvent;
import laoqi123.module.Module;
import laoqi123.util.TimerUtil;
import laoqi123.value.properties.FloatValue;
import laoqi123.value.properties.IntValue;
import laoqi123.value.properties.TextValue;
import net.minecraft.client.MinecraftClient;

public class Spammer extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final TimerUtil timer = new TimerUtil();
    private int charOffset = 19968;
    public final TextValue text = new TextValue("text", "meow");
    public final FloatValue delay = new FloatValue("delay", 3.5F, 0.0F, 3600.0F);
    public final IntValue random = new IntValue("random", 0, 0, 10);

    public Spammer() {
        super("Spammer", false);
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled()) {
            if (this.timer.hasTimeElapsed((long) (this.delay.getValue() * 1000.0F))) {
                this.timer.reset();
                String text = this.text.getValue();
                if (this.random.getValue() > 0) {
                    text = String.format("%s ", text);
                    for (int i = 0; i < this.random.getValue(); i++) {
                        text = String.format("%s%s", text, (char) this.charOffset);
                        this.charOffset++;
                        if (this.charOffset > 40959) {
                            this.charOffset = 19968;
                        }
                    }
                }
                mc.player.networkHandler.sendChatMessage(text);
            }
        }
    }
}
