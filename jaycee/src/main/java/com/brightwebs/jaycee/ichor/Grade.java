package com.brightwebs.jaycee.ichor;

/** The nine Grades of Divinity, set by Ichor. Thresholds from the Ichor: Foundation doc. */
public enum Grade {
    MORTAL("Mortal", 0),
    TOUCHED("Touched", 10),
    SEER("Seer", 50),
    HERO("Hero", 200),
    DEMIGOD("Demigod", 1_000),
    ASCENDANT("Ascendant", 5_000),
    LESSER_GOD("Lesser God", 25_000),
    GREATER_GOD("Greater God", 1_000_000),
    PRIMORDIAL("Primordial", 1_000_000_000);

    /** The hard cap for any being. Fits a Java int (max 2,147,483,647). */
    public static final int MAX_ICHOR = 2_000_000_000;
    /** At 50 the Mist thins enough to see through. */
    public static final int SIGHT_THRESHOLD = 50;
    /** At 200 the Mist no longer shields you: hidden beings can touch you and you them. */
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

    /** The next Grade up, or null at Primordial. */
    public Grade next() {
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
    }
}
