package laoqi123.module.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import laoqi123.Myau;
import laoqi123.enums.ChatColors;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.HitBlockEvent;
import laoqi123.events.LeftClickMouseEvent;
import laoqi123.events.LivingUpdateEvent;
import laoqi123.events.LoadWorldEvent;
import laoqi123.events.MoveInputEvent;
import laoqi123.events.PacketEvent;
import laoqi123.events.Render2DEvent;
import laoqi123.events.Render3DEvent;
import laoqi123.events.RightClickMouseEvent;
import laoqi123.events.SafeWalkEvent;
import laoqi123.events.UpdateEvent;
import laoqi123.mixin.KeyBindingAccessor;
import laoqi123.mixin.PlayerInteractEntityC2SPacketAccessor;
import laoqi123.module.Module;
import laoqi123.module.modules.Eagle;
import laoqi123.property.Property;
import laoqi123.property.properties.BooleanProperty;
import laoqi123.util.BlockUtil;
import laoqi123.util.ChatUtil;
import laoqi123.util.ItemUtil;
import laoqi123.util.KeyBindUtil;
import laoqi123.util.RenderUtil;
import laoqi123.util.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.lwjgl.glfw.GLFW;

public class Telly
extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private DrawContext renderContext;
    private final String scriptName = "Telly";
    public final BooleanProperty autoSwap = new BooleanProperty("auto-swap", true);
    public final BooleanProperty disableSafeWalk = new BooleanProperty("disable-safewalk", true);
    public final BooleanProperty showActivationHitbox = new BooleanProperty("show-activation-hitbox", false);
    public final BooleanProperty print = new BooleanProperty("print", false);
    private final ClientApi client = new ClientApi();
    private final ModulesApi modules = new ModulesApi();
    private final KeybindsApi keybinds = new KeybindsApi();
    private final InventoryApi inventory = new InventoryApi();
    private final WorldApi world = new WorldApi();
    private final RenderApi render = new RenderApi();
    private final GlApi gl = new GlApi();
    private final UtilApi util = new UtilApi();
    private final BridgeApi bridge = new BridgeApi();
    boolean armed = false;
    boolean running = false;
    long activatePromptAt = 0L;
    long promptBrokeAt = 0L;
    float promptAlpha = 0.0f;
    long promptFadeLastAt = 0L;
    int promptFadeRgb = 0xFF5555;
    int[] hitboxLastPos = null;
    int hitboxLastFace = -1;
    int[] activationAnchorPos = null;
    int activationAnchorFace = -1;
    boolean activationMovementHeld = false;
    boolean eagleDisabledForActivation = false;
    boolean eagleWasDisabledByTelly = false;
    boolean antiSwayTapUsed = false;
    HashSet<String> cancelledGhostBlocks = new HashSet();
    boolean tellyAutoPlaceWindow = false;
    boolean autoPlaceDebugActive = false;
    boolean safeWalkStateCaptured = false;
    boolean safeWalkWasEnabled = false;
    int setupTick = 0;
    int cyclePhase = 19;
    float stagedForward = -1.0f;
    float stagedStrafe = -1.0f;
    boolean stagedJump = false;
    boolean stagedSprint = false;
    float baseYaw = 0.0f;
    int travelX = 0;
    int travelZ = 0;
    double antiSwayLane = 0.0;
    float antiSwayYawOffset = 0.0f;
    int bridgeLaneBlock = 0;
    int bridgeStartProgress = 0;
    int[] latestStraightPlacedPos = null;
    boolean firstTellyPlacementPending = false;
    boolean adaptiveAimValid = false;
    float adaptiveAimYaw = 0.0f;
    float adaptiveAimPitch = 0.0f;
    long adaptiveAimUpdatedAt = 0L;
    long takeoverDetectionAt = 0L;
    boolean takeoverCameraValid = false;
    float takeoverCameraYaw = 0.0f;
    float takeoverCameraPitch = 0.0f;
    float takeoverAccumulated = 0.0f;
    long takeoverLastFrameAt = 0L;
    long freezeLastTickAt = 0L;
    boolean ignoreForwardUntilRelease = false;
    boolean ignoreBackUntilRelease = false;
    boolean ignoreLeftUntilRelease = false;
    boolean ignoreRightUntilRelease = false;
    boolean ignoreJumpUntilRelease = false;
    boolean ignoreSneakUntilRelease = false;
    boolean ignoreSprintUntilRelease = false;
    boolean rotationActive = false;
    long rotationStartedAt = 0L;
    long rotationDuration = 50L;
    float rotationStartYaw = 0.0f;
    float rotationStartPitch = 0.0f;
    float rotationTargetYaw = 0.0f;
    float rotationTargetPitch = 0.0f;
    float scriptedRotationYaw = 0.0f;
    float scriptedRotationPitch = 0.0f;
    final int[] YAW_NUDGE_PATTERN = new int[]{0, 1, -1, 2, -2};
    int rotationStepCounter = 0;
    final double ACTIVATION_ACROSS_MIN = 0.38;
    final double ACTIVATION_ACROSS_MAX = 0.65;
    final double ACTIVATION_HEIGHT_MIN = 0.25;
    final double ACTIVATION_HEIGHT_MAX = 0.75;
    final float ACTIVATION_YAW_TOLERANCE = 2.0f;
    float[] yawCurve = new float[]{91.68f, 98.88f, 78.94f, 37.45f, 1.61f, -21.69f, -33.98f, -35.8f, -34.64f, -33.85f, -33.06f, -31.55f, -29.26f, -26.65f, -24.19f, -21.07f, -18.84f, -17.06f, -8.87f, 2.61f, 41.94f};
    float[] pitchCurve = new float[]{64.31f, 59.95f, 60.57f, 61.46f, 60.64f, 58.89f, 56.91f, 56.63f, 58.65f, 61.63f, 64.2f, 66.74f, 68.69f, 70.64f, 73.01f, 75.37f, 77.46f, 78.56f, 78.9f, 77.22f, 72.25f};
    float[] forwardCurve = new float[]{1.0f, 1.0f, 0.0f, 0.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f};
    float[] strafeCurve = new float[]{-1.0f, -1.0f, -1.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, -1.0f, -1.0f, -1.0f};
    final double[] FACE_HIT_OFFSETS = new double[]{0.5, 0.25, 0.75, 0.15, 0.85};
    final double[] EXTENDED_FACE_HIT_OFFSETS = new double[]{0.5, 0.25, 0.75, 0.15, 0.85, 0.35, 0.65, 0.05, 0.95};
    final int[] ALLOWED_PLACE_FACES = new int[]{2, 3, 4, 5, 1};
    final String[] REPLACEABLE_BLOCKS = new String[]{"air", "water", "flowing_water", "lava", "flowing_lava", "fire", "tallgrass", "deadbush", "snow_layer", "double_plant", "vine"};
    final String[] EXPERIMENTAL_REPLACEABLE_BLOCKS = new String[]{"sapling", "yellow_flower", "red_flower", "brown_mushroom", "red_mushroom", "wheat", "carrots", "potatoes", "nether_wart", "reeds"};
    final String[] UNPLACEABLE_EXACT = new String[]{"snow_layer", "web", "sapling", "daylight_detector", "beacon", "banner", "end_portal_frame", "end_portal", "lever", "stone_button", "wooden_button", "skull", "cactus", "double_plant", "waterlily", "carpet", "tripwire_hook", "tallgrass", "yellow_flower", "red_flower", "flower_pot", "sign", "ladder", "torch", "redstone_torch", "unlit_redstone_torch", "gravel", "clay", "sand", "soul_sand", "chest", "trapped_chest", "ender_chest", "furnace", "lit_furnace", "jukebox", "enchanting_table", "dropper", "dispenser", "hopper", "anvil", "noteblock", "crafting_table", "mob_spawner", "brewing_stand", "bed"};
    final String[] UNPLACEABLE_CONTAINS = new String[]{"stairs", "slab", "fence", "pane", "rail", "door", "torch", "pumpkin", "flower", "sapling", "banner", "button", "skull", "web", "carpet", "cactus", "sign", "mushroom"};
    final String[] INTERACTABLE_TYPES = new String[]{"BlockTrapDoor", "BlockDoor", "BlockContainer", "BlockJukebox", "BlockFenceGate", "BlockChest", "BlockEnderChest", "BlockEnchantmentTable", "BlockBrewingStand", "BlockBed", "BlockDropper", "BlockDispenser", "BlockHopper", "BlockAnvil", "BlockNote", "BlockWorkbench", "BlockFurnace", "BlockBeacon", "BlockMobSpawner", "BlockDaylightDetector", "BlockCommandBlock", "BlockStandingSign", "BlockWallSign", "BlockSkull"};
    int currentClientTick = Integer.MIN_VALUE;
    int placementEvaluationTick = Integer.MIN_VALUE;
    int lastPlacementAttemptTick = Integer.MIN_VALUE;
    int lastSuccessfulPlaceTick = Integer.MIN_VALUE;
    int forceSuppressTick = Integer.MIN_VALUE;
    long totalC08Counter = 0L;
    long c08CounterAtTickBoundary = 0L;
    boolean hasLastSentServerPos = false;
    double lastSentServerPosX;
    double lastSentServerPosY;
    double lastSentServerPosZ;
    Object[] cachedCandidate = null;
    int cachedCandidateTick = Integer.MIN_VALUE;
    float cachedCandidateYaw = Float.NaN;
    float cachedCandidatePitch = Float.NaN;
    boolean candidateResolvedThisTick = false;
    int[] lastPlacedPos = null;
    int[] lastSupportPos = null;
    int lastSupportFace = -1;
    List<int[]> cachedBelowTargets = null;
    int cachedBelowTargetsTick = Integer.MIN_VALUE;
    Map<String, Integer> rejectedTargets = new HashMap<String, Integer>();
    int forcedModeCheck = 0;
    boolean useSuppressed = false;
    boolean silentPitchActive = false;
    float silentPitch = 0.0f;
    boolean placingViaModule = false;
    boolean manualC08InWindow = false;

    public Telly() {
        super("Telly", false);
        this.onLoad();
    }

    @Override
    public void onEnabled() {
        this.onEnable();
    }

    @Override
    public void onDisabled() {
        this.onDisable();
    }

    @EventTarget(value=0)
    public void handleUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (event.getType() == EventType.PRE) {
            this.onPreUpdate();
            if (this.running) {
                this.advanceTellyCycle();
                PlayerState state = new PlayerState(event);
                this.onPreMotion(state);
                event.setRotation(state.yaw, state.pitch, 5);
                event.setPervRotation(state.yaw, 5);
            }
        } else if (event.getType() == EventType.POST) {
            this.onPostMotion();
        }
    }

    @EventTarget(value=0)
    public void handleMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            this.applyTellyMovementInput();
        }
    }

    @EventTarget(value=0)
    public void handleLivingUpdate(LivingUpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.running) {
            this.client.setSneak(false);
            this.enforceSafeWalkDisabledForRun();
        }
    }

    @EventTarget(value=0)
    public void handleRender2D(Render2DEvent event) {
        if (this.isEnabled()) {
            this.renderContext = event.getContext();
            this.onRenderTick(event.getPartialTicks());
        }
    }

    @EventTarget(value=0)
    public void handleRender3D(Render3DEvent event) {
        if (this.isEnabled()) {
            this.onRenderWorld(event.getPartialTicks());
        }
    }

    @EventTarget(value=0)
    public void handlePacket(PacketEvent event) {
        SPacket packet;
        if (!this.isEnabled() || event.isCancelled()) {
            return;
        }
        if (event.getType() == EventType.SEND) {
            CPacket packet2 = CPacket.from(event.getPacket());
            if (packet2 != null && !this.onPacketSent(packet2)) {
                event.setCancelled(true);
            }
        } else if (event.getType() == EventType.RECEIVE && (packet = SPacket.from(event.getPacket())) != null && !this.onPacketReceived(packet)) {
            event.setCancelled(true);
        }
    }

    @EventTarget(value=0)
    public void handleWorldLoad(LoadWorldEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        this.stopAutomation(false);
        this.autoPlaceOnWorldJoin(this.client.getPlayer());
    }

    @EventTarget(value=0)
    public void handleLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled() && !this.onMouse(0, true, 0, 0)) {
            event.setCancelled(true);
        }
    }

    @EventTarget(value=0)
    public void handleRightClick(RightClickMouseEvent event) {
        if (this.isEnabled() && !this.onMouse(1, true, 0, 0)) {
            event.setCancelled(true);
        }
    }

    @EventTarget(value=0)
    public void handleHitBlock(HitBlockEvent event) {
        if (this.isEnabled() && !this.onMouse(0, true, 0, 0)) {
            event.setCancelled(true);
        }
    }

    @EventTarget(value=0)
    public void handleSafeWalk(SafeWalkEvent event) {
        if (this.isEnabled() && this.running && ((Boolean)this.disableSafeWalk.getValue()).booleanValue()) {
            event.setSafeWalk(false);
        }
    }

    void onLoad() {
        this.modules.registerDescription("Decrypted");
        this.modules.registerButton("Auto swap", true);
        this.modules.registerButton("Disable SafeWalk", true);
        this.modules.registerButton("Show activation hitbox", false);
    }

    void onEnable() {
        this.autoPlaceOnEnable();
        this.armAutomation();
    }

    void onDisable() {
        this.stopAutomation(false);
        this.autoPlaceOnDisable();
    }

    void onWorldJoin(Entity entity) {
        if (entity != null && entity.isUser) {
            this.stopAutomation(false);
        }
        this.autoPlaceOnWorldJoin(entity);
    }

    void onPreUpdate() {
        this.enforceSafeWalkDisabledForRun();
        if (this.running) {
            this.keybinds.setPressed("attack", false);
            this.applySmoothedRotation();
            this.holdScriptedRotation();
        }
        if (this.armed && !this.running) {
            this.updateActivationPrompt();
        }
        if (!this.running) {
            return;
        }
        long freezeNow = this.client.time();
        if (this.freezeLastTickAt != 0L && freezeNow - this.freezeLastTickAt > 300L) {
            this.stopAutomation(true);
            return;
        }
        this.freezeLastTickAt = freezeNow;
        Entity player = this.client.getPlayer();
        if (player == null || player.isDead() || player.getFallDistance() > 7.0f) {
            this.stopAutomation(true);
            return;
        }
        this.handleAutoSwap(player);
        if (!player.isHoldingBlock()) {
            this.stopAutomation(true);
            return;
        }
        if (this.firstTellyPlacementPending) {
            this.updateAdaptivePlacementAim(player);
        }
        this.autoPlaceOnPreUpdate();
        if (this.firstTellyPlacementPending) {
            this.updateAdaptivePlacementAim(player);
        }
    }

    float activationPitch() {
        return 75.0f;
    }

    void handleAutoSwap(Entity player) {
        int heldCount;
        if (!this.modules.getButton("Telly", "Auto swap")) {
            return;
        }
        int threshold = 5;
        ItemStack held = player.getHeldItem();
        int n = heldCount = held != null && this.isUsableBlockStack(held) ? held.stackSize : 0;
        if (heldCount > threshold) {
            return;
        }
        int bestSlot = -1;
        int bestSize = heldCount;
        for (int slot = 0; slot <= 8; ++slot) {
            ItemStack stack;
            if (slot == this.inventory.getSlot() || !this.isUsableBlockStack(stack = this.inventory.getStackInSlot(slot)) || stack.stackSize <= bestSize) continue;
            bestSize = stack.stackSize;
            bestSlot = slot;
        }
        if (bestSlot != -1) {
            this.inventory.setSlot(bestSlot);
        }
    }

    boolean activationPromptReady() {
        return this.activatePromptAt != 0L && this.client.time() - this.activatePromptAt >= 1000L;
    }

    boolean activationSuppressUse() {
        return this.activatePromptAt != 0L && this.client.time() - this.activatePromptAt >= 850L;
    }

    void updateActivationPrompt() {
        boolean atEdge;
        Entity player = this.client.getPlayer();
        if (player == null || !this.client.getScreen().isEmpty()) {
            this.clearActivationPrompt();
            return;
        }
        this.setActivationMovementHold(this.activationPromptReady() && this.keybinds.isMouseDown(1));
        boolean lookingDown = player.getPitch() >= this.activationPitch();
        boolean bl = atEdge = lookingDown && this.isLookingAtEdge(player);
        if (this.client.isSneak() && atEdge) {
            if (this.activatePromptAt == 0L) {
                this.activatePromptAt = this.client.time();
            }
            this.promptBrokeAt = 0L;
            this.captureActivationAnchor(player);
            if (this.activationSuppressUse()) {
                this.keybinds.setPressed("use", false);
            }
            if (this.activationPromptReady()) {
                this.disableEagleForActivation();
            }
            if (this.activationPromptReady() && this.keybinds.isMouseDown(1)) {
                this.disableSafeWalkForRun();
                this.enforceSafeWalkDisabledForRun();
            } else if (this.safeWalkStateCaptured) {
                this.restoreSafeWalkState();
            }
            return;
        }
        if (this.activatePromptAt == 0L) {
            return;
        }
        if (!this.activationPromptReady()) {
            this.clearActivationPrompt();
            return;
        }
        if (this.promptBrokeAt == 0L) {
            this.rememberActivationPromptColor();
            this.promptBrokeAt = this.client.time();
        }
        this.keybinds.setPressed("use", false);
        if (!this.client.isSneak() && this.keybinds.isMouseDown(1) && this.isActivationYawAligned(player.getYaw())) {
            this.rememberActivationPromptColor();
            this.activatePromptAt = 0L;
            this.promptBrokeAt = 0L;
            this.beginAutomation();
            if (!this.running) {
                this.keybinds.setPressed("use", false);
            }
            return;
        }
        if (this.client.time() - this.promptBrokeAt > 300L) {
            this.clearActivationPrompt();
        }
    }

    void clearActivationPrompt() {
        this.rememberActivationPromptColor();
        if (this.activationSuppressUse()) {
            this.keybinds.setPressed("use", false);
        }
        this.activatePromptAt = 0L;
        this.promptBrokeAt = 0L;
        this.activationAnchorPos = null;
        this.activationAnchorFace = -1;
        this.eagleDisabledForActivation = false;
        this.setActivationMovementHold(false);
        if (!this.running) {
            this.restoreSafeWalkState();
        }
    }

    void rememberActivationPromptColor() {
        if (this.activatePromptAt != 0L) {
            this.promptFadeRgb = this.activationPromptReady() ? 0x55FF55 : 0xFF5555;
        }
    }

    int[] travelDirectionFromYaw(float yaw) {
        double radians = Math.toRadians(yaw);
        double rawX = Math.sin(radians) - Math.cos(radians);
        double rawZ = -Math.cos(radians) - Math.sin(radians);
        if (Math.abs(rawX) >= Math.abs(rawZ)) {
            return new int[]{rawX >= 0.0 ? 1 : -1, 0};
        }
        return new int[]{0, rawZ >= 0.0 ? 1 : -1};
    }

    boolean isLookingAtEdge(Entity player) {
        int travelFace;
        if (!this.isActivationYawAligned(player.getYaw())) {
            return false;
        }
        Object[] hit = this.client.raycastBlock(4.5);
        if (hit == null || hit.length < 3 || hit[0] == null || hit[1] == null || hit[2] == null) {
            return false;
        }
        int face = this.faceFromName((String)hit[2]);
        if (face < 2) {
            return false;
        }
        if (!this.isInActivationFaceCenter(face, (Vec3)hit[1])) {
            return false;
        }
        int[] travel = this.travelDirectionFromYaw(player.getYaw());
        travelFace = travel[0] > 0 ? 5 : (travel[0] < 0 ? 4 : (travel[1] > 0 ? 3 : 2));
        int n = travelFace;
        if (face != travelFace) {
            return false;
        }
        int[] pos = this.posFromVec((Vec3)hit[0]);
        if (!this.isPlayerOnActivationBlock(player, pos)) {
            return false;
        }
        int aheadX = pos[0] + travel[0];
        int aheadZ = pos[2] + travel[1];
        if (!this.isReplaceableName(this.blockNameAt(aheadX, pos[1] + 1, aheadZ), false)) {
            return false;
        }
        Vec3 playerPos = player.getPosition();
        double lipDistance = face == 5 ? (double)(pos[0] + 1) - playerPos.x : (face == 4 ? playerPos.x - (double)pos[0] : (face == 3 ? (double)(pos[2] + 1) - playerPos.z : playerPos.z - (double)pos[2]));
        if (lipDistance > 0.65) {
            return false;
        }
        this.hitboxLastPos = new int[]{pos[0], pos[1], pos[2]};
        this.hitboxLastFace = face;
        return true;
    }

    void captureActivationAnchor(Entity player) {
        int travelFace;
        if (player == null) {
            return;
        }
        Object[] hit = this.client.raycastBlock(4.5);
        if (hit == null || hit.length < 3 || hit[0] == null || hit[2] == null) {
            if (this.hitboxLastPos != null && this.hitboxLastFace >= 2) {
                this.activationAnchorPos = new int[]{this.hitboxLastPos[0], this.hitboxLastPos[1], this.hitboxLastPos[2]};
                this.activationAnchorFace = this.hitboxLastFace;
            }
            return;
        }
        int face = this.faceFromName((String)hit[2]);
        if (face < 2) {
            return;
        }
        int[] pos = this.posFromVec((Vec3)hit[0]);
        if (!this.isPlayerOnActivationBlock(player, pos)) {
            return;
        }
        if (!this.isInActivationFaceCenter(face, (Vec3)hit[1])) {
            return;
        }
        int[] travel = this.travelDirectionFromYaw(player.getYaw());
        travelFace = travel[0] > 0 ? 5 : (travel[0] < 0 ? 4 : (travel[1] > 0 ? 3 : 2));
        int n = travelFace;
        if (face != travelFace) {
            return;
        }
        this.activationAnchorPos = new int[]{pos[0], pos[1], pos[2]};
        this.activationAnchorFace = face;
        this.hitboxLastPos = new int[]{pos[0], pos[1], pos[2]};
        this.hitboxLastFace = face;
    }

    boolean isActivationYawAligned(float yaw) {
        float nearestDiagonal = (float)Math.round((yaw - 45.0f) / 90.0f) * 90.0f + 45.0f;
        return Math.abs(this.tellyWrapAngle(yaw - nearestDiagonal)) <= 2.0f;
    }

    boolean isPlayerOnActivationBlock(Entity player, int[] pos) {
        if (pos == null) {
            return false;
        }
        Vec3 playerPos = player.getPosition();
        if (pos[1] != this.floor(playerPos.y - 0.01)) {
            return false;
        }
        double centerX = (double)pos[0] + 0.5;
        double centerZ = (double)pos[2] + 0.5;
        return Math.abs(playerPos.x - centerX) <= 0.85 && Math.abs(playerPos.z - centerZ) <= 0.85;
    }

    boolean isInActivationFaceCenter(int face, Vec3 localHit) {
        double acrossFace;
        if (localHit == null) {
            return false;
        }
        double d = acrossFace = face == 4 || face == 5 ? localHit.z : localHit.x;
        if (face == 3 || face == 4) {
            acrossFace = 1.0 - acrossFace;
        }
        return acrossFace >= 0.38 && acrossFace <= 0.65 && localHit.y >= 0.25 && localHit.y <= 0.75;
    }

    void onRenderWorld(float partialTicks) {
        int face;
        Object[] hit;
        if (!this.modules.getButton("Telly", "Show activation hitbox")) {
            return;
        }
        if (!this.armed || this.running) {
            return;
        }
        if (this.promptAlpha < 0.05f) {
            return;
        }
        if (this.activatePromptAt != 0L && (hit = this.client.raycastBlock(4.5)) != null && hit.length >= 3 && hit[0] != null && hit[2] != null && (face = this.faceFromName((String)hit[2])) >= 2) {
            this.hitboxLastPos = this.posFromVec((Vec3)hit[0]);
            this.hitboxLastFace = face;
        }
        if (this.hitboxLastPos == null || this.hitboxLastFace < 2) {
            return;
        }
        this.drawActivationFaceRegion(this.hitboxLastPos, this.hitboxLastFace);
    }

    void drawActivationFaceRegion(int[] pos, int face) {
        double z2;
        double z1;
        double x2;
        double x1;
        Vec3 cam = this.render.getPosition();
        if (cam == null) {
            return;
        }
        double yMin = (double)pos[1] + 0.25;
        double yMax = (double)pos[1] + 0.75;
        if (face == 5) {
            x2 = x1 = (double)pos[0] + 1.005;
            z1 = (double)pos[2] + 0.38;
            z2 = (double)pos[2] + 0.65;
        } else if (face == 4) {
            x2 = x1 = (double)pos[0] - 0.005;
            z1 = (double)pos[2] + 0.35;
            z2 = (double)pos[2] + 0.62;
        } else if (face == 3) {
            z2 = z1 = (double)pos[2] + 1.005;
            x1 = (double)pos[0] + 0.35;
            x2 = (double)pos[0] + 0.62;
        } else {
            z2 = z1 = (double)pos[2] - 0.005;
            x1 = (double)pos[0] + 0.38;
            x2 = (double)pos[0] + 0.65;
        }
        int r = this.promptFadeRgb >> 16 & 0xFF;
        int g = this.promptFadeRgb >> 8 & 0xFF;
        int b = this.promptFadeRgb & 0xFF;
        int fillAlpha = (int)(60.0f * this.promptAlpha);
        int lineAlpha = (int)(220.0f * this.promptAlpha);
        if (fillAlpha < 4) {
            fillAlpha = 4;
        }
        if (lineAlpha < 16) {
            lineAlpha = 16;
        }
        double c1x = x1 - cam.x;
        double c1y = yMin - cam.y;
        double c1z = z1 - cam.z;
        double c2x = x2 - cam.x;
        double c2y = yMin - cam.y;
        double c2z = z2 - cam.z;
        double c3x = x2 - cam.x;
        double c3y = yMax - cam.y;
        double c3z = z2 - cam.z;
        double c4x = x1 - cam.x;
        double c4y = yMax - cam.y;
        double c4z = z1 - cam.z;
        int fillArgb = fillAlpha << 24 | r << 16 | g << 8 | b;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        BufferBuilder fill = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        fill.vertex((float)c1x, (float)c1y, (float)c1z).color(fillArgb);
        fill.vertex((float)c2x, (float)c2y, (float)c2z).color(fillArgb);
        fill.vertex((float)c3x, (float)c3y, (float)c3z).color(fillArgb);
        fill.vertex((float)c4x, (float)c4y, (float)c4z).color(fillArgb);
        BufferRenderer.drawWithGlobalProgram(fill.end());
        float red = (float)r / 255.0f;
        float green = (float)g / 255.0f;
        float blue = (float)b / 255.0f;
        float lineAlphaF = (float)lineAlpha / 255.0f;
        RenderUtil.drawLine3D(new Vec3d(c1x, c1y, c1z), c2x, c2y, c2z, red, green, blue, lineAlphaF, 2.0f);
        RenderUtil.drawLine3D(new Vec3d(c2x, c2y, c2z), c3x, c3y, c3z, red, green, blue, lineAlphaF, 2.0f);
        RenderUtil.drawLine3D(new Vec3d(c3x, c3y, c3z), c4x, c4y, c4z, red, green, blue, lineAlphaF, 2.0f);
        RenderUtil.drawLine3D(new Vec3d(c4x, c4y, c4z), c1x, c1y, c1z, red, green, blue, lineAlphaF, 2.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    void drawActivatePrompt() {
        if (this.promptAlpha < 0.05f) {
            return;
        }
        int[] display = this.client.getDisplaySize();
        if (display == null || display.length < 2) {
            return;
        }
        String text = "Activate?";
        int alpha = (int)(this.promptAlpha * 255.0f);
        if (alpha < 16) {
            alpha = 16;
        }
        int color = alpha << 24 | this.promptFadeRgb;
        float x = (float)display[0] / 2.0f - (float)this.render.getFontWidth(text) / 2.0f;
        float y = (float)display[1] / 2.0f + 10.0f;
        this.render.text(text, x, y, 1.0f, color, true);
    }

    void updateActivatePromptFade() {
        boolean show;
        boolean bl = show = this.armed && !this.running && this.activatePromptAt != 0L;
        if (show) {
            this.rememberActivationPromptColor();
        }
        long now = this.client.time();
        long elapsed = this.promptFadeLastAt == 0L ? 0L : Math.min(100L, now - this.promptFadeLastAt);
        this.promptFadeLastAt = now;
        float step = (float)elapsed / 200.0f;
        this.promptAlpha += show ? step : -step;
        if (this.promptAlpha < 0.0f) {
            this.promptAlpha = 0.0f;
        }
        if (this.promptAlpha > 1.0f) {
            this.promptAlpha = 1.0f;
        }
    }

    boolean onMouse(int button, boolean state, int mouseX, int mouseY) {
        if (this.running) {
            if (button == 0) {
                this.keybinds.setPressed("attack", false);
                return false;
            }
            if (button == 1) {
                this.keybinds.setPressed("use", this.tellyAutoPlaceWindow);
                return false;
            }
            return this.autoPlaceOnMouse(button, state);
        }
        if (this.armed && button == 1 && !state) {
            this.setActivationMovementHold(false);
        }
        return !this.armed || !this.activationSuppressUse() || button != 1;
    }

    boolean onKey(String keyName, int keyCode, boolean state, boolean inGui) {
        boolean dropKey;
        boolean bl = dropKey = keyCode == this.keybinds.getKeycode("drop") || keyName != null && keyName.toLowerCase().contains("drop");
        if (this.isDropProtected() && dropKey) {
            this.keybinds.setPressed("drop", false);
            return false;
        }
        if (!(this.running || !this.activationMovementHeld || state || keyCode != this.keybinds.getKeycode("back") && keyCode != this.keybinds.getKeycode("right"))) {
            this.keybinds.setPressed("back", true);
            this.keybinds.setPressed("right", true);
            return false;
        }
        if (!this.running) {
            return true;
        }
        if (keyCode == this.keybinds.getKeycode("sneak")) {
            this.suppressSneakInput();
            return false;
        }
        if (!state) {
            this.clearInitialMovementHold(keyCode);
        }
        if (state && this.setupTick < 0 && this.isManualMovementKey(keyCode) && !this.isInitialMovementHold(keyCode) && !this.isScriptHeldKey(keyCode)) {
            this.stopAutomation(true);
            return true;
        }
        if (inGui) {
            return true;
        }
        return !this.isManualMovementKey(keyCode);
    }

    void setActivationMovementHold(boolean hold) {
        if (hold) {
            this.activationMovementHeld = true;
            this.keybinds.setPressed("back", true);
            this.keybinds.setPressed("right", true);
            return;
        }
        if (!this.activationMovementHeld) {
            return;
        }
        this.activationMovementHeld = false;
        this.keybinds.setPressed("back", this.keybinds.isKeyDown(this.keybinds.getKeycode("back")));
        this.keybinds.setPressed("right", this.keybinds.isKeyDown(this.keybinds.getKeycode("right")));
    }

    boolean isScriptHeldKey(int keyCode) {
        if (keyCode == this.keybinds.getKeycode("forward")) {
            return this.keybinds.isPressed("forward");
        }
        if (keyCode == this.keybinds.getKeycode("back")) {
            return this.keybinds.isPressed("back");
        }
        if (keyCode == this.keybinds.getKeycode("left")) {
            return this.keybinds.isPressed("left");
        }
        if (keyCode == this.keybinds.getKeycode("right")) {
            return this.keybinds.isPressed("right");
        }
        if (keyCode == this.keybinds.getKeycode("jump")) {
            return this.keybinds.isPressed("jump");
        }
        if (keyCode == this.keybinds.getKeycode("sprint")) {
            return this.keybinds.isPressed("sprint");
        }
        return false;
    }

    boolean isManualMovementKey(int keyCode) {
        return keyCode == this.keybinds.getKeycode("forward") || keyCode == this.keybinds.getKeycode("back") || keyCode == this.keybinds.getKeycode("left") || keyCode == this.keybinds.getKeycode("right") || keyCode == this.keybinds.getKeycode("jump") || keyCode == this.keybinds.getKeycode("sneak") || keyCode == this.keybinds.getKeycode("sprint");
    }

    void captureInitialMovementHolds() {
        this.ignoreForwardUntilRelease = this.keybinds.isKeyDown(this.keybinds.getKeycode("forward"));
        this.ignoreBackUntilRelease = this.keybinds.isKeyDown(this.keybinds.getKeycode("back"));
        this.ignoreLeftUntilRelease = this.keybinds.isKeyDown(this.keybinds.getKeycode("left"));
        this.ignoreRightUntilRelease = this.keybinds.isKeyDown(this.keybinds.getKeycode("right"));
        this.ignoreJumpUntilRelease = this.keybinds.isKeyDown(this.keybinds.getKeycode("jump"));
        this.ignoreSneakUntilRelease = this.keybinds.isKeyDown(this.keybinds.getKeycode("sneak"));
        this.ignoreSprintUntilRelease = this.keybinds.isKeyDown(this.keybinds.getKeycode("sprint"));
    }

    boolean isInitialMovementHold(int keyCode) {
        if (keyCode == this.keybinds.getKeycode("forward")) {
            return this.ignoreForwardUntilRelease;
        }
        if (keyCode == this.keybinds.getKeycode("back")) {
            return this.ignoreBackUntilRelease;
        }
        if (keyCode == this.keybinds.getKeycode("left")) {
            return this.ignoreLeftUntilRelease;
        }
        if (keyCode == this.keybinds.getKeycode("right")) {
            return this.ignoreRightUntilRelease;
        }
        if (keyCode == this.keybinds.getKeycode("jump")) {
            return this.ignoreJumpUntilRelease;
        }
        if (keyCode == this.keybinds.getKeycode("sneak")) {
            return this.ignoreSneakUntilRelease;
        }
        if (keyCode == this.keybinds.getKeycode("sprint")) {
            return this.ignoreSprintUntilRelease;
        }
        return false;
    }

    void clearInitialMovementHold(int keyCode) {
        if (keyCode == this.keybinds.getKeycode("forward")) {
            this.ignoreForwardUntilRelease = false;
        }
        if (keyCode == this.keybinds.getKeycode("back")) {
            this.ignoreBackUntilRelease = false;
        }
        if (keyCode == this.keybinds.getKeycode("left")) {
            this.ignoreLeftUntilRelease = false;
        }
        if (keyCode == this.keybinds.getKeycode("right")) {
            this.ignoreRightUntilRelease = false;
        }
        if (keyCode == this.keybinds.getKeycode("jump")) {
            this.ignoreJumpUntilRelease = false;
        }
        if (keyCode == this.keybinds.getKeycode("sneak")) {
            this.ignoreSneakUntilRelease = false;
        }
        if (keyCode == this.keybinds.getKeycode("sprint")) {
            this.ignoreSprintUntilRelease = false;
        }
    }

    void clearInitialMovementHolds() {
        this.ignoreForwardUntilRelease = false;
        this.ignoreBackUntilRelease = false;
        this.ignoreLeftUntilRelease = false;
        this.ignoreRightUntilRelease = false;
        this.ignoreJumpUntilRelease = false;
        this.ignoreSneakUntilRelease = false;
        this.ignoreSprintUntilRelease = false;
    }

    boolean detectManualCameraTakeover() {
        if (!this.running || this.setupTick >= 0 || this.client.time() < this.takeoverDetectionAt) {
            return false;
        }
        Entity player = this.client.getPlayer();
        if (player == null) {
            return false;
        }
        long now = this.client.time();
        float expectedYaw = this.scriptedRotationYaw;
        float expectedPitch = this.scriptedRotationPitch;
        if (!this.takeoverCameraValid) {
            this.takeoverCameraValid = true;
            this.takeoverCameraYaw = player.getYaw();
            this.takeoverCameraPitch = player.getPitch();
            this.takeoverAccumulated = 0.0f;
            this.takeoverLastFrameAt = now;
            return false;
        }
        double yawInput = Math.abs(this.tellyWrapAngle(player.getYaw() - expectedYaw));
        double pitchInput = Math.abs(player.getPitch() - expectedPitch);
        double noiseFloor = (double)this.rotationGcd() * 0.45;
        long elapsed = Math.max(0L, now - this.takeoverLastFrameAt);
        this.takeoverLastFrameAt = now;
        this.takeoverAccumulated -= (float)((double)elapsed * 0.045);
        if (this.takeoverAccumulated < 0.0f) {
            this.takeoverAccumulated = 0.0f;
        }
        if (yawInput > noiseFloor || pitchInput > noiseFloor) {
            this.takeoverAccumulated += (float)(yawInput + pitchInput);
        }
        this.takeoverCameraYaw = player.getYaw();
        this.takeoverCameraPitch = player.getPitch();
        if (this.takeoverAccumulated >= 25.0f) {
            this.stopAutomation(true);
            return true;
        }
        return false;
    }

    boolean onPacketSent(CPacket packet) {
        boolean allowed;
        String status;
        C07 digging;
        if (this.isDropProtected() && packet instanceof C07) {
            digging = (C07)packet;
            String string = status = digging.status == null ? "" : String.valueOf(digging.status).toUpperCase();
            if (status.contains("DROP")) {
                return false;
            }
        }
        if (!this.running) {
            return true;
        }
        if (packet instanceof C02) {
            C02 interaction = (C02)packet;
            if ("ATTACK".equals(interaction.action)) {
                return false;
            }
        }
        if (packet instanceof C07) {
            digging = (C07)packet;
            String string = status = digging.status == null ? "" : String.valueOf(digging.status).toUpperCase();
            if (status.contains("DESTROY")) {
                return false;
            }
        }
        if (packet instanceof C0B) {
            C0B action = (C0B)packet;
            if ("START_SNEAKING".equals(action.action)) {
                return false;
            }
        }
        int[] placedTarget = null;
        if (packet instanceof C08) {
            C08 placement = (C08)packet;
            if (placement.direction != 255 && placement.position != null && !this.isStraightTellyTarget(placedTarget = this.offsetPos(this.posFromVec(placement.position), placement.direction))) {
                this.cancelledGhostBlocks.add(this.posKey(placedTarget));
                return false;
            }
        }
        if ((allowed = this.autoPlaceOnPacketSent(packet)) && placedTarget != null) {
            this.cancelledGhostBlocks.remove(this.posKey(placedTarget));
            this.latestStraightPlacedPos = new int[]{placedTarget[0], placedTarget[1], placedTarget[2]};
            if (this.firstTellyPlacementPending && this.setupTick < 0) {
                this.firstTellyPlacementPending = false;
                this.adaptiveAimValid = false;
                this.adaptiveAimUpdatedAt = 0L;
            }
        }
        return allowed;
    }

    boolean isActivationInProgress() {
        return this.armed && !this.running && this.activatePromptAt != 0L;
    }

    boolean isDropProtected() {
        return this.running || this.isActivationInProgress();
    }

    void onPostPlayerInput() {
        this.advanceTellyCycle();
        this.applyTellyMovementInput();
    }

    void advanceTellyCycle() {
        if (!this.running) {
            return;
        }
        this.suppressSneakInput();
        this.enforceSafeWalkDisabledForRun();
        if (this.setupTick >= 0) {
            if (this.setupTick < 12) {
                boolean setupJump = this.setupTick >= 6;
                this.stagedForward = -1.0f;
                this.stagedStrafe = -1.0f;
                this.stagedJump = setupJump;
                this.stagedSprint = false;
                this.applyUse(true);
                if (this.setupTick == 11) {
                    this.setRotationTarget(this.baseYaw + this.yawCurve[19], this.pitchCurve[19], 50L);
                } else {
                    this.setRotationTarget(this.baseYaw, 74.52f, 50L);
                }
                ++this.setupTick;
                return;
            }
            this.setupTick = -1;
            this.takeoverDetectionAt = this.client.time() + 125L;
            Entity takeoverPlayer = this.client.getPlayer();
            this.takeoverCameraValid = takeoverPlayer != null;
            this.takeoverAccumulated = 0.0f;
            this.takeoverLastFrameAt = this.client.time();
            if (takeoverPlayer != null) {
                this.takeoverCameraYaw = takeoverPlayer.getYaw();
                this.takeoverCameraPitch = takeoverPlayer.getPitch();
            }
            this.captureInitialMovementHolds();
            this.cyclePhase = 19;
            this.firstTellyPlacementPending = true;
            this.adaptiveAimValid = false;
            this.clearCachedCandidate();
            this.updateAdaptivePlacementAim(this.client.getPlayer());
        }
        int phase = this.cyclePhase;
        float strafe = this.strafeCurve[phase];
        boolean sprinting = phase == 0 || phase == 1;
        boolean jumping = phase >= 1 && phase <= 19;
        boolean use = phase >= 7;
        this.stagedForward = this.forwardCurve[phase];
        this.stagedStrafe = strafe;
        this.stagedJump = jumping;
        this.stagedSprint = sprinting;
        this.applyUse(use);
        int nextPhase = (phase + 1) % this.yawCurve.length;
        this.setRotationTarget(this.baseYaw + this.yawCurve[nextPhase], this.pitchCurve[nextPhase], 50L);
        this.cyclePhase = nextPhase;
    }

    void applyTellyMovementInput() {
        if (!this.running) {
            return;
        }
        this.suppressSneakInput();
        this.enforceSafeWalkDisabledForRun();
        this.holdScriptedRotation();
        this.applyMovement(this.stagedForward, this.stagedStrafe, this.stagedJump, this.stagedSprint);
    }

    void onPreMotion(PlayerState state) {
        if (!this.running || state == null) {
            return;
        }
        Entity player = this.client.getPlayer();
        if (player == null) {
            return;
        }
        this.holdScriptedRotation();
        state.yaw = this.scriptedRotationYaw;
        state.pitch = this.scriptedRotationPitch;
        this.autoPlaceOnPreMotion(state);
    }

    void onRenderTick(float partialTicks) {
        this.updateActivatePromptFade();
        this.drawActivatePrompt();
        if (!this.running) {
            return;
        }
        if (this.detectManualCameraTakeover()) {
            return;
        }
        this.applySmoothedRotation();
    }

    void onPostMotion() {
        if (!this.running) {
            return;
        }
        this.autoPlaceOnPostMotion();
    }

    boolean onPacketReceived(SPacket packet) {
        if (this.running && packet != null && "PlayerPositionLookS2CPacket".equals(packet.name)) {
            this.stopAutomation(true);
            return true;
        }
        if (packet instanceof S23 && !this.cancelledGhostBlocks.isEmpty()) {
            S23 change = (S23)packet;
            if (change.position != null) {
                this.cancelledGhostBlocks.remove(this.posKey(this.posFromVec(change.position)));
            }
        }
        return true;
    }

    void armAutomation() {
        this.armed = true;
        this.running = false;
        this.activatePromptAt = 0L;
        this.promptBrokeAt = 0L;
        this.setupTick = 0;
        this.cyclePhase = 19;
        this.rotationActive = false;
        this.activationMovementHeld = false;
        this.eagleDisabledForActivation = false;
        this.eagleWasDisabledByTelly = false;
        this.printStatus("&eArmed. Sneak looking down, wait for green, hold rmb and release sneak");
    }

    void disableEagleForActivation() {
        if (this.eagleDisabledForActivation) {
            return;
        }
        this.eagleDisabledForActivation = true;
        try {
            if (Myau.moduleManager == null) {
                return;
            }
            Eagle eagle = (Eagle)Myau.moduleManager.getModule("Eagle");
            if (eagle != null && eagle.isEnabled()) {
                eagle.toggle();
                this.eagleWasDisabledByTelly = true;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    void restoreEagleAfterTelly() {
        try {
            if (Myau.moduleManager == null) {
                return;
            }
            Eagle eagle = (Eagle)Myau.moduleManager.getModule("Eagle");
            if (eagle != null && !eagle.isEnabled()) {
                eagle.toggle();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    void beginAutomation() {
        Entity player = this.client.getPlayer();
        if (player == null || !player.isHoldingBlock()) {
            this.printStatus("&cHold blocks before starting");
            return;
        }
        if (!this.isActivationYawAligned(player.getYaw())) {
            return;
        }
        this.disableSafeWalkForRun();
        this.baseYaw = (float)Math.round((player.getYaw() - 45.0f) / 90.0f) * 90.0f + 45.0f;
        this.calculateTravelDirection(this.baseYaw);
        this.antiSwayLane = this.travelX != 0 ? player.getPosition().z : player.getPosition().x;
        this.antiSwayYawOffset = 0.0f;
        this.antiSwayTapUsed = false;
        this.cancelledGhostBlocks.clear();
        if (this.activationAnchorPos == null) {
            this.captureActivationAnchor(player);
        }
        this.initializeStraightBridgeLane(player);
        this.firstTellyPlacementPending = false;
        this.adaptiveAimValid = false;
        this.adaptiveAimUpdatedAt = 0L;
        this.setupTick = 0;
        this.cyclePhase = 19;
        this.stagedForward = -1.0f;
        this.stagedStrafe = -1.0f;
        this.stagedJump = false;
        this.stagedSprint = false;
        this.armed = false;
        this.running = true;
        this.freezeLastTickAt = this.client.time();
        this.activationMovementHeld = false;
        this.tellyAutoPlaceWindow = true;
        this.scriptedRotationYaw = this.baseYaw;
        this.scriptedRotationPitch = 74.52f;
        this.rotationStartYaw = this.baseYaw;
        this.rotationStartPitch = 74.52f;
        this.rotationTargetYaw = this.baseYaw;
        this.rotationTargetPitch = 74.52f;
        this.rotationActive = false;
        player.setYaw(this.baseYaw);
        player.setPitch(74.52f);
        this.takeoverDetectionAt = 0L;
        this.takeoverCameraValid = false;
        this.clearInitialMovementHolds();
        this.resetControllerState();
        this.keybinds.setPressed("attack", false);
        this.applyMovement(-1.0f, -1.0f, false, false);
        this.setRotationTarget(this.baseYaw, 74.52f, 50L);
        this.applySmoothedRotation();
        this.applyUse(true);
        this.printStatus("&aStarted");
    }

    void stopAutomation(boolean turnOffButton) {
        boolean restoreEagleAfterStop = this.eagleWasDisabledByTelly;
        this.armed = false;
        this.running = false;
        this.setupTick = 0;
        this.cyclePhase = 19;
        this.rotationActive = false;
        this.activationMovementHeld = false;
        this.eagleDisabledForActivation = false;
        this.eagleWasDisabledByTelly = false;
        this.tellyAutoPlaceWindow = false;
        this.autoPlaceDebugActive = false;
        this.antiSwayYawOffset = 0.0f;
        this.antiSwayTapUsed = false;
        this.firstTellyPlacementPending = false;
        this.latestStraightPlacedPos = null;
        this.activationAnchorPos = null;
        this.activationAnchorFace = -1;
        this.stagedForward = 0.0f;
        this.stagedStrafe = 0.0f;
        this.stagedJump = false;
        this.stagedSprint = false;
        this.adaptiveAimValid = false;
        this.adaptiveAimUpdatedAt = 0L;
        this.scriptedRotationYaw = 0.0f;
        this.scriptedRotationPitch = 0.0f;
        this.takeoverDetectionAt = 0L;
        this.takeoverCameraValid = false;
        this.takeoverCameraYaw = 0.0f;
        this.takeoverCameraPitch = 0.0f;
        this.takeoverAccumulated = 0.0f;
        this.takeoverLastFrameAt = 0L;
        try {
            this.cancelledGhostBlocks.clear();
            this.clearInitialMovementHolds();
            this.resetControllerState();
            this.client.setForward(0.0f);
            this.client.setStrafe(0.0f);
            this.client.setJump(false);
            this.client.setSprinting(false);
            this.releaseMovementKeys();
            this.restorePhysicalUse();
            this.keybinds.setPressed("attack", this.keybinds.isMouseDown(0));
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.restoreSafeWalkState();
        this.freezeLastTickAt = 0L;
        this.armed = true;
        this.activatePromptAt = 0L;
        this.promptBrokeAt = 0L;
        if (restoreEagleAfterStop) {
            this.restoreEagleAfterTelly();
        }
        if (turnOffButton) {
            this.printStatus("&eStopped. Sneak looking down to arm again");
        }
    }

    void disableSafeWalkForRun() {
        if (this.safeWalkStateCaptured) {
            this.enforceSafeWalkDisabledForRun();
            return;
        }
        if (!this.modules.getButton("Telly", "Disable SafeWalk")) {
            return;
        }
        try {
            this.safeWalkWasEnabled = this.modules.isEnabled("SafeWalk");
            this.safeWalkStateCaptured = true;
            if (this.safeWalkWasEnabled) {
                this.modules.disable("SafeWalk");
            }
        }
        catch (Exception ignored) {
            this.safeWalkStateCaptured = false;
        }
    }

    void enforceSafeWalkDisabledForRun() {
        if (!this.safeWalkStateCaptured) {
            return;
        }
        try {
            if (this.modules.isEnabled("SafeWalk")) {
                this.modules.disable("SafeWalk");
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    void restoreSafeWalkState() {
        if (!this.safeWalkStateCaptured) {
            return;
        }
        boolean restoreEnabled = this.safeWalkWasEnabled;
        this.safeWalkStateCaptured = false;
        try {
            boolean currentlyEnabled = this.modules.isEnabled("SafeWalk");
            if (restoreEnabled && !currentlyEnabled) {
                this.modules.enable("SafeWalk");
            }
            if (!restoreEnabled && currentlyEnabled) {
                this.modules.disable("SafeWalk");
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    void printStatus(String message) {
        try {
            if (this.modules.getButton("Telly", "Print")) {
                this.client.print(this.util.color("&bTelly &7| " + message));
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    void setRotationTarget(float targetYaw, float targetPitch, long duration) {
        boolean adaptivePlacementTarget;
        Entity player = this.client.getPlayer();
        if (player == null) {
            return;
        }
        this.applySmoothedRotation();
        if (this.running) {
            this.rotationStartYaw = this.scriptedRotationYaw;
            this.rotationStartPitch = this.scriptedRotationPitch;
        } else {
            this.rotationStartYaw = player.getYaw();
            this.rotationStartPitch = player.getPitch();
        }
        float correctedTargetYaw = targetYaw;
        boolean bl = adaptivePlacementTarget = this.running && this.tellyAutoPlaceWindow && this.firstTellyPlacementPending && this.adaptiveAimValid && this.client.time() - this.adaptiveAimUpdatedAt <= 125L;
        if (adaptivePlacementTarget) {
            correctedTargetYaw = this.adaptiveAimYaw;
            targetPitch = this.adaptiveAimPitch;
        } else if (this.running) {
            correctedTargetYaw += this.antiSwayYawOffset;
        }
        ++this.rotationStepCounter;
        this.rotationTargetYaw = this.rotationStartYaw + this.tellyWrapAngle((correctedTargetYaw += this.rotationGcd() * (float)this.YAW_NUDGE_PATTERN[this.rotationStepCounter % 5]) - this.rotationStartYaw);
        this.rotationTargetPitch = this.clamp(targetPitch, -90.0f, 90.0f);
        this.rotationStartedAt = this.client.time();
        this.rotationDuration = Math.max(1L, duration);
        this.rotationActive = true;
    }

    void applySmoothedRotation() {
        if (!this.rotationActive) {
            if (this.running) {
                this.holdScriptedRotation();
            }
            return;
        }
        Entity player = this.client.getPlayer();
        if (player == null) {
            return;
        }
        double progress = (double)(this.client.time() - this.rotationStartedAt) / (double)this.rotationDuration;
        if (progress < 0.0) {
            progress = 0.0;
        }
        if (progress > 1.0) {
            progress = 1.0;
        }
        float desiredYaw = this.rotationStartYaw + (this.rotationTargetYaw - this.rotationStartYaw) * (float)progress;
        float desiredPitch = this.rotationStartPitch + (this.rotationTargetPitch - this.rotationStartPitch) * (float)progress;
        float quantizedYaw = this.quantizeFrom(this.rotationStartYaw, desiredYaw);
        float quantizedPitch = this.quantizeFrom(this.rotationStartPitch, desiredPitch);
        this.scriptedRotationYaw = quantizedYaw;
        this.scriptedRotationPitch = this.clamp(quantizedPitch, -90.0f, 90.0f);
        this.holdScriptedRotation();
        if (progress >= 1.0) {
            this.rotationActive = false;
        }
    }

    void holdScriptedRotation() {
        Entity player = this.client.getPlayer();
        if (player == null) {
            return;
        }
        player.setYaw(this.scriptedRotationYaw);
        player.setPitch(this.scriptedRotationPitch);
    }

    float quantizeFrom(float origin, float value) {
        float gcd = this.rotationGcd();
        float delta = value - origin;
        delta -= delta % gcd;
        return origin + delta;
    }

    float rotationGcd() {
        if (mc == null || Telly.mc.options == null) {
            return 0.03404715f;
        }
        float fovSetting = (float)((Telly.mc.options.getFov().getValue() - 70.0) / 80.0 + 0.5);
        float f = fovSetting * 0.6f + 0.2f;
        return f * f * f * 8.0f * 0.15f;
    }

    float[] applyGCD(float[] rotations, float[] prevRotations) {
        if (rotations == null || prevRotations == null || rotations.length < 2 || prevRotations.length < 2) {
            return rotations;
        }
        float gcd = this.rotationGcd();
        float deltaYaw = rotations[0] - prevRotations[0];
        float deltaPitch = rotations[1] - prevRotations[1];
        deltaYaw -= deltaYaw % gcd;
        deltaPitch -= deltaPitch % gcd;
        return new float[]{prevRotations[0] + deltaYaw, prevRotations[1] + deltaPitch};
    }

    void applyMovement(float forward, float strafe, boolean jumping, boolean sprinting) {
        float controlledForward = forward;
        boolean controlledSprint = sprinting;
        float correctedStrafe = strafe;
        boolean antiSway = this.running;
        if (antiSway) {
            correctedStrafe = this.applyAntiSwayCorrection(controlledForward, strafe);
        } else {
            this.antiSwayYawOffset = 0.0f;
        }
        this.keybinds.setPressed("forward", controlledForward > 0.03f);
        this.keybinds.setPressed("back", controlledForward < -0.03f);
        this.keybinds.setPressed("left", correctedStrafe > 0.5f);
        this.keybinds.setPressed("right", correctedStrafe < -0.5f);
        this.keybinds.setPressed("jump", jumping);
        this.keybinds.setPressed("sprint", controlledSprint);
        this.client.setForward(controlledForward);
        this.client.setStrafe(correctedStrafe);
        this.client.setJump(jumping);
        this.client.setSneak(false);
        this.client.setSprinting(controlledSprint);
    }

    void suppressSneakInput() {
        this.keybinds.setPressed("sneak", false);
        this.client.setSneak(false);
    }

    void calculateTravelDirection(float yaw) {
        double radians = Math.toRadians(yaw);
        double rawX = Math.sin(radians) - Math.cos(radians);
        double rawZ = -Math.cos(radians) - Math.sin(radians);
        if (Math.abs(rawX) >= Math.abs(rawZ)) {
            this.travelX = rawX >= 0.0 ? 1 : -1;
            this.travelZ = 0;
        } else {
            this.travelX = 0;
            this.travelZ = rawZ >= 0.0 ? 1 : -1;
        }
    }

    void initializeStraightBridgeLane(Entity player) {
        int[] hitPos;
        int face;
        Object[] hit;
        Vec3 position = player.getPosition();
        int startX = this.floor(position.x);
        int startY = this.floor(position.y) - 1;
        int startZ = this.floor(position.z);
        int[] anchor = this.activationAnchorPos;
        if (anchor == null && this.hitboxLastPos != null) {
            anchor = this.hitboxLastPos;
        }
        if (anchor == null && (hit = this.client.raycastBlock(4.5, this.baseYaw, Math.max(player.getPitch(), this.activationPitch()))) != null && hit.length >= 3 && hit[0] instanceof Vec3 && hit[2] != null && (face = this.faceFromName((String)hit[2])) >= 2 && this.isPlayerOnActivationBlock(player, hitPos = this.posFromVec((Vec3)hit[0]))) {
            anchor = hitPos;
            this.activationAnchorPos = new int[]{hitPos[0], hitPos[1], hitPos[2]};
            this.activationAnchorFace = face;
        }
        if (anchor != null) {
            startX = anchor[0];
            startY = anchor[1];
            startZ = anchor[2];
        }
        this.bridgeLaneBlock = this.travelX != 0 ? startZ : startX;
        this.bridgeStartProgress = startX * this.travelX + startZ * this.travelZ;
        hit = this.client.raycastBlock(4.5, this.baseYaw, Math.max(player.getPitch(), this.activationPitch()));
        if (hit != null && hit.length > 0 && hit[0] instanceof Vec3) {
            int[] hitPos2 = this.posFromVec((Vec3)hit[0]);
            int hitLane = this.travelX != 0 ? hitPos2[2] : hitPos2[0];
            int hitProgress = this.straightProgress(hitPos2);
            if (hitLane == this.bridgeLaneBlock && Math.abs(hitPos2[0] - startX) <= 2 && Math.abs(hitPos2[2] - startZ) <= 2 && hitProgress < this.bridgeStartProgress) {
                this.bridgeStartProgress = hitProgress;
                this.latestStraightPlacedPos = new int[]{hitPos2[0], hitPos2[1], hitPos2[2]};
                return;
            }
        }
        this.latestStraightPlacedPos = new int[]{startX, startY, startZ};
    }

    int straightProgress(int[] position) {
        if (position == null) {
            return Integer.MIN_VALUE;
        }
        return position[0] * this.travelX + position[2] * this.travelZ;
    }

    boolean isStraightTellyTarget(int[] position) {
        int lane;
        if (!this.running || position == null) {
            return true;
        }
        int n = lane = this.travelX != 0 ? position[2] : position[0];
        if (lane != this.bridgeLaneBlock) {
            return false;
        }
        return this.straightProgress(position) >= this.bridgeStartProgress;
    }

    void updateAdaptivePlacementAim(Entity player) {
        int[] support;
        if (!this.firstTellyPlacementPending) {
            return;
        }
        Object[] candidate = this.cachedCandidate;
        if (candidate != null) {
            int[] target = this.candidatePlacedPos(candidate);
            Vec3 hitVec = this.candidateHitVec(candidate);
            if (this.isStraightTellyTarget(target) && hitVec != null) {
                this.setAdaptiveAimToPoint(player, hitVec);
                return;
            }
        }
        int[] nArray = support = this.latestStraightPlacedPos != null ? this.latestStraightPlacedPos : this.lastPlacedPos;
        if (support == null || !this.isStraightTellyTarget(support)) {
            return;
        }
        int face = this.travelX > 0 ? 5 : (this.travelX < 0 ? 4 : (this.travelZ > 0 ? 3 : 2));
        int[] nextTarget = this.offsetPos(support, face);
        if (!this.isStraightTellyTarget(nextTarget) || !this.isReplaceable(nextTarget[0], nextTarget[1], nextTarget[2])) {
            return;
        }
        Vec3 fallbackHit = this.getSupportFaceHitVec(support, face, 0.5, 0.5);
        this.setAdaptiveAimToPoint(player, fallbackHit);
    }

    void setAdaptiveAimToPoint(Entity player, Vec3 point) {
        if (player == null || point == null) {
            return;
        }
        Vec3 position = player.getPosition();
        double eyeX = position.x;
        double eyeY = position.y + (double)player.getEyeHeight();
        double eyeZ = position.z;
        double dx = point.x - eyeX;
        double dy = point.y - eyeY;
        double dz = point.z - eyeZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0E-5 && Math.abs(dy) < 1.0E-5) {
            return;
        }
        this.adaptiveAimYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        this.adaptiveAimPitch = this.clamp((float)(-Math.toDegrees(Math.atan2(dy, horizontal))), -89.0f, 89.0f);
        this.adaptiveAimUpdatedAt = this.client.time();
        this.adaptiveAimValid = true;
    }

    float applyAntiSwayCorrection(float forward, float recordedStrafe) {
        boolean tapHelps;
        double lanePosition;
        Entity player = this.client.getPlayer();
        if (player == null) {
            return recordedStrafe;
        }
        Vec3 position = player.getPosition();
        Vec3 motion = this.client.getMotion();
        double d = lanePosition = this.travelX != 0 ? position.z : position.x;
        double laneVelocity = motion == null ? 0.0 : (this.travelX != 0 ? motion.z : motion.x);
        double error = this.antiSwayLane - lanePosition;
        if (Math.abs(error) < 0.015 && Math.abs(laneVelocity) < 0.008) {
            this.antiSwayTapUsed = false;
            this.antiSwayYawOffset *= 0.65f;
            if (Math.abs(this.antiSwayYawOffset) < 0.03f) {
                this.antiSwayYawOffset = 0.0f;
            }
            return recordedStrafe;
        }
        double desiredLaneVelocity = error * 0.42 - laneVelocity * 0.78;
        if (desiredLaneVelocity > 0.16) {
            desiredLaneVelocity = 0.16;
        }
        if (desiredLaneVelocity < -0.16) {
            desiredLaneVelocity = -0.16;
        }
        double velocityCorrection = desiredLaneVelocity - laneVelocity;
        double radians = Math.toRadians(this.running ? (double)this.scriptedRotationYaw : (double)player.getYaw());
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double yawLaneDerivative = this.travelX != 0 ? (double)(-forward) * sin + (double)recordedStrafe * cos : (double)(-forward) * cos - (double)recordedStrafe * sin;
        double desiredYawOffset = 0.0;
        if (Math.abs(yawLaneDerivative) >= 0.12) {
            desiredYawOffset = Math.toDegrees(velocityCorrection * 0.55 / yawLaneDerivative);
        }
        if (desiredYawOffset > 2.25) {
            desiredYawOffset = 2.25;
        }
        if (desiredYawOffset < -2.25) {
            desiredYawOffset = -2.25;
        }
        this.antiSwayYawOffset = this.antiSwayYawOffset * 0.6f + (float)desiredYawOffset * 0.4f;
        double strafeLaneAxis = this.travelX != 0 ? sin : cos;
        boolean bl = tapHelps = Math.abs(strafeLaneAxis) >= 0.2 && velocityCorrection * strafeLaneAxis > 0.0;
        if (tapHelps && !this.antiSwayTapUsed && Math.abs(velocityCorrection) >= 0.03 && recordedStrafe < 0.5f) {
            this.antiSwayTapUsed = true;
            return recordedStrafe + 1.0f;
        }
        return recordedStrafe;
    }

    void applyUse(boolean pressed) {
        if (pressed && !this.autoPlaceDebugActive) {
            this.printStatus("&aAutoPlace activated");
        }
        this.autoPlaceDebugActive = pressed;
        this.tellyAutoPlaceWindow = pressed;
        this.keybinds.setPressed("use", pressed);
    }

    void restorePhysicalUse() {
        this.tellyAutoPlaceWindow = false;
        this.autoPlaceDebugActive = false;
        this.keybinds.setPressed("use", this.keybinds.isMouseDown(1));
    }

    void releaseMovementKeys() {
        this.restorePhysicalKey("forward");
        this.restorePhysicalKey("back");
        this.restorePhysicalKey("left");
        this.restorePhysicalKey("right");
        this.restorePhysicalKey("jump");
        this.restorePhysicalKey("sneak");
        this.restorePhysicalKey("sprint");
    }

    void restorePhysicalKey(String key) {
        int code = this.keybinds.getKeycode(key);
        this.keybinds.setPressed(key, code >= 0 && this.keybinds.isKeyDown(code));
    }

    float tellyWrapAngle(float angle) {
        while (angle <= -180.0f) {
            angle += 360.0f;
        }
        while (angle > 180.0f) {
            angle -= 360.0f;
        }
        return angle;
    }

    float clamp(float value, float minimum, float maximum) {
        if (value < minimum) {
            return minimum;
        }
        if (value > maximum) {
            return maximum;
        }
        return value;
    }

    void autoPlaceOnEnable() {
        this.keybinds.setPressed("attack", false);
        this.resetControllerState();
    }

    void autoPlaceOnDisable() {
        this.resetControllerState();
        this.restoreUseToPhysicalState();
        this.keybinds.setPressed("attack", false);
        this.bridge.remove("AutoPlacePlacing");
        this.releaseExperimentalPlacementClaim();
    }

    void resetControllerState() {
        this.currentClientTick = Integer.MIN_VALUE;
        this.placementEvaluationTick = Integer.MIN_VALUE;
        this.lastPlacementAttemptTick = Integer.MIN_VALUE;
        this.lastSuccessfulPlaceTick = Integer.MIN_VALUE;
        this.forceSuppressTick = Integer.MIN_VALUE;
        this.totalC08Counter = 0L;
        this.c08CounterAtTickBoundary = 0L;
        this.hasLastSentServerPos = false;
        this.clearCachedCandidate();
        this.lastPlacedPos = null;
        this.lastSupportPos = null;
        this.lastSupportFace = -1;
        this.cachedBelowTargets = null;
        this.cachedBelowTargetsTick = Integer.MIN_VALUE;
        this.rejectedTargets.clear();
        this.forcedModeCheck = 0;
        this.useSuppressed = false;
        this.silentPitchActive = false;
        this.placingViaModule = false;
        this.manualC08InWindow = false;
    }

    void autoPlaceOnWorldJoin(Entity entity) {
        if (entity != null && entity.isUser) {
            this.resetControllerState();
        }
    }

    void autoPlaceOnPreUpdate() {
        Entity player = this.client.getPlayer();
        if (player == null) {
            return;
        }
        this.syncPlacementTick(player);
        if (this.placementEvaluationTick != this.currentClientTick) {
            this.placementEvaluationTick = this.currentClientTick;
            this.processAutoPlaceTick(player);
        }
    }

    void syncPlacementTick(Entity player) {
        int tick = this.placementTick(player);
        if (tick == this.currentClientTick) {
            return;
        }
        this.currentClientTick = tick;
        this.candidateResolvedThisTick = false;
        this.silentPitchActive = false;
    }

    boolean useExtendedSearch() {
        return true;
    }

    void autoPlaceOnPostMotion() {
        this.c08CounterAtTickBoundary = this.totalC08Counter;
        this.manualC08InWindow = false;
    }

    void autoPlaceOnRenderTick(float partialTicks) {
        Entity player = this.client.getPlayer();
        if (player == null) {
            return;
        }
        if (!this.isAutoPlaceActiveWindow(player)) {
            return;
        }
        ItemStack heldStack = player.getHeldItem();
        if (!this.isUsableBlockStack(heldStack)) {
            return;
        }
        float basePitch = this.sanitizePitch(player.getPitch(), player.getPitch());
        Object[] candidate = this.resolveCandidateWithOffCursorSilentPitch(player, player.getYaw(), basePitch, heldStack);
        if (candidate != null) {
            this.silentPitch = this.sanitizePitch(this.candidatePitch(candidate), basePitch);
            this.silentPitchActive = true;
            this.suppressUse();
        }
    }

    void autoPlaceOnPreMotion(PlayerState state) {
        if (this.silentPitchActive && !this.manualC08InWindow) {
            state.pitch = this.sanitizePitch(this.silentPitch, state.pitch);
        }
    }

    boolean autoPlaceOnPacketSent(CPacket packet) {
        if (packet instanceof C03) {
            C03 c03 = (C03)packet;
            if (c03.moving && c03.position != null) {
                this.hasLastSentServerPos = true;
                this.lastSentServerPosX = c03.position.x;
                this.lastSentServerPosY = c03.position.y;
                this.lastSentServerPosZ = c03.position.z;
            }
            return true;
        }
        if (packet instanceof C08) {
            C08 c08 = (C08)packet;
            if (c08.direction == 255) {
                if (this.shouldCancelAutoPlaceUseItem()) {
                    this.suppressUse();
                    return false;
                }
            } else if (c08.itemStack != null && c08.itemStack.isBlock) {
                ++this.totalC08Counter;
                if (!this.placingViaModule) {
                    this.manualC08InWindow = true;
                }
            }
        }
        return true;
    }

    boolean autoPlaceOnMouse(int button, boolean state) {
        if (!state || button != 0 && button != 1) {
            return true;
        }
        if (button == 1 && this.shouldCancelAutoPlaceUseItem()) {
            this.suppressUse();
            return false;
        }
        if (!this.shouldSuppressManualClicksThisTick()) {
            return true;
        }
        this.keybinds.setPressed("attack", false);
        return false;
    }

    boolean shouldSuppressManualClicksThisTick() {
        if (!this.isInGameContext()) {
            return false;
        }
        return this.lastSuccessfulPlaceTick == this.currentClientTick || this.forceSuppressTick == this.currentClientTick;
    }

    boolean shouldCancelAutoPlaceUseItem() {
        if (!this.isInGameContext()) {
            return false;
        }
        if (this.shouldSuppressManualClicksThisTick()) {
            return true;
        }
        return this.useSuppressed && this.silentPitchActive;
    }

    void suppressUse() {
        this.keybinds.setPressed("use", false);
        this.useSuppressed = true;
    }

    void restoreUseToPhysicalState() {
        this.keybinds.setPressed("use", this.running ? this.tellyAutoPlaceWindow : this.keybinds.isMouseDown(1));
        this.useSuppressed = false;
    }

    boolean isInGameContext() {
        return this.client.getPlayer() != null && this.client.getScreen().isEmpty();
    }

    boolean areAutoPlaceConditionsMet(Entity player) {
        if (!this.tellyAutoPlaceWindow) {
            return false;
        }
        return this.isUsableBlockStack(player.getHeldItem());
    }

    boolean isAutoPlaceActiveWindow(Entity player) {
        if (!this.isInGameContext()) {
            return false;
        }
        if (this.bridge.has("ScaffoldRunning")) {
            return false;
        }
        if (!this.areAutoPlaceConditionsMet(player)) {
            return false;
        }
        return this.isUsableBlockStack(player.getHeldItem());
    }

    boolean isUsableBlockStack(ItemStack stack) {
        if (stack == null || !stack.isBlock || stack.name == null || stack.stackSize <= 0) {
            return false;
        }
        String name = stack.name.toLowerCase();
        for (String bad : this.UNPLACEABLE_EXACT) {
            if (!name.equals(bad)) continue;
            return false;
        }
        for (String bad : this.UNPLACEABLE_CONTAINS) {
            if (!name.contains(bad)) continue;
            return false;
        }
        return true;
    }

    boolean isBlockBelowPlayerReplaceable(Entity player) {
        Vec3 pos = player.getPosition();
        return this.isReplaceable(this.floor(pos.x), this.floor(pos.y) - 1, this.floor(pos.z));
    }

    boolean placedInCurrentWindow() {
        return this.totalC08Counter > this.c08CounterAtTickBoundary;
    }

    boolean claimExperimentalPlacementTick() {
        Object tickValue = this.bridge.get("PlacementArbiterTick");
        Object ownerValue = this.bridge.get("PlacementArbiterOwner");
        if (tickValue instanceof Number && ((Number)tickValue).intValue() == this.currentClientTick && ownerValue != null && !"Telly".equals(String.valueOf(ownerValue))) {
            return false;
        }
        this.bridge.add("PlacementArbiterTick", this.currentClientTick);
        this.bridge.add("PlacementArbiterOwner", "Telly");
        return true;
    }

    void releaseExperimentalPlacementClaim() {
        Object ownerValue = this.bridge.get("PlacementArbiterOwner");
        if (ownerValue == null || !"Telly".equals(String.valueOf(ownerValue))) {
            return;
        }
        this.bridge.remove("PlacementArbiterTick");
        this.bridge.remove("PlacementArbiterOwner");
    }

    void processAutoPlaceTick(Entity player) {
        float basePitch;
        this.pruneRejectedTargets();
        if (this.lastPlacedPos != null && !this.isSupportAvailable(this.lastPlacedPos[0], this.lastPlacedPos[1], this.lastPlacedPos[2])) {
            this.lastPlacedPos = null;
            this.lastSupportPos = null;
            this.lastSupportFace = -1;
        }
        if (!this.isAutoPlaceActiveWindow(player)) {
            this.clearCachedCandidate();
            this.bridge.remove("AutoPlacePlacing");
            if (this.useSuppressed) {
                this.restoreUseToPhysicalState();
            }
            return;
        }
        ItemStack heldStack = player.getHeldItem();
        if (!this.isUsableBlockStack(heldStack)) {
            this.clearCachedCandidate();
            if (this.useSuppressed) {
                this.restoreUseToPhysicalState();
            }
            return;
        }
        if (!this.isBlockBelowPlayerReplaceable(player)) {
            this.clearCachedCandidate();
            if (this.useSuppressed) {
                this.restoreUseToPhysicalState();
            }
            return;
        }
        float yaw = this.running ? this.scriptedRotationYaw : player.getYaw();
        Object[] candidate = this.resolveCandidateWithOffCursorSilentPitch(player, yaw, basePitch = this.sanitizePitch(this.running ? this.scriptedRotationPitch : player.getPitch(), player.getPitch()), heldStack);
        if (candidate != null) {
            this.silentPitch = this.sanitizePitch(this.candidatePitch(candidate), basePitch);
            this.silentPitchActive = true;
            this.suppressUse();
        } else if (this.useSuppressed && !this.placedInCurrentWindow() && this.lastPlacementAttemptTick != this.currentClientTick) {
            this.restoreUseToPhysicalState();
        }
        if (this.placedInCurrentWindow() || this.lastPlacementAttemptTick == this.currentClientTick) {
            this.suppressUse();
            return;
        }
        if (candidate == null) {
            this.clearCachedCandidate();
            return;
        }
        if (!this.claimExperimentalPlacementTick()) {
            this.clearCachedCandidate();
            return;
        }
        this.bridge.add("AutoPlacePlacing");
        this.lastPlacementAttemptTick = this.currentClientTick;
        if (this.attemptPlacement(player, candidate, heldStack)) {
            return;
        }
        if (this.placedInCurrentWindow()) {
            return;
        }
        float retryYaw = this.running ? this.scriptedRotationYaw : player.getYaw();
        float retryPitch = this.running ? this.scriptedRotationPitch : player.getPitch();
        this.clearCachedCandidate();
        Object[] retryCandidate = this.findBelowPlacement(player, retryYaw, retryPitch, heldStack, this.client.time() + (this.useExtendedSearch() ? 4L : 2L));
        this.cacheCandidate(retryCandidate, retryYaw, retryPitch);
        if (retryCandidate != null) {
            this.silentPitch = this.sanitizePitch(this.candidatePitch(retryCandidate), retryPitch);
            this.silentPitchActive = true;
            if (this.attemptPlacement(player, retryCandidate, heldStack)) {
                return;
            }
        }
        this.releaseExperimentalPlacementClaim();
    }

    boolean attemptPlacement(Entity player, Object[] candidate, ItemStack heldStack) {
        boolean packetSent;
        float placementPitch;
        if (candidate == null) {
            return false;
        }
        int[] placedPos = this.candidatePlacedPos(candidate);
        int[] supportPos = this.candidateSupportPos(candidate);
        int face = this.candidateFace(candidate);
        if (placedPos == null || supportPos == null || face <= 0) {
            return false;
        }
        if (!this.isStraightTellyTarget(placedPos)) {
            return false;
        }
        if (!this.isBlockBelowPlayerReplaceable(player)) {
            return false;
        }
        if (!this.isUsableBlockStack(player.getHeldItem())) {
            return false;
        }
        if (this.placedInCurrentWindow()) {
            return false;
        }
        Object[] prePlaceHit = this.resolveVerifiedHit(this.running ? this.scriptedRotationYaw : player.getYaw(), placementPitch = this.sanitizePitch(this.candidatePitch(candidate), this.running ? this.scriptedRotationPitch : player.getPitch()), supportPos, face, placedPos);
        if (prePlaceHit == null) {
            return false;
        }
        if (this.cancelledGhostBlocks.contains(this.posKey(supportPos))) {
            return false;
        }
        if (!this.isReplaceable(placedPos[0], placedPos[1], placedPos[2])) {
            return false;
        }
        if (!this.isSupportAvailable(supportPos[0], supportPos[1], supportPos[2])) {
            return false;
        }
        if (this.doesPlacementIntersectPlayer(player, placedPos)) {
            return false;
        }
        long counterBefore = this.totalC08Counter;
        Vec3 hitAbs = (Vec3)prePlaceHit[2];
        this.placingViaModule = true;
        boolean placed = this.client.placeBlock(new Vec3(supportPos[0], supportPos[1], supportPos[2]), this.faceName(face), hitAbs);
        this.placingViaModule = false;
        boolean bl = packetSent = this.totalC08Counter > counterBefore;
        if (!placed && !packetSent) {
            return false;
        }
        if (!packetSent) {
            this.markRejectedTarget(placedPos);
            return false;
        }
        this.lastPlacedPos = placedPos;
        this.lastSupportPos = supportPos;
        this.lastSupportFace = face;
        this.lastSuccessfulPlaceTick = this.currentClientTick;
        this.forceSuppressTick = this.currentClientTick;
        this.client.swing();
        return true;
    }

    Object[] resolveVerifiedHit(float yaw, float pitch, int[] expectedSupport, int expectedFace, int[] expectedPlaced) {
        Object[] traced = this.rayCast(yaw, pitch);
        if (traced == null) {
            return null;
        }
        int[] tracedSupport = (int[])traced[0];
        int tracedFace = (Integer)traced[1];
        if (!this.posEquals(tracedSupport, expectedSupport) || tracedFace != expectedFace) {
            return null;
        }
        int[] tracedPlaced = this.offsetPos(tracedSupport, tracedFace);
        if (!this.posEquals(tracedPlaced, expectedPlaced)) {
            return null;
        }
        return traced;
    }

    Object[] resolveCandidateWithOffCursorSilentPitch(Entity player, float yaw, float basePitch, ItemStack heldStack) {
        float safeBasePitch = this.sanitizePitch(basePitch, player.getPitch());
        Object[] previousCandidate = this.cachedCandidate;
        Object[] baseCandidate = this.resolveCandidateForCurrentTick(player, yaw, safeBasePitch, heldStack);
        if (baseCandidate == null) {
            if (previousCandidate != null) {
                float previousBlockPitch = this.getBlockDerivedSilentPitch(player, previousCandidate, safeBasePitch);
                Object[] recovered = this.resolveCandidateForCurrentTick(player, yaw, previousBlockPitch, heldStack);
                if (recovered != null) {
                    return recovered;
                }
                this.cacheCandidate(previousCandidate, yaw, safeBasePitch);
                return previousCandidate;
            }
            return null;
        }
        if (this.isPlacementLookAligned(yaw, safeBasePitch, this.candidateSupportPos(baseCandidate), this.candidateFace(baseCandidate), this.candidatePlacedPos(baseCandidate))) {
            return baseCandidate;
        }
        float blockPitch = this.getBlockDerivedSilentPitch(player, baseCandidate, safeBasePitch);
        if (this.isPlacementLookAligned(yaw, blockPitch, this.candidateSupportPos(baseCandidate), this.candidateFace(baseCandidate), this.candidatePlacedPos(baseCandidate))) {
            return new Object[]{Float.valueOf(blockPitch), this.candidateSupportPos(baseCandidate), this.candidateFace(baseCandidate), this.candidateHitVec(baseCandidate), this.candidatePlacedPos(baseCandidate)};
        }
        Object[] corrected = this.resolveCandidateForCurrentTick(player, yaw, blockPitch, heldStack);
        if (corrected != null && this.posEquals(this.candidatePlacedPos(baseCandidate), this.candidatePlacedPos(corrected))) {
            return corrected;
        }
        this.cacheCandidate(baseCandidate, yaw, safeBasePitch);
        return baseCandidate;
    }

    Object[] resolveCandidateForCurrentTick(Entity player, float yaw, float pitch, ItemStack heldStack) {
        float safePitch = this.sanitizePitch(pitch, player.getPitch());
        if (this.hasCachedCandidateForCurrentTick(yaw, safePitch)) {
            return this.cachedCandidate;
        }
        Object[] candidate = this.findBelowPlacement(player, yaw, safePitch, heldStack, this.client.time() + (this.useExtendedSearch() ? 8L : 4L));
        this.cacheCandidate(candidate, yaw, safePitch);
        return candidate;
    }

    float getBlockDerivedSilentPitch(Entity player, Object[] candidate, float fallbackPitch) {
        Float derived;
        if (candidate == null) {
            return this.sanitizePitch(fallbackPitch, fallbackPitch);
        }
        Vec3 hitVec = this.candidateHitVec(candidate);
        if (hitVec != null && (derived = this.computePitchToHitVec(player, hitVec)) != null) {
            return this.sanitizePitch(derived.floatValue(), fallbackPitch);
        }
        return this.sanitizePitch(this.candidatePitch(candidate), fallbackPitch);
    }

    void cacheCandidate(Object[] candidate, float yaw, float pitch) {
        this.cachedCandidate = candidate;
        this.cachedCandidateTick = this.currentClientTick;
        this.cachedCandidateYaw = yaw;
        this.cachedCandidatePitch = pitch;
        this.candidateResolvedThisTick = candidate != null;
    }

    boolean hasCachedCandidateForCurrentTick(float yaw, float pitch) {
        if (this.cachedCandidateTick != this.currentClientTick || !this.candidateResolvedThisTick || this.cachedCandidate == null) {
            return false;
        }
        if (Float.isNaN(this.cachedCandidateYaw) || Float.isNaN(this.cachedCandidatePitch)) {
            return false;
        }
        return Math.abs(this.wrapAngle(yaw - this.cachedCandidateYaw)) <= 0.75f && Math.abs(pitch - this.cachedCandidatePitch) <= 0.75f;
    }

    void clearCachedCandidate() {
        this.cachedCandidate = null;
        this.cachedCandidateTick = Integer.MIN_VALUE;
        this.cachedCandidateYaw = Float.NaN;
        this.cachedCandidatePitch = Float.NaN;
        this.candidateResolvedThisTick = false;
    }

    Object[] findBelowPlacement(Entity player, float yaw, float currentPitch, ItemStack heldStack, long deadlineMs) {
        int modeCheck;
        if (this.client.time() >= deadlineMs) {
            return null;
        }
        Object[] cursorRayCandidate = this.findDirectCursorRayPlacement(player, yaw, currentPitch, heldStack);
        if (cursorRayCandidate != null) {
            return cursorRayCandidate;
        }
        this.forcedModeCheck = modeCheck = this.getConditionModeCheck(player);
        Object[] result = null;
        if (modeCheck == 1) {
            boolean tryPreviousVisibleFirst;
            boolean straightGroundException = this.isStraightPreviousTickCenterOnGroundSupport(player);
            boolean straightCenterBelowAir = this.isStraightCenterBelowAir(player);
            boolean bl = tryPreviousVisibleFirst = straightGroundException || !this.isCursorDirectedAtBlock(yaw, currentPitch) || this.isNearStraightSupportEdge(player) || straightCenterBelowAir;
            if (straightCenterBelowAir) {
                result = this.findBelowPlayerAirborneFallback(player, yaw, currentPitch, heldStack, Math.max(deadlineMs, this.client.time() + (this.useExtendedSearch() ? 4L : 2L)));
            }
            if (result == null && tryPreviousVisibleFirst) {
                result = this.findStraightPreviousVisibleFaceFallback(player, yaw, currentPitch, heldStack, deadlineMs);
            }
            if (result == null && straightGroundException) {
                result = this.findStraightGroundExceptionCandidate(player, yaw, currentPitch, heldStack, deadlineMs);
            }
            if (result == null) {
                result = this.findStraightLegacyLaneFallback(player, yaw, currentPitch, heldStack, deadlineMs);
            }
            if (result == null && !tryPreviousVisibleFirst) {
                result = this.findStraightPreviousVisibleFaceFallback(player, yaw, currentPitch, heldStack, deadlineMs);
            }
            if (result == null) {
                result = this.findPreviousBlockAirborneFallback(player, yaw, currentPitch, heldStack, Math.max(deadlineMs, this.client.time() + (this.useExtendedSearch() ? 4L : 2L)));
            }
        } else {
            long diagonalDeadline = Math.max(deadlineMs, this.client.time() + (this.useExtendedSearch() ? 10L : 6L));
            result = this.findBelowPlacementForSupport(player, yaw, currentPitch, heldStack, null, -1, diagonalDeadline);
            if (result == null) {
                result = this.findBelowPlayerAirborneFallback(player, yaw, currentPitch, heldStack, diagonalDeadline);
            }
            if (result == null) {
                result = this.findNearestSupportToBelowPlayerFallback(player, yaw, currentPitch, heldStack, diagonalDeadline);
            }
            if (result == null) {
                result = this.findLegacyBelowPlacement(player, yaw, currentPitch, heldStack, diagonalDeadline);
            }
        }
        this.forcedModeCheck = 0;
        return result;
    }

    Object[] findDirectCursorRayPlacement(Entity player, float yaw, float pitch, ItemStack heldStack) {
        if (!this.isUsableBlockStack(heldStack)) {
            return null;
        }
        Object[] traced = this.rayCast(yaw, pitch);
        if (traced == null) {
            return null;
        }
        int[] supportPos = (int[])traced[0];
        int face = (Integer)traced[1];
        if (face == 0) {
            return null;
        }
        int[] targetPos = this.offsetPos(supportPos, face);
        if (!this.isPlacementTargetAvailable(player, targetPos)) {
            return null;
        }
        if (!this.isSupportAvailable(supportPos[0], supportPos[1], supportPos[2])) {
            return null;
        }
        if (this.shouldRejectStraightSideSwitch(player, targetPos, face)) {
            return null;
        }
        float tracedPitch = this.clampFloat(pitch, -89.0f, 89.0f);
        return new Object[]{Float.valueOf(tracedPitch), supportPos, face, (Vec3)traced[2], targetPos};
    }

    Object[] findStraightGroundExceptionCandidate(Entity player, float yaw, float currentPitch, ItemStack heldStack, long deadlineMs) {
        int previousForcedMode = this.forcedModeCheck;
        this.forcedModeCheck = 2;
        Object[] candidate = this.findBelowPlacementForSupport(player, yaw, currentPitch, heldStack, null, -1, deadlineMs);
        if (candidate == null) {
            candidate = this.findBelowPlayerAirborneFallback(player, yaw, currentPitch, heldStack, Math.max(deadlineMs, this.client.time() + (this.useExtendedSearch() ? 4L : 2L)));
        }
        if (candidate == null) {
            candidate = this.findNearestSupportToBelowPlayerFallback(player, yaw, currentPitch, heldStack, Math.max(deadlineMs, this.client.time() + (this.useExtendedSearch() ? 4L : 2L)));
        }
        this.forcedModeCheck = previousForcedMode;
        return candidate;
    }

    Object[] findPreviousBlockAirborneFallback(Entity player, float yaw, float currentPitch, ItemStack heldStack, long deadlineMs) {
        if (!this.hasValidLastSupportFace(player) || this.client.time() >= deadlineMs) {
            return null;
        }
        int[] exactTarget = this.offsetPos(this.lastSupportPos, this.lastSupportFace);
        if (!this.isPlacementTargetAvailable(player, exactTarget)) {
            return null;
        }
        boolean diagonal = this.isDiagonalMovementContext(player);
        return this.findPitchPlacementForTarget(player, yaw, currentPitch, exactTarget, heldStack, this.lastSupportPos, this.lastSupportFace, deadlineMs, false, diagonal);
    }

    Object[] findStraightLegacyLaneFallback(Entity player, float yaw, float currentPitch, ItemStack heldStack, long deadlineMs) {
        if (this.client.time() >= deadlineMs) {
            return null;
        }
        int currentY = this.getCurrentBelowTargetY(player);
        int strictY = this.getStrictBelowTargetY(player);
        int previousY = this.getPreviousBelowTargetY(player);
        int upwardY = this.isStraightAscendingContext(player) ? currentY + 1 : Integer.MIN_VALUE;
        ArrayList<int[]> laneTargets = new ArrayList<int[]>();
        this.addBelowTarget(player, laneTargets, this.getCursorStartTargetAtY(player, yaw, currentPitch, currentY));
        this.addBelowTarget(player, laneTargets, this.getCursorPlacedTargetFromRay(yaw, currentPitch, currentY));
        this.addBelowTarget(player, laneTargets, this.getCursorTargetAtY(player, yaw, currentPitch, currentY));
        if (strictY != currentY) {
            this.addBelowTarget(player, laneTargets, this.getCursorStartTargetAtY(player, yaw, currentPitch, strictY));
            this.addBelowTarget(player, laneTargets, this.getCursorPlacedTargetFromRay(yaw, currentPitch, strictY));
            this.addBelowTarget(player, laneTargets, this.getCursorTargetAtY(player, yaw, currentPitch, strictY));
        }
        if (previousY != currentY && previousY != strictY) {
            this.addBelowTarget(player, laneTargets, this.getCursorStartTargetAtY(player, yaw, currentPitch, previousY));
            this.addBelowTarget(player, laneTargets, this.getCursorPlacedTargetFromRay(yaw, currentPitch, previousY));
            this.addBelowTarget(player, laneTargets, this.getCursorTargetAtY(player, yaw, currentPitch, previousY));
        }
        if (upwardY != Integer.MIN_VALUE && upwardY != currentY && upwardY != strictY && upwardY != previousY) {
            this.addBelowTarget(player, laneTargets, this.getCursorStartTargetAtY(player, yaw, currentPitch, upwardY));
            this.addBelowTarget(player, laneTargets, this.getCursorPlacedTargetFromRay(yaw, currentPitch, upwardY));
            this.addBelowTarget(player, laneTargets, this.getCursorTargetAtY(player, yaw, currentPitch, upwardY));
        }
        for (int[] targetPos : laneTargets) {
            Object[] candidate;
            if (this.client.time() >= deadlineMs) {
                return null;
            }
            if (!this.isStraightLaneTargetAvailable(player, targetPos, currentY, strictY, previousY, upwardY) || (candidate = this.findLegacyPitchPlacementForTarget(player, yaw, currentPitch, targetPos, heldStack, null, deadlineMs)) == null) continue;
            return candidate;
        }
        return null;
    }

    boolean isCursorDirectedAtBlock(float yaw, float pitch) {
        return this.rayCast(yaw, pitch) != null;
    }

    boolean isStraightCenterBelowAir(Entity player) {
        Vec3 pos = player.getPosition();
        return this.isReplaceableName(this.blockNameAt(this.floor(pos.x), this.getCurrentBelowTargetY(player), this.floor(pos.z)), true);
    }

    boolean isStraightPreviousTickCenterOnGroundSupport(Entity player) {
        Vec3 last = player.getLastPosition();
        return !this.isReplaceableName(this.blockNameAt(this.floor(last.x), this.floor(last.y) - 1, this.floor(last.z)), true);
    }

    boolean isNearStraightSupportEdge(Entity player) {
        if (this.lastSupportPos == null || this.lastSupportFace < 2) {
            return false;
        }
        Vec3 pos = player.getPosition();
        double localX = pos.x - (double)this.lastSupportPos[0];
        double localZ = pos.z - (double)this.lastSupportPos[2];
        if (this.isPastStraightSupportEdgeThreshold(this.lastSupportFace, localX, localZ)) {
            return true;
        }
        Vec3 motion = this.client.getMotion();
        if (motion.x * motion.x + motion.z * motion.z < 1.0E-4) {
            return false;
        }
        if (!this.isMovingTowardStraightSupportEdge(this.lastSupportFace, motion.x, motion.z)) {
            return false;
        }
        return this.isPastStraightSupportEdgeThreshold(this.lastSupportFace, localX + motion.x * 1.45, localZ + motion.z * 1.45);
    }

    boolean isPastStraightSupportEdgeThreshold(int supportFace, double localX, double localZ) {
        if (supportFace == 5) {
            return localX >= 0.52;
        }
        if (supportFace == 4) {
            return localX <= 0.48;
        }
        if (supportFace == 3) {
            return localZ >= 0.52;
        }
        if (supportFace == 2) {
            return localZ <= 0.48;
        }
        return false;
    }

    boolean isMovingTowardStraightSupportEdge(int supportFace, double motionX, double motionZ) {
        if (supportFace == 5) {
            return motionX > 0.0;
        }
        if (supportFace == 4) {
            return motionX < 0.0;
        }
        if (supportFace == 3) {
            return motionZ > 0.0;
        }
        if (supportFace == 2) {
            return motionZ < 0.0;
        }
        return false;
    }

    Object[] findStraightPreviousVisibleFaceFallback(Entity player, float yaw, float currentPitch, ItemStack heldStack, long deadlineMs) {
        if (this.client.time() >= deadlineMs || !this.hasValidLastSupportFace(player)) {
            return null;
        }
        if (this.lastSupportFace <= 0) {
            return null;
        }
        int[] targetPos = this.offsetPos(this.lastSupportPos, this.lastSupportFace);
        if (!this.isPlacementTargetAvailable(player, targetPos)) {
            return null;
        }
        return this.findPitchPlacementForTarget(player, yaw, currentPitch, targetPos, heldStack, this.lastSupportPos, this.lastSupportFace, deadlineMs, true, true);
    }

    List<int[]> getBelowPlayerFallbackEndpoints(Entity player, float yaw, float pitch, int targetY) {
        ArrayList<int[]> endpoints = new ArrayList<int[]>();
        if (!this.isDiagonalMovementContext(player)) {
            if (!player.onGround()) {
                this.addBelowTargetIfUnique(player, endpoints, this.getFeetBelowTargetAtY(player, targetY));
                this.addBelowTargetIfUnique(player, endpoints, this.getMotionBelowTargetAtY(player, targetY, 1.0));
                this.addBelowTargetIfUnique(player, endpoints, this.getMotionBelowTargetAtY(player, targetY, 1.7));
            }
            this.addBelowTargetIfUnique(player, endpoints, this.getCursorStartTargetAtY(player, yaw, pitch, targetY));
            this.addBelowTargetIfUnique(player, endpoints, this.getCursorPlacedTargetFromRay(yaw, pitch, targetY));
            this.addBelowTargetIfUnique(player, endpoints, this.getCursorTargetAtY(player, yaw, pitch, targetY));
            return endpoints;
        }
        this.addBelowTargetIfUnique(player, endpoints, this.getMotionBelowTargetAtY(player, targetY, 1.0));
        this.addBelowTargetIfUnique(player, endpoints, this.getMotionBelowTargetAtY(player, targetY, 1.7));
        return endpoints;
    }

    Object[] findBelowPlayerAirborneFallback(Entity player, float yaw, float currentPitch, ItemStack heldStack, long deadlineMs) {
        if (this.client.time() >= deadlineMs) {
            return null;
        }
        int playerBelowY = this.getCurrentBelowTargetY(player);
        boolean diagonal = this.isDiagonalMovementContext(player);
        boolean allowNonCursorTarget = diagonal || !player.onGround();
        ArrayList<int[]> fallbackTargets = new ArrayList<int[]>();
        for (int[] endpoint : this.getBelowPlayerFallbackEndpoints(player, yaw, currentPitch, playerBelowY)) {
            this.addBelowTarget(player, fallbackTargets, endpoint);
        }
        for (int[] targetPos : fallbackTargets) {
            Object[] candidate;
            if (this.client.time() >= deadlineMs) {
                return null;
            }
            if (!this.isPlacementTargetAvailable(player, targetPos) || (candidate = this.findPitchPlacementForTarget(player, yaw, currentPitch, targetPos, heldStack, null, -1, deadlineMs, false, allowNonCursorTarget)) == null) continue;
            return candidate;
        }
        return null;
    }

    Object[] findNearestSupportToBelowPlayerFallback(Entity player, float yaw, float currentPitch, ItemStack heldStack, long deadlineMs) {
        if (this.client.time() >= deadlineMs) {
            return null;
        }
        int targetY = this.getCurrentBelowTargetY(player);
        int[] belowPlayer = this.getFeetBelowTargetAtY(player, targetY);
        if (belowPlayer == null || this.hasDirectSupportNeighbor(belowPlayer)) {
            return null;
        }
        int[] searchOrigin = this.getPathStartTowardBelowPlayer(player, targetY, belowPlayer);
        int[] nearestStart = this.findNearestSupportedReplaceableTarget(player, searchOrigin, belowPlayer, targetY, deadlineMs);
        if (nearestStart == null) {
            return null;
        }
        List<int[]> requiredPath = this.rasterizeHorizontalLineAtY(nearestStart, belowPlayer, targetY, 64);
        for (int i = requiredPath.size() - 1; i >= 0; --i) {
            Object[] candidate;
            if (this.client.time() >= deadlineMs) {
                return null;
            }
            int[] pathPos = requiredPath.get(i);
            if (!this.isPlacementTargetAvailable(player, pathPos) || (candidate = this.findPitchPlacementForTarget(player, yaw, currentPitch, pathPos, heldStack, null, -1, deadlineMs, false, true)) == null) continue;
            return candidate;
        }
        return null;
    }

    int[] findNearestSupportedReplaceableTarget(Entity player, int[] origin, int[] belowPlayer, int targetY, long deadlineMs) {
        if (origin == null || belowPlayer == null || this.client.time() >= deadlineMs) {
            return null;
        }
        for (int radius = 0; radius <= 3; ++radius) {
            int[] bestAtRadius = null;
            double bestScore = Double.POSITIVE_INFINITY;
            for (int dx = -radius; dx <= radius; ++dx) {
                double score;
                int[] negative;
                double score2;
                int dzAbs = radius - Math.abs(dx);
                int[] positive = new int[]{origin[0] + dx, targetY, origin[2] + dzAbs};
                if (this.isPlacementTargetAvailable(player, positive) && this.hasDirectSupportNeighbor(positive) && (score2 = this.scoreAirPathStartCandidate(positive, belowPlayer, origin)) < bestScore) {
                    bestScore = score2;
                    bestAtRadius = positive;
                }
                if (dzAbs == 0 || !this.isPlacementTargetAvailable(player, negative = new int[]{origin[0] + dx, targetY, origin[2] - dzAbs}) || !this.hasDirectSupportNeighbor(negative) || !((score = this.scoreAirPathStartCandidate(negative, belowPlayer, origin)) < bestScore)) continue;
                bestScore = score;
                bestAtRadius = negative;
            }
            if (bestAtRadius == null) continue;
            return bestAtRadius;
        }
        return null;
    }

    double scoreAirPathStartCandidate(int[] candidate, int[] belowPlayer, int[] origin) {
        double sampleY = (double)candidate[1] + 0.5;
        double goalDistSq = this.distSq((double)candidate[0] + 0.5, sampleY, (double)candidate[2] + 0.5, (double)belowPlayer[0] + 0.5, sampleY, (double)belowPlayer[2] + 0.5);
        double originDistSq = this.distSq((double)candidate[0] + 0.5, sampleY, (double)candidate[2] + 0.5, (double)origin[0] + 0.5, sampleY, (double)origin[2] + 0.5);
        return goalDistSq * 4.0 + originDistSq;
    }

    int[] getPathStartTowardBelowPlayer(Entity player, int targetY, int[] fallback) {
        int[] pathStart = null;
        if (this.lastPlacedPos != null && this.lastPlacedPos[1] == targetY) {
            pathStart = this.lastPlacedPos;
        }
        if (pathStart == null) {
            pathStart = this.getMotionBelowTargetAtY(player, targetY, 1.7);
        }
        if (pathStart == null) {
            pathStart = this.getMotionBelowTargetAtY(player, targetY, 1.0);
        }
        return pathStart != null ? pathStart : fallback;
    }

    boolean hasValidLastPlacedPos(Entity player) {
        if (this.lastPlacedPos == null) {
            return false;
        }
        return this.isWithinReach(player, this.lastPlacedPos) && this.isSupportAvailable(this.lastPlacedPos[0], this.lastPlacedPos[1], this.lastPlacedPos[2]) && !this.isInteractable(this.lastPlacedPos[0], this.lastPlacedPos[1], this.lastPlacedPos[2]);
    }

    boolean hasValidLastSupportFace(Entity player) {
        if (this.lastSupportPos == null || this.lastSupportFace < 0) {
            return false;
        }
        return this.isWithinReach(player, this.lastSupportPos) && this.isSupportAvailable(this.lastSupportPos[0], this.lastSupportPos[1], this.lastSupportPos[2]) && !this.isInteractable(this.lastSupportPos[0], this.lastSupportPos[1], this.lastSupportPos[2]);
    }

    Object[] findLegacyBelowPlacement(Entity player, float yaw, float currentPitch, ItemStack heldStack, long deadlineMs) {
        Object[] preferred;
        Object[] diagonalCandidate;
        if (this.client.time() >= deadlineMs || !this.isUsableBlockStack(heldStack)) {
            return null;
        }
        if (this.isDiagonalMovementContext(player) && (diagonalCandidate = this.findLegacyDiagonalPlacement(player, yaw, currentPitch, heldStack, deadlineMs)) != null) {
            return diagonalCandidate;
        }
        if (this.hasValidLastPlacedPos(player) && (preferred = this.findLegacyBelowPlacementForSupport(player, yaw, currentPitch, heldStack, this.lastPlacedPos, deadlineMs)) != null) {
            return preferred;
        }
        return this.findLegacyBelowPlacementForSupport(player, yaw, currentPitch, heldStack, null, deadlineMs);
    }

    Object[] findLegacyDiagonalPlacement(Entity player, float yaw, float currentPitch, ItemStack heldStack, long deadlineMs) {
        Object[] candidate;
        if (this.client.time() >= deadlineMs) {
            return null;
        }
        ArrayList<int[]> diagonalTargets = new ArrayList<int[]>();
        int currentY = this.getCurrentBelowTargetY(player);
        int strictY = this.getStrictBelowTargetY(player);
        for (int[] endpoint : this.getBelowPlayerFallbackEndpoints(player, yaw, currentPitch, currentY)) {
            this.addBelowTarget(player, diagonalTargets, endpoint);
        }
        if (strictY != currentY) {
            for (int[] endpoint : this.getBelowPlayerFallbackEndpoints(player, yaw, currentPitch, strictY)) {
                this.addBelowTarget(player, diagonalTargets, endpoint);
            }
        }
        if (diagonalTargets.isEmpty()) {
            return null;
        }
        int[] preferredSupportPos = this.hasValidLastPlacedPos(player) ? this.lastPlacedPos : null;
        for (int[] targetPos : diagonalTargets) {
            if (this.client.time() >= deadlineMs) {
                return null;
            }
            if (!this.isPlacementTargetAvailable(player, targetPos) || (candidate = this.findLegacyPitchPlacementForTarget(player, yaw, currentPitch, targetPos, heldStack, preferredSupportPos, deadlineMs)) == null) continue;
            return candidate;
        }
        if (preferredSupportPos == null) {
            return null;
        }
        for (int[] targetPos : diagonalTargets) {
            if (this.client.time() >= deadlineMs) {
                return null;
            }
            if (!this.isPlacementTargetAvailable(player, targetPos) || (candidate = this.findLegacyPitchPlacementForTarget(player, yaw, currentPitch, targetPos, heldStack, null, deadlineMs)) == null) continue;
            return candidate;
        }
        return null;
    }

    Object[] findLegacyBelowPlacementForSupport(Entity player, float yaw, float currentPitch, ItemStack heldStack, int[] preferredSupportPos, long deadlineMs) {
        for (int[] targetPos : this.getMessageStyleBelowTargets(player)) {
            Object[] candidate;
            if (this.client.time() >= deadlineMs) {
                return null;
            }
            if (!this.isPlacementTargetAvailable(player, targetPos) || (candidate = this.findLegacyPitchPlacementForTarget(player, yaw, currentPitch, targetPos, heldStack, preferredSupportPos, deadlineMs)) == null) continue;
            return candidate;
        }
        return null;
    }

    Object[] findLegacyPitchPlacementForTarget(Entity player, float yaw, float currentPitch, int[] targetPos, ItemStack heldStack, int[] preferredSupportPos, long deadlineMs) {
        float clampedBasePitch = this.clampFloat(currentPitch, 40.0f, 89.0f);
        Object[] direct = this.tryLegacyPitch(yaw, clampedBasePitch, targetPos, preferredSupportPos, deadlineMs);
        if (direct != null) {
            return direct;
        }
        for (int offset = 1; offset <= 49; ++offset) {
            Object[] candidate;
            Object[] candidate2;
            if (this.client.time() >= deadlineMs) {
                return null;
            }
            float up = clampedBasePitch + (float)offset;
            if (up <= 89.0f && (candidate2 = this.tryLegacyPitch(yaw, up, targetPos, preferredSupportPos, deadlineMs)) != null) {
                return candidate2;
            }
            float down = clampedBasePitch - (float)offset;
            if (!(down >= 40.0f) || (candidate = this.tryLegacyPitch(yaw, down, targetPos, preferredSupportPos, deadlineMs)) == null) continue;
            return candidate;
        }
        return null;
    }

    Object[] tryLegacyPitch(float yaw, float pitch, int[] targetPos, int[] preferredSupportPos, long deadlineMs) {
        if (this.client.time() >= deadlineMs) {
            return null;
        }
        Object[] traced = this.rayCast(yaw, pitch);
        if (traced == null) {
            return null;
        }
        int[] supportPos = (int[])traced[0];
        int face = (Integer)traced[1];
        if (preferredSupportPos != null && !this.posEquals(supportPos, preferredSupportPos)) {
            return null;
        }
        if (face == 0) {
            return null;
        }
        if (this.isReplaceable(supportPos[0], supportPos[1], supportPos[2]) || this.isInteractable(supportPos[0], supportPos[1], supportPos[2])) {
            return null;
        }
        int[] placedPos = this.offsetPos(supportPos, face);
        if (!this.posEquals(placedPos, targetPos)) {
            return null;
        }
        return new Object[]{Float.valueOf(Math.min(pitch, 89.0f)), supportPos, face, (Vec3)traced[2], placedPos};
    }

    List<int[]> getMessageStyleBelowTargets(Entity player) {
        double[] offsets = new double[]{0.0, 0.29, -0.29};
        Vec3 pos = player.getPosition();
        int maxY = this.floor(pos.y) - 1;
        int minY = this.floor(pos.y) - 2;
        ArrayList<int[]> targets = new ArrayList<int[]>();
        for (int targetY = maxY; targetY >= minY; --targetY) {
            for (double xOffset : offsets) {
                for (double zOffset : offsets) {
                    targets.add(new int[]{this.floor(pos.x + xOffset), targetY, this.floor(pos.z + zOffset)});
                }
            }
        }
        return targets;
    }

    Object[] findBelowPlacementForSupport(Entity player, float yaw, float currentPitch, ItemStack heldStack, int[] preferredSupportPos, int preferredSupportFace, long deadlineMs) {
        boolean diagonal = this.isDiagonalMovementContext(player);
        for (int[] targetPos : this.getBelowTargets(player, yaw, currentPitch)) {
            Object[] candidate;
            if (this.client.time() >= deadlineMs) {
                return null;
            }
            if (!this.isPlacementTargetAvailable(player, targetPos) || !this.isStrictOneBelowPlayer(player, targetPos) || (candidate = this.findPitchPlacementForTarget(player, yaw, currentPitch, targetPos, heldStack, preferredSupportPos, preferredSupportFace, deadlineMs, false, diagonal)) == null) continue;
            return candidate;
        }
        return null;
    }

    boolean isWithinReach(Entity player, int[] pos) {
        double cz;
        double dz;
        double cy;
        double dy;
        if (pos == null) {
            return false;
        }
        Vec3 eyes = this.getEyes(player);
        double cx = Math.max((double)pos[0], Math.min(eyes.x, (double)pos[0] + 1.0));
        double dx = eyes.x - cx;
        return dx * dx + (dy = eyes.y - (cy = Math.max((double)pos[1], Math.min(eyes.y, (double)pos[1] + 1.0)))) * dy + (dz = eyes.z - (cz = Math.max((double)pos[2], Math.min(eyes.z, (double)pos[2] + 1.0)))) * dz <= this.reach() * this.reach();
    }

    Object[] findPitchPlacementForTarget(Entity player, float yaw, float currentPitch, int[] targetPos, ItemStack heldStack, int[] preferredSupportPos, int preferredSupportFace, long deadlineMs, boolean requireLookAlignment, boolean allowNonCursorTarget) {
        boolean effectiveAllowNonCursorTarget;
        if (this.client.time() >= deadlineMs || targetPos == null) {
            return null;
        }
        boolean bl = effectiveAllowNonCursorTarget = allowNonCursorTarget || this.shouldAllowPlayerOneNonCursorTarget(player, targetPos);
        if (!effectiveAllowNonCursorTarget && !this.isCursorOrBelowPlayerTarget(player, targetPos, yaw, currentPitch)) {
            return null;
        }
        if (!this.isPlacementTargetAvailable(player, targetPos)) {
            return null;
        }
        Object[] bestCandidate = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (int placeFace : this.getAllowedPlaceFacesForContext(player, yaw)) {
            double[] hitOffsets;
            if (this.client.time() >= deadlineMs) break;
            if (this.shouldRejectStraightSideSwitch(player, targetPos, placeFace)) continue;
            int[] supportPos = this.offsetPos(targetPos, this.opposite(placeFace));
            if (preferredSupportPos != null && !this.posEquals(supportPos, preferredSupportPos) || preferredSupportFace >= 0 && placeFace != preferredSupportFace || !this.isSupportAvailable(supportPos[0], supportPos[1], supportPos[2]) || !this.isWithinReach(player, supportPos)) continue;
            block1: for (double primaryOffset : hitOffsets = this.useExtendedSearch() ? this.EXTENDED_FACE_HIT_OFFSETS : this.FACE_HIT_OFFSETS) {
                for (double secondaryOffset : hitOffsets) {
                    double candidateScore;
                    if (this.client.time() >= deadlineMs) continue block1;
                    Vec3 hitVec = this.getSupportFaceHitVec(supportPos, placeFace, primaryOffset, secondaryOffset);
                    Object[] candidate = this.buildPlacementCandidateForHitVec(player, yaw, targetPos, supportPos, placeFace, hitVec, requireLookAlignment, effectiveAllowNonCursorTarget);
                    if (candidate == null || !((candidateScore = this.scorePlacementCandidate(player, currentPitch, this.candidatePitch(candidate), placeFace, primaryOffset, secondaryOffset)) < bestScore)) continue;
                    bestScore = candidateScore;
                    bestCandidate = candidate;
                }
            }
        }
        if (bestCandidate == null && preferredSupportPos != null && preferredSupportFace >= 0) {
            return this.findRayAlignedPitchCandidate(yaw, currentPitch, targetPos, preferredSupportPos, preferredSupportFace, deadlineMs);
        }
        return bestCandidate;
    }

    Object[] findRayAlignedPitchCandidate(float yaw, float currentPitch, int[] targetPos, int[] supportPos, int placeFace, long deadlineMs) {
        float clampedBasePitch = this.clampFloat(currentPitch, 40.0f, 89.0f);
        for (int offset = 0; offset <= 49; ++offset) {
            Object[] candidate;
            float downPitch;
            Object[] candidate2;
            if (this.client.time() >= deadlineMs) {
                return null;
            }
            float upPitch = clampedBasePitch + (float)offset;
            if (upPitch <= 89.0f && (candidate2 = this.tryRayAlignedPitch(yaw, upPitch, targetPos, supportPos, placeFace)) != null) {
                return candidate2;
            }
            if (offset == 0 || !((downPitch = clampedBasePitch - (float)offset) >= 40.0f) || (candidate = this.tryRayAlignedPitch(yaw, downPitch, targetPos, supportPos, placeFace)) == null) continue;
            return candidate;
        }
        return null;
    }

    Object[] tryRayAlignedPitch(float yaw, float pitch, int[] targetPos, int[] supportPos, int placeFace) {
        Object[] traced = this.rayCast(yaw, pitch);
        if (traced == null) {
            return null;
        }
        int[] tracedSupport = (int[])traced[0];
        int tracedFace = (Integer)traced[1];
        if (!this.posEquals(tracedSupport, supportPos) || tracedFace != placeFace) {
            return null;
        }
        int[] tracedPlaced = this.offsetPos(tracedSupport, tracedFace);
        if (!this.posEquals(tracedPlaced, targetPos)) {
            return null;
        }
        return new Object[]{Float.valueOf(pitch), tracedSupport, tracedFace, (Vec3)traced[2], tracedPlaced};
    }

    double scorePlacementCandidate(Entity player, float currentPitch, float candidatePitchValue, int placeFace, double primaryOffset, double secondaryOffset) {
        double pitchPenalty = Math.abs(this.wrapAngle(candidatePitchValue - currentPitch));
        double centerPenalty = Math.abs(primaryOffset - 0.5) + Math.abs(secondaryOffset - 0.5);
        double facePenalty = placeFace == 1 ? 0.0 : 0.35;
        double straightSidePenalty = this.getStraightSideSwitchPenalty(player, placeFace);
        return pitchPenalty + centerPenalty * 2.0 + facePenalty + straightSidePenalty;
    }

    double getStraightSideSwitchPenalty(Entity player, int placeFace) {
        if (this.getConditionModeCheck(player) != 1) {
            return 0.0;
        }
        if (this.lastSupportFace < 2) {
            return 0.0;
        }
        if (placeFace == this.lastSupportFace) {
            return 0.0;
        }
        return 0.8;
    }

    boolean shouldRejectStraightSideSwitch(Entity player, int[] targetPos, int placeFace) {
        if (targetPos == null || this.getConditionModeCheck(player) != 1) {
            return false;
        }
        if (placeFace < 2) {
            return false;
        }
        if (this.lastSupportFace < 2) {
            return false;
        }
        if (placeFace == this.lastSupportFace) {
            return false;
        }
        if (this.isNearStraightSupportEdge(player)) {
            return false;
        }
        int[] laneSupportPos = this.offsetPos(targetPos, this.opposite(this.lastSupportFace));
        return this.isSupportAvailable(laneSupportPos[0], laneSupportPos[1], laneSupportPos[2]) && this.isWithinReach(player, laneSupportPos);
    }

    Object[] buildPlacementCandidateForHitVec(Entity player, float yaw, int[] targetPos, int[] supportPos, int placeFace, Vec3 hitVec, boolean requireLookAlignment, boolean allowNonCursorTarget) {
        if (hitVec == null) {
            return null;
        }
        int[] offsetTarget = this.offsetPos(supportPos, placeFace);
        if (!this.posEquals(offsetTarget, targetPos)) {
            return null;
        }
        if (!this.isStrictOneBelowPlayer(player, offsetTarget)) {
            return null;
        }
        Float pitch = this.computePitchToHitVec(player, hitVec);
        if (pitch == null) {
            return null;
        }
        if (!this.isPlacementLookAligned(yaw, pitch.floatValue(), supportPos, placeFace, targetPos)) {
            return null;
        }
        if (!(allowNonCursorTarget || this.isDiagonalMovementContext(player) || this.isSupportFaceVisible(player, supportPos, placeFace, hitVec))) {
            return null;
        }
        return new Object[]{pitch, supportPos, placeFace, hitVec, offsetTarget};
    }

    int[] getAllowedPlaceFacesForContext(Entity player, float yaw) {
        if (this.getConditionModeCheck(player) != 1) {
            return this.ALLOWED_PLACE_FACES;
        }
        int forward = this.getStraightForwardFacing(player, yaw);
        if (this.useExtendedSearch()) {
            return new int[]{this.rotateY(forward), this.rotateYCCW(forward), forward, this.opposite(forward), 1};
        }
        return new int[]{this.rotateY(forward), this.rotateYCCW(forward), forward, this.opposite(forward)};
    }

    boolean isPlacementLookAligned(float yaw, float pitch, int[] supportPos, int placeFace, int[] targetPos) {
        if (supportPos == null || placeFace < 0 || targetPos == null) {
            return false;
        }
        Object[] traced = this.rayCast(yaw, pitch);
        if (traced == null) {
            return false;
        }
        if (!this.posEquals((int[])traced[0], supportPos) || (Integer)traced[1] != placeFace) {
            return false;
        }
        int[] tracedOffset = this.offsetPos((int[])traced[0], (Integer)traced[1]);
        return this.posEquals(tracedOffset, targetPos);
    }

    boolean isSupportFaceVisible(Entity player, int[] supportPos, int placeFace, Vec3 hitVec) {
        float tracePitch;
        Vec3 eyes = this.getEyes(player);
        double dx = hitVec.x - eyes.x;
        double dy = hitVec.y - eyes.y;
        double dz = hitVec.z - eyes.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 1.0E-4) {
            return false;
        }
        float traceYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        Object[] traced = this.client.raycastBlock(distance + 0.5, traceYaw, tracePitch = (float)(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))));
        if (traced == null) {
            return false;
        }
        int[] tracedPos = this.posFromVec((Vec3)traced[0]);
        int tracedFace = this.faceFromName((String)traced[2]);
        return this.posEquals(tracedPos, supportPos) && tracedFace == placeFace;
    }

    Vec3 getSupportFaceHitVec(int[] supportPos, int placeFace, double primaryOffset, double secondaryOffset) {
        double primary = Math.max(0.001, Math.min(0.999, primaryOffset));
        double secondary = Math.max(0.001, Math.min(0.999, secondaryOffset));
        if (placeFace == 2) {
            return new Vec3((double)supportPos[0] + primary, (double)supportPos[1] + secondary, (double)supportPos[2] + 0.001);
        }
        if (placeFace == 3) {
            return new Vec3((double)supportPos[0] + primary, (double)supportPos[1] + secondary, (double)supportPos[2] + 0.999);
        }
        if (placeFace == 5) {
            return new Vec3((double)supportPos[0] + 0.999, (double)supportPos[1] + primary, (double)supportPos[2] + secondary);
        }
        if (placeFace == 4) {
            return new Vec3((double)supportPos[0] + 0.001, (double)supportPos[1] + primary, (double)supportPos[2] + secondary);
        }
        if (placeFace == 0) {
            return new Vec3((double)supportPos[0] + primary, (double)supportPos[1] + 0.001, (double)supportPos[2] + secondary);
        }
        return new Vec3((double)supportPos[0] + primary, (double)supportPos[1] + 0.999, (double)supportPos[2] + secondary);
    }

    Float computePitchToHitVec(Entity player, Vec3 hitVec) {
        Vec3 eyes = this.getEyes(player);
        double dx = hitVec.x - eyes.x;
        double dz = hitVec.z - eyes.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double dy = hitVec.y - eyes.y;
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontal)));
        return Float.valueOf(Math.max(-89.0f, Math.min(89.0f, pitch)));
    }

    List<int[]> getBelowTargets(Entity player, float yaw, float pitch) {
        if (this.cachedBelowTargetsTick == this.currentClientTick && this.cachedBelowTargets != null) {
            return this.cachedBelowTargets;
        }
        ArrayList<int[]> belowTargets = new ArrayList<int[]>();
        boolean diagonal = this.isDiagonalMovementContext(player);
        if (!diagonal) {
            int strictY;
            int currentY = this.getCurrentBelowTargetY(player);
            this.addBelowTarget(player, belowTargets, this.getCursorStartTargetAtY(player, yaw, pitch, currentY));
            if (belowTargets.isEmpty() && (strictY = this.getStrictBelowTargetY(player)) != currentY) {
                this.addBelowTarget(player, belowTargets, this.getCursorStartTargetAtY(player, yaw, pitch, strictY));
            }
            if (belowTargets.isEmpty()) {
                this.addBelowTarget(player, belowTargets, this.getCursorPlacedTargetFromRay(yaw, pitch, currentY));
            }
            if (belowTargets.isEmpty() && (strictY = this.getStrictBelowTargetY(player)) != currentY) {
                this.addBelowTarget(player, belowTargets, this.getCursorPlacedTargetFromRay(yaw, pitch, strictY));
            }
            if (belowTargets.isEmpty()) {
                this.addBelowTarget(player, belowTargets, this.getCursorTargetAtY(player, yaw, pitch, currentY));
            }
        } else {
            int currentY = this.getCurrentBelowTargetY(player);
            this.addBelowTarget(player, belowTargets, this.getMotionBelowTargetAtY(player, currentY, 1.0));
            this.addBelowTarget(player, belowTargets, this.getMotionBelowTargetAtY(player, currentY, 1.7));
            for (int[] endpoint : this.getBelowPlayerFallbackEndpoints(player, yaw, pitch, currentY)) {
                this.addBelowTarget(player, belowTargets, endpoint);
            }
        }
        this.cachedBelowTargets = belowTargets;
        this.cachedBelowTargetsTick = this.currentClientTick;
        return belowTargets;
    }

    boolean isCursorOrBelowPlayerTarget(Entity player, int[] targetPos, float yaw, float pitch) {
        if (targetPos == null) {
            return false;
        }
        if (!this.isDiagonalMovementContext(player)) {
            int currentY = this.getCurrentBelowTargetY(player);
            if (this.posEquals(this.getCursorStartTargetAtY(player, yaw, pitch, currentY), targetPos)) {
                return true;
            }
            if (this.posEquals(this.getCursorPlacedTargetFromRay(yaw, pitch, currentY), targetPos)) {
                return true;
            }
            int strictY = this.getStrictBelowTargetY(player);
            if (strictY != currentY) {
                if (this.posEquals(this.getCursorStartTargetAtY(player, yaw, pitch, strictY), targetPos)) {
                    return true;
                }
                if (this.posEquals(this.getCursorPlacedTargetFromRay(yaw, pitch, strictY), targetPos)) {
                    return true;
                }
            }
            if (this.isCursorInsideTargetAtY(player, targetPos, yaw, pitch, currentY)) {
                return true;
            }
            return this.posEquals(this.getCursorTargetAtY(player, yaw, pitch, currentY), targetPos);
        }
        int strictY = this.getStrictBelowTargetY(player);
        if (this.isBelowPlayerTargetAtY(player, targetPos, strictY, yaw, pitch)) {
            return true;
        }
        return this.isBelowPlayerTargetAtY(player, targetPos, this.getCurrentBelowTargetY(player), yaw, pitch);
    }

    boolean isBelowPlayerTargetAtY(Entity player, int[] targetPos, int targetY, float yaw, float pitch) {
        for (int[] candidate : this.getBelowPlayerFallbackEndpoints(player, yaw, pitch, targetY)) {
            if (!this.posEquals(targetPos, candidate)) continue;
            return true;
        }
        return false;
    }

    int[] getFeetBelowTargetAtY(Entity player, int targetY) {
        Vec3 pos = player.getPosition();
        return new int[]{this.floor(pos.x), targetY, this.floor(pos.z)};
    }

    boolean shouldAllowPlayerOneNonCursorTarget(Entity player, int[] targetPos) {
        if (targetPos == null) {
            return false;
        }
        if (this.isDiagonalMovementContext(player) || player.onGround()) {
            return false;
        }
        if (!this.isPlayerHitboxFullyInsideSingleBlockColumn(player)) {
            return false;
        }
        if (!this.hasValidLastSupportFace(player) || this.lastSupportFace == 0) {
            return false;
        }
        int[] continuationTarget = this.offsetPos(this.lastSupportPos, this.lastSupportFace);
        if (!this.posEquals(targetPos, continuationTarget)) {
            return false;
        }
        int targetY = targetPos[1];
        int currentY = this.getCurrentBelowTargetY(player);
        int strictY = this.getStrictBelowTargetY(player);
        if (targetY != currentY && targetY != strictY) {
            return false;
        }
        int[] feetBelow = this.getFeetBelowTargetAtY(player, targetY);
        int horizontalDistance = Math.abs(targetPos[0] - feetBelow[0]) + Math.abs(targetPos[2] - feetBelow[2]);
        return horizontalDistance <= 1;
    }

    boolean isPlayerHitboxFullyInsideSingleBlockColumn(Entity player) {
        int maxZ;
        int maxX;
        Vec3 pos = player.getPosition();
        double half = (double)player.getWidth() / 2.0;
        int minX = this.floor(pos.x - half + 1.0E-4);
        if (minX != (maxX = this.floor(pos.x + half - 1.0E-4))) {
            return false;
        }
        int minZ = this.floor(pos.z - half + 1.0E-4);
        return minZ == (maxZ = this.floor(pos.z + half - 1.0E-4));
    }

    int[] getMotionBelowTargetAtY(Entity player, int targetY, double multiplier) {
        Vec3 pos = player.getPosition();
        Vec3 motion = this.client.getMotion();
        return new int[]{this.floor(pos.x + motion.x * multiplier), targetY, this.floor(pos.z + motion.z * multiplier)};
    }

    boolean hasDirectSupportNeighbor(int[] targetPos) {
        for (int placeFace : this.ALLOWED_PLACE_FACES) {
            int[] supportPos = this.offsetPos(targetPos, this.opposite(placeFace));
            if (!this.isSupportAvailable(supportPos[0], supportPos[1], supportPos[2])) continue;
            return true;
        }
        return false;
    }

    void addBelowTargetIfUnique(Entity player, List<int[]> targets, int[] candidate) {
        if (candidate == null) {
            return;
        }
        if (!this.isStrictOneBelowPlayer(player, candidate)) {
            return;
        }
        for (int[] existing : targets) {
            if (!this.posEquals(existing, candidate)) continue;
            return;
        }
        targets.add(candidate);
    }

    void addBelowTarget(Entity player, List<int[]> targets, int[] candidate) {
        this.addBelowTargetIfUnique(player, targets, candidate);
    }

    List<int[]> rasterizeHorizontalLineAtY(int[] start, int[] end, int y, int maxSteps) {
        ArrayList<int[]> line = new ArrayList<int[]>();
        int x0 = start[0];
        int z0 = start[2];
        int x1 = end[0];
        int z1 = end[2];
        int dx = Math.abs(x1 - x0);
        int dz = Math.abs(z1 - z0);
        int sx = Integer.compare(x1, x0);
        int sz = Integer.compare(z1, z0);
        int movedX = 0;
        int movedZ = 0;
        for (int steps = 0; steps < maxSteps; ++steps) {
            line.add(new int[]{x0, y, z0});
            if (x0 == x1 && z0 == z1 || movedX >= dx && movedZ >= dz) break;
            if (movedX >= dx) {
                z0 += sz;
                ++movedZ;
                continue;
            }
            if (movedZ >= dz) {
                x0 += sx;
                ++movedX;
                continue;
            }
            if ((1 + 2 * movedX) * dz < (1 + 2 * movedZ) * dx) {
                x0 += sx;
                ++movedX;
                continue;
            }
            z0 += sz;
            ++movedZ;
        }
        return line;
    }

    int getDetectedModeCheck(Entity player) {
        float forwardInput = Math.abs(this.client.getForward());
        float strafeInput = Math.abs(this.client.getStrafe());
        if (forwardInput >= 0.08f || strafeInput >= 0.08f) {
            return forwardInput >= 0.08f && strafeInput >= 0.08f ? 1 : 2;
        }
        double[] direction = this.getMotionDirectionComponents(player);
        if (direction == null) {
            return 1;
        }
        double angleDeg = Math.toDegrees(Math.atan2(direction[1], direction[0]));
        double norm90 = (angleDeg % 90.0 + 90.0) % 90.0;
        return Math.abs(norm90 - 45.0) <= 18.0 ? 2 : 1;
    }

    double[] getMotionDirectionComponents(Entity player) {
        Vec3 pos = player.getPosition();
        Vec3 last = player.getLastPosition();
        double dirX = pos.x - last.x;
        double dirZ = pos.z - last.z;
        double speedSq = dirX * dirX + dirZ * dirZ;
        if (speedSq < 1.0E-4) {
            Vec3 motion = this.client.getMotion();
            dirX = motion.x;
            dirZ = motion.z;
            speedSq = dirX * dirX + dirZ * dirZ;
        }
        if (speedSq < 1.0E-4) {
            return null;
        }
        return new double[]{dirX, dirZ};
    }

    double[] getInputDirectionComponents(float referenceYaw) {
        double dirZ;
        double cosYaw;
        float forwardInput = this.client.getForward();
        float strafeInput = this.client.getStrafe();
        if (Math.abs(forwardInput) < 0.08f && Math.abs(strafeInput) < 0.08f) {
            return null;
        }
        double yawRadians = Math.toRadians(referenceYaw);
        double sinYaw = Math.sin(yawRadians);
        double dirX = (double)forwardInput * -sinYaw + (double)strafeInput * (cosYaw = Math.cos(yawRadians));
        if (dirX * dirX + (dirZ = (double)forwardInput * cosYaw - (double)strafeInput * sinYaw) * dirZ < 1.0E-4) {
            return null;
        }
        return new double[]{dirX, dirZ};
    }

    int getStraightForwardFacing(Entity player, float fallbackYaw) {
        double[] direction = this.getInputDirectionComponents(fallbackYaw);
        if (direction == null) {
            direction = this.getMotionDirectionComponents(player);
        }
        if (direction == null) {
            return this.facingFromYaw(fallbackYaw);
        }
        float directionYaw = (float)(Math.toDegrees(Math.atan2(direction[1], direction[0])) - 90.0);
        return this.facingFromYaw(directionYaw);
    }

    int getConditionModeCheck(Entity player) {
        if (this.forcedModeCheck != 0) {
            return this.forcedModeCheck;
        }
        if (this.running) {
            return 1;
        }
        return this.getDetectedModeCheck(player);
    }

    boolean isDiagonalMovementContext(Entity player) {
        return this.getConditionModeCheck(player) == 2;
    }

    int[] getCursorPlacedTargetFromRay(float yaw, float pitch, int targetY) {
        Object[] traced = this.rayCast(yaw, pitch);
        if (traced == null) {
            return null;
        }
        int[] offsetTarget = this.offsetPos((int[])traced[0], (Integer)traced[1]);
        if (offsetTarget[1] != targetY) {
            return null;
        }
        return offsetTarget;
    }

    int[] getCursorStartTargetAtY(Entity player, float fallbackYaw, float fallbackPitch, int targetY) {
        Vec3 cursorPoint = this.getCursorIntersectionAtY(player, targetY);
        Vec3 lookVec = this.getCursorLookVec(player);
        if (cursorPoint == null || lookVec == null) {
            return null;
        }
        double startX = cursorPoint.x - lookVec.x * 0.03;
        double startZ = cursorPoint.z - lookVec.z * 0.03;
        return new int[]{this.floor(startX), targetY, this.floor(startZ)};
    }

    int[] getCursorTargetAtY(Entity player, float fallbackYaw, float fallbackPitch, int targetY) {
        Vec3 cursorPoint = this.getCursorIntersectionAtY(player, targetY);
        if (cursorPoint == null) {
            return null;
        }
        return new int[]{this.floor(cursorPoint.x), targetY, this.floor(cursorPoint.z)};
    }

    Vec3 getCursorIntersectionAtY(Entity player, int targetY) {
        Vec3 eyes = this.getEyes(player);
        Vec3 lookVec = this.getCursorLookVec(player);
        if (lookVec == null || Math.abs(lookVec.y) < 1.0E-4) {
            return null;
        }
        double t = ((double)targetY - eyes.y) / lookVec.y;
        if (t <= 0.0) {
            return null;
        }
        return new Vec3(eyes.x + lookVec.x * t, (double)targetY + 0.5, eyes.z + lookVec.z * t);
    }

    Vec3 getCursorLookVec(Entity player) {
        double[] cameraRotations = this.render.getRotations();
        if (cameraRotations != null && cameraRotations.length >= 2) {
            return this.getLookVec((float)cameraRotations[0], (float)cameraRotations[1]);
        }
        return this.getLookVec(player.getYaw(), player.getPitch());
    }

    Vec3 getLookVec(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);
        return new Vec3(-Math.sin(yawRad) * cosPitch, -Math.sin(pitchRad), Math.cos(yawRad) * cosPitch);
    }

    boolean isCursorInsideTargetAtY(Entity player, int[] targetPos, float yaw, float pitch, int targetY) {
        if (targetPos == null || targetPos[1] != targetY) {
            return false;
        }
        Vec3 cursorPoint = this.getCursorIntersectionAtY(player, targetY);
        if (cursorPoint == null) {
            return false;
        }
        double x = cursorPoint.x;
        double z = cursorPoint.z;
        return x >= (double)targetPos[0] - 1.0E-6 && x <= (double)targetPos[0] + 1.0 + 1.0E-6 && z >= (double)targetPos[2] - 1.0E-6 && z <= (double)targetPos[2] + 1.0 + 1.0E-6;
    }

    boolean isPlacementTargetAvailable(Entity player, int[] pos) {
        return this.isBasePlacementTargetAvailable(player, pos) && this.isStrictOneBelowPlayer(player, pos);
    }

    boolean isStraightLaneTargetAvailable(Entity player, int[] pos, int currentY, int strictY, int previousY, int upwardY) {
        if (!this.isBasePlacementTargetAvailable(player, pos)) {
            return false;
        }
        int targetY = pos[1];
        if (targetY == currentY || targetY == strictY) {
            return true;
        }
        if (previousY != Integer.MIN_VALUE && targetY == previousY) {
            return true;
        }
        return upwardY != Integer.MIN_VALUE && targetY == upwardY;
    }

    boolean isBasePlacementTargetAvailable(Entity player, int[] pos) {
        return pos != null && this.isStraightTellyTarget(pos) && !this.isRejectedTarget(pos) && !this.doesPlacementIntersectPlayer(player, pos) && this.isReplaceable(pos[0], pos[1], pos[2]);
    }

    boolean doesPlacementIntersectPlayer(Entity player, int[] placePos) {
        double height;
        if (placePos == null) {
            return false;
        }
        if (this.isInsideAnyPlayerPositionCell(player, placePos)) {
            return true;
        }
        Vec3 pos = player.getPosition();
        double half = (double)player.getWidth() / 2.0;
        if (this.boxIntersectsBlock(pos.x - half, pos.y, pos.z - half, pos.x + half, pos.y + (height = (double)player.getHeight()), pos.z + half, placePos)) {
            return true;
        }
        if (this.isBlockPosInsideBounds(placePos, pos.x - half, pos.y, pos.z - half, pos.x + half, pos.y + height, pos.z + half)) {
            return true;
        }
        if (!this.shouldUseHistoricalPlayerCollisionChecks(player, placePos)) {
            return false;
        }
        Vec3 last = player.getLastPosition();
        if (last.x != pos.x || last.y != pos.y || last.z != pos.z) {
            if (this.boxIntersectsBlock(last.x - half, last.y, last.z - half, last.x + half, last.y + height, last.z + half, placePos)) {
                return true;
            }
            if (this.isBlockPosInsideBounds(placePos, last.x - half, last.y, last.z - half, last.x + half, last.y + height, last.z + half)) {
                return true;
            }
        }
        if (this.hasLastSentServerPos && (this.lastSentServerPosX != pos.x || this.lastSentServerPosY != pos.y || this.lastSentServerPosZ != pos.z)) {
            double sx = this.lastSentServerPosX;
            double sy = this.lastSentServerPosY;
            double sz = this.lastSentServerPosZ;
            if (this.boxIntersectsBlock(sx - half, sy, sz - half, sx + half, sy + height, sz + half, placePos)) {
                return true;
            }
            if (this.isBlockPosInsideBounds(placePos, sx - half, sy, sz - half, sx + half, sy + height, sz + half)) {
                return true;
            }
        }
        return false;
    }

    boolean boxIntersectsBlock(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int[] pos) {
        return maxX > (double)pos[0] && minX < (double)pos[0] + 1.0 && maxY > (double)pos[1] && minY < (double)pos[1] + 1.0 && maxZ > (double)pos[2] && minZ < (double)pos[2] + 1.0;
    }

    boolean isBlockPosInsideBounds(int[] pos, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        int bMinX = this.floor(minX + 1.0E-4);
        int bMaxX = this.floor(maxX - 1.0E-4);
        if (pos[0] < bMinX || pos[0] > bMaxX) {
            return false;
        }
        int bMinZ = this.floor(minZ + 1.0E-4);
        int bMaxZ = this.floor(maxZ - 1.0E-4);
        if (pos[2] < bMinZ || pos[2] > bMaxZ) {
            return false;
        }
        int bMinY = this.floor(minY + 1.0E-4);
        int bMaxY = this.floor(maxY - 1.0E-4);
        return pos[1] >= bMinY && pos[1] <= bMaxY;
    }

    boolean isInsideAnyPlayerPositionCell(Entity player, int[] placePos) {
        Vec3 pos = player.getPosition();
        if (this.isInsidePlayerPositionCell(placePos, pos.x, pos.y, pos.z)) {
            return true;
        }
        if (!this.shouldUseHistoricalPlayerCollisionChecks(player, placePos)) {
            return false;
        }
        Vec3 last = player.getLastPosition();
        if (this.isInsidePlayerPositionCell(placePos, last.x, last.y, last.z)) {
            return true;
        }
        return this.hasLastSentServerPos && this.isInsidePlayerPositionCell(placePos, this.lastSentServerPosX, this.lastSentServerPosY, this.lastSentServerPosZ);
    }

    boolean shouldUseHistoricalPlayerCollisionChecks(Entity player, int[] placePos) {
        if (!player.onGround()) {
            return false;
        }
        if (placePos == null) {
            return true;
        }
        return placePos[1] > this.getCurrentBelowTargetY(player);
    }

    boolean isInsidePlayerPositionCell(int[] placePos, double x, double y, double z) {
        int playerX = this.floor(x);
        int playerY = this.floor(y);
        int playerZ = this.floor(z);
        return placePos[0] == playerX && placePos[2] == playerZ && (placePos[1] == playerY || placePos[1] == playerY + 1);
    }

    boolean isStrictOneBelowPlayer(Entity player, int[] pos) {
        if (pos == null) {
            return false;
        }
        int targetY = pos[1];
        int currentY = this.getCurrentBelowTargetY(player);
        if (targetY == currentY) {
            return true;
        }
        if (targetY == this.getStrictBelowTargetY(player)) {
            return true;
        }
        int previousY = this.getPreviousBelowTargetY(player);
        if (previousY != Integer.MIN_VALUE && targetY == previousY) {
            return true;
        }
        return this.isStraightAscendingContext(player) && targetY == currentY + 1;
    }

    double getStableBelowReferenceY(Entity player) {
        Vec3 pos = player.getPosition();
        double referenceY = pos.y;
        Vec3 motion = this.client.getMotion();
        if (!player.onGround() && motion.y > -0.12 && motion.y <= 0.0) {
            referenceY = Math.max(referenceY, player.getLastPosition().y);
        }
        return referenceY;
    }

    int getStrictBelowTargetY(Entity player) {
        if (this.isDiagonalMovementContext(player)) {
            return this.getCurrentBelowTargetY(player);
        }
        double projectedY = this.getStableBelowReferenceY(player);
        Vec3 motion = this.client.getMotion();
        if (!player.onGround() && motion.y < -0.12) {
            projectedY = player.getPosition().y + motion.y * 0.75;
        }
        return this.floor(projectedY) - 1;
    }

    int getCurrentBelowTargetY(Entity player) {
        return this.floor(this.getStableBelowReferenceY(player)) - 1;
    }

    int getPreviousBelowTargetY(Entity player) {
        return this.floor(player.getLastPosition().y) - 1;
    }

    boolean isStraightAscendingContext(Entity player) {
        if (this.getConditionModeCheck(player) != 1) {
            return false;
        }
        Vec3 motion = this.client.getMotion();
        return motion.y > 0.0 || player.getPosition().y > player.getLastPosition().y + 1.0E-4;
    }

    boolean isSupportAvailable(int x, int y, int z) {
        if (this.isInteractable(x, y, z)) {
            return false;
        }
        return !this.isReplaceable(x, y, z);
    }

    boolean isRejectedTarget(int[] pos) {
        Integer rejectedAtTick = this.rejectedTargets.get(this.posKey(pos));
        if (rejectedAtTick == null) {
            return false;
        }
        return this.currentClientTick - rejectedAtTick <= 4;
    }

    void markRejectedTarget(int[] pos) {
        if (pos == null) {
            return;
        }
        this.rejectedTargets.put(this.posKey(pos), this.currentClientTick);
    }

    void pruneRejectedTargets() {
        if (this.rejectedTargets.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, Integer>> iterator = this.rejectedTargets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            if (this.currentClientTick - entry.getValue() > 4) {
                iterator.remove();
                continue;
            }
            String[] parts = entry.getKey().split(",");
            if (this.isReplaceable(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]))) continue;
            iterator.remove();
        }
    }

    Object[] rayCast(float yaw, float pitch) {
        Object[] hit = this.client.raycastBlock(this.reach(), yaw, pitch);
        if (hit == null || hit[0] == null || hit[2] == null) {
            return null;
        }
        int face = this.faceFromName((String)hit[2]);
        if (face < 0 || face == 0) {
            return null;
        }
        int[] supportPos = this.posFromVec((Vec3)hit[0]);
        Vec3 offset = (Vec3)hit[1];
        Vec3 hitAbs = new Vec3((double)supportPos[0] + offset.x, (double)supportPos[1] + offset.y, (double)supportPos[2] + offset.z);
        return new Object[]{supportPos, face, hitAbs};
    }

    Vec3 getEyes(Entity player) {
        Vec3 pos = player.getPosition();
        return new Vec3(pos.x, pos.y + (double)player.getEyeHeight(), pos.z);
    }

    String blockNameAt(int x, int y, int z) {
        Block block = this.world.getBlockAt(x, y, z);
        return block == null || block.name == null ? "air" : block.name.toLowerCase();
    }

    boolean isReplaceable(int x, int y, int z) {
        return this.isReplaceableName(this.blockNameAt(x, y, z), false);
    }

    boolean isReplaceableName(String name, boolean airOnly) {
        if (airOnly) {
            return name.equals("air");
        }
        for (String replaceable : this.REPLACEABLE_BLOCKS) {
            if (!name.equals(replaceable)) continue;
            return true;
        }
        for (String replaceable : this.EXPERIMENTAL_REPLACEABLE_BLOCKS) {
            if (!name.equals(replaceable)) continue;
            return true;
        }
        return false;
    }

    boolean isInteractable(int x, int y, int z) {
        Block block = this.world.getBlockAt(x, y, z);
        if (block == null) {
            return false;
        }
        if (block.interactable) {
            return true;
        }
        if (block.type == null) {
            return false;
        }
        for (String interactableType : this.INTERACTABLE_TYPES) {
            if (!block.type.equals(interactableType)) continue;
            return true;
        }
        return false;
    }

    double reach() {
        return this.client.isCreative() ? 5.0 : 4.5;
    }

    int placementTick(Entity player) {
        if (this.isRavenTimerActive()) {
            return (int)(this.client.time() / 50L);
        }
        return player.getTicksExisted();
    }

    boolean isRavenTimerActive() {
        try {
            return this.modules.isEnabled("Timer");
        }
        catch (Exception ignored) {
            return false;
        }
    }

    float candidatePitch(Object[] candidate) {
        return this.clampFloat(((Float)candidate[0]).floatValue(), -90.0f, 90.0f);
    }

    int[] candidateSupportPos(Object[] candidate) {
        return (int[])candidate[1];
    }

    int candidateFace(Object[] candidate) {
        return (Integer)candidate[2];
    }

    Vec3 candidateHitVec(Object[] candidate) {
        return (Vec3)candidate[3];
    }

    int[] candidatePlacedPos(Object[] candidate) {
        return (int[])candidate[4];
    }

    float sanitizePitch(float pitch, float fallbackPitch) {
        float safeFallback = this.clampFloat(Float.isNaN(fallbackPitch) ? 0.0f : fallbackPitch, -90.0f, 90.0f);
        if (Float.isNaN(pitch) || Float.isInfinite(pitch)) {
            return safeFallback;
        }
        return this.clampFloat(pitch, -90.0f, 90.0f);
    }

    int floor(double value) {
        int i = (int)value;
        return value < (double)i ? i - 1 : i;
    }

    float clampFloat(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    float wrapAngle(float angle) {
        if ((angle %= 360.0f) >= 180.0f) {
            angle -= 360.0f;
        }
        if (angle < -180.0f) {
            angle += 360.0f;
        }
        return angle;
    }

    double distSq(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    boolean posEquals(int[] a, int[] b) {
        return a != null && b != null && a[0] == b[0] && a[1] == b[1] && a[2] == b[2];
    }

    String posKey(int[] pos) {
        return pos[0] + "," + pos[1] + "," + pos[2];
    }

    int[] posFromVec(Vec3 vec) {
        return new int[]{this.floor(vec.x), this.floor(vec.y), this.floor(vec.z)};
    }

    int[] offsetPos(int[] pos, int face) {
        if (face == 0) {
            return new int[]{pos[0], pos[1] - 1, pos[2]};
        }
        if (face == 1) {
            return new int[]{pos[0], pos[1] + 1, pos[2]};
        }
        if (face == 2) {
            return new int[]{pos[0], pos[1], pos[2] - 1};
        }
        if (face == 3) {
            return new int[]{pos[0], pos[1], pos[2] + 1};
        }
        if (face == 4) {
            return new int[]{pos[0] - 1, pos[1], pos[2]};
        }
        return new int[]{pos[0] + 1, pos[1], pos[2]};
    }

    int opposite(int face) {
        if (face == 0) {
            return 1;
        }
        if (face == 1) {
            return 0;
        }
        if (face == 2) {
            return 3;
        }
        if (face == 3) {
            return 2;
        }
        if (face == 4) {
            return 5;
        }
        return 4;
    }

    int rotateY(int face) {
        if (face == 2) {
            return 5;
        }
        if (face == 5) {
            return 3;
        }
        if (face == 3) {
            return 4;
        }
        if (face == 4) {
            return 2;
        }
        return face;
    }

    int rotateYCCW(int face) {
        if (face == 2) {
            return 4;
        }
        if (face == 4) {
            return 3;
        }
        if (face == 3) {
            return 5;
        }
        if (face == 5) {
            return 2;
        }
        return face;
    }

    int facingFromYaw(float yaw) {
        int index = this.floor((double)yaw / 90.0 + 0.5) & 3;
        if (index == 0) {
            return 3;
        }
        if (index == 1) {
            return 4;
        }
        if (index == 2) {
            return 2;
        }
        return 5;
    }

    String faceName(int face) {
        if (face == 0) {
            return "DOWN";
        }
        if (face == 1) {
            return "UP";
        }
        if (face == 2) {
            return "NORTH";
        }
        if (face == 3) {
            return "SOUTH";
        }
        if (face == 4) {
            return "WEST";
        }
        return "EAST";
    }

    int faceFromName(String name) {
        if (name == null) {
            return -1;
        }
        String upper = name.toUpperCase();
        if (upper.equals("DOWN")) {
            return 0;
        }
        if (upper.equals("UP")) {
            return 1;
        }
        if (upper.equals("NORTH")) {
            return 2;
        }
        if (upper.equals("SOUTH")) {
            return 3;
        }
        if (upper.equals("WEST")) {
            return 4;
        }
        if (upper.equals("EAST")) {
            return 5;
        }
        return -1;
    }

    private static Direction enumFacing(String name, Direction fallback) {
        if (name == null) {
            return fallback;
        }
        try {
            return Direction.valueOf((String)name.toUpperCase());
        }
        catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static String stripMinecraftPrefix(String registryName) {
        if (registryName == null) {
            return "air";
        }
        return registryName.startsWith("minecraft:") ? registryName.substring("minecraft:".length()) : registryName;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replace("-", "").replace("_", "").replace(" ", "");
    }

    private static final class PlayerState {
        double x;
        double y;
        double z;
        float yaw;
        float pitch;
        boolean onGround;
        boolean isSprinting;
        boolean isSneaking;
        boolean rotated;

        PlayerState(UpdateEvent event) {
            if (mc.player != null) {
                this.x = mc.player.getX();
                this.y = mc.player.getY();
                this.z = mc.player.getZ();
                this.onGround = mc.player.isOnGround();
                this.isSprinting = mc.player.isSprinting();
                this.isSneaking = mc.player.isSneaking();
            }
            this.yaw = event.getNewYaw();
            this.pitch = event.getNewPitch();
            this.rotated = true;
        }
    }

    private static final class S23
    extends SPacket {
        final Vec3 position;
        final Block block;

        S23(BlockUpdateS2CPacket packet) {
            super((Packet<?>)packet);
            this.position = Vec3.convert(packet.getPos());
            this.block = new Block(packet.getState(), packet.getPos());
        }
    }

    private static class SPacket {
        final String name;
        final Packet<?> packet;

        SPacket(Packet<?> packet) {
            this.packet = packet;
            this.name = packet == null ? "" : packet.getClass().getSimpleName();
        }

        static SPacket from(Packet<?> packet) {
            if (packet instanceof BlockUpdateS2CPacket) {
                return new S23((BlockUpdateS2CPacket)packet);
            }
            return packet == null ? null : new SPacket(packet);
        }
    }

    private static final class C0B
    extends CPacket {
        final String action;
        final int horsePower;

        C0B(ClientCommandC2SPacket packet) {
            super((Packet<?>)packet);
            this.action = packet.getMode() == null ? "" : packet.getMode().name();
            this.horsePower = packet.getMountJumpHeight();
        }
    }

    private static final class C08
    extends CPacket {
        final ItemStack itemStack;
        final Vec3 position;
        final int direction;
        final Vec3 offset;

        C08(PlayerInteractBlockC2SPacket packet) {
            super((Packet<?>)packet);
            this.itemStack = mc.player == null ? null : ItemStack.convert(mc.player.getStackInHand(packet.getHand()));
            BlockHitResult blockHitResult = packet.getBlockHitResult();
            this.position = Vec3.convert(blockHitResult.getBlockPos());
            this.direction = blockHitResult.getSide().getId();
            this.offset = new Vec3(blockHitResult.getPos().x, blockHitResult.getPos().y, blockHitResult.getPos().z);
        }

        C08(PlayerInteractItemC2SPacket packet) {
            super((Packet<?>)packet);
            this.itemStack = mc.player == null ? null : ItemStack.convert(mc.player.getStackInHand(packet.getHand()));
            this.position = null;
            this.direction = 255;
            this.offset = null;
        }
    }

    private static final class C07
    extends CPacket {
        final Vec3 position;
        final String status;
        final String facing;

        C07(PlayerActionC2SPacket packet) {
            super((Packet<?>)packet);
            this.position = Vec3.convert(packet.getPos());
            this.status = packet.getAction() == null ? "" : packet.getAction().name();
            this.facing = packet.getDirection() == null ? "" : packet.getDirection().name();
        }
    }

    private static final class C03
    extends CPacket {
        final Vec3 position;
        final float yaw;
        final float pitch;
        final boolean ground;
        final boolean moving;

        C03(PlayerMoveC2SPacket packet) {
            super((Packet<?>)packet);
            this.moving = packet instanceof PlayerMoveC2SPacket.PositionAndOnGround || packet instanceof PlayerMoveC2SPacket.Full;
            this.position = this.moving ? new Vec3(packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0)) : null;
            boolean rotating = packet instanceof PlayerMoveC2SPacket.LookAndOnGround || packet instanceof PlayerMoveC2SPacket.Full;
            this.yaw = rotating ? packet.getYaw(0.0f) : 0.0f;
            this.pitch = rotating ? packet.getPitch(0.0f) : 0.0f;
            this.ground = packet.isOnGround();
        }
    }

    private static final class C02
    extends CPacket {
        final Entity entity;
        final String action;

        C02(PlayerInteractEntityC2SPacket packet) {
            super((Packet<?>)packet);
            this.entity = mc.world == null ? null : Entity.convert(mc.world.getEntityById(((PlayerInteractEntityC2SPacketAccessor)packet).getEntityId()));
            this.action = C02.actionOf(packet);
        }

        private static String actionOf(PlayerInteractEntityC2SPacket packet) {
            final String[] action = {""};
            try {
                packet.handle(new PlayerInteractEntityC2SPacket.Handler() {
                    @Override
                    public void interact(Hand hand) {
                        action[0] = "INTERACT";
                    }

                    @Override
                    public void interactAt(Hand hand, Vec3d pos) {
                        action[0] = "INTERACT_AT";
                    }

                    @Override
                    public void attack() {
                        action[0] = "ATTACK";
                    }
                });
            }
            catch (Exception ignored) {
                // empty catch block
            }
            return action[0];
        }
    }

    private static class CPacket {
        final String name;
        final Packet<?> packet;

        CPacket(Packet<?> packet) {
            this.packet = packet;
            this.name = packet == null ? "" : packet.getClass().getSimpleName();
        }

        static CPacket from(Packet<?> packet) {
            if (packet instanceof PlayerInteractEntityC2SPacket) {
                return new C02((PlayerInteractEntityC2SPacket)packet);
            }
            if (packet instanceof PlayerMoveC2SPacket) {
                return new C03((PlayerMoveC2SPacket)packet);
            }
            if (packet instanceof PlayerActionC2SPacket) {
                return new C07((PlayerActionC2SPacket)packet);
            }
            if (packet instanceof PlayerInteractBlockC2SPacket) {
                return new C08((PlayerInteractBlockC2SPacket)packet);
            }
            if (packet instanceof PlayerInteractItemC2SPacket) {
                return new C08((PlayerInteractItemC2SPacket)packet);
            }
            if (packet instanceof ClientCommandC2SPacket) {
                return new C0B((ClientCommandC2SPacket)packet);
            }
            return packet == null ? null : new CPacket(packet);
        }
    }

    private static final class Vec3 {
        double x;
        double y;
        double z;

        Vec3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Vec3(BlockPos pos) {
            this(pos.getX(), pos.getY(), pos.getZ());
        }

        static Vec3 convert(BlockPos pos) {
            return pos == null ? null : new Vec3(pos);
        }

        static BlockPos getBlockPos(Vec3 vec) {
            return new BlockPos((int)vec.x, (int)vec.y, (int)vec.z);
        }

        public String toString() {
            return "Vec3(" + this.x + "," + this.y + "," + this.z + ")";
        }
    }

    private static final class Block {
        final String type;
        final String name;
        final boolean interactable;
        final int variant;
        final double height;
        final double width;
        final double length;
        final double x;
        final double y;
        final double z;

        Block(net.minecraft.block.Block block, BlockPos pos) {
            this(block == null ? Blocks.AIR.getDefaultState() : block.getDefaultState(), pos);
        }

        Block(BlockState state, BlockPos pos) {
            net.minecraft.block.Block block = state == null ? Blocks.AIR : state.getBlock();
            this.type = block.getClass().getSimpleName();
            this.name = Telly.stripMinecraftPrefix(block.getRegistryEntry().getKey().map(key -> key.getValue().toString()).orElse(""));
            this.interactable = BlockUtil.isInteractable(block);
            this.variant = net.minecraft.block.Block.STATE_IDS.getRawId(state == null ? block.getDefaultState() : state);
            if (Telly.mc.world != null) {
                VoxelShape shape = (state == null ? block.getDefaultState() : state).getCollisionShape(Telly.mc.world, pos);
                this.height = shape.getMax(Direction.Axis.Y) - shape.getMin(Direction.Axis.Y);
                this.width = shape.getMax(Direction.Axis.X) - shape.getMin(Direction.Axis.X);
                this.length = shape.getMax(Direction.Axis.Z) - shape.getMin(Direction.Axis.Z);
            } else {
                this.height = 1.0;
                this.width = 1.0;
                this.length = 1.0;
            }
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        }
    }

    private static final class ItemStack {
        final net.minecraft.item.ItemStack itemStack;
        final String type;
        final String name;
        final String displayName;
        final int stackSize;
        final int maxStackSize;
        final int durability;
        final int maxDurability;
        final int meta;
        final boolean isBlock;

        ItemStack(net.minecraft.item.ItemStack itemStack) {
            this.itemStack = itemStack;
            if (itemStack == null) {
                this.type = "";
                this.name = "";
                this.displayName = "";
                this.stackSize = 0;
                this.maxStackSize = 0;
                this.durability = 0;
                this.maxDurability = 0;
                this.meta = 0;
                this.isBlock = false;
                return;
            }
            Item item = itemStack.getItem();
            this.isBlock = item instanceof BlockItem;
            this.type = this.isBlock ? ((BlockItem)item).getBlock().getClass().getSimpleName() : item.getClass().getSimpleName();
            String registry = item.getRegistryEntry().getKey().map(key -> key.getValue().toString()).orElse("");
            this.name = Telly.stripMinecraftPrefix(registry);
            this.displayName = itemStack.getName().getString();
            this.stackSize = itemStack.getCount();
            this.maxStackSize = itemStack.getMaxCount();
            this.maxDurability = itemStack.getMaxDamage();
            this.durability = this.maxDurability - itemStack.getDamage();
            this.meta = itemStack.getDamage();
        }

        static ItemStack convert(net.minecraft.item.ItemStack stack) {
            return stack == null ? null : new ItemStack(stack);
        }
    }

    private static final class Entity {
        final net.minecraft.entity.Entity entity;
        final boolean isLiving;
        final boolean isPlayer;
        final boolean isUser;
        final String type;
        final int entityId;

        Entity(net.minecraft.entity.Entity entity) {
            this.entity = entity;
            this.isLiving = entity instanceof LivingEntity;
            this.isPlayer = entity instanceof PlayerEntity;
            this.isUser = entity != null && mc.player != null && entity.getUuid().equals(mc.player.getUuid());
            this.type = entity == null ? "" : entity.getClass().getSimpleName();
            this.entityId = entity == null ? -1 : entity.getId();
        }

        static Entity convert(net.minecraft.entity.Entity entity) {
            return entity == null ? null : new Entity(entity);
        }

        boolean isDead() {
            return this.entity == null || this.entity.isRemoved() || this.entity instanceof LivingEntity && ((LivingEntity)this.entity).hurtTime > 0;
        }

        boolean isHoldingBlock() {
            if (!(this.entity instanceof LivingEntity)) {
                return false;
            }
            return ItemUtil.isBlock(((LivingEntity)this.entity).getMainHandStack());
        }

        ItemStack getHeldItem() {
            if (this.entity instanceof ItemEntity) {
                return ItemStack.convert(((ItemEntity)this.entity).getStack());
            }
            if (!(this.entity instanceof LivingEntity)) {
                return null;
            }
            return ItemStack.convert(((LivingEntity)this.entity).getMainHandStack());
        }

        Vec3 getPosition() {
            return this.entity == null ? null : new Vec3(this.entity.getX(), this.entity.getY(), this.entity.getZ());
        }

        Vec3 getLastPosition() {
            return this.entity == null ? null : new Vec3(this.entity.prevX, this.entity.prevY, this.entity.prevZ);
        }

        Vec3 getMotion() {
            return this.entity == null ? null : new Vec3(this.entity.getVelocity().x, this.entity.getVelocity().y, this.entity.getVelocity().z);
        }

        float getFallDistance() {
            return this.entity == null ? 0.0f : this.entity.fallDistance;
        }

        float getYaw() {
            return this.entity == null ? 0.0f : this.entity.getYaw();
        }

        float getPitch() {
            return this.entity == null ? 0.0f : this.entity.getPitch();
        }

        float getPrevYaw() {
            return this.entity == null ? 0.0f : this.entity.prevYaw;
        }

        float getPrevPitch() {
            return this.entity == null ? 0.0f : this.entity.prevPitch;
        }

        int getTicksExisted() {
            return this.entity == null ? 0 : this.entity.age;
        }

        float getEyeHeight() {
            return this.entity == null ? 0.0f : this.entity.getEyeHeight(this.entity.getPose());
        }

        float getHeight() {
            return this.entity == null ? 0.0f : this.entity.getHeight();
        }

        float getWidth() {
            return this.entity == null ? 0.0f : this.entity.getWidth();
        }

        boolean onGround() {
            return this.entity != null && this.entity.isOnGround();
        }

        void setYaw(float yaw) {
            if (this.entity != null) {
                this.entity.setYaw(yaw);
            }
        }

        void setPitch(float pitch) {
            if (this.entity != null) {
                this.entity.setPitch(pitch);
            }
        }
    }

    private static final class UtilApi {
        private UtilApi() {
        }

        String color(String message) {
            return ChatColors.formatColor(message);
        }
    }

    private static final class GlApi {
        private GlApi() {
        }

        void push() {
        }

        void pop() {
        }

        void blend(boolean value) {
            if (value) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }
        }

        void texture2d(boolean value) {
        }

        void alpha(boolean value) {
        }

        void cull(boolean value) {
            if (value) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }
        }

        void depth(boolean value) {
            if (value) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
        }

        void depthMask(boolean value) {
            RenderSystem.depthMask(value);
        }

        void translate(double x, double y, double z) {
        }

        void color(int r, int g, int b, int a) {
            RenderSystem.setShaderColor((float)((float)r / 255.0f), (float)((float)g / 255.0f), (float)((float)b / 255.0f), (float)((float)a / 255.0f));
        }

        void color(float r, float g, float b, float a) {
            RenderSystem.setShaderColor((float)GlApi.norm(r), (float)GlApi.norm(g), (float)GlApi.norm(b), (float)GlApi.norm(a));
        }

        void begin(int mode) {
        }

        void end() {
        }

        void vertex3(double x, double y, double z) {
        }

        void lineWidth(float width) {
            RenderSystem.lineWidth(width);
        }

        void resetColor() {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        private static float norm(float v) {
            return v > 1.0f ? v / 255.0f : v;
        }
    }

    private final class RenderApi {
        private RenderApi() {
        }

        Vec3 getPosition() {
            if (mc.player == null) {
                return new Vec3(0.0, 0.0, 0.0);
            }
            if (mc.gameRenderer == null || mc.gameRenderer.getCamera() == null) {
                return new Vec3(0.0, 0.0, 0.0);
            }
            Vec3d cam = mc.gameRenderer.getCamera().getPos();
            return new Vec3(cam.x, cam.y, cam.z);
        }

        int getFontWidth(String text) {
            return mc.textRenderer == null ? 0 : mc.textRenderer.getWidth(text);
        }

        void text(String text, float x, float y, float scale, int color, boolean shadow) {
            if (mc.textRenderer == null || Telly.this.renderContext == null) {
                return;
            }
            DrawContext context = Telly.this.renderContext;
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            if (scale != 1.0f) {
                matrices.scale(scale, scale, scale);
            }
            context.drawText(mc.textRenderer, text, (int)(x / scale), (int)(y / scale), color, shadow);
            matrices.pop();
        }

        double[] getRotations() {
            if (mc.player == null) {
                return null;
            }
            return new double[]{mc.player.getYaw(), mc.player.getPitch()};
        }
    }

    private final class WorldApi {
        private WorldApi() {
        }

        Block getBlockAt(int x, int y, int z) {
            if (mc.world == null) {
                return new Block(Blocks.AIR, new BlockPos(x, y, z));
            }
            BlockPos pos = new BlockPos(x, y, z);
            return new Block(mc.world.getBlockState(pos), pos);
        }
    }

    private final class KeybindsApi {
        private KeybindsApi() {
        }

        boolean isPressed(String key) {
            KeyBinding binding = this.binding(key);
            return binding != null && binding.isPressed();
        }

        void setPressed(String key, boolean pressed) {
            KeyBinding binding = this.binding(key);
            if (binding == null) {
                return;
            }
            KeyBindUtil.setKeyBindState(binding, pressed);
            try {
                ((KeyBindingAccessor)binding).setPressed(pressed);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }

        int getKeycode(String key) {
            KeyBinding binding = this.binding(key);
            if (binding == null) {
                return -1;
            }
            InputUtil.Key inputKey = InputUtil.fromTranslationKey(binding.getBoundKeyTranslationKey());
            return inputKey.getCategory() == InputUtil.Type.MOUSE ? inputKey.getCode() - 100 : inputKey.getCode();
        }

        int getKeyCode(String key) {
            return this.getKeycode(key);
        }

        boolean isMouseDown(int mouseButton) {
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), mouseButton) == GLFW.GLFW_PRESS;
        }

        boolean isKeyDown(int keyCode) {
            return KeyBindUtil.isKeyDown(keyCode);
        }

        private KeyBinding binding(String key) {
            String normalized;
            if (key == null || mc.options == null) {
                return null;
            }
            switch (normalized = Telly.normalize(key)) {
                case "forward": {
                    return mc.options.forwardKey;
                }
                case "back": {
                    return mc.options.backKey;
                }
                case "left": {
                    return mc.options.leftKey;
                }
                case "right": {
                    return mc.options.rightKey;
                }
                case "jump": {
                    return mc.options.jumpKey;
                }
                case "sneak": {
                    return mc.options.sneakKey;
                }
                case "sprint": {
                    return mc.options.sprintKey;
                }
                case "attack": {
                    return mc.options.attackKey;
                }
                case "use":
                case "useitem": {
                    return mc.options.useKey;
                }
                case "drop": {
                    return mc.options.dropKey;
                }
            }
            return null;
        }
    }

    private final class InventoryApi {
        private InventoryApi() {
        }

        int getSlot() {
            return mc.player == null ? 0 : mc.player.getInventory().selectedSlot;
        }

        void setSlot(int slot) {
            if (mc.player != null && slot >= 0 && slot <= 8) {
                mc.player.getInventory().selectedSlot = slot;
            }
        }

        ItemStack getStackInSlot(int slot) {
            if (mc.player == null || slot < 0 || slot >= mc.player.getInventory().size()) {
                return null;
            }
            return ItemStack.convert(mc.player.getInventory().getStack(slot));
        }
    }

    private final class ClientApi {
        private ClientApi() {
        }

        Entity getPlayer() {
            return mc.player == null ? null : Entity.convert((net.minecraft.entity.Entity)mc.player);
        }

        long time() {
            return System.currentTimeMillis();
        }

        boolean isCreative() {
            return mc.player != null && mc.player.getAbilities().creativeMode;
        }

        boolean isSneak() {
            return mc.player != null && mc.player.input != null && mc.player.input.playerInput.sneak();
        }

        void setSneak(boolean sneak) {
            if (mc.player != null && mc.player.input != null) {
                PlayerInput playerInput = mc.player.input.playerInput;
                mc.player.input.playerInput = new PlayerInput(playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right(), playerInput.jump(), sneak, playerInput.sprint());
            }
        }

        void setJump(boolean jump) {
            if (mc.player != null && mc.player.input != null) {
                PlayerInput playerInput = mc.player.input.playerInput;
                mc.player.input.playerInput = new PlayerInput(playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right(), jump, playerInput.sneak(), playerInput.sprint());
            }
        }

        void setForward(float forward) {
            if (mc.player != null && mc.player.input != null) {
                mc.player.input.movementForward = forward;
            }
        }

        void setStrafe(float strafe) {
            if (mc.player != null && mc.player.input != null) {
                mc.player.input.movementSideways = strafe;
            }
        }

        float getForward() {
            return mc.player == null || mc.player.input == null ? 0.0f : mc.player.input.movementForward;
        }

        float getStrafe() {
            return mc.player == null || mc.player.input == null ? 0.0f : mc.player.input.movementSideways;
        }

        void setSprinting(boolean sprinting) {
            if (mc.player != null) {
                mc.player.setSprinting(sprinting);
            }
        }

        Vec3 getMotion() {
            return mc.player == null ? new Vec3(0.0, 0.0, 0.0) : new Vec3(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z);
        }

        String getScreen() {
            return mc.currentScreen == null ? "" : mc.currentScreen.getClass().getSimpleName();
        }

        int[] getDisplaySize() {
            return new int[]{mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), (int)mc.getWindow().getScaleFactor()};
        }

        void print(String message) {
            ChatUtil.sendRaw(message);
        }

        Object[] raycastBlock(double distance) {
            if (Telly.this.running) {
                return this.raycastBlock(distance, Telly.this.scriptedRotationYaw, Telly.this.scriptedRotationPitch);
            }
            return this.raycastBlock(distance, mc.player == null ? 0.0f : mc.player.getYaw(), mc.player == null ? 0.0f : mc.player.getPitch());
        }

        Object[] raycastBlock(double distance, float yaw, float pitch) {
            if (mc.player == null || mc.world == null) {
                return null;
            }
            HitResult mop = RotationUtil.rayTrace(yaw, pitch, distance, 1.0f);
            if (mop == null || mop.getType() != HitResult.Type.BLOCK) {
                return null;
            }
            BlockHitResult blockHitResult = (BlockHitResult)mop;
            Vec3 pos = new Vec3(blockHitResult.getBlockPos());
            Vec3 offset = new Vec3(blockHitResult.getPos().x - pos.x, blockHitResult.getPos().y - pos.y, blockHitResult.getPos().z - pos.z);
            return new Object[]{pos, offset, blockHitResult.getSide().name()};
        }

        boolean placeBlock(Vec3 targetPos, String side, Vec3 hitVec) {
            if (mc.player == null || mc.world == null || mc.interactionManager == null || targetPos == null || side == null || hitVec == null) {
                return false;
            }
            Direction facing = Telly.enumFacing(side, Direction.UP);
            BlockPos pos = Vec3.getBlockPos(targetPos);
            Vec3d hit = new Vec3d(hitVec.x, hitVec.y, hitVec.z);
            return mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hit, facing, pos, false)).isAccepted();
        }

        void swing() {
            if (mc.player == null) {
                return;
            }
            mc.player.swingHand(Hand.MAIN_HAND);
            if (mc.player.networkHandler != null) {
                mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        }

        void swingReset() {
        }
    }

    private static final class BridgeApi {
        private static final Map<String, Object> VALUES = new HashMap<String, Object>();

        private BridgeApi() {
        }

        void add(String key) {
            VALUES.put(key, null);
        }

        void add(String key, Object value) {
            VALUES.put(key, value);
        }

        void remove(String key) {
            VALUES.remove(key);
        }

        boolean has(String key) {
            return VALUES.containsKey(key);
        }

        Object get(String key) {
            return VALUES.get(key);
        }
    }

    private final class ModulesApi {
        private ModulesApi() {
        }

        void registerDescription(String description) {
        }

        void registerButton(String name, boolean defaultValue) {
        }

        boolean getButton(String moduleName, String name) {
            Module module;
            String normalized = Telly.normalize(name);
            if (Telly.normalize(moduleName).equals(Telly.normalize("Telly"))) {
                if (normalized.equals("autoswap")) {
                    return (Boolean)Telly.this.autoSwap.getValue();
                }
                if (normalized.equals("disablesafewalk")) {
                    return (Boolean)Telly.this.disableSafeWalk.getValue();
                }
                if (normalized.equals("showactivationhitbox")) {
                    return (Boolean)Telly.this.showActivationHitbox.getValue();
                }
                if (normalized.equals("print")) {
                    return (Boolean)Telly.this.print.getValue();
                }
            }
            if ((module = this.getModule(moduleName)) == null || Myau.propertyManager == null) {
                return false;
            }
            try {
                Property<?> property = Myau.propertyManager.getProperty(module, name);
                Object value = property == null ? null : property.getValue();
                return value instanceof Boolean && (Boolean)value != false;
            }
            catch (Exception ignored) {
                return false;
            }
        }

        boolean isEnabled(String moduleName) {
            Module module = this.getModule(moduleName);
            return module != null && module.isEnabled();
        }

        void enable(String moduleName) {
            Module module = this.getModule(moduleName);
            if (module != null) {
                module.setEnabled(true);
            }
        }

        void disable(String moduleName) {
            Module module = this.getModule(moduleName);
            if (module != null) {
                module.setEnabled(false);
            }
        }

        private Module getModule(String moduleName) {
            if (moduleName == null || Myau.moduleManager == null) {
                return null;
            }
            return Myau.moduleManager.getModule(moduleName);
        }
    }
}
