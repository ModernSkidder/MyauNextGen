package laoqi123.oneconfig.huds

import org.jetbrains.skia.Font
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.oneconfig.api.hud.v1.Hud

/**
 * Applies a [Hud]'s own text settings when drawing to a Skia canvas.
 *
 * <p>Panels that draw through [org.polyfrost.compose.composables.PolyCanvas] bypass
 * OneConfig's text composables, so the per-HUD options users expect (font family, weight,
 * italic, size, colour, chroma, shadow, underline, casing and alignment) have to be
 * honoured explicitly. Everything here reads straight off the HUD so the values stay in
 * sync with whatever the HUD Designer writes.
 */
object HudText {

    /** Text alignment values used by `Hud.textAlign`. */
    private const val ALIGN_LEFT = 0
    private const val ALIGN_CENTER = 1
    private const val ALIGN_RIGHT = 2

    /** Casing values used by `Hud.caseType`. */
    private const val CASE_UPPER = 1
    private const val CASE_LOWER = 2

    /**
     * The Skia font for this HUD's current settings.
     *
     * <p>Poppins variants resolve through the HUD's own `getPoppinsFontName`, so weight and
     * italic follow the user's choice. The Minecraft option maps onto the bitmap-style face
     * OneConfig registers under `minecraft`.
     */
    fun font(hud: Hud): Font {
        val name = when (hud.font) {
            org.polyfrost.oneconfig.api.hud.v1.Font.Minecraft ->
                if (hud.textBold) "minecraft-bold" else "minecraft"
            else -> hud.getPoppinsFontName()
        }
        return FontManager.getFont(size(hud), name)
    }

    /** Base size scaled by the HUD's text scale, matching TextHud's `8f * textScale`. */
    fun size(hud: Hud): Float = BASE_SIZE * hud.textScale

    /** Line height for laying rows out. */
    fun lineHeight(hud: Hud): Float {
        val metrics = font(hud).metrics
        return -metrics.ascent + metrics.descent
    }

    /** Applies the HUD's casing option. */
    fun transform(hud: Hud, text: String): String = when (hud.caseType) {
        CASE_UPPER -> text.uppercase()
        CASE_LOWER -> text.lowercase()
        else -> text
    }

    /** Width of [text] once cased, in panel pixels. */
    fun width(hud: Hud, text: String): Float =
        font(hud).measureText(transform(hud, text)).width

    /** The HUD's text colour, cycling when chroma is enabled. */
    fun color(hud: Hud): PolyColor =
        PolyColor(hud.textColor, hud.textChroma, hud.textChromaSpeed)

    /**
     * Draws [text] inside a box of [boxWidth] at the HUD's alignment, honouring casing,
     * colour, chroma, shadow and underline.
     *
     * @param x left edge of the box the text is aligned within
     * @param baseline text baseline, so callers control vertical placement
     * @param color overrides the HUD's colour when a caller needs a per-row colour
     */
    fun RenderContext.hudText(
        hud: Hud,
        text: String,
        x: Float,
        baseline: Float,
        boxWidth: Float,
        color: PolyColor = color(hud),
    ) {
        if (text.isEmpty()) return
        val cased = transform(hud, text)
        val skiaFont = font(hud)
        val textWidth = skiaFont.measureText(cased).width

        val drawX = when (hud.textAlign) {
            ALIGN_CENTER -> x + (boxWidth - textWidth) / 2f
            ALIGN_RIGHT -> x + boxWidth - textWidth
            else -> x
        }

        if (hud.showShadow) {
            val shadow = PolyColor(hud.shadowColor, hud.shadowChroma, hud.shadowChromaSpeed)
            text(
                cased,
                drawX + hud.shadowOffsetX,
                baseline + hud.shadowOffsetY,
                shadow,
                skiaFont,
            )
        }

        text(cased, drawX, baseline, color, skiaFont)

        if (hud.textUnderline) {
            val metrics = skiaFont.metrics
            val position = metrics.underlinePosition ?: (size(hud) * 0.08f)
            val thickness = metrics.underlineThickness ?: (size(hud) * 0.06f)
            line(
                drawX,
                baseline + position,
                drawX + textWidth,
                baseline + position,
                color,
                thickness,
            )
        }
    }

    /** Matches the base size OneConfig's TextHud renders Poppins at. */
    private const val BASE_SIZE = 8f
}
