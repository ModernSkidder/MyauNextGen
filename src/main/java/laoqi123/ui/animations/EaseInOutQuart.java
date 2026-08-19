package laoqi123.ui.animations;

public class EaseInOutQuart extends Animation {
    public EaseInOutQuart(int duration, float start, float end, boolean reverse) {
        super(duration, start, end, reverse);
    }

    @Override
    protected float animate(float x) {
        return x < 0.5 ? 8 * x * x * x * x : (float) (1 - Math.pow(-2 * x + 2, 4) / 2);
    }
}
