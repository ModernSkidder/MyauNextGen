package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.event.EventManager;
import laoqi123.event.types.EventType;
import laoqi123.events.LivingUpdateEvent;
import laoqi123.events.MoveInputEvent;
import laoqi123.events.PlayerUpdateEvent;
import laoqi123.events.UpdateEvent;
import laoqi123.management.RotationState;
import laoqi123.module.modules.AntiDebuff;
import laoqi123.module.modules.NoSlow;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayerEntity.class, priority = 9999)
public abstract class MixinEntityPlayerSP {
    @Unique
    private static boolean myauLocalPlayerReady() {
        return net.minecraft.client.MinecraftClient.getInstance().player != null;
    }
    @Unique
    private float overrideYaw = Float.NaN;
    @Unique
    private float overridePitch = Float.NaN;
    @Unique
    private float pendingYaw;
    @Unique
    private float pendingPitch;
    @Shadow
    private float lastYaw;
    @Shadow
    private float lastPitch;
    @Shadow
    public Input input;
    @Shadow
    public float renderYaw;
    @Shadow
    public float lastRenderYaw;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onUpdate(CallbackInfo callbackInfo) {
        if (!myauLocalPlayerReady()) return;
        Entity self = (Entity) (Object) this;
        if (self.getWorld().isChunkLoaded(BlockPos.ofFloored(self.getX(), 0.0, self.getZ()))) {
            UpdateEvent event = new UpdateEvent(EventType.PRE, this.lastYaw, this.lastPitch, self.getYaw(), self.getPitch());
            EventManager.call(event);
            RotationState.applyState(event.isRotated() && !self.hasVehicle(), event.getNewYaw(), event.getNewPitch(), event.getPreYaw(), event.isRotating());
            if (event.isRotated()) {
                this.pendingYaw = self.getYaw();
                this.pendingPitch = self.getPitch();
                this.overrideYaw = event.getNewYaw();
                this.overridePitch = event.getNewPitch();
            } else {
                this.pendingYaw = Float.NaN;
                this.pendingPitch = Float.NaN;
                this.overrideYaw = Float.NaN;
                this.overridePitch = Float.NaN;
            }
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void postUpdate(CallbackInfo callbackInfo) {
        if (!myauLocalPlayerReady()) return;
        Entity self = (Entity) (Object) this;
        if (self.getWorld().isChunkLoaded(BlockPos.ofFloored(self.getX(), 0.0, self.getZ()))) {
            if (!Float.isNaN(this.pendingYaw) && !Float.isNaN(this.pendingPitch)) {
                this.lastYaw = self.getYaw();
                this.lastPitch = self.getPitch();
                self.setYaw(self.getYaw() + MathHelper.wrapDegrees(this.pendingYaw - self.getYaw()));
                self.setPitch(this.pendingPitch);
                self.prevYaw = self.getYaw();
                self.prevPitch = self.getPitch();
                this.lastRenderYaw = self.getYaw() - (this.renderYaw - this.lastRenderYaw) * 2.0F;
                this.renderYaw = self.getYaw();
            }
            EventManager.call(new UpdateEvent(EventType.POST, this.lastYaw, this.lastPitch, self.getYaw(), self.getPitch()));
        }
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasVehicle()Z"
            )
    )
    private boolean onRidding(ClientPlayerEntity clientPlayerEntity) {
        if (!Float.isNaN(this.overrideYaw) && !Float.isNaN(this.overridePitch)) {
            clientPlayerEntity.setYaw(this.overrideYaw);
            clientPlayerEntity.setPitch(this.overridePitch);
        }
        return clientPlayerEntity.hasVehicle();
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendMovementPackets()V"
            )
    )
    private void onMotionUpdate(CallbackInfo callbackInfo) {
        EventManager.call(new PlayerUpdateEvent());
    }

    @Inject(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tickMovement()V"
            )
    )
    private void onLivingUpdate(CallbackInfo callbackInfo) {
        EventManager.call(new LivingUpdateEvent());
    }

    @Inject(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/input/Input;tick()V",
                    shift = At.Shift.AFTER
            )
    )
    private void updateMove(CallbackInfo callbackInfo) {
        MoveInputEvent moveInputEvent = new MoveInputEvent();
        EventManager.call(moveInputEvent);
        if (moveInputEvent.isJumpModified()) {
            this.input.playerInput = new PlayerInput(
                    this.input.playerInput.forward(),
                    this.input.playerInput.backward(),
                    this.input.playerInput.left(),
                    this.input.playerInput.right(),
                    moveInputEvent.getJump(),
                    this.input.playerInput.sneak(),
                    this.input.playerInput.sprint()
            );
        }
        if (moveInputEvent.isForwardModified()) {
            this.input.movementForward = moveInputEvent.getForward();
        }
        if (moveInputEvent.isStrafeModified()) {
            this.input.movementSideways = moveInputEvent.getStrafe();
        }
    }

    @Redirect(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
            )
    )
    private boolean isUsing(ClientPlayerEntity clientPlayerEntity) {
        NoSlow noSlow = (NoSlow) Myau.moduleManager.modules.get(NoSlow.class);
        return (!noSlow.isEnabled() || !noSlow.isAnyActive()) && clientPlayerEntity.isUsingItem();
    }

    @Redirect(
            method = "tickNausea",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"
            )
    )
    private boolean checkPotion(ClientPlayerEntity clientPlayerEntity, RegistryEntry<StatusEffect> statusEffect) {
        if (statusEffect == StatusEffects.NAUSEA && Myau.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff) Myau.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.nausea.getValue()) {
                return false;
            }
        }
        return clientPlayerEntity.hasStatusEffect(statusEffect);
    }
}
