package com.brightwebs.brightcore.api;

// skill tier like tensura. decides how much mastery it needs and how it's sorted and colored in menus
public enum SkillType {
    INTRINSIC("Intrinsic", 100, 0xFF9FD8A6),
    COMMON("Common", 100, 0xFFB5D9F4),
    EXTRA("Extra", 500, 0xFF75C6FF),
    UNIQUE("Unique", 1_000, 0xFFF3CF82),
    ULTIMATE("Ultimate", 10_000, 0xFFE07BFF),
    RESISTANCE("Resistance", 100, 0xFFC2B59B),
    NULLIFICATION("Nullification", 100, 0xFFE9E1C8),
    MAGIC("Magic", 1_000, 0xFF8FB8FF);

    private final String label;
    private final int maxMastery;
    private final int color;

    SkillType(String label, int maxMastery, int color) {
        this.label = label;
        this.maxMastery = maxMastery;
        this.color = color;
    }

    public String label() { return label; }
    // mastery points to master a skill of this tier
    public int maxMastery() { return maxMastery; }
    public int color() { return color; }
}
