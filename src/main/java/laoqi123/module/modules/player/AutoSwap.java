package laoqi123.module.modules.player;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.TickEvent;
import laoqi123.module.Module;
import laoqi123.value.properties.BooleanValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.List;

public class AutoSwap extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    private Item lastItem;
    private int lastSlot = -1;

    public final BooleanValue blocks = new BooleanValue("blocks", true);
    public final BooleanValue projectiles = new BooleanValue("projectiles", true);
    public final BooleanValue pearls = new BooleanValue("pearls", true);
    public final BooleanValue swords = new BooleanValue("swords", true);
    public final BooleanValue tools = new BooleanValue("tools", true);
    public final BooleanValue resources = new BooleanValue("resources", true);

    private final List<String> ALLOWED_BLOCKS = Arrays.asList("stone", "grass", "dirt", "planks", "wool", "wood", "glass", "leaves", "clay", "cloth", "cobblestone", "sand", "gravel", "netherrack");
    private final List<String> PROJECTILES = Arrays.asList("egg", "snowball", "ender_pearl", "fireball");
    private final List<String> PEARLS = Arrays.asList("pearl", "ender_pearl");
    private final List<String> SWORDS = Arrays.asList("sword", "axe");
    private final List<String> TOOLS = Arrays.asList("rod", "pickaxe", "axe", "shovel", "hoe", "flint_and_steel");
    private final List<String> RESOURCES = Arrays.asList("265", "266", "388", "264", "diamond", "gold", "iron", "emerald");

    public AutoSwap() {
        super("AutoSwap", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }

        if (mc.world == null || mc.player == null) {
            return;
        }

        if (mc.currentScreen != null) {
            return;
        }

        int slot = mc.player.getInventory().selectedSlot;
        ItemStack held = mc.player.getInventory().getStack(slot);

        if (this.lastItem != null && slot == this.lastSlot && (held == null || held.getCount() < 1)) {
            this.swapItem(this.lastItem);
        }

        this.lastItem = held != null ? held.getItem() : null;
        this.lastSlot = slot;
    }

    private void swapItem(Item lastItem) {
        if (lastItem == null) {
            return;
        }

        String lastId = lastItem.getTranslationKey().toLowerCase();
        boolean isBlock = lastItem instanceof BlockItem;
        int current = mc.player.getInventory().selectedSlot;
        List<String> category = null;

        if (!isBlock) {
            if (this.projectiles.getValue() && containsAny(lastId, PROJECTILES) && !lastId.contains("leggings")) {
                category = PROJECTILES;
            } else if (this.pearls.getValue() && containsAny(lastId, PEARLS)) {
                category = PEARLS;
            } else if (this.swords.getValue() && containsAny(lastId, SWORDS)) {
                category = SWORDS;
            } else if (this.tools.getValue() && containsAny(lastId, TOOLS)) {
                category = TOOLS;
            } else if (this.resources.getValue() && containsAny(lastId, RESOURCES)) {
                category = RESOURCES;
            }
        }

        // Loop through hotbar to find replacement
        for (int offset = 1; offset <= 9; ++offset) {
            int i = (current + offset) % 9;
            ItemStack stack = mc.player.getInventory().getStack(i);

            if (stack != null && stack.getCount() >= 1) {
                Item item = stack.getItem();
                String id = item.getTranslationKey().toLowerCase();

                // Check if it's the same item
                if (item == lastItem) {
                    mc.player.getInventory().selectedSlot = i;
                    return;
                }

                // Check for blocks
                if (isBlock && this.blocks.getValue() && isValidBlock(stack)) {
                    mc.player.getInventory().selectedSlot = i;
                    return;
                }

                // Check for category matches
                if (category != null) {
                    if (containsAny(id, category) && !id.contains("leggings")) {
                        mc.player.getInventory().selectedSlot = i;
                        return;
                    }
                }
            }
        }
    }

    private boolean isValidBlock(ItemStack stack) {
        if (!this.blocks.getValue()) {
            return false;
        }

        if (!(stack.getItem() instanceof BlockItem)) {
            return false;
        }

        String id = stack.getItem().getTranslationKey().toLowerCase();
        return containsAny(id, ALLOWED_BLOCKS);
    }

    private boolean containsAny(String str, List<String> items) {
        for (String item : items) {
            if (str.contains(item)) {
                return true;
            }
        }
        return false;
    }
}
