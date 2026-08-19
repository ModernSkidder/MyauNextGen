package laoqi123.events;

import laoqi123.event.events.Event;

public class SafeWalkEvent implements Event {
    private boolean safeWalk;

    public SafeWalkEvent(boolean safeWalk) {
        this.safeWalk = safeWalk;
    }

    public boolean isSafeWalk() {
        return this.safeWalk;
    }

    public void setSafeWalk(boolean safeWalk) {
        this.safeWalk = safeWalk;
    }
}
