package laoqi123.event.impl;

public class KeyEvent implements Event {
    private final int keyCode;

    public KeyEvent(int key) {
        this.keyCode = key;
    }

    public int getKey() {
        return this.keyCode;
    }
}
