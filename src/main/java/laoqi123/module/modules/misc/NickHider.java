package laoqi123.module.modules.misc;

import laoqi123.enums.ChatColors;
import laoqi123.module.Module;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.TextValue;
import net.minecraft.client.MinecraftClient;

import java.util.regex.Matcher;

public class NickHider extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final TextValue protectName = new TextValue("name", "You");
    public final BooleanValue scoreboard = new BooleanValue("scoreboard", true);
    public final BooleanValue level = new BooleanValue("level", true);

    public NickHider() {
        super("NickHider", false, true);
    }

    public String replaceNick(String input) {
        if (input != null && mc.player != null) {
            if (this.scoreboard.getValue() && input.matches("§7\\d{2}/\\d{2}/\\d{2}(?:\\d{2})?  ?§8.*")) {
                input = input.replaceAll("§8", "§8§k").replaceAll("[^\\x00-\\x7F§]", "?");
            }
            return input.replaceAll(
                    mc.getSession().getUsername(), Matcher.quoteReplacement(ChatColors.formatColor(this.protectName.getValue()))
            );
        }
        return input;
    }
}
