package io.github.whileaway;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class StoryConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENCOUNTERS, MUSIC;
    public static final ModConfigSpec.DoubleValue SOUND_GAIN;
    public static final ModConfigSpec.IntValue COOLDOWN_SECONDS;
    static {
        var b = new ModConfigSpec.Builder();
        ENCOUNTERS = b.comment("Allow the first encounter after the signal is restored. Does not erase progress.").define("encounters", true);
        MUSIC = b.comment("Play original, brief story cues. Vanilla music remains otherwise unchanged.").define("storyMusic", true);
        SOUND_GAIN = b.comment("Multiplier for this mod's effects; Minecraft sound sliders still apply.").defineInRange("soundGain", 0.65, 0.0, 1.0);
        COOLDOWN_SECONDS = b.comment("Minimum time between scripted events. Reading adds a separate grace period.").defineInRange("eventCooldownSeconds", 90, 20, 900);
        SPEC = b.build();
    }
    private StoryConfig() {}
}
