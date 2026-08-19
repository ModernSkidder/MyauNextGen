package laoqi123.module.modules.player;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.UpdateEvent;
import laoqi123.event.impl.WindowClickEvent;
import laoqi123.module.Module;
import laoqi123.util.ChatUtil;
import laoqi123.util.ItemUtil;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.IntValue;
import laoqi123.util.RandomUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

public class ChestStealer extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private int clickDelay = 0;
    private int oDelay = 0;
    private boolean inChest = false;
    private boolean warnedFull = false;
    public final IntValue minDelay = new IntValue("min-delay", 1, 0, 20);
    public final IntValue maxDelay = new IntValue("max-delay", 2, 0, 20);
    public final IntValue openDelay = new IntValue("open-delay", 1, 0, 20);
    public final BooleanValue autoClose = new BooleanValue("auto-close", false);
    public final BooleanValue nameCheck = new BooleanValue("name-check", true);
    public final BooleanValue skipTrash = new BooleanValue("skip-trash", true);
    public final BooleanValue moreArmor = new BooleanValue("more-armor", false);
    public final BooleanValue moreSword = new BooleanValue("more-sword", false);

    private boolean isValidGameMode() {
        GameMode gameType = mc.interactionManager.getCurrentGameMode();
        return gameType == GameMode.SURVIVAL || gameType == GameMode.ADVENTURE;
    }

    private boolean isDiamondMaterial(ItemStack itemStack) {
        Identifier id = Registries.ITEM.getId(itemStack.getItem());
        return id.getNamespace().equals("minecraft") && id.getPath().startsWith("diamond_");
    }

    private boolean isIronMaterial(ItemStack itemStack) {
        Identifier id = Registries.ITEM.getId(itemStack.getItem());
        return id.getNamespace().equals("minecraft") && id.getPath().startsWith("iron_");
    }

    private boolean isMoreArmor(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;
        if (!this.moreArmor.getValue()) return false;
        if (!(itemStack.getItem() instanceof ArmorItem)) return false;
        if (isDiamondMaterial(itemStack)) return true;
        return isIronMaterial(itemStack) && itemStack.hasEnchantments();
    }

    private boolean hasFireAspect(ItemStack itemStack) {
        for (RegistryEntry<Enchantment> entry : itemStack.getEnchantments().getEnchantments()) {
            if (entry.matchesKey(Enchantments.FIRE_ASPECT)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMoreSword(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;
        if (!this.moreSword.getValue()) return false;
        if (!(itemStack.getItem() instanceof SwordItem)) return false;
        Identifier id = Registries.ITEM.getId(itemStack.getItem());
        boolean diamond = id.getNamespace().equals("minecraft") && id.getPath().equals("diamond_sword");
        boolean iron = id.getNamespace().equals("minecraft") && id.getPath().equals("iron_sword");
        if (diamond) return true;
        if (hasFireAspect(itemStack)) return true;
        return iron && itemStack.hasEnchantments();
    }

    private int getArmorType(ItemStack itemStack) {
        EquippableComponent equippable = itemStack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable == null) return -1;
        EquipmentSlot slot = equippable.slot();
        switch (slot) {
            case HEAD:
                return 0;
            case CHEST:
                return 1;
            case LEGS:
                return 2;
            case FEET:
                return 3;
            default:
                return -1;
        }
    }

    private boolean isInvManagerRequire(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;
        InvManager invManager = (InvManager) Myau.moduleManager.modules.get(InvManager.class);
        if (ItemUtil.ItemType.Block.contains(itemStack)) {
            return !invManager.isEnabled() || ItemUtil.findInventorySlot(ItemUtil.ItemType.Block) < invManager.blocks.getValue();
        }
        if (ItemUtil.ItemType.Projectile.contains(itemStack)) {
            return !invManager.isEnabled() || ItemUtil.findInventorySlot(ItemUtil.ItemType.Projectile) < invManager.projectiles.getValue();
        }
        if (ItemUtil.ItemType.FishRod.contains(itemStack)) {
            return ItemUtil.findInventorySlot(ItemUtil.ItemType.Projectile) == 0;
        }
        if (ItemUtil.ItemType.Arrow.contains(itemStack)) {
            return !invManager.isEnabled() || ItemUtil.findInventorySlot(ItemUtil.ItemType.Arrow) < invManager.arrow.getValue();
        }
        if (ItemUtil.ItemType.FireCharge.contains(itemStack)) {
            return !invManager.isEnabled() || ItemUtil.findInventorySlot(ItemUtil.ItemType.FireCharge) < invManager.fireCharges.getValue();
        }
        if (ItemUtil.ItemType.WindCharge.contains(itemStack)) {
            return !invManager.isEnabled() || ItemUtil.findInventorySlot(ItemUtil.ItemType.WindCharge) < invManager.windCharges.getValue();
        }
        if (ItemUtil.ItemType.Mace.contains(itemStack)) {
            return !invManager.isEnabled() || ItemUtil.findInventorySlot(ItemUtil.ItemType.Mace) < invManager.mace.getValue();
        }
        return false;
    }

    private void shiftClick(int windowId, int slotId) {
        mc.interactionManager.clickSlot(windowId, slotId, 0, SlotActionType.QUICK_MOVE, mc.player);
    }

    public ChestStealer() {
        super("ChestStealer", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.clickDelay > 0) {
                this.clickDelay--;
            }
            if (this.oDelay > 0) {
                this.oDelay--;
            }
            if (!(mc.currentScreen instanceof HandledScreen)) {
                this.inChest = false;
            } else {
                ScreenHandler container = ((HandledScreen<?>) mc.currentScreen).getScreenHandler();
                if (!(container instanceof net.minecraft.screen.GenericContainerScreenHandler)) {
                    this.inChest = false;
                } else {
                    if (!this.inChest) {
                        this.inChest = true;
                        this.warnedFull = false;
                        this.oDelay = this.openDelay.getValue() + 1;
                    }
                    if (this.oDelay <= 0 && this.clickDelay <= 0) {
                        if (this.isEnabled() && this.isValidGameMode()) {
                            Inventory inventory = ((net.minecraft.screen.GenericContainerScreenHandler) container).getInventory();
                            if (this.nameCheck.getValue()) {
                                String rawName = mc.currentScreen.getTitle().getString();
                                String cleanName = rawName.replaceAll("(?i)§[0-9a-fklmnor]", "").trim();
                                String localizedChest = Text.translatable("container.chest").getString();
                                String localizedDouble = Text.translatable("container.chestDouble").getString();
                                String cleanLocalizedChest = localizedChest.replaceAll("(?i)§[0-9a-fklmnor]", "").trim();
                                String cleanLocalizedDouble = localizedDouble.replaceAll("(?i)§[0-9a-fklmnor]", "").trim();

                                boolean matches = cleanName.equalsIgnoreCase("Chest")
                                        || cleanName.equalsIgnoreCase("Large Chest")
                                        || cleanName.equalsIgnoreCase(cleanLocalizedChest)
                                        || cleanName.equalsIgnoreCase(cleanLocalizedDouble);

                                if (!matches) {
                                    return;
                                }
                            }
                            if (mc.player.getInventory().getEmptySlot() == -1) {
                                if (!this.warnedFull) {
                                    ChatUtil.sendFormatted(String.format("%s%s: &cYour inventory is full!&r", Myau.clientName, this.getName()));
                                    this.warnedFull = true;
                                }
                                if (this.autoClose.getValue()) {
                                    mc.player.closeHandledScreen();
                                }
                            } else {
                                boolean isZeroDelay = this.minDelay.getValue() == 0 && this.maxDelay.getValue() == 0;
                                int maxIterations = isZeroDelay ? 5 : 1;
                                int stolen = 0;
                                while (stolen < maxIterations) {
                                    if (!trySteal(container, inventory)) {
                                        break;
                                    }
                                    stolen++;
                                    if (!isZeroDelay) {
                                        break;
                                    }
                                    if (mc.player.getInventory().getEmptySlot() == -1) {
                                        if (this.autoClose.getValue()) {
                                            mc.player.closeHandledScreen();
                                        }
                                        break;
                                    }
                                    if (!(mc.currentScreen instanceof HandledScreen)) {
                                        break;
                                    }
                                    container = ((HandledScreen<?>) mc.currentScreen).getScreenHandler();
                                    if (!(container instanceof net.minecraft.screen.GenericContainerScreenHandler)) {
                                        break;
                                    }
                                    inventory = ((net.minecraft.screen.GenericContainerScreenHandler) container).getInventory();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean trySteal(ScreenHandler container, Inventory inventory) {
        if (this.skipTrash.getValue()) {
            int bestSword = -1;
            double bestDamage = 0.0;
            int[] bestArmorSlots = new int[]{-1, -1, -1, -1};
            double[] bestArmorProtection = new double[]{0.0, 0.0, 0.0, 0.0};
            int bestPickaxeSlot = -1;
            float bestPickaxeEfficiency = 1.0F;
            int bestShovelSlot = -1;
            float bestShovelEfficiency = 1.0F;
            int bestAxeSlot = -1;
            float bestAxeEfficiency = 1.0F;
            int bestBow = -1;
            double bestBowDamage = 0.0;
            for (int i = 0; i < inventory.size(); i++) {
                if (container.getSlot(i).hasStack()) {
                    ItemStack stack = container.getSlot(i).getStack();
                    net.minecraft.item.Item item = stack.getItem();
                    if (item instanceof SwordItem) {
                        double damage = ItemUtil.getAttackBonus(stack);
                        if (bestSword == -1 || damage > bestDamage) {
                            bestSword = i;
                            bestDamage = damage;
                        }
                    } else if (item instanceof ArmorItem) {
                        int armorType = getArmorType(stack);
                        if (armorType >= 0) {
                            double protectionLevel = ItemUtil.getArmorProtection(stack);
                            if (bestArmorSlots[armorType] == -1 || protectionLevel > bestArmorProtection[armorType]) {
                                bestArmorSlots[armorType] = i;
                                bestArmorProtection[armorType] = protectionLevel;
                            }
                        }
                    } else if (item instanceof net.minecraft.item.PickaxeItem) {
                        float efficiency = ItemUtil.getToolEfficiency(stack);
                        if (bestPickaxeSlot == -1 || efficiency > bestPickaxeEfficiency) {
                            bestPickaxeSlot = i;
                            bestPickaxeEfficiency = efficiency;
                        }
                    } else if (item instanceof net.minecraft.item.ShovelItem) {
                        float efficiency = ItemUtil.getToolEfficiency(stack);
                        if (bestShovelSlot == -1 || efficiency > bestShovelEfficiency) {
                            bestShovelSlot = i;
                            bestShovelEfficiency = efficiency;
                        }
                    } else if (item instanceof net.minecraft.item.AxeItem) {
                        float efficiency = ItemUtil.getToolEfficiency(stack);
                        if (bestAxeSlot == -1 || efficiency > bestAxeEfficiency) {
                            bestAxeSlot = i;
                            bestAxeEfficiency = efficiency;
                        }
                    } else if (item instanceof net.minecraft.item.BowItem) {
                        double damage = ItemUtil.getBowAttackBonus(stack);
                        if (bestBow == -1 || damage > bestBowDamage) {
                            bestBow = i;
                            bestBowDamage = damage;
                        }
                    }
                }
            }
            int swordInInventorySlot = ItemUtil.findSwordInInventorySlot(0, true);
            double damage = swordInInventorySlot != -1 ? ItemUtil.getAttackBonus(mc.player.getInventory().getStack(swordInInventorySlot)) : 0.0;
            if (bestDamage > damage) {
                this.shiftClick(container.syncId, bestSword);
                return true;
            }
            for (int i = 0; i < 4; i++) {
                int slot = ItemUtil.findArmorInventorySlot(i, true);
                double protectionLevel = slot != -1
                        ? ItemUtil.getArmorProtection(mc.player.getInventory().getStack(slot))
                        : 0.0;
                if (bestArmorProtection[i] > protectionLevel) {
                    this.shiftClick(container.syncId, bestArmorSlots[i]);
                    return true;
                }
            }
            int pickaxeSlot = ItemUtil.findInventorySlot("pickaxe", 0, true);
            float pickaxeEfficiency = pickaxeSlot != -1 ? ItemUtil.getToolEfficiency(mc.player.getInventory().getStack(pickaxeSlot)) : 1.0F;
            if (bestPickaxeEfficiency > pickaxeEfficiency) {
                this.shiftClick(container.syncId, bestPickaxeSlot);
                return true;
            }
            int shovelSlot = ItemUtil.findInventorySlot("shovel", 0, true);
            float shovelEfficiency = shovelSlot != -1 ? ItemUtil.getToolEfficiency(mc.player.getInventory().getStack(shovelSlot)) : 1.0F;
            if (bestShovelEfficiency > shovelEfficiency) {
                this.shiftClick(container.syncId, bestShovelSlot);
                return true;
            }
            int axeSlot = ItemUtil.findInventorySlot("axe", 0, true);
            float efficiency = axeSlot != -1 ? ItemUtil.getToolEfficiency(mc.player.getInventory().getStack(axeSlot)) : 1.0F;
            if (bestAxeEfficiency > efficiency) {
                this.shiftClick(container.syncId, bestAxeSlot);
                return true;
            }
            int bowSlot = ItemUtil.findBowInventorySlot(0, true);
            double bowDamage = bowSlot != -1 ? ItemUtil.getBowAttackBonus(mc.player.getInventory().getStack(bowSlot)) : 0.0;
            if (bestBowDamage > bowDamage) {
                this.shiftClick(container.syncId, bestBow);
                return true;
            }
        }
        for (int i = 0; i < inventory.size(); i++) {
            if (container.getSlot(i).hasStack()) {
                ItemStack stack = container.getSlot(i).getStack();
                if (!this.skipTrash.getValue() || !ItemUtil.isNotSpecialItem(stack) || isMoreArmor(stack) || isMoreSword(stack) || isInvManagerRequire(stack)) {
                    this.shiftClick(container.syncId, i);
                    return true;
                }
            }
        }
        if (this.autoClose.getValue()) {
            mc.player.closeHandledScreen();
        }
        return false;
    }

    @EventTarget
    public void onWindowClick(WindowClickEvent event) {
        if (minDelay.getValue() == 0 && maxDelay.getValue() == 0) {
            clickDelay = 0;
        } else {
            int newMin = Math.max(0, minDelay.getValue() - 1);
            int newMax = Math.max(0, maxDelay.getValue() - 1);
            clickDelay = RandomUtil.nextInt(newMin + 1, newMax + 1);
        }
    }

    @Override
    public void verifyValue(String mode) {
        switch (mode) {
            case "min-delay":
                if (this.minDelay.getValue() > this.maxDelay.getValue()) {
                    this.maxDelay.setValue(this.minDelay.getValue());
                }
                break;
            case "max-delay":
                if (this.minDelay.getValue() > this.maxDelay.getValue()) {
                    this.minDelay.setValue(this.maxDelay.getValue());
                }
        }
    }
}
