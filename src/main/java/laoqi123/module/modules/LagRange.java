package laoqi123.module.modules;

import laoqi123.Myau;
import laoqi123.enums.BlinkModules;
import laoqi123.event.EventTarget;
import laoqi123.event.types.Priority;
import laoqi123.events.PacketEvent;
import laoqi123.events.Render3DEvent;
import laoqi123.events.TickEvent;
import laoqi123.mixin.ClientPlayerInteractionManagerAccessor;
import laoqi123.module.Module;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.property.properties.ModeProperty;
import laoqi123.util.ItemUtil;
import laoqi123.util.RenderUtil;
import laoqi123.util.RotationUtil;
import laoqi123.util.TeamUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class LagRange extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Delay Blink", "Lag"});
    public final IntProperty blinkTick = new IntProperty("Blink Tick", 3, 0, 10, () -> mode.getValue() == 0);
    public final IntProperty delay = new IntProperty("Delay", 150, 0, 1000, () -> mode.getValue() == 1);
    public final FloatProperty range = new FloatProperty("Range", 10.0F, 3.0F, 100.0F);
    public final BooleanProperty weaponsOnly = new BooleanProperty("Weapons Only", true);
    public final BooleanProperty allowTools = new BooleanProperty("Allow Tools", false, this.weaponsOnly::getValue);
    public final BooleanProperty botCheck = new BooleanProperty("Bot Check", true);
    public final BooleanProperty teams = new BooleanProperty("Teams", true);
    public final ModeProperty showPosition = new ModeProperty("Show Position", 0, new String[]{"None", "Default", "Hud"});
    private int tickIndex = -1;
    private long delayCounter = 0L;
    private boolean hasTarget = false;
    private Vec3d lastPosition = null;
    private Vec3d currentPosition = null;

    public LagRange() {
        super("LagRange", false);
    }

    private boolean isValidTarget(PlayerEntity playerEntity) {
        if (playerEntity != mc.player && playerEntity != mc.player.getVehicle()) {
            if (playerEntity == mc.getCameraEntity() || playerEntity == mc.getCameraEntity().getVehicle()) {
                return false;
            } else if (playerEntity.deathTime > 0) {
                return false;
            } else if (TeamUtil.isFriend(playerEntity)) {
                return false;
            } else {
                return (!this.teams.getValue() || !TeamUtil.isSameTeam(playerEntity)) && (!this.botCheck.getValue() || !TeamUtil.isBot(playerEntity));
            }
        } else {
            return false;
        }
    }

    private boolean shouldResetOnPacket(Packet<?> packet) {
        if (packet instanceof PlayerInteractEntityC2SPacket) {
            return true;
        } else if (packet instanceof PlayerActionC2SPacket) {
            return ((PlayerActionC2SPacket) packet).getAction() != PlayerActionC2SPacket.Action.RELEASE_USE_ITEM;
        } else if (packet instanceof PlayerInteractBlockC2SPacket) {
            ItemStack item = mc.player.getMainHandStack();
            return item.isEmpty() || !(item.getItem() instanceof SwordItem);
        } else {
            return false;
        }
    }

    @EventTarget(Priority.LOW)
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            switch (event.getType()) {
                case PRE:
                    KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
                    if (killAura != null && killAura.isEnabled() && killAura.shouldAutoBlock() && mode.getValue() == 0) {
                        Myau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                        return;
                    }
                    Myau.lagManager.setDelay(0);
                    this.hasTarget = false;

                    BedNuker bedNuker = (BedNuker) Myau.moduleManager.getModule(BedNuker.class);
                    boolean bedNukerActive = bedNuker != null && bedNuker.isEnabled() && bedNuker.isReady();

                    if ((!bedNukerActive)
                            && !((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getIsHittingBlock()
                            && (!mc.player.isUsingItem() || mc.player.isBlocking())
                            && (
                            !this.weaponsOnly.getValue()
                                    || ItemUtil.hasRawUnbreakingEnchant()
                                    || this.allowTools.getValue() && ItemUtil.isHoldingTool()
                    )) {
                        List<PlayerEntity> players = mc.world.getPlayers().stream()
                                .map(entity -> (PlayerEntity) entity)
                                .filter(this::isValidTarget)
                                .collect(Collectors.toList());
                        if (players.isEmpty()) {
                            Myau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                            this.tickIndex = -1;
                        } else {
                            double height = mc.player.getStandingEyeHeight();
                            Vec3d eyePosition = Myau.lagManager.getLastPosition().add(0.0, height, 0.0);
                            Vec3d targetEyePosition = new Vec3d(mc.player.prevX, mc.player.prevY + height, mc.player.prevZ);
                            Vec3d playerEyePosition = new Vec3d(mc.player.getX(), mc.player.getY() + height, mc.player.getZ());
                            for (PlayerEntity player : players) {
                                double distance = RotationUtil.distanceToBox(player, playerEyePosition);
                                if (!(distance > this.range.getValue())) {
                                    double targetDist = RotationUtil.distanceToBox(player, targetEyePosition);
                                    double eyeDist = RotationUtil.distanceToBox(player, eyePosition);
                                    if (distance < targetDist || distance < eyeDist) {
                                        if (this.tickIndex < 0) {
                                            this.tickIndex = 0;
                                            for (this.delayCounter = this.delayCounter + (long) this.delay.getValue();
                                                 this.delayCounter > 0L;
                                                 this.delayCounter = this.delayCounter - 50
                                            ) {
                                                this.tickIndex++;
                                            }
                                        }
                                        if (mode.getValue() == 1) {
                                            Myau.lagManager.setDelay(this.tickIndex);
                                        }
                                        if (mode.getValue() == 0) {
                                            Myau.blinkManager.setBlinkState(true, BlinkModules.BLINK);
                                            if (Myau.blinkManager.countMovement() > blinkTick.getValue().longValue()) {
                                                Myau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                                            }
                                        }
                                        this.hasTarget = true;
                                        return;
                                    }
                                }
                            }
                        }
                    } else {
                        this.tickIndex = -1;
                        Myau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                    }
                    if (!hasTarget) {
                        Myau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                    }
                    break;
                case POST:
                    Vec3d savedPosition = Myau.lagManager.getLastPosition();
                    if (this.currentPosition == null) {
                        this.lastPosition = savedPosition;
                    } else {
                        this.lastPosition = this.currentPosition;
                    }
                    this.currentPosition = savedPosition;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            if (this.shouldResetOnPacket(event.getPacket())) {
                Myau.lagManager.setDelay(0);
                this.tickIndex = -1;
            }
        }
    }

    @EventTarget(Priority.HIGH)
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled()) {
            if (this.showPosition.getValue() != 0
                    && !mc.options.getPerspective().isFirstPerson()
                    && this.hasTarget
                    && this.lastPosition != null
                    && this.currentPosition != null) {
                Color color = new Color(-1);
                switch (this.showPosition.getValue()) {
                    case 1:
                        color = TeamUtil.getTeamColor(mc.player, 1.0F);
                        break;
                    case 2:
                        HUD hud = (HUD) Myau.moduleManager.getModule(HUD.class);
                        if (hud != null) {
                            color = hud.getColor(System.currentTimeMillis());
                        }
                        break;
                }
                double x = RenderUtil.lerpDouble(this.currentPosition.x, this.lastPosition.x, event.getPartialTicks());
                double y = RenderUtil.lerpDouble(this.currentPosition.y, this.lastPosition.y, event.getPartialTicks());
                double z = RenderUtil.lerpDouble(this.currentPosition.z, this.lastPosition.z, event.getPartialTicks());
                float size = 0.1F;
                float width = mc.player.getWidth();
                float height = mc.player.getHeight();
                Box aabb = new Box(
                        x - (double) width / 2.0,
                        y,
                        z - (double) width / 2.0,
                        x + (double) width / 2.0,
                        y + (double) height,
                        z + (double) width / 2.0
                )
                        .expand(size, size, size)
                        .offset(
                                -mc.gameRenderer.getCamera().getPos().x,
                                -mc.gameRenderer.getCamera().getPos().y,
                                -mc.gameRenderer.getCamera().getPos().z
                        );
                RenderUtil.enableRenderState();
                RenderUtil.drawFilledBox(aabb, color.getRed(), color.getGreen(), color.getBlue());
                RenderUtil.disableRenderState();
            }
        }
    }

    @Override
    public void onDisabled() {
        Myau.lagManager.setDelay(0);
        this.tickIndex = -1;
        this.delayCounter = 0L;
        this.hasTarget = false;
        this.lastPosition = null;
        this.currentPosition = null;
    }

    @Override
    public String[] getSuffix() {
        if (this.mode.getValue() == 1) {
            return new String[]{String.format("%dms", this.delay.getValue())};
        } else {
            return new String[]{blinkTick.getValue().toString()};
        }
    }
}
