package laoqi123.events;

import laoqi123.event.events.Event;

public class RaytraceEvent implements Event {
    private double range;

    public RaytraceEvent(double range) {
        this.range = range;
    }

    public double getRange() {
        return this.range;
    }

    public void setRange(double range) {
        this.range = range;
    }
}
