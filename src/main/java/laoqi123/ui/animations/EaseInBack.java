package laoqi123.ui.animations;

public class EaseInBack extends Animation {
    public EaseInBack(int duration, float start, float end, boolean reverse) {
        super(duration, start, end, reverse);
    }

    @Override
    protected float animate(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return (float) (c3 * x * x * x - c1 * x * x);
    }
}
