package com.brightwebs.jaycee.gameplay;

import java.util.LinkedHashMap;
import java.util.Map;

/** Dependency-free server rules. All numbers are balance defaults, not canon requirements. */
public final class SkillMath {
    private SkillMath() {}
    /** Immutable paid parameters; requested configuration cannot strengthen an existing lease. */
    public record Lease(int output, String style, double drain) {
        public static final Lease NONE = new Lease(0, "", 0);
        public Lease {
            if (output < 0 || output > 100 || style == null || !Double.isFinite(drain) || drain < 0)
                throw new IllegalArgumentException("Invalid paid skill parameters");
        }
    }

    public static long capXp(long xp, int maxLevel, int xpPerLevel) {
        return Math.clamp(xp, 0, (long) maxLevel * maxLevel * xpPerLevel);
    }

    public static long addXp(long xp, long earned, int maxLevel, int xpPerLevel) {
        long cap = (long) maxLevel * maxLevel * xpPerLevel;
        long current = capXp(xp, maxLevel, xpPerLevel);
        return current + Math.min(Math.max(0, earned), cap - current);
    }

    public static int level(long xp, int maxLevel, int xpPerLevel) {
        return Math.min(maxLevel, (int) Math.sqrt(capXp(xp, maxLevel, xpPerLevel) / (double) xpPerLevel));
    }

    public static boolean modeUnlocked(int index, int count, int level) {
        return modeUnlocked(index, count, level, 3);
    }
    public static boolean permission(boolean intrinsic, boolean granted) { return !intrinsic || granted; }
    public static boolean validMovement(double distance, double maximum) {
        return Double.isFinite(distance) && Double.isFinite(maximum)
            && distance > 0.05 && distance <= Math.min(1.5, maximum);
    }
    public static boolean effectActive(boolean permitted, boolean enabled, int output, boolean paid) {
        return permitted && enabled && output > 0 && output <= 100 && paid;
    }
    public static boolean configurationStops(boolean enabled, int output) {
        return !enabled || output <= 0 || output > 100;
    }
    public static String iconPath(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Invalid skill ID");
        return "textures/ability/" + id.substring(id.indexOf(':') + 1) + ".png";
    }
    public static boolean modeUnlocked(int index, int count, int level, int levelStep) {
        return index >= 0 && index < count && levelStep > 0 && level >= (long) index * levelStep;
    }
    public static int nextMode(int current, int count, int level, int levelStep) {
        if (count <= 0) return 0;
        int next = Math.floorMod(current, count);
        for (int i = 0; i < count; i++) {
            next = (next + 1) % count;
            if (levelStep == 0 || modeUnlocked(next, count, level, levelStep)) return next;
        }
        return 0;
    }

    public static double output(int percentage) { return Math.clamp(percentage, 0, 100) / 100.0; }

    public static double drain(double base, int output, boolean effective) {
        return effective && Double.isFinite(base) && base > 0 ? base * output(output) : 0;
    }
    public static double healingDrain(double base, int output, double missing, double healAtFull) {
        if (!Double.isFinite(missing) || !Double.isFinite(healAtFull) || missing <= 0 || healAtFull <= 0) return 0;
        return drain(base, output, true) * Math.min(1, missing / (healAtFull * Math.max(0.0001, output(output))));
    }

    /** One bounded gate per player; changing targets cannot bypass the shared minute budget. */
    public static final class Gate {
        private final Map<String, Long> recent = new LinkedHashMap<>();
        private long window = Long.MIN_VALUE;
        private int awarded;

        public Gate() {}
        public Gate(long window, int awarded, Map<String, Long> entries) {
            this.window = window;
            this.awarded = Math.max(0, awarded);
            entries.entrySet().stream().limit(128).forEach(e -> recent.put(e.getKey(), e.getValue()));
        }
        public long window() { return window; }
        public int awarded() { return awarded; }
        public Map<String, Long> entries() { return Map.copyOf(recent); }

        public boolean allow(String action, long tick, int cooldown, int xp, int minuteBudget) {
            if (tick < 0 || xp <= 0 || xp > minuteBudget) return false;
            long bucket = tick / 1200;
            if (window != bucket) { window = bucket; awarded = 0; }
            Long previous = recent.get(action);
            if (previous != null && (tick < previous || tick - previous < cooldown)) return false;
            if (awarded > minuteBudget - xp) return false;
            recent.put(action, tick);
            if (recent.size() > 128) recent.remove(recent.keySet().iterator().next());
            awarded += xp;
            return true;
        }
    }
}
