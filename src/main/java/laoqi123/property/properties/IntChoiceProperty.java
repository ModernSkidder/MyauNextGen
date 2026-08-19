package laoqi123.property.properties;

import com.google.gson.JsonObject;
import laoqi123.property.Property;
import laoqi123.util.config.Choice;
import laoqi123.util.config.ChoiceConfigurable;

import java.util.function.BooleanSupplier;

public class IntChoiceProperty extends Property<Integer> {
    private final ChoiceConfigurable configurable;

    public IntChoiceProperty(ChoiceConfigurable configurable) {
        super(configurable.getName(), configurable.getActiveIndex(), (BooleanSupplier) null);
        this.configurable = configurable;
    }

    @Override
    public Integer getValue() {
        return this.configurable.getActiveIndex();
    }

    @Override
    public boolean setValue(Object value) {
        this.configurable.setActiveIndex((Integer) value);
        return true;
    }

    @Override
    public String getValuePrompt() {
        StringBuilder sb = new StringBuilder();
        for (Choice choice : this.configurable.getChoices()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(choice.getChoiceName());
        }
        return sb.toString();
    }

    @Override
    public String formatValue() {
        Choice active = this.configurable.getActiveChoice();
        return active == null ? "&4?" : String.format("&9%s", active.getChoiceName());
    }

    @Override
    public boolean parseString(String string) {
        String clean = string.replace("_", "").replace(" ", "");
        for (Choice choice : this.configurable.getChoices()) {
            if (choice.getChoiceName().replace("_", "").replace(" ", "").equalsIgnoreCase(clean)) {
                this.configurable.setActiveChoice(choice);
                return true;
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
        Choice active = this.configurable.getActiveChoice();
        jsonObject.addProperty(this.getName(), active == null ? "" : active.getChoiceName());
    }
}
