package laoqi123.ui.pages;

import laoqi123.Myau;
import laoqi123.module.Module;
import laoqi123.property.Property;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.ColorProperty;
import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.property.properties.ModeProperty;
import laoqi123.property.properties.PercentProperty;
import laoqi123.property.properties.TextProperty;
import laoqi123.ui.ClickGui;
import laoqi123.ui.Colors;
import laoqi123.ui.InputHandler;
import laoqi123.ui.renderer.NanoVGRenderUtil;
import laoqi123.ui.dataset.impl.FloatSlider;
import laoqi123.ui.dataset.impl.IntSlider;
import laoqi123.ui.dataset.impl.PercentageSlider;
import laoqi123.ui.elements.config.ConfigColorElement;
import laoqi123.ui.elements.config.ConfigDropdown;
import laoqi123.ui.elements.config.ConfigKeyBind;
import laoqi123.ui.elements.config.ConfigOption;
import laoqi123.ui.elements.config.ConfigSlider;
import laoqi123.ui.elements.config.ConfigSwitch;
import laoqi123.ui.elements.config.ConfigTextBox;

import java.util.ArrayList;
import java.util.List;

public class ModConfigPage extends Page {
    private final Module module;
    private final ArrayList<ConfigOption> options = new ArrayList<>();
    private int totalSize = 728;

    public ModConfigPage(Module module) {
        super(module.getName());
        this.module = module;
        options.add(new ConfigKeyBind(module, 2));
        List<Property<?>> props = Myau.propertyManager.properties.get(module.getClass());
        if (props != null) {
            for (Property<?> property : props) {
                ConfigOption option = null;
                if (property instanceof BooleanProperty) option = new ConfigSwitch((BooleanProperty) property, 2);
                else if (property instanceof FloatProperty) option = new ConfigSlider(property, new FloatSlider((FloatProperty) property), 2);
                else if (property instanceof IntProperty) option = new ConfigSlider(property, new IntSlider((IntProperty) property), 2);
                else if (property instanceof PercentProperty) option = new ConfigSlider(property, new PercentageSlider((PercentProperty) property), 2);
                else if (property instanceof ModeProperty) option = new ConfigDropdown((ModeProperty) property, 2);
                else if (property instanceof ColorProperty) option = new ConfigColorElement((ColorProperty) property, 2);
                else if (property instanceof TextProperty) option = new ConfigTextBox((TextProperty) property, 2);
                if (option != null) options.add(option);
            }
        }
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        String search = ClickGui.INSTANCE == null ? "" : ClickGui.INSTANCE.getSearchValue().toLowerCase().trim();
        List<ConfigOption> visible = new ArrayList<>();
        for (ConfigOption option : options) {
            option.setEnabled(option.property == null || option.property.isVisible());
            if (option.isEnabled() && option.matches(search)) visible.add(option);
        }
        int optionY = y + 16;
        int xx = x + 30;
        if (!visible.isEmpty()) {
            int backgroundSize = 16;
            for (ConfigOption option : visible) backgroundSize += option.getHeight() + 16;
            NanoVGRenderUtil.drawRoundedRect(vg, xx - 16, optionY, 1024, backgroundSize, Colors.GRAY_900, 20);
            optionY += 16;
        }
        for (ConfigOption option : visible) {
            if (optionY + option.getHeight() >= y && optionY <= y + 728) {
                option.draw(vg, xx, optionY, inputHandler);
            }
            optionY += option.getHeight() + 16;
        }
        optionY += 16;
        totalSize = optionY - y;
    }

    @Override
    public void drawLast(long vg, InputHandler inputHandler) {
        int xx = 30;
        for (ConfigOption option : options) {
            option.drawLast(vg, xx, inputHandler);
        }
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        for (ConfigOption option : options) {
            option.keyTyped(key, keyCode);
        }
    }

    @Override
    public void finishUpAndClose() {
        for (ConfigOption option : options) {
            option.finishUpAndClose();
        }
        super.finishUpAndClose();
    }

    @Override
    public int getMaxScrollHeight() {
        return totalSize;
    }

    @Override
    public boolean hasFocus() {
        for (ConfigOption option : options) {
            if (option.hasFocus()) return true;
        }
        return false;
    }

    public Module getModule() {
        return module;
    }
}
