package laoqi123.module.modules.combat.antikb;

import laoqi123.event.impl.LoadWorldEvent;
import laoqi123.event.impl.PacketEvent;
import laoqi123.event.impl.Render2DEvent;
import laoqi123.event.impl.StrafeEvent;
import laoqi123.event.impl.TickEvent;
import laoqi123.module.modules.combat.Velocity;

/**
 * Extensible mode base for the Velocity (anti-knockback) module.
 * <p>
 * A concrete mode overrides whichever callbacks it needs; the module
 * delegates its events to the currently selected mode. Add new modes by
 * extending this class and registering them in {@link Velocity}.
 */
public abstract class AntiKBMode {
    protected Velocity parent;

    public void setParent(Velocity parent) {
        this.parent = parent;
    }

    public Velocity getParent() {
        return this.parent;
    }

    public String getName() {
        return "";
    }

    public boolean isActive() {
        return false;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onPacket(PacketEvent event) {
    }

    public void onTick(TickEvent event) {
    }

    public void onStrafe(StrafeEvent event) {
    }

    public void onRender2D(Render2DEvent event) {
    }

    public void onLoadWorld(LoadWorldEvent event) {
    }
}
