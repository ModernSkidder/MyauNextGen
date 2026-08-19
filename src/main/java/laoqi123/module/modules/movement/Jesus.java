package laoqi123.module.modules.movement;

import laoqi123.module.Module;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.FloatValue;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Jesus extends Module {
    private static final DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
    public final FloatValue speed = new FloatValue("speed", 2.5F, 0.0F, 3.0F);
    public final BooleanValue noPush = new BooleanValue("no-push", true);
    public final BooleanValue groundOnly = new BooleanValue("ground-only", true);

    public Jesus() {
        super("Jesus", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{df.format(this.speed.getValue())};
    }
}
