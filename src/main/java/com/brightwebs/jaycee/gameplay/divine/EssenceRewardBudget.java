package com.brightwebs.jaycee.gameplay.divine;

/** Persisted tick-window budget; neither reconnects nor dimension clocks reset awards. */
public final class EssenceRewardBudget {
    public record State(long start, long used) {}
    public record Award(State state, long amount) {}

    private EssenceRewardBudget() {}

    public static Award claim(State previous, long tick, long requested, long maximum, long period) {
        if (tick < 0 || requested < 0 || maximum < 0 || period < 1)
            throw new IllegalArgumentException("Invalid reward budget");
        long start = Math.max(0, previous.start());
        long used = Math.max(0, previous.used());
        if (tick >= start && tick - start >= period) { start = tick; used = 0; }
        long amount = Math.min(requested, Math.max(0, maximum - Math.min(maximum, used)));
        return new Award(new State(start, Math.addExact(Math.min(maximum, used), amount)), amount);
    }
}
