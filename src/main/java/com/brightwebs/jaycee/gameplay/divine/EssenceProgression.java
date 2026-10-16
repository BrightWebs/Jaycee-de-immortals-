package com.brightwebs.jaycee.gameplay.divine;

import java.math.BigInteger;
import java.util.List;

/** Pure Essence arithmetic. Level one starts at zero; advancing L costs coefficient * (L + 1)^2. */
public final class EssenceProgression {
    private static final BigInteger MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private static final String[] GRADES = {"F", "E", "D", "C", "B", "A", "S", "SS", "SSS"};

    public record Tuning(long coefficient, List<Integer> gradeMinimumLevels) {
        public Tuning {
            gradeMinimumLevels = List.copyOf(gradeMinimumLevels);
            if (coefficient < 1 || gradeMinimumLevels.size() != GRADES.length || gradeMinimumLevels.getFirst() != 1)
                throw new IllegalArgumentException("Positive progression coefficient and nine grade thresholds starting at one required");
            for (int i = 1; i < gradeMinimumLevels.size(); i++)
                if (gradeMinimumLevels.get(i) <= gradeMinimumLevels.get(i - 1))
                    throw new IllegalArgumentException("Grade thresholds must strictly increase");
        }
        public static Tuning defaults() {
            return new Tuning(100, List.of(1, 11, 21, 36, 51, 66, 81, 96, 111));
        }
    }

    /**
     * earned is lifetime qualifying Essence, independent of EP and spendable balances.
     * intoLevel is earned minus the current level threshold. nextCost is saturated only when
     * nextCostCapped is true. policy is not a divine rank or player ability tree.
     */
    public record Progress(int level, String grade, long earned, long intoLevel, long nextCost,
                           boolean nextCostCapped, boolean supported, String policy) {}

    /**
     * earned and ep are actual stored fields. Nullable level, grade, and evolutionEligible mean
     * no reviewed species policy supplies that information, not level zero or ineligibility.
     * supported refers to numeric species progression/evolution, not the ability to receive gifts.
     */
    public record MobProgress(long earned, long ep, Integer level, String grade, Boolean evolutionEligible,
                              boolean supported, String policy, String unavailableReason) {}

    public static Progress player(long earned, boolean sssAllowed, Tuning tuning) {
        if (earned < 0) throw new IllegalArgumentException("Negative earned Essence");
        int low = 1, high = Integer.MAX_VALUE;
        while (low < high) {
            int middle = low + (int)(((long)high - low + 1) / 2);
            if (threshold(middle, tuning).compareTo(BigInteger.valueOf(earned)) <= 0) low = middle;
            else high = middle - 1;
        }
        int grade = 0;
        for (int i = 1; i < GRADES.length; i++)
            if (low >= tuning.gradeMinimumLevels().get(i)) grade = i;
        if (!sssAllowed) grade = Math.min(grade, 7);
        BigInteger next = cost(low, tuning);
        return new Progress(low, GRADES[grade], earned, earned - threshold(low, tuning).longValueExact(),
            next.min(MAX).longValueExact(), next.compareTo(MAX) > 0, true, "player_quadratic");
    }

    /** Without reviewed species recipes, never derive EP, grades, evolution, or a player tree from gifts. */
    public static MobProgress mob(String species, long earned, long ep) {
        if (earned < 0 || ep < 0) throw new IllegalArgumentException("Negative mob progression");
        return new MobProgress(earned, ep, null, null, null, false, "species_received_only:" + species,
            "No reviewed numeric progression or evolution policy is registered for " + species);
    }

    public static long earnedForLevel(int level, Tuning tuning) {
        return threshold(level, tuning).longValueExact();
    }

    private static BigInteger cost(int level, Tuning tuning) {
        return BigInteger.valueOf((long)level + 1).pow(2).multiply(BigInteger.valueOf(tuning.coefficient()));
    }

    private static BigInteger threshold(int level, Tuning tuning) {
        if (level < 1) throw new IllegalArgumentException("Level must be at least one");
        BigInteger n = BigInteger.valueOf(level);
        return n.multiply(n.add(BigInteger.ONE)).multiply(n.multiply(BigInteger.TWO).add(BigInteger.ONE))
            .divide(BigInteger.valueOf(6)).subtract(BigInteger.ONE).multiply(BigInteger.valueOf(tuning.coefficient()));
    }

    private EssenceProgression() {}
}
