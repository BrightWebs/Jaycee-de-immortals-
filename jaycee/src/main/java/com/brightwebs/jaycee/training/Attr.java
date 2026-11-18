package com.brightwebs.jaycee.training;

/** The trainable attributes, in Stats-menu order. Names, descriptions and sources live in the lang file. */
public enum Attr {
    PHYSICAL_STRENGTH, SPEED, AGILITY, ENDURANCE, VITALITY, LUNG_CAPACITY, SWIMMING_SPEED, PERCEPTION,
    TARGETING, ARCHERY, COMBAT_MASTERY, MAGIC_CONTROL, AURA_CONTROL, MENTAL_FORTITUDE, REGENERATION;

    public String key() { return name().toLowerCase(); }

    public static Attr byKey(String k) {
        for (Attr a : values()) if (a.key().equals(k)) return a;
        return null;
    }
}
