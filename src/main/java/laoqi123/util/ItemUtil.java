package laoqi123.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;

public class ItemUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final ArrayList<Integer> specialItems = new SpecialItems();

    public static boolean isNotSpecialItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        Item item = itemStack.getItem();
        if (item instanceof PotionItem || item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
            PotionContentsComponent contents = itemStack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) {
                return true;
            }
            for (StatusEffectInstance effect : contents.getEffects()) {
                if (specialItems.contains(getPotionId(effect))) {
                    return false;
                }
            }
            return true;
        }
        if (item instanceof EnderPearlItem) return false;
        if (item == Items.TOTEM_OF_UNDYING) return false;
        if (itemStack.get(DataComponentTypes.FOOD) != null && item != Items.SPIDER_EYE) return false;
        if (item instanceof SpawnEggItem) return false;
        return item != Items.NETHER_STAR;
    }

    private static int getPotionId(StatusEffectInstance effect) {
        RegistryEntry<StatusEffect> type = effect.getEffectType();
        if (type.matches(StatusEffects.SPEED)) return 1;
        if (type.matches(StatusEffects.HASTE)) return 3;
        if (type.matches(StatusEffects.STRENGTH)) return 5;
        if (type.matches(StatusEffects.INSTANT_HEALTH)) return 6;
        if (type.matches(StatusEffects.JUMP_BOOST)) return 8;
        if (type.matches(StatusEffects.REGENERATION)) return 10;
        if (type.matches(StatusEffects.RESISTANCE)) return 11;
        if (type.matches(StatusEffects.FIRE_RESISTANCE)) return 12;
        if (type.matches(StatusEffects.INVISIBILITY)) return 14;
        if (type.matches(StatusEffects.LUCK)) return 21;
        return -1;
    }

    public static boolean isBlock(ItemStack itemStack) {
        if (itemStack == null || itemStack.getCount() < 1) {
            return false;
        }
        Item item = itemStack.getItem();
        if (item instanceof BlockItem) {
            return ItemUtil.isContainerBlock((BlockItem) item);
        }
        return false;
    }

    public static boolean isProjectile(ItemStack itemStack) {
        if (itemStack == null || itemStack.getCount() < 1) {
            return false;
        }
        Item item = itemStack.getItem();
        if (item instanceof EggItem) return true;
        if (item instanceof SnowballItem) return true;
        return false;
    }

    public static boolean isContainerBlock(BlockItem itemBlock) {
        Block block = itemBlock.getBlock();
        if (BlockUtil.isInteractable(block)) return false;
        return BlockUtil.isSolid(block);
    }

    public static double getAttackBonus(ItemStack itemStack) {
        double attackBonus = 0.0;
        if (itemStack == null || itemStack.isEmpty()) {
            return 0.0;
        }
        AttributeModifiersComponent modifiers = itemStack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        for (AttributeModifiersComponent.Entry modifier : modifiers.modifiers()) {
            if (modifier.attribute().matches(EntityAttributes.ATTACK_DAMAGE)) {
                attackBonus += modifier.modifier().value();
                break;
            }
        }
        attackBonus += (double) ItemUtil.getEnchantLevel(itemStack, Enchantments.FIRE_ASPECT);
        attackBonus += (double) ItemUtil.getEnchantLevel(itemStack, Enchantments.SHARPNESS) * 1.25;
        return attackBonus;
    }

    public static float getToolEfficiency(ItemStack itemStack) {
        float efficiency = 1.0f;
        if (itemStack != null && !itemStack.isEmpty()) {
            if (itemStack.getItem() instanceof MiningToolItem) {
                efficiency = getBaseToolSpeed(itemStack);
                int enchantLevel = ItemUtil.getEnchantLevel(itemStack, Enchantments.EFFICIENCY);
                if (efficiency > 1.0f && enchantLevel > 0) {
                    efficiency += (float) (enchantLevel * enchantLevel + 1);
                }
            }
        }
        return efficiency;
    }

    public static float getToolEfficiency(ItemStack itemStack, Block block) {
        float efficiency = 1.0f;
        if (itemStack != null && !itemStack.isEmpty()) {
            BlockState state = block.getDefaultState();
            boolean isPickaxe = itemStack.getItem() instanceof PickaxeItem;
            efficiency = itemStack.isSuitableFor(state) || !isPickaxe
                    ? itemStack.getMiningSpeedMultiplier(state) : 1.0f;
            if (itemStack.getItem() instanceof MiningToolItem) {
                int enchantLevel = ItemUtil.getEnchantLevel(itemStack, Enchantments.EFFICIENCY);
                if (efficiency > 1.0f && enchantLevel > 0) {
                    efficiency += (float) (enchantLevel * enchantLevel + 1);
                }
            }
        }
        return efficiency;
    }

    private static float getBaseToolSpeed(ItemStack itemStack) {
        Identifier id = Registries.ITEM.getId(itemStack.getItem());
        String path = id.getPath();
        if (path.contains("netherite")) return 9.0f;
        if (path.contains("golden")) return 12.0f;
        if (path.contains("diamond")) return 8.0f;
        if (path.contains("iron")) return 6.0f;
        if (path.contains("stone")) return 4.0f;
        if (path.contains("wooden")) return 2.0f;
        return 1.0f;
    }

    public static double getArmorProtection(ItemStack itemStack) {
        double protection = 0.0;
        if (itemStack != null && !itemStack.isEmpty()) {
            AttributeModifiersComponent modifiers = itemStack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
            for (AttributeModifiersComponent.Entry modifier : modifiers.modifiers()) {
                if (modifier.attribute().matches(EntityAttributes.ARMOR)) {
                    protection += modifier.modifier().value();
                }
            }
            protection += (double) ItemUtil.getEnchantLevel(itemStack, Enchantments.PROTECTION) * 0.8;
            protection += (double) ItemUtil.getEnchantLevel(itemStack, Enchantments.FEATHER_FALLING) * 0.05;
            protection += (double) ItemUtil.getEnchantLevel(itemStack, Enchantments.PROJECTILE_PROTECTION) * 0.01;
        }
        return protection;
    }

    public static double getBowAttackBonus(ItemStack itemStack) {
        double attackBonus = 0.0;
        if (itemStack != null && !itemStack.isEmpty()) {
            if (itemStack.getItem() instanceof BowItem) {
                attackBonus = 2.0;
                int power = ItemUtil.getEnchantLevel(itemStack, Enchantments.POWER);
                if (power > 0) {
                    attackBonus += (double) (power + 1) * 0.25;
                }
                attackBonus += (double) ItemUtil.getEnchantLevel(itemStack, Enchantments.FLAME) * 0.25;
                attackBonus += (double) ItemUtil.getEnchantLevel(itemStack, Enchantments.INFINITY) * 0.05;
            }
        }
        return attackBonus;
    }

    public static int findSwordInInventorySlot(int startSlot, boolean checkDurability) {
        int bestSlot = -1;
        double bestAttackBonus = 0.0;
        if (startSlot < 0) return bestSlot;
        for (int i = 0; i < 36; ++i) {
            int currentSlot = (startSlot + i) % 36;
            ItemStack itemStack = ItemUtil.mc.player.getInventory().getStack(currentSlot);
            if (itemStack == null || itemStack.isEmpty()) continue;
            if (!(itemStack.getItem() instanceof SwordItem)) continue;
            if (checkDurability) {
                if (itemStack.isDamageable() && itemStack.getDamage() > 0
                        && itemStack.getMaxDamage() - itemStack.getDamage() < 30) {
                    continue;
                }
            }
            double attackBonus = ItemUtil.getAttackBonus(itemStack);
            if (!(attackBonus > bestAttackBonus)) continue;
            bestSlot = currentSlot;
            bestAttackBonus = attackBonus;
        }
        return bestSlot;
    }

    public static int findBowInventorySlot(int startSlot, boolean checkDurability) {
        int bestSlot = -1;
        double bestAttackBonus = 0.0;
        if (startSlot < 0) return bestSlot;
        for (int i = 0; i < 36; ++i) {
            int currentSlot = (startSlot + i) % 36;
            ItemStack itemStack = ItemUtil.mc.player.getInventory().getStack(currentSlot);
            if (itemStack == null || itemStack.isEmpty()) continue;
            if (!(itemStack.getItem() instanceof BowItem)) continue;
            if (checkDurability) {
                if (itemStack.isDamageable() && itemStack.getDamage() > 0
                        && itemStack.getMaxDamage() - itemStack.getDamage() < 30) {
                    continue;
                }
            }
            double attackBonus = ItemUtil.getBowAttackBonus(itemStack);
            if (!(attackBonus > bestAttackBonus)) continue;
            bestSlot = currentSlot;
            bestAttackBonus = attackBonus;
        }
        return bestSlot;
    }

    public static int findInventorySlot(String toolClass, int startSlot, boolean checkDurability) {
        int bestSlot = -1;
        float bestEfficiency = 1.0f;
        if (startSlot < 0) return bestSlot;
        for (int i = 0; i < 36; ++i) {
            int currentSlot = (startSlot + i) % 36;
            ItemStack itemStack = ItemUtil.mc.player.getInventory().getStack(currentSlot);
            if (itemStack == null || itemStack.isEmpty()) continue;
            if (!(itemStack.getItem() instanceof MiningToolItem)) continue;
            if (!isToolOfClass(itemStack.getItem(), toolClass)) continue;
            if (checkDurability) {
                if (itemStack.isDamageable() && itemStack.getDamage() > 0
                        && itemStack.getMaxDamage() - itemStack.getDamage() < 30) {
                    continue;
                }
            }
            float efficiency = ItemUtil.getToolEfficiency(itemStack);
            if (!(efficiency > bestEfficiency)) continue;
            bestSlot = currentSlot;
            bestEfficiency = efficiency;
        }
        return bestSlot;
    }

    private static boolean isToolOfClass(Item item, String toolClass) {
        if ("pickaxe".equals(toolClass)) return item instanceof PickaxeItem;
        if ("axe".equals(toolClass)) return item instanceof AxeItem;
        if ("shovel".equals(toolClass)) return item instanceof ShovelItem;
        return item instanceof MiningToolItem;
    }

    public static int findInventorySlot(int currentSlot, Block block) {
        ItemStack currentItem = ItemUtil.mc.player.getInventory().getStack(currentSlot);
        int bestSlot = currentSlot;
        float bestStrength = getToolEfficiency(currentItem, block);
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = ItemUtil.mc.player.getInventory().getStack(i);
            if (itemStack == null || itemStack.isEmpty()) continue;
            float strength = getToolEfficiency(itemStack, block);
            if (!(strength > bestStrength)) continue;
            bestSlot = i;
            bestStrength = strength;
        }
        return bestSlot;
    }

    public static int findAndurilHotbarSlot(int currentSlot) {
        for (int i = currentSlot; i < currentSlot + 9; ++i) {
            int slot = i % 9;
            ItemStack itemStack = ItemUtil.mc.player.getInventory().getStack(slot);
            if (itemStack == null || itemStack.isEmpty()) continue;
            if (itemStack.getItem() == Items.IRON_SWORD) {
                LoreComponent lore = itemStack.get(DataComponentTypes.LORE);
                if (lore != null) {
                    for (Text line : lore.lines()) {
                        if (line.getString().contains("§9Justice")) {
                            return slot;
                        }
                    }
                }
            }
        }
        return -1;
    }

    public static int findArmorInventorySlot(int armorType, boolean checkDurability) {
        int bestSlot = -1;
        double bestProtection = 0.0;
        int limit = checkDurability ? 40 : 36;
        for (int i = 0; i < limit; ++i) {
            ItemStack itemStack = ItemUtil.mc.player.getInventory().getStack(i);
            if (itemStack == null || itemStack.isEmpty()) continue;
            if (!(itemStack.getItem() instanceof ArmorItem)) continue;
            EquippableComponent equippable = itemStack.get(DataComponentTypes.EQUIPPABLE);
            EquipmentSlot slot = equippable == null ? null : equippable.slot();
            if (slot != getArmorSlot(armorType)) continue;
            if (checkDurability) {
                if (itemStack.isDamageable() && itemStack.getDamage() > 0
                        && itemStack.getMaxDamage() - itemStack.getDamage() < 30) {
                    continue;
                }
            }
            double protection = ItemUtil.getArmorProtection(itemStack);
            if (!(protection >= bestProtection)) continue;
            bestSlot = i;
            bestProtection = protection;
        }
        return bestSlot;
    }

    private static EquipmentSlot getArmorSlot(int armorType) {
        switch (armorType) {
            case 0: return EquipmentSlot.HEAD;
            case 1: return EquipmentSlot.CHEST;
            case 2: return EquipmentSlot.LEGS;
            case 3: return EquipmentSlot.FEET;
            default: return null;
        }
    }

    public static int findInventorySlot(int startSlot, ItemType itemType) {
        int bestSlot = -1;
        int maxStackSize = 0;
        if (startSlot < 0) startSlot = 0;
        for (int i = 0; i < 36; ++i) {
            int currentSlot = (startSlot + i) % 36;
            ItemStack itemStack = ItemUtil.mc.player.getInventory().getStack(currentSlot);
            if (itemStack == null || itemStack.isEmpty()) continue;
            if (!itemType.contains(itemStack)) continue;
            if (maxStackSize >= itemStack.getCount()) continue;
            bestSlot = currentSlot;
            maxStackSize = itemStack.getCount();
        }
        return bestSlot;
    }

    public static int findInventorySlot(ItemType itemType) {
        int stackSize = 0;
        for (int i = 0; i < 36; ++i) {
            ItemStack itemStack = ItemUtil.mc.player.getInventory().getStack(i);
            if (itemStack == null || itemStack.isEmpty()) continue;
            if (!itemType.contains(itemStack)) continue;
            stackSize += itemStack.getCount();
        }
        return stackSize;
    }

    public static boolean hasRawUnbreakingEnchant() {
        ItemStack itemStack = ItemUtil.mc.player.getMainHandStack();
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        NbtComponent customData = itemStack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound tag = customData.getNbt();
            if (tag.contains("ExtraAttributes")) {
                NbtCompound extra = tag.getCompound("ExtraAttributes");
                if (extra.contains("UHCid")) {
                    long id = extra.getLong("UHCid");
                    if (id == 50006L || id == 50009L) {
                        return true;
                    }
                }
            }
            if (itemStack.contains(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP)
                    && itemStack.getItem() == Items.DIAMOND_SHOVEL) {
                return true;
            }
        }
        if (itemStack.contains(DataComponentTypes.STORED_ENCHANTMENTS)) {
            return false;
        }
        if (ItemUtil.getEnchantLevel(itemStack, Enchantments.UNBREAKING) > 0) {
            return true;
        }
        return itemStack.getItem() instanceof SwordItem;
    }

    public static boolean isHoldingSword() {
        ItemStack itemStack = ItemUtil.mc.player.getMainHandStack();
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        return itemStack.getItem() instanceof SwordItem;
    }

    public static boolean isHoldingTool() {
        ItemStack itemStack = ItemUtil.mc.player.getMainHandStack();
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        return itemStack.getItem() instanceof MiningToolItem;
    }

    public static boolean isEating() {
        ItemStack itemStack = ItemUtil.mc.player.getMainHandStack();
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        if (itemStack.getItem() instanceof SplashPotionItem || itemStack.getItem() instanceof LingeringPotionItem) {
            return false;
        }
        UseAction action = itemStack.getUseAction();
        return action == UseAction.EAT || action == UseAction.DRINK;
    }

    public static boolean isUsingBow() {
        ItemStack itemStack = ItemUtil.mc.player.getMainHandStack();
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        return itemStack.getItem() instanceof BowItem;
    }

    public static boolean isHoldingNonEmpty() {
        ItemStack itemStack = ItemUtil.mc.player.getMainHandStack();
        if (itemStack == null || itemStack.getCount() < 1) {
            return false;
        }
        return itemStack.getItem() instanceof BlockItem;
    }

    public static boolean isHoldingBlock() {
        return ItemUtil.isBlock(ItemUtil.mc.player.getMainHandStack());
    }

    public static boolean hasHoldItem() {
        ItemStack itemStack = ItemUtil.mc.player.getMainHandStack();
        if (itemStack == null || itemStack.getCount() < 1) {
            return false;
        }
        return itemStack.getItem() instanceof FireChargeItem;
    }

    private static int getEnchantLevel(ItemStack itemStack, RegistryKey<Enchantment> enchantment) {
        if (ItemUtil.mc.world == null || itemStack == null || itemStack.isEmpty()) return 0;
        return EnchantmentHelper.getLevel(
                ItemUtil.mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(enchantment),
                itemStack);
    }

    static final class SpecialItems extends ArrayList<Integer> {
        SpecialItems() {
            this.add(1);
            this.add(3);
            this.add(5);
            this.add(6);
            this.add(8);
            this.add(10);
            this.add(11);
            this.add(12);
            this.add(14);
            this.add(21);
            this.add(22);
        }
    }

    public enum ItemType {
        Block {
            public boolean contains(ItemStack itemStack) {
                return isBlock(itemStack);
            }
        },
        Projectile {
            public boolean contains(ItemStack itemStack) {
                return isProjectile(itemStack);
            }
        },
        FishRod {
            public boolean contains(ItemStack itemStack) {
                return itemStack.getItem() instanceof FishingRodItem;
            }
        },
        GoldApple {
            public boolean contains(ItemStack itemStack) {
                return itemStack.getItem() == Items.GOLDEN_APPLE || itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
            }
        },
        Arrow {
            public boolean contains(ItemStack itemStack) {
                return itemStack.getItem() == Items.ARROW;
            }
        },
        FireCharge {
            public boolean contains(ItemStack itemStack) {
                return itemStack.getItem() instanceof FireChargeItem;
            }
        },
        WindCharge {
            public boolean contains(ItemStack itemStack) {
                return itemStack.getItem() instanceof WindChargeItem;
            }
        },
        Mace {
            public boolean contains(ItemStack itemStack) {
                return itemStack.getItem() instanceof MaceItem;
            }
        };
        abstract public boolean contains(ItemStack itemStack);
    }
}
