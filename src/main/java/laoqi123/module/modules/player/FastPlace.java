package laoqi123.module.modules.player;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.TickEvent;
import laoqi123.mixin.MinecraftClientAccessor;
import laoqi123.module.Module;
import laoqi123.util.BlockUtil;
import laoqi123.util.RotationUtil;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.FloatValue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FastPlace extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final DecimalFormat df = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.US));
    private long delayMS = 0L;
    public final FloatValue delay = new FloatValue("delay", 1.0F, 1.0F, 3.0F);
    public final BooleanValue blocksOnly = new BooleanValue("blocks-only", true);
    public final BooleanValue placeFix = new BooleanValue("place-fix", true);
    public final BooleanValue skipObsidian = new BooleanValue("skip-obsidian", true);
    public final BooleanValue skipInteractable = new BooleanValue("skip-interactable", true);

    private boolean canPlace() {
        ItemStack stack = mc.player.getMainHandStack();
        if (stack != null) {
            Item item = stack.getItem();
            if (item instanceof FishingRodItem) {
                return false;
            }
            if (item instanceof BlockItem) {
                Block block = ((BlockItem) item).getBlock();
                if (skipObsidian.getValue() && block == Blocks.OBSIDIAN) {
                    return false;
                }
                if (skipInteractable.getValue() && BlockUtil.isInteractable(block)) {
                    return false;
                }
                if (!(Boolean) this.placeFix.getValue()) {
                    return true;
                }
                HitResult mop = RotationUtil.rayTrace(
                        mc.player.getYaw(), mc.player.getPitch(), mc.player.getBlockInteractionRange(), 1.0F
                );
                if (mop != null && mop.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) mop;
                    BlockState state = block.getDefaultState();
                    BlockPos placePos = blockHit.getBlockPos().offset(blockHit.getSide());
                    return mc.world.canPlace(state, placePos, ShapeContext.of(mc.player)) && state.canPlaceAt(mc.world, placePos);
                }
                return false;
            }
        }
        return !(Boolean) this.blocksOnly.getValue();
    }

    public FastPlace() {
        super("FastPlace", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            int rightClickDelayTimer = ((MinecraftClientAccessor) mc).getRightClickDelayTimer();
            if (rightClickDelayTimer == 4) {
                this.delayMS = this.delayMS + (long) (50.0F * this.delay.getValue());
            }
            if (this.delayMS > 0L) {
                this.delayMS = this.delayMS - 50;
            }
            if (this.delayMS <= 0L && rightClickDelayTimer > 1 && this.canPlace()) {
                ((MinecraftClientAccessor) mc).setRightClickDelayTimer(0);
            }
        }
    }

    @Override
    public void onDisabled() {
        this.delayMS = 0L;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{df.format(this.delay.getValue())};
    }
}
