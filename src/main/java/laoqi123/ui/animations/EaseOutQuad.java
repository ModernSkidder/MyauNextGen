package laoqi123.ui.animations;

public class EaseOutQuad extends Animation {
    public EaseOutQuad(int duration, float start, float end, boolean reverse) {
        super(duration, start, end, reverse);
    }

    @Override
    protected float animate(float x) {
        return 1 - (1 - x) * (1 - x);
    }
}
