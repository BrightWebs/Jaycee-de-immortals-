package com.brightwebs.jaycee.ichor;

// the nine grades of divinity, set by ichor. thresholds from the ichor foundation doc
public enum Grade {
    MORTAL("Mortal", 0),
    TOUCHED("Touched", 10),
    SEER("Seer", 50),
    INITIATE("Initiate", 200),
    DEMIGOD("Demigod", 1_000),
    ASCENDANT("Ascendant", 5_000),
    LESSER_GOD("Lesser God", 25_000),
    GREATER_GOD("Greater God", 1_000_000),
    PRIMORDIAL("Primordial", 1_000_000_000);

    // hard cap for anything. fits in a java int (max 2,147,483,647)
    public static final int MAX_ICHOR = 2_000_000_000;
    // at 50 the mist is thin enough to see through
    public static final int SIGHT_THRESHOLD = 50;
    // at 200 the mist stops protecting you: hidden beings can touch you and you can touch them
    public static final int TOUCH_THRESHOLD = 200;

    private final String label;
    private final int minIchor;

    Grade(String label, int minIchor) {
        this.label = label;
        this.minIchor = minIchor;
    }

    public String label() { return label; }
    public int minIchor() { return minIchor; }
    public int number() { return ordinal(); }

    public static Grade of(int ichor) {
        Grade result = MORTAL;
        for (Grade g : values()) if (ichor >= g.minIchor) result = g;
        return result;
    }

    // next grade up, or null at primordial
    public Grade next() {
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
    }
}
