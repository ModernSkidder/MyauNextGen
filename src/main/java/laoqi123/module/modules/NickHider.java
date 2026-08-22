package laoqi123.module.modules;

import laoqi123.enums.ChatColors;
import laoqi123.module.Module;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.TextProperty;
import net.minecraft.client.MinecraftClient;

import java.util.regex.Matcher;

public class NickHider extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final TextProperty protectName = new TextProperty("name", "You");
    public final BooleanProperty scoreboard = new BooleanProperty("scoreboard", true);
    public final BooleanProperty level = new BooleanProperty("level", true);

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
