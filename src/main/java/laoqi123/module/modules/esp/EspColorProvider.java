package laoqi123.module.modules.esp;

import net.minecraft.entity.Entity;

import java.awt.Color;

public interface EspColorProvider {
    Color getColor(Object var1);

    default float getFillAlpha() {
        return 0.125f;
    }

    boolean shouldHighlight(Entity var1);

    default float getOutlineAlpha() {
        return 0.25f;
    }

    default float getLineWidth() {
        return 0.03f;
    }
}