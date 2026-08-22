package laoqi123.oneconfig.huds

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import laoqi123.Myau
import laoqi123.module.modules.HUD
import laoqi123.oneconfig.huds.GlassSurface.glassPanel
import laoqi123.oneconfig.huds.HudText.hudText
import org.polyfrost.compose.composables.PolyCanvas
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager

/**
 * The enabled-module list, rendered through OneConfig's Compose HUD scene on frosted panels
 * (see [GlassSurface]).
 *
 * <p>Layout mirrors the original vanilla-drawn one: one panel per row rather than a single
 * shared box, the module name in the gradient colour with its suffixes in grey beside it, an
 * accent bar down the side of each row, and rows hugging whichever edge the module is
 * anchored to.
 *
 * <p>Rows animate. A newly enabled module slides in from the anchored edge while fading up,
 * a disabled one slides back out before being dropped, and rows glide to their new slot when
 * something above them leaves. All of it is driven by wall-clock deltas so the motion is the
 * same regardless of frame rate, with easing rather than a linear ramp.
 *
 * <p>Filtering, sorting and colours still belong to [HUD]; this only draws what that module
 * already computes.
 */
class ArrayListComposeHud private constructor() : Hud(
    "myau-arraylist-compose",
    "ArrayList",
    Category.INFO,
) {

    /** A row's animation state, kept between frames and keyed by module name. */
    private class RowState(var name: String) {
        /** Text as it should appear right now, including suffixes. */
        var label: String = name

        /** Suffix portion, drawn grey after the name. */
        var suffix: String = ""

        var argb: Int = -1

        /** Slot this row is heading for; fractional while it glides. */
        var slot: Float = 0f

        /** Slot it currently occupies. */
        var currentSlot: Float = -1f

        /** 0 hidden, 1 fully present. Drives both the slide-in and the fade. */
        var presence: Float = 0f

        /** Set when the module switched off, so the row plays its exit and is then removed. */
        var leaving: Boolean = false
    }

    /** Ordered snapshot handed to composition; rebuilt by [update]. */
    private val visible = mutableStateOf(emptyList<RowState>())

    private val states = LinkedHashMap<String, RowState>()
    private var alignRight = false
    private var showSidebar = false
    private var lastUpdateNanos = 0L

    init {
        // Rows carry their own padding, so the HUD itself adds none.
        padLeft = 0f
        padTop = 0f
        padRight = 0f
        padBottom = 0f
        showBackground = false
        bgRadius = GlassSurface.RADIUS
    }

    /**
     * The blur samples the live scene, the gradient cycles on wall-clock time and rows are
     * mid-animation, so a cached redraw would freeze all three.
     */
    override val alwaysRedraw: Boolean
        get() = true

    @Composable
    override fun Content() {
        val rows = visible.value
        val font = HudText.font(this)
        val lineHeight = HudText.lineHeight(this)
        val rowHeight = lineHeight + ROW_PAD_Y * 2f
        val stride = rowHeight + ROW_GAP

        if (rows.isEmpty()) {
            PolyCanvas(modifier = PolyModifier.size(0f, 0f)) { _, _, _, _ -> }
            return
        }

        val hud = this@ArrayListComposeHud
        val widths = rows.map { row ->
            val nameWidth = HudText.width(hud, row.name)
            val suffixWidth =
                if (row.suffix.isEmpty()) 0f else SUFFIX_GAP + HudText.width(hud, row.suffix)
            nameWidth + suffixWidth + ROW_PAD_X * 2f + if (showSidebar) SIDEBAR_WIDTH else 0f
        }
        val panelWidth = widths.max()
        // Sized for the furthest slot in use so a row gliding into place is never clipped.
        val slots = rows.maxOf { maxOf(it.slot, it.currentSlot) } + 1f
        val panelHeight = slots * stride - ROW_GAP

        PolyCanvas(modifier = PolyModifier.size(panelWidth, panelHeight)) { x, y, w, _ ->
            rows.forEachIndexed { index, row ->
                val rowWidth = widths[index]

                // Eased presence drives both axes: rows enter from the anchored edge and
                // fade up together, which reads as one motion rather than two.
                val eased =
                    if (row.leaving) 1f - HudAnimation.easeInCubic(1f - row.presence)
                    else HudAnimation.easeOutCubic(row.presence)
                val slide = (1f - eased) * rowWidth * SLIDE_FRACTION
                val alpha = eased

                if (alpha <= 0.01f) return@forEachIndexed

                // Each row hugs the anchored edge, which is what gives the list its
                // staircase outline instead of one flush block.
                val restX = if (alignRight) x + w - rowWidth else x
                val rowX = if (alignRight) restX + slide else restX - slide
                val rowY = y + row.currentSlot * stride

                glassPanel(rowX, rowY, rowWidth, rowHeight)

                if (showSidebar) {
                    // Accent bar on the outer edge, matching the row's gradient colour.
                    val barX = if (alignRight) rowX + rowWidth - SIDEBAR_WIDTH else rowX
                    rect(
                        barX,
                        rowY,
                        SIDEBAR_WIDTH,
                        rowHeight,
                        PolyColor(withAlpha(row.argb, alpha)),
                        0f,
                    )
                }

                val contentX =
                    if (alignRight) rowX + ROW_PAD_X
                    else rowX + ROW_PAD_X + if (showSidebar) SIDEBAR_WIDTH else 0f
                val baseline = rowY + ROW_PAD_Y - font.metrics.ascent

                // Alignment is handled per row by the layout above, so each label is drawn
                // into a box exactly its own width to keep the name and suffix adjacent.
                val nameWidth = HudText.width(hud, row.name)
                hudText(
                    hud,
                    row.name,
                    contentX,
                    baseline,
                    nameWidth,
                    PolyColor(withAlpha(row.argb, alpha)),
                )
                if (row.suffix.isNotEmpty()) {
                    hudText(
                        hud,
                        row.suffix,
                        contentX + nameWidth + SUFFIX_GAP,
                        baseline,
                        HudText.width(hud, row.suffix),
                        PolyColor(withAlpha(SUFFIX_COLOR, alpha)),
                    )
                }
            }
        }
    }

    override fun update(): Boolean {
        val now = System.nanoTime()
        val dt = if (lastUpdateNanos == 0L) 0f else ((now - lastUpdateNanos) / 1.0E9).toFloat()
        lastUpdateNanos = now

        val hud = Myau.moduleManager?.getModule(HUD::class.java) as? HUD
        if (hud == null || !hud.isEnabled) {
            // Let existing rows animate out rather than blinking away.
            states.values.forEach { it.leaving = true }
            advance(dt.coerceAtMost(MAX_STEP))
            return true
        }

        // Rows hug the nearer screen edge, decided by which half of the screen the HUD sits
        // in. `Hud.alignment` is not the right signal: it defaults to Center and describes
        // content placement inside a fixed-width HUD, not which edge the HUD is docked to, so
        // it never changed when the HUD was dragged. The module's own position-x still forces
        // right alignment for anyone who set it deliberately.
        val screenWidth = net.minecraft.client.MinecraftClient.getInstance()
            .window?.scaledWidth?.toFloat() ?: 0f
        val pastMidpoint = screenWidth > 0f && x + renderedW / 2f > screenWidth / 2f
        alignRight = pastMidpoint || hud.posX.value == 1
        showSidebar = hud.showBar.value && hud.sidebarMode.value != SIDEBAR_MODE_NONE

        val millis = System.currentTimeMillis()
        val active = hud.activeModules.toList()
        val seen = HashSet<String>(active.size)

        active.forEachIndexed { index, module ->
            // Keyed by the module's identity, not its label. getModuleName applies the
            // lower-case option and the label also carries live suffixes, so keying on it
            // orphaned a row's state the moment its text changed: the old entry was treated
            // as gone and animated out while a fresh one faded in, which looked like rows
            // vanishing at random.
            val key = module.javaClass.name
            seen += key
            val state = states.getOrPut(key) { RowState(hud.getModuleName(module)) }
            state.name = hud.getModuleName(module)
            state.leaving = false
            state.slot = index.toFloat()
            // A row appearing for the first time starts at its slot; presence handles the
            // slide, so it does not also need to travel vertically.
            if (state.currentSlot < 0f) {
                state.currentSlot = index.toFloat()
            }
            val suffixes = hud.getModuleSuffix(module)
            state.suffix = if (hud.suffixes.value && suffixes.isNotEmpty()) {
                suffixes.joinToString(" ")
            } else {
                ""
            }
            // Offsetting by the row index is what produces the per-row gradient.
            state.argb = hud.getColor(millis, index.toLong()).rgb
        }

        // Anything no longer active plays its exit animation before being dropped.
        // Keys, not labels: the map is keyed by module class so the check must match.
        states.forEach { (key, state) -> if (key !in seen) state.leaving = true }

        advance(dt.coerceAtMost(MAX_STEP))
        return true
    }

    /** Steps every row's animation and publishes the rows still worth drawing. */
    private fun advance(dt: Float) {
        val finished = ArrayList<String>()
        for ((key, state) in states) {
            val target = if (state.leaving) 0f else 1f
            state.presence = HudAnimation.approach(state.presence, target, dt, PRESENCE_SPEED)
            state.currentSlot =
                HudAnimation.approach(state.currentSlot, state.slot, dt, SLOT_SPEED)
            if (state.leaving && state.presence <= 0.001f) {
                finished += key
            }
        }
        finished.forEach(states::remove)

        // Sorted by the slot being animated towards so draw order matches the final layout.
        visible.value = states.values.sortedBy { it.slot }
    }

    /** Scales a colour's alpha by [factor]. */
    private fun withAlpha(argb: Int, factor: Float): Int {
        val alpha = (((argb ushr 24) and 0xFF) * factor.coerceIn(0f, 1f)).toInt()
        return (alpha shl 24) or (argb and 0xFFFFFF)
    }

    /** Every frame: rows are animating and the gradient shifts with time. */
    override fun updateFrequency(): Long = 16_000_000L

    override fun deletable(): Boolean = false

    override fun multipleInstancesAllowed(): Boolean = false

    companion object {
        private const val ROW_PAD_X = 4f
        private const val ROW_PAD_Y = 2f
        private const val ROW_GAP = 1f
        private const val SUFFIX_GAP = 3f
        private const val SIDEBAR_WIDTH = 2f

        /** How far a row starts off-edge, as a fraction of its own width. */
        private const val SLIDE_FRACTION = 0.35f

        /** Reciprocal time constants: larger settles sooner. */
        private const val PRESENCE_SPEED = 14f
        private const val SLOT_SPEED = 16f

        /**
         * Caps the step used after a pause (world load, alt-tab) so rows ease in rather than
         * teleporting once updates resume.
         */
        private const val MAX_STEP = 0.05f

        /** Index of "NONE" in the module's `sidebar-mode` property. */
        private const val SIDEBAR_MODE_NONE = 4

        /** Matches the grey the vanilla renderer used for suffixes. */
        private const val SUFFIX_COLOR = 0xFFAAAAAA.toInt()

        @Volatile
        private var active = false

        /** Lets the vanilla-drawn module stand down instead of double-drawing. */
        @JvmStatic
        fun isActive(): Boolean = active

        @JvmStatic
        fun register(): Boolean {
            return try {
                HudManager.register(ArrayListComposeHud(), laoqi123.oneconfig.MyauOneConfig.CONFIG_ID)
                active = true
                true
            } catch (t: Throwable) {
                com.mojang.logging.LogUtils.getLogger()
                    .error("[Myau] Failed to register the Compose ArrayList", t)
                false
            }
        }
    }
}
