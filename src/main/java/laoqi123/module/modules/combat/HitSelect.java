package laoqi123.module.modules.combat;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.event.impl.PacketEvent;
import laoqi123.event.impl.UpdateEvent;
import laoqi123.module.Module;
import laoqi123.mixin.PlayerInteractEntityC2SPacketAccessor;
import laoqi123.module.modules.movement.KeepSprint;
import laoqi123.value.properties.ModeValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class HitSelect extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    public final ModeValue mode = new ModeValue("mode", 0, new String[]{"Second", "Criticals", "W_Tap"});
    
    private boolean sprintState = false;
    private boolean set = false;
    private double savedSlowdown = 0.0;
    
    private int blockedHits = 0;
    private int allowedHits = 0;

    public HitSelect() {
        super("HitSelect", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        
        if (event.getType() == EventType.POST) {
            this.resetMotion();
        }
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.SEND || event.isCancelled()) {
            return;
        }

        if (event.getPacket() instanceof ClientCommandC2SPacket) {
            ClientCommandC2SPacket packet = (ClientCommandC2SPacket) event.getPacket();
            switch (packet.getMode()) {
                case START_SPRINTING:
                    this.sprintState = true;
                    break;
                case STOP_SPRINTING:
                    this.sprintState = false;
                    break;
                default:
                    break;
            }
            return;
        }

        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket) {
            PlayerInteractEntityC2SPacket use = (PlayerInteractEntityC2SPacket) event.getPacket();

            final boolean[] isAttack = {false};
            use.handle(new PlayerInteractEntityC2SPacket.Handler() {
                @Override
                public void interact(Hand hand) {
                }

                @Override
                public void interactAt(Hand hand, Vec3d pos) {
                }

                @Override
                public void attack() {
                    isAttack[0] = true;
                }
            });
            if (!isAttack[0]) {
                return;
            }

            Entity target = mc.world.getEntityById(((PlayerInteractEntityC2SPacketAccessor) use).getEntityId());
            if (target == null || target instanceof FireballEntity) {
                return;
            }

            if (!(target instanceof LivingEntity)) {
                return;
            }

            LivingEntity living = (LivingEntity) target;
            boolean allow = true;

            switch (this.mode.getValue()) {
                case 0: // SECOND
                    allow = this.prioritizeSecondHit(mc.player, living);
                    break;
                case 1: // CRITICALS
                    allow = this.prioritizeCriticalHits(mc.player);
                    break;
                case 2: // WTAP
                    allow = this.prioritizeWTapHits(mc.player, this.sprintState);
                    break;
                default:
                    break;
            }

            if (!allow) {
                event.setCancelled(true);
                this.blockedHits++;
            } else {
                this.allowedHits++;
            }
        }
    }

    private boolean prioritizeSecondHit(LivingEntity player, LivingEntity target) {
        // If target is already hurt, allow the hit
        if (target.hurtTime != 0) {
            return true;
        }

        // If player hasn't recovered from hurt time, allow the hit
        if (player.hurtTime <= player.maxHurtTime - 1) {
            return true;
        }

        // If too close, allow the hit
        double dist = player.distanceTo(target);
        if (dist < 2.5) {
            return true;
        }

        // If not moving towards each other, allow the hit
        if (!this.isMovingTowards(target, player, 60.0)) {
            return true;
        }

        if (!this.isMovingTowards(player, target, 60.0)) {
            return true;
        }

        // Block the hit and fix motion
        this.fixMotion();
        return false;
    }

    private boolean prioritizeCriticalHits(LivingEntity player) {
        // If on ground, allow the hit
        if (player.isOnGround()) {
            return true;
        }

        // If hurt, allow the hit
        if (player.hurtTime != 0) {
            return true;
        }

        // If falling, allow the hit (for crits)
        if (player.fallDistance > 0.0f) {
            return true;
        }

        // Block the hit and fix motion
        this.fixMotion();
        return false;
    }

    private boolean prioritizeWTapHits(LivingEntity player, boolean sprinting) {
        // If against wall, allow the hit
        if (player.horizontalCollision) {
            return true;
        }

        // If not moving forward, allow the hit
        if (!mc.options.forwardKey.isPressed()) {
            return true;
        }

        // If already sprinting, allow the hit
        if (sprinting) {
            return true;
        }

        // Block the hit and fix motion
        this.fixMotion();
        return false;
    }

    private void fixMotion() {
        if (this.set) {
            return;
        }

        KeepSprint keepSprint = (KeepSprint) Myau.moduleManager.modules.get(KeepSprint.class);
        if (keepSprint == null) {
            return;
        }

        try {
            // Save the current slowdown value
            this.savedSlowdown = keepSprint.slowdown.getValue().doubleValue();
            
            // Enable KeepSprint and set slowdown to 0
            if (!keepSprint.isEnabled()) {
                keepSprint.toggle();
            }
            keepSprint.slowdown.setValue(0);
            
            this.set = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetMotion() {
        if (!this.set) {
            return;
        }

        KeepSprint keepSprint = (KeepSprint) Myau.moduleManager.modules.get(KeepSprint.class);
        if (keepSprint == null) {
            return;
        }

        try {
            // Restore the original slowdown value
            keepSprint.slowdown.setValue((int) this.savedSlowdown);
            
            // Disable KeepSprint if we enabled it
            if (keepSprint.isEnabled()) {
                keepSprint.toggle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.set = false;
        this.savedSlowdown = 0.0;
    }

    private boolean isMovingTowards(LivingEntity source, LivingEntity target, double maxAngle) {
        Vec3d currentPos = source.getPos();
        Vec3d lastPos = new Vec3d(source.prevX, source.prevY, source.prevZ);
        Vec3d targetPos = target.getPos();

        // Calculate movement vector
        double mx = currentPos.x - lastPos.x;
        double mz = currentPos.z - lastPos.z;
        double movementLength = Math.sqrt(mx * mx + mz * mz);

        // If not moving, return false
        if (movementLength == 0.0) {
            return false;
        }

        // Normalize movement vector
        mx /= movementLength;
        mz /= movementLength;

        // Calculate vector to target
        double tx = targetPos.x - currentPos.x;
        double tz = targetPos.z - currentPos.z;
        double targetLength = Math.sqrt(tx * tx + tz * tz);

        // If target is at same position, return false
        if (targetLength == 0.0) {
            return false;
        }

        // Normalize target vector
        tx /= targetLength;
        tz /= targetLength;

        // Calculate dot product (cosine of angle between vectors)
        double dotProduct = mx * tx + mz * tz;

        // Check if angle is within threshold
        return dotProduct >= Math.cos(Math.toRadians(maxAngle));
    }

    @Override
    public void onDisabled() {
        this.resetMotion();
        this.sprintState = false;
        this.set = false;
        this.savedSlowdown = 0.0;
        this.blockedHits = 0;
        this.allowedHits = 0;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
