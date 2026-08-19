package laoqi123.util.config;

import laoqi123.value.Value;
import laoqi123.value.properties.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class Configurable {
    private final String name;
    private final List<Value<?>> properties = new ArrayList<>();
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

    protected <T extends Value<?>> T register(T property) {
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

    public List<Value<?>> collectProperties() {
        List<Value<?>> all = new ArrayList<>(this.properties);
        for (Configurable child : this.children) {
            all.addAll(child.collectProperties());
        }
        return all;
    }

    protected FloatValue floatProperty(String name, float value, float min, float max) {
        return this.register(new FloatValue(name, value, min, max));
    }

    protected FloatValue floatProperty(String name, float value, float min, float max, BooleanSupplier check) {
        return this.register(new FloatValue(name, value, min, max, check));
    }

    protected BooleanValue booleanProperty(String name, boolean value) {
        return this.register(new BooleanValue(name, value));
    }

    protected BooleanValue booleanProperty(String name, boolean value, BooleanSupplier check) {
        return this.register(new BooleanValue(name, value, check));
    }

    protected IntValue intProperty(String name, int value, int min, int max) {
        return this.register(new IntValue(name, value, min, max));
    }

    protected IntValue intProperty(String name, int value, int min, int max, BooleanSupplier check) {
        return this.register(new IntValue(name, value, min, max, check));
    }

    protected PercentValue percentProperty(String name, int value) {
        return this.register(new PercentValue(name, value));
    }

    protected PercentValue percentProperty(String name, int value, BooleanSupplier check) {
        return this.register(new PercentValue(name, value, check));
    }

    protected TextValue textProperty(String name, String value) {
        return this.register(new TextValue(name, value));
    }

    protected ColorValue colorProperty(String name, int color) {
        return this.register(new ColorValue(name, color));
    }

    protected ColorValue colorProperty(String name, int color, BooleanSupplier check) {
        return this.register(new ColorValue(name, color, check));
    }

    protected IntRangeValue intRangeProperty(String name, int min, int max, int boundMin, int boundMax) {
        return this.register(new IntRangeValue(name, min, max, boundMin, boundMax));
    }

    protected IntRangeValue intRangeProperty(String name, int min, int max, int boundMin, int boundMax, BooleanSupplier check) {
        return this.register(new IntRangeValue(name, min, max, boundMin, boundMax, check));
    }

    protected FloatRangeValue floatRangeProperty(String name, float min, float max, float boundMin, float boundMax) {
        return this.register(new FloatRangeValue(name, min, max, boundMin, boundMax));
    }

    protected FloatRangeValue floatRangeProperty(String name, float min, float max, float boundMin, float boundMax, BooleanSupplier check) {
        return this.register(new FloatRangeValue(name, min, max, boundMin, boundMax, check));
    }

    protected <E extends Enum<E> & NamedChoice> EnumChoiceValue<E> enumChoiceProperty(String name, E value) {
        return this.register(new EnumChoiceValue<>(name, value, null));
    }

    protected <E extends Enum<E> & NamedChoice> MultiEnumChoiceValue<E> multiEnumChoiceProperty(String name, E[] values) {
        return this.register(new MultiEnumChoiceValue<>(name, values, null));
    }

    protected ChoiceConfigurable choices(String name, int activeIndex, Choice... choices) {
        ChoiceConfigurable choiceConfigurable = new ChoiceConfigurable(name, activeIndex, choices);
        this.tree(choiceConfigurable);
        return choiceConfigurable;
    }
}
