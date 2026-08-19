package laoqi123.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class ServerUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static ArrayList<String> getScoreboardLines() {
        if (ServerUtil.mc.world == null) {
            return new ArrayList<>();
        }
        Scoreboard scoreboard = ServerUtil.mc.world.getScoreboard();
        if (scoreboard == null) {
            return new ArrayList<>();
        }
        ScoreboardObjective scoreObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (scoreObjective == null) {
            return new ArrayList<>();
        }
        return scoreboard.getScoreboardEntries(scoreObjective).stream()
                .map(entry -> Team.decorateName(scoreboard.getTeam(entry.owner()), Text.literal(entry.owner())).getString())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static boolean isHypixel() {
        ArrayList<String> arrayList = ServerUtil.getScoreboardLines();
        if (arrayList.isEmpty()) return false;
        if (arrayList.get(0).equals("§ewww.hypixel.ne🎂§et")) return true;
        return arrayList.get(0).equals("§ewww.hypixel.ne§g§et");
    }

    public static boolean hasPlayerCountInfo() {
        for (String s : ServerUtil.getScoreboardLines()) {
            if (!s.matches(".*Players: §a\\d+/\\d+.*")) continue;
            return true;
        }
        return false;
    }
}
