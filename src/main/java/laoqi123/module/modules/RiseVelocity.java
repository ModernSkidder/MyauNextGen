package laoqi123.module.modules;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.AttackEvent;
import laoqi123.events.MoveInputEvent;
import laoqi123.events.PacketEvent;
import laoqi123.events.PlayerUpdateEvent;
import laoqi123.events.TickEvent;
import laoqi123.module.Module;
import laoqi123.module.modules.risevelocity.RiseVelocityMode;
import laoqi123.module.modules.risevelocity.impl.BounceVelocity;
import laoqi123.module.modules.risevelocity.impl.DelayVelocity;
import laoqi123.module.modules.risevelocity.impl.GrimReduceVelocity;
import laoqi123.module.modules.risevelocity.impl.GrimVelocity;
import laoqi123.module.modules.risevelocity.impl.GroundVelocity;
import laoqi123.module.modules.risevelocity.impl.IntaveVelocity;
import laoqi123.module.modules.risevelocity.impl.LegitVelocity;
import laoqi123.module.modules.risevelocity.impl.StandardVelocity;
import laoqi123.module.modules.risevelocity.impl.TickVelocity;
import laoqi123.property.Property;
import laoqi123.property.properties.ModeProperty;
import laoqi123.util.config.PropertyProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class RiseVelocity extends Module implements PropertyProvider {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{
            "Standard", "Delay", "Legit", "Ground", "Tick", "Bounce", "Intave", "Grim Reduce", "Grim"
    });

    private final List<RiseVelocityMode> modes = new ArrayList<>();
    private int ticksSinceAttack;
    private int sinceTeleport;
    private int jumpTicks;

    public RiseVelocity() {
        super("RiseVelocity", false);
        this.modes.add(new StandardVelocity());
        this.modes.add(new DelayVelocity());
        this.modes.add(new LegitVelocity());
        this.modes.add(new GroundVelocity());
        this.modes.add(new TickVelocity());
        this.modes.add(new BounceVelocity());
        this.modes.add(new IntaveVelocity());
        this.modes.add(new GrimReduceVelocity());
        this.modes.add(new GrimVelocity());
        for (RiseVelocityMode riseVelocityMode : this.modes) {
            riseVelocityMode.setParent(this);
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
        this.ticksSinceAttack = 0;
        this.sinceTeleport = 0;
        this.jumpTicks = 0;
        for (RiseVelocityMode riseVelocityMode : this.modes) {
            riseVelocityMode.onEnable();
        }
    }

    @Override
    public void onDisabled() {
        for (RiseVelocityMode riseVelocityMode : this.modes) {
            riseVelocityMode.onDisable();
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
        Myau.propertyManager.properties.put(RiseVelocity.class, list);
    }

    private List<Property<?>> collectModeProperties() {
        List<Property<?>> properties = new ArrayList<>();
        RiseVelocityMode current = this.getCurrentMode();
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

    public RiseVelocityMode getCurrentMode() {
        int index = this.mode.getValue();
        return index >= 0 && index < this.modes.size() ? this.modes.get(index) : this.modes.get(0);
    }

    public int getTicksSinceTeleport() {
        return this.sinceTeleport;
    }

    public int getTicksSinceAttack() {
        return this.ticksSinceAttack;
    }

    public int getJumpTicks() {
        return this.jumpTicks;
    }

    @EventTarget
    public void onPacketReceive(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.RECEIVE) {
            return;
        }
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.sinceTeleport = 0;
        }
        if (mc.player == null || mc.world == null) {
            return;
        }
        this.getCurrentMode().onPacketReceive(event);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE || mc.player == null) {
            return;
        }
        if (this.sinceTeleport < 10000) {
            this.sinceTeleport++;
        }
        if (this.jumpTicks > 0) {
            this.jumpTicks--;
        }
        this.getCurrentMode().onTick(event);
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || mc.player == null) {
            return;
        }
        if (event.isJumpModified() && event.getJump()) {
            this.jumpTicks = 10;
        }
        this.getCurrentMode().onMoveInput(event);
    }

    @EventTarget
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (!this.isEnabled() || mc.player == null) {
            return;
        }
        if (this.ticksSinceAttack < 200) {
            this.ticksSinceAttack++;
        }
        this.getCurrentMode().onPlayerUpdate(event);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        this.ticksSinceAttack = 0;
    }
}