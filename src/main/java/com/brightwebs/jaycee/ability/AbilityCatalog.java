package com.brightwebs.jaycee.ability;

import java.util.List;

/** Stable IDs shared by the server and presentation layer. */
public final class AbilityCatalog {
    public record Ability(int id, String name, String category, String description, String effects,
                          int mana, int cooldownTicks, int range, List<String> modes, int requiredEssences) {}

    public static final List<Ability> ALL = List.of(
        new Ability(0, "Reality Severance", "Reality", "Cleave the veil with a focused strike.", "12 magic damage along a sight ray.", 40, 160, 24, List.of("Focused", "Measured"), 3),
        new Ability(1, "Celestial Guard", "Defense", "Conjure a radiant barrier.", "Resistance for 5 seconds.", 25, 200, 0, List.of("Guard", "Enduring"), 0),
        new Ability(2, "Divine Pulse", "Offense", "Release a wave of divine energy.", "6 magic damage to nearby hostile creatures.", 30, 100, 6, List.of("Burst", "Measured"), 0),
        new Ability(3, "Temporal Step", "Movement", "Step beyond the flow of time.", "Collision-checked short teleport.", 20, 80, 8, List.of("Forward", "Short"), 1),
        new Ability(4, "Soul Restore", "Support", "Restore your spirit.", "Restore up to 6 health.", 35, 240, 0, List.of("Restore", "Gentle"), 1),
        new Ability(5, "Aether Flight", "Movement", "Ride the currents of aether.", "Slow falling for 10 seconds; no creative flight.", 15, 240, 0, List.of("Glide", "Drift"), 2),
        new Ability(6, "Solar Lance", "Offense", "Focus sunlight into a lance.", "8 magic damage along a sight ray.", 28, 120, 20, List.of("Lance", "Measured"), 2),
        new Ability(7, "Astral Ward", "Defense", "Wrap yourself in a mantle of stars.", "Absorption for 10 seconds.", 22, 300, 0, List.of("Ward", "Enduring"), 2),
        new Ability(8, "Blessing", "Support", "Bolster a companion with celestial resolve.", "Regeneration for you and nearby allied players.", 18, 240, 6, List.of("Grace", "Enduring"), 3)
    );

    public static Ability get(int id) {
        return id >= 0 && id < ALL.size() ? ALL.get(id) : null;
    }

    private AbilityCatalog() {}
}
