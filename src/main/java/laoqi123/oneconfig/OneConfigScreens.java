package laoqi123.oneconfig;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

/**
 * Helpers for asking about the OneConfig settings screen without referencing its
 * Kotlin/Compose types directly.
 *
 * <p>The screen class lives in OneConfig's internal implementation package, so it
 * is resolved by name and cached. When OneConfig is missing or its UI cannot
 * start, every query simply reports "not open".
 */
public final class OneConfigScreens {

    private static final String COMPOSE_SCREEN =
            "org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen";

    private static Class<?> composeScreen;
    private static boolean resolved;

    private OneConfigScreens() {
    }

    private static Class<?> composeScreenClass() {
        if (!resolved) {
            resolved = true;
            try {
                composeScreen = Class.forName(COMPOSE_SCREEN, false,
                        OneConfigScreens.class.getClassLoader());
            } catch (Throwable ignored) {
                composeScreen = null;
            }
        }
        return composeScreen;
    }

    /** True when the given screen is a OneConfig Compose screen. */
    public static boolean isSettingsScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        Class<?> clazz = composeScreenClass();
        return clazz != null && clazz.isInstance(screen);
    }

    /**
     * True when the settings menu is the screen currently on top. Replaces the old
     * {@code mc.currentScreen instanceof ClickGui} checks.
     */
    public static boolean isSettingsOpen() {
        return isSettingsScreen(MinecraftClient.getInstance().currentScreen);
    }

    private static final String UI_SCREEN =
            "org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen";

    /**
     * Build the settings screen already opened on Myau's own config page, so pressing
     * the ClickGUI key lands on the categories and modules instead of the mod grid.
     *
     * <p>{@code OneConfigUIScreen(String treeId)} takes the config id and turns it into
     * the matching route itself, and an explicitly supplied route wins over the user's
     * "opening behavior" preference. The class sits in OneConfig's internal package so
     * it is resolved by name; {@code null} means the caller should fall back to
     * {@code OneConfigUI.open()} and whatever page that chooses.
     */
    public static Screen myauSettingsScreen() {
        try {
            Class<?> screen = Class.forName(UI_SCREEN, true,
                    OneConfigScreens.class.getClassLoader());
            return (Screen) screen.getConstructor(String.class)
                    .newInstance(MyauOneConfig.CONFIG_ID);
        } catch (Throwable t) {
            com.mojang.logging.LogUtils.getLogger().warn(
                    "[Myau] Could not build the direct settings screen, "
                            + "falling back to OneConfig's default page", t);
            return null;
        }
    }
}
