package laoqi123.util;

import laoqi123.Myau;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TeamUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean isEntityLoaded(Entity entity) {
        if (entity == null) return false;
        for (Entity loaded : TeamUtil.mc.world.getEntities()) {
            if (loaded == entity) return true;
        }
        return false;
    }

    public static List<Entity> getLoadedEntitiesSorted() {
        List<Entity> entities = new ArrayList<>();
        for (Entity entity : TeamUtil.mc.world.getEntities()) {
            entities.add(entity);
        }
        net.minecraft.client.render.Camera camera = mc.gameRenderer.getCamera();
        entities.sort((entity1, entity2) -> {
            double dist1 = camera.getPos().squaredDistanceTo(entity1.getPos());
            double dist2 = camera.getPos().squaredDistanceTo(entity2.getPos());
            if (dist1 < dist2) {
                return 1;
            }
            if (dist1 > dist2) {
                return -1;
            }
            return entity1.getUuid().toString().compareTo(entity2.getUuid().toString());
        });
        return entities;
    }

    public static float getHealthScore(LivingEntity livingEntity) {
        float armorValue = (float) livingEntity.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ARMOR);
        return livingEntity.getHealth() * (20.0f / Math.max(armorValue, 1.0f));
    }

    public static String stripName(Entity entity) {
        return entity.getDisplayName().getString().replaceAll("§\\S$", "").replaceAll("(?i)§r", "§f").trim();
    }

    public static Color getTeamColor(PlayerEntity player, float alpha) {
        int colorCode = 0xFFFFFF;
        Team playerTeam = player.getScoreboardTeam();
        if (playerTeam != null) {
            Formatting formatting = playerTeam.getColor();
            if (formatting != null && formatting.getColorValue() != null) {
                colorCode = formatting.getColorValue();
            }
        }
        return new Color(colorCode & 0xFFFFFF | (int) (alpha * 255) << 24, true);
    }

    public static boolean isBot(PlayerEntity player) {
        if (player == TeamUtil.mc.player) {
            return false;
        }
        PlayerListEntry playerInfo = mc.getNetworkHandler().getPlayerListEntry(player.getName().getString());
        if (playerInfo == null) {
            return true;
        }
        if (!ServerUtil.isHypixel()) return false;
        if (player.getName().getString().startsWith("§k")) {
            return player.isInvisible();
        }
        if (playerInfo.getLatency() < 1) {
            return true;
        }
        Team playerTeam = player.getScoreboardTeam();
        if (playerTeam == null) return false;
        if (!playerTeam.getName().isEmpty()) return false;
        return playerTeam.getColor() == Formatting.RED;
    }

    public static boolean isSameTeam(PlayerEntity player) {
        if (player == TeamUtil.mc.player) {
            return true;
        }
        PlayerListEntry selfInfo = mc.getNetworkHandler().getPlayerListEntry(TeamUtil.mc.player.getUuid());
        if (selfInfo == null) {
            return false;
        }
        Team selfTeam = mc.player.getScoreboardTeam();
        if (selfTeam == null) {
            return false;
        }
        PlayerListEntry targetInfo = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (targetInfo == null) {
            return false;
        }
        Team targetTeam = player.getScoreboardTeam();
        if (targetTeam == null) {
            return false;
        }
        return selfTeam.getColor() == targetTeam.getColor();
    }

    public static boolean hasTeamColor(LivingEntity entity) {
        if (entity == TeamUtil.mc.player) {
            return true;
        }
        Team selfTeam = mc.player.getScoreboardTeam();
        if (selfTeam == null) {
            return false;
        }
        if (selfTeam.getColor() == null) {
            return false;
        }
        LivingEntity nearestArmorStand = findNearestArmorStand(entity.getBoundingBox());
        if (nearestArmorStand != null) {
            String prefix = selfTeam.getColor().toString();
            return nearestArmorStand.getName().getString().contains(prefix);
        }
        return false;
    }

    public static boolean isShop(LivingEntity entity) {
        if (entity == TeamUtil.mc.player) {
            return false;
        }
        LivingEntity armorStand = findNearestArmorStand(entity.getBoundingBox());
        if (armorStand == null) return false;
        String displayName = armorStand.getDisplayName().getString();
        if (displayName.contains("RIGHT CLICK")) return true;
        if (displayName.contains("ITEM SHOP")) return true;
        if (displayName.contains("UPGRADES")) return true;
        if (displayName.contains("BANKER")) return true;
        return displayName.contains("STREAK POWERS");
    }

    private static LivingEntity findNearestArmorStand(Box box) {
        List<ArmorStandEntity> armorStands = TeamUtil.mc.world.getNonSpectatingEntities(ArmorStandEntity.class, box.expand(3.0));
        if (armorStands.isEmpty()) return null;
        return armorStands.stream()
                .min(Comparator.comparingDouble(stand -> stand.getPos().squaredDistanceTo(TeamUtil.mc.player.getPos())))
                .orElse(null);
    }

    public static boolean isTeammate(Entity entity) {
        if (!(entity instanceof PlayerEntity)) return false;
        return isSameTeam((PlayerEntity) entity);
    }

    public static boolean isFriend(PlayerEntity player) {
        return Myau.friendManager.isFriend(player.getName().getString());
    }

    public static boolean isTarget(PlayerEntity player) {
        return Myau.targetManager.isFriend(player.getName().getString());
    }
}
