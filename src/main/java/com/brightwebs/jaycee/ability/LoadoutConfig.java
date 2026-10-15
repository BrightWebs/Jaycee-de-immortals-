package com.brightwebs.jaycee.ability;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class LoadoutConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue BETA_HUD;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        BETA_HUD = builder.comment("Expose the beta HUD without essence mastery. Individual ability unlocks still apply.")
            .define("betaUnlockAbilityHud", true);
        SPEC = builder.build();
    }

    private LoadoutConfig() {}
}
