package com.brightwebs.jaycee.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Presentation identities only. A palette never grants a domain or ability. */
final class PatronTheme {
    record Theme(String id, String name, int accent, int secondary) {}
    private static final Map<String, Theme> THEMES = new LinkedHashMap<>();
    static final Theme NEUTRAL = new Theme("unblessed", "Unblessed", 0xFF91C4F4, 0xFFAD91DC);
    static {
        add("zeus", "Zeus", 0xFFF5D56A, 0xFFFFEFBD);
        add("poseidon", "Poseidon", 0xFF56CFE7, 0xFF418CD6);
        add("hades", "Hades", 0xFFB28ADE, 0xFF73648F);
        add("athena", "Athena", 0xFFD3DCE3, 0xFFE3D5A0);
        add("ares", "Ares", 0xFFE4777E, 0xFFA94E63);
        add("apollo", "Apollo", 0xFFFFDD87, 0xFFFFF1CF);
        add("artemis", "Artemis", 0xFFD1DFE9, 0xFF8CBD9B);
        add("hermes", "Hermes", 0xFFE9C77B, 0xFF70C7BD);
        add("hephaestus", "Hephaestus", 0xFFFFAA67, 0xFFB2BCC3);
        add("aphrodite", "Aphrodite", 0xFFEFADCF, 0xFFF5E3E8);
        add("demeter", "Demeter", 0xFFA1CC82, 0xFFE2C879);
        add("dionysus", "Dionysus", 0xFFC290D5, 0xFF93B983);
        add("hera", "Hera", 0xFFC49BE3, 0xFF73B9CF);
        add("hestia", "Hestia", 0xFFFFA776, 0xFFF5D38B);
        add("hecate", "Hecate", 0xFFBD9CE9, 0xFFCFD6E0);
        add("eris", "Eris", 0xFFCCB071, 0xFFD37987);
        add("nemesis", "Nemesis", 0xFFB7C2CD, 0xFFBD8196);
        add("kronos", "Kronos", 0xFFD1AA76, 0xFFAC8CCB);
        add("gaia", "Gaia", 0xFF7FD2A0, 0xFFD4BD7F);
        add("ouranos", "Ouranos", 0xFF8CBFF2, 0xFFE0EBF7);
        add("nyx", "Nyx", 0xFFB49BD9, 0xFF8994DB);
        add("chaos", "Chaos", 0xFFCE8DDA, 0xFF8778BE);
    }
    private static void add(String id, String name, int accent, int secondary) {
        THEMES.put(id, new Theme(id, name, accent, secondary));
    }
    static Theme find(String id) { return THEMES.getOrDefault(id, NEUTRAL); }
    static List<Theme> retained(List<String> ids) {
        return ids.stream().distinct().sorted().filter(THEMES::containsKey).map(THEMES::get).toList();
    }
    static Theme palette(List<Theme> patrons) { return patrons.size() == 1 ? patrons.getFirst() : NEUTRAL; }
    static boolean relevant(Theme patron, String id, String name) {
        List<String> domains = switch (patron.id()) {
            case "zeus" -> List.of("lightning", "storm");
            case "poseidon" -> List.of("water", "sea");
            case "hades" -> List.of("underworld", "shadow");
            case "gaia" -> List.of("earth", "nature");
            case "apollo" -> List.of("light", "solar");
            case "hecate" -> List.of("arcane", "mystical");
            default -> List.of();
        };
        return java.util.Arrays.stream((id + " " + name).toLowerCase(java.util.Locale.ROOT).split("[^a-z]+"))
            .anyMatch(domains::contains);
    }
    static double angle(int index, int count, double ticks, boolean reducedMotion) {
        return -Math.PI / 2 + index * Math.PI * 2 / Math.max(1, count) +
            (reducedMotion ? 0 : ticks * Math.PI * 2 / 640);
    }
    private PatronTheme() {}
}
