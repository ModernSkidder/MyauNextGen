package laoqi123.command.commands;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import laoqi123.Myau;
import laoqi123.command.Command;
import laoqi123.enums.ChatColors;
import laoqi123.util.ChatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Locale;

public class DenickCommand extends Command {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public DenickCommand() {
        super(new ArrayList<>(Collections.singletonList("denick")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            ChatUtil.sendFormatted(String.format("%sUsage: .%s <&oname&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT)));
        } else {
            PlayerListEntry playerInfo = mc.getNetworkHandler().getPlayerListEntry(ChatColors.formatColor(args.get(1)));
            if (playerInfo != null) {
                GameProfile gameProfile = playerInfo.getProfile();
                Property property = Iterables.getFirst(gameProfile.getProperties().get("textures"), null);
                if (property != null) {
                    String code = new String(Base64.getDecoder().decode(property.value().getBytes(StandardCharsets.UTF_8)));
                    String name = code.contains("profileName\" : \"") ? code.split("profileName\" : \"")[1].split("\"")[0] : "?";
                    String uuid = code.contains("profileId\" : \"") ? code.split("profileId\" : \"")[1].split("\"")[0] : "?";
                    ChatUtil.sendRaw(
                            String.format(
                                    ChatColors.formatColor("%s%s&r -> %s (&o%s&r)&r"),
                                    ChatColors.formatColor(Myau.clientName),
                                    gameProfile.getName().replace("§", "&"),
                                    name,
                                    uuid
                            )
                    );
                    if (!uuid.isEmpty() && !uuid.equals("?")) {
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(uuid), null);
                    }
                } else {
                    ChatUtil.sendRaw(
                            String.format(
                                    ChatColors.formatColor("%sNo textures for entity with name &o%s&r"),
                                    ChatColors.formatColor(Myau.clientName),
                                    args.get(1)
                            )
                    );
                }
            } else {
                ChatUtil.sendRaw(
                        String.format(
                                ChatColors.formatColor("%sNo entity with name &o%s&r"),
                                ChatColors.formatColor(Myau.clientName),
                                args.get(1)
                        )
                );
            }
        }
    }
}
