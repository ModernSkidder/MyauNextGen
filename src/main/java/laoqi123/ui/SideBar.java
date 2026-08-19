package laoqi123.ui;

import laoqi123.module.Category;
import laoqi123.ui.animations.Animation;
import laoqi123.ui.animations.DummyAnimation;
import laoqi123.ui.animations.EaseOutExpo;
import laoqi123.ui.elements.BasicButton;
import laoqi123.ui.renderer.NanoVGRenderUtil;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static laoqi123.ui.elements.BasicButton.ALIGNMENT_LEFT;
import static laoqi123.ui.elements.BasicButton.SIZE_36;

public class SideBar {
    private final List<BasicButton> buttons = new ArrayList<>();
    private final BasicButton closeButton = new BasicButton(192, SIZE_36, "Close", 4, 0, ALIGNMENT_LEFT, ColorPalette.TERTIARY_DESTRUCTIVE);
    private int selected = 0;
    private Animation moveAnimation;
    private Animation sizeAnimation;
    private int y;
    private Consumer<Category> categoryCallback;

    public SideBar() {
        int width = 192;
        Category[] categories = Category.values();
        int[] icons = {10, 11, 12, 13, 14};
        for (int i = 0; i < categories.length; i++) {
            int index = i;
            BasicButton button = new BasicButton(width, SIZE_36, categories[i].getName(), icons[i], 0, ALIGNMENT_LEFT, i == 0 ? ColorPalette.PRIMARY : ColorPalette.TERTIARY);
            button.setClickAction(() -> {
                if (categoryCallback != null) categoryCallback.accept(categories[index]);
            });
            buttons.add(button);
        }
        closeButton.setClickAction(() -> MinecraftClient.getInstance().setScreen(null));
    }

    public void setCategoryCallback(Consumer<Category> callback) {
        this.categoryCallback = callback;
    }

    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        this.y = y;
        for (BasicButton button : buttons) {
            if (!button.isClicked()) continue;
            moveSideBar(button);
            break;
        }
        if (moveAnimation != null) {
            NanoVGRenderUtil.drawRoundedRect(vg, x + 16, y + moveAnimation.get() - (sizeAnimation.get() - 36) / 2f, 192, sizeAnimation.get(0), Colors.PRIMARY_600, 12);
            if (moveAnimation.isFinished() && sizeAnimation.isFinished()) {
                moveAnimation = null;
                sizeAnimation = null;
                buttons.get(selected).setColorPalette(ColorPalette.PRIMARY);
            }
        }
        int sidebarY = y + 80;
        for (int i = 0; i < buttons.size(); i++) {
            BasicButton button = buttons.get(i);
            button.draw(vg, x + 16, sidebarY, inputHandler);
            sidebarY += 36;
        }
        closeButton.draw(vg, x + 16, y + 704, inputHandler);
    }

    public void pageOpened(String page) {
        for (BasicButton button : buttons) {
            if (!button.getText().equalsIgnoreCase(page)) continue;
            moveSideBar(button);
            return;
        }
    }

    private void moveSideBar(BasicButton button) {
        if (button.equals(buttons.get(selected))) return;
        buttons.get(selected).setColorPalette(ColorPalette.TERTIARY);
        moveAnimation = new EaseOutExpo(300, buttons.get(selected).y - y, button.y - y, false);
        sizeAnimation = new DummyAnimation(36);
        selected = buttons.indexOf(button);
    }
}
