package laoqi123.module.modules.player;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.UpdateEvent;
import laoqi123.event.impl.WindowClickEvent;
import laoqi123.module.Module;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.IntValue;
import laoqi123.util.ItemUtil;
import laoqi123.util.RandomUtil;
import laoqi123.util.TimerUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.*;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.world.GameMode;

import java.util.*;

public class InvManager extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final IntValue minDelay = new IntValue("min-delay", 1, 0, 20);
    public final IntValue maxDelay = new IntValue("max-delay", 2, 0, 20);
    public final IntValue openDelay = new IntValue("open-delay", 1, 0, 20);
    public final BooleanValue autoArmor = new BooleanValue("auto-armor", true);
    public final IntValue autoArmorInterval = new IntValue("auto-armor-interval", 0, 0, 100, this.autoArmor::getValue);
    public final BooleanValue dropTrash = new BooleanValue("drop-trash", false);
    public final BooleanValue checkDurability = new BooleanValue("check-durability", true);
    public final BooleanValue keepWaterBucket = new BooleanValue("keep-water-bucket", true);
    public final BooleanValue keepLavaBucket = new BooleanValue("keep-lava-bucket", true);
    public final IntValue swordSlot = new IntValue("sword-slot", 1, 0, 9);
    public final IntValue pickaxeSlot = new IntValue("pickaxe-slot", 3, 0, 9);
    public final IntValue shovelSlot = new IntValue("shovel-slot", 4, 0, 9);
    public final IntValue axeSlot = new IntValue("axe-slot", 5, 0, 9);
    public final IntValue blocksSlot = new IntValue("blocks-slot", 2, 0, 9);
    public final IntValue blocks = new IntValue("blocks", 128, 64, 2304);
    public final IntValue projectileSlot = new IntValue("projectile-slot", 7, 0, 9);
    public final IntValue projectiles = new IntValue("projectiles", 64, 16, 2304);
    public final IntValue goldAppleSlot = new IntValue("gold-apple-slot", 9, 0, 9);
    public final IntValue arrow = new IntValue("arrow", 256, 0, 2304);
    public final IntValue pearlSlot = new IntValue("pearl-slot", 6, 0, 9);
    public final IntValue fireChargeSlot = new IntValue("fire-charge-slot", 7, 0, 9);
    public final IntValue fireCharges = new IntValue("fire-charges", 64, 0, 2304);
    public final IntValue windCharges = new IntValue("wind-charges", 64, 0, 2304);
    public final IntValue mace = new IntValue("mace", 1, 0, 4);
    public final IntValue bowSlot = new IntValue("bow-slot", 8, 0, 9);
    public final BooleanValue autoClose = new BooleanValue("auto-close", false);
    public final BooleanValue autoOffhand = new BooleanValue("auto-offhand", false);
    private final TimerUtil autoArmorTime = new TimerUtil();
    private int actionDelay = 0;
    private int oDelay = 0;
    private boolean inventoryOpen = false;
    private boolean clickedThisTick = false;

    public InvManager() {
        super("InvManager", false);
    }

    private boolean isValidGameMode() {
        GameMode gameType = mc.interactionManager.getCurrentGameMode();
        return gameType == GameMode.SURVIVAL || gameType == GameMode.ADVENTURE;
    }

    private int convertSlotIndex(int slot) {
        if (slot >= 36) return 8 - (slot - 36);
        return slot <= 8 ? slot + 36 : slot;
    }

    private void clickSlot(int windowId, int slotId, int mouseButtonClicked, int mode) {
        SlotActionType action = SlotActionType.PICKUP;
        switch (mode) {
            case 1: action = SlotActionType.QUICK_MOVE; break;
            case 2: action = SlotActionType.SWAP; break;
            case 4: action = SlotActionType.THROW; break;
            case 3: action = SlotActionType.CLONE; break;
        }
        mc.interactionManager.clickSlot(windowId, slotId, mouseButtonClicked, action, mc.player);
        this.clickedThisTick = true;
    }

    private int getStackSize(int slot) {
        if (slot == -1) return 0;
        ItemStack stack = mc.player.getInventory().getStack(slot);
        return stack != null ? stack.getCount() : 0;
    }

    private int nextDelay() {
        if (maxDelay.getValue() == 0) return 0;
        return RandomUtil.nextInt(minDelay.getValue() + 1, maxDelay.getValue() + 1);
    }

    private boolean isWaterBucket(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET;
    }

    private boolean isLavaBucket(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == Items.LAVA_BUCKET;
    }

    private int findPearlSlot(int preferredSlot) {
        if (preferredSlot >= 0 && preferredSlot <= 8) {
            ItemStack stack = mc.player.getInventory().getStack(preferredSlot);
            if (stack != null && !stack.isEmpty() && stack.getItem() == Items.ENDER_PEARL) return preferredSlot;
        }
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && !stack.isEmpty() && stack.getItem() == Items.ENDER_PEARL) return i;
        }
        return -1;
    }

    private int findOffhandAppleSlot() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && !stack.isEmpty() && stack.getItem() == Items.GOLDEN_APPLE && !stack.hasEnchantments()) {
                return i;
            }
        }
        return -1;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.PRE) return;

        if (this.actionDelay > 0) this.actionDelay--;
        if (this.oDelay > 0) this.oDelay--;

        this.clickedThisTick = false;

        if (!(mc.currentScreen instanceof InventoryScreen)) {
            this.inventoryOpen = false;
            return;
        }
        if (!(mc.player.playerScreenHandler instanceof PlayerScreenHandler)) {
            this.inventoryOpen = false;
            return;
        }

        if (!this.inventoryOpen) {
            this.inventoryOpen = true;
            this.oDelay = this.openDelay.getValue() + 1;
            this.autoArmorTime.reset();
            return;
        }

        if (this.oDelay > 0 || this.actionDelay > 0) return;
        if (!this.isEnabled() || !this.isValidGameMode()) return;

        ArrayList<Integer> equippedArmorSlots = new ArrayList<>(Arrays.asList(-1, -1, -1, -1));
        ArrayList<Integer> inventoryArmorSlots = new ArrayList<>(Arrays.asList(-1, -1, -1, -1));
        for (int i = 0; i < 4; i++) {
            equippedArmorSlots.set(i, ItemUtil.findArmorInventorySlot(i, true));
            inventoryArmorSlots.set(i, ItemUtil.findArmorInventorySlot(i, false));
        }

        int prefSword = swordSlot.getValue() - 1;
        int invSword = ItemUtil.findSwordInInventorySlot(prefSword, checkDurability.getValue());
        if (invSword == -1) invSword = ItemUtil.findSwordInInventorySlot(prefSword, false);

        int prefPick = pickaxeSlot.getValue() - 1;
        int invPick = ItemUtil.findInventorySlot("pickaxe", prefPick, checkDurability.getValue());
        if (invPick == -1) invPick = ItemUtil.findInventorySlot("pickaxe", prefPick, false);

        int prefShovel = shovelSlot.getValue() - 1;
        int invShovel = ItemUtil.findInventorySlot("shovel", prefShovel, checkDurability.getValue());
        if (invShovel == -1) invShovel = ItemUtil.findInventorySlot("shovel", prefShovel, false);

        int prefAxe = axeSlot.getValue() - 1;
        int invAxe = ItemUtil.findInventorySlot("axe", prefAxe, checkDurability.getValue());
        if (invAxe == -1) invAxe = ItemUtil.findInventorySlot("axe", prefAxe, false);

        int prefBlock = blocksSlot.getValue() - 1;
        int invBlock = ItemUtil.findInventorySlot(prefBlock, ItemUtil.ItemType.Block);

        int prefProj = projectileSlot.getValue() - 1;
        int invProj = ItemUtil.findInventorySlot(prefProj, ItemUtil.ItemType.Projectile);
        if (invProj == -1) invProj = ItemUtil.findInventorySlot(prefProj, ItemUtil.ItemType.FishRod);

        int prefApple = goldAppleSlot.getValue() - 1;
        int invApple = ItemUtil.findInventorySlot(prefApple, ItemUtil.ItemType.GoldApple);

        int prefBow = bowSlot.getValue() - 1;
        int invBow = ItemUtil.findBowInventorySlot(prefBow, checkDurability.getValue());
        if (invBow == -1) invBow = ItemUtil.findBowInventorySlot(prefBow, false);

        int prefPearl = pearlSlot.getValue() - 1;
        int invPearl = findPearlSlot(prefPearl);

        int prefFire = fireChargeSlot.getValue() - 1;
        int invFire = ItemUtil.findInventorySlot(prefFire, ItemUtil.ItemType.FireCharge);

        if (autoArmor.getValue() && autoArmorTime.hasTimeElapsed(autoArmorInterval.getValue() * 50L)) {
            for (int i = 0; i < 4; i++) {
                int eq = equippedArmorSlots.get(i);
                int inv = inventoryArmorSlots.get(i);
                if (eq != -1 || inv != -1) {
                    int armorSlot = 39 - i;
                    if (eq != armorSlot && inv != armorSlot) {
                        if (mc.player.getInventory().getStack(armorSlot) != null
                                && !mc.player.getInventory().getStack(armorSlot).isEmpty()) {
                            if (mc.player.getInventory().getEmptySlot() != -1) {
                                clickSlot(mc.player.playerScreenHandler.syncId, convertSlotIndex(armorSlot), 0, 1);
                            } else {
                                clickSlot(mc.player.playerScreenHandler.syncId, convertSlotIndex(armorSlot), 1, 4);
                            }
                        } else {
                            int toEquip = eq != -1 ? eq : inv;
                            clickSlot(mc.player.playerScreenHandler.syncId, convertSlotIndex(toEquip), 0, 1);
                            autoArmorTime.reset();
                        }
                        int d = nextDelay();
                        if (d > 0) { actionDelay = d; return; }
                    }
                }
            }
        }

        LinkedHashSet<Integer> used = new LinkedHashSet<>();

        if (prefSword >= 0 && prefSword <= 8 && invSword != -1) {
            used.add(prefSword);
            if (invSword != prefSword) {
                clickSlot(mc.player.playerScreenHandler.syncId, convertSlotIndex(invSword), prefSword, 2);
                int d = nextDelay();
                if (d > 0) { actionDelay = d; return; }
            }
        }
        if (prefPick >= 0 && prefPick <= 8 && !used.contains(prefPick) && invPick != -1) {
            used.add(prefPick);
            if (invPick != prefPick) {
                clickSlot(mc.player.playerScreenHandler.syncId, convertSlotIndex(invPick), prefPick, 2);
                int d = nextDelay();
                if (d > 0) { actionDelay = d; return; }
            }
        }
        if (prefShovel >= 0 && prefShovel <= 8 && !used.contains(prefShovel) && invShovel != -1) {
            used.add(prefShovel);
            if (invShovel != prefShovel) {
                clickSlot(mc.player.playerScreenHandler.syncId, convertSlotIndex(invShovel), prefShovel, 2);
                int d = nextDelay();
                if (d > 0) { actionDelay = d; return; }
            }
        }
        if (prefAxe >= 0 && prefAxe <= 8 && !used.contains(prefAxe) && invAxe != -1) {
            used.add(prefAxe);
            if (invAxe != prefAxe) {
                clickSlot(mc.player.playerScreenHandler.syncId, convertSlotIndex(invAxe), prefAxe, 2);
                int d = nextDelay();
                if (d > 0) { actionDelay = d; return; }
            }
        }
        if (prefBlock >= 0 && prefBlock <= 8 && !used.contains(prefBlock) && invBlock != -1) {
            used.add(prefBlock);
            if (invBlock != prefBlock) {
                clickSlot(mc.player.playerScreenHandler.syncId, convertSlotIndex(invBlock), prefBlock, 2);
                int d = nextDelay();
                if (d > 0) { actionDelay = d; return; }
            }
        }
        if (prefProj >= 0 && prefProj <= 8 && !used.contains(prefProj) && invProj != -1) {
            used.add(prefProj);
            if (invProj != prefProj) {
                clickSlot(mc.player.playerScreenHandler.syncId, convertSlotIndex(invProj), prefProj, 2);
                int d = nextDelay();
                if (d > 0) { actionDelay = d; return; }
            }
        }
        if (prefApple >= 0 && prefApple <= 8 && !used.contains(prefApple) && invApple != -1) {
            used.add(prefApple);
            if (invApple != prefApple) {
                clickSlot(mc.player.playerScreenHandler.syncId, convertSlotIndex(invApple), prefApple, 2);
                int d = nextDelay();
                if (d > 0) { actionDelay = d; return; }
            }
        }
        if (prefBow >= 0 && prefBow <= 8 && !used.contains(prefBow) && invBow != -1) {
            used.add(prefBow);
            if (invBow != prefBow) {
                clickSlot(mc.player.playerScreenHandler.syncId, convertSlotIndex(invBow), prefBow, 2);
                int d = nextDelay();
                if (d > 0) { actionDelay = d; return; }
            }
        }
        if (prefPearl >= 0 && prefPearl <= 8 && !used.contains(prefPearl) && invPearl != -1) {
            used.add(prefPearl);
            if (invPearl != prefPearl) {
                clickSlot(mc.player.playerScreenHandler.syncId, convertSlotIndex(invPearl), prefPearl, 2);
                int d = nextDelay();
                if (d > 0) { actionDelay = d; return; }
            }
        }
        if (prefFire >= 0 && prefFire <= 8 && !used.contains(prefFire) && invFire != -1) {
            used.add(prefFire);
            if (invFire != prefFire) {
                clickSlot(mc.player.playerScreenHandler.syncId, convertSlotIndex(invFire), prefFire, 2);
                int d = nextDelay();
                if (d > 0) { actionDelay = d; return; }
            }
        }

        if (autoOffhand.getValue()) {
            ItemStack offhandStack = mc.player.getOffHandStack();
            if (offhandStack == null || offhandStack.isEmpty() || offhandStack.getItem() != Items.GOLDEN_APPLE) {
                int appleSlot = findOffhandAppleSlot();
                if (appleSlot != -1) {
                    clickSlot(mc.player.playerScreenHandler.syncId, convertSlotIndex(appleSlot), 40, 2);
                    int d = nextDelay();
                    if (d > 0) { actionDelay = d; return; }
                }
            }
        }

        if (dropTrash.getValue()) {
            int bestSword = ItemUtil.findSwordInInventorySlot(0, checkDurability.getValue());
            if (bestSword == -1) bestSword = ItemUtil.findSwordInInventorySlot(0, false);
            int bestPick = ItemUtil.findInventorySlot("pickaxe", 0, checkDurability.getValue());
            if (bestPick == -1) bestPick = ItemUtil.findInventorySlot("pickaxe", 0, false);
            int bestShovel = ItemUtil.findInventorySlot("shovel", 0, checkDurability.getValue());
            if (bestShovel == -1) bestShovel = ItemUtil.findInventorySlot("shovel", 0, false);
            int bestAxe = ItemUtil.findInventorySlot("axe", 0, checkDurability.getValue());
            if (bestAxe == -1) bestAxe = ItemUtil.findInventorySlot("axe", 0, false);
            int bestBow = ItemUtil.findBowInventorySlot(0, checkDurability.getValue());
            if (bestBow == -1) bestBow = ItemUtil.findBowInventorySlot(0, false);

            List<Integer> bestArmorSlots = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                int inv = ItemUtil.findArmorInventorySlot(i, false);
                if (inv != -1) bestArmorSlots.add(inv);
                int eq = ItemUtil.findArmorInventorySlot(i, true);
                if (eq != -1) bestArmorSlots.add(eq);
            }

            Set<Integer> keepSlots = new HashSet<>();
            if (bestSword != -1) keepSlots.add(bestSword);
            if (bestPick != -1) keepSlots.add(bestPick);
            if (bestShovel != -1) keepSlots.add(bestShovel);
            if (bestAxe != -1) keepSlots.add(bestAxe);
            if (bestBow != -1) keepSlots.add(bestBow);
            keepSlots.addAll(bestArmorSlots);

            int curBlock = ItemUtil.findInventorySlot(0, ItemUtil.ItemType.Block);
            int curProj = ItemUtil.findInventorySlot(0, ItemUtil.ItemType.Projectile);
            if (curProj == -1) curProj = ItemUtil.findInventorySlot(0, ItemUtil.ItemType.FishRod);
            int curApple = ItemUtil.findInventorySlot(0, ItemUtil.ItemType.GoldApple);
            int curArrow = ItemUtil.findInventorySlot(0, ItemUtil.ItemType.Arrow);
            int curPearl = findPearlSlot(0);
            int curFire = ItemUtil.findInventorySlot(0, ItemUtil.ItemType.FireCharge);
            int curWind = ItemUtil.findInventorySlot(0, ItemUtil.ItemType.WindCharge);
            int curMace = ItemUtil.findInventorySlot(0, ItemUtil.ItemType.Mace);
            if (curBlock != -1) keepSlots.add(curBlock);
            if (curProj != -1) keepSlots.add(curProj);
            if (curApple != -1) keepSlots.add(curApple);
            if (curArrow != -1) keepSlots.add(curArrow);
            if (curPearl != -1) keepSlots.add(curPearl);
            if (curFire != -1) keepSlots.add(curFire);
            if (curWind != -1) keepSlots.add(curWind);
            if (curMace != -1) keepSlots.add(curMace);

            int currentBlockCount = getStackSize(curBlock);
            int currentProjectileCount = getStackSize(curProj);

            for (int i = 0; i < 36; i++) {
                if (keepSlots.contains(i)) continue;

                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack == null || stack.isEmpty()) continue;

                Item item = stack.getItem();

                if (item instanceof SwordItem || item instanceof MiningToolItem || item instanceof BowItem || item instanceof ArmorItem) {
                    clickSlot(mc.player.playerScreenHandler.syncId, convertSlotIndex(i), 1, 4);
                    int d = nextDelay();
                    if (d > 0) { actionDelay = d; return; }
                    continue;
                }

                if (keepWaterBucket.getValue() && isWaterBucket(stack)) continue;
                if (keepLavaBucket.getValue() && isLavaBucket(stack)) continue;

                boolean isBlock = ItemUtil.isBlock(stack);
                boolean isProjectile = ItemUtil.isProjectile(stack);
                boolean shouldDrop = false;

                if (isBlock) {
                    currentBlockCount += stack.getCount();
                    if (currentBlockCount > this.blocks.getValue()) shouldDrop = true;
                } else if (isProjectile) {
                    currentProjectileCount += stack.getCount();
                    if (currentProjectileCount > this.projectiles.getValue()) shouldDrop = true;
                } else if (isNotSpecialItem(stack)) {
                    shouldDrop = true;
                }

                if (shouldDrop) {
                    clickSlot(mc.player.playerScreenHandler.syncId, convertSlotIndex(i), 1, 4);
                    int d = nextDelay();
                    if (d > 0) { actionDelay = d; return; }
                }
            }
        }

        if (this.autoClose.getValue() && !this.clickedThisTick) {
            mc.player.closeHandledScreen();
        }
    }

    @EventTarget
    public void onClick(WindowClickEvent event) {
        if (maxDelay.getValue() != 0) {
            this.actionDelay = RandomUtil.nextInt(minDelay.getValue() + 1, maxDelay.getValue() + 1);
        }
    }

    @Override
    public void verifyValue(String mode) {
        switch (mode) {
            case "min-delay":
                if (minDelay.getValue() > maxDelay.getValue())
                    maxDelay.setValue(minDelay.getValue());
                break;
            case "max-delay":
                if (minDelay.getValue() > maxDelay.getValue())
                    minDelay.setValue(maxDelay.getValue());
                break;
        }
    }

    private static boolean isNotSpecialItem(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ArmorItem) return false;
        if (item instanceof SwordItem) return false;
        if (item instanceof MiningToolItem) return false;
        if (item instanceof BowItem) return false;
        if (item instanceof FishingRodItem) return false;
        if (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE) return false;
        if (item == Items.ENDER_PEARL) return false;
        if (item == Items.TOTEM_OF_UNDYING) return false;
        if (item == Items.ARROW) return false;
        if (item instanceof FireChargeItem) return false;
        if (item instanceof WindChargeItem) return false;
        if (item instanceof MaceItem) return false;
        if (ItemUtil.isBlock(stack)) return false;
        if (ItemUtil.isProjectile(stack)) return false;
        return true;
    }
}
