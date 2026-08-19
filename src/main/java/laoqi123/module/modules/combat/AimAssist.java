package laoqi123.module.modules.combat;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.KeyEvent;
import laoqi123.event.impl.TickEvent;
import laoqi123.module.Module;
import laoqi123.util.*;
import laoqi123.value.properties.*;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.IntValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AimAssist extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final TimerUtil timer = new TimerUtil();
    public final FloatValue hSpeed = new FloatValue("horizontal-speed", 3.0F, 0.0F, 10.0F);
    public final FloatValue vSpeed = new FloatValue("vertical-speed", 0.0F, 0.0F, 10.0F);
    public final PercentValue smoothing = new PercentValue("smoothing", 50);
    public final FloatValue range = new FloatValue("range", 4.5F, 3.0F, 8.0F);
    public final IntValue fov = new IntValue("fov", 90, 30, 360);
    public final BooleanValue weaponOnly = new BooleanValue("weapons-only", true);
    public final BooleanValue allowTools = new BooleanValue("allow-tools", false, this.weaponOnly::getValue);
    public final BooleanValue botChecks = new BooleanValue("bot-check", true);
    public final BooleanValue team = new BooleanValue("teams", true);

    private boolean isValidTarget(PlayerEntity entityPlayer) {
        if (entityPlayer != mc.player && entityPlayer != mc.player.getVehicle()) {
            if (entityPlayer == mc.getCameraEntity() || entityPlayer == mc.getCameraEntity().getVehicle()) {
                return false;
            } else if (entityPlayer.deathTime > 0) {
                return false;
            } else if (RotationUtil.distanceToEntity(entityPlayer) > (double) this.range.getValue()) {
                return false;
            } else if (RotationUtil.angleToEntity(entityPlayer) > (float) this.fov.getValue()) {
                return false;
            } else if (RotationUtil.rayTrace(entityPlayer) != null) {
                return false;
            } else if (TeamUtil.isFriend(entityPlayer)) {
                return false;
            } else {
                return (!this.team.getValue() || !TeamUtil.isSameTeam(entityPlayer)) && (!this.botChecks.getValue() || !TeamUtil.isBot(entityPlayer));
            }
        } else {
            return false;
        }
    }

    private boolean isInReach(PlayerEntity entityPlayer) {
        Reach reach = (Reach) Myau.moduleManager.modules.get(Reach.class);
        double distance = reach.isEnabled() ? (double) reach.range.getValue() : 3.0;
        return RotationUtil.distanceToEntity(entityPlayer) <= distance;
    }

    private boolean isLookingAtBlock() {
        return mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK;
    }

    public AimAssist() {
        super("AimAssist", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST && mc.currentScreen == null) {
            if (!(Boolean) this.weaponOnly.getValue()
                    || ItemUtil.hasRawUnbreakingEnchant()
                    || this.allowTools.getValue() && ItemUtil.isHoldingTool()) {
                boolean attacking = PlayerUtil.isAttacking();
                if (!attacking || !this.isLookingAtBlock()) {
                    if (attacking || !this.timer.hasTimeElapsed(350L)) {
                        List<PlayerEntity> inRange = StreamSupport.stream(mc.world
                                .getEntities()
                                .spliterator(), false)
                                .filter(entity -> entity instanceof PlayerEntity)
                                .map(entity -> (PlayerEntity) entity)
                                .filter(this::isValidTarget)
                                .sorted(Comparator.comparingDouble(RotationUtil::distanceToEntity))
                                .collect(Collectors.toList());
                        if (!inRange.isEmpty()) {
                            if (inRange.stream().anyMatch(this::isInReach)) {
                                inRange.removeIf(entityPlayer -> !this.isInReach(entityPlayer));
                            }
                            PlayerEntity player = inRange.get(0);
                            if (!(RotationUtil.distanceToEntity(player) <= 0.0)) {
                                Box axisAlignedBB = player.getBoundingBox();
                                double collisionBorderSize = player.getTargetingMargin();
                                float[] rotation = RotationUtil.getRotationsToBox(
                                        axisAlignedBB.expand(collisionBorderSize, collisionBorderSize, collisionBorderSize),
                                        mc.player.getYaw(),
                                        mc.player.getPitch(),
                                        180.0F,
                                        (float) this.smoothing.getValue() / 100.0F
                                );
                                float yaw = Math.min(Math.abs(this.hSpeed.getValue()), 10.0F);
                                float pitch = Math.min(Math.abs(this.vSpeed.getValue()), 10.0F);
                                Myau.rotationManager
                                        .setRotation(
                                                mc.player.getYaw() + (rotation[0] - mc.player.getYaw()) * 0.1F * yaw,
                                                mc.player.getPitch() + (rotation[1] - mc.player.getPitch()) * 0.1F * pitch,
                                                0,
                                                false
                                        );
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPress(KeyEvent event) {
        InputUtil.Key attackKey = InputUtil.fromTranslationKey(mc.options.attackKey.getBoundKeyTranslationKey());
        int attackKeyCode = attackKey.getCode();
        if (attackKey.getCategory() == InputUtil.Type.MOUSE) {
            attackKeyCode -= 100;
        }
        if (event.getKey() == attackKeyCode && !Myau.moduleManager.modules.get(AutoClicker.class).isEnabled()) {
            this.timer.reset();
        }
    }
}
