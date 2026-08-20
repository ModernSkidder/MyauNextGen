package laoqi123.ui.pages;

import laoqi123.Myau;
import laoqi123.module.Module;
import laoqi123.value.Value;
import laoqi123.value.properties.*;
import laoqi123.value.properties.ModeValue;
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
import laoqi123.ui.elements.config.ConfigMultiChoice;
import laoqi123.ui.elements.config.ConfigOption;
import laoqi123.ui.elements.config.ConfigRangeSlider;
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
        List<Value<?>> props = Myau.valueManager.properties.get(module.getClass());
        if (props != null) {
            for (Value<?> value : props) {
                ConfigOption option = null;
                if (value instanceof BooleanValue) option = new ConfigSwitch((BooleanValue) value, 2);
                else if (value instanceof FloatValue) option = new ConfigSlider(value, new FloatSlider((FloatValue) value), 2);
                else if (value instanceof IntValue) option = new ConfigSlider(value, new IntSlider((IntValue) value), 2);
                else if (value instanceof PercentValue) option = new ConfigSlider(value, new PercentageSlider((PercentValue) value), 2);
                else if (value instanceof ModeValue) option = new ConfigDropdown((ModeValue) value, 2);
                else if (value instanceof ColorValue) option = new ConfigColorElement((ColorValue) value, 2);
                else if (value instanceof TextValue) option = new ConfigTextBox((TextValue) value, 2);
                else if (value instanceof IntRangeValue) option = new ConfigRangeSlider((IntRangeValue) value, 2);
                else if (value instanceof FloatRangeValue) option = new ConfigRangeSlider((FloatRangeValue) value, 2);
                else if (value instanceof EnumChoiceValue) option = new ConfigDropdown((EnumChoiceValue<?>) value, 2);
                else if (value instanceof IntChoiceValue) option = new ConfigDropdown((IntChoiceValue) value, 2);
                else if (value instanceof MultiEnumChoiceValue) option = new ConfigMultiChoice((MultiEnumChoiceValue<?>) value, 2);
                if (option != null) options.add(option);
            }
        }
        // 按名称字母排序(KeyBind 值为空,自然排最前),方便查找
        options.sort(java.util.Comparator.comparing(o -> o.value == null ? "" : o.value.getName().toLowerCase()));
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        String search = ClickGui.INSTANCE == null ? "" : ClickGui.INSTANCE.getSearchValue().toLowerCase().trim();
        List<ConfigOption> visible = new ArrayList<>();
        for (ConfigOption option : options) {
            option.setEnabled(option.value == null || option.value.isVisible());
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
