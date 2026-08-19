package laoqi123.ui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import laoqi123.Myau;
import laoqi123.font.UFontRenderer;
import laoqi123.module.Module;
import laoqi123.module.modules.combat.*;
import laoqi123.module.modules.misc.*;
import laoqi123.module.modules.movement.*;
import laoqi123.module.modules.player.*;
import laoqi123.module.modules.player.Timer;
import laoqi123.module.modules.render.*;
import laoqi123.ui.components.BindComponent;
import laoqi123.ui.components.CategoryComponent;
import laoqi123.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ClickGui extends Screen {
    private static ClickGui instance;
    private static int lastMouseX;
    private static int lastMouseY;

    private final File configFile = new File("./config/Myau/", "clickgui.txt");
    private final ArrayList<CategoryComponent> categoryList = new ArrayList<>();

    private UFontRenderer currentRenderer;
    private int selectedCategory = 0;
    private int hoveredCategory = -1;
    private final StringBuilder searchQuery = new StringBuilder();
    private boolean searchFocused = false;

    public ClickGui() {
        super(Text.empty());
        instance = this;

        List<Module> combatModules = new ArrayList<>();
        combatModules.add(Myau.moduleManager.getModule(AimAssist.class));
        combatModules.add(Myau.moduleManager.getModule(AutoClicker.class));
        combatModules.add(Myau.moduleManager.getModule(AutoProjectiles.class));
        combatModules.add(Myau.moduleManager.getModule(KillAura.class));
        combatModules.add(Myau.moduleManager.getModule(Wtap.class));
        combatModules.add(Myau.moduleManager.getModule(Velocity.class));
        combatModules.add(Myau.moduleManager.getModule(RiseVelocity.class));
        combatModules.add(Myau.moduleManager.getModule(Reach.class));
        combatModules.add(Myau.moduleManager.getModule(TargetStrafe.class));
        combatModules.add(Myau.moduleManager.getModule(NoHitDelay.class));
        combatModules.add(Myau.moduleManager.getModule(AntiFireball.class));
        combatModules.add(Myau.moduleManager.getModule(KnockbackDelay.class));
        combatModules.add(Myau.moduleManager.getModule(LagRange.class));
        combatModules.add(Myau.moduleManager.getModule(HitBox.class));
        combatModules.add(Myau.moduleManager.getModule(MoreKB.class));
        combatModules.add(Myau.moduleManager.getModule(HitSelect.class));
        combatModules.add(Myau.moduleManager.getModule(BackTrack.class));
        combatModules.add(Myau.moduleManager.getModule(BlockHit.class));
        combatModules.add(Myau.moduleManager.getModule(OldHitting.class));
        combatModules.add(Myau.moduleManager.getModule(Teams.class));

        List<Module> movementModules = new ArrayList<>();
        movementModules.add(Myau.moduleManager.getModule(AntiAFK.class));
        movementModules.add(Myau.moduleManager.getModule(Fly.class));
        movementModules.add(Myau.moduleManager.getModule(Freeze.class));
        movementModules.add(Myau.moduleManager.getModule(Speed.class));
        movementModules.add(Myau.moduleManager.getModule(LongJump.class));
        movementModules.add(Myau.moduleManager.getModule(Sprint.class));
        movementModules.add(Myau.moduleManager.getModule(Jesus.class));
        movementModules.add(Myau.moduleManager.getModule(Blink.class));
        movementModules.add(Myau.moduleManager.getModule(NoFall.class));
        movementModules.add(Myau.moduleManager.getModule(NoSlow.class));
        movementModules.add(Myau.moduleManager.getModule(KeepSprint.class));
        movementModules.add(Myau.moduleManager.getModule(Eagle.class));
        movementModules.add(Myau.moduleManager.getModule(NoJumpDelay.class));
        movementModules.add(Myau.moduleManager.getModule(AntiVoid.class));
        movementModules.add(Myau.moduleManager.getModule(Stasis.class));
        movementModules.add(Myau.moduleManager.getModule(Stuck.class));

        List<Module> renderModules = new ArrayList<>();
        renderModules.add(Myau.moduleManager.getModule(Animations.class));
        renderModules.add(Myau.moduleManager.getModule(ESP.class));
        renderModules.add(Myau.moduleManager.getModule(Chams.class));
        renderModules.add(Myau.moduleManager.getModule(FullBright.class));
        renderModules.add(Myau.moduleManager.getModule(Tracers.class));
        renderModules.add(Myau.moduleManager.getModule(NameTags.class));
        renderModules.add(Myau.moduleManager.getModule(Xray.class));
        renderModules.add(Myau.moduleManager.getModule(TargetHUD.class));
        renderModules.add(Myau.moduleManager.getModule(TargetHud2.class));
        renderModules.add(Myau.moduleManager.getModule(Indicators.class));
        renderModules.add(Myau.moduleManager.getModule(BedESP.class));
        renderModules.add(Myau.moduleManager.getModule(ItemESP.class));
        renderModules.add(Myau.moduleManager.getModule(PotionEffects.class));
        renderModules.add(Myau.moduleManager.getModule(ViewClip.class));
        renderModules.add(Myau.moduleManager.getModule(NoHurtCam.class));
        renderModules.add(Myau.moduleManager.getModule(HUD.class));
        renderModules.add(Myau.moduleManager.getModule(GuiModule.class));
        renderModules.add(Myau.moduleManager.getModule(ChestESP.class));
        renderModules.add(Myau.moduleManager.getModule(Trajectories.class));
        renderModules.add(Myau.moduleManager.getModule(Notifications.class));
        renderModules.add(Myau.moduleManager.getModule(WaterMark.class));
        renderModules.add(Myau.moduleManager.getModule(TabGui.class));
        renderModules.add(Myau.moduleManager.getModule(BedPlates.class));

        List<Module> playerModules = new ArrayList<>();
        playerModules.add(Myau.moduleManager.getModule(AutoHeal.class));
        playerModules.add(Myau.moduleManager.getModule(AutoMLG.class));
        playerModules.add(Myau.moduleManager.getModule(AutoTool.class));
        playerModules.add(Myau.moduleManager.getModule(AutoSwap.class));
        playerModules.add(Myau.moduleManager.getModule(ChestAura.class));
        playerModules.add(Myau.moduleManager.getModule(ChestStealer.class));
        playerModules.add(Myau.moduleManager.getModule(FakeLag.class));
        playerModules.add(Myau.moduleManager.getModule(InvManager.class));
        playerModules.add(Myau.moduleManager.getModule(InvWalk.class));
        playerModules.add(Myau.moduleManager.getModule(Scaffold.class));
        playerModules.add(Myau.moduleManager.getModule(Scaffold2.class));
        playerModules.add(Myau.moduleManager.getModule(Telly.class));
        playerModules.add(Myau.moduleManager.getModule(AutoBlockIn.class));
        playerModules.add(Myau.moduleManager.getModule(SpeedMine.class));
        playerModules.add(Myau.moduleManager.getModule(FastPlace.class));
        playerModules.add(Myau.moduleManager.getModule(GhostHand.class));
        playerModules.add(Myau.moduleManager.getModule(MCF.class));
        playerModules.add(Myau.moduleManager.getModule(AntiDebuff.class));
        playerModules.add(Myau.moduleManager.getModule(Timer.class));

        List<Module> miscModules = new ArrayList<>();
        miscModules.add(Myau.moduleManager.getModule(Spammer.class));
        miscModules.add(Myau.moduleManager.getModule(BedNuker.class));
        miscModules.add(Myau.moduleManager.getModule(BedTracker.class));
        miscModules.add(Myau.moduleManager.getModule(LightningTracker.class));
        miscModules.add(Myau.moduleManager.getModule(NoRotate.class));
        miscModules.add(Myau.moduleManager.getModule(NickHider.class));
        miscModules.add(Myau.moduleManager.getModule(AntiObbyTrap.class));
        miscModules.add(Myau.moduleManager.getModule(AntiObfuscate.class));
        miscModules.add(Myau.moduleManager.getModule(AutoAnduril.class));
        miscModules.add(Myau.moduleManager.getModule(InventoryClicker.class));
        miscModules.add(Myau.moduleManager.getModule(ClientSpoofer.class));
        miscModules.add(Myau.moduleManager.getModule(FlagDetector.class));
        miscModules.add(Myau.moduleManager.getModule(AntiStaff.class));

        Comparator<Module> comparator = Comparator.comparing(m -> m.getName().toLowerCase());
        combatModules.sort(comparator);
        movementModules.sort(comparator);
        renderModules.sort(comparator);
        playerModules.sort(comparator);
        miscModules.sort(comparator);

        Set<Module> registered = new HashSet<>();
        registered.addAll(combatModules);
        registered.addAll(movementModules);
        registered.addAll(renderModules);
        registered.addAll(playerModules);
        registered.addAll(miscModules);

        for (Module module : Myau.moduleManager.modules.values()) {
            if (!registered.contains(module)) {
                throw new RuntimeException(module.getClass().getName() + " is unregistered to click gui.");
            }
        }

        this.categoryList.add(new CategoryComponent("Combat", combatModules));
        this.categoryList.add(new CategoryComponent("Movement", movementModules));
        this.categoryList.add(new CategoryComponent("Render", renderModules));
        this.categoryList.add(new CategoryComponent("Player", playerModules));
        this.categoryList.add(new CategoryComponent("Misc", miscModules));

        loadPositions();
        for (CategoryComponent cat : categoryList) {
            cat.updateAllOffsets();
        }
    }

    public static ClickGui getInstance() {
        return instance;
    }

    public static int getLastMouseX() {
        return lastMouseX;
    }

    public static int getLastMouseY() {
        return lastMouseY;
    }

    public UFontRenderer getCurrentRenderer() {
        return currentRenderer;
    }

    private boolean isModernFontEnabled() {
        return getCurrentRenderer() != null;
    }

    public static int getFontHeight() {
        ClickGui gui = getInstance();
        if (gui != null && gui.isModernFontEnabled()) {
            return gui.getCurrentRenderer().FONT_HEIGHT;
        }
        return MinecraftClient.getInstance().textRenderer.fontHeight;
    }

    public static int getStringWidth(DrawContext context, String text) {
        ClickGui gui = getInstance();
        if (gui != null && gui.isModernFontEnabled()) {
            return gui.getCurrentRenderer().getStringWidth(text);
        }
        return MinecraftClient.getInstance().textRenderer.getWidth(text);
    }

    public static void drawString(DrawContext context, String text, float x, float y, int color, boolean shadow) {
        ClickGui gui = getInstance();
        if (gui != null && gui.isModernFontEnabled()) {
            gui.getCurrentRenderer().drawString(text, x, y, color, shadow);
            return;
        }
        context.drawText(MinecraftClient.getInstance().textRenderer, text, Math.round(x), Math.round(y), color, shadow);
    }

    public static void drawStringWithShadow(DrawContext context, String text, float x, float y, int color) {
        drawString(context, text, x, y, color, true);
    }

    private UFontRenderer getFontRenderer() {
        return ((HUD) Myau.moduleManager.getModule(HUD.class)).getModernFontRenderer();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        context.fill(0, 0, this.width, this.height, Gnome.WINDOW_BG);

        currentRenderer = getFontRenderer();

        this.drawHeader(context);
        this.drawSidebar(context);

        CategoryComponent category = this.categoryList.get(this.selectedCategory);
        category.setX(Gnome.SIDEBAR_WIDTH + Gnome.CONTENT_PAD);
        category.setY(Gnome.HEADER_HEIGHT + Gnome.CONTENT_PAD);
        category.setSearchQuery(this.searchQuery.toString());
        category.render(context);

        for (Component module : category.getModules()) {
            module.update(mouseX, mouseY);
        }
    }

    private void drawHeader(DrawContext context) {
        int height = this.height;
        RenderUtil.drawRect(0.0F, 0.0F, (float) this.width, Gnome.HEADER_HEIGHT, Gnome.HEADER_BG);
        RenderUtil.drawRect(0.0F, Gnome.HEADER_HEIGHT, (float) this.width, Gnome.HEADER_HEIGHT + 1, Gnome.SIDEBAR_BORDER);

        drawString(context, "dev, laoqi123", 16.0F, (float) ((Gnome.HEADER_HEIGHT - ClickGui.getFontHeight()) / 2), Gnome.TEXT_PRIMARY, false);

        int searchX = this.width - 270;
        int searchY = (Gnome.HEADER_HEIGHT - 26) / 2;
        RenderUtil.drawRoundedRect(searchX, searchY, 250, 26, 8, Gnome.ENTRY_BG);
        if (this.searchFocused) {
            RenderUtil.drawRoundedRectOutline(searchX, searchY, 250, 26, 8, 1.0F, Gnome.ACCENT, true, true, true, true);
        }
        if (this.searchQuery.length() > 0) {
            drawString(context, this.searchQuery.toString(), searchX + 10, (float) (searchY + (26 - ClickGui.getFontHeight()) / 2), Gnome.TEXT_PRIMARY, false);
        } else {
            drawString(context, "Search", searchX + 10, (float) (searchY + (26 - ClickGui.getFontHeight()) / 2), Gnome.TEXT_DISABLED, false);
        }
    }

    private void drawSidebar(DrawContext context) {
        RenderUtil.drawRect(0.0F, Gnome.HEADER_HEIGHT, Gnome.SIDEBAR_WIDTH, (float) this.height, Gnome.SIDEBAR_BG);
        RenderUtil.drawRect(Gnome.SIDEBAR_WIDTH - 1, Gnome.HEADER_HEIGHT, Gnome.SIDEBAR_WIDTH, (float) this.height, Gnome.SIDEBAR_BORDER);

        this.hoveredCategory = -1;
        for (int i = 0; i < this.categoryList.size(); i++) {
            int y = Gnome.HEADER_HEIGHT + 12 + i * 44;
            boolean hovered = lastMouseX < Gnome.SIDEBAR_WIDTH && lastMouseY >= y && lastMouseY <= y + 36;
            if (hovered) {
                this.hoveredCategory = i;
            }
            if (i == this.selectedCategory) {
                RenderUtil.drawRoundedRect(10, y, 200, 36, 8, Gnome.ACCENT);
                drawString(context, this.categoryList.get(i).getName(), 24.0F, (float) (y + (36 - ClickGui.getFontHeight()) / 2), 0xFF000000, false);
            } else if (hovered) {
                RenderUtil.drawRoundedRect(10, y, 200, 36, 8, Gnome.HOVER);
                drawString(context, this.categoryList.get(i).getName(), 24.0F, (float) (y + (36 - ClickGui.getFontHeight()) / 2), 0xFF000000, false);
            } else {
                drawString(context, this.categoryList.get(i).getName(), 24.0F, (float) (y + (36 - ClickGui.getFontHeight()) / 2), 0xFF000000, false);
            }
        }

        drawString(context, "dev, laoqi123", 16.0F, (float) (this.height - Gnome.CONTENT_PAD - ClickGui.getFontHeight() * 2), 0xFF000000, false);
        drawString(context, "Myau " + Myau.version, 16.0F, (float) (this.height - Gnome.CONTENT_PAD - ClickGui.getFontHeight()), 0xFF000000, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0 && mouseX >= Gnome.SIDEBAR_WIDTH) {
            int scrollDir = verticalAmount > 0 ? 1 : -1;
            this.categoryList.get(this.selectedCategory).onScroll((int) mouseX, (int) mouseY, scrollDir);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        int x = (int) mouseX;
        int y = (int) mouseY;

        int searchX = this.width - 270;
        int searchY = (Gnome.HEADER_HEIGHT - 26) / 2;
        if (x >= searchX && x <= searchX + 250 && y >= searchY && y <= searchY + 26) {
            this.searchFocused = true;
            return true;
        }

        if (x < Gnome.SIDEBAR_WIDTH && y > Gnome.HEADER_HEIGHT) {
            this.searchFocused = false;
            if (mouseButton == 0) {
                int index = (y - (Gnome.HEADER_HEIGHT + 12)) / 44;
                if (index < 0) {
                    index = 0;
                }
                if (index >= this.categoryList.size()) {
                    index = this.categoryList.size() - 1;
                }
                if (this.selectedCategory != index) {
                    this.selectedCategory = index;
                    this.categoryList.get(this.selectedCategory).resetScroll();
                }
            }
            return true;
        }

        this.searchFocused = false;
        this.categoryList.get(this.selectedCategory).mouseClicked(x, y, mouseButton);
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        this.categoryList.get(this.selectedCategory).mouseReleasedAll((int) mouseX, (int) mouseY, mouseButton);
        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.searchFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (this.searchQuery.length() > 0) {
                    this.searchQuery.deleteCharAt(this.searchQuery.length() - 1);
                }
                return true;
            }
            char c = (char) keyCode;
            if (c >= 'A' && c <= 'Z' && (modifiers & GLFW.GLFW_MOD_SHIFT) == 0) {
                c = Character.toLowerCase(c);
            }
            if (Character.isLetterOrDigit(c) || Character.isSpaceChar(c) || c == '-' || c == '_' || c == '.') {
                this.searchQuery.append(c);
            }
            return true;
        }

        boolean hasBinding = false;
        for (CategoryComponent cat : categoryList) {
            for (Component comp : cat.getModules()) {
                if (comp instanceof BindComponent && ((BindComponent) comp).isBinding) {
                    hasBinding = true;
                    break;
                }
            }
            if (hasBinding) break;
        }

        if (hasBinding) {
            for (CategoryComponent cat : categoryList) {
                for (Component comp : cat.getModules()) {
                    comp.keyTyped((char) keyCode, keyCode);
                }
            }
        } else {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.close();
            } else {
                for (CategoryComponent cat : categoryList) {
                    for (Component comp : cat.getModules()) {
                        comp.keyTyped((char) keyCode, keyCode);
                    }
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        savePositions();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void savePositions() {
        JsonObject json = new JsonObject();
        json.addProperty("selected", this.categoryList.get(this.selectedCategory).getName());
        try (FileWriter writer = new FileWriter(configFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPositions() {
        if (!configFile.exists()) return;
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            if (json.has("selected")) {
                String name = json.get("selected").getAsString();
                for (int i = 0; i < this.categoryList.size(); i++) {
                    if (this.categoryList.get(i).getName().equals(name)) {
                        this.selectedCategory = i;
                        break;
                    }
                }
            }
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
    }
}