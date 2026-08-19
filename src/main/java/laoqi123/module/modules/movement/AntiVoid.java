package laoqi123.module.modules.movement;

import com.google.common.base.CaseFormat;
import laoqi123.Myau;
import laoqi123.enums.BlinkModules;
import laoqi123.event.EventTarget;
import laoqi123.event.types.Priority;
import laoqi123.events.KeyEvent;
import laoqi123.events.PlayerUpdateEvent;
import laoqi123.module.Module;
import laoqi123.util.PlayerUtil;
import laoqi123.util.RandomUtil;
import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.ModeProperty;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;

public class AntiVoid extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private boolean isInVoid = false;
    private boolean wasInVoid = false;
    private double[] lastSafePosition = null;
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"BLINK"});
    public final FloatProperty distance = new FloatProperty("distance", 5.0F, 0.0F, 16.0F);

    private void resetBlink() {
        Myau.blinkManager.setBlinkState(false, BlinkModules.ANTI_VOID);
        this.lastSafePosition = null;
    }

    private boolean canUseAntiVoid() {
        LongJump longJump = (LongJump) Myau.moduleManager.modules.get(LongJump.class);
        return !longJump.isJumping();
    }

    public AntiVoid() {
        super("AntiVoid", false);
    }

    @EventTarget(Priority.LOWEST)
    public void onUpdate(PlayerUpdateEvent event) {
        if (this.isEnabled()) {
            this.isInVoid = !mc.player.getAbilities().allowFlying && PlayerUtil.isInWater();
            if (this.mode.getValue() == 0) {
                if (!this.isInVoid) {
                    this.resetBlink();
                }
                if (this.lastSafePosition != null) {
                    float subWidth = mc.player.getWidth() / 2.0F;
                    float height = mc.player.getHeight();
                    if (PlayerUtil.checkInWater(
                            new Box(
                                    this.lastSafePosition[0] - (double) subWidth,
                                    this.lastSafePosition[1],
                                    this.lastSafePosition[2] - (double) subWidth,
                                    this.lastSafePosition[0] + (double) subWidth,
                                    this.lastSafePosition[1] + (double) height,
                                    this.lastSafePosition[2] + (double) subWidth
                            )
                    )) {
                        this.resetBlink();
                    }
                }
                if (!this.wasInVoid && this.isInVoid && this.canUseAntiVoid()) {
                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    if (Myau.blinkManager.setBlinkState(true, BlinkModules.ANTI_VOID)) {
                        this.lastSafePosition = new double[]{mc.player.prevX, mc.player.prevY, mc.player.prevZ};
                    }
                }
                if (Myau.blinkManager.getBlinkingModule() == BlinkModules.ANTI_VOID
                        && this.lastSafePosition != null
                        && this.lastSafePosition[1] - (double) this.distance.getValue().floatValue() > mc.player.getY()) {
                    Myau.blinkManager
                            .blinkedPackets
                            .offerFirst(
                                    new PlayerMoveC2SPacket.PositionAndOnGround(
                                            this.lastSafePosition[0], this.lastSafePosition[1] - RandomUtil.nextDouble(10.0, 20.0), this.lastSafePosition[2], false, false
                                    )
                            );
                    this.resetBlink();
                }
            }
            this.wasInVoid = this.isInVoid;
        }
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (event.getKey() == InputUtil.fromTranslationKey(mc.options.useKey.getBoundKeyTranslationKey()).getCode()) {
            ItemStack currentItem = mc.player.getInventory().getMainHandStack();
            if (currentItem != null && currentItem.getItem() instanceof EnderPearlItem) {
                this.resetBlink();
            }
        }
    }

    @Override
    public void onEnabled() {
        this.isInVoid = false;
        this.wasInVoid = false;
        this.resetBlink();
    }

    @Override
    public void onDisabled() {
        Myau.blinkManager.setBlinkState(false, BlinkModules.ANTI_VOID);
    }

    @Override
    public void verifyValue(String mode) {
        if (this.isEnabled()) {
            this.onDisabled();
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
