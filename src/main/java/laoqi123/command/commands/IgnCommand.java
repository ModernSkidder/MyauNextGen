package laoqi123.command.commands;

import laoqi123.Myau;
import laoqi123.command.Command;
import laoqi123.util.ChatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import net.minecraft.util.StringHelper;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;

public class IgnCommand extends Command {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public IgnCommand() {
        super(new ArrayList<String>(Arrays.asList("username", "name", "ign")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        Session session = mc.getSession();
        if (session != null) {
            String username = session.getUsername();
            if (!StringHelper.isEmpty(username)) {
                try {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(username), null);
                    ChatUtil.sendFormatted(String.format("%sYour username has been copied to the clipboard (&o%s&r)&r", Myau.clientName, username));
                } catch (Exception e) {
                    ChatUtil.sendFormatted(String.format("%sFailed to copy&r", Myau.clientName));
                }
            }
        }
    }
}
