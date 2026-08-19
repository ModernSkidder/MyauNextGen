package laoqi123.module.modules.combat;

import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.events.LeftClickMouseEvent;
import laoqi123.events.TickEvent;
import laoqi123.module.Module;
import laoqi123.property.Property;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.util.*;
import laoqi123.util.clicking.Clicker;
import laoqi123.util.config.Configurable;
import laoqi123.util.config.PropertyProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.GameMode;

import java.util.List;
import java.util.Objects;

public class AutoClicker extends Module implements PropertyProvider {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final Configurable rootConfig = new Configurable("AutoClicker");
    private boolean clickPending = false;
    private boolean blockHitPending = false;
    private long blockHitDelay = 0L;
    public final IntProperty minCPS = new IntProperty("min-cps", 8, 1, 20);
    public final IntProperty maxCPS = new IntProperty("max-cps", 12, 1, 20);
    public final Clicker clicker;
    public final BooleanProperty blockHit = new BooleanProperty("block-hit", false);
    public final FloatProperty blockHitTicks = new FloatProperty("block-hit-ticks", 1.5F, 1.0F, 20.0F, this.blockHit::getValue);
    public final BooleanProperty weaponsOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, this.weaponsOnly::getValue);
    public final BooleanProperty breakBlocks = new BooleanProperty("break-blocks", true);
    public final FloatProperty range = new FloatProperty("range", 3.0F, 3.0F, 8.0F, this.breakBlocks::getValue);
    public final FloatProperty hitBoxVertical = new FloatProperty("hit-box-vertical", 0.1F, 0.0F, 1.0F, this.breakBlocks::getValue);
    public final FloatProperty hitBoxHorizontal = new FloatProperty("hit-box-horizontal", 0.2F, 0.0F, 1.0F, this.breakBlocks::getValue);

    public AutoClicker() {
        super("AutoClicker", false);
        this.clicker = new Clicker("AutoClickerClicker", this.minCPS, this.maxCPS);
        this.rootConfig.setRunningOverride(this::isEnabled);
        this.rootConfig.addChild(this.clicker);
    }

    private int getKeyCode(KeyBinding keyBinding) {
        InputUtil.Key key = InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey());
        int code = key.getCode();
        return key.getCategory() == InputUtil.Type.MOUSE ? code - 100 : code;
    }

    private long getBlockHitDelay() {
        return (long) (50.0F * this.blockHitTicks.getValue());
    }

    private boolean isBreakingBlock() {
        return mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK;
    }

    private boolean canClick() {
        if (!this.weaponsOnly.getValue()
                || ItemUtil.hasRawUnbreakingEnchant()
                || this.allowTools.getValue() && ItemUtil.isHoldingTool()) {
            if (this.breakBlocks.getValue() && this.isBreakingBlock() && !this.hasValidTarget()) {
                GameMode gameType = mc.interactionManager.getCurrentGameMode();
                return gameType != GameMode.SURVIVAL && gameType != GameMode.CREATIVE;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean isValidTarget(PlayerEntity entityPlayer) {
        if (entityPlayer != mc.player && entityPlayer != mc.player.getVehicle()) {
            if (entityPlayer == mc.getCameraEntity() || entityPlayer == mc.getCameraEntity().getVehicle()) {
                return false;
            } else if (entityPlayer.deathTime > 0) {
                return false;
            } else {
                float borderSize = entityPlayer.getTargetingMargin();
                return RotationUtil.rayTrace(entityPlayer.getBoundingBox().expand(
                        borderSize + this.hitBoxHorizontal.getValue(),
                        borderSize + this.hitBoxVertical.getValue(),
                        borderSize + this.hitBoxHorizontal.getValue()
                ), mc.player.getYaw(), mc.player.getPitch(), this.range.getValue()) != null;
            }
        } else {
            return false;
        }
    }

    private boolean hasValidTarget() {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity && this.isValidTarget((PlayerEntity) entity)) {
                return true;
            }
        }
        return false;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            this.clicker.tick();
            if (this.blockHitDelay > 0L) {
                this.blockHitDelay -= 50L;
            }
            if (mc.currentScreen != null) {
                this.clickPending = false;
                this.blockHitPending = false;
            } else {
                if (this.clickPending) {
                    this.clickPending = false;
                    KeyBindUtil.updateKeyState(this.getKeyCode(mc.options.attackKey));
                }
                if (this.blockHitPending) {
                    this.blockHitPending = false;
                    KeyBindUtil.updateKeyState(this.getKeyCode(mc.options.useKey));
                }
                if (this.isEnabled() && this.canClick() && KeyBindUtil.isKeyDown(mc.options.attackKey)) {
                    if (!mc.player.isUsingItem()) {
                        this.clicker.click(() -> {
                            KeyBindUtil.setKeyBindState(this.getKeyCode(mc.options.attackKey), false);
                            KeyBindUtil.pressKeyOnce(this.getKeyCode(mc.options.attackKey));
                            this.clickPending = true;
                            return true;
                        });
                    }
                    if (this.blockHit.getValue()
                            && this.blockHitDelay <= 0L
                            && KeyBindUtil.isKeyDown(mc.options.useKey)
                            && ItemUtil.isHoldingSword()) {
                        this.blockHitPending = true;
                        KeyBindUtil.setKeyBindState(this.getKeyCode(mc.options.useKey), false);
                        if (!mc.player.isUsingItem()) {
                            this.blockHitDelay = this.blockHitDelay + this.getBlockHitDelay();
                            KeyBindUtil.pressKeyOnce(this.getKeyCode(mc.options.useKey));
                        }
                    }
                }
            }
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onCLick(LeftClickMouseEvent event) {
        if (this.isEnabled() && !event.isCancelled()) {
            if (!this.clickPending) {
                this.clicker.itemCooldown.newCooldown();
            }
        }
    }

    @Override
    public void onEnabled() {
        this.blockHitDelay = 0L;
        this.clicker.reset();
    }

    @Override
    public void verifyValue(String mode) {
        if (this.minCPS.getName().equals(mode)) {
            if (this.minCPS.getValue() > this.maxCPS.getValue()) {
                this.maxCPS.setValue(this.minCPS.getValue());
            }
        } else {
            if (this.maxCPS.getName().equals(mode) && this.minCPS.getValue() > this.maxCPS.getValue()) {
                this.minCPS.setValue(this.maxCPS.getValue());
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return Objects.equals(this.minCPS.getValue(), this.maxCPS.getValue())
                ? new String[]{this.minCPS.getValue().toString()}
                : new String[]{String.format("%d-%d", this.minCPS.getValue(), this.maxCPS.getValue())};
    }

    @Override
    public List<Property<?>> getAdditionalProperties() {
        return this.rootConfig.collectProperties();
    }
}
