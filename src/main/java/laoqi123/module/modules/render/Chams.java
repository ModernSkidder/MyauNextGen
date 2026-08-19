package laoqi123.module.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.impl.RenderLivingEvent;
import laoqi123.module.Module;
import laoqi123.value.properties.BooleanValue;
import laoqi123.util.TeamUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;

public class Chams extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanValue players = new BooleanValue("players", true);
    public final BooleanValue friends = new BooleanValue("friends", true);
    public final BooleanValue enemiess = new BooleanValue("enemies", true);
    public final BooleanValue bosses = new BooleanValue("bosses", false);
    public final BooleanValue mobs = new BooleanValue("mobs", false);
    public final BooleanValue creepers = new BooleanValue("creepers", false);
    public final BooleanValue enderman = new BooleanValue("endermen", false);
    public final BooleanValue blaze = new BooleanValue("blazes", false);
    public final BooleanValue animals = new BooleanValue("animals", false);
    public final BooleanValue self = new BooleanValue("self", false);
    public final BooleanValue bots = new BooleanValue("bots", false);

    private boolean shouldRenderChams(LivingEntity entityLivingBase) {
        if (entityLivingBase.deathTime > 0) {
            return false;
        } else if (mc.getCameraEntity().distanceTo(entityLivingBase) > 512.0F) {
            return false;
        } else if (entityLivingBase instanceof PlayerEntity) {
            if (entityLivingBase != mc.player && entityLivingBase != mc.getCameraEntity()) {
                if (TeamUtil.isBot((PlayerEntity) entityLivingBase)) {
                    return this.bots.getValue();
                } else if (TeamUtil.isFriend((PlayerEntity) entityLivingBase)) {
                    return this.friends.getValue();
                } else {
                    return TeamUtil.isTarget((PlayerEntity) entityLivingBase) ? this.enemiess.getValue() : this.players.getValue();
                }
            } else {
                return this.self.getValue() && !mc.options.getPerspective().isFirstPerson();
            }
        } else if (entityLivingBase instanceof EnderDragonEntity || entityLivingBase instanceof WitherEntity) {
            return !entityLivingBase.isInvisible() && this.bosses.getValue();
        } else if (!(entityLivingBase instanceof MobEntity) && !(entityLivingBase instanceof SlimeEntity)) {
            return (entityLivingBase instanceof AnimalEntity
                    || entityLivingBase instanceof BatEntity
                    || entityLivingBase instanceof SquidEntity
                    || entityLivingBase instanceof VillagerEntity) && this.animals.getValue();
        } else if (entityLivingBase instanceof CreeperEntity) {
            return this.creepers.getValue();
        } else if (entityLivingBase instanceof EndermanEntity) {
            return this.enderman.getValue();
        } else {
            return entityLivingBase instanceof BlazeEntity ? this.blaze.getValue() : this.mobs.getValue();
        }
    }

    public Chams() {
        super("Chams", false);
    }

    @EventTarget
    public void onRenderLiving(RenderLivingEvent event) {
        if (this.isEnabled() && this.shouldRenderChams(event.getEntity())) {
            if (event.getType() == EventType.PRE) {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(1.0F, -2500000.0F);
            } else {
                RenderSystem.polygonOffset(1.0F, 2500000.0F);
                RenderSystem.disablePolygonOffset();
            }
        }
    }
}
