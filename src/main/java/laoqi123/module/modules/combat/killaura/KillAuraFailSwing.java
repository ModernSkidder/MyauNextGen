package laoqi123.module.modules.combat.killaura;

import laoqi123.value.properties.*;
import laoqi123.value.properties.BooleanValue;
import laoqi123.util.RenderUtil;
import laoqi123.util.config.Choice;
import laoqi123.util.config.ChoiceConfigurable;
import laoqi123.util.config.NoneChoice;
import laoqi123.util.config.ToggleableConfigurable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class KillAuraFailSwing extends ToggleableConfigurable {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public final FloatRangeValue additionalRange;
    public final ChoiceConfigurable notifyWhenFail;
    public final BoxChoice box;
    public final SoundChoice sound;

    private final List<FailedHit> failedHits = new ArrayList<>();
    private float currentAdditionalRange;

    public KillAuraFailSwing() {
        super("FailSwing", true);
        this.additionalRange = this.register(new FloatRangeValue("AdditionalRange", 2.5f, 3.0f, 0.0f, 10.0f));
        this.additionalRange.setChangeListener(range -> this.currentAdditionalRange = this.additionalRange.random());
        this.currentAdditionalRange = this.additionalRange.random();

        this.box = new BoxChoice();
        this.sound = new SoundChoice();
        this.notifyWhenFail = new ChoiceConfigurable("NotifyWhenFail", 1, new NoneChoice(null), this.box, this.sound);
        this.notifyWhenFail.doNotIncludeAlways();
        this.addChild(this.notifyWhenFail);
    }

    public float getCurrentAdditionalRange() {
        return this.currentAdditionalRange;
    }

    public void recordFailedHit(LivingEntity entity) {
        this.currentAdditionalRange = this.additionalRange.random();
        if (this.box.isSelected()) {
            Vec3d point = entity.getBoundingBox().getCenter();
            this.failedHits.add(new FailedHit(point, 0));
        } else if (this.sound.isSelected()) {
            this.playSound();
        }
    }

    private void playSound() {
        Identifier id = Identifier.tryParse("minecraft:ui.button.click");
        if (id == null) {
            return;
        }
        SoundEvent soundEvent = Registries.SOUND_EVENT.get(id);
        if (soundEvent == null) {
            return;
        }
        float volume = Math.max(0.0f, Math.min(1.0f, this.sound.volume.getValue() / 100.0f));
        mc.getSoundManager().play(PositionedSoundInstance.master(soundEvent, volume, this.sound.pitch.getValue()));
    }

    public void renderFailedHits() {
        int maxAge = this.box.fade.getValue() * 50;
        Iterator<FailedHit> iterator = this.failedHits.iterator();
        while (iterator.hasNext()) {
            FailedHit hit = iterator.next();
            if (hit.age >= maxAge) {
                iterator.remove();
                continue;
            }
            hit.age++;
        }
        if (this.failedHits.isEmpty() || !this.isEnabled() || !this.box.isSelected()) {
            this.failedHits.clear();
            return;
        }

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        RenderUtil.enableRenderState();
        Color base = this.box.rainbow.getValue()
                ? Color.getHSBColor((System.currentTimeMillis() % 3600) / 3600.0F, 1.0F, 1.0F)
                : new Color(this.box.color.getValue());
        for (FailedHit hit : this.failedHits) {
            int fade = (int) (255.0D + (0.0D - 255.0D) * (double) hit.age / (double) maxAge);
            double x = hit.pos.x - cameraPos.x;
            double y = hit.pos.y - cameraPos.y;
            double z = hit.pos.z - cameraPos.z;
            Box pointBox = new Box(x, y, z, x + 0.05, y + 0.05, z + 0.05);
            RenderUtil.drawBoundingBox(pointBox, base.getRed(), base.getGreen(), base.getBlue(), fade, 2.0F);
            RenderUtil.drawFilledBox(pointBox, base.getRed(), base.getGreen(), base.getBlue());
        }
        RenderUtil.disableRenderState();
    }

    public void reset() {
        this.failedHits.clear();
    }

    public static class BoxChoice extends Choice {
        public final IntValue fade;
        public final ColorValue color;
        public final BooleanValue rainbow;

        BoxChoice() {
            super("Box");
            this.fade = this.register(new IntValue("Fade", 4, 1, 10));
            this.color = this.register(new ColorValue("Color", new Color(255, 179, 72).getRGB()));
            this.rainbow = this.register(new BooleanValue("Rainbow", false));
        }
    }

    public static class SoundChoice extends Choice {
        public final FloatValue volume;
        public final FloatValue pitch;

        SoundChoice() {
            super("Sound");
            this.volume = this.register(new FloatValue("Volume", 50.0f, 0.0f, 100.0f));
            this.pitch = this.register(new FloatValue("Pitch", 0.8f, 0.0f, 2.0f));
        }
    }

    private static class FailedHit {
        private final Vec3d pos;
        private int age;

        FailedHit(Vec3d pos, int age) {
            this.pos = pos;
            this.age = age;
        }
    }
}
