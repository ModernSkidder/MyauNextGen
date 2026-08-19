package laoqi123.command.commands;

import laoqi123.Myau;
import laoqi123.command.Command;
import laoqi123.util.ChatUtil;
import net.minecraft.client.MinecraftClient;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class VclipCommand extends Command {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));

    public VclipCommand() {
        super(new ArrayList<>(Collections.singletonList("vclip")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() >= 2) {
            double distance = 0.0;
            try {
                distance = Double.parseDouble(args.get(1));
            } catch (NumberFormatException e) {
            } finally {
                mc.player.refreshPositionAndAngles(mc.player.getX(), mc.player.getY() + distance, mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch());
                ChatUtil.sendFormatted(String.format("%sClipped (%s blocks)", Myau.clientName, df.format(distance)));
            }
            return;
        }
        ChatUtil.sendFormatted(
                String.format("%sUsage: .%s <&odistance&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT))
        );
    }
}
