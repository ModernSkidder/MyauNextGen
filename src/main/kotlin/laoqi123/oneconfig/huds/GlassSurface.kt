package laoqi123.oneconfig.huds

import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Rect
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.RenderContext

/**
 * The frosted-glass surface shared by Myau's Compose HUDs.
 *
 * <p>Each panel blurs the game behind it with the blur cache, the same path
 * OneConfig's own window backdrop uses, then runs a small SkSL shader that refracts the
 * result near the rim. The refraction is the point: real glass bends light passing through
 * its curved edge, so the backdrop is magnified and compressed against the border. No
 * specular or bevel term is applied, because that is what makes a panel read as an
 * embossed button rather than as glass.
 *
 * <h2>Why the drawing happens in device space</h2>
 * `drawRegion` is written for an untransformed canvas: internally it clips to
 * `Rect(0, 0, width, height)` and does `translate(-x, -y)`, so it assumes the canvas origin
 * already sits on the panel and that its arguments are framebuffer pixels. OneConfig's only
 * other caller confirms this, passing the render target's pixel size with the raw
 * `SkiaCtx.canvas`.
 *
 * <p>HUD content is drawn through a canvas carrying `scale(guiScale)`, which silently
 * scales that internal translate. Reconstructing the right numbers by hand is what kept the
 * blur misaligned by an amount that grew with GUI scale, so the matrix is reset to identity
 * for the duration of the blur and every value converted to device pixels first.
 *
 * <h2>Cost</h2>
 * The runtime shader and its filter are native Skia objects. Rebuilding them per panel per
 * frame — which `alwaysRedraw` makes ~60 times a second — both allocated and leaked them,
 * and was heavy enough to cost frames. They are therefore cached per panel geometry and the
 * previous instance is closed when the geometry changes.
 */
object GlassSurface {

    /** Matches OneConfig's own HUD corner radius (`Hud.bgRadius`). */
    const val RADIUS = 4f

    /** Matches the radius OneConfig's window backdrop blurs with. */
    const val BLUR_RADIUS = 8f

    /** Solid stand-in used when Skia blur is unavailable, so a HUD never vanishes. */
    const val FALLBACK_FILL: Int = 0xC0101418.toInt()

    // ---------------------------------------------------------------- glass tuning

    /** Body tint strength, 0..1. Higher reads as more opaque frosted glass. */
    var tintStrength = 0.34f

    /** Cool body colour the blurred backdrop is tinted towards. */
    var tintColor = 0x121A24

    /**
     * Width of the refracting rim, in panel pixels. This is the band where the edge bends
     * light; the interior stays flat.
     */
    var rimWidth = 6f

    /**
     * How far the rim displaces its sample, in panel pixels. Kept modest: a large offset
     * near a corner drags in pixels from well inside the panel, which reads as a smear
     * rather than as glass.
     */
    var refractStrength = 3f

    /** Width of the alpha falloff at the very edge, in panel pixels. */
    var featherWidth = 1.5f

    /** Drop shadow blur radius, in panel pixels. Zero disables the shadow. */
    var shadowRadius = 5f

    /** How far the shadow is offset down from the panel, in panel pixels. */
    var shadowOffsetY = 2f

    /** Shadow colour and opacity. */
    var shadowColor: Int = 0x66000000

    @Volatile
    private var blurFailed = false

    @Volatile
    private var shaderFailed = false

    private val paint = Paint()
    private val shadowPaint = Paint()

    /** Compiled once and reused for every panel. */
    private val effect: RuntimeEffect? by lazy {
        runCatching { RuntimeEffect.makeForShader(SKSL) }
            .onFailure {
                shaderFailed = true
                com.mojang.logging.LogUtils.getLogger()
                    .warn("[Myau] Glass shader failed to compile; panels will render plain", it)
            }
            .getOrNull()
    }

    /**
     * Cached glass filter. Panels keep a stable size for many frames, so keying on the
     * geometry means the filter is normally built once and then reused.
     */
    private class CachedFilter(
        val filter: ImageFilter,
        val builder: RuntimeShaderBuilder,
    )

    /**
     * Keyed by geometry so panels of different sizes do not evict each other. The ArrayList
     * alone draws one panel per row, and a single-entry cache would rebuild the shader for
     * every one of them on every frame.
     */
    private val cache = HashMap<Long, CachedFilter>()

    /** Guards against unbounded growth if a panel animates its size. */
    private const val MAX_CACHED = 64

