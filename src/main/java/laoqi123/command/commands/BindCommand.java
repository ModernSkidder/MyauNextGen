package laoqi123.command.commands;

import laoqi123.Myau;
import laoqi123.command.Command;
import laoqi123.module.Module;
import laoqi123.util.ChatUtil;
import laoqi123.util.KeyBindUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class BindCommand extends Command {
    public BindCommand() {
        super(new ArrayList<>(Arrays.asList("bind", "b")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 3) {
            if (args.size() == 2 && (args.get(1).equalsIgnoreCase("l") || args.get(1).equalsIgnoreCase("list"))) {
                List<Module> modules = Myau.moduleManager.modules.values().stream().filter(module -> module.getKey() != 0).collect(Collectors.toList());
                if (modules.isEmpty()) {
                    ChatUtil.sendFormatted(String.format("%sNo binds&r", Myau.clientName));
                } else {
                    ChatUtil.sendFormatted(String.format("%sBinds:&r", Myau.clientName));
                    for (Module module : modules) {
                        ChatUtil.sendFormatted(String.format("%s»&r %s&r", module.isHidden() ? "&8" : "&7", module.formatModule()));
                    }
                }
            } else {
                ChatUtil.sendFormatted(
                        String.format(
                                "%sUsage: .%s <&omodule&r> <&okey&r>&r | .%s <&omodule&r> &onone&r | .%s &olist&r",
                                Myau.clientName,
                                args.get(0).toLowerCase(Locale.ROOT),
                                args.get(0).toLowerCase(Locale.ROOT),
                                args.get(0).toLowerCase(Locale.ROOT)
                        )
                );
            }
        } else {
            String keyInput = args.get(2).toUpperCase();
            int keyIndex = 0;

            if (keyInput.equalsIgnoreCase("NONE") || keyInput.equalsIgnoreCase("NULL") || keyInput.equalsIgnoreCase("0")) {
                keyIndex = 0;
            } else {
                keyIndex = getKeyIndex(keyInput);

                if (keyIndex == 0) {
                    int buttonIndex = getMouseButtonIndex(keyInput);
                    if (buttonIndex != -1) {
                        keyIndex = buttonIndex - 100;
                    }
                }
            }

            if (!args.get(1).equals("*")) {
                Module module = Myau.moduleManager.getModule(args.get(1));
                if (module == null) {
                    ChatUtil.sendFormatted(String.format("%sModule not found (&o%s&r)&r", Myau.clientName, args.get(1)));
                } else {
                    module.setKey(keyIndex);
                    if (keyIndex == 0) {
                        ChatUtil.sendFormatted(
                                String.format("%sUnbind &o%s&r", Myau.clientName, module.getName())
                        );
                    } else {
                        ChatUtil.sendFormatted(
                                String.format("%sBound &o%s&r to &l[%s]&r", Myau.clientName, module.getName(), KeyBindUtil.getKeyName(keyIndex))
                        );
                    }
                }
            } else {
                for (Module module : Myau.moduleManager.modules.values()) {
                    module.setKey(keyIndex);
                }
                if (keyIndex == 0) {
                    ChatUtil.sendFormatted(
                            String.format("%sUnbind all modules&r", Myau.clientName)
                    );
                } else {
                    ChatUtil.sendFormatted(
                            String.format("%sBind all modules to &l[%s]&r", Myau.clientName, KeyBindUtil.getKeyName(keyIndex))
                    );
                }
            }
        }
    }

    private int getMouseButtonIndex(String buttonName) {
        // Handle numbered format (MOUSE0, MOUSE1, etc.)
        if (buttonName.startsWith("MOUSE")) {
            try {
                String numStr = buttonName.substring(5);
                int buttonNum = Integer.parseInt(numStr);
                if (buttonNum >= 0 && buttonNum < GLFW.GLFW_MOUSE_BUTTON_LAST + 1) {
                    return buttonNum;
                }
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            }
        }

        int buttonIndex = getButtonIndex(buttonName);
        if (buttonIndex != -1) {
            return buttonIndex;
        }

        switch (buttonName) {
            case "LBUTTON":
            case "LMB":
            case "LEFTCLICK":
                return 0;
            case "RBUTTON":
            case "RMB":
            case "RIGHTCLICK":
                return 1;
            case "MBUTTON":
            case "MMB":
            case "MIDDLECLICK":
            case "SCROLLCLICK":
                return 2;
            case "MOUSE3":
            case "XBUTTON1":
            case "SIDEBUTTON1":
            case "BOTTOMSIDE":
                return 3;
            case "MOUSE4":
            case "XBUTTON2":
            case "SIDEBUTTON2":
            case "TOPSIDE":
                return 4;
            case "MOUSE5":
                return 5;
            case "MOUSE6":
                return 6;
            case "MOUSE7":
                return 7;
            default:
                return -1;
        }
    }

    private static int getKeyIndex(String keyName) {
        String name = keyName.toUpperCase(Locale.ROOT).replace("KEY_", "");
        try {
            return GLFW.class.getField("GLFW_KEY_" + getGlfwKeyName(name)).getInt(null);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String getGlfwKeyName(String lwjglName) {
        switch (lwjglName) {
            case "RETURN": return "ENTER";
            case "CAPITAL": return "CAPS_LOCK";
            case "BACK": return "BACKSPACE";
            case "EQUALS": return "EQUAL";
            case "GRAVE": return "GRAVE_ACCENT";
            case "LBRACKET": return "LEFT_BRACKET";
            case "RBRACKET": return "RIGHT_BRACKET";
            case "LSHIFT": return "LEFT_SHIFT";
            case "RSHIFT": return "RIGHT_SHIFT";
            case "LCONTROL": return "LEFT_CONTROL";
            case "RCONTROL": return "RIGHT_CONTROL";
            case "LMENU": return "LEFT_ALT";
            case "RMENU": return "RIGHT_ALT";
            case "LMETA": return "LEFT_SUPER";
            case "RMETA": return "RIGHT_SUPER";
            case "PRIOR": return "PAGE_UP";
            case "NEXT": return "PAGE_DOWN";
            case "NUMLOCK": return "NUM_LOCK";
            case "SCROLL": return "SCROLL_LOCK";
            case "SYSRQ": return "PRINT_SCREEN";
            case "ADD": return "KP_ADD";
            case "SUBTRACT": return "KP_SUBTRACT";
            case "MULTIPLY": return "KP_MULTIPLY";
            case "DIVIDE": return "KP_DIVIDE";
            case "DECIMAL": return "KP_DECIMAL";
            case "NUMPADENTER": return "KP_ENTER";
            case "NUMPADEQUALS": return "KP_EQUAL";
            default:
                if (lwjglName.startsWith("NUMPAD")) {
                    return "KP_" + lwjglName.substring(6);
                }
                return lwjglName;
        }
    }

    private int getButtonIndex(String buttonName) {
        if (buttonName.startsWith("BUTTON")) {
            try {
                int buttonIndex = Integer.parseInt(buttonName.substring(6));
                if (buttonIndex >= 0 && buttonIndex < GLFW.GLFW_MOUSE_BUTTON_LAST + 1) {
                    return buttonIndex;
                }
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                return -1;
            }
        }
        return -1;
    }
}
