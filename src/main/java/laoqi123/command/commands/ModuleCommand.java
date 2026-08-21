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
        if (module == null) {
            ChatUtil.sendFormatted(String.format("%sUnknown module &o%s&r", Myau.clientName, args.get(0)));
            return;
        }
        if (args.size() >= 2) {
            // Value names may contain spaces (e.g. "Max Delay Ticks"). Progressively
            // join args[1..i] until a full name matches, so both "maxdelayticks" and
            // "max delay ticks" resolve; the first (shortest) match wins.
            Value<?> value = null;
            int nameEnd = -1;
            for (int i = 1; i < args.size(); i++) {
                Value<?> candidate = Myau.valueManager.getProperty(module, String.join(" ", args.subList(1, i + 1)));
                if (candidate != null) {
                    value = candidate;
                    nameEnd = i;
                    break;
                }
            }
            if (value == null) {
                ChatUtil.sendFormatted(String.format("%s%s has no value &o%s&r", Myau.clientName, module.getName(), args.get(1)));
            } else if (nameEnd == args.size() - 1 && !(value instanceof BooleanValue)) {
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
                String newValue = nameEnd == args.size() - 1 ? null : String.join(" ", args.subList(nameEnd + 1, args.size()));
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
