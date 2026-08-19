package laoqi123.module.modules.chestesp;

import laoqi123.events.LoadWorldEvent;
import laoqi123.events.PacketEvent;
import laoqi123.events.Render3DEvent;
import laoqi123.module.Module;

public abstract class ChestESPMode {
    protected Module parent;

    public void setParent(Module parent) {
        this.parent = parent;
    }

    public Module getParent() {
        return this.parent;
    }

    public void onRender3D(Render3DEvent event) {
    }

    public void onPacket(PacketEvent event) {
    }

    public void onLoadWorld(LoadWorldEvent event) {
    }

    public void onEnable() {
    }

    public void onDisable() {
    }
}