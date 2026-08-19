package laoqi123.ui.animations;

public class EaseOutExpo extends Animation {
    public EaseOutExpo(int duration, float start, float end, boolean reverse) {
        super(duration, start, end, reverse);
    }

    @Override
    protected float animate(float x) {
        return x == 1 ? 1 : 1 - (float) Math.pow(2, -10 * x);
    }
}
