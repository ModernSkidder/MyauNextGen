package laoqi123.module.modules.combat.killaura;

import laoqi123.value.properties.ModeValue;
import laoqi123.util.config.Configurable;

public class KillAuraTargetSelect extends Configurable {
    public final ModeValue mode;
    public final ModeValue sort;

    public KillAuraTargetSelect() {
        super("TargetSelect");
        this.mode = this.register(new ModeValue("Mode", 0, new String[]{"Single", "Switch"}));
        this.sort = this.register(new ModeValue("Sort", 0, new String[]{"Distance", "Health", "Hurt Time", "FOV"}));
    }
}
