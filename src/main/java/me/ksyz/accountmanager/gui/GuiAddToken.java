package me.ksyz.accountmanager.gui;

import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.MicrosoftAuth;
import me.ksyz.accountmanager.auth.SessionManager;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.apache.commons.lang3.RandomStringUtils;
import org.lwjgl.glfw.GLFW;

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
public class GuiAddToken extends Screen {
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
    private TextFieldWidget tokenField;

    public GuiAddToken(Screen previousScreen) {
        super(Text.literal("Add Token"));
        this.previousScreen = previousScreen;
        this.state = RandomStringUtils.randomAlphanumeric(8);
    }

    @Override
    public void init() {
        tokenField = this.addDrawableChild(new TextFieldWidget(
                this.textRenderer, width / 2 - 100, height / 2, 200, 20, Text.literal("")
        ));
        tokenField.setMaxLength(32767);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add"), button -> actionPerformed(998))
                .dimensions(width / 2 - 100, height / 2 + 30, 200, 20)
                .build());
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(tokenField);
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
                this.textRenderer, "Add Token",
                width / 2, height / 2 - textRenderer.fontHeight / 2 - textRenderer.fontHeight * 2 - 14, 11184810
        );

        if (status != null) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer, TextFormatting.translate(status),
                    width / 2, height / 2 - textRenderer.fontHeight / 2 - 14, -1
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
            if (task == null || task.isDone() || task.isCancelled() || task.isCompletedExceptionally()) {
                client.setScreen(previousScreen);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void actionPerformed(int id) {
        if (id == 998) {
            if (task == null) {
                if (executor == null) {
                    executor = Executors.newSingleThreadExecutor();
                }
                AtomicReference<String> refreshToken = new AtomicReference<>("");
                AtomicReference<String> accessToken = new AtomicReference<>("");
                MicrosoftAuth.CLIENT_ID = "00000000402b5328";
                MicrosoftAuth.SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
                task = MicrosoftAuth.login(tokenField.getText(), executor)
                        .handle((session, error) -> session != null)
                        .thenComposeAsync(completed -> {
                            if (completed) {
                                throw new NoSuchElementException();
                            }
                            status = "&7Refreshing Microsoft access tokens...&r";
                            return MicrosoftAuth.refreshMSAccessTokens(tokenField.getText(), executor);
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
                                    refreshToken.get(), accessToken.get(), session.getUsername(), "00000000402b5328", "service::user.auth.xboxlive.com::MBI_SSL"
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
                            task.cancel(true);
                            task = null;
                            return null;
                        });
            }
        }
    }
}
