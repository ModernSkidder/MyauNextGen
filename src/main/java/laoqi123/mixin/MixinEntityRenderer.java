package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.data.Box;
import laoqi123.event.EventManager;
import laoqi123.event.impl.PickEvent;
import laoqi123.event.impl.RaytraceEvent;
import laoqi123.event.impl.Render3DEvent;
import laoqi123.module.modules.player.AutoBlockIn;
import laoqi123.module.modules.player.GhostHand;
import laoqi123.module.modules.combat.KillAura;
import laoqi123.module.modules.render.NoHurtCam;
import laoqi123.module.modules.player.Scaffold;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.function.Predicate;

@Mixin(value = GameRenderer.class, priority = 9999)
public abstract class MixinEntityRenderer {
    @Unique
    private Box<Integer> slot = null;
    @Unique
    private Box<ItemStack> using = null;
    @Unique
    private Box<Integer> useCount = null;
    @Shadow
    private BufferBuilderStorage buffers;
    @Shadow
    private MinecraftClient client;

    @Inject(method = "render", at = @At("HEAD"))
    private void updateCameraAndRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo callbackInfo) {
        if (this.client.player != null) {
            Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
            if (scaffold.isEnabled() && scaffold.spoofItem.getValue()) {
                int slot = scaffold.getSlot();
                if (slot >= 0) {
                    this.slot = new Box<>(this.client.player.getInventory().selectedSlot);
                    this.client.player.getInventory().selectedSlot = slot;
                }
            }
            AutoBlockIn autoBlockIn = (AutoBlockIn) Myau.moduleManager.modules.get(AutoBlockIn.class);
            if (autoBlockIn.isEnabled() && autoBlockIn.itemSpoof.getValue()) {
                int slot = autoBlockIn.getSlot();
                if (slot >= 0) {
                    this.slot = new Box<>(this.client.player.getInventory().selectedSlot);
                    this.client.player.getInventory().selectedSlot = slot;
                }
            }
            KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
            if (killAura.isEnabled() && killAura.isBlocking()) {
                this.using = new Box<>(((LivingEntityItemUseAccessor) this.client.player).getItemInUse());
                ((LivingEntityItemUseAccessor) this.client.player).setItemInUse(this.client.player.getInventory().getMainHandStack());
                this.useCount = new Box<>(((LivingEntityItemUseAccessor) this.client.player).getItemInUseCount());
                ((LivingEntityItemUseAccessor) this.client.player).setItemInUseCount(69000);
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void postUpdateCameraAndRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo callbackInfo) {
        if (this.slot != null) {
            this.client.player.getInventory().selectedSlot = this.slot.value;
            this.slot = null;
        }
        if (this.using != null) {
            ((LivingEntityItemUseAccessor) this.client.player).setItemInUse(this.using.value);
            this.using = null;
        }
        if (this.useCount != null) {
            ((LivingEntityItemUseAccessor) this.client.player).setItemInUseCount(this.useCount.value);
            this.useCount = null;
        }
    }

    @Inject(
            method = "renderWorld",
            at = @At("RETURN")
    )
    private void renderWorldPass(RenderTickCounter tickCounter, CallbackInfo callbackInfo) {
        EventManager.call(new Render3DEvent(new MatrixStack(), this.buffers.getEntityVertexConsumers(), tickCounter.getTickDelta(true)));
    }

    @ModifyConstant(
            method = "tiltViewWhenHurt",
            constant = @Constant(doubleValue = 14.0)
    )
    private double hurtCameraEffect(double value) {
        if (Myau.moduleManager == null) {
            return value;
        } else {
            NoHurtCam noHurtCam = (NoHurtCam) Myau.moduleManager.modules.get(NoHurtCam.class);
            return noHurtCam.isEnabled() ? value * (float) noHurtCam.multiplier.getValue().intValue() / 100.0F : value;
        }
    }

    @ModifyArgs(
            method = "updateCrosshairTarget",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/GameRenderer;findCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;"
            )
    )
    private void getMouseOver(Args args) {
        double blockRange = args.get(1);
        RaytraceEvent raytraceEvent = new RaytraceEvent(blockRange);
        EventManager.call(raytraceEvent);
        args.set(1, raytraceEvent.getRange());
        double entityRange = args.get(2);
        PickEvent pickEvent = new PickEvent(entityRange);
        EventManager.call(pickEvent);
        args.set(2, pickEvent.getRange());
    }

    @ModifyArg(
            method = "findCrosshairTarget",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"
            ),
            index = 4
    )
    private Predicate<Entity> filterEntities(Predicate<Entity> predicate) {
        if (Myau.moduleManager == null) {
            return predicate;
        } else {
            GhostHand ghostHand = (GhostHand) Myau.moduleManager.modules.get(GhostHand.class);
            return ghostHand.isEnabled() ? predicate.and(entity -> !ghostHand.shouldSkip(entity)) : predicate;
        }
    }
}
