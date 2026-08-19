package me.ksyz.accountmanager;

import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.SessionManager;
import me.ksyz.accountmanager.gui.GuiAccountManager;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.WeakHashMap;

/*
 * This file is derived from https://github.com/ksyzov/AccountManager.
 * Originally licensed under the GNU LGPL.
 *
 * This modified version is licensed under the GNU GPL v3.
 */
public class Events {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final WeakHashMap<Screen, ButtonWidget> accountsButtons = new WeakHashMap<>();
    private static final WeakHashMap<Screen, Drawable> usernameOverlays = new WeakHashMap<>();
    private static final WeakHashMap<Screen, Boolean> disconnectedScreens = new WeakHashMap<>();
    private static boolean wasInWorld = false;
    private static boolean registered = false;
    private static Field drawablesField = null;
    private static Field childrenField = null;
    private static Field selectablesField = null;
    private static Field infoField = null;

    public static void init() {
        if (registered) {
            return;
        }
        registered = true;
        ClientTickEvents.END_CLIENT_TICK.register(client -> onTick());
    }

    private static void onTick() {
        Screen currentScreen = mc.currentScreen;

        if (currentScreen instanceof SelectWorldScreen || currentScreen instanceof MultiplayerScreen) {
            injectAccountButton(currentScreen);
        }

        if (currentScreen instanceof DisconnectedScreen) {
            handleDisconnected((DisconnectedScreen) currentScreen);
        }

        boolean inWorld = mc.world != null;
        if (inWorld && !wasInWorld) {
            onWorldLoad();
        }
        wasInWorld = inWorld;
    }

    private static void injectAccountButton(Screen screen) {
        ButtonWidget button = accountsButtons.get(screen);
        Drawable overlay = usernameOverlays.get(screen);
        if (button == null) {
            try {
                button = ButtonWidget.builder(Text.literal("Accounts"), b -> mc.setScreen(new GuiAccountManager(screen)))
                        .dimensions(screen.width - 106, 6, 100, 20)
                        .build();
                overlay = (context, mouseX, mouseY, delta) -> {
                    String text = TextFormatting.translate(String.format(
                            "&7Username: &3%s&r", SessionManager.get().getUsername()
                    ));
                    context.drawTextWithShadow(mc.textRenderer, text, 3, 3, -1);
                };
                addToScreen(screen, button);
                addDrawable(screen, overlay);
                accountsButtons.put(screen, button);
                usernameOverlays.put(screen, overlay);
            } catch (Exception ignored) {
                //
            }
        } else {
            try {
                List<Drawable> drawables = getDrawables(screen);
                if (!drawables.contains(button)) {
                    addToScreen(screen, button);
                }
                if (!drawables.contains(overlay)) {
                    addDrawable(screen, overlay);
                }
            } catch (Exception ignored) {
                //
            }
        }
    }

    private static void handleDisconnected(DisconnectedScreen screen) {
        if (disconnectedScreens.containsKey(screen)) {
            return;
        }
        disconnectedScreens.put(screen, true);
        try {
            if (infoField == null) {
                infoField = DisconnectedScreen.class.getDeclaredField("info");
                infoField.setAccessible(true);
            }
            DisconnectionInfo info = (DisconnectionInfo) infoField.get(screen);
            String text = info.reason().getString().split("\n\n")[0];
            if (
                    text.equals("§r§cYou are permanently banned from this server!") ||
                            text.equals("§r§cYour account has been blocked.")
            ) {
                AccountManager.load();
                for (Account account : AccountManager.accounts) {
                    if (mc.getSession().getUsername().equals(account.getUsername())) {
                        account.setUnban(-1L);
                    }
                }
                AccountManager.save();
                return;
            }

            if (
                    text.matches("§r§cYou are temporarily banned for §r§f.*§r§c from this server!") ||
                            text.matches("§r§cYour account is temporarily blocked for §r§f.*§r§c from this server!")
            ) {
                String unban = StringUtils.substringBetween(text, "§r§f", "§r§c");
                if (unban != null) {
                    long time = System.currentTimeMillis();
                    for (String duration : unban.split(" ")) {
                        String type = duration.substring(duration.length() - 1);
                        long value = Long.parseLong(duration.substring(0, duration.length() - 1));
                        switch (type) {
                            case "d": {
                                time += value * 86400000L;
                            }
                            break;
                            case "h": {
                                time += value * 3600000L;
                            }
                            break;
                            case "m": {
                                time += value * 60000L;
                            }
                            break;
                            case "s": {
                                time += value * 1000L;
                            }
                            break;
                        }
                    }

                    AccountManager.load();
                    for (Account account : AccountManager.accounts) {
                        if (mc.getSession().getUsername().equals(account.getUsername())) {
                            account.setUnban(time);
                        }
                    }
                    AccountManager.save();
                }
            }
        } catch (Exception e) {
            //
        }
    }

    private static void onWorldLoad() {
        ServerInfo serverInfo = mc.getCurrentServerEntry();
        if (serverInfo != null) {
            String serverIP = serverInfo.address;
            if (serverIP.endsWith("hypixel.net") || serverIP.endsWith("hypixel.io")) {
                AccountManager.load();
                for (Account account : AccountManager.accounts) {
                    if (mc.getSession().getUsername().equals(account.getUsername())) {
                        account.setUnban(0L);
                    }
                }
                AccountManager.save();
            }
        }
    }

    private static List<Drawable> getDrawables(Screen screen) throws Exception {
        if (drawablesField == null) {
            drawablesField = Screen.class.getDeclaredField("drawables");
            drawablesField.setAccessible(true);
        }
        return (List<Drawable>) drawablesField.get(screen);
    }

    private static void addDrawable(Screen screen, Drawable drawable) throws Exception {
        getDrawables(screen).add(drawable);
    }

    private static void addToScreen(Screen screen, ButtonWidget button) throws Exception {
        addDrawable(screen, button);
        if (childrenField == null) {
            childrenField = Screen.class.getDeclaredField("children");
            childrenField.setAccessible(true);
        }
        if (selectablesField == null) {
            selectablesField = Screen.class.getDeclaredField("selectables");
            selectablesField.setAccessible(true);
        }
        ((List<Element>) childrenField.get(screen)).add(button);
        ((List<Selectable>) selectablesField.get(screen)).add(button);
    }
}
