package laoqi123.util.clicking;

import java.util.Random;

public interface ClickPattern {
    void fill(int[] clickArray, int minCps, int maxCps, Random rng);
}
