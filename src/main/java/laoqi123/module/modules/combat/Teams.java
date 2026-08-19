package laoqi123.module.modules.combat;

import laoqi123.module.Module;
import laoqi123.value.properties.ModeValue;
import laoqi123.util.TeamUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;

import java.util.Objects;

public class Teams extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static Teams INSTANCE;
    public final ModeValue mode = new ModeValue("Mode", 1, new String[]{"Color", "Scoreboard"});

    public Teams() {
        super("Teams", false);
        INSTANCE = this;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }

    public static boolean isSameTeam(Entity entity) {
        Teams teams = INSTANCE;
        if (teams == null || !teams.isEnabled()) {
            return false;
        }
        if (!(entity instanceof PlayerEntity)) {
            return false;
        }
        if (teams.mode.getValue() == 0) {
            return entity.getTeamColorValue() == mc.player.getTeamColorValue();
        }
        String team = Teams.getTeam(entity);
        String selfTeam = Teams.getTeam(mc.player);
        return Objects.equals(team, selfTeam);
    }

    public static boolean isKillAuraTeam(PlayerEntity player) {
        Teams teams = INSTANCE;
        if (teams != null && teams.isEnabled()) {
            return Teams.isSameTeam(player);
        }
        return TeamUtil.isSameTeam(player);
    }

    public static String getTeam(Entity entity) {
        PlayerListEntry playerInfo = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
        if (playerInfo == null) {
            return null;
        }
        Team team = playerInfo.getScoreboardTeam();
        return team == null ? null : team.getName();
    }
}