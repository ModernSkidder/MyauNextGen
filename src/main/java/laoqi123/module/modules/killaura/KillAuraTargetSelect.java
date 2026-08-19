package laoqi123.module.modules.killaura;

import laoqi123.property.properties.ModeProperty;
import laoqi123.util.config.Configurable;

public class KillAuraTargetSelect extends Configurable {
    public final ModeProperty mode;
    public final ModeProperty sort;

    public KillAuraTargetSelect() {
        super("TargetSelect");
        this.mode = this.register(new ModeProperty("Mode", 0, new String[]{"Single", "Switch"}));
        this.sort = this.register(new ModeProperty("Sort", 0, new String[]{"Distance", "Health", "Hurt Time", "FOV"}));
    }
}
