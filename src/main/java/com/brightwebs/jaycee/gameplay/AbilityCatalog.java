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
        return ALL.stream().filter(art -> art.id().equals(id)).findFirst().orElse(null);
    }
}
