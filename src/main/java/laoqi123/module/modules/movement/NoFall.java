package laoqi123.module.modules.movement;

import com.google.common.base.CaseFormat;
import laoqi123.Myau;
import laoqi123.enums.BlinkModules;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.events.MoveInputEvent;
import laoqi123.events.PacketEvent;
import laoqi123.events.TickEvent;
import laoqi123.events.UpdateEvent;
import laoqi123.management.RotationState;
import laoqi123.mixin.PlayerMoveC2SPacketAccessor;
import laoqi123.module.Module;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.property.properties.ModeProperty;
import laoqi123.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class NoFall extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final TimerUtil packetDelayTimer = new TimerUtil();
    private final TimerUtil scoreboardResetTimer = new TimerUtil();
    private boolean slowFalling = false;
    private boolean lastOnGround = false;

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"Packet", "Blink", "No_Ground", "Spoof", "MLG"});

    public final FloatProperty distance = new FloatProperty("distance", 3.0F, 0.0F, 20.0F);
    public final IntProperty delay = new IntProperty("delay", 0, 0, 10000);

    public final BooleanProperty autoSwitch = new BooleanProperty("Auto Switch", true, () -> mode.getValue() == 4);
    public final ModeProperty moveFix = new ModeProperty("Move Fix", 1, new String[]{"NONE", "SILENT"}, () -> mode.getValue() == 4);
    public final IntProperty priority = new IntProperty("Priority", 2, 1, 10, () -> mode.getValue() == 4);

    private boolean active = false;
    private boolean onDistance = false;
    private boolean prevOnGround = false;
    private double highestY = 0.0;
    private float originalYaw = 0.0f;
    private boolean firstClickDone = false;
    private boolean secondClickDone = false;
    private int lastSlot = -1;

    private boolean canTrigger() {
        return this.scoreboardResetTimer.hasTimeElapsed(3000) && this.packetDelayTimer.hasTimeElapsed(this.delay.getValue().longValue());
    }

    public NoFall() {
        super("NoFall", false);
    }

    @EventTarget(Priority.HIGH)
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            if (mode.getValue() == 4) {
                resetMLGState();
                restoreSlot();
            } else {
                this.onDisabled();
            }
        } else if (this.isEnabled() && event.getType() == EventType.SEND && !event.isCancelled()) {
            if (mode.getValue() == 4) return;
            if (event.getPacket() instanceof PlayerMoveC2SPacket) {
                PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket) event.getPacket();
                switch (this.mode.getValue()) {
                    case 0:
                        if (this.slowFalling) {
                            this.slowFalling = false;
                        } else if (!packet.isOnGround()) {
                            Box aabb = mc.player.getBoundingBox().expand(2.0, 0.0, 2.0);
                            if (PlayerUtil.canFly(this.distance.getValue())
                                    && !PlayerUtil.checkInWater(aabb)
                                    && this.canTrigger()) {
                                this.packetDelayTimer.reset();
                                this.slowFalling = true;
                            }
                        }
                        break;
                    case 1:
                        boolean allowed = !mc.player.isClimbing() && !mc.player.getAbilities().allowFlying && mc.player.hurtTime == 0;
                        if (Myau.blinkManager.getBlinkingModule() != BlinkModules.NO_FALL) {
                            if (this.lastOnGround
                                    && !packet.isOnGround()
                                    && allowed
                                    && PlayerUtil.canFly(this.distance.getValue().intValue())
                                    && mc.player.getVelocity().y < 0.0) {
                                Myau.blinkManager.setBlinkState(false, Myau.blinkManager.getBlinkingModule());
                                Myau.blinkManager.setBlinkState(true, BlinkModules.NO_FALL);
                            }
                        } else if (!allowed) {
                            Myau.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
                            ChatUtil.sendFormatted(String.format("%s%s: &cFailed player check!&r", Myau.clientName, this.getName()));
                        } else if (PlayerUtil.checkInWater(mc.player.getBoundingBox().expand(2.0, 0.0, 2.0))) {
                            Myau.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
                            ChatUtil.sendFormatted(String.format("%s%s: &cFailed void check!&r", Myau.clientName, this.getName()));
                        } else if (packet.isOnGround()) {
                            for (Packet<?> blinkedPacket : Myau.blinkManager.blinkedPackets) {
                                if (blinkedPacket instanceof PlayerMoveC2SPacket) {
                                    ((PlayerMoveC2SPacketAccessor) blinkedPacket).setOnGround(true);
                                }
                            }
                            Myau.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
                            this.packetDelayTimer.reset();
                        }
                        this.lastOnGround = packet.isOnGround() && allowed && this.canTrigger();
                        break;
                    case 2:
                        ((PlayerMoveC2SPacketAccessor) packet).setOnGround(false);
                        break;
                    case 3:
                        if (!packet.isOnGround()) {
                            Box aabb = mc.player.getBoundingBox().expand(2.0, 0.0, 2.0);
                            if (PlayerUtil.canFly(this.distance.getValue())
                                    && !PlayerUtil.checkInWater(aabb)
                                    && this.canTrigger()) {
                                this.packetDelayTimer.reset();
                                ((PlayerMoveC2SPacketAccessor) packet).setOnGround(true);
                                mc.player.fallDistance = 0.0F;
                            }
                        }
                }
            }
        }
    }

    @EventTarget(Priority.HIGHEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mode.getValue() == 4) return;
            if (ServerUtil.hasPlayerCountInfo()) {
                this.scoreboardResetTimer.reset();
            }
            if (this.mode.getValue() == 0 && this.slowFalling) {
                PacketUtil.sendPacketNoEvent(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
                mc.player.fallDistance = 0.0F;
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled() || event.getType() != EventType.PRE) return;
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        if (mode.getValue() == 4) {
            Module scaffold = Myau.moduleManager.getModule("Scaffold");
            if (scaffold != null && scaffold.isEnabled()) {
                if (active) {
                    restoreSlot();
                    resetMLGState();
                }
                return;
            }

            fallCheck();

            if (!active && onDistance && !mc.player.isOnGround()) {
                active = true;
                originalYaw = mc.player.getYaw();
                firstClickDone = false;
                secondClickDone = false;

                if (autoSwitch.getValue()) {
                    lastSlot = mc.player.getInventory().selectedSlot;
                    int bucketSlot = findWaterBucketSlot();
                    if (bucketSlot != -1) {
                        mc.player.getInventory().selectedSlot = bucketSlot;
                    }
                }
            }

            if (active) {
                if (mc.player.isOnGround() && !secondClickDone) {
                    performRightClick();
                    secondClickDone = true;
                    active = false;
                    restoreSlot();
                    return;
                }

                if (autoSwitch.getValue()) {
                    ItemStack held = mc.player.getMainHandStack();
                    if (held == null || held.isEmpty() || held.getItem() != Items.WATER_BUCKET) {
                        active = false;
                        restoreSlot();
                        return;
                    }
                }

                event.setRotation(originalYaw, 90.0f, priority.getValue());
                event.setPervRotation(originalYaw, priority.getValue());

                if (!firstClickDone) {
                    double dist = getDistanceToGround();
                    if (dist >= 0 && dist <= 3.0) {
                        performRightClick();
                        firstClickDone = true;
                    }
                }
            }
        }
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (!isEnabled()) return;
        if (mode.getValue() != 4) return;

        Module scaffold = Myau.moduleManager.getModule("Scaffold");
        if (scaffold != null && scaffold.isEnabled()) {
            return;
        }

        if (active && moveFix.getValue() == 1
                && RotationState.isActived()
                && RotationState.getPriority() == priority.getValue()
                && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
        }
    }

    private void fallCheck() {
        boolean onGround = mc.player.isOnGround();
        if (onGround) {
            onDistance = false;
            highestY = mc.player.getY();
        } else if (prevOnGround) {
            highestY = mc.player.getY();
        } else {
            if (highestY - mc.player.getY() > 3.0) {
                onDistance = true;
            }
        }
        prevOnGround = onGround;
    }

    private double getDistanceToGround() {
        RotationUtil.RotationVec rotation = new RotationUtil.RotationVec(originalYaw, 90.0f);
        RayCastUtil.RayCastResult result = RayCastUtil.rayCast(rotation, 10.0, 0.0f);
        if (result != null && result.typeOfHit == RayCastUtil.RayCastResult.Type.BLOCK && result.hitVec != null) {
            double footY = mc.player.getBoundingBox().minY;
            return footY - result.hitVec.y;
        }
        return -1;
    }

    private void performRightClick() {
        RotationUtil.RotationVec rotation = new RotationUtil.RotationVec(originalYaw, 90.0f);
        RayCastUtil.RayCastResult result = RayCastUtil.rayCast(rotation, 10.0, 0.0f);
        if (result != null && result.typeOfHit == RayCastUtil.RayCastResult.Type.BLOCK && result.hitVec != null) {
            BlockPos blockPos = result.getBlockPos();
            Direction sideHit = result.sideHit;
            BlockHitResult blockHit = new BlockHitResult(result.hitVec, sideHit, blockPos, false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private int findWaterBucketSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && !stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET) {
                return i;
            }
        }
        return -1;
    }

    private void restoreSlot() {
        if (autoSwitch.getValue() && lastSlot != -1 && mc.player != null
                && mc.player.getInventory().selectedSlot != lastSlot) {
            mc.player.getInventory().selectedSlot = lastSlot;
        }
    }

    private void resetMLGState() {
        active = false;
        onDistance = false;
        prevOnGround = false;
        highestY = 0.0;
        firstClickDone = false;
        secondClickDone = false;
    }

    @Override
    public void onEnabled() {
        if (mode.getValue() == 4) {
            resetMLGState();
            if (mc.player != null) {
                originalYaw = mc.player.getYaw();
                highestY = mc.player.getY();
                prevOnGround = mc.player.isOnGround();
            }
        } else {
            this.lastOnGround = false;
        }
    }

    @Override
    public void onDisabled() {
        if (mode.getValue() == 4) {
            resetMLGState();
            restoreSlot();
        } else {
            this.lastOnGround = false;
            Myau.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
            if (this.slowFalling) {
                this.slowFalling = false;
            }
        }
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
