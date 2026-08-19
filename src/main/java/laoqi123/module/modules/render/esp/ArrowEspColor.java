package laoqi123.module.modules.render.esp;

import net.minecraft.entity.projectile.ArrowEntity;

import java.awt.Color;
import java.util.Collections;
import java.util.HashSet;

public class ArrowEspColor extends ClassEspColor {
    public ArrowEspColor() {
        super(new HashSet<>(Collections.singletonList(ArrowEntity.class)), new Color(255, 0, 0));
    }

    @Override
    public float getFillAlpha() {
        return 0.25f;
    }

    @Override
    public float getOutlineAlpha() {
        return 0.5f;
    }

    @Override
    public float getLineWidth() {
        return 0.05f;
    }
}