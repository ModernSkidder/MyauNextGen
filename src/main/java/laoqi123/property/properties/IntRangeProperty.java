package laoqi123.property.properties;

import com.google.gson.JsonObject;
import laoqi123.property.Property;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

public class IntRangeProperty extends Property<int[]> {

    public IntRangeProperty(String name, int min, int max, int boundMin, int boundMax) {
        this(name, min, max, boundMin, boundMax, null);
    }

    public IntRangeProperty(String name, int min, int max, int boundMin, int boundMax, BooleanSupplier check) {
        super(name, new int[]{min, max}, v -> v[0] >= boundMin && v[0] <= boundMax && v[1] >= boundMin && v[1] <= boundMax && v[0] <= v[1], check);
    }

    public int getMin() {
        return this.getValue()[0];
    }

    public int getMax() {
        return this.getValue()[1];
    }

    public int random() {
        int[] value = this.getValue();
        int low = Math.min(value[0], value[1]);
        int high = Math.max(value[0], value[1]);
        return low == high ? low : ThreadLocalRandom.current().nextInt(low, high + 1);
    }

    @Override
    public String getValuePrompt() {
        return "min..max";
    }

    @Override
    public String formatValue() {
        return String.format("&e%d&7..&e%d", this.getMin(), this.getMax());
    }

    @Override
    public boolean parseString(String string) {
        String[] parts = string.split("\\.\\.");
        if (parts.length != 2) {
            return false;
        }
        try {
            int min = Integer.parseInt(parts[0].trim());
            int max = Integer.parseInt(parts[1].trim());
            return this.setValue(new int[]{Math.min(min, max), Math.max(min, max)});
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        return this.parseString(jsonObject.get(this.getName()).getAsString());
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(this.getName(), this.getMin() + ".." + this.getMax());
    }
}
