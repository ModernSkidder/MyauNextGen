package laoqi123.command.commands;

import laoqi123.Myau;
import laoqi123.command.Command;
import laoqi123.module.Module;
import laoqi123.util.ChatUtil;
import laoqi123.value.Value;
import laoqi123.value.properties.BooleanValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleCommand extends Command {
    public ModuleCommand() {
        super(new ArrayList<>(Myau.moduleManager.modules.values().stream().<String>map(Module::getName).collect(Collectors.<String>toList())));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        Module module = Myau.moduleManager.getModule(args.get(0));
        if (args.size() >= 2) {
            Value<?> value = Myau.valueManager.getProperty(module, args.get(1));
            if (value == null) {
                ChatUtil.sendFormatted(String.format("%s%s has no value &o%s&r", Myau.clientName, module.getName(), args.get(1)));
            } else if (args.size() < 3 && !(value instanceof BooleanValue)) {
                ChatUtil.sendFormatted(
                        String.format(
                                "%s%s: &o%s&r is set to %s&r (%s)&r",
                                Myau.clientName,
                                module.getName(),
                                value.getName(),
                                value.formatValue(),
                                value.getValuePrompt()
                        )
                );
            } else {
                String newValue = args.size() < 3 ? null : String.join(" ", args.subList(2, args.size()));
                try {
                    if (value.parseString(newValue)) {
                        ChatUtil.sendFormatted(
                                String.format("%s%s: &o%s&r has been set to %s&r", Myau.clientName, module.getName(), value.getName(), value.formatValue())
                        );
                        return;
                    }
                } catch (Exception e) {
                }
                ChatUtil.sendFormatted(
                        String.format("%sInvalid value for value &o%s&r (%s)&r", Myau.clientName, value.getName(), value.getValuePrompt())
                );
            }
        } else {
            List<Value<?>> properties = Myau.valueManager.properties.get(module.getClass());
            if (properties != null) {
                List<Value<?>> visible = properties.stream().filter(Value::isVisible).collect(Collectors.toList());
                if (!visible.isEmpty()) {
                    ChatUtil.sendFormatted(String.format("%s%s:&r", Myau.clientName, module.formatModule()));
                    for (Value<?> value : visible) {
                        ChatUtil.sendFormatted(String.format("&7»&r %s: %s&r", value.getName(), value.formatValue()));
                    }
                    return;
                }
            }
            ChatUtil.sendFormatted(String.format("%s%s has no properties&r", Myau.clientName, module.formatModule()));
        }
    }
}
