package laoqi123.ui.elements.config;

import laoqi123.module.Module;
import laoqi123.property.Property;
import laoqi123.ui.Colors;
import laoqi123.ui.InputHandler;

public abstract class ConfigOption {
    protected final String name;
    public final Property<?> property;
    protected final Module module;
    protected final int size;
    protected boolean enabled = true;
    protected int nameColor = Colors.WHITE_80;

    public ConfigOption(Property<?> property, int size) {
        this(property, null, size);
    }

    public ConfigOption(Property<?> property, Module module, int size) {
        this.name = property == null ? "" : property.getName().replace("-", " ");
        this.property = property;
        this.module = module;
        this.size = size;
    }

    public abstract void draw(long vg, int x, int y, InputHandler inputHandler);

    public void drawLast(long vg, int x, InputHandler inputHandler) {
    }

    public void keyTyped(char key, int keyCode) {
    }

    public void finishUpAndClose() {
    }

    public int getHeight() {
        return 32;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean hasFocus() {
        return false;
    }

    public boolean matches(String search) {
        return search.isEmpty() || name.toLowerCase().contains(search);
    }
}
