package laoqi123.module.modules.movement;

import laoqi123.Myau;
import laoqi123.enums.FloatModules;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.event.impl.*;
import laoqi123.module.Module;
import laoqi123.module.modules.combat.KillAura;
import laoqi123.value.properties.*;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.IntValue;
import laoqi123.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

public class NoSlow extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeValue swordMode = new ModeValue("Sword Mode", 1, new String[]{"None", "Vanilla", "Hypixel"});
    public final IntValue swapDelay = new IntValue("Swap Delay", 0, 0, 3, () -> swordMode.getValue() == 2);
    public final BooleanValue noAttack = new BooleanValue("No Attack", false, () -> swordMode.getValue() == 2);
    public final PercentValue swordMotion = new PercentValue("Sword Motion", 100, () -> this.swordMode.getValue() != 0);
    public final BooleanValue swordSprint = new BooleanValue("Sword Sprint", true, () -> this.swordMode.getValue() != 0);
    public final BooleanValue onlyKillAuraAutoBlock = new BooleanValue("Only Kill Aura Auto Block", false, () -> this.swordMode.getValue() != 0);
    public final ModeValue foodMode = new ModeValue("Food Mode", 0, new String[]{"None", "Vanilla", "Float"});
    public final PercentValue foodMotion = new PercentValue("Food Motion", 100, () -> this.foodMode.getValue() != 0);
    public final BooleanValue foodSprint = new BooleanValue("Food Sprint", true, () -> this.foodMode.getValue() != 0);
    public final ModeValue bowMode = new ModeValue("Bow Mode", 0, new String[]{"None", "Vanilla", "Float"});
    public final PercentValue bowMotion = new PercentValue("Bow Motion", 100, () -> this.bowMode.getValue() != 0);
    public final BooleanValue bowSprint = new BooleanValue("Bow Sprint", true, () -> this.bowMode.getValue() != 0);

    private int delay = 0;
    private boolean post = false;

    public NoSlow() {
        super("NoSlow", false);
    }

    public boolean isSwordActive() {
        return this.swordMode.getValue() != 0 && ItemUtil.isHoldingSword() && (!this.onlyKillAuraAutoBlock.getValue() || this.isKillAuraAutoBlocking());
    }

    public boolean isFoodActive() {
        return this.foodMode.getValue() != 0 && ItemUtil.isEating();
    }

    public boolean isBowActive() {
        return this.bowMode.getValue() != 0 && ItemUtil.isUsingBow();
    }

    public boolean isFloatMode() {
        return this.foodMode.getValue() == 2 && ItemUtil.isEating()
                || this.bowMode.getValue() == 2 && ItemUtil.isUsingBow();
    }

    private boolean isKillAuraAutoBlocking() {
        KillAura aura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (!aura.isPlayerBlocking() || !aura.isEnabled()) {
            return false;
        }
        return aura.isBlocking();
    }

    public boolean isAnyActive() {
        if (this.swordMode.getValue() != 2) {
            return mc.player.isUsingItem() && (this.isSwordActive() || this.isFoodActive() || this.isBowActive());
        } else if (this.swordMode.getValue() == 2 && isSwordActive()) {
            KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
            if (!noAttack.getValue() || !((killAura.getAutoBlock().getBlockTick() == 0 && killAura.getAutoBlock().mode.getValue() == 2) || (killAura.getAutoBlock().mode.getValue() == 6 && killAura.getAutoBlock().getBlockTick() == killAura.getAutoBlock().attackTick.getValue()) || (killAura.getAutoBlock().mode.getValue() != 6 && killAura.getAutoBlock().mode.getValue() != 2) || (killAura.getAutoBlock().mode.getValue() == 5 && killAura.getAutoBlock().getBlockTick() == 0) && killAura.isEnabled() && killAura.isPlayerBlocking())) {
                return delay == 0;
            }
        }
        return false;
    }

    public boolean canSprint() {
        return this.isSwordActive() && this.swordSprint.getValue()
                || this.isFoodActive() && this.foodSprint.getValue()
                || this.isBowActive() && this.bowSprint.getValue();
    }

    public int getMotionMultiplier() {
        if (ItemUtil.isHoldingSword()) {
            return this.swordMotion.getValue();
        } else if (ItemUtil.isEating()) {
            return this.foodMotion.getValue();
        } else {
            return ItemUtil.isUsingBow() ? this.bowMotion.getValue() : 100;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) return;
        if (ItemUtil.isHoldingSword() && mc.player.isUsingItem()) {
            if (isSwordActive()) {
                if (this.swordMode.getValue() == 2) {
                    if (event.getType() == EventType.PRE) {
                        delay--;
                        if (delay < 0) {
                            KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
                            if (!noAttack.getValue() || !((killAura.getAutoBlock().getBlockTick() == 0 && killAura.getAutoBlock().mode.getValue() == 2) || (killAura.getAutoBlock().mode.getValue() == 6 && killAura.getAutoBlock().getBlockTick() == killAura.getAutoBlock().attackTick.getValue()) || (killAura.getAutoBlock().mode.getValue() != 6 && killAura.getAutoBlock().mode.getValue() != 2) || (killAura.getAutoBlock().mode.getValue() == 5 && killAura.getAutoBlock().getBlockTick() == 0) && killAura.isEnabled() && killAura.isPlayerBlocking())) {
                                int randomSlot = new Random().nextInt(9);
                                while (randomSlot == mc.player.getInventory().selectedSlot) {
                                    randomSlot = new Random().nextInt(9);
                                }
                                PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(randomSlot));
                                PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                            }
                            post = true;
                            delay = swapDelay.getValue();
                        }
                    }
                }
            }
        } else {
            if (post) {
                post = false;
            }
        }
    }

    @EventTarget
    public void onMotion(PostMotionEvent event) {
        if (!this.isEnabled()) return;
        if (!ItemUtil.isHoldingSword() || !mc.player.isUsingItem()) return;
        if (isSwordActive()) {
            if (this.swordMode.getValue() == 2) {
                if (post) {
                    post = false;
                }
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && this.isAnyActive()) {
            float multiplier = (float) this.getMotionMultiplier() / 100.0F;
            mc.player.input.movementForward *= multiplier;
            mc.player.input.movementSideways *= multiplier;
            if (!this.canSprint()) {
                mc.player.setSprinting(false);
            }
        }
    }

    @EventTarget(Priority.LOW)
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.isEnabled() && this.isFloatMode()) {
            int item = mc.player.getInventory().selectedSlot;
            Myau.floatManager.setFloatState(true, FloatModules.NO_SLOW);
        } else {
            Myau.floatManager.setFloatState(false, FloatModules.NO_SLOW);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            if (mc.crosshairTarget != null) {
                switch (mc.crosshairTarget.getType()) {
                    case BLOCK:
                        if (mc.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult) {
                            BlockPos blockPos = ((net.minecraft.util.hit.BlockHitResult) mc.crosshairTarget).getBlockPos();
                            if (BlockUtil.isInteractable(blockPos) && !PlayerUtil.isSneaking()) {
                                return;
                            }
                        }
                        break;
                    case ENTITY:
                        if (mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult) {
                            Entity entityHit = ((net.minecraft.util.hit.EntityHitResult) mc.crosshairTarget).getEntity();
                            if (entityHit instanceof VillagerEntity) {
                                return;
                            }
                            if (entityHit instanceof LivingEntity && TeamUtil.isShop((LivingEntity) entityHit)) {
                                return;
                            }
                        }
                }
            }
            if (this.isFloatMode() && !Myau.floatManager.isPredicted() && mc.player.isOnGround()) {
                event.setCancelled(true);
                mc.player.setVelocity(mc.player.getVelocity().x, 0.42F, mc.player.getVelocity().z);
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{swordMotion.getValue() + "%"};
    }
}
