package laoqi123.oneconfig.huds

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import laoqi123.oneconfig.huds.GlassSurface.glassPanel
import laoqi123.oneconfig.huds.HudText.hudText
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.registry.Registries
import org.jetbrains.skia.Image
import org.polyfrost.compose.composables.PolyCanvas
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.render.ImageLoader
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager

/**
 * The active potion list, rendered through OneConfig's Compose HUD scene on a frosted
 * panel (see [GlassSurface]).
 *
 * <p>Effect icons come from the vanilla `textures/mob_effect/<id>.png` sprites, loaded as
 * Skia images. That avoids Minecraft's `DrawContext`, which cannot draw into the Skia
 * surface this HUD renders to.
 */
class PotionEffectsComposeHud private constructor() : Hud(
    "myau-potions-compose",
    "PotionEffects",
    Category.PLAYER,
) {

    /** Snapshot of what to draw, refreshed by [update] so composition stays cheap. */
    private val rows = mutableStateOf(emptyList<Row>())

    private data class Row(val name: String, val duration: String, val iconPath: String?)

    init {
        padLeft = 6f
        padTop = 5f
        padRight = 6f
        padBottom = 5f
        showBackground = false
        bgRadius = GlassSurface.RADIUS
    }

    /** The blur samples the live scene, so a cached redraw would freeze the backdrop. */
    override val alwaysRedraw: Boolean
        get() = true

    @Composable
    override fun Content() {
        val entries = rows.value
        val font = HudText.font(this)
        val lineHeight = HudText.lineHeight(this)
        val rowHeight = maxOf(lineHeight, ICON_SIZE) + ROW_GAP

        // An empty panel would just be a floating blurred box, so collapse to nothing.
        if (entries.isEmpty()) {
            PolyCanvas(modifier = PolyModifier.size(0f, 0f)) { _, _, _, _ -> }
            return
        }

        val widest = entries.maxOf { row ->
            ICON_SIZE + ICON_GAP + HudText.width(this@PotionEffectsComposeHud, row.name) +
                NAME_GAP + HudText.width(this@PotionEffectsComposeHud, row.duration)
        }
        val panelWidth = widest + padLeft + padRight
        val panelHeight = entries.size * rowHeight - ROW_GAP + padTop + padBottom

        PolyCanvas(modifier = PolyModifier.size(panelWidth, panelHeight)) { x, y, w, h ->
            glassPanel(x, y, w, h)

            entries.forEachIndexed { index, row ->
                val rowY = y + padTop + index * rowHeight
                val baseline = rowY + (maxOf(lineHeight, ICON_SIZE) - lineHeight) / 2f -
                    font.metrics.ascent

                row.iconPath?.let { path ->
                    icon(path)?.let { image ->
                        val iconY = rowY + (maxOf(lineHeight, ICON_SIZE) - ICON_SIZE) / 2f
                        image(image, x + padLeft, iconY, ICON_SIZE, ICON_SIZE, paint)
                    }
                }

                val hud = this@PotionEffectsComposeHud
                val textX = x + padLeft + ICON_SIZE + ICON_GAP
                // Drawn into a box of its own width: the row's own layout positions the
                // name and duration, so the HUD's alignment must not shift them again.
                hudText(hud, row.name, textX, baseline, HudText.width(hud, row.name))

                // Duration right-aligned against the panel's inner edge.
                val durationWidth = HudText.width(hud, row.duration)
                hudText(
                    hud,
                    row.duration,
                    x + w - padRight - durationWidth,
                    baseline,
                    durationWidth,
                    PolyColor(SECONDARY_TEXT),
                )
            }
        }
    }

    override fun update(): Boolean {
        val player = MinecraftClient.getInstance().player
        if (player == null) {
            rows.value = emptyList()
            return true
        }
        rows.value = player.statusEffects
            .sortedByDescending(StatusEffectInstance::getDuration)
            .map { effect ->
                Row(
                    name = effect.effectType.value().name.string + amplifierSuffix(effect),
                    duration = formatDuration(effect),
                    iconPath = iconPath(effect),
                )
            }
        return true
    }

    private fun amplifierSuffix(effect: StatusEffectInstance): String =
        if (effect.amplifier > 0) " ${effect.amplifier + 1}" else ""

    private fun formatDuration(effect: StatusEffectInstance): String {
        if (effect.isInfinite) return "**"
        val seconds = effect.duration / TICKS_PER_SECOND
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }

    /** Vanilla sprite path for an effect, or null when it has no registry id. */
    private fun iconPath(effect: StatusEffectInstance): String? {
        val id = Registries.STATUS_EFFECT.getId(effect.effectType.value()) ?: return null
        return "assets/${id.namespace}/textures/mob_effect/${id.path}.png"
    }

    /** Cached so the sprite is decoded once rather than every frame. */
    private fun icon(path: String): Image? {
        iconCache[path]?.let { return it }
        val image = runCatching { ImageLoader.fromResource(path, javaClass.classLoader) }.getOrNull()
        if (image != null) {
            iconCache[path] = image
        }
        return image
    }

    /** Twice a second is enough for a countdown shown to the second. */
    override fun updateFrequency(): Long = 500_000_000L

    override fun deletable(): Boolean = false

    override fun multipleInstancesAllowed(): Boolean = false

    companion object {
        private const val ICON_SIZE = 12f
        private const val ICON_GAP = 5f
        private const val NAME_GAP = 10f
        private const val ROW_GAP = 3f
        private const val TICKS_PER_SECOND = 20
        private const val SECONDARY_TEXT = 0xFFB9BDCC.toInt()

        private val iconCache = HashMap<String, Image>()

        @Volatile
        private var active = false

        /** Lets the vanilla-drawn module stand down instead of double-drawing. */
        @JvmStatic
        fun isActive(): Boolean = active

        @JvmStatic
        fun register(): Boolean {
            return try {
                HudManager.register(PotionEffectsComposeHud(), laoqi123.oneconfig.MyauOneConfig.CONFIG_ID)
                active = true
                true
            } catch (t: Throwable) {
                com.mojang.logging.LogUtils.getLogger()
                    .error("[Myau] Failed to register the Compose potion list", t)
                false
            }
        }
    }
}
