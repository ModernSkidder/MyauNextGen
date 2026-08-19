package laoqi123.command.commands;

import laoqi123.Myau;
import laoqi123.command.Command;
import laoqi123.enums.ChatColors;
import laoqi123.util.ChatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.Arrays;

public class PlayerCommand extends Command {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public PlayerCommand() {
        super(new ArrayList<>(Arrays.asList("playerlist", "players")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        ArrayList<String> players = new ArrayList<>();
        for (PlayerListEntry playerInfo : mc.getNetworkHandler().getPlayerList()) {
            players.add(playerInfo.getProfile().getName().replace("§", "&"));
        }
        if (players.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sNo players&r", Myau.clientName));
        } else {
            ChatUtil.sendRaw(
                    String.format(
                            ChatColors.formatColor("%sPlayers:&r %s"),
                            ChatColors.formatColor(Myau.clientName),
                            String.join(", ", players)
                    )
            );
        }
    }
}
