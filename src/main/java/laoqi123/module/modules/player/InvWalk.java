package laoqi123.module.modules.player;

import com.google.common.base.CaseFormat;
import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.event.impl.PacketEvent;
import laoqi123.event.impl.TickEvent;
import laoqi123.event.impl.UpdateEvent;
import laoqi123.mixin.CloseHandledScreenC2SPacketAccessor;
import laoqi123.module.Module;
import laoqi123.module.modules.movement.Sprint;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.IntValue;
import laoqi123.value.properties.ModeValue;
import laoqi123.util.KeyBindUtil;
import laoqi123.util.PacketUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InvWalk extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final Queue<ClickSlotC2SPacket> clickQueue = new ConcurrentLinkedQueue<>();
    private boolean keysPressed = false;
    private ClientCommandC2SPacket pendingStatus = null;
    private int delayTicks = 0;
    private int openDelayTicks = -1;
    private int closeDelayTicks = -1;
    private Map<KeyBinding, Boolean> movementKeys = null;

    private Map<KeyBinding, Boolean> getMovementKeys() {
        if (movementKeys == null) {
            movementKeys = new HashMap<>();
            movementKeys.put(mc.options.forwardKey, false);
            movementKeys.put(mc.options.backKey, false);
            movementKeys.put(mc.options.leftKey, false);
            movementKeys.put(mc.options.rightKey, false);
            movementKeys.put(mc.options.jumpKey, false);
            movementKeys.put(mc.options.sneakKey, false);
            movementKeys.put(mc.options.sprintKey, false);
        }
        return movementKeys;
    }

    public final ModeValue mode = new ModeValue("mode", 1, new String[]{"VANILLA", "LEGIT", "HYPIXEL", "LEGIT+"});
    public final BooleanValue guiEnabled = new BooleanValue("click-gui", true);
    public final IntValue openDelay = new IntValue("open-delay", 0, 0, 20, () -> mode.getValue() == 3);
    public final IntValue closeDelay = new IntValue("close-delay", 4, 0, 20, () -> mode.getValue() == 3);
    public final BooleanValue lockMoveKey = new BooleanValue("lock-move-dey", false);

    public InvWalk() {
        super("InvWalk", false);
    }

    public void pressMovementKeys(boolean skipSneak) {
        this.getMovementKeys().keySet().stream()
                .filter(key -> !skipSneak || key != mc.options.sneakKey)
                .forEach(key -> KeyBindUtil.updateKeyState(key));
        if (Myau.moduleManager.modules.get(Sprint.class).isEnabled()) {
            KeyBindUtil.setKeyBindState(mc.options.sprintKey, true);
        }
        this.keysPressed = true;
    }

    public void resetMovementKeys() {
        this.getMovementKeys().replaceAll((k, v) -> false);
    }

    public boolean isSetMovementKeys() {
        return this.getMovementKeys().values().stream().anyMatch(Boolean::booleanValue);
    }

    public void storeMovementKeys() {
        this.getMovementKeys().replaceAll((k, v) -> KeyBindUtil.isKeyDown(k));
    }

    public void restoreMovementKeys() {
        for (Map.Entry<KeyBinding, Boolean> keyBinding : this.getMovementKeys().entrySet()) {
            KeyBindUtil.setKeyBindState(keyBinding.getKey(), keyBinding.getValue());
        }
        if (Myau.moduleManager.modules.get(Sprint.class).isEnabled()) {
            KeyBindUtil.setKeyBindState(mc.options.sprintKey, true);
        }
        this.keysPressed = true;
    }

    public boolean canInvWalk() {
        if (!(mc.currentScreen instanceof HandledScreen)) return false;
        if (mc.currentScreen instanceof CreativeInventoryScreen) return false;

        switch (this.mode.getValue()) {
            case 0: // Vanilla
                return true;
            case 1: // Legit
                if (!(mc.currentScreen instanceof InventoryScreen)) return false;
                return this.pendingStatus != null && this.clickQueue.isEmpty();
            case 2: // Hypixel
                return this.delayTicks == 0 && this.clickQueue.isEmpty();
            case 3: // Legit+
                if (!(mc.currentScreen instanceof InventoryScreen)) return false;
                return this.closeDelayTicks == -1 && this.clickQueue.isEmpty();
            default:
                return false;
        }
    }

    public boolean temporaryStackIsEmpty() {
        if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) return false;
        if (mc.player.playerScreenHandler instanceof PlayerScreenHandler) {
            PlayerScreenHandler screenHandler = (PlayerScreenHandler) mc.player.playerScreenHandler;
            for (int i = PlayerScreenHandler.CRAFTING_INPUT_START; i < PlayerScreenHandler.CRAFTING_INPUT_END; i++) {
                Slot slot = screenHandler.slots.get(i);
                if (slot.hasStack()) {
                    return false;
                }
            }
        }
        return true;
    }

    @EventTarget(Priority.LOWEST)
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.openDelayTicks >= 0) {
                this.openDelayTicks--;
                return;
            }
            while (!this.clickQueue.isEmpty()) {
                PacketUtil.sendPacketNoEvent(this.clickQueue.poll());
            }
            if (this.closeDelayTicks > 0) {
                if (this.temporaryStackIsEmpty()) {
                    this.closeDelayTicks--;
                }
            } else if (this.closeDelayTicks == 0) {
                if (mc.currentScreen instanceof InventoryScreen)
                    PacketUtil.sendPacketNoEvent(new CloseHandledScreenC2SPacket(0));
                this.closeDelayTicks = -1;
            }
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) return;

        if (mc.currentScreen instanceof laoqi123.ui.ClickGui && this.guiEnabled.getValue()) {
            this.pressMovementKeys(true);
            return;
        }

        if (this.canInvWalk()) {
            if (this.isSetMovementKeys() && this.lockMoveKey.getValue()) {
                this.restoreMovementKeys();
            } else {
                this.pressMovementKeys(true);
            }
        } else {
            if (this.keysPressed) {
                if (mc.currentScreen != null) {
                    KeyBinding.unpressAll();
                } else if (this.isSetMovementKeys()) {
                    this.resetMovementKeys();
                    this.pressMovementKeys(false);
                }
                this.keysPressed = false;
            }
            if (this.pendingStatus != null) {
                PacketUtil.sendPacketNoEvent(this.pendingStatus);
                this.pendingStatus = null;
            }
            if (this.delayTicks > 0) {
                this.delayTicks--;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.SEND) return;

        if (event.getPacket() instanceof ClientCommandC2SPacket) {
            this.storeMovementKeys();
            if (this.mode.getValue() == 1 || this.mode.getValue() == 3) {
                ClientCommandC2SPacket packet = (ClientCommandC2SPacket) event.getPacket();
                if (packet.getMode() == ClientCommandC2SPacket.Mode.OPEN_INVENTORY) {
                    event.setCancelled(true);
                    if (this.mode.getValue() == 1) {
                        this.pendingStatus = packet;
                    }
                }
            }
        } else if (!(event.getPacket() instanceof ClickSlotC2SPacket)) {
            if (event.getPacket() instanceof CloseHandledScreenC2SPacket) {
                CloseHandledScreenC2SPacket packet = (CloseHandledScreenC2SPacket) event.getPacket();
                if (((CloseHandledScreenC2SPacketAccessor) packet).getWindowId() == 0) {
                    if (this.mode.getValue() == 3) {
                        if (!this.clickQueue.isEmpty()) {
                            this.clickQueue.clear();
                        }
                        if (this.openDelayTicks >= 0) {
                            this.openDelayTicks = -1;
                        }
                        if (this.closeDelayTicks >= 0) {
                            this.closeDelayTicks = -1;
                        } else {
                            event.setCancelled(true);
                        }
                    } else if (this.pendingStatus != null) {
                        this.pendingStatus = null;
                        event.setCancelled(true);
                    }
                } else {
                    if (!this.clickQueue.isEmpty()) {
                        this.clickQueue.clear();
                    }
                    if (this.openDelayTicks >= 0) {
                        this.openDelayTicks = -1;
                    }
                    if (this.closeDelayTicks >= 0) {
                        this.closeDelayTicks = -1;
                    }
                }
            }
        } else {
            ClickSlotC2SPacket packet = (ClickSlotC2SPacket) event.getPacket();
            switch (this.mode.getValue()) {
                case 1: // Legit
                    if (packet.getSyncId() == 0) {
                        if ((packet.getActionType() == SlotActionType.CLONE || packet.getActionType() == SlotActionType.THROW) && packet.getSlot() == ScreenHandler.EMPTY_SPACE_SLOT_INDEX) {
                            event.setCancelled(true);
                            return;
                        }
                        if (this.pendingStatus != null) {
                            KeyBinding.unpressAll();
                            event.setCancelled(true);
                            this.clickQueue.offer(packet);
                        }
                    }
                    break;
                case 2: // Hypixel
                    if ((packet.getActionType() == SlotActionType.CLONE || packet.getActionType() == SlotActionType.THROW) && packet.getSlot() == ScreenHandler.EMPTY_SPACE_SLOT_INDEX) {
                        event.setCancelled(true);
                    } else {
                        KeyBinding.unpressAll();
                        event.setCancelled(true);
                        this.clickQueue.offer(packet);
                        this.delayTicks = 8;
                    }
                    break;
                case 3: // Legit+
                    if (packet.getSyncId() == 0) {
                        if ((packet.getActionType() == SlotActionType.CLONE || packet.getActionType() == SlotActionType.THROW) && packet.getSlot() == ScreenHandler.EMPTY_SPACE_SLOT_INDEX) {
                            event.setCancelled(true);
                            return;
                        }
                        KeyBinding.unpressAll();
                        event.setCancelled(true);
                        this.clickQueue.offer(packet);
                        if (this.closeDelayTicks < 0 && this.openDelayTicks < 0) {
                            this.pendingStatus = new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.OPEN_INVENTORY);
                            this.openDelayTicks = openDelay.getValue();
                        }
                        this.closeDelayTicks = closeDelay.getValue();
                    }
                    break;
            }
            if (this.pendingStatus != null) {
                PacketUtil.sendPacketNoEvent(this.pendingStatus);
                this.pendingStatus = null;
            }
        }
    }

    @Override
    public void onDisabled() {
        if (this.keysPressed) {
            if (mc.currentScreen != null) {
                KeyBinding.unpressAll();
            }
            this.keysPressed = false;
        }
        if (this.pendingStatus != null) {
            PacketUtil.sendPacketNoEvent(this.pendingStatus);
            this.pendingStatus = null;
        }
        this.delayTicks = 0;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
