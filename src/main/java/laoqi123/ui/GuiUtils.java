package laoqi123.ui;

public class GuiUtils {
    private static long lastFrameNanos = System.nanoTime();
    private static float deltaTime = 16f;

    private GuiUtils() {
    }

    public static void updateDeltaTime() {
        long now = System.nanoTime();
        float dt = (now - lastFrameNanos) / 1_000_000f;
        lastFrameNanos = now;
        deltaTime = Math.max(0.1f, Math.min(dt, 50f));
    }

    public static float getDeltaTime() {
        return deltaTime;
    }

    public static float map(float value, float min, float max, float newMin, float newMax) {
        return newMin + (value - min) / (max - min) * (newMax - newMin);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float mapIncrement(float value, float min, float max, float newMin, float newMax, float increment) {
        float mapped = map(value, min, max, newMin, newMax);
        if (increment > 0) {
            mapped = Math.round(mapped / increment) * increment;
        }
        return mapped;
    }
}
