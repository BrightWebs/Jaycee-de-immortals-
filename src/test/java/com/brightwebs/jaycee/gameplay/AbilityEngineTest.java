package com.brightwebs.jaycee.gameplay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Dependency-free regression checks, also runnable directly with the Java 21 compiler/runtime. */
public final class AbilityEngineTest {
    private static final String ID = "jaycee:soul_restore";
    private static int assertions;

    private static final class State implements AbilityEngine.Context {
        double mana = 100, soul = 40, barrier;
        boolean allowed = true, acceptsPayment = true;
        int charges, effects, completions, lastMode = -1;
        Map<String, Long> cooldowns = new HashMap<>();
        public boolean permitted() { return allowed && soul > 0; }
        public long cooldownExpiry(String id) { return cooldowns.getOrDefault(id, 0L); }
        public boolean consumeMana(double amount) {
            charges++;
            if (!acceptsPayment || mana < amount) return false;
            mana -= amount;
            return true;
        }
        public void complete(String id, int mode, long expiry) {
            completions++;
            lastMode = mode;
            cooldowns.put(id, expiry);
        }
    }

    private static AbilityEngine.Definition<State> definition() {
        return new AbilityEngine.Definition<>(ID, List.of(
            new AbilityEngine.Mode<>("Restore", 20, 200, s -> s.soul < 100,
                s -> { s.soul = Math.min(100, s.soul + 30); s.effects++; }),
            new AbilityEngine.Mode<>("Fortify", 20, 200, s -> s.barrier < 100,
                s -> { s.barrier = Math.min(100, s.barrier + 30); s.effects++; })
        ));
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static void rejected(Runnable operation) {
        assertions++;
        try { operation.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid definition accepted");
    }

    private static void noMutation(AbilityEngine<State> engine, String id, int mode, long now,
                                   State state, AbilityEngine.Result expected) {
        double mana = state.mana, soul = state.soul, barrier = state.barrier;
        check(engine.activate(id, mode, now, state) == expected, "Rejection reason: " + expected);
        check(state.mana == mana && state.soul == soul && state.barrier == barrier
            && state.effects == 0 && state.completions == 0 && state.cooldowns.isEmpty(), "Rejected cast changed state");
    }

    public static void main(String[] args) {
        var engine = new AbilityEngine<>(List.of(definition()));
        var state = new State();
        check(engine.activate(ID, 0, 1000, state) == AbilityEngine.Result.ACTIVATED, "Restore activates");
        check(state.mana == 80 && state.soul == 70 && state.barrier == 0, "Restore uses mana and heals only SP");
        check(state.charges == 1 && state.effects == 1 && state.completions == 1, "Exactly one payment/effect/commit");
        check(state.lastMode == 0 && state.cooldownExpiry(ID) == 1200, "Restore records mode and 200-tick expiry");
        check(engine.activate(ID, 1, 1199, state) == AbilityEngine.Result.COOLDOWN, "Modes share cooldown");
        check(state.mana == 80 && state.barrier == 0 && state.charges == 1, "Cooldown charges nothing");
        check(engine.activate(ID, 1, 1200, state) == AbilityEngine.Result.ACTIVATED, "Exact expiry is ready");
        check(state.mana == 60 && state.soul == 70 && state.barrier == 30 && state.lastMode == 1, "Fortify changes only barrier");

        var otherSlot = new State();
        otherSlot.cooldowns = state.cooldowns;
        check(engine.activate(ID, 0, 1201, otherSlot) == AbilityEngine.Result.COOLDOWN, "Cooldown key is ability, not slot/page");
        check(otherSlot.charges == 0 && otherSlot.mana == 100, "Duplicate copy cannot pay or bypass cooldown");

        state = new State();
        state.soul = 95;
        check(engine.activate(ID, 0, 1, state) == AbilityEngine.Result.ACTIVATED && state.soul == 100, "Restore caps to maximum");
        state = new State();
        state.barrier = 95;
        check(engine.activate(ID, 1, 1, state) == AbilityEngine.Result.ACTIVATED && state.barrier == 100, "Barrier caps to maximum");

        noMutation(engine, "jaycee:preview", 0, 1000, new State(), AbilityEngine.Result.UNKNOWN_ABILITY);
        noMutation(engine, ID, -1, 1000, new State(), AbilityEngine.Result.INVALID_MODE);
        noMutation(engine, ID, 2, 1000, new State(), AbilityEngine.Result.INVALID_MODE);
        noMutation(engine, ID, 0, -1, new State(), AbilityEngine.Result.INVALID_TIME);
        noMutation(engine, ID, 0, Long.MAX_VALUE, new State(), AbilityEngine.Result.INVALID_TIME);
        state = new State();
        state.allowed = false;
        noMutation(engine, ID, 0, 1000, state, AbilityEngine.Result.RESTRICTED);
        check(state.charges == 0, "No unlock/permission means no charge");
        state = new State();
        state.soul = 0;
        noMutation(engine, ID, 1, 1000, state, AbilityEngine.Result.RESTRICTED);
        state = new State();
        state.soul = 100;
        noMutation(engine, ID, 0, 1000, state, AbilityEngine.Result.NO_EFFECT);
        check(state.charges == 0, "Full SP means no charge");
        state = new State();
        state.barrier = 100;
        noMutation(engine, ID, 1, 1000, state, AbilityEngine.Result.NO_EFFECT);
        check(state.charges == 0, "Full barrier means no charge");
        state = new State();
        state.mana = 19;
        noMutation(engine, ID, 0, 1000, state, AbilityEngine.Result.NO_MANA);
        state = new State();
        state.acceptsPayment = false;
        noMutation(engine, ID, 0, 1000, state, AbilityEngine.Result.NO_MANA);
        check(state.charges == 1, "Backend rejection attempted payment exactly once");

        var immutable = definition();
        rejected(() -> new AbilityEngine<>(List.of(immutable, immutable)));
        rejected(() -> new AbilityEngine.Definition<State>("missing_namespace", immutable.modes()));
        rejected(() -> new AbilityEngine.Definition<State>(ID, List.of()));
        rejected(() -> new AbilityEngine.Definition<>(ID, List.of(immutable.modes().getFirst(), immutable.modes().getFirst())));
        rejected(() -> new AbilityEngine.Mode<State>("", 20, 200, s -> true, s -> {}));
        for (double cost : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1}) {
            rejected(() -> new AbilityEngine.Mode<State>("Invalid", cost, 200, s -> true, s -> {}));
        }
        rejected(() -> new AbilityEngine.Mode<State>("Invalid", 20, -1, s -> true, s -> {}));
        check(immutable.modes().stream().map(AbilityEngine.Mode::name).toList().equals(List.of("Restore", "Fortify")),
            "Mode order remains stable for saved indices");
        System.out.println("AbilityEngineTest: " + assertions + " assertions passed");
    }
}
