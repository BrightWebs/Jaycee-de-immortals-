package com.brightwebs.jaycee.gameplay;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class AbilityCatalog {
    public record Art(String id, String name, ResourceLocation icon, double manaCost,
                      int cooldownTicks, List<String> modes, boolean implemented) {}

    public static final List<Art> ALL = List.of(
        preview("reality_severance", "Reality Severance"),
        preview("celestial_guard", "Celestial Guard"),
        preview("divine_pulse", "Divine Pulse"),
        preview("temporal_step", "Temporal Step"),
        soulRestore(),
        preview("aether_flight", "Aether Flight")
    );

    private AbilityCatalog() {}

    private static Art soulRestore() {
        var definition = AbilityExecutors.SOUL_RESTORE;
        var primary = definition.modes().getFirst();
        return new Art(definition.id(), "Soul Restore", icon("soul_restore"), primary.manaCost(),
            primary.cooldownTicks(), definition.modes().stream().map(AbilityEngine.Mode::name).toList(), true);
    }

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath("jaycee", "textures/ability/" + name + ".png");
    }

    private static Art preview(String id, String name) {
        return new Art("jaycee:" + id, name, icon(id), 0, 0, List.of("Preview"), false);
    }

    public static Art find(String id) {
        var existing = ALL.stream().filter(art -> art.id().equals(id)).findFirst().orElse(null);
        if (existing != null) return existing;
        var skill = SkillDefinitions.find(id);
        return skill != null && skill.intrinsic() ? intrinsic(skill) : null;
    }
    public static Art find(String id, PlayerState state) {
        var art = find(id);
        if (art == null || !SkillDefinitions.intrinsic(id)) return art;
        return new Art(art.id(), art.name(), art.icon(), state.skillActivationMana(id),
            art.cooldownTicks(), art.modes(), art.implemented());
    }
    /** The approved six-entry Abilities display remains unchanged; Skills uses this combined view. */
    public static List<Art> registered() {
        var entries = new java.util.ArrayList<>(ALL);
        SkillDefinitions.ALL.stream().filter(SkillDefinition::intrinsic).map(AbilityCatalog::intrinsic).forEach(entries::add);
        return List.copyOf(entries);
    }
    public static List<Art> registered(PlayerState state) {
        return registered().stream().map(art -> find(art.id(), state)).toList();
    }
    private static Art intrinsic(SkillDefinition skill) {
        // Registry lookup can run before configs load. Configured presentation uses find(id, state).
        double defaultCost = SkillDefinitions.REGEN.equals(skill.id()) ? 0 : 2;
        return new Art(skill.id(), skill.name(), skill.icon(), defaultCost, 20, skill.modes(), true);
    }
}
