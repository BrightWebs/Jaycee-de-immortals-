package com.brightwebs.jaycee.gameplay;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Public UI metadata, backed by the same definitions used by server execution. */
public record SkillDefinition(String id, String name, String category, String description,
                              boolean intrinsic, List<String> modes, List<String> styles) {
    public List<String> categories() {
        String secondary = switch (id) {
            case SkillDefinitions.SPEED, SkillDefinitions.FLIGHT -> "Movement";
            case SkillDefinitions.PERCEPTION -> "Perception";
            case SkillDefinitions.RESISTANCE -> "Resistances";
            default -> "";
        };
        return secondary.isEmpty() ? List.of(category) : List.of(category, secondary);
    }
    public ResourceLocation icon() {
        return ResourceLocation.fromNamespaceAndPath("jaycee", SkillMath.iconPath(id));
    }
    public int minimumModeLevel(int mode) {
        return SkillDefinitions.PERCEPTION.equals(id) ? mode * SkillConfig.MODE_LEVEL_STEP.get() : 0;
    }
    public double activationMana() {
        if (SkillDefinitions.REGEN.equals(id)) return 0; // Only effective healing has a cost.
        return intrinsic ? SkillConfig.ACTIVATION.get() : SkillDefinitions.RESTORE.equals(id) ? 20 : 0;
    }
    public double upkeepMana() { return SkillDefinitions.upkeep(id); }
    public String effectSummary() { return SkillDefinitions.effectSummary(id); }
    /** UI overloads read the server snapshot, including on remote clients and before config loading. */
    public double activationMana(PlayerState state) { return state.skillActivationMana(id); }
    public double upkeepMana(PlayerState state) { return state.skillUpkeepMana(id); }
    public String effectSummary(PlayerState state) { return state.skillEffectSummary(id); }
    public int minimumModeLevel(PlayerState state, int mode) {
        return SkillDefinitions.PERCEPTION.equals(id) ? mode * state.skillModeLevelStep() : 0;
    }
}
