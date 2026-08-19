package laoqi123.module.modules.misc;

import laoqi123.Myau;
import laoqi123.enums.ChatColors;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.event.types.Priority;
import laoqi123.events.LoadWorldEvent;
import laoqi123.events.PacketEvent;
import laoqi123.events.Render2DEvent;
import laoqi123.events.TickEvent;
import laoqi123.module.Module;
import laoqi123.util.ChatUtil;
import laoqi123.util.ColorUtil;
import laoqi123.util.SoundUtil;
import laoqi123.util.TeamUtil;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.property.properties.FloatProperty;
import laoqi123.property.properties.IntProperty;
import laoqi123.property.properties.ModeProperty;
import laoqi123.property.properties.TextProperty;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class BedTracker extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final ScheduledExecutorService executor;
    private final LinkedHashMap<String, Long> alertCooldowns;
    private final LinkedHashSet<EnderPearlEntity> trackedPearls;
    private final LinkedHashSet<String> whitelistedPlayers;
    private final Color wBed;
    private final Color rBed;
    private final Color yBed;
    private final Color gBed;
    private BlockPos bedPos;
    private long lastMarcoTime;
    private boolean waiting;
    public final BooleanProperty alerts;
    public final IntProperty alertRange;
    public final BooleanProperty alertOnPearl;
    public final ModeProperty alertSound;
    public final IntProperty alertFrequency;
    public final BooleanProperty marco;
    public final IntProperty marcoRange;
    public final BooleanProperty marcoOnPreal;
    public final TextProperty marcoText;
    public final IntProperty marcoDelay;
    public final BooleanProperty hud;
    public final ModeProperty hudPosX;
    public final ModeProperty hudPosY;
    public final IntProperty hudOffX;
    public final IntProperty hudOffY;
    public final FloatProperty hudScale;
    public final BooleanProperty hudShadow;

    private void playAlertSound() {
        switch (this.alertSound.getValue()) {
            case 1:
                SoundUtil.playSound("mob.cat.meow");
                break;
            case 2:
                SoundUtil.playSound("random.anvil_land");
        }
    }

    private Color getHudColor(int distance) {
        if (distance < 0) {
            return this.wBed;
        } else if (distance <= 100) {
            return this.gBed;
        } else if (distance <= 114) {
            return ColorUtil.interpolate((float) (114 - distance) / 14.0F, this.yBed, this.gBed);
        } else {
            return distance <= 128 ? ColorUtil.interpolate((float) (128 - distance) / 14.0F, this.rBed, this.yBed) : this.rBed;
        }
    }

    private boolean isBed(BlockPos blockPos) {
        return blockPos != null && mc.world.getBlockState(blockPos).isOf(Blocks.RED_BED);
    }

    public BedTracker() {
        super("BedTracker", false, true);
        this.executor = Executors.newScheduledThreadPool(1);
        this.alertCooldowns = new LinkedHashMap<>();
        this.trackedPearls = new LinkedHashSet<>();
        this.whitelistedPlayers = new LinkedHashSet<>();
        this.wBed = new Color(ChatColors.WHITE.toAwtColor());
        this.rBed = new Color(ChatColors.RED.toAwtColor());
        this.yBed = new Color(ChatColors.YELLOW.toAwtColor());
        this.gBed = new Color(ChatColors.GREEN.toAwtColor());
        this.bedPos = null;
        this.lastMarcoTime = -1L;
        this.waiting = false;
        this.alerts = new BooleanProperty("alerts", true);
        this.alertRange = new IntProperty("alerts-range", 48, 8, 128, this.alerts::getValue);
        this.alertOnPearl = new BooleanProperty("alerts-on-pearl", true);
        this.alertSound = new ModeProperty("alerts-sound", 1, new String[]{"NONE", "MEOW", "ANVIL"}, () -> this.alerts.getValue() || this.alertOnPearl.getValue());
        this.alertFrequency = new IntProperty("alerts-frequency", 5, 1, 30, () -> this.alerts.getValue() || this.alertOnPearl.getValue());
        this.marco = new BooleanProperty("macro", false);
        this.marcoRange = new IntProperty("macro-range", 24, 8, 128, this.marco::getValue);
        this.marcoOnPreal = new BooleanProperty("macro-on-pearl", false);
        this.marcoText = new TextProperty("macro-text", "/lobby", () -> this.marco.getValue() || this.marcoOnPreal.getValue());
        this.marcoDelay = new IntProperty("macro-delay", 1, 1, 10, () -> this.marco.getValue() || this.marcoOnPreal.getValue());
        this.hud = new BooleanProperty("hud", true);
        this.hudPosX = new ModeProperty("hud-position-x", 0, new String[]{"LEFT", "MIDDLE", "RIGHT"}, this.hud::getValue);
        this.hudPosY = new ModeProperty("hud-position-y", 0, new String[]{"TOP", "MIDDLE", "BOTTOM"}, this.hud::getValue);
        this.hudOffX = new IntProperty("hud-offset-x", 2, 0, 255, this.hud::getValue);
        this.hudOffY = new IntProperty("hud-offset-y", 2, 0, 255, this.hud::getValue);
        this.hudScale = new FloatProperty("hud-scale", 1.0F, 0.5F, 1.5F, this.hud::getValue);
        this.hudShadow = new BooleanProperty("hud-shadow", true, this.hud::getValue);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST && this.isBed(this.bedPos)) {
            long millis = System.currentTimeMillis();
            boolean pearl = false;
            boolean marco = false;
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EnderPearlEntity) {
                    EnderPearlEntity enderPearl = (EnderPearlEntity) entity;
                    if (!this.trackedPearls.contains(enderPearl)) {
                        this.trackedPearls.add(enderPearl);
                        if (this.alertOnPearl.getValue()) {
                            ChatUtil.sendFormatted(String.format("%s%s: &fDetected &5Ender Pearl&r &e&l!", Myau.clientName, this.getName()));
                            pearl = true;
                        }
                        if (this.marcoOnPreal.getValue() && this.lastMarcoTime + (long) this.marcoDelay.getValue() * 1000L <= millis) {
                            this.lastMarcoTime = millis;
                            marco = true;
                        }
                    }
                }
            }
            for (PlayerEntity player : StreamSupport.stream(mc.world
                    .getEntities()
                    .spliterator(), false)
                    .filter(entity -> entity instanceof PlayerEntity)
                    .map(entity -> (PlayerEntity) entity)
                    .filter(entityPlayer -> !TeamUtil.isBot(entityPlayer) && !this.whitelistedPlayers.contains(entityPlayer.getName()))
                    .collect(Collectors.toList())) {
                if (TeamUtil.isSameTeam(player)) {
                    this.whitelistedPlayers.add(player.getName().getString());
                } else {
                    double distance = Math.sqrt(player.squaredDistanceTo((double) this.bedPos.getX() + 0.5, (double) this.bedPos.getY() + 0.5, (double) this.bedPos.getZ() + 0.5));
                    String name = player.getName().getString();
                    String text = player.getDisplayName().getString();
                    ItemStack item = player.getMainHandStack();
                    boolean isPearl = item != null && item.isOf(Items.ENDER_PEARL);
                    if (this.alerts.getValue() && distance < (double) this.alertRange.getValue()) {
                        Long cooldown = this.alertCooldowns.get(name);
                        if (cooldown == null || cooldown + (long) this.alertFrequency.getValue() * 1000L <= millis) {
                            this.alertCooldowns.put(name, millis);
                            ChatUtil.sendFormatted(
                                    String.format("%s%s: %s&r &fis %d blocks away from your bed &e&l!", Myau.clientName, this.getName(), text, (int) distance + 1)
                            );
                            pearl = true;
                        }
                    }
                    if (this.alertOnPearl.getValue() && isPearl) {
                        Long cooldown = this.alertCooldowns.get(name);
                        if (cooldown == null || cooldown + (long) this.alertFrequency.getValue() * 1000L <= millis) {
                            this.alertCooldowns.put(name, millis);
                            ChatUtil.sendFormatted(
                                    String.format("%s%s: %s&r &fhas &5Ender Pearl&r &e&l!", Myau.clientName, this.getName(), text)
                            );
                            pearl = true;
                        }
                    }
                    if ((
                            this.marco.getValue() && distance < (double) this.marcoRange.getValue()
                                    || this.marcoOnPreal.getValue() && isPearl
                    )
                            && this.lastMarcoTime + (long) this.marcoDelay.getValue() * 1000L <= millis) {
                        this.lastMarcoTime = millis;
                        marco = true;
                    }
                }
            }
            if (pearl) {
                this.playAlertSound();
            }
            if (marco) {
                ChatUtil.sendRaw(
                        String.format(
                                ChatColors.formatColor("%s%s: &fRunning &6%s&r"),
                                ChatColors.formatColor(Myau.clientName),
                                this.getName(),
                                this.marcoText.getValue()
                        )
                );
                ChatUtil.sendMessage(this.marcoText.getValue());
            }
        }
    }

    @EventTarget(Priority.LOW)
    public void onRender(Render2DEvent event) {
        if (this.isEnabled() && this.hud.getValue()) {
            if (mc.world != null && mc.player != null && !mc.inGameHud.getDebugHud().shouldShowDebugHud()) {
                if (mc.currentScreen == null || mc.currentScreen instanceof ChatScreen) {
                    int distanceSq = 0;
                    boolean hasBed = this.isBed(this.bedPos);
                    if (hasBed) {
                        double xDiff = mc.player.getX() - (double) this.bedPos.getX();
                        double zDiff = mc.player.getZ() - (double) this.bedPos.getZ();
                        distanceSq = (int) Math.sqrt(xDiff * xDiff + zDiff * zDiff) + 1;
                    }
                    String text = ChatColors.formatColor(
                            String.format(
                                    "&fBed: %s%s",
                                    !hasBed ? "&cfalse&r" : "&atrue&r",
                                    !hasBed ? "" : String.format(" &7| &fDistance: &r%d%s", distanceSq, distanceSq >= 128 ? " &c&l!" : "")
                            )
                    );
                    float width = (float) mc.textRenderer.getWidth(text);
                    float height = (float) mc.textRenderer.fontHeight - 1.0F;
                    float scale = (float) this.hudOffX.getValue() / this.hudScale.getValue();
                    switch (this.hudPosX.getValue()) {
                        case 0:
                            scale++;
                            break;
                        case 1:
                            scale += (float) mc.getWindow().getScaledWidth() / this.hudScale.getValue() / 2.0F - width / 2.0F;
                            break;
                        case 2:
                            scale = (scale + 1.0F) * -1.0F;
                            scale += (float) mc.getWindow().getScaledWidth() / this.hudScale.getValue() - width;
                    }
                    float offset = (float) this.hudOffY.getValue() / this.hudScale.getValue();
                    switch (this.hudPosY.getValue()) {
                        case 0:
                            offset++;
                            break;
                        case 1:
                            offset += (float) mc.getWindow().getScaledHeight() / this.hudScale.getValue() / 2.0F - height / 2.0F;
                            break;
                        case 2:
                            offset = (offset + 1.0F) * -1.0F;
                            offset += (float) mc.getWindow().getScaledHeight() / this.hudScale.getValue() - height;
                    }
                    MatrixStack matrices = event.getContext().getMatrices();
                    matrices.push();
                    matrices.scale(this.hudScale.getValue(), this.hudScale.getValue(), 1.0F);
                    matrices.translate(scale, offset, 0.0F);
                    event.getContext().drawText(mc.textRenderer, text, 0, 0, this.getHudColor(distanceSq).getRGB(), this.hudShadow.getValue());
                    matrices.pop();
                }
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.waiting = false;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            if (event.getPacket() instanceof ChatMessageS2CPacket) {
                ChatMessageS2CPacket packet = (ChatMessageS2CPacket) event.getPacket();
                String msg = null;
                if (packet.unsignedContent() != null) {
                    msg = packet.unsignedContent().getString();
                } else if (packet.body() != null && packet.body().content() != null) {
                    msg = packet.body().content();
                }
                if (msg != null && (msg.contains("§e§lProtect your bed and destroy the enemy bed") || msg.contains("§e§lDestroy the enemy bed and then eliminate them"))) {
                    this.alertCooldowns.clear();
                    this.trackedPearls.clear();
                    this.whitelistedPlayers.clear();
                    this.bedPos = null;
                    this.waiting = true;
                }
            }
            if (event.getPacket() instanceof PlayerPositionLookS2CPacket && this.waiting) {
                this.waiting = false;
                this.executor
                        .schedule(
                                () -> {
                                    ClientPlayerEntity player = mc.player;
                                    if (player == null) {
                                        return;
                                    }
                                    int x = MathHelper.floor(player.getX());
                                    int y = MathHelper.floor(player.getY() + (double) player.getStandingEyeHeight());
                                    int z = MathHelper.floor(player.getZ());
                                    for (int i = x - 25; i <= x + 25; i++) {
                                        for (int j = y - 25; j <= y + 25; j++) {
                                            for (int k = z - 25; k <= z + 25; k++) {
                                                BlockPos blockPos = new BlockPos(i, j, k);
                                                if (this.isBed(blockPos)) {
                                                    this.bedPos = blockPos;
                                                    ChatUtil.sendFormatted(
                                                            String.format(
                                                                    "%s%s: &fWhitelisted your bed at (%d, %d, %d) &a&l!",
                                                                    Myau.clientName,
                                                                    this.getName(),
                                                                    this.bedPos.getX(),
                                                                    this.bedPos.getY(),
                                                                    this.bedPos.getZ()
                                                            )
                                                    );
                                                    SoundUtil.playSound("note.pling");
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                },
                                3000L,
                                TimeUnit.MILLISECONDS
                        );
            }
        }
    }

    @Override
    public void onDisabled() {
        this.alertCooldowns.clear();
        this.trackedPearls.clear();
        this.whitelistedPlayers.clear();
        this.bedPos = null;
    }
}
