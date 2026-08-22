package laoqi123.events;

import laoqi123.event.events.callables.EventCancellable;
import laoqi123.event.types.EventType;
import net.minecraft.network.packet.Packet;

public class PacketEvent extends EventCancellable {
    private final EventType type;
    private final Packet<?> packet;

    public PacketEvent(EventType type, Packet<?> packet) {
        this.type = type;
        this.packet = packet;
    }

    public EventType getType() {
        return this.type;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }
}
