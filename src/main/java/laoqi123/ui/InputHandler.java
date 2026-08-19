package laoqi123.ui;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class InputHandler {
    private float mouseX;
    private float mouseY;
    private int mouseXInt;
    private int mouseYInt;
    private boolean mouseDown;
    private boolean mouseWasDown;
    private boolean clicked;
    private final boolean[] buttonsDown = new boolean[3];
    private final boolean[] buttonsWasDown = new boolean[3];
    private final boolean[] buttonsClicked = new boolean[3];
    private double dWheel;
    private boolean blockingAllInput;
    private boolean blockingDWheel;
    private boolean pageTransition;
    private float contentX;
    private float contentY;
    private float contentW;
    private float contentH;

    public void setPageTransition(boolean pageTransition) {
        this.pageTransition = pageTransition;
    }

    public void setContentArea(float x, float y, float w, float h) {
        this.contentX = x;
        this.contentY = y;
        this.contentW = w;
        this.contentH = h;
    }

    private boolean isInContentArea() {
        return mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= contentY && mouseY <= contentY + contentH;
    }

    public void update(float mouseX, float mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.mouseXInt = (int) mouseX;
        this.mouseYInt = (int) mouseY;
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        for (int b = 0; b < 3; b++) {
            this.buttonsWasDown[b] = this.buttonsDown[b];
            this.buttonsDown[b] = GLFW.glfwGetMouseButton(handle, b) == GLFW.GLFW_PRESS;
            this.buttonsClicked[b] = this.buttonsDown[b] && !this.buttonsWasDown[b];
        }
        this.mouseWasDown = this.buttonsWasDown[0];
        this.mouseDown = this.buttonsDown[0];
        this.clicked = this.buttonsClicked[0];
    }

    public void scale(float scaleX, float scaleY) {
        this.mouseX /= scaleX;
        this.mouseY /= scaleY;
        this.mouseXInt = (int) this.mouseX;
        this.mouseYInt = (int) this.mouseY;
    }

    public void addScroll(double amount) {
        this.dWheel += amount;
    }

    public double getDWheel() {
        return getDWheel(false);
    }

    public double getDWheel(boolean unblock) {
        if (blockingDWheel && !unblock) return 0;
        double wheel = dWheel;
        dWheel = 0;
        return wheel;
    }

    public void blockDWheel() {
        blockingDWheel = true;
    }

    public void unblockDWheel() {
        blockingDWheel = false;
    }

    public boolean isButtonDown(int button) {
        if (button < 0 || button >= 3) {
            long handle = MinecraftClient.getInstance().getWindow().getHandle();
            return GLFW.glfwGetMouseButton(handle, button) == GLFW.GLFW_PRESS;
        }
        return buttonsDown[button];
    }

    public boolean isClicked(int button) {
        if (button < 0 || button >= 3) return false;
        return buttonsClicked[button] && !blockingAllInput && !(pageTransition && isInContentArea());
    }

    public boolean isBlockingInput() {
        return blockingAllInput;
    }

    public void blockAllInput() {
        blockingAllInput = true;
    }

    public void stopBlockingInput() {
        blockingAllInput = false;
    }

    public boolean isClicked() {
        return isClicked(false);
    }

    public boolean isClicked(boolean unblock) {
        return clicked && (!blockingAllInput || unblock) && !(pageTransition && isInContentArea());
    }

    public boolean isAreaHovered(float x, float y, float width, float height) {
        return isAreaHovered(x, y, width, height, false);
    }

    public boolean isAreaHovered(float x, float y, float width, float height, boolean ignoreBlocked) {
        return (!blockingAllInput || ignoreBlocked) && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isAreaClicked(float x, float y, float width, float height) {
        return isAreaClicked(x, y, width, height, false);
    }

    public boolean isAreaClicked(float x, float y, float width, float height, boolean ignoreBlocked) {
        return isClicked(ignoreBlocked) && isAreaHovered(x, y, width, height, ignoreBlocked);
    }

    public float mouseX() {
        return mouseX;
    }

    public float mouseY() {
        return mouseY;
    }

    public int getMouseX() {
        return mouseXInt;
    }

    public int getMouseY() {
        return mouseYInt;
    }

    public boolean isMouseDown() {
        return mouseDown;
    }

    public boolean isMouseWasDown() {
        return mouseWasDown;
    }
}
