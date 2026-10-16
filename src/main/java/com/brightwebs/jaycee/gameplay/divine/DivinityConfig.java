package com.brightwebs.jaycee.gameplay.divine;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

/** Provisional server tuning, not natural progression recipes or canonical costs. */
public final class DivinityConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.LongValue EP, ESSENCE, SUPREME, REGEN, MINIMUM, POWER, EXPIRY;
    private static final ModConfigSpec.IntValue PERCENT;
    private static final ModConfigSpec.DoubleValue DISTANCE;
    private static final ModConfigSpec.LongValue PROGRESSION_COEFFICIENT;
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> GRADE_LEVELS;
    static {
        var b = new ModConfigSpec.Builder();
        b.comment("Provisional Divine Ascension tuning, not canon.",
            "EP is permanent power, Essence spendable. Cumulative earned Essence independently drives level/grade; NPC offering recipes are separate.",
            "Provisional establishment fee = max(minimum, ceil(recipient current Essence * percent / 100)), burned IN ADDITION to the transfer.",
            "The approximately 3-million threshold's exact capped field is unresolved; these independent caps are provisional policy.",
            "Each explicit power also burns powerFee. Jaycee bypasses fees but still pays transferred Essence.",
            "Cap revocation never deletes historical EP or existing Essence; new credits stop at the cap.");
        EP = b.defineInRange("ordinaryEpCap", 3_000_000L, 1L, Long.MAX_VALUE);
        ESSENCE = b.defineInRange("ordinaryEssenceCap", 3_000_000L, 1L, Long.MAX_VALUE);
        SUPREME = b.defineInRange("jayceeEssenceCap", 2_000_000_000L, 1L, Long.MAX_VALUE);
        REGEN = b.defineInRange("jayceeEssencePerSecond", 1_000_000L, 0L, Long.MAX_VALUE);
        MINIMUM = b.defineInRange("minimumEstablishmentFee", 100L, 1L, Long.MAX_VALUE);
        PERCENT = b.defineInRange("establishmentPercent", 125, 1, 10000);
        POWER = b.defineInRange("powerFee", 100L, 1L, Long.MAX_VALUE);
        EXPIRY = b.defineInRange("offerLifetimeMillis", 120_000L, 1000L, 3_600_000L);
        DISTANCE = b.defineInRange("bestowalDistance", 8.0, 1.0, 32.0);
        PROGRESSION_COEFFICIENT = b.comment("Essence progression: next level costs coefficient * (current level + 1)^2; level one begins at zero. Never uses vanilla XP.")
            .defineInRange("essenceLevelCoefficient", 100L, 1L, Long.MAX_VALUE);
        GRADE_LEVELS = b.comment("Strictly increasing minimum levels for F E D C B A S SS SSS; F must start at one. SSS also requires cap lift or unique authority.")
            .define("essenceGradeMinimumLevels", List.of(1, 11, 21, 36, 51, 66, 81, 96, 111), value -> {
                if (!(value instanceof List<?> levels) || levels.size() != 9) return false;
                int previous = 0;
                for (Object entry : levels) {
                    if (!(entry instanceof Integer level) || level <= previous) return false;
                    if (previous == 0 && level != 1) return false;
                    previous = level;
                }
                return true;
            });
        SPEC = b.build();
    }
    public static DivinityModel.Config values() {
        return new DivinityModel.Config(EP.get(), ESSENCE.get(), SUPREME.get(), REGEN.get(), MINIMUM.get(),
            PERCENT.get(), POWER.get(), EXPIRY.get(), DISTANCE.get(),
            new EssenceProgression.Tuning(PROGRESSION_COEFFICIENT.get(), List.copyOf(GRADE_LEVELS.get())));
    }
    private DivinityConfig() {}
}
