package laoqi123.util;

import laoqi123.enums.ChatColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ChatUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void send(Text text) {
        if (ChatUtil.mc.player != null) {
            ChatUtil.mc.player.sendMessage(text, false);
        }
    }

    public static void sendFormatted(String string) {
        ChatUtil.send(Text.literal(ChatColors.formatColor(string)));
    }

    public static void sendRaw(String string) {
        ChatUtil.send(Text.literal(string));
    }

    public static void sendMessage(String string) {
        if (ChatUtil.mc.player != null) {
            ChatUtil.mc.player.networkHandler.sendChatMessage(string);
        }
    }
}
