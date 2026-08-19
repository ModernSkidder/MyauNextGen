package laoqi123.module.modules.render.esp;

import net.minecraft.entity.Entity;

import java.awt.Color;
import java.util.Set;

public class ClassEspColor implements EspColorProvider {
    private final Color color;
    private final Set<Class<?>> entityClasses;

    public ClassEspColor(Set<Class<?>> set) {
        this(set, new Color(255, 255, 255));
    }

    public ClassEspColor(Set<Class<?>> set, Color color) {
        this.entityClasses = set;
        this.color = color;
    }

    @Override
    public Color getColor(Object object) {
        return this.color;
    }

    @Override
    public boolean shouldHighlight(Entity entity) {
        for (Class<?> clazz : this.entityClasses) {
            if (!clazz.isInstance(entity)) continue;
            return true;
        }
        return false;
    }
}