package laoqi123.util;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;

public class SlotUtils {
    public static final int OFFHAND = 40;

    public static boolean isGoodForBridging(Item item) {
        return item instanceof BlockItem;
    }
}