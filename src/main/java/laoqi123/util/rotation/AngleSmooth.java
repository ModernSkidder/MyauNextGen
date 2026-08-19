package laoqi123.util.rotation;

import laoqi123.util.config.Choice;

public abstract class AngleSmooth extends Choice implements RotationProcessor {
    public AngleSmooth(String name) {
        super(name);
    }

    public abstract int calculateTicks(Rotation currentRotation, Rotation targetRotation);
}