    /**
     * Draws a frosted rounded panel at [x], [y] with [width] x [height] in the receiver's
     * coordinate space: drop shadow, blurred backdrop, edge refraction and a feathered rim.
     *
     * <p>Degrades to a flat translucent panel if the blur or shader is unavailable,
     * remembering the failure so a broken driver cannot spam the log at framerate.
     */
    fun RenderContext.glassPanel(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float = RADIUS,
    ) {
        if (width <= 0f || height <= 0f) return

        if (blurFailed) {
            rect(x, y, width, height, PolyColor(FALLBACK_FILL), radius)
            return
        }

        // The canvas transform is the authoritative source: it already folds in every scale
        // and translate applied above this node, however many there are.
        val m = canvas.localToDeviceAsMatrix33.mat
        val scaleX = m[0]
        val scaleY = m[4]
        val deviceX = m[0] * x + m[1] * y + m[2]
        val deviceY = m[3] * x + m[4] * y + m[5]
        // Rounded to whole pixels so the shader cache key is stable: text measurement makes
        // panel widths jitter by fractions, which would otherwise miss the cache every frame
        // and rebuild the filter.
        val deviceW = kotlin.math.round(width * scaleX)
        val deviceH = kotlin.math.round(height * scaleY)
        val deviceR = radius * scaleX

        if (deviceW <= 0f || deviceH <= 0f) return

        // Everything inside is in framebuffer pixels, which is the space drawRegion is
        // written for. The canvas is used directly so RenderContext's own absolute-position
        // bookkeeping stays untouched, and the save count is captured so a failure part way
        // through cannot leak the reset matrix into every later HUD.
        val depth = canvas.save()
        try {
            canvas.resetMatrix()
            canvas.translate(deviceX, deviceY)

            drawShadow(canvas, deviceW, deviceH, deviceR, scaleX)

            // One shared blur per frame rather than one per panel: the old path blurred the
            // whole screen on every call, so cost scaled with panel count and the frame rate
            // fell apart as soon as anything moved.
            val blur = BlurCache.acquire(BLUR_RADIUS)
            val filter = if (blur == null) null else glassFilter(deviceW, deviceH, deviceR, scaleX)

            if (blur == null) {
                // No blur available: a flat panel still keeps the HUD readable.
                paint.reset()
                paint.color = FALLBACK_FILL
                canvas.drawRRect(RRect.makeXYWH(0f, 0f, deviceW, deviceH, deviceR), paint)
            } else if (filter != null) {
                paint.reset()
                paint.imageFilter = filter
                canvas.saveLayer(Rect.makeXYWH(0f, 0f, deviceW, deviceH), paint)
                paint.imageFilter = null
                // Square clip on purpose: the shader derives the rounded shape from its own
                // distance field, and rounding the layer first would leave the corners empty
                // for it to sample, which shows up as a dark border.
                BlurCache.draw(canvas, blur, deviceX, deviceY, deviceW, deviceH)
            } else {
                canvas.clipRRect(RRect.makeXYWH(0f, 0f, deviceW, deviceH, deviceR), true)
                BlurCache.draw(canvas, blur, deviceX, deviceY, deviceW, deviceH)
                paint.reset()
                paint.color = tintArgb()
                canvas.drawRRect(RRect.makeXYWH(0f, 0f, deviceW, deviceH, deviceR), paint)
            }
        } catch (t: Throwable) {
            blurFailed = true
            com.mojang.logging.LogUtils.getLogger()
                .warn("[Myau] Skia blur unavailable; glass panels will render flat", t)
        } finally {
            // Unwinds both the layer (when one was opened) and the matrix save.
            canvas.restoreToCount(depth)
        }
    }

    /**
     * A soft drop shadow under the panel, which is what lifts it off the world behind it.
     *
     * <p>Drawn with a blurred mask filter rather than a stack of translucent rectangles so
     * the falloff is smooth at any size.
     */
    private fun drawShadow(
        canvas: org.jetbrains.skia.Canvas,
        width: Float,
        height: Float,
        radius: Float,
        scale: Float,
    ) {
        val blur = shadowRadius * scale
        if (blur <= 0f || (shadowColor ushr 24) == 0) return

        val mask = org.jetbrains.skia.MaskFilter.makeBlur(
            org.jetbrains.skia.FilterBlurMode.NORMAL,
            // Skia's sigma is roughly a third of a visual blur radius.
            blur / 3f,
        )
        try {
            shadowPaint.reset()
            shadowPaint.color = shadowColor
            shadowPaint.maskFilter = mask
            canvas.drawRRect(
                RRect.makeXYWH(0f, shadowOffsetY * scale, width, height, radius),
                shadowPaint,
            )
        } finally {
            // The mask filter is a native object, so it is released rather than left to a
            // finaliser; the paint must drop its reference first.
            shadowPaint.maskFilter = null
            mask.close()
        }
    }

