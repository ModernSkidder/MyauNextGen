package laoqi123;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import laoqi123.value.Value;
import me.ksyz.accountmanager.AccountManager;
import laoqi123.command.CommandManager;
import laoqi123.command.commands.*;
import laoqi123.config.Config;
import laoqi123.event.EventManager;
import laoqi123.management.*;
import laoqi123.module.Module;
import laoqi123.module.ModuleManager;
import laoqi123.value.ValueManager;
import laoqi123.util.MovementUtils;
import laoqi123.util.player.PlayerUtils;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

public class Myau {
    public static String clientName = "&7[&0M&8y&8a&7u&7]&r ";
    public static String version;

    /**
     * Global tick-rate override (1.0 = vanilla). Consumed by
     * {@link laoqi123.mixin.MixinRenderTickCounter}. Modules like Velocity
     * (anti-knockback) temporarily slow / speed the game to land combos.
     */
    public static volatile float serverTickRate = 1.0F;
    public static RotationManager rotationManager;
    public static FloatManager floatManager;
    public static BlinkManager blinkManager;
    public static DelayManager delayManager;
    public static LagManager lagManager;
    public static PlayerStateManager playerStateManager;
    public static FriendManager friendManager;
    public static TargetManager targetManager;
    public static ValueManager valueManager;
    public static ModuleManager moduleManager;
    public static PlayerUtils playerUtils;
    public static CommandManager commandManager;

    public Myau() {
        this.init();
    }

    public void init() {
        rotationManager = new RotationManager();
        floatManager = new FloatManager();
        blinkManager = new BlinkManager();
        delayManager = new DelayManager();
        lagManager = new LagManager();
        playerStateManager = new PlayerStateManager();
        friendManager = new FriendManager();
        targetManager = new TargetManager();
        valueManager = new ValueManager();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        playerUtils = new PlayerUtils();
        EventManager.register(rotationManager);
        EventManager.register(playerUtils);
        EventManager.register(MovementUtils.INSTANCE);
        EventManager.register(floatManager);
        EventManager.register(blinkManager);
        EventManager.register(delayManager);
        EventManager.register(lagManager);
        EventManager.register(moduleManager);
        EventManager.register(commandManager);
        commandManager.commands.add(new BindCommand());
        commandManager.commands.add(new ConfigCommand());
        commandManager.commands.add(new DenickCommand());
        commandManager.commands.add(new FriendCommand());
        commandManager.commands.add(new HelpCommand());
        commandManager.commands.add(new HideCommand());
        commandManager.commands.add(new IgnCommand());
        commandManager.commands.add(new ItemCommand());
        commandManager.commands.add(new ListCommand());
        commandManager.commands.add(new ModuleCommand());
        commandManager.commands.add(new PlayerCommand());
        commandManager.commands.add(new ShowCommand());
        commandManager.commands.add(new TargetCommand());
        commandManager.commands.add(new ToggleCommand());
        commandManager.commands.add(new VclipCommand());
        for (Module module : moduleManager.modules.values()) {
            ArrayList<Value<?>> properties = new ArrayList<>();
            for (final Field field : module.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                final Object obj;
                try {
                    obj = field.get(module);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                if (obj instanceof Value<?>) {
                    ((Value<?>) obj).setOwner(module);
                    properties.add((Value<?>) obj);
                }
            }
            if (module instanceof laoqi123.util.config.PropertyProvider) {
                for (Value<?> value : ((laoqi123.util.config.PropertyProvider) module).getAdditionalProperties()) {
                    value.setOwner(module);
                    properties.add(value);
                }
            }
            valueManager.properties.put(module.getClass(), properties);
            EventManager.register(module);
        }
        Config config = new Config("default", true);
        if (config.file.exists()) {
            config.load();
        }
        if (friendManager.file.exists()) {
            friendManager.load();
        }
        if (targetManager.file.exists()) {
            targetManager.load();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(config::save));

        try (InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(Myau.class.getResourceAsStream("/assets/myaunextgen/version.json")), StandardCharsets.UTF_8)) {
            JsonObject modInfo = new JsonParser().parse(reader).getAsJsonObject();
            version = modInfo.get("version").getAsString();
        } catch (Exception e) {
            version = "dev";
        }

        AccountManager.init();
    }
}
