package laoqi123.oneconfig.huds

import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx

/**
 * One shared, cached blur of the game behind the HUDs.
 *
 * <h2>Why this exists</h2>
 * OneConfig's `BlurRenderer.drawRegion` downsamples and blurs the *entire* render target on
 * every call, then clips the result to the requested rectangle. That is fine for the one
 * full-screen backdrop it was written for, but a HUD layer draws many panels a frame — the
 * module list alone draws one per row — so the cost multiplied by the panel count and the
 * frame rate collapsed as soon as anything moved.
 *
 * <p>Here the screen is blurred at most once per frame into a surface we own, and every
 * panel samples that one image. The blur is also skipped entirely while nothing on screen
 * has changed, which is the common case when standing still.
 *
 * <p>Everything is in framebuffer pixels. The blur runs at a quarter resolution, which is
 * both much cheaper and closer to how a real frosted surface loses detail.
 */
object BlurCache {

    /** Downsample factor before blurring. Matches OneConfig's own choice. */
    private const val DOWNSAMPLE = 4

    /** Sigma is roughly a third of a visual blur radius. */
    private const val SIGMA_DIVISOR = 3f

    private val copyPaint = Paint().apply { blendMode = org.jetbrains.skia.BlendMode.SRC }

    /**
     * Forces the blurred copy fully opaque.
     *
     * <p>The game's framebuffer carries an alpha of zero, and [org.jetbrains.skia.BlendMode.SRC]
     * copies that alpha verbatim, so the blur came out transparent and the panels rendered as
     * flat black once the tint went over the top. Adding solid black with `PLUS` leaves the RGB
     * untouched while lifting alpha to one. OneConfig's own blur does the same thing for the
     * same reason.
     */
    private val opaquePaint = Paint().apply {
        color = 0xFF000000.toInt()
        blendMode = org.jetbrains.skia.BlendMode.PLUS
    }
    private val blurPaint = Paint()

    // Wrapper around the game's framebuffer, rebuilt only when the FBO or size changes.
    private var sourceSurface: Surface? = null
    private var sourceTarget: BackendRenderTarget? = null
    private var sourceFbo = -1
    private var sourceWidth = -1
    private var sourceHeight = -1

    // Our own low-resolution surfaces: one to receive the downsample, one to hold the blur.
    private var scratch: Surface? = null
    private var blurred: Surface? = null
    private var lowWidth = -1
    private var lowHeight = -1

    /** The blurred image every panel samples this frame. */
    private var image: Image? = null
    private var lastRadius = -1f

    @Volatile
    private var failed = false

    /** When the cached blur was built. */
    private var lastBuildNanos = 0L

    /**
     * Window within which callers are treated as belonging to the same frame.
     *
     * <p>The module list draws one panel per row, so without grouping, a single frame would run
     * a full-screen downsample and Gaussian once per panel — which is what made moving tank the
     * frame rate. Panels in one frame are drawn microseconds apart, while consecutive frames are
     * at least four milliseconds apart even at 250 fps, so this groups a frame's panels together
     * without ever holding a blur across frames.
     *
     * <p>Deliberately not a rate limit: the blur refreshes every frame.
     */
    private const val FRAME_GROUPING_NANOS = 2_000_000L

    /** Forces the next [acquire] to rebuild. [acquire] self-gates, so this is rarely needed. */
    fun invalidate() {
        lastBuildNanos = 0L
    }

    /**
     * The blurred screen, or null when unavailable.
     *
     * <p>Rebuilt once per frame, then reused verbatim by every later caller in that frame no
     * matter how many panels ask for it.
     */
    fun acquire(radius: Float): Image? {
        if (failed) return null

        val now = System.nanoTime()
        val cached = image
        if (cached != null && lastRadius == radius &&
            now - lastBuildNanos < FRAME_GROUPING_NANOS
        ) {
            return cached
        }

        return try {
            val built = rebuild(radius)
            if (built != null) {
                lastRadius = radius
                // Recorded only on success, so a transient failure retries next frame rather
                // than leaving the panels on a stale backdrop.
                lastBuildNanos = System.nanoTime()
            }
            built
        } catch (t: Throwable) {
            failed = true
            com.mojang.logging.LogUtils.getLogger()
                .warn("[Myau] Background blur unavailable; glass panels will render flat", t)
            null
        }
    }

