package laoqi123.ui.animations;

import java.util.concurrent.Callable;

public class DummyAnimation extends Animation {
    protected final float value;
    protected Callable<Boolean> done = null;

    public DummyAnimation(float value, Callable<Boolean> done) {
        super(0, value, value, false);
        this.value = value;
        this.done = done;
    }

    public DummyAnimation(float value, float duration) {
        super(duration, value, value, false);
        this.value = value;
    }

    public DummyAnimation(float value) {
        this(value, 0);
    }

    @Override
    public float get(float deltaTime) {
        timePassed += deltaTime;
        return value;
    }

    @Override
    public boolean isFinished() {
        if (done != null) {
            try {
                return done.call();
            } catch (Exception ignored) {
            }
        }
        return super.isFinished();
    }

    @Override
    protected float animate(float x) {
        return x;
    }
}
