package com.brightwebs.brightcore.api;

/** What an ability does. Matches the category filter in Jaycee's Abilities tab. */
public enum AbilityCategory {
    OFFENSE("Offense"),
    DEFENSE("Defense"),
    SUPPORT("Support"),
    MOVEMENT("Movement"),
    REALITY("Reality");

    private final String label;

    AbilityCategory(String label) { this.label = label; }

    public String label() { return label; }
}
