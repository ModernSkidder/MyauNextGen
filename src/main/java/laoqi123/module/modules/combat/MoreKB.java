package laoqi123.module.modules.combat;

import laoqi123.event.EventTarget;
import laoqi123.event.impl.AttackEvent;
import laoqi123.event.impl.TickEvent;
import laoqi123.module.Module;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.ModeValue;
import laoqi123.util.PacketUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;

public class MoreKB extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final ModeValue mode = new ModeValue("mode", 0, new String[]{"Legit", "Legit_Fast", "Less_Packet", "Packet", "Double_Packet"});
    public final BooleanValue intelligent = new BooleanValue("intelligent", false);
    public final BooleanValue onlyGround = new BooleanValue("only-ground", true);
    private boolean shouldSprintReset;
    private LivingEntity target;

    public MoreKB() {
        super("MoreKB", false);
        this.shouldSprintReset = false;
        this.target = null;
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        Entity targetEntity = event.getTarget();
        if (targetEntity != null && targetEntity instanceof LivingEntity) {
            this.target = (LivingEntity) targetEntity;
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.mode.getValue() == 1) {
            if (this.target != null && this.isMoving()) {
                if ((this.onlyGround.getValue() && mc.player.isOnGround()) || !this.onlyGround.getValue()) {
                    mc.player.setSprinting(false);
                }
                this.target = null;
            }
            return;
        }
        LivingEntity entity = null;
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY && ((EntityHitResult) mc.crosshairTarget).getEntity() instanceof LivingEntity) {
            entity = (LivingEntity) ((EntityHitResult) mc.crosshairTarget).getEntity();
        }
        if (entity == null) {
            return;
        }
        double x = mc.player.getX() - entity.getX();
        double z = mc.player.getZ() - entity.getZ();
        float calcYaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI - 90.0);
        float diffY = Math.abs(MathHelper.wrapDegrees(calcYaw - entity.getHeadYaw()));
        if (this.intelligent.getValue() && diffY > 120.0F) {
            return;
        }
        if (entity.hurtTime == 10) {
            switch (this.mode.getValue()) {
                case 0:
                    this.shouldSprintReset = true;
                    if (mc.player.isSprinting()) {
                        mc.player.setSprinting(false);
                        mc.player.setSprinting(true);
                    }
                    this.shouldSprintReset = false;
                    break;
                case 2:
                    if (mc.player.isSprinting()) {
                        mc.player.setSprinting(false);
                    }
                    PacketUtil.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                    mc.player.setSprinting(true);
                    break;
                case 3:
                    PacketUtil.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                    PacketUtil.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                    mc.player.setSprinting(true);
                    break;
                case 4:
                    PacketUtil.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                    PacketUtil.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                    PacketUtil.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                    PacketUtil.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                    mc.player.setSprinting(true);
                    break;
            }
        }
    }

    private boolean isMoving() {
        return mc.player.input.movementForward != 0.0F || mc.player.input.movementSideways != 0.0F;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
