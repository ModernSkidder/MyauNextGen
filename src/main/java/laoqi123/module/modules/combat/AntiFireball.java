package laoqi123.module.modules.combat;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.event.impl.*;
import laoqi123.management.RotationState;
import laoqi123.module.Module;
import laoqi123.module.modules.render.HUD;
import laoqi123.util.*;
import laoqi123.value.properties.*;
import laoqi123.value.properties.FloatValue;
import laoqi123.value.properties.IntValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AntiFireball extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final ArrayList<AbstractFireballEntity> farList = new ArrayList<>();
    private final ArrayList<AbstractFireballEntity> nearList = new ArrayList<>();
    private AbstractFireballEntity target = null;
    public final FloatValue range = new FloatValue("range", 5.0F, 3.0F, 8.0F);
    public final IntValue fov = new IntValue("fov", 360, 1, 360);
    public final BooleanValue rotations = new BooleanValue("rotations", true);
    public final BooleanValue swing = new BooleanValue("swing", true);
    public final ModeValue moveFix = new ModeValue("move-fix", 1, new String[]{"NONE", "SILENT", "STRICT"});
    public final ModeValue showTarget = new ModeValue("show-target", 0, new String[]{"NONE", "DEFAULT", "HUD"});

    private boolean isValidTarget(AbstractFireballEntity entityFireball) {
        return !entityFireball.getBoundingBox().isNaN() && RotationUtil.distanceToEntity(entityFireball) <= (double) this.range.getValue() + 3.0
                && RotationUtil.angleToEntity(entityFireball) <= (float) this.fov.getValue();
    }

    private void doAttackAnimation() {
        if (this.swing.getValue()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            PacketUtil.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
    }

    public AntiFireball() {
        super("AntiFireball", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            List<AbstractFireballEntity> fireballs = new ArrayList<>();
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof AbstractFireballEntity) {
                    fireballs.add((AbstractFireballEntity) entity);
                }
            }
            this.farList.removeIf(entityFireball -> !fireballs.contains(entityFireball));
            this.nearList.removeIf(entityFireball -> !fireballs.contains(entityFireball));
            for (AbstractFireballEntity fireball : fireballs) {
                if (!this.farList.contains(fireball) && !this.nearList.contains(fireball)) {
                    if (RotationUtil.distanceToEntity(fireball) > 3.0) {
                        this.farList.add(fireball);
                    } else {
                        this.nearList.add(fireball);
                    }
                }
            }
            if (mc.player.getAbilities().allowFlying) {
                this.target = null;
            } else {
                this.target = this.farList.stream().filter(this::isValidTarget).min(Comparator.comparingDouble(RotationUtil::distanceToEntity)).orElse(null);
            }
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            AbstractFireballEntity fireball = this.target;
            if (TeamUtil.isEntityLoaded(fireball)) {
                float[] rotations = RotationUtil.getRotationsToBox(this.target.getBoundingBox(), event.getYaw(), event.getPitch(), 180.0F, 0.0F);
                if (this.rotations.getValue()
                        && !ItemUtil.isHoldingNonEmpty()
                        && !ItemUtil.isUsingBow()
                        && !ItemUtil.hasHoldItem()) {
                    event.setRotation(rotations[0], rotations[1], 0);
                    event.setPervRotation(this.moveFix.getValue() != 0 ? rotations[0] : mc.player.getYaw(), 0);
                }
                if (!Myau.playerStateManager.attacking && !Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                    this.doAttackAnimation();
                    if (RotationUtil.distanceToEntity(this.target) <= (double) this.range.getValue().floatValue()) {
                        PacketUtil.sendPacket(PlayerInteractEntityC2SPacket.attack(this.target, mc.player.isSneaking()));
                        PlayerUtil.attackEntity(this.target);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (this.isEnabled()) {
            if (this.moveFix.getValue() == 1
                    && RotationState.isActived()
                    && RotationState.getPriority() == 0.0F
                    && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled()) {
            if (this.showTarget.getValue() != 0 && TeamUtil.isEntityLoaded(this.target)) {
                Color color = new Color(-1);
                switch (this.showTarget.getValue()) {
                    case 1:
                        double dist = (this.target.getX() - this.target.prevX) * (mc.player.getX() - this.target.getX())
                                + (this.target.getY() - this.target.prevY)
                                * (mc.player.getY() + (double) mc.player.getEyeHeight(mc.player.getPose()) - this.target.getY() - (double) this.target.getHeight() / 2.0)
                                + (this.target.getZ() - this.target.prevZ) * (mc.player.getZ() - this.target.getZ());
                        if (dist < 0.0) {
                            color = new Color(16733525);
                        } else {
                            color = new Color(5635925);
                        }
                        break;
                    case 2:
                        color = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
                }
                RenderUtil.enableRenderState();
                RenderUtil.drawEntityBox(this.target, color.getRed(), color.getGreen(), color.getBlue());
                RenderUtil.disableRenderState();
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.farList.clear();
        this.nearList.clear();
    }
}
