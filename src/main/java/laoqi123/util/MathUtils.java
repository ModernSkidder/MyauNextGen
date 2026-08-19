package laoqi123.util;

public class MathUtils {
    public static double getRandomInRange(double min, double max) {
        if (min > max) {
            double t = min;
            min = max;
            max = t;
        }
        return min + Math.random() * (max - min);
    }
}