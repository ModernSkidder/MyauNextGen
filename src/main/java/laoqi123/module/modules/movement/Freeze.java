package laoqi123.module.modules.movement;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.MoveInputEvent;
import laoqi123.events.PacketEvent;
import laoqi123.module.Module;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.ModeProperty;
import laoqi123.util.ChatUtil;
import laoqi123.util.PacketUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.PlayerInput;

import java.util.concurrent.ConcurrentLinkedDeque;

public class Freeze extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"Queue", "Cancel", "Stationary"});
    public final BooleanProperty disableOnFlag = new BooleanProperty("disable-on-flag", true);
    public final BooleanProperty notification = new BooleanProperty("notification", false);

    private final ConcurrentLinkedDeque<Packet<?>> packetQueue = new ConcurrentLinkedDeque<>();
    private boolean dispatching = false;

    public Freeze() {
        super("Freeze", false);
    }

    @Override
    public void onEnabled() {
        packetQueue.clear();
        dispatching = false;
    }

    @Override
    public void onDisabled() {
        dispatching = true;
        while (!packetQueue.isEmpty()) {
            PacketUtil.sendPacket(packetQueue.poll());
        }
        packetQueue.clear();
        dispatching = false;
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (!this.enabled || mode.getValue() != 2 || mc.player == null) return;
        mc.player.input.playerInput = new PlayerInput(false, false, false, false, false, false, false);
        mc.player.input.movementSideways = 0.0F;
        mc.player.input.movementForward = 0.0F;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.enabled || dispatching || mc.player == null || mc.world == null) return;

        if (event.getType() == EventType.RECEIVE) {
            if (disableOnFlag.getValue() && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
                if (notification.getValue()) {
                    ChatUtil.sendFormatted("&cFreeze&r&8: &7Disabled on flag");
                }
                this.setEnabled(false);
            }
            return;
        }

        if (event.getType() != EventType.SEND) return;
        Packet<?> packet = event.getPacket();

        if (packet instanceof PlayerMoveC2SPacket) {
            event.setCancelled(true);
            if (mode.getValue() == 0) {
                packetQueue.add(packet);
            }
        } else if (mode.getValue() == 2 && packet instanceof CommonPongC2SPacket) {
            event.setCancelled(true);
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{mode.getModeString()};
    }
}
