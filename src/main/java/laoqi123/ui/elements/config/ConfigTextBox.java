package laoqi123.ui.elements.config;

import laoqi123.property.properties.TextProperty;
import laoqi123.ui.InputHandler;
import laoqi123.ui.elements.IFocusable;
import laoqi123.ui.elements.text.TextInputField;
import laoqi123.ui.renderer.NanoVGRenderUtil;

public class ConfigTextBox extends ConfigOption implements IFocusable {
    public final TextInputField textField;

    public ConfigTextBox(TextProperty property, int size) {
        super(property, size);
        this.textField = new TextInputField(size == 1 ? 256 : 640, 32, "Enter text...", false, false, 0, 8f);
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        if (!enabled) NanoVGRenderUtil.setAlpha(vg, 0.5f);
        textField.disable(!enabled);
        NanoVGRenderUtil.drawText(vg, name, x, y + 16, nameColor, 14f);
        String value = (String) property.getValue();
        if (!textField.isToggled()) textField.setInput(value == null ? "" : value);
        textField.draw(vg, x + (size == 1 ? 224 : 352), y, inputHandler);
        NanoVGRenderUtil.setAlpha(vg, 1f);
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        if (!enabled) return;
        textField.keyTyped(key, keyCode);
        property.setValue(textField.getInput());
    }

    @Override
    public void finishUpAndClose() {
        textField.onClose();
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public boolean hasFocus() {
        return textField.isToggled();
    }
}
