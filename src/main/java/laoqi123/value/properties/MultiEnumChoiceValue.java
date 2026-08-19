package laoqi123.value.properties;

import com.google.gson.JsonObject;
import laoqi123.value.Value;
import laoqi123.util.config.NamedChoice;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;

public class MultiEnumChoiceValue<E extends Enum<E> & NamedChoice> extends Value<EnumSet<E>> {

    private final E[] values;

    public MultiEnumChoiceValue(String name, E[] values, BooleanSupplier check) {
        super(name, EnumSet.copyOf(java.util.Arrays.asList(values)), check);
        this.values = values;
    }

    public E[] getValues() {
        return this.values;
    }

    @Override
    public String getValuePrompt() {
        StringBuilder sb = new StringBuilder();
        for (E value : this.values) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(value.getChoiceName());
        }
        return sb.toString();
    }

    @Override
    public String formatValue() {
        EnumSet<E> set = this.getValue();
        if (set == null || set.isEmpty()) {
            return "&4?";
        }
        StringBuilder sb = new StringBuilder();
        for (E value : set) {
            if (sb.length() > 0) {
                sb.append("&7, &9");
            }
            sb.append(value.getChoiceName());
        }
        return "&9" + sb;
    }

    @Override
    public boolean parseString(String string) {
        String clean = string.replace("_", "").replace(" ", "");
        for (E value : this.values) {
            if (value.getChoiceName().replace("_", "").replace(" ", "").equalsIgnoreCase(clean)
                    || value.name().replace("_", "").equalsIgnoreCase(clean)) {
                EnumSet<E> set = this.getValue();
                if (set == null) {
                    set = EnumSet.noneOf(this.values[0].getDeclaringClass());
                }
                if (set.contains(value)) {
                    set.remove(value);
                } else {
                    set.add(value);
                }
                return this.setValue(set);
            }
        }
        return false;
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        return this.parseString(jsonObject.get(this.getName()).getAsString());
    }

    @Override
    public void write(JsonObject jsonObject) {
        EnumSet<E> set = this.getValue();
        StringBuilder sb = new StringBuilder();
        if (set != null) {
            for (E value : set) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(value.getChoiceName());
            }
        }
        jsonObject.addProperty(this.getName(), sb.toString());
    }
}
