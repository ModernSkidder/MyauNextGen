package laoqi123.module.modules.combat;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.LoadWorldEvent;
import laoqi123.event.impl.PacketEvent;
import laoqi123.event.impl.Render2DEvent;
import laoqi123.event.impl.StrafeEvent;
import laoqi123.event.impl.TickEvent;
import laoqi123.module.Module;
import laoqi123.module.modules.combat.antikb.AntiKBMode;
import laoqi123.module.modules.combat.antikb.NoXZMode;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.IntValue;
import laoqi123.value.properties.ModeValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Velocity — anti-knockback module.
 * <p>
 * Extensible through {@link AntiKBMode}: each mode implements a distinct
 * anti-velocity strategy, selected with the "Mode" setting and dispatched to
 * {@link #getCurrentMode()}. Settings below are NoXZ-scoped — each one carries
 * a visibility check keyed on the selected mode (same secondary-menu pattern as
 * BlockHit), so when a new mode is added at another index, its settings can be
 * gated with {@code () -> this.mode.getValue() == N}.
 */
public class Velocity extends Module {
    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"NoXZ"});

    public final BooleanValue debugLog = new BooleanValue("Debug Log", false, () -> this.mode.getValue() == 0);
    public final IntValue maxDelayTicks = new IntValue("Max Delay Ticks", 20, 5, 100, () -> this.mode.getValue() == 0);
    public final BooleanValue instantAttack = new BooleanValue("Instant Attack", true, () -> this.mode.getValue() == 0);
    public final BooleanValue tickManipulation = new BooleanValue("Tick Manipulation", false, () -> this.mode.getValue() == 0 && this.instantAttack.getValue());
    public final BooleanValue requireKillAura = new BooleanValue("Require KillAura", true, () -> this.mode.getValue() == 0);
    public final BooleanValue autoAttackCount = new BooleanValue("Auto Attack Count", true, () -> this.mode.getValue() == 0);
    public final IntValue attackAmount = new IntValue("Attack Amount", 3, 1, 10, () -> this.mode.getValue() == 0);
    public final BooleanValue sprintStateCheck = new BooleanValue("Sprint State Check", true, () -> this.mode.getValue() == 0);
    public final BooleanValue renderBar = new BooleanValue("Render Bar", true, () -> this.mode.getValue() == 0);

    private final List<AntiKBMode> modes = new ArrayList<>();

    public Velocity() {
        super("Velocity", false);
        this.modes.add(new NoXZMode());
        for (AntiKBMode antiKBMode : this.modes) {
            antiKBMode.setParent(this);
        }
    }

    public AntiKBMode getCurrentMode() {
        int index = this.mode.getValue();
        return index >= 0 && index < this.modes.size() ? this.modes.get(index) : this.modes.get(0);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }

    @Override
    public void onEnabled() {
        for (AntiKBMode antiKBMode : this.modes) {
            antiKBMode.onEnable();
        }
    }

    @Override
    public void onDisabled() {
        for (AntiKBMode antiKBMode : this.modes) {
            antiKBMode.onDisable();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        this.getCurrentMode().onPacket(event);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }
        this.getCurrentMode().onTick(event);
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        this.getCurrentMode().onStrafe(event);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        this.getCurrentMode().onRender2D(event);
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.getCurrentMode().onLoadWorld(event);
    }
}
