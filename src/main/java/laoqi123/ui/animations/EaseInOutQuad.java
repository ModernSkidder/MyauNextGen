package laoqi123.ui.animations;

public class EaseInOutQuad extends Animation {
    public EaseInOutQuad(int duration, float start, float end, boolean reverse) {
        super(duration, start, end, reverse);
    }

    @Override
    protected float animate(float x) {
        return x < 0.5 ? 2 * x * x : (float) (1 - Math.pow(-2 * x + 2, 2) / 2);
    }
}
