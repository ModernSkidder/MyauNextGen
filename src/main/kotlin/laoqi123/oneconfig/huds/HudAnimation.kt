package laoqi123.oneconfig.huds

import kotlin.math.exp
import kotlin.math.pow

/**
 * Easing and frame-rate independent smoothing for the Compose HUDs.
 *
 * <p>HUD content is redrawn every frame, so animation is driven by wall-clock deltas rather
 * than by a tick count. That keeps motion identical at 30 and 240 fps, which a per-frame
 * lerp would not.
 */
object HudAnimation {

    /** Decelerating ramp. The default for anything moving into place. */
    fun easeOutCubic(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        return 1f - (1f - c).pow(3)
    }

    /** Accelerating ramp, used on the way out so rows leave briskly. */
    fun easeInCubic(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        return c * c * c
    }

    /**
     * Decelerating ramp that overshoots slightly before settling, which is what makes an
     * entry feel elastic rather than mechanical.
     */
    fun easeOutBack(t: Float, overshoot: Float = 1.4f): Float {
        val c = t.coerceIn(0f, 1f) - 1f
        return 1f + (overshoot + 1f) * c.pow(3) + overshoot * c.pow(2)
    }

    /**
     * Moves [current] towards [target] by an exponential decay over [dt] seconds.
     *
     * <p>Unlike `current += (target - current) * factor`, the result depends only on elapsed
     * time, so the motion does not speed up on a faster machine. [speed] is the reciprocal
     * of the time constant: larger converges sooner.
     */
    fun approach(current: Float, target: Float, dt: Float, speed: Float): Float {
        if (dt <= 0f) return current
        val blend = 1f - exp(-speed * dt)
        val next = current + (target - current) * blend
        // Snap once the remaining distance stops being visible, so values settle exactly.
        return if (kotlin.math.abs(target - next) < 0.01f) target else next
    }

    /** Advances a 0..1 progress value towards [target] at [perSecond] units per second. */
    fun advance(current: Float, target: Float, dt: Float, perSecond: Float): Float {
        val step = perSecond * dt
        return if (current < target) {
            (current + step).coerceAtMost(target)
        } else {
            (current - step).coerceAtLeast(target)
        }
    }
}
