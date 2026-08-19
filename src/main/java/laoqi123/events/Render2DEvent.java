package laoqi123.events;

import laoqi123.event.events.Event;
import net.minecraft.client.gui.DrawContext;

public class Render2DEvent implements Event {
    private final DrawContext context;
    private final float partialTicks;

    public Render2DEvent(DrawContext context, float partialTicks) {
        this.context = context;
        this.partialTicks = partialTicks;
    }

    public DrawContext getContext() {
        return this.context;
    }

    public float getPartialTicks() {
        return this.partialTicks;
    }
}
