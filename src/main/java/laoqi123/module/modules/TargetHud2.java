package laoqi123.module.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.PacketEvent;
import laoqi123.events.Render2DEvent;
import laoqi123.module.Module;
import laoqi123.module.modules.targethud.TargetHUDMode;
import laoqi123.module.modules.targethud.impl.ExhibitionTargetHUD;
import laoqi123.module.modules.targethud.impl.MyauTargetHUD;
import laoqi123.module.modules.targethud.impl.NovolineTargetHUD;
import laoqi123.module.modules.targethud.impl.RavenLegacyTargetHUD;
import laoqi123.module.modules.targethud.impl.RavenModernTargetHUD;
import laoqi123.module.modules.targethud.impl.UnfairTargetHUD;
import laoqi123.mixin.PlayerInteractEntityC2SPacketAccessor;
import laoqi123.property.Property;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.property.properties.ModeProperty;
import laoqi123.util.RenderUtil;
import laoqi123.util.TeamUtil;
import laoqi123.util.TimerUtil;
import laoqi123.util.config.PropertyProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.Identifier;
import org.joml.Vector4d;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TargetHud2 extends Module implements PropertyProvider {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static MinecraftClient getMinecraft() {
        return mc;
    }
    public static final String FORMAT = "§";
    public static final DecimalFormat HEALTH_FORMAT = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    public static final DecimalFormat DIFF_FORMAT = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));
    private static final Identifier STEVE_SKIN = Identifier.ofVanilla("textures/entity/steve.png");
    private static final float FADE_DURATION_MS = 400.0F;
    private static final float FOLLOW_PLAYER_X_PADDING = 2.0F;

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Myau", "RavenModern", "RavenLegacy", "Unfair", "Novoline", "Exhibition"});
    public final ModeProperty health = new ModeProperty("Health", 0, new String[]{"Entity", "Tab"});
    public final BooleanProperty kaOnly = new BooleanProperty("Ka Only", true);
    public final BooleanProperty chatPreview = new BooleanProperty("Chat Preview", false);
    public final BooleanProperty followPlayer = new BooleanProperty("Follow Player", false);
    public final ModeProperty posX = new ModeProperty("position-x", 0, new String[]{"LEFT", "MIDDLE", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 0, new String[]{"TOP", "MIDDLE", "BOTTOM"});
    public final IntProperty offX = new IntProperty("offset-x", 0, -500, 500);
    public final IntProperty offY = new IntProperty("offset-y", 30, -500, 500);

    private final TimerUtil lastAttackTimer = new TimerUtil();
    public final TimerUtil animTimer = new TimerUtil();
    private final List<TargetHUDMode> modes = new ArrayList<>();

    private LivingEntity lastTarget = null;
    private LivingEntity target = null;
    private LivingEntity fadingEntity = null;
    private TimerUtil fadeTimer = null;
    private boolean fadingIn = false;
    public Identifier headTexture = null;
    public float oldHealth = 0.0F;
    public float newHealth = 0.0F;
    public float maxHealth = 0.0F;
    public float lastHealthBar = 0.0F;
    private DrawContext activeContext = null;

    private boolean dragging = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int dragStartOffX = 0;
    private int dragStartOffY = 0;
    private boolean positionLocked = true;

    public TargetHud2() {
        super("TargetHud2", false, true);
        this.modes.add(new MyauTargetHUD());
        this.modes.add(new RavenModernTargetHUD());
        this.modes.add(new RavenLegacyTargetHUD());
        this.modes.add(new UnfairTargetHUD());
        this.modes.add(new NovolineTargetHUD());
        this.modes.add(new ExhibitionTargetHUD());
        this.mode.setChangeListener(value -> this.rebuildSettings());
        this.rebuildSettings();
    }

    @Override
    public List<Property<?>> getAdditionalProperties() {
        return this.collectModeProperties();
    }

    @Override
    public void onDisabled() {
        this.target = null;
        this.fadingEntity = null;
        this.fadeTimer = null;
        this.lastTarget = null;
        this.headTexture = null;
        this.dragging = false;
    }

    private void rebuildSettings() {
        if (Myau.propertyManager == null) {
            return;
        }
        ArrayList<Property<?>> list = new ArrayList<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object object = field.get(this);
                if (object instanceof Property<?>) {
                    list.add((Property<?>) object);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        list.addAll(this.collectModeProperties());
        Myau.propertyManager.properties.put(TargetHud2.class, list);
    }

    private List<Property<?>> collectModeProperties() {
        List<Property<?>> properties = new ArrayList<>();
        TargetHUDMode current = this.getCurrentMode();
        if (current == null) {
            return properties;
        }
        for (Field field : current.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object object = field.get(current);
                if (object instanceof Property<?>) {
                    Property<?> property = (Property<?>) object;
                    property.setOwner(this);
                    properties.add(property);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return properties;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }

    public TargetHUDMode getCurrentMode() {
        int index = this.mode.getValue();
        return index >= 0 && index < this.modes.size() ? this.modes.get(index) : this.modes.get(0);
    }

    public DrawContext getActiveContext() {
        return this.activeContext;
    }

    public void drawText(String text, float x, float y, int color, boolean shadow) {
        RenderUtil.drawGuiText(text, x, y, color, shadow);
    }

    public int getTextWidth(String text) {
        return mc.textRenderer.getWidth(text);
    }

    public void renderPlayerHead(LivingEntity entity, float x, float y, float size) {
        this.renderPlayerHead(entity, x, y, size, 0xFFFFFFFF);
    }

    public void renderPlayerHead(LivingEntity entity, float x, float y, float size, int argb) {
        if (this.activeContext == null) {
            return;
        }
        Identifier texture = entity instanceof PlayerEntity ? this.getSkin(entity) : null;
        if (texture == null) {
            texture = STEVE_SKIN;
        }
        float alpha = ((argb >>> 24) & 255) / 255.0F;
        float red = (argb >> 16 & 255) / 255.0F;
        float green = (argb >> 8 & 255) / 255.0F;
        float blue = (argb & 255) / 255.0F;
        RenderUtil.enableRenderState();
        RenderSystem.setShaderColor(red, green, blue, alpha);
        RenderUtil.drawTexturedRect(texture, x, y, size, size, 8.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F, 16.0F / 64.0F, 0xFFFFFFFF);
        RenderUtil.drawTexturedRect(texture, x, y, size, size, 40.0F / 64.0F, 8.0F / 64.0F, 48.0F / 64.0F, 16.0F / 64.0F, 0xFFFFFFFF);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderUtil.disableRenderState();
    }

    public void renderRoundedPlayerHead(LivingEntity entity, float x, float y, float size, float radius, int argb) {
        if (this.activeContext == null) {
            return;
        }
        Identifier texture = entity instanceof PlayerEntity ? this.getSkin(entity) : null;
        if (texture == null) {
            texture = STEVE_SKIN;
        }
        RenderUtil.enableRenderState();
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.stencilMask(0xFF);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE);
        RenderUtil.drawRoundedRect(x, y, size, size, Math.min(radius, size / 2.0F), 0xFFFFFFFF);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.colorMask(true, true, true, true);
        float alpha = ((argb >>> 24) & 255) / 255.0F;
        float red = (argb >> 16 & 255) / 255.0F;
        float green = (argb >> 8 & 255) / 255.0F;
        float blue = (argb & 255) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, alpha);
        RenderUtil.drawTexturedRect(texture, x, y, size, size, 8.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F, 16.0F / 64.0F, 0xFFFFFFFF);
        RenderUtil.drawTexturedRect(texture, x, y, size, size, 40.0F / 64.0F, 8.0F / 64.0F, 48.0F / 64.0F, 16.0F / 64.0F, 0xFFFFFFFF);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.stencilMask(0);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.depthMask(true);
        RenderUtil.disableRenderState();
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        // TargetHUD's Compose panel covers this readout too, so only one of them draws.
        // Its six skins stay available through the vanilla path when Compose is unavailable.
        if (laoqi123.oneconfig.huds.TargetHUDComposeHud.isActive()) {
            this.target = null;
            this.clearFade();
            return;
        }
        if (!this.isEnabled() || mc.player == null) {
            this.target = null;
            this.clearFade();
            return;
        }
        this.activeContext = event.getContext();
        try {
            this.updateTargetState();
            if (this.target == null && this.fadeTimer == null) {
                return;
            }
            LivingEntity entity = this.target != null ? this.target : this.fadingEntity;
            if (entity == null) {
                return;
            }
            HealthInfo targetHealthInfo = this.getHealthInfo(entity);
            float targetHealth = targetHealthInfo.health;
            if (entity != this.target) {
                this.headTexture = null;
                this.animTimer.setTime();
                this.oldHealth = targetHealth;
                this.newHealth = targetHealth;
            }
            TargetHUDMode currentMode = this.getCurrentMode();
            if (!currentMode.shouldAnimateHealth() || this.animTimer.hasTimeElapsed(150L)) {
                this.oldHealth = this.newHealth;
                this.newHealth = targetHealth;
                this.maxHealth = targetHealthInfo.maxHealth;
                if (this.oldHealth != this.newHealth) {
                    this.animTimer.reset();
                }
            }
            Identifier resourceLocation = this.getSkin(entity);
            if (resourceLocation != null) {
                this.headTexture = resourceLocation;
            }
            RenderData data = this.getRenderData();
            if (data == null) {
                return;
            }
            float[] size = currentMode.getSize(this, data);
            if (size == null) {
                return;
            }
            this.updateDrag(size[0], size[1]);
            float x = this.getWidgetX(size[0]);
            float y = this.getWidgetY(size[1]);
            if (!this.dragging) {
                float[] follow = this.getFollowPosition(size[0], size[1]);
                if (follow != null) {
                    x = follow[0];
                    y = follow[1];
                }
            }
            currentMode.render(this, data, x, y);
        } finally {
            this.activeContext = null;
        }
    }

    public float getWidgetX(float width) {
        int scaledWidth = mc.getWindow().getScaledWidth();
        float x = this.offX.getValue().floatValue();
        switch (this.posX.getValue()) {
            case 1:
                x += scaledWidth / 2.0F - width / 2.0F;
                break;
            case 2:
                x = scaledWidth - width - this.offX.getValue().floatValue();
                break;
            default:
                break;
        }
        return x;
    }

    public float getWidgetY(float height) {
        int scaledHeight = mc.getWindow().getScaledHeight();
        float y = this.offY.getValue().floatValue();
        switch (this.posY.getValue()) {
            case 1:
                y += scaledHeight / 2.0F - height / 2.0F;
                break;
            case 2:
                y = scaledHeight - height - this.offY.getValue().floatValue();
                break;
            default:
                break;
        }
        return y;
    }

    private void updateDrag(float width, float height) {
        int scaledWidth = mc.getWindow().getScaledWidth();
        int scaledHeight = mc.getWindow().getScaledHeight();
        int mouseX = (int) (mc.mouse.getX() * scaledWidth / mc.getWindow().getFramebufferWidth());
        int mouseY = (int) (scaledHeight - mc.mouse.getY() * scaledHeight / mc.getWindow().getFramebufferHeight() - 1);
        this.positionLocked = !(mc.currentScreen instanceof ChatScreen);
        if (this.positionLocked) {
            return;
        }
        float x = this.getWidgetX(width);
        float y = this.getWidgetY(height);
        if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS && !this.dragging) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                this.dragging = true;
                this.dragStartX = mouseX;
                this.dragStartY = mouseY;
                this.dragStartOffX = this.offX.getValue();
                this.dragStartOffY = this.offY.getValue();
            }
        } else if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            this.dragging = false;
        }
        if (this.dragging) {
            int deltaX = mouseX - this.dragStartX;
            int deltaY = mouseY - this.dragStartY;
            if (this.posX.getValue() == 2) {
                deltaX = -deltaX;
            }
            if (this.posY.getValue() == 2) {
                deltaY = -deltaY;
            }
            this.offX.setValue(this.dragStartOffX + deltaX);
            this.offY.setValue(this.dragStartOffY + deltaY);
        }
    }

    private LivingEntity resolveTarget() {
        KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.isAttackAllowed() && TeamUtil.isEntityLoaded(killAura.getTarget())) {
            return killAura.getTarget();
        } else if (!this.kaOnly.getValue()
                && !this.lastAttackTimer.hasTimeElapsed(1500L)
                && TeamUtil.isEntityLoaded(this.lastTarget)) {
            return this.lastTarget;
        } else {
            return this.chatPreview.getValue() && mc.currentScreen instanceof ChatScreen ? mc.player : null;
        }
    }

    private boolean isChatPreviewTarget(LivingEntity entity) {
        return entity == mc.player && this.chatPreview.getValue() && mc.currentScreen instanceof ChatScreen;
    }

    private void startFadeIn() {
        this.fadeTimer = new TimerUtil();
        this.fadeTimer.reset();
        this.fadingIn = true;
        this.fadingEntity = null;
    }

    private void startFadeOut(LivingEntity entity) {
        this.fadeTimer = new TimerUtil();
        this.fadeTimer.reset();
        this.fadingIn = false;
        this.fadingEntity = entity;
    }

    private void clearFade() {
        this.fadeTimer = null;
        this.fadingIn = false;
        this.fadingEntity = null;
    }

    private void updateTargetState() {
        if (!this.isEnabled() || mc.player == null) {
            this.target = null;
            this.clearFade();
            return;
        }
        LivingEntity previousTarget = this.target;
        LivingEntity resolvedTarget = this.resolveTarget();
        this.target = resolvedTarget;
        if (this.target != null) {
            if (this.isChatPreviewTarget(this.target)) {
                this.clearFade();
                return;
            }
            this.fadingEntity = null;
            if ((previousTarget == null || this.fadeTimer != null && !this.fadingIn) && this.fadeTimer == null) {
                this.startFadeIn();
            } else if (this.fadeTimer != null && !this.fadingIn) {
                this.startFadeIn();
            } else if (this.fadingIn && this.fadeTimer != null && this.fadeTimer.getElapsedTime() >= 400L) {
                this.clearFade();
            }
            return;
        }
        if (previousTarget != null) {
            if (previousTarget == mc.player) {
                this.clearFade();
            } else if (this.fadeTimer == null || this.fadingIn) {
                this.startFadeOut(previousTarget);
            }
        }
        if (this.fadeTimer != null && (this.fadingIn || this.fadingEntity == null || this.fadeTimer.getElapsedTime() >= 400L)) {
            this.target = null;
            this.clearFade();
        }
    }

    private Identifier getSkin(Entity entity) {
        if (entity instanceof PlayerEntity && mc.getNetworkHandler() != null) {
            PlayerListEntry info = mc.getNetworkHandler().getPlayerListEntry(entity.getName().getString());
            if (info != null && info.getSkinTextures() != null) {
                return info.getSkinTextures().texture();
            }
        }
        return null;
    }

    private float getTabHealth(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity) || mc.world == null) {
            return -1.0F;
        }
        Scoreboard scoreboard = mc.world.getScoreboard();
        if (scoreboard == null) {
            return -1.0F;
        }
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
        if (objective == null) {
            return -1.0F;
        }
        ReadableScoreboardScore score = scoreboard.getScore(ScoreHolder.fromName(entity.getName().getString()), objective);
        return score == null ? -1.0F : (float) score.getScore();
    }

    public static float getPartialTicks() {
        return mc.getRenderTickCounter().getTickDelta(true);
    }

    public static float finiteOrDefault(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    public static float finiteHealth(float value) {
        return Math.max(0.0F, finiteOrDefault(value, 0.0F));
    }

    public HealthInfo getHealthInfo(LivingEntity entity) {
        float healthPoints = finiteHealth(entity.getHealth());
        if (this.health.getValue() == 1) {
            float tabHealth = this.getTabHealth(entity);
            if (Float.isFinite(tabHealth) && tabHealth >= 0.0F) {
                healthPoints = finiteHealth(tabHealth);
            }
        }
        float absorptionHearts = finiteHealth(entity.getAbsorptionAmount()) / 2.0F;
        float healthHearts = healthPoints / 2.0F + absorptionHearts;
        float maxHearts = Math.max(finiteHealth(entity.getMaxHealth()), healthPoints) / 2.0F;
        return new HealthInfo(healthHearts, absorptionHearts, Math.max(maxHearts, 1.0F));
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof PlayerInteractEntityC2SPacket) {
            PlayerInteractEntityC2SPacket packet = (PlayerInteractEntityC2SPacket) event.getPacket();
            int entityId = ((PlayerInteractEntityC2SPacketAccessor) packet).getEntityId();
            Entity entity = mc.world != null ? mc.world.getEntityById(entityId) : null;
            if (entity instanceof LivingEntity && !(entity instanceof ArmorStandEntity)) {
                this.lastAttackTimer.reset();
                this.lastTarget = (LivingEntity) entity;
            }
        }
    }

    public boolean shouldFollowPlayer() {
        LivingEntity entity = this.getRenderableEntity();
        return this.followPlayer.getValue() && entity != null && entity != mc.player;
    }

    public float[] getFollowPosition(float width, float height) {
        LivingEntity entity = this.getRenderableEntity();
        if (!this.followPlayer.getValue() || entity == null || entity == mc.player) {
            return null;
        }
        Vector4d projection = RenderUtil.projectToScreen(entity, 1.0D);
        if (projection == null) {
            return null;
        }
        float right = (float) projection.z;
        float top = (float) projection.y;
        float bottom = (float) projection.w;
        if (!Float.isFinite(right) || !Float.isFinite(top) || !Float.isFinite(bottom)) {
            return null;
        }
        return new float[]{right + FOLLOW_PLAYER_X_PADDING, bottom - (bottom - top) / 2.0F - height / 2.0F};
    }

    public LivingEntity getRenderableEntity() {
        return this.target != null ? this.target : this.fadingEntity;
    }

    public RenderData getRenderData() {
        LivingEntity entity = this.getRenderableEntity();
        if (entity == null || mc.player == null) {
            return null;
        }
        HealthInfo playerHealthInfo = this.getHealthInfo(mc.player);
        HealthInfo targetHealthInfo = this.getHealthInfo(entity);
        return new RenderData(entity, playerHealthInfo.health, targetHealthInfo.absorption, targetHealthInfo.health, targetHealthInfo.maxHealth);
    }

    public int getFadeAlpha() {
        if (this.fadeTimer == null) {
            return 255;
        }
        long elapsed = this.fadeTimer.getElapsedTime();
        if (elapsed < 400L) {
            return this.fadingIn ? (int) (elapsed / FADE_DURATION_MS * 255.0F) : (int) (255.0F - elapsed / FADE_DURATION_MS * 255.0F);
        }
        if (!this.fadingIn) {
            this.target = null;
            this.fadeTimer = null;
            this.fadingEntity = null;
            return 0;
        }
        return 255;
    }

    public TargetHudBounds getModernBounds(String playerInfo, float widgetX, float widgetY) {
        int padding = 8;
        int targetStrWithPadding = mc.textRenderer.getWidth(playerInfo) + padding;
        int textX = Math.round(widgetX) + padding;
        int textY = Math.round(widgetY) + padding;
        int left = textX - padding;
        int top = textY - padding;
        int right = textX + targetStrWithPadding;
        int contentBottom = textY + (mc.textRenderer.fontHeight + 5) - 6 + padding;
        return new TargetHudBounds(left, top, right, contentBottom, contentBottom + 13, textX, textY);
    }

    public String buildModernPlayerInfo(LivingEntity entity, float targetHealth, float playerHealth, boolean indicator) {
        targetHealth = finiteHealth(targetHealth);
        playerHealth = finiteHealth(playerHealth);
        String playerInfo = entity.getDisplayName().getString();
        playerInfo += " " + FORMAT + "c" + String.format("%.1f", targetHealth);
        if (indicator) {
            playerInfo += " " + (targetHealth <= playerHealth ? FORMAT + "aW" : FORMAT + "cL");
        }
        return playerInfo;
    }

    public int[] getRavenGradientColors() {
        HUD hud = (HUD) Myau.moduleManager.getModule(HUD.class);
        int left = hud != null ? hud.getColor(System.currentTimeMillis()).getRGB() : Color.WHITE.getRGB();
        int right = hud != null ? hud.getColor(System.currentTimeMillis() + 500L).getRGB() : Color.WHITE.getRGB();
        return new int[]{left, right};
    }

    public float updateRavenHealthBar(float healthBar, int barLeft, int barRight) {
        healthBar = finiteOrDefault(healthBar, barLeft);
        this.lastHealthBar = finiteOrDefault(this.lastHealthBar, healthBar);
        if (this.lastHealthBar != healthBar && this.lastHealthBar - barLeft >= 3.0F) {
            float diff = this.lastHealthBar - healthBar;
            if (diff > 0.0F) {
                this.lastHealthBar -= diff * 0.1F;
            } else {
                this.lastHealthBar += -diff * 0.1F;
            }
        } else {
            this.lastHealthBar = healthBar;
        }
        if (this.lastHealthBar > barRight) {
            this.lastHealthBar = barRight;
        }
        return this.lastHealthBar;
    }

    public record TargetHudBounds(int left, int top, int right, int contentBottom, int bottom, int textX, int textY) {

        public float width() {
            return this.right - this.left;
        }

        public float height() {
            return this.bottom - this.top;
        }
    }

    public record HealthInfo(float health, float absorption, float maxHealth) {
    }

    public record RenderData(LivingEntity entity, float playerHealth, float absorption, float targetHealth,
                             float maxHealth) {
    }
}