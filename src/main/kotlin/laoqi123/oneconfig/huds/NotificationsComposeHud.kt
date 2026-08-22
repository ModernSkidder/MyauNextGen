package laoqi123.oneconfig.huds

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import laoqi123.oneconfig.huds.GlassSurface.glassPanel
import laoqi123.oneconfig.huds.HudText.hudText
import org.polyfrost.compose.composables.PolyCanvas
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Module toggle notifications, rendered through OneConfig's Compose HUD scene on frosted
 * panels (see [GlassSurface]).
 *
 * <p>Entries slide in from the anchored edge, hold while a remaining-time bar drains, then
 * slide back out. Motion is driven by wall-clock deltas with easing, so it looks the same at
 * any frame rate, and the stack closes the gap smoothly when an entry above expires.
 *
 * <p>Posting is thread-safe: toggles arrive from the game thread while the HUD renders on
 * the render thread, so new entries land in a concurrent queue and are absorbed during
 * [update].
 */
class NotificationsComposeHud private constructor() : Hud(
    "myau-notifications-compose",
    "Notifications",
    Category.INFO,
) {

    private class Entry(val name: String, val enabled: Boolean) {
        val born = System.currentTimeMillis()

        /** Slot this entry is heading for; fractional while the stack closes up. */
        var slot: Float = 0f

        /** Slot it currently occupies. */
        var currentSlot: Float = -1f

        /** 0 hidden, 1 fully present. Drives both the slide and the fade. */
        var presence: Float = 0f

        var leaving: Boolean = false

        /** 0..1 of the hold window elapsed, used for the drain bar. */
        val progress: Float
            get() = ((System.currentTimeMillis() - born).toFloat() / LIFETIME_MS)
                .coerceIn(0f, 1f)

        val expired: Boolean
            get() = System.currentTimeMillis() - born > LIFETIME_MS
    }

    private val entries = ArrayList<Entry>()
    private val visible = mutableStateOf(emptyList<Entry>())
    private var lastUpdateNanos = 0L

    init {
        padLeft = 8f
        padTop = 5f
        padRight = 8f
        padBottom = 5f
        showBackground = false
        bgRadius = GlassSurface.RADIUS
    }

    /** Entries are always mid-animation while any are on screen. */
    override val alwaysRedraw: Boolean
        get() = true

    @Composable
    override fun Content() {
        val rows = visible.value
        val font = HudText.font(this)
        val lineHeight = HudText.lineHeight(this)
        val rowHeight = lineHeight + padTop + padBottom
        val stride = rowHeight + ROW_GAP

        if (rows.isEmpty()) {
            PolyCanvas(modifier = PolyModifier.size(0f, 0f)) { _, _, _, _ -> }
            return
        }

        val hud = this@NotificationsComposeHud
        val widths = rows.map { entry ->
            HudText.width(hud, prefixOf(entry)) + HudText.width(hud, entry.name) +
                padLeft + padRight
        }
        val panelWidth = widths.max()
        val slots = rows.maxOf { maxOf(it.slot, it.currentSlot) } + 1f
        val panelHeight = slots * stride - ROW_GAP

        PolyCanvas(modifier = PolyModifier.size(panelWidth, panelHeight)) { x, y, w, _ ->
            rows.forEachIndexed { index, entry ->
                val rowWidth = widths[index]

                val eased =
                    if (entry.leaving) 1f - HudAnimation.easeInCubic(1f - entry.presence)
                    else HudAnimation.easeOutBack(entry.presence)
                val alpha = entry.presence.coerceIn(0f, 1f)
                if (alpha <= 0.01f) return@forEachIndexed

                // Entries stack against the right edge of the HUD's own box, sliding in from
                // beyond it so they appear to come from off-screen.
                val restX = x + w - rowWidth
                val slide = (1f - eased) * rowWidth * SLIDE_FRACTION
                val rowX = restX + slide
                val rowY = y + entry.currentSlot * stride

                glassPanel(rowX, rowY, rowWidth, rowHeight)

                // Remaining-time bar along the bottom edge, inset to clear the corners.
                val remaining = 1f - entry.progress
                if (remaining > 0f) {
                    val trackWidth = rowWidth - GlassSurface.RADIUS * 2f
                    rect(
                        rowX + GlassSurface.RADIUS,
                        rowY + rowHeight - BAR_HEIGHT - 1f,
                        trackWidth * remaining,
                        BAR_HEIGHT,
                        PolyColor(withAlpha(accentOf(entry), alpha)),
                        BAR_HEIGHT * 0.5f,
                    )
                }

                val baseline = rowY + padTop - font.metrics.ascent
                val prefix = prefixOf(entry)
                val prefixWidth = HudText.width(hud, prefix)

                hudText(
                    hud,
                    prefix,
                    rowX + padLeft,
                    baseline,
                    prefixWidth,
                    PolyColor(withAlpha(accentOf(entry), alpha)),
                )
                hudText(
                    hud,
                    entry.name,
                    rowX + padLeft + prefixWidth,
                    baseline,
                    HudText.width(hud, entry.name),
                    PolyColor(withAlpha(HudText.color(hud).argb, alpha)),
                )
            }
        }
    }

    override fun update(): Boolean {
        val now = System.nanoTime()
        val dt = if (lastUpdateNanos == 0L) 0f else ((now - lastUpdateNanos) / 1.0E9).toFloat()
        lastUpdateNanos = now

        // Absorb anything posted from the game thread since the last frame.
        while (true) {
            val posted = pending.poll() ?: break
            entries += posted
            // Oldest first: drop from the front rather than letting the stack grow forever.
            while (entries.size > MAX_VISIBLE) {
                entries.removeAt(0)
            }
        }

        entries.forEach { if (it.expired) it.leaving = true }

        // Newest at the top of the stack, which is where the eye expects a new entry.
        entries.sortByDescending { it.born }
        entries.forEachIndexed { index, entry ->
            entry.slot = index.toFloat()
            if (entry.currentSlot < 0f) {
                entry.currentSlot = index.toFloat()
            }
        }

        val step = dt.coerceAtMost(MAX_STEP)
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val target = if (entry.leaving) 0f else 1f
            entry.presence = HudAnimation.approach(entry.presence, target, step, PRESENCE_SPEED)
            entry.currentSlot =
                HudAnimation.approach(entry.currentSlot, entry.slot, step, SLOT_SPEED)
            if (entry.leaving && entry.presence <= 0.001f) {
                iterator.remove()
            }
        }

        visible.value = entries.toList()
        return true
    }

    private fun prefixOf(entry: Entry): String =
        if (entry.enabled) "Enabled " else "Disabled "

    private fun accentOf(entry: Entry): Int =
        if (entry.enabled) ENABLED_COLOR else DISABLED_COLOR

    private fun withAlpha(argb: Int, factor: Float): Int {
        val alpha = (((argb ushr 24) and 0xFF) * factor.coerceIn(0f, 1f)).toInt()
        return (alpha shl 24) or (argb and 0xFFFFFF)
    }

    /** Every frame while entries are animating. */
    override fun updateFrequency(): Long = 16_000_000L

    override fun deletable(): Boolean = false

    override fun multipleInstancesAllowed(): Boolean = false

    companion object {
        private const val LIFETIME_MS = 2600f
        private const val ROW_GAP = 3f
        private const val BAR_HEIGHT = 2f
        private const val MAX_VISIBLE = 6

        /** How far an entry starts off-edge, as a fraction of its own width. */
        private const val SLIDE_FRACTION = 1.05f

        private const val PRESENCE_SPEED = 11f
        private const val SLOT_SPEED = 14f
        private const val MAX_STEP = 0.05f

        /** Toggle-state accents, matching PolyColor's GREEN and RED. */
        private const val ENABLED_COLOR = 0xFF44FF44.toInt()
        private const val DISABLED_COLOR = 0xFFFF4444.toInt()

        /**
         * Posted from the game thread, drained on the render thread. A concurrent queue
         * avoids having to lock the entry list that composition reads.
         */
        private val pending = ConcurrentLinkedQueue<Entry>()

        @Volatile
        private var active = false

        /** Lets the vanilla-drawn module stand down instead of double-drawing. */
        @JvmStatic
        fun isActive(): Boolean = active

        /** Queues a toggle notification. Safe to call from any thread. */
        @JvmStatic
        fun post(moduleName: String, enabled: Boolean) {
            pending += Entry(moduleName, enabled)
        }

        @JvmStatic
        fun register(): Boolean {
            return try {
                HudManager.register(NotificationsComposeHud(), laoqi123.oneconfig.MyauOneConfig.CONFIG_ID)
                active = true
                true
            } catch (t: Throwable) {
                com.mojang.logging.LogUtils.getLogger()
                    .error("[Myau] Failed to register the Compose notifications", t)
                false
            }
        }
    }
}
