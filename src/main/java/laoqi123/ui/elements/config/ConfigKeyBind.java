package laoqi123.ui.elements.config;

import laoqi123.module.Module;
import laoqi123.ui.ColorPalette;
import laoqi123.ui.InputHandler;
import laoqi123.ui.elements.BasicButton;
import laoqi123.ui.elements.IFocusable;
import laoqi123.ui.renderer.NanoVGRenderUtil;
import laoqi123.util.KeyBindUtil;
import org.lwjgl.glfw.GLFW;

public class ConfigKeyBind extends ConfigOption implements IFocusable {
    private final BasicButton button;
    private boolean clicked = false;
    private InputHandler inputHandler;

    public ConfigKeyBind(Module module, int size) {
        super(null, module, size);
        button = new BasicButton(256, 32, "", 8, 0, BasicButton.ALIGNMENT_JUSTIFIED, ColorPalette.SECONDARY);
        button.setToggleable(true);
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        this.inputHandler = inputHandler;
        NanoVGRenderUtil.drawText(vg, "Keybind", x, y + 17, nameColor, 14f);
        String text = module.getKey() == 0 ? "NONE" : KeyBindUtil.getKeyName(module.getKey());
        if (button.isToggled()) {
            inputHandler.blockAllInput();
            if (!clicked) {
                module.setKey(0);
                clicked = true;
            }
        } else if (clicked) {
            clicked = false;
            inputHandler.stopBlockingInput();
        }
        button.setText(text.equals("NONE") && button.isToggled() ? "Recording... (ESC to clear)" : text);
        button.draw(vg, x + (size == 1 ? 224 : 736), y, inputHandler);
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        if (!button.isToggled() || keyCode == 0) return;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            module.setKey(0);
            button.setToggled(false);
            clicked = false;
            inputHandler.stopBlockingInput();
        } else {
            module.setKey(keyCode);
            button.setToggled(false);
            clicked = false;
            inputHandler.stopBlockingInput();
        }
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public boolean hasFocus() {
        return clicked;
    }
}
