package laoqi123.util.rotation;

public interface RotationProcessor {
    Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation);
}
