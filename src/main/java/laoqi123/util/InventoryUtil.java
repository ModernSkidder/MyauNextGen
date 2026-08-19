package laoqi123.util;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

public class InventoryUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean isFullBlock(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
            return false;
        }
        Block block = ((BlockItem) stack.getItem()).getBlock();
        if (mc.world == null || block.getDefaultState().isAir()) {
            return false;
        }
        return block.getDefaultState().isFullCube(mc.world, BlockPos.ORIGIN);
    }

    public static void swap(int slot, int switchSlot) {
        if (mc.player == null || mc.interactionManager == null) {
            return;
        }
        int hotbarIndex = 36 + switchSlot;
        if (slot == hotbarIndex) {
            return;
        }
        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(0, hotbarIndex, 0, SlotActionType.PICKUP, mc.player);
    }
}