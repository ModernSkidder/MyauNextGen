package laoqi123.util.config;

public abstract class Choice extends Configurable implements NamedChoice {
    private ChoiceConfigurable parentChoice;

    public Choice(String name) {
        super(name);
    }

    public ChoiceConfigurable getParentChoice() {
        return this.parentChoice;
    }

    public void setParentChoice(ChoiceConfigurable parentChoice) {
        this.parentChoice = parentChoice;
        this.setParent(parentChoice);
    }

    @Override
    public String getChoiceName() {
        return this.getName();
    }

    public boolean isSelected() {
        return this.parentChoice != null && this.parentChoice.getActiveChoice() == this;
    }

    @Override
    public boolean running() {
        return super.running() && this.isSelected();
    }

    public void enable() {
    }

    public void disable() {
    }
}
