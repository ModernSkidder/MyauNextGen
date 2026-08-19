package me.ksyz.accountmanager.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.util.UndashedUuid;
import me.ksyz.accountmanager.auth.SessionManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import org.apache.commons.io.IOUtils;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

/*
 * This file is derived from https://github.com/ksyzov/AccountManager.
 * Originally licensed under the GNU LGPL.
 *
 * This modified version is licensed under the GNU GPL v3.
 */
public class GuiSessionLogin extends Screen {
    private Screen previousScreen;

    private String status = "Session Login";
    private TextFieldWidget sessionField;

    public GuiSessionLogin(Screen previousScreen) {
        super(Text.literal("Session Login"));
        this.previousScreen = previousScreen;
    }

    @Override
    public void init() {
        sessionField = this.addDrawableChild(new TextFieldWidget(
                this.textRenderer, width / 2 - 100, height / 2, 200, 20, Text.literal("")
        ));
        sessionField.setMaxLength(32767);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Login"), button -> actionPerformed(998))
                .dimensions(width / 2 - 100, height / 2 + 30, 200, 20)
                .build());
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(sessionField);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        context.drawTextWithShadow(
                this.textRenderer, status, width / 2 - textRenderer.getWidth(status) / 2, height / 2 - 30, Color.WHITE.getRGB()
        );

        super.render(context, mouseX, mouseY, delta);
    }

    private void actionPerformed(int id) {
        //login button
        if (id == 998) {
            try {
                String username, uuid, token, session = sessionField.getText();

                if (session.contains(":")) { //if fully formatted string (ign:uuid:token)
                    //split string to data
                    username = session.split(":")[0];
                    uuid = session.split(":")[1];
                    token = session.split(":")[2];
                } else { //if only token
                    //make request
                    HttpURLConnection c = (HttpURLConnection) new URL("https://api.minecraftservices.com/minecraft/profile/").openConnection();
                    c.setRequestProperty("Content-type", "application/json");
                    c.setRequestProperty("Authorization", "Bearer " + sessionField.getText());
                    c.setDoOutput(true);

                    //get json
                    JsonObject json = new JsonParser().parse(IOUtils.toString(c.getInputStream())).getAsJsonObject();

                    //get data
                    username = json.get("name").getAsString();
                    uuid = json.get("id").getAsString();
                    token = session;
                }

                SessionManager.set(new Session(username, UndashedUuid.fromStringLenient(uuid), token, Optional.empty(), Optional.empty(), Session.AccountType.MOJANG));
                client.setScreen(previousScreen);
            } catch (IOException IOException) {
                if (IOException.getMessage().contains("401")) {
                    status = "§cError: Invalid session.";
                } else {
                    IOException.printStackTrace();
                }
            } catch (Exception e) {
                status = "§cError: Couldn't set session (check mc logs)";
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (GLFW.GLFW_KEY_ESCAPE == keyCode) {
            client.setScreen(previousScreen);
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return true;
    }
}
