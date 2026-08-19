package laoqi123.command.commands;

import laoqi123.Myau;
import laoqi123.command.Command;
import laoqi123.enums.ChatColors;
import laoqi123.util.ChatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.Arrays;

public class ItemCommand extends Command {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public ItemCommand() {
        super(new ArrayList<>(Arrays.asList("itemname", "item")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        ItemStack stack = mc.player.getMainHandStack();
        if (stack != null) {
            String display = stack.getName().getString().replace('§', '&');
            String registryName = Registries.ITEM.getId(stack.getItem()).toString();
            String compound = stack.contains(DataComponentTypes.CUSTOM_DATA) ? stack.get(DataComponentTypes.CUSTOM_DATA).getNbt().toString().replace('§', '&') : "";
            ChatUtil.sendRaw(String.format("%s%s (%s) %s", ChatColors.formatColor(Myau.clientName), display, registryName, compound));
        }
    }
}
