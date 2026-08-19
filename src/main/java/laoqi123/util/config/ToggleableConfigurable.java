package laoqi123.util.config;

import laoqi123.property.properties.BooleanProperty;

public class ToggleableConfigurable extends Configurable {
    private final BooleanProperty enabledProperty;

    public ToggleableConfigurable(String name, boolean enabledByDefault) {
        super(name);
        this.enabledProperty = this.register(new BooleanProperty("Enabled", enabledByDefault));
    }

    public boolean isEnabled() {
        return this.enabledProperty.getValue();
    }

    public void setEnabled(boolean enabled) {
        this.enabledProperty.setValue(enabled);
    }

    public BooleanProperty getEnabledProperty() {
        return this.enabledProperty;
    }

    @Override
    public boolean running() {
        return this.isEnabled() && super.running();
    }
}
