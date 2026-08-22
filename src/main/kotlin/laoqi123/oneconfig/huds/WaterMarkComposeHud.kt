package laoqi123.oneconfig.huds

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import laoqi123.oneconfig.huds.GlassSurface.glassPanel
import laoqi123.oneconfig.huds.HudText.hudText
import net.minecraft.client.MinecraftClient
import org.polyfrost.compose.composables.PolyCanvas
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.size
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager

/**
 * Myau's watermark, rendered through OneConfig's Compose HUD scene.
 *
 * <p>Running inside OneConfig's Skia pipeline is what buys the real frosted background
 * (see [GlassSurface]) and hands placement and scale to the HUD Designer. Text follows the
 * HUD's own font, size, colour, alignment and shadow settings through [HudText].
 */
class WaterMarkComposeHud private constructor() : Hud(
    "myau-watermark-compose",
    "WaterMark",
    Category.INFO,
) {

    private val line = mutableStateOf("Myau NextGen")

    init {
        padLeft = 6f
        padTop = 4f
        padRight = 6f
        padBottom = 4f
        // The blur is the background, so the theme's flat fill would only mute it.
        showBackground = false
        bgRadius = GlassSurface.RADIUS
    }

    /**
     * The blur samples the live scene, so a cached redraw would freeze the backdrop on
     * whichever frame it was captured.
     */
    override val alwaysRedraw: Boolean
        get() = true

    @Composable
    override fun Content() {
        val text = line.value
        val textWidth = HudText.width(this, text)
        val lineHeight = HudText.lineHeight(this)

        val panelWidth = textWidth + padLeft + padRight
        val panelHeight = lineHeight + padTop + padBottom

        // A single canvas keeps the backdrop and the text in one coordinate space and
        // guarantees the draw order.
        PolyCanvas(modifier = PolyModifier.size(panelWidth, panelHeight)) { x, y, w, _ ->
            glassPanel(x, y, w, panelHeight)
            val baseline = y + padTop - HudText.font(this@WaterMarkComposeHud).metrics.ascent
            hudText(
                this@WaterMarkComposeHud,
                text,
                x + padLeft,
                baseline,
                w - padLeft - padRight,
            )
        }
    }

    override fun update(): Boolean {
        // One shared blur per frame: the first HUD to update marks it stale.
        BlurCache.invalidate()
        val mc = MinecraftClient.getInstance()
        // Captured into locals: both are mutable fields, so Kotlin cannot smart-cast them.
        val player = mc.player
        val network = mc.networkHandler
        val ping = if (player != null && network != null) {
            network.getPlayerListEntry(player.uuid)?.latency ?: 0
        } else {
            0
        }
        line.value = "Myau NextGen  ${mc.currentFps} FPS  $ping ms"
        return true
    }

    /** Four refreshes a second; nanoseconds, matching TextHud's convention. */
    override fun updateFrequency(): Long = 250_000_000L

    /** The watermark is intrinsic to the client, same policy as the vanilla one. */
    override fun deletable(): Boolean = false

    override fun multipleInstancesAllowed(): Boolean = false

    companion object {
        @Volatile
        private var active = false

        /**
         * Whether the Compose HUD took over drawing, so the vanilla-drawn module knows to
         * stand down instead of double-drawing the watermark.
         */
        @JvmStatic
        fun isActive(): Boolean = active

        /**
         * Registers the provider only.
         *
         * <p>Instantiation is HudManager's job: it calls `make` and `setup` itself from
         * `loadFromActiveProfile`, restoring the saved position in the process. Doing it
         * here as well made the manager throw "HUD is already made" and drop the HUD.
         */
        @JvmStatic
        fun register(): Boolean {
            return try {
                HudManager.register(WaterMarkComposeHud(), laoqi123.oneconfig.MyauOneConfig.CONFIG_ID)
                active = true
                true
            } catch (t: Throwable) {
                com.mojang.logging.LogUtils.getLogger()
                    .error("[Myau] Failed to register the Compose watermark", t)
                false
            }
        }
    }
}
