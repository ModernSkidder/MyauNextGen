package laoqi123.render.type;

import org.joml.Vector2f;

public record Rect(float x1, float y1, float x2, float y2) {
    public Rect {
        if (x1 > x2 || y1 > y2) {
            throw new IllegalArgumentException("Invalid rect: (" + x1 + "," + y1 + "," + x2 + "," + y2 + ")");
        }
    }

    public float getCx() {
        return (x1 + x2) * 0.5F;
    }

    public float getCy() {
        return (y1 + y2) * 0.5F;
    }

    public float getW() {
        return x2 - x1;
    }

    public float getH() {
        return y2 - y1;
    }

    public Vector2f getCenter() {
        return new Vector2f(getCx(), getCy());
    }

    public boolean contains(float px, float py) {
        return px >= x1 && px <= x2 && py >= y1 && py <= y2;
    }

    public boolean intersects(Rect other) {
        return !(other.x1 > x2 || other.x2 < x1 || other.y1 > y2 || other.y2 < y1);
    }

    public static Rect of(float cx, float cy, float w, float h) {
        return new Rect(cx - w * 0.5F, cy - h * 0.5F, cx + w * 0.5F, cy + h * 0.5F);
    }
}
