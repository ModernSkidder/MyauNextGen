package laoqi123.config;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import laoqi123.Myau;
import laoqi123.module.Module;
import laoqi123.util.ChatUtil;
import laoqi123.value.Value;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;

public class Config {
    public static MinecraftClient mc = MinecraftClient.getInstance();
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    public String name;
    public File file;

    public static String lastConfig;

    public Config(String name, boolean newConfig) {
        this.name = name;
        lastConfig = name;
        if (name.equals("!") || name.equals("default")) {
            this.name = "default";
        }
        this.file = new File(FabricLoader.getInstance().getConfigDir().toFile(), String.format("Myau/%s.json", this.name));
        try {
            file.getParentFile().mkdirs();
            if (newConfig) {
                LOGGER.info(String.format("Created: %s", this.file.getName()));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    public void load() {
        try {

            if (!file.exists()) {
                ChatUtil.sendFormatted(String.format("%sConfig file not found (&c&o%s&r). Creating default config...&r", Myau.clientName, file.getName()));
                save();
                return;
            }

            JsonElement parsed = JsonParser.parseString(Files.readString(file.toPath()));
            if (parsed == null || !parsed.isJsonObject()) {
                ChatUtil.sendFormatted(String.format("%sInvalid config format (&c&o%s&r)&r", Myau.clientName, file.getName()));
                return;
            }

            JsonObject jsonObject = parsed.getAsJsonObject();
            for (Module module : Myau.moduleManager.modules.values()) {
                JsonElement moduleObj = jsonObject.get(module.getName());
                if (moduleObj != null && moduleObj.isJsonObject()) {
                    JsonObject object = moduleObj.getAsJsonObject();

                    ArrayList<Value<?>> list = Myau.valueManager.properties.get(module.getClass());
                    if (list != null) {
                        for (Value<?> value : list) {
                            if (value.isDoNotIncludeAlways()) {
                                continue;
                            }
                            if (object.has(value.getName())) {
                                try {
                                    value.read(object);
                                } catch (Exception e) {
                                    LOGGER.warn(String.format("Failed to load value %s for module %s", value.getName(), module.getName()));
                                }
                            }
                        }
                    }

                    if (object.has("toggled")) {
                        JsonElement toggled = object.get("toggled");
                        if (toggled != null && toggled.isJsonPrimitive()) {
                            module.setEnabled(toggled.getAsBoolean());
                        }
                    }

                    if (object.has("key")) {
                        JsonElement key = object.get("key");
                        if (key != null && key.isJsonPrimitive()) {
                            module.setKey(key.getAsInt());
                        }
                    }

                    if (object.has("hidden")) {
                        JsonElement hidden = object.get("hidden");
                        if (hidden != null && hidden.isJsonPrimitive()) {
                            module.setHidden(hidden.getAsBoolean());
                        }
                    }
                }
            }
            ChatUtil.sendFormatted(String.format("%sConfig has been loaded (&a&o%s&r)&r", Myau.clientName, file.getName()));
        } catch (FileNotFoundException e) {
            ChatUtil.sendFormatted(String.format("%sConfig file not found (&c&o%s&r)&r", Myau.clientName, file.getName()));
        } catch (JsonSyntaxException e) {
            ChatUtil.sendFormatted(String.format("%sConfig has invalid JSON syntax (&c&o%s&r)&r", Myau.clientName, file.getName()));
            LOGGER.error("JSON Syntax Error: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error loading config: " + e.getMessage());
            ChatUtil.sendFormatted(String.format("%sConfig couldn't be loaded (&c&o%s&r)&r", Myau.clientName, file.getName()));
        }
    }

    public void save() {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            JsonObject object = new JsonObject();
            for (Module module : Myau.moduleManager.modules.values()) {
                JsonObject moduleObject = new JsonObject();
                moduleObject.addProperty("toggled", module.isEnabled());
                moduleObject.addProperty("key", module.getKey());
                moduleObject.addProperty("hidden", module.isHidden());

                ArrayList<Value<?>> list = Myau.valueManager.properties.get(module.getClass());
                if (list != null) {
                    for (Value<?> value : list) {
                        if (value.isDoNotIncludeAlways()) {
                            continue;
                        }
                        try {
                            value.write(moduleObject);
                        } catch (Exception e) {
                            LOGGER.warn(String.format("Failed to save value %s for module %s", value.getName(), module.getName()));
                        }
                    }
                }
                object.add(module.getName(), moduleObject);
            }

            PrintWriter printWriter = new PrintWriter(new FileWriter(file));
            printWriter.println(gson.toJson(object));
            printWriter.close();
            ChatUtil.sendFormatted(String.format("%sConfig has been saved (&a&o%s&r)&r", Myau.clientName, file.getName()));
        } catch (IOException e) {
            LOGGER.error("Error saving config: " + e.getMessage());
            ChatUtil.sendFormatted(String.format("%sConfig couldn't be saved (&c&o%s&r)&r", Myau.clientName, file.getName()));
        }
    }
}
