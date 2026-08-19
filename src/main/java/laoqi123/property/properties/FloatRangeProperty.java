package laoqi123.property.properties;

import com.google.gson.JsonObject;
import laoqi123.property.Property;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

public class FloatRangeProperty extends Property<float[]> {

    public FloatRangeProperty(String name, float min, float max, float boundMin, float boundMax) {
        this(name, min, max, boundMin, boundMax, null);
    }

    public FloatRangeProperty(String name, float min, float max, float boundMin, float boundMax, BooleanSupplier check) {
        super(name, new float[]{min, max}, v -> v[0] >= boundMin && v[0] <= boundMax && v[1] >= boundMin && v[1] <= boundMax && v[0] <= v[1], check);
    }

    public float getMin() {
        return this.getValue()[0];
    }

    public float getMax() {
        return this.getValue()[1];
    }

    public float random() {
        float[] value = this.getValue();
        float low = Math.min(value[0], value[1]);
        float high = Math.max(value[0], value[1]);
        return low == high ? low : (float) ThreadLocalRandom.current().nextDouble(low, high);
    }

    @Override
    public String getValuePrompt() {
        return "min..max";
    }

    @Override
    public String formatValue() {
        return String.format("&6%.1f&7..&6%.1f", this.getMin(), this.getMax());
    }

    @Override
    public boolean parseString(String string) {
        String[] parts = string.split("\\.\\.");
        if (parts.length != 2) {
            return false;
        }
        try {
            float min = Float.parseFloat(parts[0].trim());
            float max = Float.parseFloat(parts[1].trim());
            return this.setValue(new float[]{Math.min(min, max), Math.max(min, max)});
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
