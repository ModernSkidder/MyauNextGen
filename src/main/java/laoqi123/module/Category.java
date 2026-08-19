package laoqi123.module;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player"),
    MISC("Misc");

    private final String name;

    Category(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static Category fromPackage(String packageName) {
        if (packageName != null) {
            if (packageName.contains(".combat")) return COMBAT;
            if (packageName.contains(".movement")) return MOVEMENT;
            if (packageName.contains(".render")) return RENDER;
            if (packageName.contains(".player")) return PLAYER;
            if (packageName.contains(".misc")) return MISC;
        }
        return MISC;
    }
}
