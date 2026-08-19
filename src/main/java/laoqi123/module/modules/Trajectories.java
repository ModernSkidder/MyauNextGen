package laoqi123.module.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import laoqi123.event.EventTarget;
import laoqi123.events.Render3DEvent;
import laoqi123.mixin.EntityRenderDispatcherAccessor;
import laoqi123.module.Module;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.PercentProperty;
import laoqi123.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.BowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.SnowballItem;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Trajectories extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final PercentProperty opacity = new PercentProperty("opacity", 100);
    public final BooleanProperty bow = new BooleanProperty("bow", true);
    public final BooleanProperty projectiles = new BooleanProperty("projectiles", false);
    public final BooleanProperty pearls = new BooleanProperty("pearls", true);

    public Trajectories() {
        super("Trajectories", false, true);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && mc.player.getMainHandStack() != null && mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            Item item = mc.player.getMainHandStack().getItem();
            boolean isBow = false;
            float velocityMultiplier = 1.5F;
            float drag = 0.99F;
            float gravity;
            float hitboxExpand;
            if (item instanceof BowItem && this.bow.getValue()) {
                if (!mc.player.isUsingItem()) {
                    return;
                }
                isBow = true;
                gravity = 0.05F;
                hitboxExpand = 0.3F;
                float charge = (float) mc.player.getItemUseTime() / 20.0F;
                charge = (charge * charge + charge * 2.0F) / 3.0F;
                if (charge < 0.1F) {
                    return;
                }
                if (charge > 1.0F) {
                    charge = 1.0F;
                }
                velocityMultiplier = charge * 3.0F;
            } else if (item instanceof FishingRodItem && this.projectiles.getValue()) {
                gravity = 0.04F;
                hitboxExpand = 0.25F;
                drag = 0.92F;
            } else if ((item instanceof SnowballItem || item instanceof EggItem) && this.projectiles.getValue()) {
                gravity = 0.03F;
                hitboxExpand = 0.25F;
            } else {
                if (!(item instanceof EnderPearlItem) || !this.pearls.getValue()) {
                    return;
                }
                gravity = 0.03F;
                hitboxExpand = 0.25F;
            }
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();
            double x = ((EntityRenderDispatcherAccessor) mc.getEntityRenderDispatcher()).getCamera().getPos().x - (double) MathHelper.cos(yaw / 180.0F * (float) Math.PI) * 0.16;
            double y = ((EntityRenderDispatcherAccessor) mc.getEntityRenderDispatcher()).getCamera().getPos().y + (double) mc.player.getStandingEyeHeight() - 0.1F;
            double z = ((EntityRenderDispatcherAccessor) mc.getEntityRenderDispatcher()).getCamera().getPos().z - (double) MathHelper.sin(yaw / 180.0F * (float) Math.PI) * 0.16;
            double mx = (double) (MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI))
                    * (isBow ? 1.0 : 0.4)
                    * -1.0;
            double my = (double) MathHelper.sin(pitch / 180.0F * (float) Math.PI) * (isBow ? 1.0 : 0.4) * -1.0;
            double mz = (double) (MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI)) * (isBow ? 1.0 : 0.4);
            float mag = MathHelper.sqrt((float) (mx * mx + my * my + mz * mz));
            mx /= mag;
            my /= mag;
            mz /= mag;
            mx *= velocityMultiplier;
            my *= velocityMultiplier;
            mz *= velocityMultiplier;
            HitResult mop = null;
            boolean hasHitBlock = false;
            boolean hasHitEntity = false;
            ArrayList<Vec3d> trajectoryPoints = new ArrayList<>();
            while (!hasHitBlock && y > 0.0) {
                Vec3d start = new Vec3d(x, y, z);
                Vec3d end = new Vec3d(x + mx, y + my, z + mz);
                mop = mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
                start = new Vec3d(x, y, z);
                end = new Vec3d(x + mx, y + my, z + mz);
                if (mop != null) {
                    hasHitBlock = true;
                    end = new Vec3d(mop.getPos().x, mop.getPos().y, mop.getPos().z);
                }
                Box aabb = new Box(
                        x - (double) hitboxExpand,
                        y - (double) hitboxExpand,
                        z - (double) hitboxExpand,
                        x + (double) hitboxExpand,
                        y + (double) hitboxExpand,
                        z + (double) hitboxExpand
                )
                        .offset(mx, my, mz)
                        .expand(1.0, 1.0, 1.0);
                List<Entity> possibleEntities = mc.world.getOtherEntities(mc.player, aabb, entity -> true);
                for (Entity entity : possibleEntities) {
                    if (entity.isCollidable() && entity != mc.player) {
                        Box entityBox = entity.getBoundingBox().expand(hitboxExpand, hitboxExpand, hitboxExpand);
                        Optional<Vec3d> intercept = entityBox.raycast(start, end);
                        if (intercept.isPresent()) {
                            hasHitEntity = true;
                            hasHitBlock = true;
                            mop = new BlockHitResult(intercept.get(), Direction.UP, BlockPos.ORIGIN, false);
                        }
                    }
                }
                x += mx;
                y += my;
                z += mz;
                if (mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).getFluidState().isIn(FluidTags.WATER)) {
                    mx *= 0.6;
                    my *= 0.6;
                    mz *= 0.6;
                } else {
                    mx *= drag;
                    my *= drag;
                    mz *= drag;
                }
                my -= gravity;
                trajectoryPoints.add(
                        new Vec3d(
                                x - ((EntityRenderDispatcherAccessor) mc.getEntityRenderDispatcher()).getCamera().getPos().x,
                                y - ((EntityRenderDispatcherAccessor) mc.getEntityRenderDispatcher()).getCamera().getPos().y,
                                z - ((EntityRenderDispatcherAccessor) mc.getEntityRenderDispatcher()).getCamera().getPos().z
                        )
                );
            }
            if (trajectoryPoints.size() > 1) {
                int trajectoryColor = new Color(hasHitEntity ? 85 : 255, 255, hasHitEntity ? 85 : 255, (int) (this.opacity.getValue().floatValue() / 100.0F * 255.0F)).getRGB();
                RenderUtil.enableRenderState();
                RenderUtil.setColor(trajectoryColor);
                RenderSystem.lineWidth(1.5F);
                RenderSystem.setShader(ShaderProgramKeys.POSITION);
                BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION);
                for (Vec3d vec3 : trajectoryPoints) {
                    bufferBuilder.vertex((float) vec3.x, (float) vec3.y, (float) vec3.z);
                }
                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
                if (mop != null) {
                    Vec3d impact = trajectoryPoints.get(trajectoryPoints.size() - 1);
                    this.draw3DLine(new Vec3d(impact.x - 0.25, impact.y - 0.25, impact.z), new Vec3d(impact.x + 0.25, impact.y + 0.25, impact.z), trajectoryColor, 1.5F);
                    this.draw3DLine(new Vec3d(impact.x - 0.25, impact.y + 0.25, impact.z), new Vec3d(impact.x + 0.25, impact.y - 0.25, impact.z), trajectoryColor, 1.5F);
                }
                RenderSystem.lineWidth(2.0F);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderUtil.disableRenderState();
            }
        }
    }

    private void draw3DLine(Vec3d start, Vec3d end, int color, float width) {
        RenderUtil.setColor(color);
        RenderSystem.lineWidth(width);
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION);
        bufferBuilder.vertex((float) start.x, (float) start.y, (float) start.z);
        bufferBuilder.vertex((float) end.x, (float) end.y, (float) end.z);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
}
