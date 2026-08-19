package laoqi123.module.modules.render.chestesp.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import laoqi123.Myau;
import laoqi123.events.Render3DEvent;
import laoqi123.module.modules.player.ChestStealer;
import laoqi123.module.modules.render.chestesp.ChestESPMode;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.ColorProperty;
import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.ModeProperty;
import laoqi123.util.BlockUtil;
import laoqi123.util.RenderUtil;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.vehicle.AbstractChestBoatEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.entity.vehicle.HopperMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class LiquidBounceChestESP extends ChestESPMode {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Box", "Glow"});
    public final BooleanProperty requiresChestStealer = new BooleanProperty("Requires Chest Stealer", false);
    public final FloatProperty maximumDistance = new FloatProperty("Maximum Distance", 128.0F, 1.0F, 512.0F);
    public final BooleanProperty outline = new BooleanProperty("Outline", true);

    public final BooleanProperty chest = new BooleanProperty("Chest", true);
    public final ColorProperty chestColor = new ColorProperty("Chest Color", 0xFF0064FF);
    public final BooleanProperty chestTracers = new BooleanProperty("Chest Tracers", false);

    public final BooleanProperty enderChest = new BooleanProperty("EnderChest", true);
    public final ColorProperty enderChestColor = new ColorProperty("EnderChest Color", 0xFFFF00FF);
    public final BooleanProperty enderChestTracers = new BooleanProperty("EnderChest Tracers", false);

    public final BooleanProperty furnace = new BooleanProperty("Furnace", true);
    public final ColorProperty furnaceColor = new ColorProperty("Furnace Color", 0xFF4F4F4F);
    public final BooleanProperty furnaceTracers = new BooleanProperty("Furnace Tracers", false);

    public final BooleanProperty brewingStand = new BooleanProperty("BrewingStand", true);
    public final ColorProperty brewingStandColor = new ColorProperty("BrewingStand Color", 0xFF8B4513);
    public final BooleanProperty brewingStandTracers = new BooleanProperty("BrewingStand Tracers", false);

    public final BooleanProperty dispenser = new BooleanProperty("Dispenser", true);
    public final ColorProperty dispenserColor = new ColorProperty("Dispenser Color", 0xFFD3D3D3);
    public final BooleanProperty dispenserTracers = new BooleanProperty("Dispenser Tracers", false);

    public final BooleanProperty hopper = new BooleanProperty("Hopper", true);
    public final ColorProperty hopperColor = new ColorProperty("Hopper Color", 0xFF808080);
    public final BooleanProperty hopperTracers = new BooleanProperty("Hopper Tracers", false);

    public final BooleanProperty shulkerBox = new BooleanProperty("ShulkerBox", true);
    public final ColorProperty shulkerBoxColor = new ColorProperty("ShulkerBox Color", 0xFF9D6E9D);
    public final BooleanProperty shulkerBoxTracers = new BooleanProperty("ShulkerBox Tracers", false);

    public final BooleanProperty pot = new BooleanProperty("Pot", true);
    public final ColorProperty potColor = new ColorProperty("Pot Color", 0xFFD18600);
    public final BooleanProperty potTracers = new BooleanProperty("Pot Tracers", false);

    private enum StorageType {
        CHEST,
        ENDER_CHEST,
        FURNACE,
        BREWING_STAND,
        DISPENSER,
        HOPPER,
        SHULKER_BOX,
        POT
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (!this.parent.isEnabled() || mc.world == null) return;
        if (this.requiresChestStealer.getValue()) {
            ChestStealer chestStealer = (ChestStealer) Myau.moduleManager.modules.get(ChestStealer.class);
            if (chestStealer == null || !chestStealer.isEnabled()) {
                return;
            }
        }

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        double maxDistanceSquared = this.maximumDistance.getValue() * this.maximumDistance.getValue();

        if (this.mode.getValue() == 0) {
            this.renderBoxes(event, cameraPos, maxDistanceSquared);
        } else {
            this.renderGlow(cameraPos, maxDistanceSquared);
        }
        this.renderTracers(cameraPos, maxDistanceSquared);
    }

    private void renderBoxes(Render3DEvent event, Vec3d cameraPos, double maxDistanceSquared) {
        RenderUtil.enableRenderState();

        for (BlockEntity blockEntity : BlockUtil.getBlockEntities()) {
            StorageType type = categorize(blockEntity);
            if (type == null) continue;
            int color = this.getTypeColor(type);
            if ((color >>> 24) == 0 || !this.isTypeEnabled(type)) continue;
            BlockPos pos = blockEntity.getPos();
            if (!this.withinDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, cameraPos, maxDistanceSquared)) continue;
            BlockState state = mc.world.getBlockState(pos);
            if (state.isAir()) continue;
            Box box = state.getOutlineShape(mc.world, pos).getBoundingBox()
                    .offset(pos.getX(), pos.getY(), pos.getZ())
                    .offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            this.drawBox(box, color);
        }

        for (Entity entity : mc.world.getEntities()) {
            StorageType type = categorize(entity);
            if (type == null) continue;
            int color = this.getTypeColor(type);
            if ((color >>> 24) == 0 || !this.isTypeEnabled(type)) continue;
            if (!this.withinDistance(entity.getX(), entity.getY(), entity.getZ(), cameraPos, maxDistanceSquared)) continue;

            double halfWidth = entity.getWidth() / 2.0;
            Box box = new Box(-halfWidth, 0.0, -halfWidth, halfWidth, entity.getHeight(), halfWidth).expand(0.05);
            double x = RenderUtil.lerpDouble(entity.getX(), entity.prevX, event.getPartialTicks()) - cameraPos.x;
            double y = RenderUtil.lerpDouble(entity.getY(), entity.prevY, event.getPartialTicks()) - cameraPos.y;
            double z = RenderUtil.lerpDouble(entity.getZ(), entity.prevZ, event.getPartialTicks()) - cameraPos.z;
            this.drawBox(box.offset(x, y, z), color);
        }

        RenderUtil.disableRenderState();
    }

    private void renderGlow(Vec3d cameraPos, double maxDistanceSquared) {
        RenderUtil.enableRenderState();

        for (BlockEntity blockEntity : BlockUtil.getBlockEntities()) {
            StorageType type = categorize(blockEntity);
            if (type == null) continue;
            int color = this.getTypeColor(type);
            if ((color >>> 24) == 0 || !this.isTypeEnabled(type)) continue;
            BlockPos pos = blockEntity.getPos();
            if (!this.withinDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, cameraPos, maxDistanceSquared)) continue;
            BlockState state = mc.world.getBlockState(pos);
            if (state.isAir() || state.getRenderType() != BlockRenderType.MODEL) continue;
            Box box = state.getOutlineShape(mc.world, pos).getBoundingBox()
                    .offset(pos.getX(), pos.getY(), pos.getZ())
                    .offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            RenderUtil.drawBoundingBox(box, getRed(color), getGreen(color), getBlue(color), 255, 2.0F);
        }

        RenderUtil.disableRenderState();
    }

    private void renderTracers(Vec3d cameraPos, double maxDistanceSquared) {
        float partialTicks = mc.getRenderTickCounter().getTickDelta(true);
        Vec3d eyeVector;
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            eyeVector = new Vec3d(0.0, 0.0, 1.0)
                    .rotateX((float) (-Math.toRadians(RenderUtil.lerpFloat(mc.getCameraEntity().getPitch(), mc.getCameraEntity().prevPitch, partialTicks))))
                    .rotateY((float) (-Math.toRadians(RenderUtil.lerpFloat(mc.getCameraEntity().getYaw(), mc.getCameraEntity().prevYaw, partialTicks))));
        } else {
            eyeVector = new Vec3d(0.0, 0.0, 0.0)
                    .rotateX((float) (-Math.toRadians(mc.gameRenderer.getCamera().getPitch())))
                    .rotateY((float) (-Math.toRadians(mc.gameRenderer.getCamera().getYaw())));
        }
        eyeVector = new Vec3d(eyeVector.x, eyeVector.y + (double) mc.getCameraEntity().getStandingEyeHeight(), eyeVector.z);

        RenderUtil.enableRenderState();
        for (BlockEntity blockEntity : BlockUtil.getBlockEntities()) {
            StorageType type = categorize(blockEntity);
            if (type == null) continue;
            if (!this.isTypeEnabled(type) || !this.isTypeTracers(type)) continue;
            int color = this.getTypeColor(type);
            if ((color >>> 24) == 0) continue;
            BlockPos pos = blockEntity.getPos();
            double x = pos.getX() + 0.5 - cameraPos.x;
            double y = pos.getY() + 0.5 - cameraPos.y;
            double z = pos.getZ() + 0.5 - cameraPos.z;
            if (!this.withinDistance(x, y, z, cameraPos, maxDistanceSquared)) continue;
            RenderUtil.drawLine3D(eyeVector, x, y, z, getRed(color), getGreen(color), getBlue(color), 255.0F, 1.5F);
        }
        RenderUtil.disableRenderState();
    }

    private void drawBox(Box box, int color) {
        this.drawFilledBoxAlpha(box, getRed(color), getGreen(color), getBlue(color), 50);
        if (this.outline.getValue()) {
            RenderUtil.drawBoundingBox(box, getRed(color), getGreen(color), getBlue(color), 100, 2.0F);
        }
    }

    private boolean withinDistance(double x, double y, double z, Vec3d cameraPos, double maxDistanceSquared) {
        double dx = x - cameraPos.x;
        double dy = y - cameraPos.y;
        double dz = z - cameraPos.z;
        return dx * dx + dy * dy + dz * dz < maxDistanceSquared;
    }

    private boolean isTypeEnabled(StorageType type) {
        switch (type) {
            case CHEST: return this.chest.getValue();
            case ENDER_CHEST: return this.enderChest.getValue();
            case FURNACE: return this.furnace.getValue();
            case BREWING_STAND: return this.brewingStand.getValue();
            case DISPENSER: return this.dispenser.getValue();
            case HOPPER: return this.hopper.getValue();
            case SHULKER_BOX: return this.shulkerBox.getValue();
            case POT: return this.pot.getValue();
            default: return false;
        }
    }

    private boolean isTypeTracers(StorageType type) {
        switch (type) {
            case CHEST: return this.chestTracers.getValue();
            case ENDER_CHEST: return this.enderChestTracers.getValue();
            case FURNACE: return this.furnaceTracers.getValue();
            case BREWING_STAND: return this.brewingStandTracers.getValue();
            case DISPENSER: return this.dispenserTracers.getValue();
            case HOPPER: return this.hopperTracers.getValue();
            case SHULKER_BOX: return this.shulkerBoxTracers.getValue();
            case POT: return this.potTracers.getValue();
            default: return false;
        }
    }

    private int getTypeColor(StorageType type) {
        switch (type) {
            case CHEST: return this.chestColor.getValue();
            case ENDER_CHEST: return this.enderChestColor.getValue();
            case FURNACE: return this.furnaceColor.getValue();
            case BREWING_STAND: return this.brewingStandColor.getValue();
            case DISPENSER: return this.dispenserColor.getValue();
            case HOPPER: return this.hopperColor.getValue();
            case SHULKER_BOX: return this.shulkerBoxColor.getValue();
            case POT: return this.potColor.getValue();
            default: return 0xFFFFFFFF;
        }
    }

    private static int getRed(int color) {
        return (color >> 16) & 0xFF;
    }

    private static int getGreen(int color) {
        return (color >> 8) & 0xFF;
    }

    private static int getBlue(int color) {
        return color & 0xFF;
    }

    private static StorageType categorize(BlockEntity blockEntity) {
        if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity) return StorageType.CHEST;
        if (blockEntity instanceof EnderChestBlockEntity) return StorageType.ENDER_CHEST;
        if (blockEntity instanceof AbstractFurnaceBlockEntity) return StorageType.FURNACE;
        if (blockEntity instanceof BrewingStandBlockEntity) return StorageType.BREWING_STAND;
        if (blockEntity instanceof DispenserBlockEntity || blockEntity instanceof CrafterBlockEntity) return StorageType.DISPENSER;
        if (blockEntity instanceof HopperBlockEntity) return StorageType.HOPPER;
        if (blockEntity instanceof ShulkerBoxBlockEntity) return StorageType.SHULKER_BOX;
        if (blockEntity instanceof DecoratedPotBlockEntity) return StorageType.POT;
        return null;
    }

    private static StorageType categorize(Entity entity) {
        if (entity instanceof HopperMinecartEntity) return StorageType.HOPPER;
        if (entity instanceof ChestMinecartEntity || entity instanceof AbstractChestBoatEntity) return StorageType.CHEST;
        if (entity instanceof AbstractDonkeyEntity donkey && donkey.hasChest()) return StorageType.CHEST;
        if (entity instanceof LlamaEntity) return StorageType.CHEST;
        return null;
    }

    private void drawFilledBoxAlpha(Box bb, int red, int green, int blue, int alpha) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        VertexRendering.drawFilledBox(new MatrixStack(), bufferBuilder, bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, red, green, blue, alpha);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
}