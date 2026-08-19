package laoqi123.module.modules.movement;

import laoqi123.Myau;
import laoqi123.enums.BlinkModules;
import laoqi123.enums.DelayModules;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.LivingUpdateEvent;
import laoqi123.event.impl.MoveInputEvent;
import laoqi123.event.impl.PacketEvent;
import laoqi123.event.impl.StrafeEvent;
import laoqi123.event.impl.TickEvent;
import laoqi123.event.impl.UpdateEvent;
import laoqi123.module.Module;
import laoqi123.value.properties.IntValue;
import laoqi123.value.properties.ModeValue;
import laoqi123.util.ItemUtil;
import laoqi123.util.PacketUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.PlayerInput;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Stuck extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Vanilla", "Heypixel"});

    public final IntValue stuckTicks = new IntValue("Stuck Ticks", 10, 1, 100);

    private double savedMotionX;
    private double savedMotionY;
    private double savedMotionZ;
    private int tick;
    private boolean using = false;

    private int stage = 0;
    private Packet<?> heypixelPacket;
    private float lastYaw;
    private float lastPitch;
    private boolean tryDisable = false;
    private final Queue<Packet<?>> heypixelPackets = new ConcurrentLinkedQueue<>();

    public Stuck() {
        super("Stuck", false, false);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mc.player != null) {
            if (enabled && !this.isEnabled()) {
                if (this.mode.getModeString().equals("Vanilla")) {
                    this.savedMotionX = mc.player.getVelocity().x;
                    this.savedMotionY = mc.player.getVelocity().y;
                    this.savedMotionZ = mc.player.getVelocity().z;
                    this.using = false;
                }
            } else if (!enabled && this.isEnabled()) {
                if (this.mode.getModeString().equals("Vanilla")) {
                    this.tick = 0;
                    this.using = false;
                    Myau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                    Myau.delayManager.setDelayState(false, DelayModules.VELOCITY);
                    mc.player.setVelocity(this.savedMotionX, this.savedMotionY, this.savedMotionZ);
                } else if (this.mode.getModeString().equals("Heypixel")) {
                    this.stage = 0;
                    this.using = false;
                    this.tryDisable = false;
                    this.heypixelPacket = null;
                    this.heypixelPackets.clear();
                }
            }
        }
        super.setEnabled(enabled);
    }

    @Override
    public void onDisabled() {
        if (mc.player != null && this.mode.getModeString().equals("Vanilla")) {
            this.using = false;
            Myau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
            Myau.delayManager.setDelayState(false, DelayModules.VELOCITY);
            mc.player.setVelocity(this.savedMotionX, this.savedMotionY, this.savedMotionZ);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (mc.player == null) {
            return;
        }
        if (this.mode.getModeString().equals("Vanilla")) {
            Myau.blinkManager.setBlinkState(true, BlinkModules.BLINK);
            KeyBinding.unpressAll();
            this.using = true;
            mc.player.setVelocity(0.0, 0.0, 0.0);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (mc.player == null) {
            return;
        }
        if (event.getType() == EventType.SEND) {
            if (mc.currentScreen instanceof HandledScreen && event.getPacket() instanceof PlayerMoveC2SPacket) {
                event.setCancelled(true);
            }
            if (this.mode.getModeString().equals("Vanilla")) {
                if (event.getPacket() instanceof ClickSlotC2SPacket) {
                    Myau.delayManager.delayedPacket.offer((Packet<ClientPlayPacketListener>) event.getPacket());
                    event.setCancelled(true);
                }
            } else if (this.mode.getModeString().equals("Heypixel")) {
                if (event.getPacket() instanceof KeepAliveC2SPacket || event.getPacket() instanceof ClickSlotC2SPacket) {
                    this.heypixelPackets.add(event.getPacket());
                    event.setCancelled(true);
                }
                if (event.getPacket() instanceof PlayerInteractBlockC2SPacket || event.getPacket() instanceof PlayerActionC2SPacket) {
                    if (this.stage == 0) {
                        this.heypixelPacket = event.getPacket();
                        this.stage = 1;
                        event.setCancelled(true);
                    }
                }
            }
        } else if (event.getType() == EventType.RECEIVE) {
            if (this.mode.getModeString().equals("Vanilla")) {
                if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket velocityPacket) {
                    if (velocityPacket.getEntityId() == mc.player.getId()) {
                        Myau.delayManager.setDelayState(true, DelayModules.VELOCITY);
                        this.tick = this.stuckTicks.getValue();
                        Myau.delayManager.delayedPacket.offer(velocityPacket);
                        event.setCancelled(true);
                    }
                }
            } else if (this.mode.getModeString().equals("Heypixel")) {
                if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
                    this.flushPackets();
                    this.stage = 3;
                    this.setEnabled(false);
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.mode.getModeString().equals("Vanilla")) {
            if (this.tick == this.stuckTicks.getValue()) {
                this.setEnabled(false);
                this.using = true;
            }
            if (this.tick == this.stuckTicks.getValue() + 1) {
                this.setEnabled(true);
                this.tick = 0;
            }
            this.tick++;
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.mode.getModeString().equals("Vanilla")) {
            mc.player.input.movementForward = 0.0f;
            mc.player.input.movementSideways = 0.0f;
            PlayerInput playerInput = mc.player.input.playerInput;
            mc.player.input.playerInput = new PlayerInput(playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right(), false, false, playerInput.sprint());
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.mode.getModeString().equals("Vanilla")) {
            mc.player.setVelocity(0.0, 0.0, 0.0);
        } else if (this.mode.getModeString().equals("Heypixel")) {
            this.update();
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.mode.getModeString().equals("Vanilla")) {
            event.setForward(0.0F);
            event.setStrafe(0.0F);
        }
    }

    private void flushPackets() {
        while (!this.heypixelPackets.isEmpty()) {
            PacketUtil.sendPacketNoEvent(this.heypixelPackets.poll());
        }
    }

    private void updateRotation(float yaw, float pitch) {
        if (mc.player.getYaw() != yaw || mc.player.getPitch() != pitch) {
            PacketUtil.sendPacketNoEvent(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }
    }

    private boolean shouldRotate() {
        if (this.heypixelPacket instanceof PlayerInteractBlockC2SPacket) {
            ItemStack item = mc.player.getMainHandStack();
            return item != null && !ItemUtil.isEating() && !(item.getItem() instanceof BowItem);
        }
        if (this.heypixelPacket instanceof PlayerActionC2SPacket digPacket) {
            return digPacket.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM
                    && mc.player.isUsingItem()
                    && mc.player.getActiveItem().getItem() instanceof BowItem;
        }
        return false;
    }

    private void update() {
        if (this.stage == 1) {
            this.stage = 2;
            if (this.shouldRotate()) {
                this.updateRotation(mc.player.getYaw(), 89.9f);
            }
            this.flushPackets();
            PacketUtil.sendPacketNoEvent(this.heypixelPacket);
            this.heypixelPackets.clear();
            this.heypixelPacket = null;
        }
    }
}
