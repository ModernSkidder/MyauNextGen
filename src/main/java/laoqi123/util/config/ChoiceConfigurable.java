package laoqi123.util.config;

import laoqi123.value.properties.IntChoiceValue;

import java.util.Arrays;
import java.util.List;

public class ChoiceConfigurable extends Configurable {
    private final List<Choice> choices;
    private final IntChoiceValue activeChoiceProperty;
    private int activeIndex;

    public ChoiceConfigurable(String name, int activeIndex, Choice... choices) {
        super(name);
        this.choices = Arrays.asList(choices);
        for (Choice choice : choices) {
            choice.setParentChoice(this);
        }
        this.activeIndex = Math.max(0, Math.min(activeIndex, choices.length - 1));
        this.activeChoiceProperty = new IntChoiceValue(this);
        this.register(this.activeChoiceProperty);
    }

    public List<Choice> getChoices() {
        return this.choices;
    }

    public Choice getActiveChoice() {
        return this.choices.get(this.activeIndex);
    }

    public int getActiveIndex() {
        return this.activeIndex;
    }

    public void setActiveIndex(int index) {
        this.activeIndex = Math.max(0, Math.min(index, this.choices.size() - 1));
    }

    public void setActiveChoice(Choice choice) {
        this.activeIndex = this.choices.indexOf(choice);
    }

    public ChoiceConfigurable doNotIncludeAlways() {
        this.activeChoiceProperty.doNotIncludeAlways();
        return this;
    }
}
