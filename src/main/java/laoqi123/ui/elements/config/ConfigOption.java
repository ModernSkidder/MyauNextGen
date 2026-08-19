package laoqi123.ui.elements.config;

import laoqi123.module.Module;
import laoqi123.value.Value;
import laoqi123.ui.Colors;
import laoqi123.ui.InputHandler;

public abstract class ConfigOption {
    protected final String name;
    public final Value<?> value;
    protected final Module module;
    protected final int size;
    protected boolean enabled = true;
    protected int nameColor = Colors.WHITE_80;

    public ConfigOption(Value<?> value, int size) {
        this(value, null, size);
    }

    public ConfigOption(Value<?> value, Module module, int size) {
        this.name = value == null ? "" : value.getName().replace("-", " ");
        this.value = value;
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
