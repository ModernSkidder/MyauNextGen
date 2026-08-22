package laoqi123.oneconfig;

import laoqi123.property.properties.*;
import laoqi123.util.config.Choice;
import laoqi123.util.config.NamedChoice;
import org.polyfrost.oneconfig.api.config.v1.Properties;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.Visualizer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Adapts Myau's {@link laoqi123.property.Property} instances into OneConfig
 * {@link Property} nodes.
 *
 * <p>Nothing is copied: every generated property delegates its getter and setter
 * straight back to the original object via
 * {@link Properties#functional}. This keeps a single source of truth, so the
 * existing save/load path ({@code laoqi123.config.Config}), change listeners and
 * {@code verifyValue} hooks all keep working untouched.
 *
 * <p>The {@code visualizer} metadata key selects which control OneConfig renders;
 * {@code min}/{@code max} and {@code options} supply the control's parameters.
 */
public final class PropertyBridge {

    private PropertyBridge() {
    }

    /**
     * Convert a single Myau property. Returns {@code null} for property kinds that
     * have no sensible OneConfig control, so callers should skip those.
     *
     * <p>{@code idPrefix} namespaces the id (settings from all modules share one flat
     * tree, and plain names like "mode" would otherwise collide).
     */
    public static Property<?> convert(laoqi123.property.Property<?> source, String idPrefix) {
        String id = idPrefix + sanitizeId(source.getName());
        String title = prettify(source.getName());

        if (source instanceof BooleanProperty p) {
            return visual(bool(p, id, title), new Visualizer.SwitchVisualizer());
        }
        if (source instanceof PercentProperty p) {
            Property<Integer> prop = intProp(p, id, title);
            prop.addMetadata("min", p.getMinimum().floatValue());
            prop.addMetadata("max", p.getMaximum().floatValue());
            prop.addMetadata("step", 1.0F);
            prop.addMetadata("unit", "%");
            return visual(prop, new Visualizer.SliderVisualizer());
        }
        if (source instanceof IntProperty p) {
            Property<Integer> prop = intProp(p, id, title);
            prop.addMetadata("min", p.getMinimum().floatValue());
            prop.addMetadata("max", p.getMaximum().floatValue());
            prop.addMetadata("step", 1.0F);
            return visual(prop, new Visualizer.SliderVisualizer());
        }
        if (source instanceof FloatProperty p) {
            Property<Float> prop = floatProp(p, id, title);
            prop.addMetadata("min", p.getMinimum());
            prop.addMetadata("max", p.getMaximum());
            return visual(prop, new Visualizer.SliderVisualizer());
        }
        if (source instanceof ColorProperty p) {
            return visual(color(p, id, title), new Visualizer.ColorVisualizer());
        }
        if (source instanceof TextProperty p) {
            return visual(text(p, id, title), new Visualizer.TextVisualizer());
        }
        if (source instanceof ModeProperty p) {
            Property<Integer> prop = intProp(p, id, title);
            prop.addMetadata("options", prettifyAll(p.getModes()));
            return visual(prop, new Visualizer.DropdownVisualizer());
        }
        if (source instanceof IntChoiceProperty p) {
            Property<Integer> prop = intProp(p, id, title);
            prop.addMetadata("options", choiceNames(p));
            return visual(prop, new Visualizer.DropdownVisualizer());
        }
        if (source instanceof EnumChoiceProperty<?> p) {
            return visual(enumChoice(p, id, title), new Visualizer.DropdownVisualizer());
        }
        if (source instanceof MultiEnumChoiceProperty<?> p) {
            return visual(multiEnumChoice(p, id, title), new Visualizer.MultiSelectDropdownVisualizer());
        }
        if (source instanceof IntRangeProperty p) {
            return visual(intRange(p, id, title), new Visualizer.RangeSliderVisualizer());
        }
        if (source instanceof FloatRangeProperty p) {
            return visual(floatRange(p, id, title), new Visualizer.RangeSliderVisualizer());
        }
        return null;
    }

    // ---------------------------------------------------------------- builders

    private static Property<Boolean> bool(BooleanProperty p, String id, String title) {
        return Properties.functional(p::getValue, p::setValue, id, title, null, Boolean.class);
    }

    /** Shared by IntProperty, PercentProperty, ModeProperty and IntChoiceProperty. */
    private static Property<Integer> intProp(laoqi123.property.Property<?> p, String id, String title) {
        return Properties.functional(
                () -> (Integer) p.getValue(),
                value -> p.setValue(value),
                id, title, null, Integer.class);
    }

    private static Property<Float> floatProp(FloatProperty p, String id, String title) {
        return Properties.functional(p::getValue, p::setValue, id, title, null, Float.class);
    }

    private static Property<String> text(TextProperty p, String id, String title) {
        return Properties.functional(p::getValue, p::setValue, id, title, null, String.class);
    }

    /**
     * ColorProperty stores a packed ARGB int, while OneConfig's colour control works
     * on {@link Color}, so the two representations are translated on the fly.
     */
    private static Property<Color> color(ColorProperty p, String id, String title) {
        return Properties.functional(
                () -> new Color(p.getValue(), true),
                value -> p.setValue(value == null ? 0xFFFFFFFF : value.getRGB()),
                id, title, null, Color.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <E extends Enum<E> & NamedChoice> Property<E> enumChoice(
            EnumChoiceProperty<E> p, String id, String title) {
        E[] values = p.getValues();
        Property<E> prop = Properties.functional(
                p::getValue,
                value -> p.setValue(value),
                id, title, null, (Class<E>) values[0].getDeclaringClass());
        List<String> labels = new ArrayList<>(values.length);
        for (E value : values) {
            labels.add(prettify(value.getChoiceName()));
        }
        // optionValues keeps the enum constants positionally aligned with the labels
        prop.addMetadata("optionValues", values);
        prop.addMetadata("options", labels.toArray(new String[0]));
        return prop;
    }

    /**
     * A multi-select over an EnumSet. OneConfig hands back the set of selected
     * option strings, which are mapped onto the matching enum constants.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <E extends Enum<E> & NamedChoice> Property<String[]> multiEnumChoice(
            MultiEnumChoiceProperty<E> p, String id, String title) {
        E[] values = p.getValues();
        Property<String[]> prop = Properties.functional(
                () -> {
                    EnumSet<E> set = p.getValue();
                    if (set == null || set.isEmpty()) {
                        return new String[0];
                    }
                    List<String> out = new ArrayList<>(set.size());
                    for (E value : set) {
                        out.add(value.getChoiceName());
                    }
                    return out.toArray(new String[0]);
                },
                selected -> {
                    EnumSet set = EnumSet.noneOf(values[0].getDeclaringClass());
                    if (selected != null) {
                        for (String name : selected) {
                            for (E value : values) {
                                if (value.getChoiceName().equalsIgnoreCase(name)) {
                                    set.add(value);
                                    break;
                                }
                            }
                        }
                    }
                    p.setValue(set);
                },
                id, title, null, String[].class);
        List<String> names = new ArrayList<>(values.length);
        for (E value : values) {
            names.add(value.getChoiceName());
        }
        prop.addMetadata("options", names.toArray(new String[0]));
        return prop;
    }

    /** Range sliders read and write a two-element numeric array. */
    private static Property<int[]> intRange(IntRangeProperty p, String id, String title) {
        Property<int[]> prop = Properties.functional(
                p::getValue,
                value -> p.setValue(value),
                id, title, null, int[].class);
        prop.addMetadata("min", 0.0F);
        prop.addMetadata("max", (float) Math.max(p.getMax(), 100));
        prop.addMetadata("step", 1.0F);
        return prop;
    }

    private static Property<float[]> floatRange(FloatRangeProperty p, String id, String title) {
        Property<float[]> prop = Properties.functional(
                p::getValue,
                value -> p.setValue(value),
                id, title, null, float[].class);
        prop.addMetadata("min", 0.0F);
        prop.addMetadata("max", Math.max(p.getMax(), 100.0F));
        return prop;
    }

    // ----------------------------------------------------------------- helpers

    private static <T> Property<T> visual(Property<T> prop, Visualizer visualizer) {
        prop.addMetadata("visualizer", visualizer);
        return prop;
    }

    private static String[] choiceNames(IntChoiceProperty p) {
        // getValuePrompt() joins the choice names with ", " and is the only public
        // accessor for them on IntChoiceProperty.
        String prompt = p.getValuePrompt();
        String[] raw = prompt.isEmpty() ? new String[0] : prompt.split(",\\s*");
        return prettifyAll(raw);
    }

    private static String[] prettifyAll(String[] values) {
        String[] out = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = prettify(values[i]);
        }
        return out;
    }

    /** {@code "fov-fix"} / {@code "FOV_FIX"} -> {@code "Fov fix"}. */
    static String prettify(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String spaced = raw.replace('-', ' ').replace('_', ' ').trim();
        if (spaced.isEmpty()) {
            return raw;
        }
        // Leave deliberate acronyms/CamelCase alone, only fix an all-caps word.
        if (spaced.equals(spaced.toUpperCase()) && spaced.length() > 1) {
            spaced = spaced.charAt(0) + spaced.substring(1).toLowerCase();
        }
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    /** OneConfig ids are used as config keys, so keep them stable and simple. */
    static String sanitizeId(String raw) {
        return raw == null ? "" : raw.replace(' ', '-').toLowerCase();
    }
}
