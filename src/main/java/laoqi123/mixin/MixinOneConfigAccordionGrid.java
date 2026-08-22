package laoqi123.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Forces OneConfig's expanded module list to show one setting per row.
 *
 * <p>{@code AccordionOptionsGrid} packs two settings into a row unless a setting is
 * considered "wide", which it decides by comparing the measured title width plus
 * {@code optionReservedWidth(prop, optionWidth)} against half the available width:
 *
 * <pre>titleWidth + optionReservedWidth(prop, optionWidth) &gt; columnWidth</pre>
 *
 * <p>No metadata flag opts out of the packing, so the reserved width is inflated at
 * that comparison instead, which makes it always true and gives every setting a full
 * row.
 *
 * <p>Only the grid's call site is wrapped. The same function is also called from
 * {@code SettingContent} to decide {@code stacked}, which moves a row's label above its
 * control; inflating that one too would push every slider's name onto its own line.
 *
 * <p>{@link Pseudo} is required because the target is another mod's Kotlin file class
 * rather than a Minecraft class. The method name carries Kotlin's inline-class mangling
 * suffix, and the descriptor is {@code (Property, float)float} because {@code Dp} is a
 * value class erased to a float.
 */
@Pseudo
@Mixin(targets = "org.polyfrost.oneconfig.internal.ui.screens.ConfigScreenKt", remap = false)
public abstract class MixinOneConfigAccordionGrid {

    /**
     * Large enough to dominate any real column width, while staying far from
     * {@code Float.MAX_VALUE} so later arithmetic on it cannot overflow.
     */
    private static final float MYAU_FORCE_FULL_ROW_DP = 100000.0F;

    /**
     * {@code require = 0} keeps a OneConfig update that renames or reshapes these
     * private members from becoming a startup crash; the layout would simply fall back
     * to two columns.
     */
    @WrapOperation(
            // Slash-delimited selectors use Mixin's regex matcher, which matches
            // Kotlin's mangled names verbatim instead of rejecting the '-'.
            method = "/^AccordionOptionsGrid\\$lambda\\$4$/",
            at = @At(
                    value = "INVOKE",
                    target = "/^optionReservedWidth-3ABfNKs$/"
            ),
            remap = false,
            require = 0
    )
    private static float myauForceSingleColumn(Property<?> prop, float optionWidth,
                                               Operation<Float> original) {
        return MYAU_FORCE_FULL_ROW_DP;
    }
}
