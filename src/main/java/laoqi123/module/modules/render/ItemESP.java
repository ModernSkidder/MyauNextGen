package laoqi123.module.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import laoqi123.enums.ChatColors;
import laoqi123.event.EventTarget;
import laoqi123.event.impl.Render3DEvent;
import laoqi123.module.Module;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.PercentValue;
import laoqi123.util.RenderUtil;
import laoqi123.util.TeamUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4fStack;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

public class ItemESP extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final PercentValue opacity = new PercentValue("opacity", 25);
    public final BooleanValue outline = new BooleanValue("outline", false);
    public final BooleanValue itemCount = new BooleanValue("item-count", true);
    public final BooleanValue autoScale = new BooleanValue("auto-scale", true);
    public final BooleanValue emeralds = new BooleanValue("emeralds", true);
    public final BooleanValue diamonds = new BooleanValue("diamonds", true);
    public final BooleanValue goldd = new BooleanValue("gold", true);
    public final BooleanValue iron = new BooleanValue("iron", true);

    private boolean shouldHighlightItem(Item item) {
        return this.emeralds.getValue() && this.isEmeraldItem(item)
                || this.diamonds.getValue() && this.isDiamondItem(item)
                || this.goldd.getValue() && this.isGoldItem(item)
                || this.iron.getValue() && this.isIronItem(item);
    }

    private boolean isEmeraldItem(Item item) {
        Block block = Block.getBlockFromItem(item);
        return item == Items.EMERALD || block == Blocks.EMERALD_BLOCK || block == Blocks.EMERALD_ORE;
    }

    private boolean isDiamondItem(Item item) {
        Block block = Block.getBlockFromItem(item);
        return item == Items.DIAMOND
                || item == Items.DIAMOND_SWORD
                || item == Items.DIAMOND_PICKAXE
                || item == Items.DIAMOND_SHOVEL
                || item == Items.DIAMOND_AXE
                || item == Items.DIAMOND_HOE
                || item == Items.DIAMOND_HELMET
                || item == Items.DIAMOND_CHESTPLATE
                || item == Items.DIAMOND_LEGGINGS
                || item == Items.DIAMOND_BOOTS
                || block == Blocks.DIAMOND_BLOCK
                || block == Blocks.DIAMOND_ORE;
    }

    private boolean isGoldItem(Item item) {
        Block block = Block.getBlockFromItem(item);
        return item == Items.GOLD_INGOT || item == Items.GOLD_NUGGET || item == Items.GOLDEN_APPLE || block == Blocks.GOLD_BLOCK || block == Blocks.GOLD_ORE;
    }

    private boolean isIronItem(Item item) {
        Block block = Block.getBlockFromItem(item);
        return item == Items.IRON_INGOT || block == Blocks.IRON_BLOCK || block == Blocks.IRON_ORE;
    }

    private Color getItemColor(Item item) {
        if (this.isEmeraldItem(item)) {
            return new Color(ChatColors.GREEN.toAwtColor());
        } else if (this.isDiamondItem(item)) {
            return new Color(ChatColors.AQUA.toAwtColor());
        } else if (this.isGoldItem(item)) {
            return new Color(ChatColors.YELLOW.toAwtColor());
        } else {
            return this.isIronItem(item) ? new Color(ChatColors.WHITE.toAwtColor()) : new Color(ChatColors.GRAY.toAwtColor());
        }
    }

    private int getItemPriority(Item item) {
        if (this.isEmeraldItem(item)) {
            return 4;
        } else if (this.isDiamondItem(item)) {
            return 3;
        } else if (this.isGoldItem(item)) {
            return 2;
        } else {
            return this.isIronItem(item) ? 1 : 0;
        }
    }

    public ItemESP() {
        super("ItemESP", false);
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled()) {
            LinkedHashMap<ItemData, Integer> itemMap = new LinkedHashMap<>();
            for (Entity entity : TeamUtil.getLoadedEntitiesSorted()) {
                if (entity.age >= 3
                        && RenderUtil.isInViewFrustum(entity.getBoundingBox(), 0.125)
                        && entity instanceof ItemEntity) {
                    ItemEntity entityItem = (ItemEntity) entity;
                    ItemStack stack = entityItem.getStack();
                    if (stack.getCount() > 0 && this.shouldHighlightItem(stack.getItem())) {
                        double x = RenderUtil.lerpDouble(entityItem.getX(), entityItem.prevX, event.getPartialTicks());
                        double y = RenderUtil.lerpDouble(entityItem.getY(), entityItem.prevY, event.getPartialTicks());
                        double z = RenderUtil.lerpDouble(entityItem.getZ(), entityItem.prevZ, event.getPartialTicks());
                        ItemData data = new ItemData(stack.getItem(), x, y, z);
                        Integer id = itemMap.get(data);
                        itemMap.put(new ItemData(stack.getItem(), x, y, z), stack.getCount() + (id == null ? 0 : id));
                    }
                }
            }
            for (Entry<ItemData, Integer> itemEntry : itemMap.entrySet().stream().sorted((entry1, entry2) -> {
                int o = this.getItemPriority(entry1.getKey().item);
                int o2 = this.getItemPriority(entry2.getKey().item);
                return Integer.compare(o, o2);
            }).collect(Collectors.toList())) {
                Color itemColor = this.getItemColor(itemEntry.getKey().item);
                Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
                double x = itemEntry.getKey().x - cameraPos.x;
                double y = itemEntry.getKey().y - cameraPos.y;
                double z = itemEntry.getKey().z - cameraPos.z;
                double distance = mc.getCameraEntity().getPos().distanceTo(new Vec3d(itemEntry.getKey().x, itemEntry.getKey().y, itemEntry.getKey().z));
                double scale = 0.5 + 0.375 * ((Math.max(6.0, this.autoScale.getValue() ? distance : 6.0) - 6.0) / 28.0);
                Box axisAlignedBB = new Box(x - scale * 0.5, y, z - scale * 0.5, x + scale * 0.5, y + scale, z + scale * 0.5);
                RenderUtil.enableRenderState();
                if (this.opacity.getValue() > 0) {
                    RenderUtil.drawFilledBox(
                            axisAlignedBB, itemColor.getRed(), itemColor.getGreen(), itemColor.getBlue()
                    );
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
                if (this.outline.getValue()) {
                    RenderUtil.drawBoundingBox(axisAlignedBB, itemColor.getRed(), itemColor.getGreen(), itemColor.getBlue(), 255, 1.5F);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
                RenderUtil.disableRenderState();
                if (this.itemCount.getValue()) {
                    Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
                    matrixStack.pushMatrix();
                    matrixStack.translate((float) x, (float) y + (float) (scale * 0.5), (float) z);
                    matrixStack.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
                    float flip = mc.options.getPerspective().isFrontView() ? -1.0F : 1.0F;
                    matrixStack.rotate(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch() * flip));
                    double fontScale = -0.04375 - 0.0328125 * ((Math.max(6.0, this.autoScale.getValue() ? distance : 6.0) - 6.0) / 28.0);
                    matrixStack.scale((float) fontScale, (float) fontScale, 1.0F);
                    String countText = String.format("%d", itemEntry.getValue());
                    RenderUtil.drawOutlinedString(
                            countText,
                            ((float) mc.textRenderer.getWidth(countText) / 2.0F - 0.5F) * -1.0F,
                            ((float) (mc.textRenderer.fontHeight / 2) - 0.5F) * -1.0F
                    );
                    matrixStack.popMatrix();
                }
            }
        }
    }

    public static class ItemData {
        private final int hashCode;
        public final Item item;
        public final double x;
        public final double y;
        public final double z;

        public ItemData(Item item, double x, double y, double z) {
            this.item = item;
            this.x = x;
            this.y = y;
            this.z = z;
            this.hashCode = Objects.hash(item, (int) x, (int) y, (int) z);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object != null && this.getClass() == object.getClass()) {
                ItemData itemData = (ItemData) object;
                return this.item == itemData.item && (int) this.x == (int) itemData.x && (int) this.y == (int) itemData.y && (int) this.z == (int) itemData.z;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }
}
