package laoqi123.util.config;

import laoqi123.property.Property;
import laoqi123.property.properties.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class Configurable {
    private final String name;
    private final List<Property<?>> properties = new ArrayList<>();
    private final List<Configurable> children = new ArrayList<>();
    private Configurable parent;
    private BooleanSupplier runningOverride;

    public Configurable(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public Configurable getParent() {
        return this.parent;
    }

    public void setParent(Configurable parent) {
        this.parent = parent;
    }

    public void setRunningOverride(BooleanSupplier runningOverride) {
        this.runningOverride = runningOverride;
    }

    public boolean running() {
        return (this.parent == null || this.parent.running())
                && (this.runningOverride == null || this.runningOverride.getAsBoolean());
    }

    protected <T extends Property<?>> T register(T property) {
        this.properties.add(property);
        return property;
    }

    protected void tree(Configurable child) {
        child.setParent(this);
        this.children.add(child);
    }

    public void addChild(Configurable child) {
        this.tree(child);
    }

    public List<Property<?>> collectProperties() {
        List<Property<?>> all = new ArrayList<>(this.properties);
        for (Configurable child : this.children) {
            all.addAll(child.collectProperties());
        }
        return all;
    }

    protected FloatProperty floatProperty(String name, float value, float min, float max) {
        return this.register(new FloatProperty(name, value, min, max));
    }

    protected FloatProperty floatProperty(String name, float value, float min, float max, BooleanSupplier check) {
        return this.register(new FloatProperty(name, value, min, max, check));
    }

    protected BooleanProperty booleanProperty(String name, boolean value) {
        return this.register(new BooleanProperty(name, value));
    }

    protected BooleanProperty booleanProperty(String name, boolean value, BooleanSupplier check) {
        return this.register(new BooleanProperty(name, value, check));
    }

    protected IntProperty intProperty(String name, int value, int min, int max) {
        return this.register(new IntProperty(name, value, min, max));
    }

    protected IntProperty intProperty(String name, int value, int min, int max, BooleanSupplier check) {
        return this.register(new IntProperty(name, value, min, max, check));
    }

    protected PercentProperty percentProperty(String name, int value) {
        return this.register(new PercentProperty(name, value));
    }

    protected PercentProperty percentProperty(String name, int value, BooleanSupplier check) {
        return this.register(new PercentProperty(name, value, check));
    }

    protected TextProperty textProperty(String name, String value) {
        return this.register(new TextProperty(name, value));
    }

    protected ColorProperty colorProperty(String name, int color) {
        return this.register(new ColorProperty(name, color));
    }

    protected ColorProperty colorProperty(String name, int color, BooleanSupplier check) {
        return this.register(new ColorProperty(name, color, check));
    }

    protected IntRangeProperty intRangeProperty(String name, int min, int max, int boundMin, int boundMax) {
        return this.register(new IntRangeProperty(name, min, max, boundMin, boundMax));
    }

    protected IntRangeProperty intRangeProperty(String name, int min, int max, int boundMin, int boundMax, BooleanSupplier check) {
        return this.register(new IntRangeProperty(name, min, max, boundMin, boundMax, check));
    }

    protected FloatRangeProperty floatRangeProperty(String name, float min, float max, float boundMin, float boundMax) {
        return this.register(new FloatRangeProperty(name, min, max, boundMin, boundMax));
    }

    protected FloatRangeProperty floatRangeProperty(String name, float min, float max, float boundMin, float boundMax, BooleanSupplier check) {
        return this.register(new FloatRangeProperty(name, min, max, boundMin, boundMax, check));
    }

    protected <E extends Enum<E> & NamedChoice> EnumChoiceProperty<E> enumChoiceProperty(String name, E value) {
        return this.register(new EnumChoiceProperty<>(name, value, null));
    }

    protected <E extends Enum<E> & NamedChoice> MultiEnumChoiceProperty<E> multiEnumChoiceProperty(String name, E[] values) {
        return this.register(new MultiEnumChoiceProperty<>(name, values, null));
    }

    protected ChoiceConfigurable choices(String name, int activeIndex, Choice... choices) {
        ChoiceConfigurable choiceConfigurable = new ChoiceConfigurable(name, activeIndex, choices);
        this.tree(choiceConfigurable);
        return choiceConfigurable;
    }
}
