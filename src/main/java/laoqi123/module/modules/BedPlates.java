package laoqi123.module.modules;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.events.Render2DEvent;
import laoqi123.events.Render3DEvent;
import laoqi123.module.Module;
import laoqi123.oneconfig.Glass;
import laoqi123.property.properties.IntProperty;
import laoqi123.util.RenderUtil;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4d;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BedPlates extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final IntProperty range = new IntProperty("Range", 1, 1, 7);

    private final List<BedRenderData> renderDataList = new ArrayList<>();

    public BedPlates() {
        super("BedPlates", false);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        this.renderDataList.clear();
        if (!this.isEnabled() || mc.player == null || mc.world == null) return;

        BedESP bedESP = (BedESP) Myau.moduleManager.modules.get(BedESP.class);
        if (bedESP == null || bedESP.beds.isEmpty()) return;

        double screenScale = mc.getWindow().getScaleFactor();

        for (BlockPos bedPos : bedESP.beds) {
            BlockState state = mc.world.getBlockState(bedPos);
            if (!(state.getBlock() instanceof BedBlock)) continue;
            if (state.get(BedBlock.PART) != BedPart.HEAD) continue;

            BlockPos footPos = bedPos.offset(state.get(BedBlock.FACING).getOpposite());
            BlockState footState = mc.world.getBlockState(footPos);
            if (!(footState.getBlock() instanceof BedBlock)) continue;

            double minX = Math.min(bedPos.getX(), footPos.getX());
            double minY = bedPos.getY();
            double minZ = Math.min(bedPos.getZ(), footPos.getZ());
            double maxX = Math.max(bedPos.getX(), footPos.getX()) + 1.0;
            double maxY = bedPos.getY() + 1.0;
            double maxZ = Math.max(bedPos.getZ(), footPos.getZ()) + 1.0;

            Vector4d pos = RenderUtil.projectToScreen(new Box(minX, minY, minZ, maxX, maxY, maxZ), screenScale);

            if (pos == null) continue;

            float screenX = (float) ((pos.x + pos.z) / 2.0);
            float screenY = (float) pos.y - 30;

            List<BlockEntry> blocks = collectProtectionBlocks(bedPos, footPos);
            if (blocks.isEmpty()) continue;

            blocks.sort((a, b) -> Float.compare(b.hardness, a.hardness));

            float itemSize = 16;
            float padding = 2;
            float totalWidth = blocks.size() * (itemSize + padding) + padding;
            float bgHeight = itemSize + padding * 2;

            double centerX = (bedPos.getX() + footPos.getX()) / 2.0 + 0.5;
            double centerY = bedPos.getY() + 0.5;
            double centerZ = (bedPos.getZ() + footPos.getZ()) / 2.0 + 0.5;
            float dist = (float) Math.sqrt(mc.player.squaredDistanceTo(centerX, centerY, centerZ));

            renderDataList.add(new BedRenderData(screenX - totalWidth / 2, screenY - bgHeight / 2, totalWidth, bgHeight, blocks, dist));
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.renderDataList.isEmpty()) return;
        DrawContext context = event.getContext();

        for (BedRenderData data : this.renderDataList) {
            float dist = data.dist;
            float scale = MathHelper.clamp(1.0f / (1.0f + dist * 0.08f) * 1.5f, 0.4f, 2.0f);

            float cx = data.bgX + data.totalWidth / 2;
            float cy = data.bgY + data.bgHeight / 2;

            float width = data.totalWidth * scale;
            float height = data.bgHeight * scale;
            float x = cx - width / 2;
            float y = cy - height / 2;

            // OneConfig HUD surface, with the corner radius following the plate's scale
            // so a distant (smaller) plate keeps the same visual roundness.
            Glass.panel(x, y, width, height, Glass.BG, Glass.RADIUS * scale);

            float itemX = x + 2 * scale;
            float itemY = y + 2 * scale;

            for (BlockEntry entry : data.blocks) {
                RenderUtil.renderItemInGUI(new ItemStack(entry.block.asItem()), (int) itemX, (int) itemY);
                itemX += 18 * scale;
            }
        }

        this.renderDataList.clear();
    }

    private static final Block[] ALLOWED_BLOCKS = {
            Blocks.WHITE_WOOL,
            Blocks.TERRACOTTA,
            Blocks.GLASS,
            Blocks.WHITE_STAINED_GLASS,
            Blocks.END_STONE,
            Blocks.LADDER,
            Blocks.OAK_PLANKS,
            Blocks.OAK_LOG,
            Blocks.OBSIDIAN,
            Blocks.PACKED_ICE
    };

    private boolean isBlockAllowed(Block block) {
        for (Block allowed : ALLOWED_BLOCKS) {
            if (block == allowed) {
                return true;
            }
        }
        return false;
    }

    private List<BlockEntry> collectProtectionBlocks(BlockPos head, BlockPos foot) {
        Map<Block, BlockEntry> blockMap = new LinkedHashMap<>();
        int centerX = (head.getX() + foot.getX()) / 2;
        int centerY = head.getY();
        int centerZ = (head.getZ() + foot.getZ()) / 2;
        int r = range.getValue();

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = 0; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (r == 1 && Math.abs(dx) + Math.abs(dz) > 1) continue;

                    BlockPos pos = new BlockPos(centerX + dx, centerY + dy, centerZ + dz);
                    Block block = mc.world.getBlockState(pos).getBlock();

                    if (!isBlockAllowed(block)) continue;

                    Block displayBlock = block instanceof StainedGlassBlock ? Blocks.GLASS : block;
                    if (blockMap.containsKey(displayBlock)) continue;

                    float hardness = block.getHardness();
                    blockMap.put(displayBlock, new BlockEntry(displayBlock, hardness < 0 ? 100 : hardness));
                }
            }
        }
        return new ArrayList<>(blockMap.values());
    }

    private static class BlockEntry {
        final Block block;
        final float hardness;
        BlockEntry(Block block, float hardness) {
            this.block = block;
            this.hardness = hardness;
        }
    }

    private static class BedRenderData {
        float bgX, bgY, totalWidth, bgHeight;
        List<BlockEntry> blocks;
        float dist;
        BedRenderData(float x, float y, float w, float h, List<BlockEntry> b, float d) {
            this.bgX = x;
            this.bgY = y;
            this.totalWidth = w;
            this.bgHeight = h;
            this.blocks = b;
            this.dist = d;
        }
    }
}
