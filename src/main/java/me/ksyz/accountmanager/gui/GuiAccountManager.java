package me.ksyz.accountmanager.gui;

import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.MicrosoftAuth;
import me.ksyz.accountmanager.auth.SessionManager;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/*
 * This file is derived from https://github.com/ksyzov/AccountManager.
 * Originally licensed under the GNU LGPL.
 *
 * This modified version is licensed under the GNU GPL v3.
 */
public class GuiAccountManager extends Screen {
    private final Screen previousScreen;

    private ButtonWidget loginButton = null;
    private ButtonWidget deleteButton = null;
    private ButtonWidget cancelButton = null;
    private GuiAccountList guiAccountList = null;
    private Notification notification = null;
    private int selectedAccount = -1;
    private ExecutorService executor = null;
    private CompletableFuture<Void> task = null;

    public GuiAccountManager(Screen previousScreen) {
        super(Text.literal("Account Manager"));
        this.previousScreen = previousScreen;
    }

    public GuiAccountManager(Screen previousScreen, Notification notification) {
        this(previousScreen);
        this.notification = notification;
    }

    @Override
    public void init() {
        AccountManager.load();

        guiAccountList = new GuiAccountList();
        this.addDrawableChild(guiAccountList);

        this.loginButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Login"), button -> actionPerformed(0)).dimensions(width / 2 - 150, height - 52, 95, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add"), button -> actionPerformed(1)).dimensions(width / 2 - 50, height - 52, 95, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Session"), button -> actionPerformed(4)).dimensions(width / 2 + 50, height - 52, 95, 20).build());

        this.deleteButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), button -> actionPerformed(2)).dimensions(width / 2 - 150, height - 28, 95, 20).build());
        this.cancelButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> actionPerformed(3)).dimensions(width / 2 + 50, height - 28, 95, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add Token"), button -> actionPerformed(5)).dimensions(width / 2 - 50, height - 28, 95, 20).build());

        this.tick();
    }

    @Override
    public void removed() {
        if (task != null && !task.isDone()) {
            task.cancel(true);
            executor.shutdownNow();
        }
    }

    @Override
    public void tick() {
        if (loginButton != null && deleteButton != null) {
            loginButton.active = deleteButton.active = selectedAccount >= 0;
            if (task != null && !task.isDone()) {
                loginButton.active = false;
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                TextFormatting.translate(String.format(
                        "&rAccount Manager &8(&7%s&8)&r", AccountManager.accounts.size()
                )),
                width / 2, 20, -1
        );

        String text = TextFormatting.translate(String.format(
                "&7Username: &3%s&r", SessionManager.get().getUsername()
        ));
        context.drawTextWithShadow(this.textRenderer, text, 3, 3, -1);

        if (notification != null && !notification.isExpired()) {
            String notificationText = notification.getMessage();
            context.fill(
                    width / 2 - this.textRenderer.getWidth(notificationText) / 2 - 3, 4,
                    width / 2 + this.textRenderer.getWidth(notificationText) / 2 + 3, 4 + 3 + this.textRenderer.fontHeight + 2,
                    0x64000000
            );
            context.drawCenteredTextWithShadow(
                    this.textRenderer, notification.getMessage(),
                    width / 2, 4 + 3, -1
            );
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_UP: {
                if (selectedAccount > 0) {
                    --selectedAccount;
                    if (hasControlDown()) {
                        Collections.swap(AccountManager.accounts, selectedAccount, selectedAccount + 1);
                        AccountManager.save();
                    }
                }
            }
            return true;
            case GLFW.GLFW_KEY_DOWN: {
                if (selectedAccount < AccountManager.accounts.size() - 1) {
                    ++selectedAccount;
                    if (hasControlDown()) {
                        Collections.swap(AccountManager.accounts, selectedAccount, selectedAccount - 1);
                        AccountManager.save();
                    }
                }
            }
            return true;
            case GLFW.GLFW_KEY_ENTER: {
                if (loginButton != null && loginButton.active) {
                    actionPerformed(0);
                }
            }
            return true;
            case GLFW.GLFW_KEY_DELETE: {
                if (deleteButton != null && deleteButton.active) {
                    actionPerformed(2);
                }
            }
            return true;
            case GLFW.GLFW_KEY_ESCAPE: {
                actionPerformed(3);
            }
            return true;
        }

        if (isCopy(keyCode) && selectedAccount >= 0) {
            client.keyboard.setClipboard(AccountManager.accounts.get(selectedAccount).getUsername());
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void actionPerformed(int id) {
        switch (id) {
            case 0: { // Login
                if (task == null || task.isDone()) {
                    if (executor == null) {
                        executor = Executors.newSingleThreadExecutor();
                    }
                    Account account = AccountManager.accounts.get(selectedAccount);
                    String username = StringUtils.isBlank(account.getUsername()) ? "???" : account.getUsername();
                    AtomicReference<String> refreshToken = new AtomicReference<>("");
                    AtomicReference<String> accessToken = new AtomicReference<>("");
                    notification = new Notification(TextFormatting.translate(String.format(
                            "&7Fetching your Minecraft profile... (%s)&r", username
                    )), -1L);
                    MicrosoftAuth.CLIENT_ID = account.getClientId();
                    MicrosoftAuth.SCOPE = account.getScope();
                    task = MicrosoftAuth.login(account.getAccessToken(), executor)
                            .handle((session, error) -> {
                                if (session != null) {
                                    account.setUsername(session.getUsername());
                                    AccountManager.save();
                                    SessionManager.set(session);
                                    notification = new Notification(TextFormatting.translate(String.format(
                                            "&aSuccessful login! (%s)&r", account.getUsername()
                                    )), 5000L);
                                    return true;
                                }
                                return false;
                            })
                            .thenComposeAsync(completed -> {
                                if (completed) {
                                    throw new NoSuchElementException();
                                }
                                notification = new Notification(TextFormatting.translate(String.format(
                                        "&7Refreshing Microsoft access tokens... (%s)&r", username
                                )), -1L);
                                return MicrosoftAuth.refreshMSAccessTokens(account.getRefreshToken(), executor);
                            })
                            .thenComposeAsync(msAccessTokens -> {
                                notification = new Notification(TextFormatting.translate(String.format(
                                        "&7Acquiring Xbox access token... (%s)&r", username
                                )), -1L);
                                refreshToken.set(msAccessTokens.get("refresh_token"));
                                return MicrosoftAuth.acquireXboxAccessToken(msAccessTokens.get("access_token"), executor);
                            })
                            .thenComposeAsync(xboxAccessToken -> {
                                notification = new Notification(TextFormatting.translate(String.format(
                                        "&7Acquiring Xbox XSTS token... (%s)&r", username
                                )), -1L);
                                return MicrosoftAuth.acquireXboxXstsToken(xboxAccessToken, executor);
                            })
                            .thenComposeAsync(xboxXstsData -> {
                                notification = new Notification(TextFormatting.translate(String.format(
                                        "&7Acquiring Minecraft access token... (%s)&r", username
                                )), -1L);
                                return MicrosoftAuth.acquireMCAccessToken(
                                        xboxXstsData.get("Token"), xboxXstsData.get("uhs"), executor
                                );
                            })
                            .thenComposeAsync(mcToken -> {
                                notification = new Notification(TextFormatting.translate(String.format(
                                        "&7Fetching your Minecraft profile... (%s)&r", username
                                )), -1L);
                                accessToken.set(mcToken);
                                return MicrosoftAuth.login(mcToken, executor);
                            })
                            .thenAccept(session -> {
                                account.setRefreshToken(refreshToken.get());
                                account.setAccessToken(accessToken.get());
                                account.setUsername(session.getUsername());
                                AccountManager.save();
                                SessionManager.set(session);
                                notification = new Notification(TextFormatting.translate(String.format(
                                        "&aSuccessful login! (%s)&r", account.getUsername()
                                )), 5000L);
                            })
                            .exceptionally(error -> {
                                if (!(error.getCause() instanceof NoSuchElementException)) {
                                    notification = new Notification(TextFormatting.translate(String.format(
                                            "&c%s (%s)&r", error.getMessage(), username
                                    )), 5000L);
                                }
                                return null;
                            });
                }
            }
            break;
            case 1: { // Add
                client.setScreen(new GuiMicrosoftAuth(previousScreen));
            }
            break;
            case 2: { // Delete
                if (selectedAccount > -1 && selectedAccount < AccountManager.accounts.size()) {
                    AccountManager.accounts.remove(selectedAccount);
                    AccountManager.save();
                    selectedAccount = -1;
                    tick();
                }
            }
            break;
            case 3: { // Cancel
                client.setScreen(previousScreen);
            }
            break;
            case 4: { // Session
                client.setScreen(new GuiSessionLogin(this));
            }
            break;
            case 5: { // Add Token
                client.setScreen(new GuiAddToken(this));
            }
            break;
        }
    }

    class GuiAccountList extends ClickableWidget {
        private final int slotHeight = 16;
        private int scrollAmount = 0;
        private long lastClickTime = 0;

        public GuiAccountList() {
            super(
                    GuiAccountManager.this.width / 2 - (150 + 4) * 2 / 2,
                    32,
                    (150 + 4) * 2,
                    GuiAccountManager.this.height - 64 - 32,
                    Text.literal("")
            );
        }

        protected int getListWidth() {
            return (150 + 4) * 2;
        }

        protected int getScrollBarX() {
            return (GuiAccountManager.this.width + getListWidth()) / 2 + 2;
        }

        protected int getContentHeight() {
            return AccountManager.accounts.size() * slotHeight;
        }

        private int getMaxScroll() {
            return Math.max(0, getContentHeight() - getHeight());
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int x = getX();
            int y = getY();
            context.fill(x - 2, y - 2, x + getListWidth() + 2, y + getHeight() + 2, 0xFF000000);
            context.fill(x - 1, y - 1, x + getListWidth() + 1, y + getHeight() + 1, 0x80282828);
            for (int i = 0; i < AccountManager.accounts.size(); i++) {
                int slotY = y + i * slotHeight - scrollAmount;
                if (slotY + slotHeight < y || slotY > y + getHeight()) {
                    continue;
                }
                if (i == GuiAccountManager.this.selectedAccount) {
                    context.fill(x - 2, slotY - 2, x + getListWidth() + 2, slotY + slotHeight - 2, 0xFF808080);
                    context.fill(x - 1, slotY - 1, x + getListWidth() + 1, slotY + slotHeight - 1, 0xFF2C2C2C);
                }
                drawSlot(context, i, x, slotY, slotHeight, mouseX, mouseY);
            }
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isMouseOver(mouseX, mouseY)) {
                int slotIndex = (int) ((mouseY - getY() + scrollAmount) / slotHeight);
                if (slotIndex >= 0 && slotIndex < AccountManager.accounts.size()) {
                    boolean isDoubleClick = System.currentTimeMillis() - lastClickTime < 250;
                    lastClickTime = System.currentTimeMillis();
                    elementClicked(slotIndex, isDoubleClick, (int) mouseX, (int) mouseY);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            scrollAmount = MathHelper.clamp((int) (scrollAmount - verticalAmount * 10), 0, getMaxScroll());
            return true;
        }

        protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
            GuiAccountManager.this.selectedAccount = slotIndex;
            GuiAccountManager.this.tick();
            if (isDoubleClick) {
                if (loginButton != null && loginButton.active) {
                    GuiAccountManager.this.actionPerformed(0);
                }
            }
        }

        protected void drawSlot(DrawContext context, int entryID, int x, int y, int k, int mouseXIn, int mouseYIn) {
            TextRenderer fr = GuiAccountManager.this.textRenderer;
            Account account = AccountManager.accounts.get(entryID);

            String username = account.getUsername();
            if (StringUtils.isBlank(username)) {
                username = "&7&l?";
            } else if (account.getAccessToken().equals(SessionManager.get().getAccessToken())) {
                username = String.format("&a&l%s", username);
            } else if (username.equals(SessionManager.get().getUsername())) {
                username = String.format("&a%s", username);
            }
            username = TextFormatting.translate(
                    String.format("&r%s&r", username)
            );
            context.drawTextWithShadow(fr, username, x + 2, y + 2, -1);

            long currentTime = System.currentTimeMillis();
            long unbanTime = account.getUnban();
            String unban;
            if (unbanTime < 0L) {
                unban = "&4&l⚠";
            } else if (unbanTime <= currentTime) {
                unban = "&2&l✔";
            } else {
                long diff = unbanTime - currentTime;
                long s = (diff / 1000L) % 60L;
                long m = (diff / 60000L) % 60L;
                long h = (diff / 3600000L) % 24L;
                long d = (diff / 86400000L);
                unban = String.format(
                        "%s%s%s%s",
                        d > 0L ? String.format("%dd", d) : "",
                        h > 0L ? String.format(" %dh", h) : "",
                        m > 0L ? String.format(" %dm", m) : "",
                        s > 0L ? String.format(" %ds", s) : ""
                );
                unban = unban.trim();
                unban = String.format("%s &c&l⚠", unban);
            }
            unban = TextFormatting.translate(
                    String.format("&r%s&r", unban)
            );
            context.drawTextWithShadow(
                    fr, unban, x + getListWidth() - 5 - fr.getWidth(unban), y + 2, -1
            );
        }
    }
}
