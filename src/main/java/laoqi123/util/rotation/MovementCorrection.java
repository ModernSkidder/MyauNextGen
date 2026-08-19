package laoqi123.util.rotation;

public enum MovementCorrection {
    OFF("Off"),
    STRICT("Strict"),
    SILENT("Silent"),
    CHANGE_LOOK("ChangeLook");

    private final String choiceName;

    MovementCorrection(String choiceName) {
        this.choiceName = choiceName;
    }

    public String getChoiceName() {
        return this.choiceName;
    }
}
