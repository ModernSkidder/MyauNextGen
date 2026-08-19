package me.ksyz.accountmanager.gui;

import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.MicrosoftAuth;
import me.ksyz.accountmanager.auth.SessionManager;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.SystemUtils;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.apache.commons.lang3.RandomStringUtils;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
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
public class GuiMicrosoftAuth extends Screen {
    private final Screen previousScreen;
    private final String state;

    private ButtonWidget openButton = null;
    private boolean openButtonEnabled = true;
    private ButtonWidget cancelButton = null;
    private String status = null;
    private String cause = null;
    private ExecutorService executor = null;
    private CompletableFuture<Void> task = null;
    private boolean success = false;

    public GuiMicrosoftAuth(Screen previousScreen) {
        super(Text.literal("Microsoft Authentication"));
        this.previousScreen = previousScreen;
        this.state = RandomStringUtils.randomAlphanumeric(8);
    }

    @Override
    public void init() {
        this.openButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Open"), button -> actionPerformed(0))
                .dimensions(
                        width / 2 - 75 - 2,
                        height / 2 + textRenderer.fontHeight / 2 + textRenderer.fontHeight,
                        75,
                        20
                )
                .build());
        this.cancelButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> actionPerformed(1))
                .dimensions(
                        width / 2 + 2,
                        height / 2 + textRenderer.fontHeight / 2 + textRenderer.fontHeight,
                        75,
                        20
                )
                .build());

        if (task == null) {
            MicrosoftAuth.CLIENT_ID = "42a60a84-599d-44b2-a7c6-b00cdef1d6a2";
            MicrosoftAuth.SCOPE = "XboxLive.signin XboxLive.offline_access";
            URI url = MicrosoftAuth.getMSAuthLink(state);
            SystemUtils.setClipboard(url != null ? url.toString() : "");
            status = "&fLogin link has been copied to the clipboard!&r";

            if (executor == null) {
                executor = Executors.newSingleThreadExecutor();
            }
            AtomicReference<String> refreshToken = new AtomicReference<>("");
            AtomicReference<String> accessToken = new AtomicReference<>("");
            task = MicrosoftAuth.acquireMSAuthCode(state, executor)
                    .thenComposeAsync(msAuthCode -> {
                        openButtonEnabled = false;
                        status = "&fAcquiring Microsoft access tokens&r";
                        return MicrosoftAuth.acquireMSAccessTokens(msAuthCode, executor);
                    })
                    .thenComposeAsync(msAccessTokens -> {
                        status = "&fAcquiring Xbox access token&r";
                        refreshToken.set(msAccessTokens.get("refresh_token"));
                        return MicrosoftAuth.acquireXboxAccessToken(msAccessTokens.get("access_token"), executor);
                    })
                    .thenComposeAsync(xboxAccessToken -> {
                        status = "&fAcquiring Xbox XSTS token&r";
                        return MicrosoftAuth.acquireXboxXstsToken(xboxAccessToken, executor);
                    })
                    .thenComposeAsync(xboxXstsData -> {
                        status = "&fAcquiring Minecraft access token&r";
                        return MicrosoftAuth.acquireMCAccessToken(
                                xboxXstsData.get("Token"), xboxXstsData.get("uhs"), executor
                        );
                    })
                    .thenComposeAsync(mcToken -> {
                        status = "&fFetching your Minecraft profile&r";
                        accessToken.set(mcToken);
                        return MicrosoftAuth.login(mcToken, executor);
                    })
                    .thenAccept(session -> {
                        status = null;
                        Account acc = new Account(
                                refreshToken.get(), accessToken.get(), session.getUsername(), "42a60a84-599d-44b2-a7c6-b00cdef1d6a2", "XboxLive.signin XboxLive.offline_access"
                        );
                        for (Account account : AccountManager.accounts) {
                            if (acc.getUsername().equals(account.getUsername())) {
                                acc.setUnban(account.getUnban());
                                break;
                            }
                        }
                        AccountManager.accounts.add(acc);
                        AccountManager.save();
                        SessionManager.set(session);
                        success = true;
                    })
                    .exceptionally(error -> {
                        openButtonEnabled = false;
                        status = String.format("&c%s&r", error.getMessage());
                        cause = String.format("&c%s&r", error.getCause().getMessage());
                        return null;
                    });
        }
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
        if (success) {
            client.setScreen(new GuiAccountManager(
                    previousScreen,
                    new Notification(
                            TextFormatting.translate(String.format(
                                    "&aSuccessful login! (%s)&r",
                                    SessionManager.get().getUsername()
                            )),
                            5000L
                    )
            ));
            success = false;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (openButton != null) {
            openButton.active = openButtonEnabled;
        }
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer, "Microsoft Authentication",
                width / 2, height / 2 - textRenderer.fontHeight / 2 - textRenderer.fontHeight * 2, 11184810
        );

        if (status != null) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer, TextFormatting.translate(status),
                    width / 2, height / 2 - textRenderer.fontHeight / 2, -1
            );
        }

        if (cause != null) {
            String causeText = TextFormatting.translate(cause);
            context.fill(
                    0, height - 2 - textRenderer.fontHeight - 3,
                    3 + this.textRenderer.getWidth(causeText) + 3, height,
                    0x64000000
            );
            context.drawTextWithShadow(
                    this.textRenderer, TextFormatting.translate(cause),
                    3, height - 2 - textRenderer.fontHeight, -1
            );
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            actionPerformed(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void actionPerformed(int id) {
        switch (id) {
            case 0: { // Open
                SystemUtils.openWebLink(MicrosoftAuth.getMSAuthLink(state));
            }
            break;
            case 1: { // Cancel
                client.setScreen(new GuiAccountManager(previousScreen));
            }
            break;
        }
    }
}
