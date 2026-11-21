package com.brightwebs.brightcore.api;

// what a skill does. same as the category filter in the abilities tab
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
