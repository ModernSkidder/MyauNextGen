package laoqi123.module.modules.movement;

import laoqi123.event.EventTarget;
import laoqi123.event.impl.MoveInputEvent;
import laoqi123.event.impl.TickEvent;
import laoqi123.event.impl.UpdateEvent;
import laoqi123.event.types.EventType;
import laoqi123.management.RotationState;
import laoqi123.mixin.LivingEntityAccessor;
import laoqi123.module.Module;
import laoqi123.util.KeyBindUtil;
import laoqi123.value.properties.BooleanValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;

public class Sprint extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private boolean wasSprinting = false;
    private float naturalYaw = 0f;
    public final BooleanValue foxFix = new BooleanValue("fov-fix", true);

    public Sprint() {
        super("Sprint", true, true);
    }

    public boolean shouldApplyFovFix(EntityAttributeInstance attribute) {
        if (!this.foxFix.getValue()) {
            return false;
        }
        EntityAttributeModifier attributeModifier = ((LivingEntityAccessor) mc.player).getSprintingSpeedBoostModifier();
        return attribute.getModifier(attributeModifier.id()) == null && this.wasSprinting;
    }

    public boolean shouldKeepFov(boolean boolean2) {
        return this.foxFix.getValue() && !boolean2 && this.wasSprinting;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            switch (event.getType()) {
                case PRE:
                    if (!mc.options.sprintKey.isPressed()) {
                        KeyBindUtil.setKeyBindState(this.getSprintKeyCode(), true);
                    }
                    break;
                case POST:
                    this.wasSprinting = mc.player.isSprinting();
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.PRE || mc.player == null) return;
        // 玩家 tick HEAD:此刻鼠标已处理、旋转还没被 mixin 应用,getYaw() 是真实自然朝向
        this.naturalYaw = mc.player.getYaw();
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!isEnabled() || mc.player == null) return;
        // 移动方向 = MoveFix 实际使用的 yaw(RotationState.smoothYaw,由 setPervRotation 喂入),
        // 而非实体 yaw(旋转只影响包和移动计算,实体 yaw 渲染时是还原后的自然朝向)
        float appliedYaw = RotationState.isActived() ? RotationState.getSmoothedYaw() : mc.player.getYaw();
        // 与玩家自然朝向夹角 >90° = 正在回头/向后移动(如 Scaffold 向后自救):
        // 疾跑方向与移动方向相反会触发 Grim Simulation,这里强制关掉疾跑
        float yawDiff = Math.abs(MathHelper.wrapDegrees(appliedYaw - this.naturalYaw));
        if (yawDiff > 90f) {
            PlayerInput pi = mc.player.input.playerInput;
            if (pi != null && pi.sprint()) {
                mc.player.input.playerInput = new PlayerInput(pi.forward(), pi.backward(), pi.left(), pi.right(), pi.jump(), pi.sneak(), false);
            }
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }
        } else {
            // 向前阶段:主动把疾跑拉回来 —— 原版 updateSprintingState 在陆地上只能"维持"疾跑、
            // 不能重新开启,而疾跑键的 wasPressed 只在边沿触发一次,所以一旦被砍就永远回不来,
            // 结果就是向前变成走路、拉不起速度,在方块上徘徊
            if (!mc.player.isSprinting() && mc.player.input.movementForward > 0.0f && !mc.player.isSneaking()) {
                mc.player.setSprinting(true);
            }
            if (!mc.options.sprintKey.isPressed()) {
                KeyBindUtil.setKeyBindState(this.getSprintKeyCode(), true);
            }
        }
    }

    @Override
    public void onDisabled() {
        this.wasSprinting = false;
        KeyBindUtil.updateKeyState(this.getSprintKeyCode());
    }

    private int getSprintKeyCode() {
        return InputUtil.fromTranslationKey(mc.options.sprintKey.getBoundKeyTranslationKey()).getCode();
    }
}
