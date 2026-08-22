package laoqi123.oneconfig.huds

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import laoqi123.Myau
import laoqi123.module.modules.TargetHUD
import laoqi123.oneconfig.huds.GlassSurface.glassPanel
import laoqi123.oneconfig.huds.HudText.hudText
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.entity.LivingEntity
import net.minecraft.util.Identifier
import org.polyfrost.compose.composables.PolyCanvas
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer

/**
 * The target readout, rendered through OneConfig's Compose HUD scene on a frosted panel
 * (see [GlassSurface]).
 *
 * <h2>Why the head is drawn with OpenGL</h2>
 * Everything except the avatar is Skia: panel, text and health bar. The avatar cannot be,
 * because a player's skin lives in a GL texture rather than on the classpath, and getting its
 * pixels into Skia would mean a GPU-to-CPU readback every frame — far more expensive than the
 * blur this work set out to make cheap.
 *
 * <p>So the head is composited on top by OpenGL instead, through OneConfig's
 * [CompatOverlayRenderer] hook, which runs with a vanilla `DrawContext` after the Skia scene
 * has been drawn. The panel reserves the space and publishes where the head belongs; the
 * overlay draws into it. Both halves are in GUI-scaled coordinates, so they line up without
 * any conversion.
 */
class TargetHUDComposeHud private constructor() : Hud(
    "myau-targethud-compose",
    "TargetHUD",
    Category.COMBAT,
) {

    /** What to draw, refreshed by [update] so composition stays cheap. */
    private class Snapshot(
        val name: String,
        val healthText: String,
        val healthRatio: Float,
        val skin: Identifier?,
    )

    private val snapshot = mutableStateOf<Snapshot?>(null)

    /** Smoothed health so the bar glides instead of stepping. */
    private var shownRatio = 0f
    private var lastUpdateNanos = 0L

    init {
        padLeft = 6f
        padTop = 5f
        padRight = 8f
        padBottom = 5f
        showBackground = false
        bgRadius = GlassSurface.RADIUS
    }

    /** The blur samples the live scene and the health bar animates. */
    override val alwaysRedraw: Boolean
        get() = true

    @Composable
    override fun Content() {
        val data = snapshot.value
        if (data == null) {
            headSlot = null
            PolyCanvas(modifier = PolyModifier.size(0f, 0f)) { _, _, _, _ -> }
            return
        }

        val hud = this@TargetHUDComposeHud
        val font = HudText.font(hud)
        val lineHeight = HudText.lineHeight(hud)

        val textWidth = maxOf(
            HudText.width(hud, data.name),
            HudText.width(hud, data.healthText),
        )
        val panelWidth = padLeft + HEAD_SIZE + HEAD_GAP + textWidth + padRight
        val panelHeight = padTop + lineHeight * 2f + LINE_GAP + BAR_HEIGHT + BAR_GAP + padBottom

        PolyCanvas(modifier = PolyModifier.size(panelWidth, panelHeight)) { x, y, w, h ->
            glassPanel(x, y, w, h)

            // Publish where the GL pass should put the avatar. absoluteX/Y are already in
            // screen coordinates because the manager scales before translating.
            headSlot = if (data.skin != null) {
                HeadSlot(absoluteX + x + padLeft, absoluteY + y + padTop, HEAD_SIZE, data.skin)
            } else {
                null
            }

            val textX = x + padLeft + HEAD_SIZE + HEAD_GAP
            val nameBaseline = y + padTop - font.metrics.ascent
            hudText(hud, data.name, textX, nameBaseline, textWidth)

            val healthBaseline = nameBaseline + lineHeight + LINE_GAP
            hudText(
                hud,
                data.healthText,
                textX,
                healthBaseline,
                textWidth,
                PolyColor(healthColor(shownRatio)),
            )

            // Health bar spanning the text column, drained from the right.
            val barY = y + h - padBottom - BAR_HEIGHT
            val barWidth = w - (textX - x) - padRight
            rect(textX, barY, barWidth, BAR_HEIGHT, PolyColor(TRACK_COLOR), BAR_HEIGHT * 0.5f)
            if (shownRatio > 0f) {
                rect(
                    textX,
                    barY,
                    (barWidth * shownRatio).coerceAtLeast(BAR_HEIGHT),
                    BAR_HEIGHT,
                    PolyColor(healthColor(shownRatio)),
                    BAR_HEIGHT * 0.5f,
                )
            }
        }
    }

    override fun update(): Boolean {
        BlurCache.invalidate()

        val now = System.nanoTime()
        val dt = if (lastUpdateNanos == 0L) 0f else ((now - lastUpdateNanos) / 1.0E9).toFloat()
        lastUpdateNanos = now

        val module = Myau.moduleManager?.getModule(TargetHUD::class.java) as? TargetHUD
        if (module == null || !module.isEnabled) {
            snapshot.value = null
            return true
        }

        val target: LivingEntity? = runCatching { module.resolveTarget() }.getOrNull()
        if (target == null || !target.isAlive) {
            snapshot.value = null
            return true
        }

        val max = target.maxHealth.coerceAtLeast(1f)
        val ratio = (target.health / max).coerceIn(0f, 1f)
        shownRatio = HudAnimation.approach(shownRatio, ratio, dt.coerceAtMost(MAX_STEP), BAR_SPEED)

        snapshot.value = Snapshot(
            name = target.name.string,
            healthText = "%.1f HP".format(target.health),
            healthRatio = ratio,
            skin = runCatching { module.getSkin(target) }.getOrNull(),
        )
        return true
    }

    /** Green through yellow to red as health drops, mirroring the vanilla readout. */
    private fun healthColor(ratio: Float): Int {
        val r = ((1f - ratio) * 255f).toInt().coerceIn(0, 255)
        val g = (ratio * 255f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or 0x40
    }

    override fun updateFrequency(): Long = 16_000_000L

    override fun deletable(): Boolean = false

    override fun multipleInstancesAllowed(): Boolean = false

    companion object {
        private const val HEAD_SIZE = 22f
        private const val HEAD_GAP = 6f
        private const val LINE_GAP = 1f
        private const val BAR_HEIGHT = 3f
        private const val BAR_GAP = 2f
        private const val TRACK_COLOR = 0x66000000
        private const val BAR_SPEED = 10f
        private const val MAX_STEP = 0.05f

        /** Skin UVs for the face and the hat overlay on a 64x64 skin. */
        private const val SKIN_SIZE = 64
        private const val FACE_U = 8
        private const val FACE_V = 8
        private const val HAT_U = 40
        private const val HAT_V = 8
        private const val FACE_SPAN = 8

        /** Where the GL pass should draw the avatar, in screen coordinates. */
        private class HeadSlot(
            val x: Float,
            val y: Float,
            val size: Float,
            val skin: Identifier,
        )

        @Volatile
        private var headSlot: HeadSlot? = null

        @Volatile
        private var active = false

        /** Lets the vanilla-drawn module stand down instead of double-drawing. */
        @JvmStatic
        fun isActive(): Boolean = active

        @JvmStatic
        fun register(): Boolean {
            return try {
                HudManager.register(TargetHUDComposeHud(), laoqi123.oneconfig.MyauOneConfig.CONFIG_ID)
                installHeadOverlay()
                active = true
                true
            } catch (t: Throwable) {
                com.mojang.logging.LogUtils.getLogger()
                    .error("[Myau] Failed to register the Compose TargetHUD", t)
                false
            }
        }

        /**
         * Registers the GL pass that composites the avatar over the Skia scene.
         *
         * <p>Runs every frame the overlay hook fires, drawing only when the panel published a
         * slot, so an absent target costs nothing.
         *
         * <p>Registered reflectively: OneConfig ships compiled against intermediary names, so
         * its hook's parameter type is `class_332` and will not resolve against Yarn's
         * `DrawContext` at compile time even though they are the same class at runtime.
         */
        private fun installHeadOverlay() {
            val hook = java.lang.reflect.Proxy.newProxyInstance(
                Function1::class.java.classLoader,
                arrayOf(Function1::class.java),
            ) { _, method, args ->
                if (method.name == "invoke" && args != null && args.isNotEmpty()) {
                    val slot = headSlot
                    val context = args[0]
                    if (slot != null && context is DrawContext) {
                        runCatching { drawHead(context, slot) }
                    }
                }
                null
            }

            @Suppress("UNCHECKED_CAST")
            CompatOverlayRenderer::class.java
                .getMethod("register", Function1::class.java)
                .invoke(null, hook)
        }

        private fun drawHead(context: DrawContext, slot: HeadSlot) {
            val size = slot.size.toInt()
            val x = slot.x.toInt()
            val y = slot.y.toInt()

            // Face, then the hat layer on top, matching how vanilla composites a head.
            context.drawTexture(
                RenderLayer::getGuiTextured,
                slot.skin,
                x, y, FACE_U.toFloat(), FACE_V.toFloat(),
                size, size,
                FACE_SPAN, FACE_SPAN,
                SKIN_SIZE, SKIN_SIZE,
            )
            context.drawTexture(
                RenderLayer::getGuiTextured,
                slot.skin,
                x, y, HAT_U.toFloat(), HAT_V.toFloat(),
                size, size,
                FACE_SPAN, FACE_SPAN,
                SKIN_SIZE, SKIN_SIZE,
            )
        }

        /** Suppresses the avatar when the HUD is gone, so a stale slot cannot linger. */
        @JvmStatic
        fun clearHead() {
            headSlot = null
        }
    }
}
