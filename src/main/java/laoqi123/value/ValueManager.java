package laoqi123.value;

import laoqi123.module.Module;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ValueManager {
    public LinkedHashMap<Class<?>, ArrayList<Value<?>>> properties = new LinkedHashMap<>();

    public Value<?> getProperty(Module module, String string) {
        ArrayList<Value<?>> values = properties.get(module.getClass());
        if (values == null) {
            return null;
        }
        String input = normalize(string);
        for (Value<?> value : values) {
            if (normalize(value.getName()).equalsIgnoreCase(input)) {
                return value;
            }
        }
        return null;
    }

    private String normalize(String string) {
        return string.replace("-", "").replaceAll("\\s+", "");
    }
}
