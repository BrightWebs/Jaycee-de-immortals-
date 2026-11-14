package com.brightwebs.jaycee.ichor;

import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

/** The card a player is dealt on first join. Odds from the Ichor: Foundation doc. */
public enum Birthright implements StringRepresentable {
    UNDRAWN("undrawn", "Undrawn", 0, 0, 0),
    MORTAL_BORN("mortal_born", "Mortal-born", 950, 3, 8),
    ORACLE_BORN("oracle_born", "Oracle-born", 45, 60, 120),
    GODBLOOD("godblood", "Godblood", 5, 220, 300);

    public static final Codec<Birthright> CODEC = StringRepresentable.fromEnum(Birthright::values);

    private final String id, label;
    /** Out of 1000. */
    private final int weight;
    private final int minIchor, maxIchor;

    Birthright(String id, String label, int weight, int minIchor, int maxIchor) {
        this.id = id;
        this.label = label;
        this.weight = weight;
        this.minIchor = minIchor;
        this.maxIchor = maxIchor;
    }

    @Override public String getSerializedName() { return id; }
    public String label() { return label; }

    public int rollIchor(RandomSource random) {
        return minIchor + random.nextInt(maxIchor - minIchor + 1);
    }

    /** 95% Mortal-born, 4.5% Oracle-born, 0.5% Godblood. */
    public static Birthright draw(RandomSource random) {
        int roll = random.nextInt(1000);
        for (Birthright b : values()) {
            if (b == UNDRAWN) continue;
            if (roll < b.weight) return b;
            roll -= b.weight;
        }
        return MORTAL_BORN;
    }
}
