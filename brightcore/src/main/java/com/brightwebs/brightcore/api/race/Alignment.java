package com.brightwebs.brightcore.api.race;

/**
 * A race's nature. Sets how much of a slain being's power it takes in (its soul share),
 * and later, how holy, demonic and undead damage treat it.
 */
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
    /** Multiplies the EP taken from kills. Darker natures feed harder. */
    public float gainMultiplier() { return gainMultiplier; }
    public int color() { return color; }
}
