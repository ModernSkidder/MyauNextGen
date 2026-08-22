package laoqi123.oneconfig.huds

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import laoqi123.Myau
import laoqi123.module.modules.HUD
import laoqi123.module.modules.TabGui
import laoqi123.oneconfig.huds.GlassSurface.glassPanel
import laoqi123.oneconfig.huds.HudText.hudText
import org.polyfrost.compose.composables.PolyCanvas
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager

/**
 * The cascading navigator, rendered through OneConfig's Compose HUD scene on frosted panels
 * (see [GlassSurface]).
 *
 * <p>Keyboard handling stays in [TabGui] itself: a HUD is a render layer and never receives
 * key events during gameplay. This reads the module's published selection state each frame
 * and draws it, so there is one source of truth for navigation and no risk of the two
 * disagreeing.
 *
 * <p>Columns cascade the way the vanilla version did — each one starts level with the row
 * selected in its parent — and the selection highlight glides between rows rather than
 * jumping, using the same frame-rate independent easing as the other HUDs.
 */
class TabGuiComposeHud private constructor() : Hud(
    "myau-tabgui-compose",
    "TabGui",
    Category.INFO,
) {

    private class Column(
        val rows: List<TabGui.Row>,
        val width: Float,
        val offsetY: Float,
        val selected: Int,
        val start: Int,
        val focused: Boolean,
        val visible: Boolean,
    )

    private val columns = mutableStateOf(emptyList<Column>())

    /** Smoothed highlight position per column, so the selection slides. */
    private val highlight = FloatArray(COLUMN_COUNT) { -1f }
    private var lastUpdateNanos = 0L

    init {
        padLeft = 0f
        padTop = 0f
        padRight = 0f
        padBottom = 0f
        showBackground = false
        bgRadius = GlassSurface.RADIUS
    }

    /** The blur samples the live scene and the highlight animates. */
    override val alwaysRedraw: Boolean
        get() = true

    @Composable
    override fun Content() {
        val cols = columns.value
        if (cols.isEmpty()) {
            PolyCanvas(modifier = PolyModifier.size(0f, 0f)) { _, _, _, _ -> }
            return
        }

        val hud = this@TabGuiComposeHud
        val font = HudText.font(hud)
        val lineHeight = HudText.lineHeight(hud)
        val rowHeight = lineHeight + ROW_PAD_Y * 2f

        val totalWidth = cols.filter { it.visible }
            .fold(0f) { acc, col -> acc + col.width + GAP }
            .coerceAtLeast(1f)
        val deepest = cols.filter { it.visible }.maxOfOrNull { col ->
            col.offsetY + visibleCount(col) * rowHeight
        } ?: rowHeight
        val accent = accentColor()

        PolyCanvas(modifier = PolyModifier.size(totalWidth, deepest)) { originX, originY, _, _ ->
            var x = originX
            cols.forEachIndexed { index, col ->
                if (!col.visible || col.rows.isEmpty()) return@forEachIndexed

                val count = visibleCount(col)
                val columnHeight = count * rowHeight
                val y = originY + col.offsetY

                glassPanel(x, y, col.width, columnHeight)

                // Selection highlight, slid into place rather than snapped.
                val shown = highlight[index]
                if (col.focused && shown >= 0f) {
                    rect(
                        x,
                        y + shown * rowHeight,
                        col.width,
                        rowHeight,
                        PolyColor(withAlpha(accent, HIGHLIGHT_ALPHA)),
                        GlassSurface.RADIUS,
                    )
                }

                for (i in 0 until count) {
                    val rowIndex = col.start + i
                    if (rowIndex >= col.rows.size) break
                    val row = col.rows[rowIndex]
                    val isSelected = rowIndex == col.selected

                    val baseline = y + i * rowHeight + ROW_PAD_Y - font.metrics.ascent
                    val labelColor = if (isSelected && col.focused) SELECTED_TEXT else row.color

                    // Value is right-aligned, so the name gets whatever room is left.
                    val valueWidth =
                        if (row.value.isEmpty()) 0f else HudText.width(hud, row.value)
                    val nameBox = col.width - TEXT_PADDING * 2f - valueWidth - VALUE_GAP

                    hudText(
                        hud,
                        row.name,
                        x + TEXT_PADDING,
                        baseline,
                        nameBox.coerceAtLeast(1f),
                        PolyColor(labelColor),
                    )
                    if (row.value.isNotEmpty()) {
                        hudText(
                            hud,
                            row.value,
                            x + col.width - TEXT_PADDING - valueWidth,
                            baseline,
                            valueWidth,
                            PolyColor(if (isSelected) SELECTED_TEXT else VALUE_TEXT),
                        )
                    }
                }

                x += col.width + GAP
            }
        }
    }

    override fun update(): Boolean {
        BlurCache.invalidate()

        val now = System.nanoTime()
        val dt = if (lastUpdateNanos == 0L) 0f else ((now - lastUpdateNanos) / 1.0E9).toFloat()
        lastUpdateNanos = now

        val tab = Myau.moduleManager?.getModule(TabGui::class.java) as? TabGui
        if (tab == null || !tab.isEnabled) {
            columns.value = emptyList()
            highlight.fill(-1f)
            return true
        }

        return try {
            val level = tab.level
            val categoryRows = tab.categoryRows
            val moduleRows = tab.moduleRows
            val settingRows = tab.settingRows
            val subRows = tab.subRows

            val categoryStart = tab.scrollStart(tab.categoryIndex, categoryRows.size)
            val moduleStart = tab.scrollStart(tab.moduleIndex, moduleRows.size)
            val settingSelected = tab.settingsSelectedRow
            val settingStart = tab.scrollStart(settingSelected, settingRows.size)
            val subStart = tab.scrollStart(tab.subIndex, subRows.size)

            // Each column starts level with the row selected in its parent, which is what
            // gives the navigator its cascading shape.
            val rowHeight = HudText.lineHeight(this) + ROW_PAD_Y * 2f
            val categoryY = 0f
            val moduleY = categoryY +
                (tab.categoryIndex - categoryStart).coerceAtLeast(0) * rowHeight
            val settingY = moduleY + (tab.moduleIndex - moduleStart).coerceAtLeast(0) * rowHeight
            val subY = settingY + (settingSelected - settingStart).coerceAtLeast(0) * rowHeight

            val built = listOf(
                Column(categoryRows, CATEGORY_WIDTH, categoryY, tab.categoryIndex,
                    categoryStart, level == 0, true),
                Column(moduleRows, MODULE_WIDTH, moduleY, tab.moduleIndex,
                    moduleStart, level == 1, level >= 1),
                Column(settingRows, SETTING_WIDTH, settingY, settingSelected,
                    settingStart, level == 2, level >= 2 && settingRows.isNotEmpty()),
                Column(subRows, SUB_WIDTH, subY, tab.subIndex,
                    subStart, level == 3, level >= 3 && subRows.isNotEmpty()),
            )
            columns.value = built

            // Animate each column's highlight towards its selected row.
            val step = dt.coerceAtMost(MAX_STEP)
            built.forEachIndexed { index, col ->
                val target = (col.selected - col.start).toFloat().coerceAtLeast(0f)
                highlight[index] = if (highlight[index] < 0f) {
                    target
                } else {
                    HudAnimation.approach(highlight[index], target, step, HIGHLIGHT_SPEED)
                }
            }
            true
        } catch (t: Throwable) {
            // A module whose settings changed shape mid-read must not take the HUD down.
            columns.value = emptyList()
            true
        }
    }

    private fun visibleCount(col: Column): Int =
        minOf(MAX_VISIBLE_ROWS, col.rows.size - col.start).coerceAtLeast(0)

    private fun accentColor(): Int {
        val hud = Myau.moduleManager?.getModule(HUD::class.java) as? HUD
        return hud?.getColor(System.currentTimeMillis())?.rgb ?: SELECTED_TEXT
    }

    private fun withAlpha(argb: Int, alpha: Int): Int =
        (alpha shl 24) or (argb and 0xFFFFFF)

    override fun updateFrequency(): Long = 16_000_000L

    override fun deletable(): Boolean = false

    override fun multipleInstancesAllowed(): Boolean = false

    companion object {
        private const val COLUMN_COUNT = 4
        private const val CATEGORY_WIDTH = 80f
        private const val MODULE_WIDTH = 82f
        private const val SETTING_WIDTH = 82f
        private const val SUB_WIDTH = 79f
        private const val GAP = 4f
        private const val TEXT_PADDING = 3f
        private const val VALUE_GAP = 4f
        private const val ROW_PAD_Y = 1.5f
        private const val MAX_VISIBLE_ROWS = 20

        private const val HIGHLIGHT_ALPHA = 0x59
        private const val HIGHLIGHT_SPEED = 18f
        private const val MAX_STEP = 0.05f

        private const val SELECTED_TEXT = 0xFFFFFFFF.toInt()
        private const val VALUE_TEXT = 0xFFCDCDCD.toInt()

        @Volatile
        private var active = false

        /** Lets the vanilla-drawn module stand down instead of double-drawing. */
        @JvmStatic
        fun isActive(): Boolean = active

        @JvmStatic
        fun register(): Boolean {
            return try {
                HudManager.register(TabGuiComposeHud(), laoqi123.oneconfig.MyauOneConfig.CONFIG_ID)
                active = true
                true
            } catch (t: Throwable) {
                com.mojang.logging.LogUtils.getLogger()
                    .error("[Myau] Failed to register the Compose TabGui", t)
                false
            }
        }
    }
}
