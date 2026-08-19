package laoqi123.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindUtil {
    public static String getKeyName(int keyCode) {
        if (keyCode < 0) {
            int mouseButton = keyCode + 100;
            InputUtil.Key mouseKey = InputUtil.Type.MOUSE.createFromCode(mouseButton);
            return mouseKey.getLocalizedText().getString();
        }
        InputUtil.Key key = InputUtil.fromKeyCode(keyCode, -1);
        return key.getLocalizedText().getString();
    }

    public static boolean isKeyDown(int keyCode) {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        return keyCode < 0 ? GLFW.glfwGetMouseButton(handle, keyCode + 100) == GLFW.GLFW_PRESS
                : InputUtil.isKeyPressed(handle, keyCode);
    }

    public static boolean isKeyDown(InputUtil.Key key) {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        if (key.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(handle, key.getCode()) == GLFW.GLFW_PRESS;
        }
        return InputUtil.isKeyPressed(handle, key.getCode());
    }

    public static boolean isKeyDown(net.minecraft.client.option.KeyBinding keyBinding) {
        return isKeyDown(InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey()));
    }

    public static void updateKeyState(int keyCode) {
        KeyBindUtil.setKeyBindState(keyCode, KeyBindUtil.isKeyDown(keyCode));
    }

    public static void updateKeyState(net.minecraft.client.option.KeyBinding keyBinding) {
        KeyBindUtil.setKeyBindState(keyBinding, KeyBindUtil.isKeyDown(keyBinding));
    }

    public static void setKeyBindState(int keyCode, boolean pressed) {
        InputUtil.Key key = KeyBindUtil.fromKeyCode(keyCode);
        if (key != null) {
            KeyBinding.setKeyPressed(key, pressed);
        }
    }

    public static void setKeyBindState(net.minecraft.client.option.KeyBinding keyBinding, boolean pressed) {
        InputUtil.Key key = InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey());
        KeyBinding.setKeyPressed(key, pressed);
    }

    public static void pressKeyOnce(int keyCode) {
        InputUtil.Key key = KeyBindUtil.fromKeyCode(keyCode);
        if (key != null) {
            KeyBinding.onKeyPressed(key);
        }
    }

    public static InputUtil.Key fromKeyCode(int keyCode) {
        if (keyCode < 0) {
            return InputUtil.Type.MOUSE.createFromCode(keyCode + 100);
        }
        return InputUtil.fromKeyCode(keyCode, -1);
    }
}
