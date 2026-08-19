package laoqi123.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class SoundUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void playSound(String soundName) {
        Identifier id = Identifier.tryParse(soundName);
        if (id == null) {
            return;
        }
        SoundEvent soundEvent = Registries.SOUND_EVENT.get(id);
        if (soundEvent == null) {
            return;
        }
        mc.getSoundManager().play(PositionedSoundInstance.master(soundEvent, 1.0F, 1.0F));
    }
}
