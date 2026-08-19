package laoqi123.module.modules.render.esp;

import net.minecraft.entity.projectile.thrown.PotionEntity;

import java.awt.Color;
import java.util.Collections;
import java.util.HashSet;

public class PotionEspColor extends ClassEspColor {
    public PotionEspColor() {
        super(new HashSet<>(Collections.singleton(PotionEntity.class)), new Color(255, 66, 249));
    }

    @Override
    public float getLineWidth() {
        return 0.05f;
    }
}