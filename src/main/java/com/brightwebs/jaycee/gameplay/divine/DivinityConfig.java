package com.brightwebs.jaycee.gameplay.divine;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Provisional server tuning, not natural progression recipes or canonical costs. */
public final class DivinityConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.LongValue EP, ESSENCE, SUPREME, REGEN, MINIMUM, POWER, EXPIRY;
    private static final ModConfigSpec.IntValue PERCENT;
    private static final ModConfigSpec.DoubleValue DISTANCE;
    static {
        var b = new ModConfigSpec.Builder();
        b.comment("Provisional Divine Ascension tuning, not canon.",
            "EP is permanent power, Essence spendable. No automatic earning or NPC offering recipe.",
            "Establishment fee = max(minimum, ceil(recipient EP * percent / 100)), burned IN ADDITION to the transfer.",
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
        SPEC = b.build();
    }
    public static DivinityModel.Config values() {
        return new DivinityModel.Config(EP.get(), ESSENCE.get(), SUPREME.get(), REGEN.get(), MINIMUM.get(),
            PERCENT.get(), POWER.get(), EXPIRY.get(), DISTANCE.get());
    }
    private DivinityConfig() {}
}
