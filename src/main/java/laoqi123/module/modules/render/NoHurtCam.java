package laoqi123.module.modules.render;

import laoqi123.module.Module;
import laoqi123.value.properties.PercentValue;

public class NoHurtCam extends Module {
    public final PercentValue multiplier = new PercentValue("multiplier", 0);

    public NoHurtCam() {
        super("NoHurtCam", false, true);
    }
}
