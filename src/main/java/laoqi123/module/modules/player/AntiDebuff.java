package laoqi123.module.modules.player;

import laoqi123.module.Module;
import laoqi123.value.properties.BooleanValue;

public class AntiDebuff extends Module {
    public final BooleanValue blindness = new BooleanValue("blindness", true);
    public final BooleanValue nausea = new BooleanValue("nausea", true);

    public AntiDebuff() {
        super("AntiDebuff", false);
    }
}
