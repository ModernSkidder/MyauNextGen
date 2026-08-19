package laoqi123.module.modules;

import laoqi123.event.EventTarget;
import laoqi123.event.types.Priority;
import laoqi123.events.*;
import laoqi123.module.Module;
import laoqi123.util.TimerUtil;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.PercentProperty;
import laoqi123.property.properties.IntProperty;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class AutoHeal extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final TimerUtil timer = new TimerUtil();
    private boolean shouldHeal = false;
    private int prevSlot = -1;
    private int hurtTick = 0;
    public final PercentProperty health = new PercentProperty("health", 35);
    public final IntProperty delay = new IntProperty("delay", 4000, 0, 5000);
    public final BooleanProperty regenCheck = new BooleanProperty("regen-check", false);
    public final BooleanProperty hurtCheck = new BooleanProperty("hurt-check", false);
    public final IntProperty hurtTime = new IntProperty("hurt-time", 20, 1, 100, hurtCheck::getValue);

    private int findHealingItem() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && stack.getCustomName() != null) {
                String name = stack.getName().getString();
                if (stack.getItem() instanceof BlockItem
                        && ((BlockItem) stack.getItem()).getBlock() instanceof SkullBlock
                        && name.contains("§6")
                        && name.contains("Golden Head")) {
                    return i;
                }
            }
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && stack.getCustomName() != null) {
                String name = stack.getName().getString();
                if (stack.getItem() instanceof BlockItem
                        && ((BlockItem) stack.getItem()).getBlock() instanceof SkullBlock
                        && name.matches("\\S+§c's Head")) {
                    return i;
                }
            }
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && stack.getCustomName() != null) {
                String name = stack.getName().getString();
                if (stack.get(DataComponentTypes.FOOD) != null && name.contains("§6Cornucopia")) {
                    return i;
                }
                if (stack.get(DataComponentTypes.FOOD) != null
                        && (name.contains("§a") && name.contains("Tasty Soup") || name.contains("§a") && name.contains("Assist Soup"))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean hasRegenEffect() {
        return this.regenCheck.getValue() && mc.player.hasStatusEffect(StatusEffects.REGENERATION);
    }

    public AutoHeal() {
        super("AutoHeal", false);
    }

    public boolean isSwitching() {
        return this.prevSlot != -1;
    }

    @EventTarget(Priority.HIGH)
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            this.prevSlot = -1;
        } else {
            if (hurtCheck.getValue()){
                if (hurtTick > 0) hurtTick--;
                if (mc.player.hurtTime > 0) {
                    hurtTick = hurtTime.getValue();
                }
            } else {
                hurtTick = 1;
            }
            switch (event.getType()) {
                case PRE:
                    boolean percent = (float) Math.ceil(mc.player.getHealth() + mc.player.getAbsorptionAmount()) / mc.player.getMaxHealth()
                            <= (float) this.health.getValue() / 100.0F;
                    if (this.shouldHeal
                            && percent
                            && !this.hasRegenEffect()
                            && this.timer.hasTimeElapsed(this.delay.getValue())
                            && hurtTick > 0) {
                        int slot = this.findHealingItem();
                        if (slot != -1) {
                            this.prevSlot = mc.player.getInventory().selectedSlot;
                            mc.player.getInventory().selectedSlot = slot;
                            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                            this.timer.reset();
                        }
                    }
                    this.shouldHeal = percent;
                    break;
                case POST:
                    if (this.prevSlot != -1) {
                        mc.player.getInventory().selectedSlot = this.prevSlot;
                        this.prevSlot = -1;
                    }
            }
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }
}
