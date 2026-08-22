package laoqi123.module.modules;

import laoqi123.Myau;
import laoqi123.module.Module;
import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.ModeProperty;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

public class OldHitting extends Module {
    public final ModeProperty animation = new ModeProperty("Animation", 1, new String[]{"Vanilla", "Leaked", "Slide"});
    public final FloatProperty size = new FloatProperty("Size", 1.0F, 0.1F, 3.0F);
    public final FloatProperty speed = new FloatProperty("Speed", 1.0F, 0.1F, 5.0F);
    public final FloatProperty yOffset = new FloatProperty("Y-Offset", 0.0F, -1.0F, 1.0F);

    public OldHitting() {
        super("OldHitting", true);
    }

    public boolean isKillAuraAttacking() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        return killAura != null
                && killAura.isEnabled()
                && killAura.autoBlock.mode.getValue() == 4
                && killAura.getTarget() != null;
    }

    public void applyHitAnimation(MatrixStack matrices, float progress, Arm arm, float equipProgress) {
        float scaledProgress = progress * this.speed.getValue();
        float size = this.size.getValue();
        matrices.translate(0.0F, this.yOffset.getValue(), 0.0F);
        if (this.animation.getValue() == 0) {
            int side = arm == Arm.RIGHT ? 1 : -1;
            translate((float) side * 0.56F, -0.52F + equipProgress * -0.6F, -0.72, matrices);
            translate((float) side * -0.1414214F, 0.08F, 0.1414213925600052, matrices);
            rotate(-102.25F, 1.0F, 0.0F, 0.0F, matrices);
            rotate((float) side * 13.365F, 0.0F, 1.0F, 0.0F, matrices);
            rotate((float) side * 78.05F, 0.0F, 0.0F, 1.0F, matrices);
            double sinSquared = Math.sin((double) (scaledProgress * scaledProgress) * Math.PI);
            double sinSqrt = Math.sin(Math.sqrt(scaledProgress) * Math.PI);
            rotate((float) (sinSquared * -20.0), 0.0F, 1.0F, 0.0F, matrices);
            rotate((float) (sinSqrt * -20.0), 0.0F, 0.0F, 1.0F, matrices);
            rotate((float) (sinSqrt * -80.0), 1.0F, 0.0F, 0.0F, matrices);
            scale(size, size, size, matrices);
        } else if (this.animation.getValue() == 1) {
            setupLeakedAnim(matrices, equipProgress, scaledProgress, size);
            setupLeakedArmPos(matrices);
            float pulse = MathHelper.sin(MathHelper.sqrt(scaledProgress) * (float) Math.PI) / 8.0F;
            matrices.translate(0.008F, 0.24F, 0.03F);
            matrices.translate(-0.16F, -0.25F, 0.0F);
            matrices.scale((0.8F + pulse) * size, (0.8F + pulse) * size, (0.8F + pulse) * size);
            rotate(-MathHelper.sin(MathHelper.sqrt(scaledProgress) * (float) Math.PI) * 20.0F, 0.0F, 1.2F, -0.8F, matrices);
            rotate(-MathHelper.sin(MathHelper.sqrt(scaledProgress) * (float) Math.PI) * 30.0F, 1.0F, 0.0F, 0.0F, matrices);
            matrices.scale(2.4F * size, 2.4F * size, 2.4F * size);
            rotate(-38.4F, 0.0F, 1.0F, 0.0F, matrices);
            scale(size, size, size, matrices);
        } else if (this.animation.getValue() == 2) {
            float slideSwing = MathHelper.sin(MathHelper.sqrt(scaledProgress) * (float) Math.PI);
            translate(0.648F, -0.55F, -0.7199999690055847, matrices);
            rotate(77.0F, 0.0F, 1.0F, 0.0F, matrices);
            rotate(-10.0F, 0.0F, 0.0F, 1.0F, matrices);
            rotate(-80.0F, 1.0F, 0.0F, 0.0F, matrices);
            rotate(-slideSwing * 20.0F, 1.0F, 0.0F, 0.0F, matrices);
            scale(1.2F * size, 1.2F * size, 1.2F * size, matrices);
            scale(size, size, size, matrices);
        }
    }

    private void setupLeakedAnim(MatrixStack matrices, float equipProgress, float scaledProgress, float size) {
        matrices.translate(0.56F, -0.52F, -0.71999997F);
        rotate(45.0F, 0.0F, 1.0F, 0.0F, matrices);
        float sinSquared = MathHelper.sin(scaledProgress * scaledProgress * (float) Math.PI);
        float sinSqrt = MathHelper.sin(MathHelper.sqrt(scaledProgress) * (float) Math.PI);
        rotate(sinSquared * -20.0F, 0.0F, 1.0F, 0.0F, matrices);
        rotate(sinSqrt * -20.0F, 0.0F, 0.0F, 1.0F, matrices);
        rotate(sinSqrt * -80.0F, 1.0F, 0.0F, 0.0F, matrices);
        matrices.scale(0.4F * size, 0.4F * size, 0.4F * size);
    }

    private void setupLeakedArmPos(MatrixStack matrices) {
        matrices.translate(-0.5F, 0.2F, 0.0F);
        rotate(30.0F, 0.0F, 1.0F, 0.0F, matrices);
        rotate(-80.0F, 1.0F, 0.0F, 0.0F, matrices);
        rotate(60.0F, 0.0F, 1.0F, 0.0F, matrices);
    }

    private static void translate(double tx, double ty, double tz, MatrixStack matrices) {
        matrices.translate(tx, ty, tz);
    }

    private static void rotate(float angle, float ax, float ay, float az, MatrixStack matrices) {
        matrices.multiply(RotationAxis.of(new Vector3f(ax, ay, az)).rotationDegrees(angle));
    }

    private static void scale(float sx, float sy, float sz, MatrixStack matrices) {
        matrices.scale(sx, sy, sz);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.animation.getModeString()};
    }
}