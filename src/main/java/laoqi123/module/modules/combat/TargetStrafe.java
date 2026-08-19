package laoqi123.module.modules.combat;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.event.impl.Render3DEvent;
import laoqi123.event.impl.StrafeEvent;
import laoqi123.event.impl.UpdateEvent;
import laoqi123.module.Module;
import laoqi123.module.modules.movement.Fly;
import laoqi123.module.modules.render.HUD;
import laoqi123.module.modules.movement.LongJump;
import laoqi123.module.modules.movement.Speed;
import laoqi123.util.*;
import laoqi123.value.properties.*;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.ModeValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

import java.awt.*;
import java.util.ArrayList;

public class TargetStrafe extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private LivingEntity target = null;
    private float targetYaw = Float.NaN;
    private int direction = 1;
    public final FloatValue radius = new FloatValue("radius", 1.0F, 0.0F, 6.0F);
    public final IntValue points = new IntValue("points", 6, 3, 24);
    public final BooleanValue requirePress = new BooleanValue("require-press", true);
    public final BooleanValue speedOnly = new BooleanValue("speed-only", true);
    public final ModeValue showTarget = new ModeValue("show-target", 1, new String[]{"NONE", "DEFAULT", "HUD"});

    private boolean canStrafe() {
        if (this.speedOnly.getValue()) {
            Speed speed = (Speed) Myau.moduleManager.modules.get(Speed.class);
            Fly fly = (Fly) Myau.moduleManager.modules.get(Fly.class);
            LongJump longJump = (LongJump) Myau.moduleManager.modules.get(LongJump.class);
            if (!speed.isEnabled() && !fly.isEnabled() && (!longJump.isEnabled() || !longJump.isJumping())) {
                return false;
            }
        }
        return !this.requirePress.getValue() || PlayerUtil.isJumping();
    }

    private LivingEntity getKillAuraTarget() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura.isEnabled() && killAura.isAttackAllowed()) {
            LivingEntity entityLivingBase = killAura.getTarget();
            return !TeamUtil.isEntityLoaded(entityLivingBase) ? null : entityLivingBase;
        } else {
            return null;
        }
    }

    private Color getTargetColor(LivingEntity entityLivingBase) {
        if (entityLivingBase instanceof PlayerEntity) {
            if (TeamUtil.isFriend((PlayerEntity) entityLivingBase)) {
                return Myau.friendManager.getColor();
            }
            if (TeamUtil.isTarget((PlayerEntity) entityLivingBase)) {
                return Myau.targetManager.getColor();
            }
        }
        switch (this.showTarget.getValue()) {
            case 1:
                if (!(entityLivingBase instanceof PlayerEntity)) {
                    return Color.WHITE;
                }
                return TeamUtil.getTeamColor((PlayerEntity) entityLivingBase, 1.0F);
            case 2:
                int color = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis()).getRGB();
                return new Color(color);
            default:
                return new Color(-1);
        }
    }

    private boolean isInWater(double x, double z) {
        return PlayerUtil.checkInWater(
                new Box(x - 0.015, mc.player.getY(), z - 0.015, x + 0.015, mc.player.getY() + (double) mc.player.getHeight(), z + 0.015)
        );
    }

    private int wrapIndex(int index, int size) {
        if (index < 0) {
            return size - 1;
        } else {
            return index >= size ? 0 : index;
        }
    }

    public TargetStrafe() {
        super("TargetStrafe", false);
    }

    public float getTargetYaw() {
        return this.targetYaw;
    }

    @EventTarget(Priority.HIGHEST)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            boolean left = PlayerUtil.isMovingLeft();
            boolean right = PlayerUtil.isMovingRight();
            if (left ^ right) {
                this.direction = left ? 1 : -1;
            }
            if (!this.canStrafe()) {
                this.target = null;
                this.targetYaw = Float.NaN;
            } else {
                this.target = this.getKillAuraTarget();
                if (this.target == null) {
                    this.targetYaw = Float.NaN;
                } else {
                    ArrayList<Vec2d> vpositions = new ArrayList<>();
                    for (int i = 0; i < this.points.getValue(); i++) {
                        vpositions.add(
                                new Vec2d(
                                        (double) this.radius.getValue()
                                                * Math.cos((double) i * ((Math.PI * 2) / (double) this.points.getValue())),
                                        (double) this.radius.getValue()
                                                * Math.sin((double) i * ((Math.PI * 2) / (double) this.points.getValue()))
                                )
                        );
                    }
                    if (vpositions.isEmpty()) {
                        this.target = null;
                        this.targetYaw = Float.NaN;
                    } else {
                        double closestDistance = 0.0;
                        int closestIndex = -1;
                        for (int i = 0; i < vpositions.size(); i++) {
                            double distance = Math.sqrt(
                                    mc.player.squaredDistanceTo(
                                            this.target.getX() + (vpositions.get(i)).getX(), mc.player.getY(), this.target.getZ() + (vpositions.get(i)).getY()
                                    )
                            );
                            if (closestIndex == -1 || distance < closestDistance) {
                                closestDistance = distance;
                                closestIndex = i;
                            }
                        }
                        if (mc.player.horizontalCollision) {
                            this.direction *= -1;
                        }
                        int nextIndex = closestIndex + this.direction;
                        nextIndex = this.wrapIndex(nextIndex, vpositions.size());
                        double nextX = this.target.getX() + (vpositions.get(nextIndex)).getX();
                        double nextZ = this.target.getZ() + (vpositions.get(nextIndex)).getY();
                        if (this.isInWater(nextX, nextZ)) {
                            this.direction *= -1;
                            nextIndex = closestIndex + this.direction;
                            nextIndex = this.wrapIndex(nextIndex, vpositions.size());
                            nextX = this.target.getX() + (vpositions.get(nextIndex)).getX();
                            nextZ = this.target.getZ() + (vpositions.get(nextIndex)).getY();
                        }
                        double deltaX = nextX - mc.player.getX();
                        double deltaZ = nextZ - mc.player.getZ();
                        float currentPitch = event.getPitch();
                        float currentYaw = event.getYaw();
                        double deltaY = 0.0;
                        this.targetYaw = RotationUtil.getRotationsTo(deltaX, deltaY, deltaZ, currentYaw, currentPitch)[0];
                        event.setPervRotation(this.targetYaw, 10);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled()) {
            if (!Float.isNaN(this.targetYaw) && MoveUtil.isForwardPressed()) {
                event.setStrafe(0.0F);
                event.setForward(1.0F);
            }
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled() && TeamUtil.isEntityLoaded(this.target)) {
            if (this.showTarget.getValue() != 0) {
                Color color = this.getTargetColor(this.target);
                RenderUtil.enableRenderState();
                RenderUtil.drawEntityCircle(
                        this.target, this.radius.getValue(), this.points.getValue(), ColorUtil.darker(color, 0.2F).getRGB()
                );
                RenderUtil.drawEntityCircle(this.target, this.radius.getValue(), this.points.getValue(), color.getRGB());
                RenderUtil.disableRenderState();
            }
        }
    }

    @Override
    public void onDisabled() {
        this.target = null;
        this.targetYaw = Float.NaN;
    }

    public static class Vec2d {
        private final double x;
        private final double y;

        public Vec2d(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }
    }
}
