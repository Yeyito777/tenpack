package dev.yeyito.tenpackdeath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Small hot-reloadable properties config for Tenpack's corpse/death rules.
 *
 * <p>This deliberately uses a simple .properties file instead of NeoForge's config
 * API because these rules are pack-level knobs, not user-facing mod options.</p>
 */
class TenpackDeathConfig {
    private static final Path PATH = Path.of("config", "tenpackdeath.properties");

    private long lastModified = Long.MIN_VALUE;

    boolean ownerProtectionEnabled = true;
    boolean publicLootRequiresSkeleton = true;
    int publicLootAfterSeconds = 60;
    boolean opsBypassProtection = false;
    boolean notifyDeniedAccess = false;

    boolean deathSoundEnabled = true;
    float deathSoundVolume = 1.0F;

    boolean decayEnabled = true;
    int decayStartSeconds = 300;
    int decayIntervalSeconds = 30;
    boolean randomDecay = true;
    boolean notifyDecayStarted = false;
    boolean notifyInternalErrors = false;

    boolean breakCorpseDropsAfterDecay = true;
    boolean requireDecayStartedToBreakCorpse = true;
    boolean breakCorpseAtSkeleton = true;
    boolean breakCorpseDropsExperience = true;
    boolean dropExperienceAsLevels = true;

    void loadIfNeeded() {
        try {
            if (!Files.exists(PATH)) {
                writeDefaultConfig();
            }
            long modified = Files.getLastModifiedTime(PATH).toMillis();
            if (modified == lastModified) {
                return;
            }
            Properties properties = new Properties();
            try (var reader = Files.newBufferedReader(PATH)) {
                properties.load(reader);
            }

            ownerProtectionEnabled = bool(properties, "ownerProtectionEnabled", ownerProtectionEnabled);
            publicLootRequiresSkeleton = bool(properties, "publicLootRequiresSkeleton", publicLootRequiresSkeleton);
            publicLootAfterSeconds = positiveInt(properties, "publicLootAfterSeconds", publicLootAfterSeconds);
            opsBypassProtection = bool(properties, "opsBypassProtection", opsBypassProtection);
            notifyDeniedAccess = bool(properties, "notifyDeniedAccess", notifyDeniedAccess);

            deathSoundEnabled = bool(properties, "deathSoundEnabled", deathSoundEnabled);
            deathSoundVolume = positiveFloat(properties, "deathSoundVolume", deathSoundVolume);

            decayEnabled = bool(properties, "decayEnabled", decayEnabled);
            decayStartSeconds = positiveInt(properties, "decayStartSeconds", decayStartSeconds);
            decayIntervalSeconds = positiveInt(properties, "decayIntervalSeconds", decayIntervalSeconds);
            randomDecay = bool(properties, "randomDecay", randomDecay);
            notifyDecayStarted = bool(properties, "notifyDecayStarted", notifyDecayStarted);
            notifyInternalErrors = bool(properties, "notifyInternalErrors", notifyInternalErrors);

            breakCorpseDropsAfterDecay = bool(properties, "breakCorpseDropsAfterDecay", breakCorpseDropsAfterDecay);
            requireDecayStartedToBreakCorpse = bool(properties, "requireDecayStartedToBreakCorpse", requireDecayStartedToBreakCorpse);
            breakCorpseAtSkeleton = bool(properties, "breakCorpseAtSkeleton", breakCorpseAtSkeleton);
            breakCorpseDropsExperience = bool(properties, "breakCorpseDropsExperience", breakCorpseDropsExperience);
            dropExperienceAsLevels = bool(properties, "dropExperienceAsLevels", dropExperienceAsLevels);

            lastModified = modified;
        } catch (IOException ignored) {
            // Keep current/default config if the file cannot be read.
        }
    }

    private void writeDefaultConfig() throws IOException {
        Files.createDirectories(PATH.getParent());
        Files.writeString(PATH, """
                # Tenpack Death config. Times are in real seconds.
                # Owner protection means only the dead player can loot their corpse.
                # If publicLootRequiresSkeleton=true, non-owners cannot loot until Corpse marks it as a skeleton.
                # If false, publicLootAfterSeconds is used instead.
                ownerProtectionEnabled=true
                publicLootRequiresSkeleton=true
                publicLootAfterSeconds=60
                opsBypassProtection=false
                notifyDeniedAccess=false

                # Re:Zero-style Return by Death inspired sound cue. Uses vanilla ominous sounds, not a bundled copyrighted clip.
                deathSoundEnabled=true
                deathSoundVolume=1.0

                # Decay starts after decayStartSeconds and removes one stored item stack every decayIntervalSeconds.
                decayEnabled=true
                decayStartSeconds=300
                decayIntervalSeconds=30
                randomDecay=true
                notifyDecayStarted=false
                notifyInternalErrors=false

                # After decay has started, attacking/breaking a corpse spills everything remaining.
                # XP is derived from Corpse's stored death experience level and dropped without vanilla XP-loss limits.
                breakCorpseDropsAfterDecay=true
                requireDecayStartedToBreakCorpse=true
                breakCorpseAtSkeleton=true
                breakCorpseDropsExperience=true
                dropExperienceAsLevels=true
                """);
    }

    private static boolean bool(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private static int positiveInt(Properties properties, String key, int fallback) {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float positiveFloat(Properties properties, String key, float fallback) {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Math.max(0.0F, Float.parseFloat(value.trim()));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
