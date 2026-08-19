package laoqi123.util.clicking;

public class RollingClickArray {

    private final int cycleLength;
    public final int iterations;
    private final int[] array;
    private int head = 0;

    public RollingClickArray(int cycleLength, int iterations) {
        this.cycleLength = cycleLength;
        this.iterations = iterations;
        this.array = new int[cycleLength * iterations];
    }

    public int get(int relativeIndex) {
        int actualIndex = (this.head + relativeIndex) % this.array.length;
        return this.array[actualIndex];
    }

    public void set(int relativeIndex, int value) {
        int actualIndex = (this.head + relativeIndex) % this.array.length;
        this.array[actualIndex] = value;
    }

    public boolean advance(int amount) {
        this.head = (this.head + amount) % this.array.length;
        return this.head % this.cycleLength == 0;
    }

    public boolean advance() {
        return this.advance(1);
    }

    public void clear() {
        java.util.Arrays.fill(this.array, 0);
        this.head = 0;
    }

    public void push(int[] cycleArray) {
        if (cycleArray.length != this.cycleLength) {
            throw new IllegalArgumentException("Array size must match cycle length");
        }
        if (this.head == 0) {
            System.arraycopy(cycleArray, 0, this.array, this.cycleLength, this.cycleLength);
        } else if (this.head == this.cycleLength) {
            System.arraycopy(cycleArray, 0, this.array, 0, this.cycleLength);
        } else {
            throw new IllegalStateException("Head must be at 0 or cycle length");
        }
    }
}
