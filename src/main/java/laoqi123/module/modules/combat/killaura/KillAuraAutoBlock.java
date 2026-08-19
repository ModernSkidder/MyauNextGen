package laoqi123.module.modules.combat.killaura;

import laoqi123.Myau;
import laoqi123.enums.BlinkModules;
import laoqi123.module.modules.combat.KillAura;
import laoqi123.property.properties.*;
import laoqi123.util.PacketUtil;
import laoqi123.util.PlayerUtil;
import laoqi123.util.RotationUtil;
import laoqi123.util.config.ToggleableConfigurable;
import laoqi123.util.ItemUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.UnknownCustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class KillAuraAutoBlock extends ToggleableConfigurable {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeProperty mode;
    private final BooleanProperty noSwap;
    private final BooleanProperty test;
    private final IntProperty moreAttackDelay;
    private final IntProperty maxTick;
    private final IntProperty startBlinkTick;
    private final IntProperty stopBlinkTick;
    private final IntProperty swapTick;
    private final IntProperty switchBackTick;
    private final IntProperty stopBlockTick;
    public final IntProperty attackTick;
    private final IntProperty startBlockTick;
    private final BooleanProperty postStartBlock;
    public final BooleanProperty requirePress;
    public final IntProperty aps;
    public final FloatProperty range;

    private boolean blockingState = false;
    private boolean isBlocking = false;
    private boolean fakeBlockState = false;
    private int blockTick = 0;
    private boolean swapped = false;
    private boolean postBlock = false;
    private boolean postSwap = false;
    private int testAttackTick = 0;

    public KillAuraAutoBlock(KillAura module) {
        super("AutoBlock", true);
        this.mode = this.register(new ModeProperty("AutoBlock", 0, new String[]{"None", "Vanilla", "Hypixel", "Legit", "Fake", "Hypixel Test", "Hypixel Custom"}));
        this.noSwap = this.register(new BooleanProperty("NoSwap", true, () -> this.mode.getValue() == 2));
        this.test = this.register(new BooleanProperty("MoreAttack", false, () -> this.mode.getValue() == 2));
        this.moreAttackDelay = this.register(new IntProperty("MoreAttackDelay", 1, 0, 3, () -> this.mode.getValue() == 2 && this.test.getValue()));
        this.maxTick = this.register(new IntProperty("MaxTick", 3, 1, 5, () -> this.mode.getValue() == 6));
        this.startBlinkTick = this.register(new IntProperty("StartBlinkTick", 0, 1, 5, () -> this.mode.getValue() == 6));
        this.stopBlinkTick = this.register(new IntProperty("StopBlinkTick", 2, 1, 5, () -> this.mode.getValue() == 6));
        this.swapTick = this.register(new IntProperty("SwapTick", 2, 1, 5, () -> this.mode.getValue() == 6));
        this.switchBackTick = this.register(new IntProperty("SwitchBackTick", 2, 1, 5, () -> this.mode.getValue() == 6));
        this.stopBlockTick = this.register(new IntProperty("StopBlockTick", 2, 1, 5, () -> this.mode.getValue() == 6));
        this.attackTick = this.register(new IntProperty("AttackTick", 0, 1, 5, () -> this.mode.getValue() == 6));
        this.startBlockTick = this.register(new IntProperty("StartBlockTick", 0, 1, 5, () -> this.mode.getValue() == 6));
        this.postStartBlock = this.register(new BooleanProperty("PostBlock", false, () -> this.mode.getValue() == 6));
        this.requirePress = this.register(new BooleanProperty("AutoBlock Require Press", false));
        this.aps = this.register(new IntProperty("AutoBlock Aps", 10, 1, 20));
        this.range = this.register(new FloatProperty("AutoBlock Range", 6.0F, 3.0F, 8.0F));
    }

    public boolean isBlocking() {
        return this.fakeBlockState && ItemUtil.isHoldingSword();
    }

    public boolean isPlayerBlocking() {
        return (mc.player.isUsingItem() || this.blockingState) && ItemUtil.isHoldingSword();
    }

    public boolean canAutoBlock() {
        if (!this.running()) {
            return false;
        }
        if (!ItemUtil.isHoldingSword()) {
            return false;
        }
        return !this.requirePress.getValue() || PlayerUtil.isUsingItem();
    }

    public int getBlockTick() {
        return this.blockTick;
    }

    public boolean isPostSwap() {
        return this.postSwap;
    }

    public boolean isPostBlock() {
        return this.postBlock;
    }

    public boolean shouldAutoBlock() {
        if (this.isPlayerBlocking() && this.isBlocking) {
            int mode = this.mode.getValue();
            return !mc.player.isTouchingWater() && !mc.player.isInLava()
                    && (mode == 2 || mode == 3 || mode == 5 || mode == 6 || mode == 7);
        }
        return false;
    }

    public void onReleaseUseItem() {
        this.blockingState = false;
    }

    public void onSlotChangePacket() {
        this.blockingState = false;
        if (this.isBlocking) {
            mc.player.clearActiveItem();
        }
    }

    public void reset() {
        this.blockingState = false;
        this.isBlocking = false;
        this.fakeBlockState = false;
        this.blockTick = 0;
        this.swapped = false;
        this.postBlock = false;
        this.postSwap = false;
    }

    public boolean handlePostTick() {
        boolean handled = false;
        if (this.postSwap) {
            int randomSlot = new Random().nextInt(9);
            while (randomSlot == mc.player.getInventory().selectedSlot) {
                randomSlot = new Random().nextInt(9);
            }
            PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(randomSlot));
            PacketUtil.sendPacketNoEvent(new CustomPayloadC2SPacket(new UnknownCustomPayload(Identifier.of("send"))));
            PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
            this.stopBlock();
            this.postSwap = false;
            handled = true;
        }
        if (this.postBlock) {
            this.sendUseItem();
            this.postBlock = false;
            handled = true;
        }
        return handled;
    }

    public void sendUseItem() {
        ((laoqi123.mixin.ClientPlayerInteractionManagerAccessor) mc.interactionManager).callSyncCurrentPlayItem();
        this.startBlock(mc.player.getMainHandStack());
    }

    public void startBlock(ItemStack itemStack) {
        PacketUtil.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, 0.0F, 0.0F));
        mc.player.setCurrentHand(Hand.MAIN_HAND);
        this.blockingState = true;
    }

    public void stopBlock() {
        PacketUtil.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
        mc.player.clearActiveItem();
        this.blockingState = false;
    }

    public void interactAttack(float yaw, float pitch, KillAura.AttackData targetData) {
        if (targetData != null) {
            HitResult mop = RotationUtil.rayTrace(targetData.getBox(), yaw, pitch, 8.0);
            if (mop != null) {
                ((laoqi123.mixin.ClientPlayerInteractionManagerAccessor) mc.interactionManager).callSyncCurrentPlayItem();
                Vec3d hitVec = mop.getPos();
                PacketUtil.sendPacket(
                        PlayerInteractEntityC2SPacket.interactAt(
                                targetData.getEntity(),
                                mc.player.isSneaking(),
                                Hand.MAIN_HAND,
                                new Vec3d(hitVec.x - targetData.getX(), hitVec.y - targetData.getY(), hitVec.z - targetData.getZ())
                        )
                );
                PacketUtil.sendPacket(PlayerInteractEntityC2SPacket.interact(targetData.getEntity(), mc.player.isSneaking(), Hand.MAIN_HAND));
                PacketUtil.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, 0.0F, 0.0F));
                mc.player.setCurrentHand(Hand.MAIN_HAND);
                this.blockingState = true;
            }
        }
    }

    public BlockResult updateBlocking(KillAura module, boolean attack) {
        boolean block = attack && this.canAutoBlock();
        if (!block) {
            Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
            this.isBlocking = false;
            this.fakeBlockState = false;
            this.blockTick = 0;
            return new BlockResult(attack, false, false);
        }

        boolean swap = false;
        boolean blocked = false;
        switch (this.mode.getValue()) {
            case 0:
                if (PlayerUtil.isUsingItem()) {
                    this.isBlocking = true;
                    if (!this.isPlayerBlocking() && !Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                        swap = true;
                    }
                } else {
                    this.isBlocking = false;
                    if (this.isPlayerBlocking() && !Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                        this.stopBlock();
                    }
                }
                Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                this.fakeBlockState = false;
                break;
            case 1:
                if (module.hasValidTarget()) {
                    if (!this.isPlayerBlocking() && !Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                        swap = true;
                    }
                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    this.isBlocking = true;
                    this.fakeBlockState = false;
                } else {
                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    this.isBlocking = false;
                    this.fakeBlockState = false;
                }
                break;
            case 2:
                if (module.hasValidTarget()) {
                    if (!Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                        switch (this.blockTick) {
                            case 0:
                                if (!this.isPlayerBlocking()) {
                                    swap = true;
                                }
                                blocked = true;
                                this.blockTick = 1;
                                break;
                            case 1:
                                attack = false;
                                this.blockTick = 2;
                                break;
                            case 2:
                                if (this.isPlayerBlocking()) {
                                    if (!this.noSwap.getValue()) {
                                        int randomSlot = new Random().nextInt(9);
                                        while (randomSlot == mc.player.getInventory().selectedSlot) {
                                            randomSlot = new Random().nextInt(9);
                                        }
                                        PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(randomSlot));
                                        PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                                    }
                                    this.stopBlock();
                                }
                                if (this.test.getValue()) {
                                    if (this.testAttackTick >= this.moreAttackDelay.getValue()) {
                                        this.testAttackTick = 0;
                                    } else {
                                        this.testAttackTick++;
                                        attack = false;
                                    }
                                } else {
                                    attack = false;
                                }
                                this.blockTick = 0;
                                break;
                            default:
                                this.blockTick = 0;
                                break;
                        }
                    }
                    this.isBlocking = true;
                    this.fakeBlockState = true;
                } else {
                    int randomSlot = new Random().nextInt(9);
                    while (randomSlot == mc.player.getInventory().selectedSlot) {
                        randomSlot = new Random().nextInt(9);
                    }
                    PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(randomSlot));
                    PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    this.isBlocking = false;
                    this.fakeBlockState = false;
                }
                break;
            case 3:
                if (module.hasValidTarget()) {
                    if (!Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                        switch (this.blockTick) {
                            case 0:
                                if (!this.isPlayerBlocking()) {
                                    swap = true;
                                }
                                this.blockTick = 1;
                                break;
                            case 1:
                                if (this.isPlayerBlocking()) {
                                    this.stopBlock();
                                    attack = false;
                                }
                                if (module.getAttackDelayMS() <= 50L) {
                                    this.blockTick = 0;
                                }
                                break;
                            default:
                                this.blockTick = 0;
                        }
                    }
                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    this.isBlocking = true;
                    this.fakeBlockState = false;
                } else {
                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    this.isBlocking = false;
                    this.fakeBlockState = false;
                }
                break;
            case 4:
                Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                this.isBlocking = false;
                this.fakeBlockState = module.hasValidTarget();
                if (PlayerUtil.isUsingItem()
                        && !this.isPlayerBlocking()
                        && !Myau.playerStateManager.digging
                        && !Myau.playerStateManager.placing) {
                    swap = true;
                }
                break;
            case 5:
                if (module.hasValidTarget()) {
                    if (!Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                        switch (this.blockTick) {
                            case 0:
                                blocked = true;
                                if (!this.isPlayerBlocking()) {
                                    swap = true;
                                }
                                this.blockTick = 1;
                                break;
                            case 1:
                                if (this.isPlayerBlocking()) {
                                    int randomSlot = new Random().nextInt(9);
                                    while (randomSlot == mc.player.getInventory().selectedSlot) {
                                        randomSlot = new Random().nextInt(9);
                                    }
                                    PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(randomSlot));
                                    PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                                }
                                attack = false;
                                this.blockTick = 2;
                                break;
                            case 2:
                                attack = false;
                                this.stopBlock();
                                if (module.getAttackDelayMS() <= 50L) {
                                    this.blockTick = 0;
                                }
                                break;
                            default:
                                this.blockTick = 0;
                        }
                    }
                    this.isBlocking = true;
                    this.fakeBlockState = true;
                } else {
                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    int randomSlot = new Random().nextInt(9);
                    while (randomSlot == mc.player.getInventory().selectedSlot) {
                        randomSlot = new Random().nextInt(9);
                    }
                    PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(randomSlot));
                    PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                    this.isBlocking = false;
                    this.fakeBlockState = false;
                }
                break;
            case 6:
                if (module.hasValidTarget()) {
                    if (!Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                        if (this.blockTick + 1 == this.startBlinkTick.getValue()) {
                            blocked = true;
                        }
                        if (this.blockTick + 1 != this.attackTick.getValue()) {
                            attack = false;
                        }
                        if (this.blockTick + 1 == this.startBlockTick.getValue()) {
                            if (!this.isPlayerBlocking()) {
                                swap = true;
                                if (this.postStartBlock.getValue()) {
                                    this.postBlock = true;
                                }
                            }
                        }
                        if (this.blockTick + 1 == this.stopBlinkTick.getValue()) {
                            Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                        }
                        if (this.blockTick + 1 == this.swapTick.getValue()) {
                            int randomSlot = new Random().nextInt(9);
                            while (randomSlot == mc.player.getInventory().selectedSlot) {
                                randomSlot = new Random().nextInt(9);
                            }
                            PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(randomSlot));
                            this.swapped = true;
                        }
                        if (this.blockTick + 1 == this.switchBackTick.getValue()) {
                            if (this.swapped) {
                                PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                                this.swapped = false;
                            }
                        }
                        if (this.blockTick + 1 == this.stopBlockTick.getValue()) {
                            if (this.isPlayerBlocking()) {
                                this.stopBlock();
                            }
                        }
                        this.blockTick++;
                        if (this.blockTick >= this.maxTick.getValue() - 1) {
                            this.blockTick = 0;
                        }
                    }
                    this.isBlocking = true;
                    this.fakeBlockState = true;
                } else {
                    if (this.swapped) {
                        PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                        this.swapped = false;
                    }
                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    this.isBlocking = false;
                    this.fakeBlockState = false;
                }
                break;
        }

        return new BlockResult(attack, swap, blocked);
    }

    public static class BlockResult {
        public boolean attack;
        public boolean swap;
        public boolean blocked;

        public BlockResult(boolean attack, boolean swap, boolean blocked) {
            this.attack = attack;
            this.swap = swap;
            this.blocked = blocked;
        }
    }
}
