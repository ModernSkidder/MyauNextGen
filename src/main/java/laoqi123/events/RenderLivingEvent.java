package laoqi123.events;

import laoqi123.event.events.Event;
import laoqi123.event.types.EventType;
import net.minecraft.entity.LivingEntity;

public class RenderLivingEvent implements Event {
    private final EventType type;
    private final LivingEntity entity;

    public RenderLivingEvent(EventType type, LivingEntity entity) {
        this.type = type;
        this.entity = entity;
    }

    public EventType getType() {
        return this.type;
    }

    public LivingEntity getEntity() {
        return this.entity;
    }
}
