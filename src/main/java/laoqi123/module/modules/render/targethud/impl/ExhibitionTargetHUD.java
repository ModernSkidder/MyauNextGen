package laoqi123.module.modules.render.targethud.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import laoqi123.module.modules.render.TargetHud2;
import laoqi123.module.modules.render.targethud.Fonts;
import laoqi123.module.modules.render.targethud.TargetHUDMode;
import laoqi123.util.ColorUtil;
import laoqi123.util.RenderUtil;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ExhibitionTargetHUD extends TargetHUDMode {
    public ExhibitionTargetHUD() {
        super("Exhibition");
    }

    @Override
    public void render(TargetHud2 targetHUD, TargetHud2.RenderData data, float x, float y) {
        float width = this.getSize(targetHUD, data)[0];
        float ratio = Math.clamp(data.targetHealth() / Math.max(data.maxHealth(), 1.0F), 0.0F, 1.0F);
        Color healthColor = ColorUtil.getHealthBlend(ratio).brighter();

        float baseX = x - 1.0F;
        float baseY = y + 5.0F;
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().translate(baseX, baseY, 0.0F);
        RenderUtil.drawSkeetRect(0.0F, -2.0F, width, 42.0F);
        Fonts.exhi.get(18.0F).drawStringWithShadow(data.entity().getDisplayName().getString(), 42.3F, 0.3F, -1);

        RenderUtil.drawRect(42.5F, 10.3F, 103.0F, 13.5F, healthColor.darker().darker().getRGB());
        RenderUtil.drawRect(42.5F, 10.3F, 42.5F + 60.5F * ratio, 13.5F, healthColor.getRGB());
        if (data.absorption() > 0.0F) {
            RenderUtil.drawRect(97.5F - data.absorption(), 10.3F, 103.5F, 13.5F, new Color(137, 112, 9).getRGB());
        }
        RenderUtil.drawRect(42.0F, 9.8F, 104.0F, 10.3F, Color.BLACK.getRGB());
        RenderUtil.drawRect(42.0F, 13.5F, 104.0F, 14.0F, Color.BLACK.getRGB());
        for (int i = 1; i < 10; ++i) {
            float lineX = 43.5F + 60.0F / 8.5F * i;
            RenderUtil.drawRect(lineX, 9.8F, lineX + 0.5F, 14.0F, Color.BLACK.getRGB());
        }

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().scale(0.5F, 0.5F, 0.5F);
        int distance = (int) (TargetHud2.getMinecraft().player.distanceTo(data.entity()));
        targetHUD.drawText("HP: " + (int) (data.targetHealth() + data.absorption()) + " | Dist: " + distance, 85.3F, 32.3F, -1, true);
        RenderSystem.getModelViewStack().popMatrix();

        if (data.entity() instanceof PlayerEntity) {
            this.renderItems(targetHUD, (PlayerEntity) data.entity(), baseX, baseY);
        }
        if (data.entity() instanceof PlayerEntity) {
            float ex = baseX + 22.0F;
            float ey = baseY + 35.0F;
            InventoryScreen.drawEntity(
                    targetHUD.getActiveContext(),
                    Math.round(ex - 11.0F), Math.round(ey - 24.0F), Math.round(ex + 11.0F), Math.round(ey + 16.0F),
                    15, 0.0F, 0.0F, 0.0F,
                    data.entity());
        } else {
            targetHUD.renderPlayerHead(data.entity(), 4.0F, 3.0F, 34.0F);
        }
        RenderSystem.getModelViewStack().popMatrix();
    }

    @Override
    public float[] getSize(TargetHud2 targetHUD, TargetHud2.RenderData data) {
        if (data == null) {
            return new float[]{124.0F, 47.0F};
        }
        float nameWidth = Fonts.exhi.get(18.0F).getStringWidth(data.entity().getDisplayName().getString());
        return new float[]{nameWidth > 70.0F ? 124.0F + nameWidth - 70.0F : 124.0F, 47.0F};
    }

    private void renderItems(TargetHud2 targetHUD, PlayerEntity target, float baseX, float baseY) {
        List<ItemStack> items = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = target.getEquippedStack(slot);
            if (!armor.isEmpty()) {
                items.add(armor);
            }
        }
        ItemStack held = target.getMainHandStack();
        if (!held.isEmpty()) {
            items.add(held);
        }
        int itemX = 26;
        for (ItemStack item : items) {
            RenderUtil.renderItemInGUI(item, Math.round(baseX + (itemX += 16)), Math.round(baseY + 20.0F));
        }
    }

}