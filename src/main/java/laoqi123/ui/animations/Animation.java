package laoqi123.ui.animations;

import laoqi123.ui.GuiUtils;

public abstract class Animation {
    protected final boolean reverse;
    protected final float duration;
    protected final float start;
    protected final float change;
    protected float timePassed = 0;

    public Animation(float duration, float start, float end, boolean reverse) {
        this.duration = duration;
        if (reverse) {
            float temp = start;
            start = end;
            end = temp;
        }
        this.start = start;
        this.change = end - start;
        this.reverse = reverse;
    }

    public float get(float deltaTime) {
        timePassed += deltaTime;
        if (timePassed >= duration) return start + change;
        return animate(timePassed / duration) * change + start;
    }

    public float get() {
        return get(GuiUtils.getDeltaTime());
    }

    public boolean isFinished() {
        return timePassed >= duration;
    }

    public boolean isReversed() {
        return reverse;
    }

    public float getStart() {
        return start;
    }

    public float getEnd() {
        return start + change;
    }

    protected abstract float animate(float x);
}
