package laoqi123.ui.elements.text;

import laoqi123.ui.ColorPalette;
import laoqi123.ui.Colors;
import laoqi123.ui.InputHandler;
import laoqi123.ui.animations.ColorAnimation;
import laoqi123.ui.renderer.Icons;
import laoqi123.ui.renderer.NanoVGRenderUtil;
import org.lwjgl.glfw.GLFW;

public class TextInputField {
    protected int width;
    protected int height;
    protected final String placeholder;
    protected final int icon;
    protected String input = "";
    protected boolean disabled;
    protected boolean errored;
    protected boolean onlyNums = false;
    protected int caretPos = 0;
    protected int prevCaret = 0;
    protected final ColorAnimation colorAnimation = new ColorAnimation(new ColorPalette(Colors.GRAY_700, Colors.GRAY_600, Colors.GRAY_500));
    protected static TextInputField focusedField;
    protected final boolean secure;
    private boolean password;

    public TextInputField(int width, int height, String placeholder, boolean secure, boolean password, int icon, float cornerRadius) {
        this.width = width;
        this.height = height;
        this.placeholder = placeholder;
        this.secure = secure;
        this.password = password;
        this.icon = icon;
    }

    public TextInputField(int width, int height, boolean secure, String placeholder) {
        this(width, height, placeholder, secure, false, 0, 8f);
    }

    public void disable(boolean disabled) {
        this.disabled = disabled;
        if (disabled && focusedField == this) focusedField = null;
    }

    public void setErrored(boolean errored) {
        this.errored = errored;
    }

    public boolean isToggled() {
        return focusedField == this;
    }

    public void onClick() {
        if (disabled) return;
        if (focusedField != null && focusedField != this) focusedField.onClose();
        focusedField = this;
        caretPos = input.length();
        prevCaret = caretPos;
    }

    public void onClose() {
        if (focusedField == this) focusedField = null;
    }

    public static boolean isAnySelected() {
        return focusedField != null;
    }

    public static void clearFocus() {
        if (focusedField != null) focusedField.onClose();
    }

    public void draw(long vg, float x, float y, InputHandler inputHandler) {
        boolean hovered = inputHandler.isAreaHovered(x, y, width, height) && !disabled;
        if (disabled) NanoVGRenderUtil.setAlpha(vg, 0.5f);
        int bg = colorAnimation.getColor(hovered, isToggled());
        NanoVGRenderUtil.drawRoundedRect(vg, x, y, width, height, bg, 8f);
        if (isToggled()) {
            NanoVGRenderUtil.drawHollowRoundRect(vg, x, y, width, height, errored ? Colors.ERROR_600 : Colors.GRAY_300, 8f, 1f);
        }
        if (hovered && inputHandler.isClicked()) {
            onClick();
        }

        String shown = secure && password ? "•".repeat(input.length()) : input;
        float textX = x + 14f;
        float baselineY = y + height / 2f + 5f;
        if (icon != 0) {
            Icons.search(vg, x + 12f, y + height / 2f - 8f, 16f, isToggled() ? Colors.WHITE : Colors.WHITE_60);
            textX = x + 36f;
        }
        if (input.isEmpty()) {
            NanoVGRenderUtil.drawText(vg, placeholder, textX, baselineY, Colors.WHITE_50, 12f);
        } else {
            NanoVGRenderUtil.drawText(vg, shown, textX, baselineY, Colors.WHITE, 12f);
        }
        if (isToggled() && !errored) {
            float caretX = textX + NanoVGRenderUtil.getTextWidth(vg, shown.substring(0, Math.min(caretPos, shown.length())), 12f);
            NanoVGRenderUtil.drawRect(vg, caretX, y + height / 2f - 6f, 1.5f, 12f, Colors.WHITE);
        }
        if (disabled) NanoVGRenderUtil.setAlpha(vg, 1f);
    }

    public void keyTyped(char key, int keyCode) {
        if (focusedField != this) return;
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (caretPos > 0) {
                input = input.substring(0, caretPos - 1) + input.substring(caretPos);
                caretPos--;
            }
            return;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT && caretPos > 0) {
            caretPos--;
            return;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT && caretPos < input.length()) {
            caretPos++;
            return;
        }
        if (keyCode == 0 && key >= 32) {
            if (onlyNums && !(Character.isDigit(key) || key == '.' || key == '-')) return;
            input = input.substring(0, caretPos) + key + input.substring(caretPos);
            caretPos++;
        }
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
        if (caretPos > input.length()) caretPos = input.length();
    }

    public int getLines() {
        return 1;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public boolean isPassword() {
        return password;
    }

    public void setPassword(boolean password) {
        this.password = password;
    }
}
