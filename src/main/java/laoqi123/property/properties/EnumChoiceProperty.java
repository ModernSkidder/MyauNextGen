package laoqi123.property.properties;

import com.google.gson.JsonObject;
import laoqi123.property.Property;
import laoqi123.util.config.NamedChoice;

import java.util.function.BooleanSupplier;

public class EnumChoiceProperty<E extends Enum<E> & NamedChoice> extends Property<E> {

    private final E[] values;

    public EnumChoiceProperty(String name, E value, BooleanSupplier check) {
        super(name, value, check);
        this.values = value.getDeclaringClass().getEnumConstants();
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
        E value = this.getValue();
        return value == null ? "&4?" : String.format("&9%s", value.getChoiceName());
    }

    @Override
    public boolean parseString(String string) {
        String clean = string.replace("_", "").replace(" ", "");
        for (E value : this.values) {
            if (value.getChoiceName().replace("_", "").replace(" ", "").equalsIgnoreCase(clean)
                    || value.name().replace("_", "").equalsIgnoreCase(clean)) {
                return this.setValue(value);
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
        E value = this.getValue();
        jsonObject.addProperty(this.getName(), value == null ? "" : value.getChoiceName());
    }
}
