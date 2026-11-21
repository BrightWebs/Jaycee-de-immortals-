package com.brightwebs.brightcore.api.race;

// what a race is (holy, demonic etc). changes how much power it takes from kills,
// and later how holy/demonic/undead damage hits it
public enum Alignment {
    HOLY("Holy", 0.8f, 0xFFF3CF82),
    NEUTRAL("Neutral", 1.0f, 0xFFB5D9F4),
    DEMONIC("Demonic", 1.3f, 0xFFE04B5A),
    UNDEAD("Undead", 1.4f, 0xFF9A8CC2),
    CHAOS("Chaos", 1.2f, 0xFFB46CFF);

    private final String label;
    private final float gainMultiplier;
    private final int color;

    Alignment(String label, float gainMultiplier, int color) {
        this.label = label;
        this.gainMultiplier = gainMultiplier;
        this.color = color;
    }

    public String label() { return label; }
    // multiplies EP from kills, darker races take more
    public float gainMultiplier() { return gainMultiplier; }
    public int color() { return color; }
}
