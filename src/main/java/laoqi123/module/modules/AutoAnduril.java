package laoqi123.module.modules;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.events.TickEvent;
import laoqi123.module.Module;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.util.ItemUtil;
import laoqi123.util.KeyBindUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.HitResult;

public class AutoAnduril extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private int previousSlot = -1;
    private int currentSlot = -1;
    private int intervalTick = -1;
    private int holdTick = -1;
    public final IntProperty interval = new IntProperty("interval", 40, 0, 100);
    public final IntProperty hold = new IntProperty("hold", 1, 0, 20);
    public final BooleanProperty speedCheck = new BooleanProperty("speed-check", false);
    public final IntProperty debug = new IntProperty("debug", 0, 0, 9);

    public AutoAnduril() {
        super("AutoAnduril", false);
    }

    public boolean canSwap() {
        if (mc.crosshairTarget != null
                && mc.crosshairTarget.getType() == HitResult.Type.BLOCK
                && KeyBindUtil.isKeyDown(mc.options.attackKey)) return false;
        ItemStack currentItem = mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot);
        if (currentItem != null) {
            if (currentItem.getItem() instanceof BlockItem && KeyBindUtil.isKeyDown(mc.options.useKey)) return false;
            if (!(currentItem.getItem() instanceof SwordItem) && mc.player.isUsingItem()) return false;
        }
        InvWalk invWalk = (InvWalk) Myau.moduleManager.modules.get(InvWalk.class);
        return mc.currentScreen == null || mc.currentScreen instanceof laoqi123.ui.ClickGui
                || invWalk.isEnabled() && invWalk.canInvWalk();
    }

    public boolean hasSpeed() {
        if (!speedCheck.getValue()) return false;
        StatusEffectInstance potionEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
        if (potionEffect == null) return false;
        return (potionEffect.getAmplifier() > 0);
    }

    @EventTarget(Priority.LOWEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.currentSlot != -1 && this.currentSlot != mc.player.getInventory().selectedSlot) {
                this.currentSlot = -1;
                this.previousSlot = -1;
                this.intervalTick = interval.getValue();
                this.holdTick = -1;
            }

            if (this.intervalTick > 0) {
                this.intervalTick--;
            } else if (intervalTick == 0) {
                if (canSwap() && !hasSpeed()) {
                    int slot = ItemUtil.findAndurilHotbarSlot(mc.player.getInventory().selectedSlot);
                    if (debug.getValue() != 0 && slot == -1) slot = debug.getValue() - 1;
                    if (slot != -1 && slot != mc.player.getInventory().selectedSlot) {
                        this.previousSlot = mc.player.getInventory().selectedSlot;
                        this.currentSlot = mc.player.getInventory().selectedSlot = slot;
                        this.intervalTick = -1;
                        this.holdTick = hold.getValue();
                        return;
                    } else {
                        this.intervalTick = interval.getValue();
                        this.holdTick = -1;
                    }
                }
            }
            if (this.holdTick > 0) {
                this.holdTick--;
            } else if (holdTick == 0) {
                if (this.previousSlot != -1 && canSwap()) {
                    mc.player.getInventory().selectedSlot = this.previousSlot;
                    this.previousSlot = -1;
                    this.holdTick = -1;
                    this.intervalTick = interval.getValue();
                }
            }
        }
    }

    @Override
    public void onEnabled() {
        this.previousSlot = -1;
        this.currentSlot = -1;
        this.intervalTick = this.interval.getValue();
        this.holdTick = -1;
    }

    @Override
    public void onDisabled() {
        this.previousSlot = -1;
        this.currentSlot = -1;
        this.intervalTick = -1;
        this.holdTick = -1;
    }
}
