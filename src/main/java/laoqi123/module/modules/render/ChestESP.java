package laoqi123.module.modules.render;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.impl.LoadWorldEvent;
import laoqi123.event.impl.PacketEvent;
import laoqi123.event.impl.Render3DEvent;
import laoqi123.module.Module;
import laoqi123.module.modules.render.chestesp.ChestESPMode;
import laoqi123.module.modules.render.chestesp.impl.LiquidBounceChestESP;
import laoqi123.module.modules.render.chestesp.impl.SimpleChestESP;
import laoqi123.value.Value;
import laoqi123.value.properties.ModeValue;
import laoqi123.util.config.PropertyProvider;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ChestESP extends Module implements PropertyProvider {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeValue mode = new ModeValue("Mode", 1, new String[]{"Simple", "LiquidBounce"});

    private final List<ChestESPMode> modes = new ArrayList<>();

    public ChestESP() {
        super("ChestESP", false);
        this.modes.add(new SimpleChestESP());
        this.modes.add(new LiquidBounceChestESP());
        for (ChestESPMode chestESPMode : this.modes) {
            chestESPMode.setParent(this);
        }
        this.mode.setChangeListener(value -> this.rebuildSettings());
        this.rebuildSettings();
    }

    @Override
    public List<Value<?>> getAdditionalProperties() {
        return this.collectModeProperties();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }

    @Override
    public void onEnabled() {
        for (ChestESPMode chestESPMode : this.modes) {
            chestESPMode.onEnable();
        }
    }

    @Override
    public void onDisabled() {
        for (ChestESPMode chestESPMode : this.modes) {
            chestESPMode.onDisable();
        }
    }

    private void rebuildSettings() {
        if (Myau.valueManager == null) {
            return;
        }
        ArrayList<Value<?>> list = new ArrayList<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object object = field.get(this);
                if (object instanceof Value<?>) {
                    list.add((Value<?>) object);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        list.addAll(this.collectModeProperties());
        Myau.valueManager.properties.put(ChestESP.class, list);
    }

    private List<Value<?>> collectModeProperties() {
        List<Value<?>> properties = new ArrayList<>();
        ChestESPMode current = this.getCurrentMode();
        if (current == null) {
            return properties;
        }
        for (Field field : current.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object object = field.get(current);
                if (object instanceof Value<?>) {
                    Value<?> value = (Value<?>) object;
                    value.setOwner(this);
                    properties.add(value);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return properties;
    }

    public ChestESPMode getCurrentMode() {
        int index = this.mode.getValue();
        return index >= 0 && index < this.modes.size() ? this.modes.get(index) : this.modes.get(0);
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (!this.isEnabled() || mc.world == null) {
            return;
        }
        this.getCurrentMode().onRender3D(event);
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.getCurrentMode().onLoadWorld(event);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        this.getCurrentMode().onPacket(event);
    }
}