package com.brightwebs.jaycee.gameplay;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Common-side execution rules; only registered, fully specified handlers can spend mana. */
public final class AbilityEngine<C extends AbilityEngine.Context> {
    public interface Context {
        boolean permitted();
        long cooldownExpiry(String abilityId);
        /** Must reject insufficient mana atomically without modifying any resource. */
        boolean consumeMana(double amount);
        void complete(String abilityId, int mode, long cooldownExpiry);
    }

    public enum Result { ACTIVATED, UNKNOWN_ABILITY, INVALID_MODE, RESTRICTED, COOLDOWN, NO_EFFECT, NO_MANA, INVALID_TIME }

    public record Mode<C>(String name, double manaCost, int cooldownTicks,
                           Predicate<C> canApply, Consumer<C> apply) {
        public Mode {
            if (name == null || name.isBlank() || !Double.isFinite(manaCost) || manaCost < 0 || cooldownTicks < 0) {
                throw new IllegalArgumentException("An executable mode needs a name and valid, specified costs");
            }
            Objects.requireNonNull(canApply);
            Objects.requireNonNull(apply);
        }
    }

    public record Definition<C>(String id, List<Mode<C>> modes) {
        public Definition {
            if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                throw new IllegalArgumentException("Invalid ability ID");
            }
            modes = List.copyOf(modes);
            if (modes.isEmpty() || modes.stream().map(Mode::name).distinct().count() != modes.size()) {
                throw new IllegalArgumentException("Executable abilities need distinct, ordered modes");
            }
        }
    }

    private final Map<String, Definition<C>> definitions;

    public AbilityEngine(List<Definition<C>> definitions) {
        Map<String, Definition<C>> registry = new LinkedHashMap<>();
        for (var definition : definitions) {
            if (registry.putIfAbsent(definition.id(), definition) != null) {
                throw new IllegalArgumentException("Duplicate ability ID: " + definition.id());
            }
        }
        this.definitions = Map.copyOf(registry);
    }

    public Result activate(String id, int modeIndex, long now, C context) {
        var definition = definitions.get(id);
        if (definition == null) return Result.UNKNOWN_ABILITY;
        if (modeIndex < 0 || modeIndex >= definition.modes().size()) return Result.INVALID_MODE;
        var mode = definition.modes().get(modeIndex);
        if (now < 0 || now > Long.MAX_VALUE - mode.cooldownTicks()) return Result.INVALID_TIME;
        if (!context.permitted()) return Result.RESTRICTED;
        if (context.cooldownExpiry(id) > now) return Result.COOLDOWN;
        if (!mode.canApply().test(context)) return Result.NO_EFFECT;
        if (!context.consumeMana(mode.manaCost())) return Result.NO_MANA;
        mode.apply().accept(context);
        context.complete(id, modeIndex, now + mode.cooldownTicks());
        return Result.ACTIVATED;
    }
}
