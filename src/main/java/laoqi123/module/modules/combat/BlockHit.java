package laoqi123.module.modules.combat;

import com.google.common.base.CaseFormat;
import laoqi123.Myau;
import laoqi123.enums.BlinkModules;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.AttackEvent;
import laoqi123.events.TickEvent;
import laoqi123.module.Module;
import laoqi123.property.properties.*;
import laoqi123.util.ItemUtil;
import laoqi123.util.KeyBindUtil;
import laoqi123.util.PacketUtil;
import laoqi123.util.TimerUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class BlockHit extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Helper", "Auto", "Lag"});

    public final IntProperty stopTime = new IntProperty("Stop Ticks", 2, 1, 5, () -> this.mode.getValue() == 0);
    public final ModeProperty autoMode = new ModeProperty("Auto Mode", 0, new String[]{"Spam", "Hold"}, () -> this.mode.getValue() == 1 && this.autoBlockTime.getValue() == 0);
    public final ModeProperty autoBlockTime = new ModeProperty("AutoBlock Time", 0, new String[]{"Delay", "HurtTime", "Sag", "Smart"}, () -> this.mode.getValue() == 1);
    public final IntProperty smartBlockTick = new IntProperty("Smart Block Ticks", 2, 1, 5, () -> this.mode.getValue() == 1 && this.autoBlockTime.getValue() == 3);
    public final IntProperty blockDelay = new IntProperty("Block Delay", 100, 0, 1000, () -> this.mode.getValue() == 1 && this.autoBlockTime.getValue() == 0);
    public final IntProperty holdTick = new IntProperty("Hold Ticks", 2, 2, 5, () -> this.mode.getValue() == 1 && this.autoMode.getValue() == 1 && this.autoBlockTime.getValue() == 0);
    public final IntProperty minHurtTime = new IntProperty("Min HurtTime", 10, 1, 10, () -> this.mode.getValue() == 1 && this.autoBlockTime.getValue() == 1);
    public final IntProperty maxHurtTime = new IntProperty("Max HurtTime", 10, 1, 10, () -> this.mode.getValue() == 1 && this.autoBlockTime.getValue() == 1);
    public final IntProperty delayPacketTick = new IntProperty("Delay Packet Ticks", 2, 1, 10, () -> this.mode.getValue() == 2);
    public final IntProperty blockTick = new IntProperty("Block Ticks", 3, 1, 5, () -> this.mode.getValue() == 2);
    public final IntProperty startHurtTime = new IntProperty("Start HurtTime", 6, 1, 10, () -> this.mode.getValue() == 2);
    public final PercentProperty chance = new PercentProperty("Block Hit Chance", 50, () -> this.mode.getValue() == 1);
    public final BooleanProperty smart = new BooleanProperty("Smart", true, () -> this.mode.getValue() == 1);
    public final BooleanProperty autoBlockRange = new BooleanProperty("AutoBlock Range", true, () -> this.mode.getValue() == 1);
    public final FloatProperty range = new FloatProperty("Range", 3.0f, 1f, 4f, () -> autoBlockRange.getValue() && mode.getValue() == 1);

    private int holdTicks, stopTick;
    private boolean startBlocking;
    private boolean attacking;
    private int attackTicks;
    private int sagTicks = 0;
    private int blockTicks = 0;
    private int blinkTicks = 0;
    private boolean canBlock = false;
    private int getBlockTicks = 0;
    private LivingEntity target;
    private final TimerUtil timer = new TimerUtil();

    public BlockHit() {
        super("BlockHit", false);
    }

    private int getKeyCode(KeyBinding keyBinding) {
        InputUtil.Key key = InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey());
        int code = key.getCode();
        return key.getCategory() == InputUtil.Type.MOUSE ? code - 100 : code;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || mc.player == null || mc.world == null) return;
        if (event.getType() != EventType.PRE) return;

        if (this.mode.getValue() == 0) {
            if (KeyBindUtil.isKeyDown(mc.options.attackKey)) {
                if (mc.player.isBlocking()) {
                    startBlocking = true;
                    KeyBindUtil.setKeyBindState(this.getKeyCode(mc.options.useKey), false);
                }
            }
            if (startBlocking) stopTick++;
            if (stopTick == 2) {
                KeyBindUtil.pressKeyOnce(this.getKeyCode(mc.options.attackKey));
            }
            if (stopTick > stopTime.getValue()) {
                KeyBindUtil.updateKeyState(this.getKeyCode(mc.options.useKey));
                startBlocking = false;
                stopTick = 0;
            }
        }

        if (this.mode.getValue() == 1) {
            if (target == null) return;
            if (attacking) {
                attackTicks++;
            }
            if (attackTicks > 5) {
                reset();
                target = null;
                return;
            }
            if (Math.random() > chance.getValue() / 100.0) {
                reset();
                return;
            }
            if (autoBlockRange.getValue() && mc.player.distanceTo(target) >= range.getValue()) {
                reset();
                return;
            }
            if (smart.getValue() && target.hurtTime == 0) {
                reset();
                return;
            }
            if (attacking && ItemUtil.isHoldingSword()) {
                if (autoBlockTime.getValue() == 0) {
                    if (timer.hasTimeElapsed(blockDelay.getValue())) {
                        if (this.autoMode.getValue() == 0) {
                            KeyBindUtil.pressKeyOnce(this.getKeyCode(mc.options.useKey));
                            timer.reset();
                            reset();
                        }
                        if (this.autoMode.getValue() == 1) {
                            startBlocking = true;
                        }
                        if (startBlocking) {
                            KeyBindUtil.setKeyBindState(this.getKeyCode(mc.options.useKey), true);
                            holdTicks++;
                        }
                        if (holdTicks > holdTick.getValue()) {
                            KeyBindUtil.setKeyBindState(this.getKeyCode(mc.options.useKey), false);
                            startBlocking = false;
                            holdTicks = 0;
                            timer.reset();
                        }
                    }
                }
                if (autoBlockTime.getValue() == 1) {
                    if (mc.player.hurtTime >= minHurtTime.getValue() && mc.player.hurtTime <= maxHurtTime.getValue()) {
                        KeyBindUtil.setKeyBindState(this.getKeyCode(mc.options.useKey), true);
                        startBlocking = true;
                    } else if (startBlocking) {
                        KeyBindUtil.setKeyBindState(this.getKeyCode(mc.options.useKey), false);
                        startBlocking = false;
                    }
                }
                if (autoBlockTime.getValue() == 2) {
                    if (sagTicks < 10) {
                        KeyBindUtil.setKeyBindState(this.getKeyCode(mc.options.useKey), true);
                        sagTicks++;
                    }
                    if (sagTicks >= 10) {
                        KeyBindUtil.updateKeyState(this.getKeyCode(mc.options.useKey));
                        sagTicks = 0;
                    }
                }
                if (autoBlockTime.getValue() == 3) {
                    if (mc.player.hurtTime <= 2) {
                        canBlock = true;
                    }
                    if (canBlock) {
                        getBlockTicks++;
                        KeyBindUtil.setKeyBindState(this.getKeyCode(mc.options.useKey), true);
                    }
                    if (getBlockTicks > smartBlockTick.getValue()) {
                        canBlock = false;
                        KeyBindUtil.updateKeyState(this.getKeyCode(mc.options.useKey));
                        getBlockTicks = 0;
                    }
                }
            }
        }

        if (this.mode.getValue() == 2) {
            if (mc.player.hurtTime == startHurtTime.getValue()) {
                blockTicks = 1;
                blinkTicks = 1;
                Myau.blinkManager.setBlinkState(true, BlinkModules.AUTO_BLOCK);
            }
            if (blockTicks >= 1) {
                KeyBindUtil.setKeyBindState(this.getKeyCode(mc.options.useKey), true);
                blockTicks++;
            }
            if (blinkTicks >= 1) {
                blinkTicks++;
            }
            if (blinkTicks > delayPacketTick.getValue()) {
                Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                blinkTicks = 0;
            }
            if (blockTicks > blockTick.getValue()) {
                KeyBindUtil.updateKeyState(this.getKeyCode(mc.options.useKey));
                blockTicks = 0;
            }
        }
    }

    private void reset() {
        attacking = false;
        canBlock = false;
        KeyBindUtil.updateKeyState(this.getKeyCode(mc.options.useKey));
        holdTicks = 0;
        sagTicks = 0;
        getBlockTicks = 0;
        timer.reset();
    }

    private void startBlock(ItemStack itemStack) {
        PacketUtil.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isEnabled() && ItemUtil.isHoldingSword()) {
            attacking = true;
            attackTicks = 0;
            target = (LivingEntity) event.getTarget();
            if (autoBlockTime.getValue() == 3) {
                if (mc.player.hurtTime == 0) canBlock = true;
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
