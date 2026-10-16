package com.brightwebs.jaycee.gameplay;

import java.util.List;

public final class SkillDefinitions {
    public static final String SPEED = "jaycee:divine_speed", FLIGHT = "jaycee:divine_flight",
        PERCEPTION = "jaycee:divine_perception", REGEN = "jaycee:regeneration",
        RESISTANCE = "jaycee:divine_resistances", PHYSICAL = "jaycee:physical_training",
        RESTORE = "jaycee:soul_restore";
    public static final List<SkillDefinition> ALL = List.of(
        new SkillDefinition(SPEED, "Divine Speed", "Divine Mastery", "Movement acceleration only; never time manipulation.", true, List.of("Movement"), List.of()),
        new SkillDefinition(FLIGHT, "Divine Flight", "Divine Mastery", "Survival flight; zero output disables flight.", true, List.of("Flight"), List.of("Hover", "Cruise")),
        new SkillDefinition(PERCEPTION, "Divine Perception", "Divine Mastery", "Line-of-sight nonplayer detection; no player stats or global glow.", true, List.of("Hostiles", "Living"), List.of()),
        new SkillDefinition(REGEN, "Regeneration", "Divine Mastery", "Restores missing vanilla health. Arming is free; healing requires paid upkeep. No drain at full health.", true, List.of("Health"), List.of()),
        new SkillDefinition(RESISTANCE, "Divine Resistances", "Divine Mastery", "Paid damage reduction composed with existing spiritual resistance.", true, List.of("Guard"), List.of()),
        new SkillDefinition(PHYSICAL, "Physical Training", "Physical Mastery", "Measured sprint movement and effective hostile combat.", false, List.of(), List.of()),
        new SkillDefinition(RESTORE, "Soul Restore", "Magical Mastery", "Mastery from effective Restore/Fortify casts, excluding recent SP test commands.", false, List.of("Restore", "Fortify"), List.of()));
    private SkillDefinitions() {}
    public static SkillDefinition find(String id) { return ALL.stream().filter(d -> d.id().equals(id)).findFirst().orElse(null); }
    public static boolean intrinsic(String id) { var d = find(id); return d != null && d.intrinsic(); }
    public static double upkeep(String id) {
        return switch (id) {
            case SPEED -> SkillConfig.SPEED_DRAIN.get();
            case FLIGHT -> SkillConfig.FLIGHT_DRAIN.get();
            case PERCEPTION -> SkillConfig.PERCEPTION_DRAIN.get();
            case REGEN -> SkillConfig.REGEN_DRAIN.get();
            case RESISTANCE -> SkillConfig.RESISTANCE_DRAIN.get();
            default -> 0;
        };
    }
    public static String effectSummary(String id) {
        return switch (id) {
            case SPEED -> "Up to +" + Math.round(SkillConfig.SPEED_BONUS.get() * 100) + "% movement speed";
            case FLIGHT -> "Flying speed up to " + SkillConfig.FLIGHT_SPEED.get() + "; Hover / forward Cruise";
            case PERCEPTION -> "Visible targets within " + SkillConfig.SCAN_RANGE.get() + " blocks; Living mode at level " + SkillConfig.MODE_LEVEL_STEP.get();
            case REGEN -> "Up to " + SkillConfig.HEAL.get() + " missing health / second";
            case RESISTANCE -> "Up to " + Math.round(SkillConfig.RESISTANCE.get() * 100) + "% damage reduction";
            case RESTORE -> "20 mana, 200 ticks; +30 SP or barrier";
            default -> "Sprint distance / effective hostile hits";
        };
    }
}
