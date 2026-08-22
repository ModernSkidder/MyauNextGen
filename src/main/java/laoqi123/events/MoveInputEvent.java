package laoqi123.events;

import laoqi123.event.events.Event;

public class MoveInputEvent implements Event {
    private boolean jump;
    private boolean jumpModified;
    private float forward;
    private boolean forwardModified;
    private float strafe;
    private boolean strafeModified;

    public boolean getJump() {
        return this.jump;
    }

    public void setJump(boolean jump) {
        this.jump = jump;
        this.jumpModified = true;
    }

    public boolean isJumpModified() {
        return this.jumpModified;
    }

    public float getForward() {
        return this.forward;
    }

    public void setForward(float forward) {
        this.forward = forward;
        this.forwardModified = true;
    }

    public boolean isForwardModified() {
        return this.forwardModified;
    }

    public float getStrafe() {
        return this.strafe;
    }

    public void setStrafe(float strafe) {
        this.strafe = strafe;
        this.strafeModified = true;
    }

    public boolean isStrafeModified() {
        return this.strafeModified;
    }
}