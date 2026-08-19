package laoqi123.util.config;

public class NoneChoice extends Choice {
    public NoneChoice(ChoiceConfigurable parent) {
        super("None");
        this.setParentChoice(parent);
    }
}
