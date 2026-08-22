package laoqi123.management;

import laoqi123.enums.FloatModules;
import laoqi123.event.EventTarget;
import laoqi123.events.PlayerUpdateEvent;
import net.minecraft.client.MinecraftClient;

import java.util.LinkedHashMap;

public class FloatManager {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final LinkedHashMap<FloatModules, Boolean> activeMap;
    private boolean floating;

    public FloatManager() {
        this.activeMap = new LinkedHashMap<>();
        this.floating = false;
    }

    public boolean isPredicted() {
        return this.floating;
    }

    public boolean isFalling() {
        return mc.player.isOnGround() && mc.player.getY() - mc.player.prevY < 0.0 && mc.player.getVelocity().y < 0.0;
    }

    public boolean hasActiveModule() {
        return this.activeMap.containsValue(true);
    }

    public void setFloatState(boolean state, FloatModules floatModules) {
        this.activeMap.put(floatModules, state);
    }

    @EventTarget
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if ((this.hasActiveModule() || this.isPredicted()) && this.isFalling()) {
            mc.player.setPosition(mc.player.getX(), mc.player.getY() + 0.001, mc.player.getZ());
            this.floating = true;
        } else {
            this.floating = false;
        }
    }
}
