package laoqi123.module.modules;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.TickEvent;
import laoqi123.module.Module;
import laoqi123.util.ItemUtil;
import laoqi123.util.KeyBindUtil;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.util.TeamUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class AutoTool extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private int currentToolSlot = -1;
    private int previousSlot = -1;
    private int tickDelayCounter = 0;
    public final IntProperty switchDelay = new IntProperty("delay", 0, 0, 5);
    public final BooleanProperty switchBack = new BooleanProperty("switch-back", true);
    public final BooleanProperty sneakOnly = new BooleanProperty("sneak-only", true);

    public AutoTool() {
        super("AutoTool", false);
    }

    public boolean isKillAura() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (!killAura.isEnabled()) return false;
        return TeamUtil.isEntityLoaded(killAura.getTarget()) && killAura.isAttackAllowed();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.currentToolSlot != -1 && this.currentToolSlot != mc.player.getInventory().selectedSlot) {
                this.currentToolSlot = -1;
                this.previousSlot = -1;
            }
            if (mc.crosshairTarget != null
                    && mc.crosshairTarget.getType() == HitResult.Type.BLOCK
                    && mc.options.attackKey.isPressed()
                    && !mc.player.isUsingItem()
                    && !isKillAura()) {
                if (this.tickDelayCounter >= this.switchDelay.getValue()
                        && (!(Boolean) this.sneakOnly.getValue() || KeyBindUtil.isKeyDown(mc.options.sneakKey))) {
                    int slot = ItemUtil.findInventorySlot(
                            mc.player.getInventory().selectedSlot, mc.world.getBlockState(((BlockHitResult) mc.crosshairTarget).getBlockPos()).getBlock()
                    );
                    if (mc.player.getInventory().selectedSlot != slot) {
                        if (this.previousSlot == -1) {
                            this.previousSlot = mc.player.getInventory().selectedSlot;
                        }
                        mc.player.getInventory().selectedSlot = this.currentToolSlot = slot;
                    }
                }
                this.tickDelayCounter++;
            } else {
                if (this.switchBack.getValue() && this.previousSlot != -1) {
                    mc.player.getInventory().selectedSlot = this.previousSlot;
                }
                this.currentToolSlot = -1;
                this.previousSlot = -1;
                this.tickDelayCounter = 0;
            }
        }
    }

    @Override
    public void onDisabled() {
        this.currentToolSlot = -1;
        this.previousSlot = -1;
        this.tickDelayCounter = 0;
    }
}
