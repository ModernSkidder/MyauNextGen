package laoqi123.ui.callback;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class GuiInput extends Screen {
    private final String title;
    private final String defaultValue;
    private final Consumer<String> callback;
    private final Screen caller;
    private TextFieldWidget textField;
    private ButtonWidget buttonOk;

    public GuiInput(String title, String defaultValue, Consumer<String> callback, Screen caller) {
        super(Text.literal(title));
        this.title = title;
        this.defaultValue = defaultValue;
        this.callback = callback;
        this.caller = caller;
    }

    public static void prompt(String title, String defaultValue, Consumer<String> callback, Screen caller) {
        MinecraftClient.getInstance().setScreen(new GuiInput(title, defaultValue, callback, caller));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        textField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 10, 200, 20, Text.literal(""));
        textField.setText(defaultValue);
        textField.setFocused(true);
        this.addSelectableChild(textField);

        buttonOk = ButtonWidget.builder(Text.literal("Confirm"), button -> this.onConfirm()).dimensions(centerX - 100, centerY + 20, 95, 20).build();
        this.addDrawableChild(buttonOk);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> this.onCancel()).dimensions(centerX + 5, centerY + 20, 95, 20).build());
    }

    private void onConfirm() {
        if (callback != null) callback.accept(textField.getText());
        this.close();
    }

    private void onCancel() {
        this.close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.onConfirm();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onCancel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 35, 0xFFFFFF);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(caller);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
