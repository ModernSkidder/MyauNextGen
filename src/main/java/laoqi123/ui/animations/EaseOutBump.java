package laoqi123.ui.animations;

public class EaseOutBump extends Animation {
    private static final double CONSTANT_1 = 1.7;
    private static final double CONSTANT_2 = 2.7;

    public EaseOutBump(int duration, float start, float end, boolean reverse) {
        super(duration, start, end, reverse);
    }

    @Override
    protected float animate(float x) {
        return (float) (1 + CONSTANT_2 * Math.pow(x - 1, 3) + CONSTANT_1 * 1.2 * Math.pow(x - 1, 2));
    }
}
