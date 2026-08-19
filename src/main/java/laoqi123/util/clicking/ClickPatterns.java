package laoqi123.util.clicking;

import java.util.Random;

public final class ClickPatterns {

    public static final ClickPattern STABILIZED = (clickArray, minCps, maxCps, rng) -> {
        int clicks = randomClicks(minCps, maxCps, rng);
        int interval = clicks > 0 ? clickArray.length / clicks : 0;
        int remainder = clicks > 0 ? clickArray.length % clicks : 0;
        int currentIndex = 0;
        for (int i = 0; i < clicks; i++) {
            clickArray[currentIndex % clickArray.length]++;
            currentIndex += Math.max(interval, 1);
            if (remainder > 0) {
                currentIndex++;
                remainder--;
            }
        }
    };

    public static final ClickPattern EFFICIENT = (clickArray, minCps, maxCps, rng) -> {
        int clicks = randomClicks(minCps, maxCps, rng);
        if (clicks < 10) {
            STABILIZED.fill(clickArray, minCps, maxCps, rng);
            return;
        }
        for (int i = 0; i < clicks; i++) {
            clickArray[i * 2 % clickArray.length]++;
        }
    };

    public static final ClickPattern SPAMMING = (clickArray, minCps, maxCps, rng) -> {
        int clicks = randomClicks(minCps, maxCps, rng);
        for (int i = 0; i < clicks; i++) {
            clickArray[rng.nextInt(clickArray.length)]++;
        }
    };

    public static final ClickPattern DOUBLE_CLICK = (clickArray, minCps, maxCps, rng) -> {
        int clicks = randomClicks(minCps, maxCps, rng);
        for (int i = 0; i < clicks; i++) {
            clickArray[rng.nextInt(clickArray.length)] += 2;
        }
    };

    public static final ClickPattern DRAG = (clickArray, minCps, maxCps, rng) -> {
        int clicks = randomClicks(minCps, maxCps, rng);
        int travelTime = 17 + rng.nextInt(2);
        while (sum(clickArray) < clicks) {
            int index = 0;
            for (int i = 1; i < travelTime; i++) {
                if (clickArray[i] < clickArray[index]) {
                    index = i;
                }
            }
            clickArray[index]++;
        }
    };

    public static final ClickPattern BUTTERFLY = (clickArray, minCps, maxCps, rng) -> {
        int clicks = randomClicks(minCps, maxCps, rng);
        while (sum(clickArray) < clicks) {
            int zeroCount = 0;
            for (int value : clickArray) {
                if (value == 0) {
                    zeroCount++;
                }
            }
            if (zeroCount > 0) {
                int index = -1;
                while (index == -1 || clickArray[index] != 0) {
                    index = rng.nextInt(clickArray.length);
                }
                clickArray[index] = 1 + rng.nextInt(2);
            } else {
                clickArray[rng.nextInt(clickArray.length)]++;
            }
        }
    };

    public static final ClickPattern NORMAL_DISTRIBUTION = (clickArray, minCps, maxCps, rng) -> {
        double t = 0.0;
        while (true) {
            double v = rng.nextDouble();
            double mean;
            double std;
            if (v >= 10.0 / 110.0) {
                mean = 179.5242718446602;
                std = 20.416937885616676;
            } else {
                mean = 87.88;
                std = 13.420088130563776;
            }
            t += (mean + rng.nextGaussian() * std) * 20.0 / 1000.0;
            if (t > 20.0) {
                break;
            }
            clickArray[(int) t]++;
        }
    };

    private ClickPatterns() {
    }

    private static int randomClicks(int minCps, int maxCps, Random rng) {
        int min = Math.min(minCps, maxCps);
        int max = Math.max(minCps, maxCps);
        return min == max ? min : min + rng.nextInt(max - min + 1);
    }

    private static int sum(int[] clickArray) {
        int sum = 0;
        for (int value : clickArray) {
            sum += value;
        }
        return sum;
    }
}
