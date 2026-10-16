package com.brightwebs.jaycee.gameplay;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.minecraft.nbt.CompoundTag;

/** Server-configurable implementation balance, never claimed to be official canon. */
public final class SkillConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_LEVEL, XP_PER_LEVEL, XP_PER_MINUTE;
    public static final ModConfigSpec.IntValue ACTION_XP, CAST_XP, ACTION_INTERVAL, SCAN_INTERVAL, MODE_LEVEL_STEP;
    public static final ModConfigSpec.DoubleValue ACTIVATION, SPEED_DRAIN, FLIGHT_DRAIN, PERCEPTION_DRAIN,
        REGEN_DRAIN, RESISTANCE_DRAIN, SPEED_BONUS, FLIGHT_SPEED, SCAN_RANGE, HEAL, RESISTANCE;
    public static final ModConfigSpec.DoubleValue SPRINT_DISTANCE, MAX_TRAINING_STEP, CRUISE_MULTIPLIER;
    static {
        var b = new ModConfigSpec.Builder();
        b.comment("Implementation balance defaults only; these are not official canon numbers.",
            "XP threshold is xpPerLevel * level squared. No EP, LP, Essence or daily rewards.",
            "Upkeep is mana per second at 100% output; one aggregate payment before each second of effects.",
            "Regeneration has no activation cost and its upkeep is prorated to missing health.",
            "Regeneration restores vanilla health, not SP. Flight styles are Hover and forward Cruise.");
        MAX_LEVEL = b.defineInRange("maxLevel", 10, 1, 100);
        XP_PER_LEVEL = b.defineInRange("xpPerLevel", 100, 1, 1000000);
        XP_PER_MINUTE = b.defineInRange("xpPerMinute", 20, 1, 1000);
        ACTION_XP = b.defineInRange("effectiveActionXp", 1, 1, 100);
        CAST_XP = b.defineInRange("effectiveSoulRestoreXp", 4, 1, 100);
        ACTION_INTERVAL = b.comment("Minimum ticks between movement/combat/heal awards for the same action or target.")
            .defineInRange("actionAwardIntervalTicks", 100, 20, 72000);
        SCAN_INTERVAL = b.comment("Minimum ticks between mastery awards for the same detected target. Movement required.")
            .defineInRange("scanTargetAwardIntervalTicks", 72000, 1200, 1728000);
        MODE_LEVEL_STEP = b.defineInRange("perceptionLivingModeLevel", 3, 1, 100);
        ACTIVATION = b.defineInRange("activationMana", 2.0, 0.1, 1000);
        SPEED_DRAIN = b.defineInRange("speedManaPerSecond", 2.0, 0.1, 1000);
        FLIGHT_DRAIN = b.defineInRange("flightManaPerSecond", 4.0, 0.1, 1000);
        PERCEPTION_DRAIN = b.defineInRange("perceptionManaPerSecond", 2.0, 0.1, 1000);
        REGEN_DRAIN = b.defineInRange("regenerationManaPerSecond", 3.0, 0.1, 1000);
        RESISTANCE_DRAIN = b.defineInRange("resistanceManaPerSecond", 3.0, 0.1, 1000);
        SPEED_BONUS = b.defineInRange("maxMovementSpeedMultiplierBonus", 1.0, 0.01, 3);
        FLIGHT_SPEED = b.defineInRange("maxFlyingSpeed", 0.08, 0.01, 0.2);
        SCAN_RANGE = b.defineInRange("perceptionRangeBlocks", 24.0, 1, 64);
        HEAL = b.defineInRange("regenerationHealthPerSecond", 1.0, 0.1, 10);
        RESISTANCE = b.defineInRange("maxDamageReduction", 0.25, 0.01, 0.5);
        SPRINT_DISTANCE = b.defineInRange("sprintBlocksPerAward", 20.0, 5, 1000);
        MAX_TRAINING_STEP = b.comment("Larger per-tick movements do not train mastery (teleport/external launch rejection).")
            .defineInRange("maxTrainingBlocksPerTick", 1.5, 0.1, 1.5);
        CRUISE_MULTIPLIER = b.defineInRange("cruiseBlocksPerTickPerFlyingSpeed", 5.0, 1, 10);
        SPEC = b.build();
    }
    private SkillConfig() {}
    static void register() {
        ModList.get().getModContainerById("jaycee").orElseThrow().registerConfig(ModConfig.Type.SERVER, SPEC);
    }
    /** Sent in JayceeGameplay snapshots; remote presentation never depends on local config loading. */
    static void snapshot(CompoundTag data) {
        var tuning = new CompoundTag();
        tuning.putDouble("Activation", ACTIVATION.get());
        tuning.putDouble("SpeedDrain", SPEED_DRAIN.get());
        tuning.putDouble("FlightDrain", FLIGHT_DRAIN.get());
        tuning.putDouble("PerceptionDrain", PERCEPTION_DRAIN.get());
        tuning.putDouble("RegenDrain", REGEN_DRAIN.get());
        tuning.putDouble("ResistanceDrain", RESISTANCE_DRAIN.get());
        tuning.putDouble("SpeedBonus", SPEED_BONUS.get());
        tuning.putDouble("FlightSpeed", FLIGHT_SPEED.get());
        tuning.putDouble("Range", SCAN_RANGE.get());
        tuning.putDouble("Heal", HEAL.get());
        tuning.putDouble("Resistance", RESISTANCE.get());
        tuning.putDouble("CruiseMultiplier", CRUISE_MULTIPLIER.get());
        data.put("SkillTuning", tuning);
        data.putInt("SkillMaxLevel", MAX_LEVEL.get());
        data.putInt("SkillXpPerLevel", XP_PER_LEVEL.get());
        data.putInt("SkillModeLevelStep", MODE_LEVEL_STEP.get());
    }
}
