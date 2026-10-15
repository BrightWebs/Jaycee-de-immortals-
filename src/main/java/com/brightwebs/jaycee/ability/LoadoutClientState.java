package com.brightwebs.jaycee.ability;

import java.util.Collections;
import java.util.List;

/** Client-readable immutable state; deliberately has no client-only class dependencies. */
public final class LoadoutClientState {
    public record Snapshot(int page, List<Integer> abilities, List<Integer> modes, List<Integer> cooldowns,
                           List<Integer> activeTicks, int mana, boolean accessible, List<Boolean> unlocked) {
        public Snapshot {
            abilities = List.copyOf(abilities);
            modes = List.copyOf(modes);
            cooldowns = List.copyOf(cooldowns);
            activeTicks = List.copyOf(activeTicks);
            unlocked = List.copyOf(unlocked);
        }
    }

    private static volatile Snapshot snapshot = empty();

    public static Snapshot get() { return snapshot; }
    public static void clear() { snapshot = empty(); }
    public static void reset() { clear(); }
    static void accept(Snapshot value) { snapshot = value; }

    private static Snapshot empty() {
        return new Snapshot(0, Collections.nCopies(40, -1), Collections.nCopies(40, 0),
            Collections.nCopies(40, 0), Collections.nCopies(40, 0), 0, false, Collections.nCopies(9, false));
    }

    private LoadoutClientState() {}
}