    /**
     * Returns the cached glass filter for this geometry, rebuilding it only when the panel's
     * size, radius, scale or tuning changed. Null when the shader is unavailable so the
     * caller can fall back to a plain tint.
     */
    private fun glassFilter(
        width: Float,
        height: Float,
        radius: Float,
        scale: Float,
    ): ImageFilter? {
        if (shaderFailed) return null
        val runtime = effect ?: return null

        val key = geometryKey(width, height, radius, scale)
        cache[key]?.let { return it.filter }

        return try {
            val builder = RuntimeShaderBuilder(runtime)
            builder.uniform("uSize", width, height)
            builder.uniform("uRadius", radius)
            builder.uniform("uRim", rimWidth * scale)
            builder.uniform("uRefract", refractStrength * scale)
            builder.uniform("uFeather", (featherWidth * scale).coerceAtLeast(0.5f))
            builder.uniform("uTint", tintStrength)
            builder.uniform(
                "uTintColor",
                ((tintColor shr 16) and 0xFF) / 255f,
                ((tintColor shr 8) and 0xFF) / 255f,
                (tintColor and 0xFF) / 255f,
            )
            // "uSource" is fed by the layer's own contents, which is the blurred backdrop.
            val filter = ImageFilter.makeRuntimeShader(builder, "uSource", null)

            // These are native objects, so anything evicted is closed rather than leaked.
            if (cache.size >= MAX_CACHED) {
                cache.values.forEach {
                    it.filter.close()
                    it.builder.close()
                }
                cache.clear()
            }
            cache[key] = CachedFilter(filter, builder)
            filter
        } catch (t: Throwable) {
            shaderFailed = true
            com.mojang.logging.LogUtils.getLogger()
                .warn("[Myau] Glass shader unavailable; panels will render plain", t)
            null
        }
    }

    /**
     * Cache key covering the panel geometry and every tunable baked into the shader, so
     * changing a value at runtime rebuilds rather than reusing a stale filter.
     *
     * <p>Sizes are quantised to whole pixels: a panel whose width jitters by a fraction
     * (text measurement does this) would otherwise miss the cache on every frame.
     */
    private fun geometryKey(width: Float, height: Float, radius: Float, scale: Float): Long {
        var key = width.toInt().toLong()
        key = key * 8191 + height.toInt()
        key = key * 8191 + (radius * 4f).toInt()
        key = key * 8191 + (scale * 16f).toInt()
        key = key * 8191 + tuningKey()
        return key
    }

    /** Cheap fingerprint of the tunables, so edits at runtime invalidate the cache. */
    private fun tuningKey(): Int {
        var key = rimWidth.toRawBits()
        key = 31 * key + refractStrength.toRawBits()
        key = 31 * key + featherWidth.toRawBits()
        key = 31 * key + tintStrength.toRawBits()
        key = 31 * key + tintColor
        return key
    }

    private fun tintArgb(): Int =
        ((tintStrength.coerceIn(0f, 1f) * 255f).toInt() shl 24) or (tintColor and 0xFFFFFF)

    /**
     * Edge refraction, in panel-local device pixels.
     *
     * <p>A rounded-rectangle signed distance field gives both the shape and its surface
     * normal, so the effect follows the real corner radius. Within [uRim] of the border the
     * sample coordinate is pulled inward along that normal, magnifying the backdrop and
     * compressing it against the edge the way a bevelled piece of glass does. Sampling
     * outward instead would read the layer's empty pixels and show up as a dark border.
     *
     * <p>The alpha falls off over [uFeather] pixels at the rim, which keeps the corners
     * smooth instead of stair-stepped.
     */
    private val SKSL = """
        uniform shader uSource;
        uniform float2 uSize;
        uniform float  uRadius;
        uniform float  uRim;
        uniform float  uRefract;
        uniform float  uFeather;
        uniform float  uTint;
        uniform float3 uTintColor;

        // Signed distance to a rounded rectangle; negative inside.
        float sdRoundRect(float2 p, float2 hs, float r) {
            float2 q = abs(p) - hs + r;
            return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
        }

        half4 main(float2 coord) {
            // Named hs rather than half: half is a type keyword in SkSL.
            float2 hs = uSize * 0.5;
            float2 p = coord - hs;
            float d = sdRoundRect(p, hs, uRadius);

            // Depth into the panel, zero exactly on the border.
            float inside = -d;

            // The distance field's gradient is the outward surface normal. Found by
            // differencing because the analytic derivative is not available here.
            float e = 1.0;
            float dx = sdRoundRect(p + float2(e, 0.0), hs, uRadius)
                     - sdRoundRect(p - float2(e, 0.0), hs, uRadius);
            float dy = sdRoundRect(p + float2(0.0, e), hs, uRadius)
                     - sdRoundRect(p - float2(0.0, e), hs, uRadius);
            float2 n = normalize(float2(dx, dy) + float2(1e-6));

            // 1 on the border, 0 once we are uRim deep, with a smooth ramp between so the
            // lensing eases in instead of showing a seam.
            float t = clamp(1.0 - inside / max(uRim, 0.001), 0.0, 1.0);
            float bend = t * t * (3.0 - 2.0 * t);

            // Inward displacement magnifies the backdrop towards the rim.
            float2 sampleAt = coord - n * (bend * uRefract);
            sampleAt = clamp(sampleAt, float2(0.5), uSize - float2(0.5));

            half4 src = uSource.eval(sampleAt);
            half3 col = mix(src.rgb, half3(uTintColor), half(uTint));

            // Feathered rim: smooth alpha ramp over the outermost uFeather pixels.
            float alpha = smoothstep(0.0, uFeather, inside);
            return half4(col * alpha, alpha);
        }
    """
}