    private fun rebuild(radius: Float): Image? {
        val client = net.minecraft.client.MinecraftClient.getInstance()
        val target = client.framebuffer ?: return null
        val width = target.textureWidth
        val height = target.textureHeight
        if (width <= 0 || height <= 0) return null

        val source = resolveSource(target, width, height) ?: return null
        // Tells Skia the framebuffer's contents changed underneath it without discarding
        // the wrapper, which is what makes reusing the surface across frames valid.
        source.notifyContentWillChange(org.jetbrains.skia.ContentChangeMode.RETAIN)

        val lw = (width + DOWNSAMPLE - 1) / DOWNSAMPLE
        val lh = (height + DOWNSAMPLE - 1) / DOWNSAMPLE
        if (!ensureLowRes(lw, lh)) return null

        val scratchSurface = scratch ?: return null
        val blurSurface = blurred ?: return null
        if (lowWidth <= 0 || lowHeight <= 0) return null

        // Downsample the framebuffer into the scratch surface. A scaled canvas plus
        // Surface.draw only samples one pixel per destination pixel, so a 4x reduction throws
        // away 15 of every 16 and aliases before the blur can hide it. Snapshotting and
        // blitting rect-to-rect lets Skia apply a real filter kernel instead.
        val sourceImage = source.makeImageSnapshot()
        try {
            val scratchCanvas = scratchSurface.canvas
            scratchCanvas.drawImageRect(
                sourceImage,
                Rect.makeXYWH(0f, 0f, width.toFloat(), height.toFloat()),
                Rect.makeXYWH(0f, 0f, lw.toFloat(), lh.toFloat()),
                SamplingMode.MITCHELL,
                copyPaint,
                true,
            )
            // Restore alpha before blurring, or the Gaussian spreads zeroes and the result is
            // an empty image that reads as solid black behind the tint.
            scratchCanvas.drawRect(Rect.makeXYWH(0f, 0f, lw.toFloat(), lh.toFloat()), opaquePaint)
        } finally {
            sourceImage.close()
        }

        // Blur it once into the second surface.
        val sigma = (radius / DOWNSAMPLE) / SIGMA_DIVISOR
        val filter = org.jetbrains.skia.ImageFilter.makeBlur(
            sigma,
            sigma,
            FilterTileMode.CLAMP,
            null,
            null,
        )
        try {
            blurPaint.reset()
            blurPaint.imageFilter = filter
            blurPaint.blendMode = org.jetbrains.skia.BlendMode.SRC
            scratchSurface.draw(blurSurface.canvas, 0, 0, blurPaint)
        } finally {
            blurPaint.imageFilter = null
            filter.close()
        }

        // Replace last frame's snapshot; it is a native object, so release it explicitly.
        image?.close()
        image = blurSurface.makeImageSnapshot()
        return image
    }

    /** Wraps the game's framebuffer as a Skia surface, reusing the wrapper when possible. */
    private fun resolveSource(
        target: net.minecraft.client.gl.Framebuffer,
        width: Int,
        height: Int,
    ): Surface? {
        // Read straight off the framebuffer: OneConfig's RenderTargetFbo helper is compiled
        // against intermediary names and does not resolve under Yarn mappings.
        val fbo = target.fbo
        if (fbo <= 0) return null

        if (sourceSurface != null && sourceFbo == fbo &&
            sourceWidth == width && sourceHeight == height
        ) {
            return sourceSurface
        }

        sourceSurface?.close()
        sourceTarget?.close()
        sourceSurface = null
        sourceTarget = null

        val brt = BackendRenderTarget.makeGL(width, height, 0, 8, fbo, FramebufferFormat.GR_GL_RGBA8)
        val surface = Surface.makeFromBackendRenderTarget(
            SkiaCtx.directContext,
            brt,
            // The game's framebuffer is bottom-up, unlike the HUD surface we draw into.
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.sRGB,
            null,
        )
        if (surface == null) {
            brt.close()
            return null
        }

        sourceTarget = brt
        sourceSurface = surface
        sourceFbo = fbo
        sourceWidth = width
        sourceHeight = height
        return surface
    }

    /** Allocates the pair of low-resolution surfaces, reusing them across frames. */
    private fun ensureLowRes(width: Int, height: Int): Boolean {
        if (scratch != null && blurred != null && lowWidth == width && lowHeight == height) {
            return true
        }

        scratch?.close()
        blurred?.close()
        scratch = null
        blurred = null

        val info = ImageInfo.makeN32Premul(width, height)
        scratch = Surface.makeRenderTarget(SkiaCtx.directContext, false, info)
        blurred = Surface.makeRenderTarget(SkiaCtx.directContext, false, info)
        lowWidth = width
        lowHeight = height
        return true
    }

    /**
     * Draws the cached blur into [canvas] so that framebuffer point ([deviceX], [deviceY])
     * lands at the canvas origin.
     *
     * <p>The caller is expected to have already clipped to the panel and moved the origin
     * onto it, matching how the rest of the glass drawing works.
     */
    fun draw(
        canvas: org.jetbrains.skia.Canvas,
        blur: Image,
        deviceX: Float,
        deviceY: Float,
        width: Float,
        height: Float,
    ) {
        val depth = canvas.save()
        try {
            canvas.clipRect(Rect.makeXYWH(0f, 0f, width, height))
            // Blit the quarter-resolution blur straight to full size with an explicit cubic
            // filter. Scaling the canvas and calling drawImage instead left Skia on its
            // default sampling, which is nearest-neighbour: at a 4x upsample that produced
            // visible blocky stair-stepping across the whole panel.
            canvas.drawImageRect(
                blur,
                Rect.makeXYWH(0f, 0f, lowWidth.toFloat(), lowHeight.toFloat()),
                Rect.makeXYWH(
                    -deviceX,
                    -deviceY,
                    sourceWidth.toFloat(),
                    sourceHeight.toFloat(),
                ),
                SamplingMode.MITCHELL,
                // Plain source-over, not SRC: this draws into the panel's layer, and SRC
                // would replace the layer's pixels including alpha rather than filling it.
                null,
                true,
            )
        } finally {
            canvas.restoreToCount(depth)
        }
    }
}
