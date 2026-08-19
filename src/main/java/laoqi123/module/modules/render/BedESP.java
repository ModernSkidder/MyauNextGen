package laoqi123.module.modules.render;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.events.Render3DEvent;
import laoqi123.mixin.EntityRenderDispatcherAccessor;
import laoqi123.module.Module;
import laoqi123.property.properties.*;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.ModeProperty;
import laoqi123.util.RenderUtil;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArraySet;

public class BedESP extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final CopyOnWriteArraySet<BlockPos> beds = new CopyOnWriteArraySet<>();
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"DEFAULT", "FULL"});
    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"CUSTOM", "HUD"});
    public final ColorProperty customColor;
    public final PercentProperty opacity;
    public final BooleanProperty outline;
    public final BooleanProperty obsidian;

    private Color getColor() {
        switch (this.color.getValue()) {
            case 0:
                return new Color(this.customColor.getValue());
            case 1:
                return ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
            default:
                return new Color(-1);
        }
    }

    private void drawObsidianBox(Box box) {
        if (this.outline.getValue()) {
            RenderUtil.drawBoundingBox(box, 170, 0, 170, 255, 1.5F);
        }
        RenderUtil.drawFilledBox(box, 170, 0, 170);
    }

    private void drawObsidian(BlockPos blockPos) {
        if (this.outline.getValue()) {
            RenderUtil.drawBlockBoundingBox(blockPos, 1.0, 170, 0, 170, 255, 1.5F);
        }
        RenderUtil.drawBlockBox(
                blockPos, 1.0, 170, 0, 170
        );
    }

    public BedESP() {
        super("BedESP", false);
        this.customColor = new ColorProperty("custom-color", (int) 8085714755840333141L, () -> this.color.getValue() == 0);
        this.opacity = new PercentProperty("opacity", 25);
        this.outline = new BooleanProperty("outline", false);
        this.obsidian = new BooleanProperty("obsidian", true);
    }

    public double getHeight() {
        return this.mode.getValue() == 1 ? 1.0 : 0.5625;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled()) {
            RenderUtil.enableRenderState();
            EntityRenderDispatcherAccessor renderManager = (EntityRenderDispatcherAccessor) mc.getEntityRenderDispatcher();
            for (BlockPos blockPos : this.beds) {
                BlockState state = mc.world.getBlockState(blockPos);
                if (state.getBlock() instanceof BedBlock && state.get(BedBlock.PART) == BedPart.HEAD) {
                    BlockPos opposite = blockPos.offset(state.get(BedBlock.FACING).getOpposite());
                    BlockState oppositeState = mc.world.getBlockState(opposite);
                    if (oppositeState.getBlock() instanceof BedBlock && oppositeState.get(BedBlock.PART) == BedPart.FOOT) {
                        if (this.obsidian.getValue()) {
                            for (Direction facing : Arrays.asList(Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
                                BlockPos offsetX = blockPos.offset(facing);
                                BlockPos offsetZ = opposite.offset(facing);
                                boolean xObsidian = mc.world.getBlockState(offsetX).isOf(Blocks.OBSIDIAN);
                                boolean zObsidian = mc.world.getBlockState(offsetZ).isOf(Blocks.OBSIDIAN);
                                if (xObsidian && zObsidian) {
                                    this.drawObsidianBox(
                                            new Box(
                                                    Math.min(offsetX.getX(), offsetZ.getX()),
                                                    offsetX.getY(),
                                                    Math.min(offsetX.getZ(), offsetZ.getZ()),
                                                    Math.max((double) offsetX.getX() + 1.0, (double) offsetZ.getX() + 1.0),
                                                    (double) offsetX.getY() + 1.0,
                                                    Math.max((double) offsetX.getZ() + 1.0, (double) offsetZ.getZ() + 1.0)
                                            )
                                                    .offset(
                                                            -renderManager.getCamera().getPos().x,
                                                            -renderManager.getCamera().getPos().y,
                                                            -renderManager.getCamera().getPos().z
                                                    )
                                    );
                                } else if (xObsidian) {
                                    this.drawObsidian(offsetX);
                                } else if (zObsidian) {
                                    this.drawObsidian(offsetZ);
                                }
                            }
                        }
                        Box box = new Box(
                                Math.min(blockPos.getX(), opposite.getX()),
                                blockPos.getY(),
                                Math.min(blockPos.getZ(), opposite.getZ()),
                                Math.max((double) blockPos.getX() + 1.0, (double) opposite.getX() + 1.0),
                                (double) blockPos.getY() + this.getHeight(),
                                Math.max((double) blockPos.getZ() + 1.0, (double) opposite.getZ() + 1.0)
                        )
                                .offset(
                                        -renderManager.getCamera().getPos().x,
                                        -renderManager.getCamera().getPos().y,
                                        -renderManager.getCamera().getPos().z
                                );
                        Color color = this.getColor();
                        if (this.outline.getValue()) {
                            RenderUtil.drawBoundingBox(box, color.getRed(), color.getGreen(), color.getBlue(), 255, 1.5F);
                        }
                        RenderUtil.drawFilledBox(
                                box,
                                color.getRed(),
                                color.getGreen(),
                                color.getBlue()
                        );
                    }
                } else {
                    this.beds.remove(blockPos);
                }
            }
            RenderUtil.disableRenderState();
        }
    }

    @Override
    public void onEnabled() {
        if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }
}
