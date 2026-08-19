package laoqi123.module.modules.render;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.events.LoadWorldEvent;
import laoqi123.events.PacketEvent;
import laoqi123.events.Render3DEvent;
import laoqi123.module.Module;
import laoqi123.module.modules.render.chestesp.ChestESPMode;
import laoqi123.module.modules.render.chestesp.impl.LiquidBounceChestESP;
import laoqi123.module.modules.render.chestesp.impl.SimpleChestESP;
import laoqi123.property.Property;
import laoqi123.property.properties.ModeProperty;
import laoqi123.util.config.PropertyProvider;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ChestESP extends Module implements PropertyProvider {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeProperty mode = new ModeProperty("Mode", 1, new String[]{"Simple", "LiquidBounce"});

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
    public List<Property<?>> getAdditionalProperties() {
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
        if (Myau.propertyManager == null) {
            return;
        }
        ArrayList<Property<?>> list = new ArrayList<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object object = field.get(this);
                if (object instanceof Property<?>) {
                    list.add((Property<?>) object);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        list.addAll(this.collectModeProperties());
        Myau.propertyManager.properties.put(ChestESP.class, list);
    }

    private List<Property<?>> collectModeProperties() {
        List<Property<?>> properties = new ArrayList<>();
        ChestESPMode current = this.getCurrentMode();
        if (current == null) {
            return properties;
        }
        for (Field field : current.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object object = field.get(current);
                if (object instanceof Property<?>) {
                    Property<?> property = (Property<?>) object;
                    property.setOwner(this);
                    properties.add(property);
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