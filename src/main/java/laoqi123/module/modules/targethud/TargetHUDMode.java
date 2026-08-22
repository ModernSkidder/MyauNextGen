package laoqi123.module.modules.targethud;

import laoqi123.module.modules.TargetHud2;

public abstract class TargetHUDMode {
    private final String name;

    public TargetHUDMode(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public abstract float[] getSize(TargetHud2 targetHUD, TargetHud2.RenderData data);

    public abstract void render(TargetHud2 targetHUD, TargetHud2.RenderData data, float x, float y);

    public boolean shouldRenderEffects(TargetHud2 targetHUD) {
        return false;
    }

    public void renderMask(TargetHud2 targetHUD, TargetHud2.RenderData data, float x, float y, int color) {
    }

    public boolean shouldAnimateHealth() {
        return true;
    }
}