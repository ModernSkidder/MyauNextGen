package laoqi123.util;

import net.minecraft.client.MinecraftClient;

public class MovementUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean cancelMove = false;

    public static void cancelMove() {
        cancelMove = true;
        if (mc.player != null) {
            mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
        }
    }

    public static void resetMove() {
        cancelMove = false;
    }
}