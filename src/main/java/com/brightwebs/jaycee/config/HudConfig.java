package com.brightwebs.jaycee.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only presentation settings; portrait choice belongs to the saved player profile. */
public final class HudConfig {
    public enum Anchor { TOP_LEFT, TOP_RIGHT, MIDDLE_LEFT, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue SHOW_VANILLA_HEARTS;
    public static final ModConfigSpec.EnumValue<Anchor> RESOURCE_ANCHOR;
    public static final ModConfigSpec.EnumValue<Anchor> ABILITY_ANCHOR;
    public static final ModConfigSpec.IntValue RESOURCE_X, RESOURCE_Y, ABILITY_X, ABILITY_Y;
    public static final ModConfigSpec.DoubleValue SCALE;
    public static final ModConfigSpec.ConfigValue<String> ICON_DIRECTORY;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("hud");
        ENABLED = builder.define("enabled", true);
        SHOW_VANILLA_HEARTS = builder.define("showVanillaHearts", false);
        RESOURCE_ANCHOR = builder.defineEnum("resourceAnchor", Anchor.TOP_LEFT);
        ABILITY_ANCHOR = builder.defineEnum("abilityAnchor", Anchor.MIDDLE_RIGHT);
        RESOURCE_X = builder.defineInRange("resourceOffsetX", 8, -4096, 4096);
        RESOURCE_Y = builder.defineInRange("resourceOffsetY", 8, -4096, 4096);
        ABILITY_X = builder.defineInRange("abilityOffsetX", -8, -4096, 4096);
        ABILITY_Y = builder.defineInRange("abilityOffsetY", 0, -4096, 4096);
        SCALE = builder.defineInRange("scale", 1.0, 0.5, 2.0);
        ICON_DIRECTORY = builder.comment("Resource-pack directory; ability IDs receive a .png suffix.")
                .define("iconDirectory", "jaycee:textures/ability",
                        value -> value instanceof String text
                                && text.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                                && !text.contains(".."));
        builder.pop();
        SPEC = builder.build();
    }

    private HudConfig() {}
}
