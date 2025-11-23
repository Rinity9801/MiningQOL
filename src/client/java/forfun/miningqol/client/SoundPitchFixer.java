package forfun.miningqol.client;

import net.minecraft.sound.SoundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class SoundPitchFixer {
    private static final Logger LOGGER = LoggerFactory.getLogger("SoundPitchFixer");
    private static boolean enabled = false;
    private static final Set<String> fixedSounds = new HashSet<>();

    static {
        // Common sounds that benefit from fixed pitch
        fixedSounds.add("minecraft:block.amethyst_block.break");
        fixedSounds.add("minecraft:block.amethyst_block.hit");
        fixedSounds.add("minecraft:block.amethyst_block.place");
        fixedSounds.add("minecraft:block.amethyst_cluster.break");
        fixedSounds.add("minecraft:block.glass.break");
        fixedSounds.add("minecraft:block.glass.place");
        fixedSounds.add("minecraft:entity.experience_orb.pickup");
        fixedSounds.add("minecraft:block.stone.break");
        fixedSounds.add("minecraft:block.stone.hit");
    }

    public static void setEnabled(boolean enabled) {
        SoundPitchFixer.enabled = enabled;
        LOGGER.info("SoundPitchFixer " + (enabled ? "enabled" : "disabled"));
    }

    public static boolean isEnabled() {
        return enabled;
    }


    public static boolean shouldFixPitch(net.minecraft.util.Identifier soundId) {
        if (!enabled) return false;
        if (soundId == null) return false;

        return fixedSounds.contains(soundId.toString());
    }

    public static void addSound(String soundId) {
        fixedSounds.add(soundId);
        LOGGER.info("Added sound to pitch fixer: " + soundId);
    }

    public static void removeSound(String soundId) {
        fixedSounds.remove(soundId);
        LOGGER.info("Removed sound from pitch fixer: " + soundId);
    }

    public static Set<String> getFixedSounds() {
        return new HashSet<>(fixedSounds);
    }
}
