package laoqi123.util.config;

import laoqi123.value.properties.BooleanValue;

public class ToggleableConfigurable extends Configurable {
    private final BooleanValue enabledProperty;

    public ToggleableConfigurable(String name, boolean enabledByDefault) {
        super(name);
        this.enabledProperty = this.register(new BooleanValue("Enabled", enabledByDefault));
    }

    public boolean isEnabled() {
        return this.enabledProperty.getValue();
    }

    public void setEnabled(boolean enabled) {
        this.enabledProperty.setValue(enabled);
    }

    public BooleanValue getEnabledProperty() {
        return this.enabledProperty;
    }

    @Override
    public boolean running() {
        return this.isEnabled() && super.running();
    }
}
