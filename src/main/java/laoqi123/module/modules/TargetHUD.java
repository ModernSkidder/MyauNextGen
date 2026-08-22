package laoqi123.module.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import laoqi123.Myau;
import laoqi123.enums.ChatColors;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.PacketEvent;
import laoqi123.events.Render2DEvent;
import laoqi123.font.UFontRenderer;
import laoqi123.module.Module;
import laoqi123.oneconfig.Glass;
import laoqi123.mixin.PlayerInteractEntityC2SPacketAccessor;
import laoqi123.property.properties.*;
import laoqi123.util.ColorUtil;
import laoqi123.util.RenderUtil;
import laoqi123.util.RotationUtil;
import laoqi123.util.TeamUtil;
import laoqi123.util.TimerUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TargetHUD extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final DecimalFormat healthFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    private static final DecimalFormat diffFormat = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));
    private static final float FIXED_BAR_WIDTH = 130f;
    private static final float ANIMATION_SPEED = 0.1f;

    private final TimerUtil lastAttackTimer = new TimerUtil();
    private final TimerUtil animTimer = new TimerUtil();
    private final TimerUtil scaleAnimTimer = new TimerUtil();
    private final TimerUtil targetLostTimer = new TimerUtil();
    private LivingEntity lastTarget = null;
    private LivingEntity target = null;
    private Identifier headTexture = null;
    private float oldHealth = 0.0F;
    private float newHealth = 0.0F;
    private float maxHealth = 0.0F;
    private float scaleAnimation = 0.0F;
    private boolean isAnimatingOut = false;
    private boolean targetLost = false;
    private LivingEntity lastRenderTarget = null;

    private final Map<Integer, Float> displayHealths = new HashMap<>();
    private final Map<Integer, Float> delayedHealths = new HashMap<>();
    private final Map<Integer, Identifier> headTextures = new HashMap<>();
    private final Map<Integer, Float> entityAlphas = new HashMap<>();
    private final Map<Integer, LivingEntity> entityCache = new HashMap<>();
    private final TimerUtil healthDelayTimer = new TimerUtil();

    private boolean dragging = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int dragStartOffX = 0;
    private int dragStartOffY = 0;
    private boolean positionLocked = true;

    private UFontRenderer modernFont;

    public final ModeProperty style = new ModeProperty("Style", 0, new String[]{"Myau", "Adjust"});
    public final ModeProperty fontMode = new ModeProperty("font-mode", 0, new String[]{"Minecraft", "Modern"});
    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"DEFAULT", "HUD"});
    public final ModeProperty posX = new ModeProperty("position-x", 1, new String[]{"LEFT", "MIDDLE", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 1, new String[]{"TOP", "MIDDLE", "BOTTOM"});
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final IntProperty offX = new IntProperty("offset-x", 0, -500, 500);
    public final IntProperty offY = new IntProperty("offset-y", 40, -500, 500);
    public final PercentProperty background = new PercentProperty("background", 25);
    public final BooleanProperty head = new BooleanProperty("head", true);
    public final BooleanProperty indicator = new BooleanProperty("indicator", true);
    public final BooleanProperty outline = new BooleanProperty("outline", false);
    public final BooleanProperty animations = new BooleanProperty("animations", true);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final BooleanProperty kaOnly = new BooleanProperty("ka-only", true);
    public final BooleanProperty chatPreview = new BooleanProperty("chat-preview", false);
    public final BooleanProperty trackTarget = new BooleanProperty("track-target", false);
    public final ModeProperty trackingMode = new ModeProperty("tracking-mode", 0, new String[]{"TOP", "MIDDLE", "LEFT", "RIGHT"}, trackTarget::getValue);
    public final BooleanProperty distanceScale = new BooleanProperty("distance-scale", true, trackTarget::getValue);

    public TargetHUD() {
        super("TargetHUD", false, true);
    }

    @Override
    public void onDisabled() {
        this.target = null;
        this.lastRenderTarget = null;
        this.lastTarget = null;
        this.isAnimatingOut = false;
        this.targetLost = false;
        this.scaleAnimation = 0.0F;
        this.entityAlphas.clear();
        this.entityCache.clear();
        this.displayHealths.clear();
        this.delayedHealths.clear();
        this.headTextures.clear();
        this.dragging = false;
    }

    private interface FontDrawer {
        int getWidth(String text);

        void draw(String text, float x, float y, int color, boolean shadow);
    }

    private FontDrawer getFontRenderer(DrawContext context) {
        if (fontMode.getValue() == 1) {
            if (modernFont == null) {
                try {
                    modernFont = new UFontRenderer("GoogleSans-Regular", 20);
                } catch (Exception e) {
                    modernFont = null;
                }
            }
            if (modernFont != null) {
                return new FontDrawer() {
                    @Override
                    public int getWidth(String text) {
                        return modernFont.getStringWidth(text);
                    }

                    @Override
                    public void draw(String text, float x, float y, int color, boolean shadow) {
                        modernFont.drawString(text, x, y, color, shadow);
                    }
                };
            }
        }
        return new FontDrawer() {
            @Override
            public int getWidth(String text) {
                return mc.textRenderer.getWidth(text);
            }

            @Override
            public void draw(String text, float x, float y, int color, boolean shadow) {
                context.drawText(mc.textRenderer, text, Math.round(x), Math.round(y), color, shadow);
            }
        };
    }

    private static float sx(float localX, float posX, float barTotalWidth, float finalScale) {
        return posX + barTotalWidth / 2.0F + (localX - barTotalWidth / 2.0F) * finalScale;
    }

    private static float sy(float localY, float posY, float finalScale) {
        return posY + 13.5F + (localY - 13.5F) * finalScale;
    }

    /**
     * The entity the HUD should describe, or null when there is none.
     *
     * <p>Exposed so the Compose HUD can reuse the same selection logic rather than
     * duplicating the KillAura and chat-preview rules.
     */
    public LivingEntity resolveTarget() {
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

    private List<LivingEntity> resolveTargets() {
        List<LivingEntity> result = new ArrayList<>();
        KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.isAttackAllowed()) {
            List<LivingEntity> kaTargets = killAura.getTargets();
            if (kaTargets != null) {
                for (LivingEntity t : kaTargets) {
                    if (TeamUtil.isEntityLoaded(t)) result.add(t);
                }
            }
        }
        if (result.isEmpty() && chatPreview.getValue() && mc.currentScreen instanceof ChatScreen) {
            result.add(mc.player);
        }
        return result;
    }

    /** Skin texture for a player target, or null. Exposed for the Compose HUD. */
    public Identifier getSkin(LivingEntity entity) {
        if (entity instanceof PlayerEntity && mc.getNetworkHandler() != null) {
            PlayerListEntry info = mc.getNetworkHandler().getPlayerListEntry(entity.getName().getString());
            if (info != null && info.getSkinTextures() != null) {
                return info.getSkinTextures().texture();
            }
        }
        return null;
    }

    private Color getTargetColor(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            if (TeamUtil.isFriend((PlayerEntity) entity)) {
                return Myau.friendManager.getColor();
            }
            if (TeamUtil.isTarget((PlayerEntity) entity)) {
                return Myau.targetManager.getColor();
            }
        }
        switch (this.color.getValue()) {
            case 0:
                if (!(entity instanceof PlayerEntity)) {
                    return new Color(-1);
                }
                return TeamUtil.getTeamColor((PlayerEntity) entity, 1.0F);
            case 1:
                HUD hud = (HUD) Myau.moduleManager.getModule(HUD.class);
                if (hud != null) {
                    int rgb = hud.getColor(System.currentTimeMillis()).getRGB();
                    return new Color(rgb);
                }
                return new Color(-1);
            default:
                return new Color(-1);
        }
    }

    private boolean isLeftMouseDown() {
        return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (!this.isEnabled() || mc.player == null) return;
        // The Compose HUD draws this panel inside OneConfig's Skia scene, compositing the
        // avatar over it with OpenGL. This stays as the fallback for when that is
        // unavailable.
        if (laoqi123.oneconfig.huds.TargetHUDComposeHud.isActive()) return;

        if (style.getValue() == 1) {
            renderAdjustMode(event.getContext());
            return;
        }

        LivingEntity currentTarget = this.resolveTarget();

        if (currentTarget != null) {
            targetLost = false;
            if (!isAnimatingOut) {
                if (this.target != currentTarget) {
                    if (this.target == null) {
                        scaleAnimTimer.reset();
                    }
                    this.target = currentTarget;
                    this.lastRenderTarget = currentTarget;
                    this.headTexture = null;
                    this.animTimer.setTime();
                    this.maxHealth = this.target.getMaxHealth() / 2.0F;
                    float heal = this.target.getHealth() / 2.0F;
                    this.oldHealth = heal;
                    this.newHealth = heal;
                }
                updateScaleAnimation();

                renderMyauMode(event.getContext());
            }
        } else if (this.target != null) {
            if (!targetLost) {
                targetLost = true;
                targetLostTimer.reset();
            }

            if (targetLostTimer.hasTimeElapsed(50L)) {
                isAnimatingOut = true;
                scaleAnimTimer.reset();
                this.target = null;
            } else {
                renderMyauMode(event.getContext());
            }
        }

        if (isAnimatingOut) {
            updateScaleAnimation();
            if (scaleAnimation > 0.0F && lastRenderTarget != null) {
                this.target = lastRenderTarget;
                renderMyauMode(event.getContext());
                this.target = null;
            } else if (scaleAnimation <= 0.0F) {
                isAnimatingOut = false;
                targetLost = false;
                lastRenderTarget = null;
            }
        }
    }

    private void updateScaleAnimation() {
        long elapsedTime = scaleAnimTimer.getElapsedTime();
        float animationDuration = 200.0F;

        if (!isAnimatingOut) {
            scaleAnimation = Math.min(elapsedTime / animationDuration, 1.0F);
        } else {
            scaleAnimation = Math.max(1.0F - (elapsedTime / animationDuration), 0.0F);
        }
        scaleAnimation = easeOutQuart(scaleAnimation);
    }

    private float easeOutQuart(float t) {
        return 1.0F - (float) Math.pow(1.0F - t, 4.0F);
    }

    private void renderMyauMode(DrawContext context) {
        FontDrawer fr = getFontRenderer(context);
        float health = (mc.player.getHealth() + mc.player.getAbsorptionAmount()) / 2.0F;
        float abs = this.target.getAbsorptionAmount() / 2.0F;
        float heal = this.target.getHealth() / 2.0F;

        if (!this.animations.getValue() || this.animTimer.hasTimeElapsed(150L)) {
            this.oldHealth = this.newHealth;
            this.newHealth = heal;
            this.maxHealth = this.target.getMaxHealth() / 2.0F;
            if (this.oldHealth != this.newHealth) {
                this.animTimer.reset();
            }
        }

        Identifier resourceLocation = this.getSkin(this.target);
        if (resourceLocation != null) {
            this.headTexture = resourceLocation;
        }

        float elapsedTime = (float) Math.min(Math.max(this.animTimer.getElapsedTime(), 0L), 150L);
        float healthRatio = Math.min(Math.max(RenderUtil.lerpFloat(this.newHealth, this.oldHealth, elapsedTime / 150.0F) / this.maxHealth, 0.0F), 1.0F);
        Color targetColor = this.getTargetColor(this.target);
        Color healthBarColor = this.color.getValue() == 0 ? ColorUtil.getHealthBlend(healthRatio) : targetColor;
        float healthDeltaRatio = Math.min(Math.max((health - heal + 1.0F) / 2.0F, 0.0F), 1.0F);
        Color healthDeltaColor = ColorUtil.getHealthBlend(healthDeltaRatio);

        int scaledWidth = mc.getWindow().getScaledWidth();
        int scaledHeight = mc.getWindow().getScaledHeight();
        String targetNameText = ChatColors.formatColor(String.format("&r%s&r", TeamUtil.stripName(this.target)));
        int targetNameWidth = fr.getWidth(targetNameText);
        String healthText = ChatColors.formatColor(
                String.format("&r&f%s%s\u2764&r", healthFormat.format(heal), abs > 0.0F ? "&6" : "&c")
        );
        int healthTextWidth = fr.getWidth(healthText);
        String statusText = ChatColors.formatColor(String.format("&r&l%s&r", heal == health ? "D" : (heal < health ? "W" : "L")));
        int statusTextWidth = fr.getWidth(statusText);
        String healthDiffText = ChatColors.formatColor(
                String.format("&r%s&r", heal == health ? "0.0" : diffFormat.format(health - heal))
        );
        int healthDiffWidth = fr.getWidth(healthDiffText);

        float barContentWidth = Math.max(
                (float) targetNameWidth + (this.indicator.getValue() ? 2.0F + (float) statusTextWidth + 2.0F : 0.0F),
                (float) healthTextWidth + (this.indicator.getValue() ? 2.0F + (float) healthDiffWidth + 2.0F : 0.0F)
        );
        float headIconOffset = this.head.getValue() && this.headTexture != null ? 25.0F : 0.0F;
        float barTotalWidth = Math.max(headIconOffset + 70.0F, headIconOffset + 2.0F + barContentWidth + 2.0F);

        float posX = this.offX.getValue().floatValue() / this.scale.getValue();
        switch (this.posX.getValue()) {
            case 1:
                posX += (float) scaledWidth / this.scale.getValue() / 2.0F - barTotalWidth / 2.0F;
                break;
            case 2:
                posX *= -1.0F;
                posX += (float) scaledWidth / this.scale.getValue() - barTotalWidth;
                break;
            default:
                break;
        }

        float posY = this.offY.getValue().floatValue() / this.scale.getValue();
        switch (this.posY.getValue()) {
            case 1:
                posY += (float) scaledHeight / this.scale.getValue() / 2.0F - 13.5F;
                break;
            case 2:
                posY *= -1.0F;
                posY += (float) scaledHeight / this.scale.getValue() - 27.0F;
                break;
            default:
                break;
        }

        int mouseX = (int) (mc.mouse.getX() * scaledWidth / mc.getWindow().getFramebufferWidth());
        int mouseY = (int) (scaledHeight - mc.mouse.getY() * scaledHeight / mc.getWindow().getFramebufferHeight() - 1);

        float renderX = posX * this.scale.getValue();
        float renderY = posY * this.scale.getValue();
        float renderWidth = barTotalWidth * this.scale.getValue();
        float renderHeight = 27.0F * this.scale.getValue();

        this.positionLocked = !(mc.currentScreen instanceof ChatScreen);

        if (!this.positionLocked) {
            if (isLeftMouseDown() && !this.dragging) {
                if (mouseX >= renderX && mouseX <= renderX + renderWidth
                        && mouseY >= renderY && mouseY <= renderY + renderHeight) {
                    this.dragging = true;
                    this.dragStartX = mouseX;
                    this.dragStartY = mouseY;
                    this.dragStartOffX = this.offX.getValue();
                    this.dragStartOffY = this.offY.getValue();
                }
            } else if (!isLeftMouseDown()) {
                this.dragging = false;
            }

            if (this.dragging) {
                int deltaX = mouseX - this.dragStartX;
                int deltaY = mouseY - this.dragStartY;
                if (this.posX.getValue() == 2) deltaX = -deltaX;
                if (this.posY.getValue() == 2) deltaY = -deltaY;
                this.offX.setValue(this.dragStartOffX + deltaX);
                this.offY.setValue(this.dragStartOffY + deltaY);
            }
        }

        float finalScale = this.scale.getValue() * scaleAnimation;

        RenderUtil.enableRenderState();
        // OneConfig HUD surface: 50% black by default, scaled by the Background slider.
        int backgroundColor = Glass.alpha(Glass.BG,
                Math.round(this.background.getValue() / 100.0F * 255.0F));
        float x0 = sx(0.0F, posX, barTotalWidth, finalScale);
        float y0 = sy(0.0F, posY, finalScale);
        float x1 = sx(barTotalWidth, posX, barTotalWidth, finalScale);
        float y1 = sy(27.0F, posY, finalScale);
        Glass.panel(x0, y0, x1 - x0, y1 - y0, backgroundColor, Glass.RADIUS * finalScale);
        if (this.outline.getValue()) {
            int outlineColor = targetColor.getRGB();
            RenderUtil.drawLine(x0, y0, x1, y0, 1.5F, outlineColor);
            RenderUtil.drawLine(x1, y0, x1, y1, 1.5F, outlineColor);
            RenderUtil.drawLine(x1, y1, x0, y1, 1.5F, outlineColor);
            RenderUtil.drawLine(x0, y1, x0, y0, 1.5F, outlineColor);
        }
        // Health bar as a pill, matching how OneConfig rounds its inline controls.
        float healthX0 = sx(headIconOffset + 2.0F, posX, barTotalWidth, finalScale);
        float healthX1 = sx(barTotalWidth - 2.0F, posX, barTotalWidth, finalScale);
        float healthY0 = sy(22.0F, posY, finalScale);
        float healthY1 = sy(25.0F, posY, finalScale);
        Glass.bar(healthX0, healthY0, healthX1 - healthX0, healthY1 - healthY0,
                healthBarColor.getRGB(), healthRatio);
        RenderUtil.disableRenderState();

        RenderUtil.enableRenderState();
        fr.draw(targetNameText, sx(headIconOffset + 2.0F, posX, barTotalWidth, finalScale), sy(2.0F, posY, finalScale), -1, this.shadow.getValue());
        fr.draw(healthText, sx(headIconOffset + 2.0F, posX, barTotalWidth, finalScale), sy(12.0F, posY, finalScale), -1, this.shadow.getValue());

        if (this.indicator.getValue()) {
            fr.draw(statusText, sx(barTotalWidth - 2.0F - (float) statusTextWidth, posX, barTotalWidth, finalScale), sy(2.0F, posY, finalScale),
                    healthDeltaColor.getRGB(), this.shadow.getValue());
            fr.draw(healthDiffText, sx(barTotalWidth - 2.0F - (float) healthDiffWidth, posX, barTotalWidth, finalScale), sy(12.0F, posY, finalScale),
                    ColorUtil.darker(healthDeltaColor, 0.8F).getRGB(), this.shadow.getValue());
        }

        if (this.head.getValue() && this.headTexture != null) {
            int hx = Math.round(sx(2.0F, posX, barTotalWidth, finalScale));
            int hy = Math.round(sy(2.0F, posY, finalScale));
            int hs = Math.round(23.0F * finalScale);
            context.drawTexture(RenderLayer::getGuiTextured, this.headTexture, hx, hy, 8.0F, 8.0F, hs, hs, 64, 64);
            context.drawTexture(RenderLayer::getGuiTextured, this.headTexture, hx, hy, 40.0F, 8.0F, hs, hs, 64, 64);
        }
        RenderUtil.disableRenderState();
    }

    private void renderAdjustMode(DrawContext context) {
        List<LivingEntity> activeTargets = resolveTargets();
        Set<Integer> activeIds = new HashSet<>();

        for (LivingEntity t : activeTargets) {
            int id = t.getId();
            activeIds.add(id);
            entityAlphas.putIfAbsent(id, 0f);
            entityCache.put(id, t);
            entityAlphas.put(id, Math.min(1f, entityAlphas.get(id) + ANIMATION_SPEED));
            displayHealths.putIfAbsent(id, t.getHealth());
            delayedHealths.putIfAbsent(id, t.getHealth());
            if (headTextures.get(id) == null) {
                headTextures.put(id, getSkin(t));
            }
        }

        Iterator<Map.Entry<Integer, Float>> iter = entityAlphas.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, Float> entry = iter.next();
            int id = entry.getKey();
            if (!activeIds.contains(id)) {
                float newAlpha = Math.max(0f, entry.getValue() - ANIMATION_SPEED);
                if (newAlpha <= 0f) {
                    iter.remove();
                    entityCache.remove(id);
                    displayHealths.remove(id);
                    delayedHealths.remove(id);
                    headTextures.remove(id);
                } else {
                    entry.setValue(newAlpha);
                }
            }
        }

        if (entityAlphas.isEmpty() && !(mc.currentScreen instanceof ChatScreen)) return;

        List<Map.Entry<Integer, Float>> sortedEntries = new ArrayList<>(entityAlphas.entrySet());
        sortedEntries.sort((a, b) -> {
            LivingEntity ea = entityCache.get(a.getKey());
            LivingEntity eb = entityCache.get(b.getKey());
            if (ea == null) return 1;
            if (eb == null) return -1;
            return Double.compare(RotationUtil.distanceToEntity(ea), RotationUtil.distanceToEntity(eb));
        });

        int renderedCount = 0;

        float s = scale.getValue();
        float barW = FIXED_BAR_WIDTH * s;
        int rows = (sortedEntries.size() + 1) / 2;
        float totalWidth = sortedEntries.size() >= 2 ? barW * 2 + 4 * s : barW;
        float totalHeight = rows * 37f * s + (rows - 1) * 4f * s;

        int scaledWidth = mc.getWindow().getScaledWidth();
        int scaledHeight = mc.getWindow().getScaledHeight();
        int mouseX = (int) (mc.mouse.getX() * scaledWidth / mc.getWindow().getFramebufferWidth());
        int mouseY = (int) (scaledHeight - mc.mouse.getY() * scaledHeight / mc.getWindow().getFramebufferHeight() - 1);

        positionLocked = !(mc.currentScreen instanceof ChatScreen);

        float x = offX.getValue().floatValue();
        float y = offY.getValue().floatValue();

        if (!positionLocked) {
            if (isLeftMouseDown() && !dragging) {
                if (mouseX >= x && mouseX <= x + totalWidth && mouseY >= y && mouseY <= y + totalHeight) {
                    dragging = true;
                    dragStartX = mouseX;
                    dragStartY = mouseY;
                    dragStartOffX = offX.getValue();
                    dragStartOffY = offY.getValue();
                }
            } else if (!isLeftMouseDown()) {
                dragging = false;
            }
            if (dragging) {
                offX.setValue(dragStartOffX + mouseX - dragStartX);
                offY.setValue(dragStartOffY + mouseY - dragStartY);
                x = offX.getValue().floatValue();
                y = offY.getValue().floatValue();
            }
        }

        List<float[]> targetRects = new ArrayList<>();
        renderedCount = 0;

        for (Map.Entry<Integer, Float> entry : sortedEntries) {
            int id = entry.getKey();
            float alpha = entry.getValue();
            if (alpha <= 0.01f) continue;

            LivingEntity target = entityCache.get(id);
            if (target == null) continue;

            int row = renderedCount / 2, col = renderedCount % 2;
            float ox = x + col * (barW + 4 * s);
            float oy = y + row * 37f * s + row * 4f * s;
            float barH = 37f * s;

            targetRects.add(new float[]{ox, oy, barW, barH});
            renderedCount++;
        }

        if (targetRects.isEmpty()) return;

        // OneConfig HUD surface per target, faded by the entry animation.
        float peakAlpha = Math.min(1f, entityAlphas.values().stream().max(Float::compare).orElse(0f));
        int bg = Glass.alpha(Glass.BG,
                Math.round(background.getValue() / 100f * 255f * peakAlpha));
        for (float[] rect : targetRects) {
            Glass.panel(rect[0], rect[1], rect[2], rect[3], bg, Glass.RADIUS * s);
        }

        FontDrawer font = getFontRenderer(context);
        renderedCount = 0;

        for (Map.Entry<Integer, Float> entry : sortedEntries) {
            int id = entry.getKey();
            float alpha = entry.getValue();
            if (alpha <= 0.01f) continue;

            LivingEntity target = entityCache.get(id);
            if (target == null) continue;

            int row = renderedCount / 2, col = renderedCount % 2;
            float ox = x + col * (barW + 4 * s);
            float oy = y + row * 37f * s + row * 4f * s;
            float barH = 37f * s;
            float padding = 2f * s;

            float health = target.getHealth(), maxHealth = target.getMaxHealth();
            float disp = displayHealths.getOrDefault(id, health);
            float del = delayedHealths.getOrDefault(id, health);

            if (health < disp) { disp = health; healthDelayTimer.reset(); }
            else { disp += (health - disp) / 4f; if (Math.abs(disp - health) < 0.01f) disp = health; }
            if (healthDelayTimer.hasTimeElapsed(200L)) {
                del += (disp - del) / 4f; if (Math.abs(del - disp) < 0.01f) del = disp;
            }
            displayHealths.put(id, disp);
            delayedHealths.put(id, del);

            if (headTextures.get(id) == null) {
                headTextures.put(id, getSkin(target));
            }
            Identifier skin = headTextures.get(id);

            float dispWidth = (barW - padding * 2) * (disp / maxHealth);
            float delWidth = (barW - padding * 2) * (del / maxHealth);

            String sheesh = healthFormat.format(Math.abs(mc.player.getHealth() - health));
            String healthDiff = mc.player.getHealth() < health ? "-" + sheesh : "+" + sheesh;

            HUD hud = (HUD) Myau.moduleManager.getModule(HUD.class);
            Color leftC = hud != null ? new Color(hud.custom1.getValue()) : Color.WHITE;
            Color rightC = hud != null ? new Color(hud.custom2.getValue()) : Color.WHITE;

            float barLeft = ox + padding, barRight = ox + barW - padding;
            float barTop = oy + barH - 6f * s, barBottom = oy + barH - 2f * s;
            float barWidth = barRight - barLeft;

            RenderUtil.enableRenderState();
            RenderUtil.drawRect(barLeft, barTop, barRight, barBottom, ColorUtil.darker(new Color(0, 0, 0, (int)(100 * alpha)), 0.3f).getRGB());
            for (int i = 0; i < (int) delWidth; i++) {
                float prog = (float) i / barWidth;
                Color blended = ColorUtil.interpolate(prog, leftC, rightC);
                RenderUtil.drawRect(barLeft + i, barTop, barLeft + i + 1, barBottom, new Color(blended.getRed(), blended.getGreen(), blended.getBlue(), (int)(128 * alpha)).getRGB());
            }
            for (int i = 0; i < (int) dispWidth; i++) {
                float prog = (float) i / barWidth;
                Color blended = ColorUtil.interpolate(prog, leftC, rightC);
                RenderUtil.drawRect(barLeft + i, barTop, barLeft + i + 1, barBottom, new Color(blended.getRed(), blended.getGreen(), blended.getBlue(), (int)(255 * alpha)).getRGB());
            }
            RenderUtil.disableRenderState();

            RenderUtil.enableRenderState();
            font.draw(target.getName().getString(), (int)(ox + padding + 28f * s), (int)(oy + 3f * s), new Color(255, 255, 255, (int)(255 * alpha)).getRGB(), false);
            font.draw(healthDiff, (int)(ox + barW - padding - font.getWidth(healthDiff)), (int)(oy + barH - 14f * s - padding), new Color(200, 200, 200, (int)(255 * alpha)).getRGB(), false);

            if (head.getValue() && skin != null) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
                int hx = Math.round(ox + padding);
                int hy = Math.round(oy + padding);
                int hs = Math.round(26f * s);
                context.drawTexture(RenderLayer::getGuiTextured, skin, hx, hy, 8.0F, 8.0F, hs, hs, 64, 64);
                context.drawTexture(RenderLayer::getGuiTextured, skin, hx, hy, 40.0F, 8.0F, hs, hs, 64, 64);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }

            List<ItemStack> items = new ArrayList<>();
            ItemStack held = target.getMainHandStack();
            if (!held.isEmpty()) items.add(held);
            for (int index = 3; index >= 0; index--) {
                EquipmentSlot slot;
                switch (index) {
                    case 3:
                        slot = EquipmentSlot.HEAD;
                        break;
                    case 2:
                        slot = EquipmentSlot.CHEST;
                        break;
                    case 1:
                        slot = EquipmentSlot.LEGS;
                        break;
                    default:
                        slot = EquipmentSlot.FEET;
                        break;
                }
                ItemStack stack = target.getEquippedStack(slot);
                if (!stack.isEmpty()) items.add(stack);
            }
            float itemX = ox + 28f * s + padding;
            for (ItemStack stack : items) {
                float itemScale = 0.7f * s;
                renderHudItem(context, stack, itemX, oy + 14f * s + padding, itemScale, alpha);
                itemX += 12f * s;
            }
            RenderUtil.disableRenderState();

            renderedCount++;
        }
    }

    private void renderHudItem(DrawContext context, ItemStack stack, float x, float y, float itemScale, float alpha) {
        if (stack == null || stack.isEmpty()) return;

        float clampedAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
        ItemStack renderStack = clampedAlpha < 0.99F ? stripGlintForAlpha(stack) : stack;

        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0.0F);
        matrices.scale(itemScale, itemScale, 1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, clampedAlpha);
        context.drawItem(renderStack, 0, 0);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        matrices.pop();
    }

    private ItemStack stripGlintForAlpha(ItemStack stack) {
        if (!stack.hasGlint()) return stack;
        ItemStack copy = stack.copy();
        copy.remove(DataComponentTypes.ENCHANTMENTS);
        copy.remove(DataComponentTypes.STORED_ENCHANTMENTS);
        return copy;
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

    @Override
    public String[] getSuffix() {
        return new String[]{style.getModeString()};
    }

    public LivingEntity getTarget() {
        return this.target;
    }
}
