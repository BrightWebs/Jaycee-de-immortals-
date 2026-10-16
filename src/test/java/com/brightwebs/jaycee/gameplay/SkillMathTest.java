package com.brightwebs.jaycee.gameplay;

import java.util.List;

/** Dependency-free regression suite for the production rules and shared casting engine. */
public final class SkillMathTest {
    private static int assertions;
    private static void check(boolean value, String name) {
        assertions++;
        if (!value) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        check(SkillMath.level(0, 10, 100) == 0, "initial level");
        check(SkillMath.level(99, 10, 100) == 0, "below threshold");
        check(SkillMath.level(100, 10, 100) == 1, "first threshold");
        check(SkillMath.level(399, 10, 100) == 1, "quadratic curve");
        check(SkillMath.level(400, 10, 100) == 2, "second threshold");
        check(SkillMath.level(Long.MAX_VALUE, 10, 100) == 10, "bounded maximum level");
        check(SkillMath.addXp(Long.MAX_VALUE, Long.MAX_VALUE, 10, 100) == 10000, "overflow safe XP");
        check(SkillMath.addXp(5, -8, 10, 100) == 5, "negative reward");
        check(SkillMath.addXp(-1, 1, 10, 100) == 1, "invalid saved XP");
        check(SkillMath.capXp(Long.MAX_VALUE, 100, 1000000) == 10000000000L, "maximum configurable threshold");
        check(SkillMath.modeUnlocked(0, 2, 0), "basic mode");
        check(!SkillMath.modeUnlocked(1, 2, 2), "locked advanced mode");
        check(SkillMath.modeUnlocked(1, 2, 3), "advanced mode threshold");
        check(!SkillMath.modeUnlocked(-1, 2, 10), "negative mode");
        check(!SkillMath.modeUnlocked(2, 2, 10), "unknown mode");
        check(!SkillMath.permission(true, false), "menu unlock alone cannot equip intrinsic");
        check(SkillMath.permission(true, true), "explicit intrinsic permission");
        check(SkillMath.permission(false, false), "existing normal equip semantics preserved");
        check(SkillMath.validMovement(0.3, 1.5), "server position delta enables movement");
        check(!SkillMath.validMovement(0, 1.5), "idle movement rejected");
        check(!SkillMath.validMovement(1.5001, 1.5), "teleport delta rejected");
        check(!SkillMath.validMovement(2, 5), "hard anti-teleport ceiling");
        check(!SkillMath.validMovement(Double.NaN, 1.5), "nonfinite movement rejected");
        check(SkillMath.effectActive(true, true, 100, true), "paid granted speed effect");
        check(!SkillMath.effectActive(false, true, 100, true), "lost grant removes speed despite paid lease");
        check(!SkillMath.effectActive(true, false, 100, true), "toggle off removes speed");
        check(!SkillMath.effectActive(true, true, 0, true), "zero output removes speed");
        check(!SkillMath.effectActive(true, true, 100, false), "unpaid speed removed");
        check(!SkillMath.configurationStops(true, 25), "nonzero output edit preserves flight lease");
        check(!SkillMath.configurationStops(true, 100), "same-cost style edit preserves flight lease");
        check(SkillMath.configurationStops(true, 0), "zero output stops flight immediately");
        check(SkillMath.configurationStops(false, 100), "off or revoked permission stops flight");
        var paidFlight = new SkillMath.Lease(25, "Hover", 1);
        int requestedOutput = 100;
        String requestedStyle = "Cruise";
        check(paidFlight.output() == 25, "requested output cannot strengthen paid flight");
        check(paidFlight.style().equals("Hover"), "requested style waits for paid boundary");
        check(paidFlight.drain() == SkillMath.drain(4, paidFlight.output(), true), "cached cost matches paid output");
        var nextFlight = new SkillMath.Lease(requestedOutput, requestedStyle, 4);
        check(nextFlight.output() == 100 && nextFlight.style().equals("Cruise"), "new payment captures new parameters");
        check(paidFlight.output() == 25 && paidFlight.drain() == 1, "prior lease remains immutable");
        for (String id : List.of("divine_speed", "divine_flight", "divine_perception", "regeneration",
                "divine_resistances", "physical_training", "soul_restore")) {
            String path = SkillMath.iconPath("jaycee:" + id);
            check(path.equals("textures/ability/" + id + ".png"), "canonical icon metadata: " + id);
            check(java.nio.file.Files.isRegularFile(java.nio.file.Path.of("src/main/resources/assets/jaycee", path)),
                "actual icon asset exists: " + id);
        }
        check(SkillMath.nextMode(0, 2, 0, 3) == 0, "Alt cycling skips locked perception mode");
        check(SkillMath.nextMode(0, 2, 3, 3) == 1, "Alt cycling reaches unlocked perception mode");
        check(SkillMath.nextMode(0, 2, 0, 0) == 1, "Soul Restore indices unchanged");
        check(SkillMath.nextMode(1, 2, 0, 0) == 0, "Soul Restore cycle wraps");
        check(SkillMath.nextMode(0, 2, 4, 5) == 0, "configured mastery threshold");
        check(SkillMath.drain(4, 50, true) == 2, "scaled upkeep");
        check(SkillMath.drain(4, 0, true) == 0, "zero output");
        check(SkillMath.drain(4, 100, false) == 0, "no ineffective upkeep");
        check(SkillMath.drain(Double.NaN, 100, true) == 0, "finite costs");
        check(SkillMath.healingDrain(3, 100, 0, 1) == 0, "no mana at full health");
        check(SkillMath.healingDrain(3, 100, 0.5, 1) == 1.5, "prorated effective heal");
        check(SkillMath.healingDrain(3, 50, 1, 1) == 1.5, "output-scaled healing");
        check(SkillMath.healingDrain(3, 0, 1, 1) == 0, "zero output cannot heal");
        check(SkillMath.output(101) == 1 && SkillMath.output(-1) == 0, "bounded output");
        var gate = new SkillMath.Gate();
        check(gate.allow("target:a", 0, 100, 4, 8), "effective initial award");
        check(!gate.allow("target:a", 1, 100, 4, 8), "same target farming rejected");
        check(gate.allow("target:b", 1, 100, 4, 8), "new target limited award");
        check(!gate.allow("target:c", 2, 100, 1, 8), "target switching shares budget");
        check(!gate.allow("target:a", -1, 100, 4, 8), "invalid time");
        check(gate.allow("target:a", 1200, 100, 4, 8), "new minute");
        var restored = new SkillMath.Gate(gate.window(), gate.awarded(), gate.entries());
        check(!restored.allow("target:a", 1201, 100, 4, 8), "reconnect retains cooldown");
        check(restored.allow("target:d", 1201, 100, 4, 8), "reconnect retains spent budget");
        check(!restored.allow("target:e", 1201, 100, 1, 8), "reconnect cannot reset cap");
        for (int i = 0; i < 256; i++) restored.allow("target:" + i, 2400L + i * 1200L, 0, 1, 8);
        check(restored.entries().size() <= 128, "bounded target history");
        var context = new Cast();
        var toggle = new AbilityEngine.Definition<Cast>("jaycee:divine_speed", List.of(
            new AbilityEngine.Mode<>("Movement", 2, 20, c -> c.granted && !c.enabled, c -> c.enabled = true)));
        var engine = new AbilityEngine<Cast>(List.of(toggle));
        check(engine.activate(toggle.id(), 0, 0, context) == AbilityEngine.Result.NO_EFFECT, "ungranted toggle rejected");
        check(context.mana == 3 && !context.enabled, "rejection never spends");
        context.granted = true;
        check(engine.activate(toggle.id(), 0, 0, context) == AbilityEngine.Result.ACTIVATED, "shared executor toggle");
        check(context.enabled && context.mana == 1 && context.expiry == 20, "activation cost and shared cooldown");
        check(engine.activate(toggle.id(), 1, 20, context) == AbilityEngine.Result.INVALID_MODE, "bounded executor modes");
        context.enabled = false;
        check(engine.activate(toggle.id(), 0, 20, context) == AbilityEngine.Result.NO_MANA, "no free effect when empty");
        check(!context.enabled, "failed payment does not enable");
        System.out.println("SkillMathTest: " + assertions + " assertions passed");
    }
    private static final class Cast implements AbilityEngine.Context {
        boolean granted, enabled;
        double mana = 3;
        long expiry;
        public boolean permitted() { return true; }
        public long cooldownExpiry(String id) { return expiry; }
        public boolean consumeMana(double amount) {
            if (mana < amount) return false;
            mana -= amount;
            return true;
        }
        public void complete(String id, int mode, long tick) { expiry = tick; }
    }
}
