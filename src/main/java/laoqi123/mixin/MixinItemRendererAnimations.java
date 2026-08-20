// not rat bro XD
// Original logic by syuto/animations-1.6, integrated into Uzi
package laoqi123.mixin;

import laoqi123.Myau;
import laoqi123.config.AnimationConfig;
import laoqi123.config.AnimationMode;
import laoqi123.module.modules.render.OldHitting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HeldItemRenderer.class, priority = 999)
public abstract class MixinItemRendererAnimations {

    @Unique
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private float spin = 0.0F;
    private float delay = 0.0F;
    private long lastUpdateTime = System.currentTimeMillis();

    @Unique
    private boolean _customTransform = false;

    @Shadow
    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
    }

    @Shadow
    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
    }

    @Inject(
            method = "applySwingOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cancelApplySwingOffset(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
        if (this.shouldApplyNormalAnimations() && !this._customTransform) {
            ci.cancel();
        }
    }

    @Inject(
            method = "applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cancelApplyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress, CallbackInfo ci) {
        if (this.shouldApplyNormalAnimations() && !this._customTransform) {
            ci.cancel();
        }
    }

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
                    shift = At.Shift.BEFORE)
    )
    private void applyAnimTransform(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (hand != Hand.MAIN_HAND) return;

        HeldItemRendererAccessor acc = (HeldItemRendererAccessor) this;
        float equippedProgress = acc.getEquippedProgress();
        float prevEquippedProgress = acc.getPrevEquippedProgress();
        float f = 1.0F - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * tickDelta);

        float sine = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        float sqrtSwing = MathHelper.sqrt(swingProgress);
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);

        Arm arm = player.getMainArm();

        OldHitting oldHitting = (OldHitting) Myau.moduleManager.modules.get(OldHitting.class);
        if (oldHitting != null && oldHitting.isEnabled() && oldHitting.isKillAuraAttacking() && swingProgress > 0.0F) {
            this._customTransform = true;
            try {
                oldHitting.applyHitAnimation(matrices, swingProgress, arm, f);
            } finally {
                this._customTransform = false;
            }
            return;
        }

        AnimationConfig.sync();
        if (AnimationConfig.isEnabled()) {
            matrices.translate(
                    AnimationConfig.getBlockPosX(),
                    AnimationConfig.getBlockPosY(),
                    AnimationConfig.getBlockPosZ()
            );
        }
        if (AnimationConfig.isEnabled() && this.shouldApplyNormalAnimations()) {
            this._customTransform = true;
            try {
                AnimationMode m = AnimationConfig.getMode();
                if (m == AnimationMode.EXHIBITION) {
                    matrices.translate(0.0F, -0.1F, 0.0F);
                    applyEquipOffset(matrices, arm, f / 2.0F);
                    matrices.translate(0.1F, 0.4F, -0.1F);
                    matrices.multiply(RotationAxis.of(new Vector3f(sine / 2.0F, 0.0F, 9.0F)).rotationDegrees(-sine * 30.0F));
                    matrices.multiply(RotationAxis.of(new Vector3f(0.8F, sine / 2.0F, 0.0F)).rotationDegrees(-sine * 50.0F));
                } else if (m == AnimationMode.SIGMA) {
                    applyEquipOffset(matrices, arm, f * 0.5F);
                    matrices.multiply(RotationAxis.of(new Vector3f(-8.0F, 0.0F, 9.0F)).rotationDegrees(-sine * 27.5F));
                    matrices.multiply(RotationAxis.of(new Vector3f(1.0F, sine / 2.0F, 0.0F)).rotationDegrees(-sine * 45.0F));
                    matrices.translate(-0.1F, 0.3F, 0.1F);
                } else if (m == AnimationMode.VANILLA) {
                    matrices.translate(0.0F, 0.05F, -0.1F);
                    applyEquipOffset(matrices, arm, f);
                    applySwingOffset(matrices, arm, swingProgress);
                } else if (m == AnimationMode.PLAIN) {
                    matrices.translate(0.0F, 0.05F, 0.0F);
                    applyEquipOffset(matrices, arm, f);
                } else if (m == AnimationMode.SPIN) {
                    matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 0.0F, -0.1F)).rotationDegrees(spin));
                    applyEquipOffset(matrices, arm, f);
                    spin = -(System.currentTimeMillis() / 2L % 360L);
                } else if (m == AnimationMode.ETB) {
                    matrices.translate(0.0F, -0.1F, 0.0F);
                    applyEquipOffset(matrices, arm, f);
                    matrices.translate(0.1F, 0.4F, -0.1F);
                    matrices.multiply(RotationAxis.of(new Vector3f(-8.0F, 0.0F, 9.0F)).rotationDegrees(-sine * 35.0F));
                    matrices.multiply(RotationAxis.of(new Vector3f(1.5F, -0.4F, 0.0F)).rotationDegrees(-sine * 70.0F));
                } else if (m == AnimationMode.DORTWARE) {
                    float alt = MathHelper.sin(sqrtSwing * 3.1415927F - 3.0F);
                    applyEquipOffset(matrices, arm, f);
                    matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 15.0F, 200.0F)).rotationDegrees(-sine * 10.0F));
                    matrices.multiply(RotationAxis.of(new Vector3f(300.0F, sine / 2.0F, 1.0F)).rotationDegrees(-sine * 10.0F));
                    matrices.translate(3.4F, 0.3F, -0.4F);
                    matrices.translate(-2.1F, -0.2F, 0.1F);
                    matrices.multiply(RotationAxis.of(new Vector3f(-10.0F, -1.4F, -10.0F)).rotationDegrees(alt * 13.0F));
                } else if (m == AnimationMode.AVATAR) {
                    matrices.translate(0.56F, -0.52F, -0.72F);
                    matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.0F, 0.0F)).rotationDegrees(45.0F));
                    matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.0F, 0.0F)).rotationDegrees(sine1 * -20.0F));
                    matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 0.0F, 1.0F)).rotationDegrees(sine * -20.0F));
                    matrices.multiply(RotationAxis.of(new Vector3f(1.0F, 0.0F, 0.0F)).rotationDegrees(sine * -40.0F));
                    matrices.scale(0.4F, 0.4F, 0.4F);
                } else if (m == AnimationMode.SWONG) {
                    applyEquipOffset(matrices, arm, f / 2.0F);
                    matrices.multiply(RotationAxis.of(new Vector3f(sine / 2.0F, 0.0F, 9.0F)).rotationDegrees(-sine * 20.0F));
                    matrices.multiply(RotationAxis.of(new Vector3f(1.0F, sine / 2.0F, 0.0F)).rotationDegrees(-sine * 30.0F));
                } else if (m == AnimationMode.SWANG) {
                    applyEquipOffset(matrices, arm, f / 2.0F);
                    applySwingOffset(matrices, arm, swingProgress);
                    matrices.multiply(RotationAxis.of(new Vector3f(-sine, 0.0F, 9.0F)).rotationDegrees(sine * 15.0F));
                    matrices.multiply(RotationAxis.of(new Vector3f(1.0F, -sine / 2.0F, 0.0F)).rotationDegrees(sine * 40.0F));
                } else if (m == AnimationMode.SWANK) {
                    applyEquipOffset(matrices, arm, f / 2.0F);
                    applySwingOffset(matrices, arm, swingProgress);
                    matrices.multiply(RotationAxis.of(new Vector3f(-sine, 0.0F, 9.0F)).rotationDegrees(sine * 30.0F));
                    matrices.multiply(RotationAxis.of(new Vector3f(1.0F, -sine, 0.0F)).rotationDegrees(sine * 40.0F));
                } else if (m == AnimationMode.STYLES) {
                    applyEquipOffset(matrices, arm, f);
                    matrices.translate(-0.05F, 0.2F, 0.0F);
                    matrices.multiply(RotationAxis.of(new Vector3f(-8.0F, 0.0F, 9.0F)).rotationDegrees(-sine * 35.0F));
                    matrices.multiply(RotationAxis.of(new Vector3f(1.0F, -0.4F, 0.0F)).rotationDegrees(-sine * 70.0F));
                } else if (m == AnimationMode.NUDGE) {
                    matrices.translate(-0.1F, 0.09F, 0.0F);
                    matrices.multiply(RotationAxis.of(new Vector3f(-320.0F, 320.0F, 0.0F)).rotationDegrees(0.0F));
                    applyEquipOffset(matrices, arm, 0.0F);
                    applySwingOffset(matrices, arm, 1.0F);
                    float ns1 = MathHelper.sin(sqrtSwing * 3.0F);
                    float ns2 = MathHelper.sin(sqrtSwing * 4.9415927F);
                    matrices.multiply(RotationAxis.of(new Vector3f(-90.0F, -ns2, 10.0F)).rotationDegrees(-ns1 * 60.0F));
                    matrices.multiply(RotationAxis.of(new Vector3f(15.0F, ns2, 0.0F)).rotationDegrees(-ns1 * 110.0F));
                } else if (m == AnimationMode.PUNCH) {
                    applyEquipOffset(matrices, arm, f);
                    matrices.translate(0.1F, 0.2F, 0.3F);
                    matrices.multiply(RotationAxis.of(new Vector3f(-5.0F, 0.0F, 9.0F)).rotationDegrees(-sine * 30.0F));
                    matrices.multiply(RotationAxis.of(new Vector3f(1.0F, -0.4F, -0.5F)).rotationDegrees(-sine * 10.0F));
                } else if (m == AnimationMode.SLIDE) {
                    matrices.translate(-0.1F, 0.15F, 0.0F);
                    applyEquipOffset(matrices, arm, 0.0F);
                    float ss = MathHelper.sin(sqrtSwing * 2.9415927F);
                    matrices.translate(-0.05F, 0.0F, 0.35F);
                    matrices.multiply(RotationAxis.of(new Vector3f(-15.0F, ss, 10.0F)).rotationDegrees(-ss * 30.0F));
                    matrices.multiply(RotationAxis.of(new Vector3f(5.0F, -ss, 0.0F)).rotationDegrees(-ss * 70.0F));
                } else if (m == AnimationMode.JIGSAW) {
                    matrices.translate(0.56F, -0.42F, -0.72F);
                    matrices.translate(0.1F * sine, 0.0F, -0.22F * sine);
                    matrices.translate(0.0F, sine1 * -0.15F, 0.0F);
                    matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.0F, 0.0F)).rotationDegrees(sine1 * 45.0F));
                    matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.0F, 0.0F)).rotationDegrees(sine1 * -20.0F));
                    matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 0.0F, 1.0F)).rotationDegrees(sine * -20.0F));
                    matrices.multiply(RotationAxis.of(new Vector3f(1.0F, 0.0F, 0.0F)).rotationDegrees(sine * -80.0F));
                } else if (m == AnimationMode.SWING) {
                    applyEquipOffset(matrices, arm, f);
                } else if (m == AnimationMode.OLD) {
                    applyEquipOffset(matrices, arm, f);
                    applyOldAnimation(matrices);
                } else if (m == AnimationMode.PUSH) {
                    applyEquipOffset(matrices, arm, f);
                    applyPushAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.DASH) {
                    applyEquipOffset(matrices, arm, f);
                    applyDashAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.SLASH) {
                    applyEquipOffset(matrices, arm, f);
                    applySlashAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.SCALE) {
                    applyEquipOffset(matrices, arm, f);
                    applyScaleAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.SWONK) {
                    applyEquipOffset(matrices, arm, f);
                    applySwonkAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.STELLA) {
                    applyEquipOffset(matrices, arm, f);
                    applyStellaAnimation(matrices);
                } else if (m == AnimationMode.SMALL) {
                    applyEquipOffset(matrices, arm, f);
                    applySmallAnimation(matrices);
                } else if (m == AnimationMode.EDIT) {
                    applyEquipOffset(matrices, arm, f);
                    applyEditAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.RHYS) {
                    applyEquipOffset(matrices, arm, f);
                    applyRhysAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.STAB) {
                    applyEquipOffset(matrices, arm, f);
                    applyStabAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.FLOAT) {
                    applyEquipOffset(matrices, arm, f);
                    applyFloatAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.REMIX) {
                    applyEquipOffset(matrices, arm, f);
                    applyRemixAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.XIV) {
                    applyEquipOffset(matrices, arm, f);
                    applyXivAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.WINTER) {
                    applyEquipOffset(matrices, arm, f);
                    applyWinterAnimation(matrices);
                } else if (m == AnimationMode.YAMATO) {
                    applyEquipOffset(matrices, arm, f);
                    applyYamatoAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.SLIDE_SWING) {
                    applyEquipOffset(matrices, arm, f);
                    applySlideSwingAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.SMALL_PUSH) {
                    applyEquipOffset(matrices, arm, f);
                    applySmallPushAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.REVERSE) {
                    applyEquipOffset(matrices, arm, f);
                    applyReverseAnimation(matrices);
                } else if (m == AnimationMode.INVENT) {
                    applyEquipOffset(matrices, arm, f);
                    applyInventAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.LEAKED) {
                    applyEquipOffset(matrices, arm, f);
                    applyLeakedAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.AQUA) {
                    applyEquipOffset(matrices, arm, f);
                    applyAquaAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.ASTRO) {
                    applyEquipOffset(matrices, arm, f);
                    applyAstroAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.FADEAWAY) {
                    applyEquipOffset(matrices, arm, f);
                    applyFadeawayAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.ASTOLFO) {
                    applyEquipOffset(matrices, arm, f);
                    applyAstolfoAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.ASTOLFO_SPIN) {
                    applyEquipOffset(matrices, arm, f);
                    applyAstolfoSpinAnimation(matrices);
                } else if (m == AnimationMode.MOON) {
                    applyEquipOffset(matrices, arm, f);
                    applyMoonAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.MOON_PUSH) {
                    applyEquipOffset(matrices, arm, f);
                    applyMoonPushAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.SMOOTH) {
                    applyEquipOffset(matrices, arm, f);
                    applySmoothAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.TAP1) {
                    applyEquipOffset(matrices, arm, f);
                    applyTap1Animation(matrices, swingProgress);
                } else if (m == AnimationMode.TAP2) {
                    applyEquipOffset(matrices, arm, f);
                    applyTap2Animation(matrices, swingProgress);
                } else if (m == AnimationMode.SIGMA3) {
                    applyEquipOffset(matrices, arm, f);
                    applySigma3Animation(matrices, swingProgress);
                } else if (m == AnimationMode.SIGMA4) {
                    applyEquipOffset(matrices, arm, f);
                    applySigma4Animation(matrices, swingProgress);
                } else if (m == AnimationMode.MYAU_1_8) {
                    applyEquipOffset(matrices, arm, f);
                } else if (m == AnimationMode.MYAU_SLIDE) {
                    applyEquipOffset(matrices, arm, f);
                    applyMyauSlideAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.MYAU_SWANK) {
                    applyEquipOffset(matrices, arm, f);
                    applyMyauSwankAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.MYAU_SWANG) {
                    applyEquipOffset(matrices, arm, f);
                    applyMyauSwangAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.MYAU_AVATAR) {
                    applyEquipOffset(matrices, arm, f);
                    applyMyauAvatarAnimation(matrices, swingProgress);
                } else if (m == AnimationMode.MYAU_JIGSAW) {
                    applyEquipOffset(matrices, arm, f);
                    applyMyauJigsawAnimation(matrices);
                }
            } finally {
                this._customTransform = false;
            }
        }
        double s = (double) AnimationConfig.getScale() / 100.0D * (1.0D + AnimationConfig.getItemSize());
        matrices.scale((float) s, (float) s, (float) s);
    }

    private void applyOldAnimation(MatrixStack matrices) {
        matrices.translate(0.08F, -0.14F, -0.05F);
        matrices.translate(-0.35F, 0.2F, 0.0F);
    }

    private void applyPushAnimation(MatrixStack matrices, float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(swing / 2.0F, 1.0F, 4.0F)).rotationDegrees(-swing * 20.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, swing / 3.0F, -0.0F)).rotationDegrees(-swing * 30.0F));
    }

    private void applyDashAnimation(MatrixStack matrices, float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(swing / 2.0F, 0.0F, 9.0F)).rotationDegrees(-swing * 22.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(0.8F, swing / 2.0F, 0.0F)).rotationDegrees(-swing * 50.0F));
    }

    private void applySlashAnimation(MatrixStack matrices, float swingProgress) {
        matrices.translate(0.08F, 0.08F, 0.0F);
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(5.0F, 13.0F, 50.0F)).rotationDegrees(-swing * 70.0F));
    }

    private void applyScaleAnimation(MatrixStack matrices, float swingProgress) {
        matrices.translate(0.84F, -0.77F, -1.1F);
        matrices.translate(0.56F, -0.52F, -0.71999997F);
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.0F, 0.0F)).rotationDegrees(45.0F));
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        float sine = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 0.0F, 0.0F)).rotationDegrees(sine1 * -27.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 0.0F, 0.0F)).rotationDegrees(sine * -27.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 0.0F, 0.0F)).rotationDegrees(sine * -27.0F));
    }

    private void applyMyauSlideAnimation(MatrixStack matrices, float swingProgress) {
        matrices.translate(0.08F, -0.11F, -0.07F);
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.translate(-0.4F, 0.28F, 0.0F);
        matrices.multiply(RotationAxis.of(new Vector3f(-8.0F, -0.0F, 9.0F)).rotationDegrees(-swing * 35.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, -0.4F, -0.0F)).rotationDegrees(-swing * 70.0F));
    }

    private void applyMyauSwankAnimation(MatrixStack matrices, float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(-swing, -0.0F, 9.0F)).rotationDegrees(swing * 15.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, -swing / 2.0F, -0.0F)).rotationDegrees(swing * 40.0F));
    }

    private void applyMyauSwangAnimation(MatrixStack matrices, float swingProgress) {
        matrices.translate(0.0F, 0.03F, 0.0F);
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(swing / 2.0F, 1.0F, 4.0F)).rotationDegrees(-swing * 37.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, swing / 3.0F, -0.0F)).rotationDegrees(-swing * 52.0F));
    }

    private void applyMyauAvatarAnimation(MatrixStack matrices, float swingProgress) {
        matrices.translate(0.56F, -0.52F, -0.72F);
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.0F, 0.0F)).rotationDegrees(45.0F));
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        float sine = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.0F, 0.0F)).rotationDegrees(sine1 * -20.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 0.0F, 1.0F)).rotationDegrees(sine * -20.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, 0.0F, 0.0F)).rotationDegrees(sine * -40.0F));
        matrices.scale(0.4F, 0.4F, 0.4F);
    }

    private void applyMyauJigsawAnimation(MatrixStack matrices) {
        matrices.translate(0.0F, -0.18F, -0.1F);
        matrices.translate(-0.5F, 0.0F, 0.0F);
    }

    private void applySwonkAnimation(MatrixStack matrices, float swingProgress) {
        matrices.translate(0.0F, 0.03F, 0.0F);
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(swing / 2.0F, 1.0F, 4.0F)).rotationDegrees(-swing * -15.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, swing / 3.0F, -0.0F)).rotationDegrees(-swing * 7.5F));
    }

    private void applyStellaAnimation(MatrixStack matrices) {
        matrices.translate(-0.5F, 0.3F, -0.2F);
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.0F, 0.0F)).rotationDegrees(32.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, 0.0F, 0.0F)).rotationDegrees(-70.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.0F, 0.0F)).rotationDegrees(40.0F));
    }

    private void applySmallAnimation(MatrixStack matrices) {
        matrices.translate(-0.01F, 0.03F, -0.24F);
    }

    private void applyEditAnimation(MatrixStack matrices, float swingProgress) {
        matrices.translate(-0.04F, 0.06F, 0.0F);
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(-swing, -0.0F, 2.0F)).rotationDegrees(swing * 8.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, -swing / 3.0F, -0.0F)).rotationDegrees(swing * 22.0F));
    }

    private void applyRhysAnimation(MatrixStack matrices, float swingProgress) {
        matrices.translate(0.0F, 0.19F, 0.0F);
        matrices.translate(0.41F, -0.25F, -0.5555557F);
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.5F, 0.0F)).rotationDegrees(35.0F));
        float slowSwing = MathHelper.sin(swingProgress * swingProgress / 64.0F * 3.1415927F);
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 0.0F, 0.0F)).rotationDegrees(slowSwing * -5.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 0.0F, 1.0F)).rotationDegrees(swing * -12.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, 0.0F, 0.0F)).rotationDegrees(swing * -65.0F));
    }

    private void applyStabAnimation(MatrixStack matrices, float swingProgress) {
        matrices.translate(-0.25F, 0.45F, 0.8F);
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.translate(0.6F, 0.3F, -0.6F + -swing * 0.7F);
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 0.0F, 0.1F)).rotationDegrees(6090.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 0.1F, 0.0F)).rotationDegrees(6085.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(0.1F, 0.0F, 0.0F)).rotationDegrees(6110.0F));
    }

    private void applyFloatAnimation(MatrixStack matrices, float swingProgress) {
        float swing = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(swing / 2.0F, -0.0F, 9.0F)).rotationDegrees(-swing * 20.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, swing / 2.0F, -0.0F)).rotationDegrees(-swing * 30.0F));
    }

    private void applyRemixAnimation(MatrixStack matrices, float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(-2.0F, 0.0F, 10.0F)).rotationDegrees(0.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(0.5F, 0.0F, 1.0F)).rotationDegrees(-swing * 25.0F));
    }

    private void applyXivAnimation(MatrixStack matrices, float swingProgress) {
        float sine = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.0F, 0.0F)).rotationDegrees(-sine1 * 20.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 0.0F, 1.0F)).rotationDegrees(-sine * 20.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, 0.0F, 0.0F)).rotationDegrees(-sine * 80.0F));
    }

    private void applyWinterAnimation(MatrixStack matrices) {
        matrices.translate(0.0F, -0.16F, 0.0F);
        matrices.translate(-0.35F, 0.1F, 0.0F);
        matrices.translate(-0.05F, -0.1F, 0.1F);
    }

    private void applyYamatoAnimation(MatrixStack matrices, float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(-9.0F, 5.0F, 9.0F)).rotationDegrees(-swing * 100.0F));
    }

    private void applySlideSwingAnimation(MatrixStack matrices, float swingProgress) {
        matrices.translate(0.56F, -0.52F, -0.72F);
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.0F, 0.0F)).rotationDegrees(45.0F));
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, 0.0F, 0.0F)).rotationDegrees(swing * -80.0F));
        matrices.scale(0.4F, 0.4F, 0.4F);
    }

    private void applySmallPushAnimation(MatrixStack matrices, float swingProgress) {
        matrices.translate(0.56F, -0.52F, -0.72F);
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.0F, 0.0F)).rotationDegrees(45.0F));
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        float sine = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, 1.0F, 1.0F)).rotationDegrees(sine1 * -10.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, 1.0F, 1.0F)).rotationDegrees(sine * -10.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, 1.0F, 1.0F)).rotationDegrees(sine * -10.0F));
        matrices.scale(0.4F, 0.4F, 0.4F);
    }

    private void applyReverseAnimation(MatrixStack matrices) {
        matrices.translate(0.0F, 0.1F, -0.12F);
        matrices.translate(0.08F, -0.1F, -0.3F);
    }

    private void applyInventAnimation(MatrixStack matrices, float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(-8.0F, -0.2F, 9.0F)).rotationDegrees(-swing * 30.0F));
    }

    private void applyLeakedAnimation(MatrixStack matrices, float swingProgress) {
        matrices.translate(0.08F, 0.02F, 0.0F);
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(1.1F, 0.8F, -0.3F)).rotationDegrees(-swing * 41.0F));
    }

    private void applyAquaAnimation(MatrixStack matrices, float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(swing / 2.0F, 1.0F, 4.0F)).rotationDegrees(-swing * 8.5F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, swing / 3.0F, -0.0F)).rotationDegrees(-swing * 6.0F));
    }

    private void applyAstroAnimation(MatrixStack matrices, float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(-swing, -0.0F, 90.0F)).rotationDegrees(swing * 50.0F / 9.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(200.0F, -swing / 2.0F, -0.0F)).rotationDegrees(swing * 50.0F));
    }

    private void applyFadeawayAnimation(MatrixStack matrices, float swingProgress) {
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 0.0F, 1.0F)).rotationDegrees(-sine1 * 45.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 0.0F, 1.0F)).rotationDegrees(0.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.5F, 0.0F, 0.0F)).rotationDegrees(0.0F));
    }

    private void applyAstolfoAnimation(MatrixStack matrices, float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(swing / 2.0F, 1.0F, 0.5F)).rotationDegrees(-swing * 29.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, swing / 3.0F, -0.0F)).rotationDegrees(-swing * 43.0F));
    }

    private void applyAstolfoSpinAnimation(MatrixStack matrices) {
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 0.0F, -0.1F)).rotationDegrees(this.delay));
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastUpdateTime;
        this.delay += elapsedTime * 360.0F / 850.0F;
        lastUpdateTime = currentTime;
        if (this.delay > 360.0F) {
            this.delay = 0.0F;
        }
    }

    private void applyMoonAnimation(MatrixStack matrices, float swingProgress) {
        matrices.translate(-0.08F, 0.12F, 0.0F);
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(swing / 2.0F, 1.0F, 4.0F)).rotationDegrees(-swing * 32.5F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, swing / 3.0F, -0.0F)).rotationDegrees(-swing * 60.0F));
    }

    private void applyMoonPushAnimation(MatrixStack matrices, float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.translate(-0.2F, 0.45F, 0.25F);
        matrices.multiply(RotationAxis.of(new Vector3f(-5.0F, -5.0F, 9.0F)).rotationDegrees(-swing * 20.0F));
    }

    private void applySmoothAnimation(MatrixStack matrices, float swingProgress) {
        matrices.translate(0.14F, -0.1F, -0.24F);
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.translate(-0.36F, 0.25F, -0.06F);
        matrices.multiply(RotationAxis.of(new Vector3f(-8.0F, -0.0F, 9.0F)).rotationDegrees(-swing * 35.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, 0.4F, -0.0F)).rotationDegrees(-swing * 70.0F));
    }

    private void applyTap1Animation(MatrixStack matrices, float swingProgress) {
        matrices.translate(0.56F, -0.52F, -0.71999997F);
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.0F, 0.0F)).rotationDegrees(45.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.0F, 0.0F)).rotationDegrees((swingProgress * 0.8F - swingProgress * swingProgress * 0.8F) * -90.0F));
        matrices.scale(0.37F, 0.37F, 0.37F);
    }

    private void applyTap2Animation(MatrixStack matrices, float swingProgress) {
        matrices.translate(0.0F, -0.1F, 0.0F);
        matrices.translate(0.56F, -0.42F, -0.71999997F);
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.0F, 0.0F)).rotationDegrees(30.0F));
        matrices.multiply(RotationAxis.of(new Vector3f(0.0F, 1.0F, 0.0F)).rotationDegrees(MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F) * -30.0F));
        matrices.scale(0.4F, 0.4F, 0.4F);
    }

    private void applySigma3Animation(MatrixStack matrices, float swingProgress) {
        matrices.translate(0.02F, 0.02F, 0.0F);
        matrices.translate(0.4F, -0.06F, -0.46F);
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(-swing, -0.0F, 9.0F)).rotationDegrees(swing * 12.5F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, -swing / 2.0F, -0.0F)).rotationDegrees(swing * 15.0F));
    }

    private void applySigma4Animation(MatrixStack matrices, float swingProgress) {
        matrices.translate(-0.6F, 0.2F, 0.11F);
        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.of(new Vector3f(-8.0F, -0.0F, 9.0F)).rotationDegrees(-swing * 27.5F));
        matrices.multiply(RotationAxis.of(new Vector3f(1.0F, swing / 2.0F, 0.0F)).rotationDegrees(-swing * 45.0F));
        matrices.translate(-0.08F, -1.25F, 1.25F);
    }

    private boolean shouldApplyNormalAnimations() {
        OldHitting oldHitting = (OldHitting) Myau.moduleManager.modules.get(OldHitting.class);
        boolean oldHittingActive = oldHitting != null && oldHitting.isEnabled() && oldHitting.isKillAuraAttacking();
        // 只有 Animations 模块开启(renderMode 1)或 OldHitting 正在生效时才取消原版挥剑/装备位移,
        // 否则两个模块都关掉时原版位移也被取消、替换动画又不生效 → 手持剑渲染异常
        return (AnimationConfig.isEnabled() && AnimationConfig.getRenderMode() == 1 || oldHittingActive)
                && mc.player != null
                && mc.player.getMainHandStack() != null
                && mc.player.getMainHandStack().getItem() instanceof SwordItem
                && mc.player.getItemUseTimeLeft() <= 0
                && !mc.player.isBlocking();
    }
}
