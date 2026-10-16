package com.brightwebs.jaycee.gameplay.divine;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Dependency-free curve, provenance, transaction and migration regressions. */
public final class EssenceProgressionTest {
    private static int assertions;
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2), C = new UUID(0, 3);
    private static final EssenceProgression.Tuning DEFAULTS = EssenceProgression.Tuning.defaults();

    public static void main(String[] args) {
        arithmetic();
        configurableGrades();
        creditAndAdmin();
        giftProvenance();
        regeneration();
        migrationAndRestart();
        mobProgress();
        System.out.println("EssenceProgressionTest: " + assertions + " assertions passed");
    }

    private static void arithmetic() {
        var initial = EssenceProgression.player(0, false, DEFAULTS);
        eq(initial.level(), 1); eq(initial.grade(), "F"); eq(initial.nextCost(), 400L);
        eq(EssenceProgression.player(399, false, DEFAULTS).intoLevel(), 399L);
        eq(EssenceProgression.player(399, false, DEFAULTS).level(), 1);
        eq(EssenceProgression.player(400, false, DEFAULTS).level(), 2);
        eq(EssenceProgression.player(400, false, DEFAULTS).intoLevel(), 0L);
        eq(EssenceProgression.player(400, false, DEFAULTS).nextCost(), 900L);
        for (int level : new int[]{2, 11, 21, 36, 51, 66, 81, 96, 111, 100_000}) {
            long threshold = EssenceProgression.earnedForLevel(level, DEFAULTS);
            eq(EssenceProgression.player(threshold, true, DEFAULTS).level(), level);
            eq(EssenceProgression.player(threshold - 1, true, DEFAULTS).level(), level - 1);
        }
        var maximum = EssenceProgression.player(Long.MAX_VALUE, true, DEFAULTS);
        long threshold = EssenceProgression.earnedForLevel(maximum.level(), DEFAULTS);
        eq(maximum.intoLevel(), Long.MAX_VALUE - threshold);
        check(BigInteger.valueOf(threshold).add(BigInteger.valueOf(maximum.nextCost()))
            .compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0);
        var enormous = new EssenceProgression.Tuning(Long.MAX_VALUE, DEFAULTS.gradeMinimumLevels());
        var saturated = EssenceProgression.player(Long.MAX_VALUE, false, enormous);
        eq(saturated.level(), 1); eq(saturated.nextCost(), Long.MAX_VALUE); check(saturated.nextCostCapped());
        rejects(() -> EssenceProgression.earnedForLevel(Integer.MAX_VALUE, DEFAULTS));
        rejects(() -> EssenceProgression.player(-1, false, DEFAULTS));
        rejects(() -> EssenceProgression.earnedForLevel(0, DEFAULTS));
    }

    private static void configurableGrades() {
        String[] grades = {"F", "E", "D", "C", "B", "A", "S", "SS", "SSS"};
        for (int i = 0; i < grades.length; i++) {
            long threshold = EssenceProgression.earnedForLevel(DEFAULTS.gradeMinimumLevels().get(i), DEFAULTS);
            eq(EssenceProgression.player(threshold, true, DEFAULTS).grade(), grades[i]);
        }
        long sss = EssenceProgression.earnedForLevel(111, DEFAULTS);
        eq(EssenceProgression.player(sss, false, DEFAULTS).grade(), "SS");
        var tuning = new EssenceProgression.Tuning(2, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
        eq(EssenceProgression.player(8, false, tuning).grade(), "E");
        eq(EssenceProgression.player(8, false, tuning).level(), 2);
        rejects(() -> new EssenceProgression.Tuning(0, DEFAULTS.gradeMinimumLevels()));
        rejects(() -> new EssenceProgression.Tuning(1, List.of(1, 2)));
        rejects(() -> new EssenceProgression.Tuning(1, List.of(1, 2, 2, 4, 5, 6, 7, 8, 9)));
        rejects(() -> new EssenceProgression.Tuning(1, List.of(0, 2, 3, 4, 5, 6, 7, 8, 9)));
    }

    private static DivinityModel world() {
        var model = new DivinityModel(new DivinityModel.Config(Long.MAX_VALUE, Long.MAX_VALUE,
            Long.MAX_VALUE, 20, 1, 100, 1, 1_000, 8), Set.of());
        model.join(A, "First"); model.join(B, "Second"); model.join(C, "Third");
        for (UUID id : List.of(A, B, C)) model.adminRank(id, DivinityModel.Rank.MINOR_GOD, 0);
        return model;
    }

    private static void creditAndAdmin() {
        var model = world();
        model.awardEp(A, 900);
        eq(model.earned(A), 0L);
        model.adminSetEssence(A, 400, 0);
        eq(model.earned(A), 0L);
        model.adminAddEssence(A, 100, 0);
        eq(model.earned(A), 0L);
        eq(model.creditEssence(A, 400), 400L);
        eq(model.progression(A).level(), 2);
        eq(model.profile(A).ep(), 900L);
        model.adminSetEssence(A, 0, 0);
        eq(model.progression(A).level(), 2);
        model.adminSetLevel(A, 111, 0);
        eq(model.progression(A).grade(), "SS");
        model.assignAuthority(C, false, 0);
        model.adjudicate(C, A, "cap_lift", 0);
        eq(model.progression(A).grade(), "SSS");
        model.adjudicate(C, A, "cap_revoke", 0);
        eq(model.progression(A).grade(), "SS");
        model.assignAuthority(A, true, 0);
        eq(model.progression(A).grade(), "SSS");
        model.adminSetEarned(A, Long.MAX_VALUE - 1, 0);
        eq(model.creditEssence(A, 20), 20L);
        eq(model.earned(A), Long.MAX_VALUE);
        rejects(() -> model.adminSetEarned(A, -1, 0));
        rejects(() -> model.adminSetLevel(A, Integer.MAX_VALUE, 0));
        var before = model.image();
        rejects(() -> model.adminAddEssence(A, Long.MAX_VALUE, 0));
        eq(model.image(), before);
        model.adminSetEssence(A, Long.MAX_VALUE, 0);
        eq(model.creditEssence(A, 1), 0L);
        eq(model.earned(A), Long.MAX_VALUE);
    }

    private static void giftProvenance() {
        var model = world();
        model.creditEssence(A, 1_000);
        var pending = model.offer(new DivinityModel.Proposal(A, B, 1_000, "", "", false), 0, true);
        eq(model.earned(B), 0L);
        var before = model.image();
        rejects(() -> model.accept(B, pending.id(), 1, false));
        eq(model.image(), before);
        model.accept(B, pending.id(), 1, true);
        eq(model.earned(B), 1_000L); eq(model.profile(B).patron(), null);
        eq(model.earned(A), 1_000L); eq(model.profile(A).essence(), 0L);
        eq(model.progression(A).level(), 2);
        rejects(() -> model.accept(B, pending.id(), 2, true));
        for (int i = 0; i < 20; i++) {
            gift(model, B, A, 1_000);
            gift(model, A, B, 1_000);
        }
        eq(model.earned(A), 1_000L); eq(model.earned(B), 1_000L);
        gift(model, B, C, 1_000);
        eq(model.earned(C), 0L);
        model.adminAddEssence(C, 10_000, 0);
        gift(model, C, A, 10_000);
        eq(model.earned(A), 1_000L);
        model.adminSetEssence(B, 0, 0);
        model.creditEssence(B, 500);
        model.adminAddEssence(B, 500, 0);
        gift(model, B, A, 1_000);
        eq(model.earned(A), 1_500L);
        var mixed = world();
        mixed.creditEssence(A, 500);
        mixed.adminAddEssence(A, 500, 0);
        gift(mixed, A, B, 400);
        eq(mixed.earned(B), 0L);
        eq(mixed.playerProgressStates().get(A).awardable(), 500L);
        gift(mixed, A, B, 200);
        eq(mixed.earned(B), 100L);
        eq(mixed.playerProgressStates().get(A).awardable(), 400L);

        var fees = world();
        fees.creditEssence(A, 1_000);
        var bound = fees.offer(new DivinityModel.Proposal(A, B, 500, "", "", true), 0, true);
        fees.accept(B, bound.id(), 1, true);
        eq(fees.earned(B), 500L);
        eq(fees.playerProgressStates().get(A).awardable(), 499L);
        eq(fees.earned(A), 1_000L);
        gift(fees, A, C, 499);
        eq(fees.earned(C), 499L);
    }

    private static void gift(DivinityModel model, UUID from, UUID to, long amount) {
        var offer = model.offer(new DivinityModel.Proposal(from, to, amount, "", "", false), 0, true);
        model.accept(to, offer.id(), 1, true);
    }

    private static void regeneration() {
        var model = world();
        model.assignAuthority(A, false, 0);
        for (int i = 0; i < 400; i++) model.tickRegeneration();
        eq(model.earned(A), 0L); eq(model.progression(A).level(), 1);
        eq(model.profile(A).essence(), 400L);
        gift(model, A, B, 400);
        eq(model.progression(B).level(), 2); eq(model.earned(A), 0L);
        for (int i = 0; i < 400; i++) model.tickRegeneration();
        gift(model, A, B, 400);
        eq(model.earned(B), 800L); eq(model.earned(A), 0L);
    }

    private static void migrationAndRestart() {
        var source = world();
        source.adminSetEssence(A, 90_000, 0); source.adminSetEp(A, 80_000, 0);
        var legacy = new DivinityModel.Image(null, source.image().profiles(), List.of(), List.of(), 0);
        source.restore(legacy, 0);
        eq(source.earned(A), 0L); eq(source.progression(A).level(), 1);
        gift(source, A, B, 1_000);
        eq(source.earned(B), 0L);
        source.creditEssence(A, 5_000);
        var pending = source.offer(new DivinityModel.Proposal(A, C, 1, "", "", false), 0, true);
        var restored = world();
        restored.restore(source.image(), 1);
        eq(restored.earned(A), 5_000L);
        eq(restored.playerProgressStates(), source.playerProgressStates());
        rejects(() -> restored.accept(C, pending.id(), 2, true));
        gift(restored, A, B, restored.profile(A).essence());
        eq(restored.earned(B), 5_000L);
        gift(restored, B, A, 5_000);
        eq(restored.earned(A), 5_000L);
        restored.restoreProgress(Map.of(A, new DivinityModel.ProgressState(12, Long.MAX_VALUE)), Map.of());
        eq(restored.playerProgressStates().get(A).awardable(), restored.profile(A).essence());
        eq(restored.earned(A), 12L);
    }

    private static void mobProgress() {
        var model = world();
        UUID id = new UUID(0, 99);
        var observed = new DivinityModel.MobProfile(id, "minecraft:wolf", "minecraft:overworld",
            "Wolf", "", "", 10, 10, 90, 800, 900, null, Map.of(), "active");
        model.restoreMobs(List.of(observed));
        eq(model.mobProgression(id).earned(), 0L);
        model.creditEssence(A, 1_000);
        var context = new DivinityModel.MobContext(id, observed.type(), observed.dimension(), 1, null);
        var quote = model.quoteMob(new DivinityModel.Proposal(A, id, 500, "", "", false), observed, context, true, 0);
        eq(model.mobProgression(id).earned(), 0L);
        var after = model.confirmMob(A, quote.id(), context, true, true, 1);
        eq(after.master(), null); eq(after.ep(), 90L); eq(after.received(), 1_400L);
        eq(model.mobProgression(id).earned(), 500L);
        check(!model.mobProgression(id).supported());
        eq(model.mobProgression(id).ep(), 90L);
        eq(model.mobProgression(id).level(), null);
        eq(model.mobProgression(id).grade(), null);
        eq(model.mobProgression(id).evolutionEligible(), null);
        rejects(() -> model.confirmMob(A, quote.id(), context, true, true, 2));
        eq(model.mobProgression(id).earned(), 500L);
        var restored = world();
        restored.restore(model.image(), 0); restored.restoreMobs(model.mobs());
        restored.restoreProgress(model.playerProgressStates(), model.mobProgressStates());
        eq(restored.mobProgression(id), model.mobProgression(id));
        var unsupported = EssenceProgression.mob("othermod:scripted_boss", 9_000, 0);
        check(!unsupported.supported()); eq(unsupported.level(), null); eq(unsupported.earned(), 9_000L);
        rejects(() -> restored.mobProgression(UUID.randomUUID()));
    }

    private static void eq(Object actual, Object expected) {
        assertions++;
        if (!java.util.Objects.equals(actual, expected)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
    private static void check(boolean value) {
        assertions++;
        if (!value) throw new AssertionError("Condition failed");
    }
    private static void rejects(Runnable operation) {
        assertions++;
        try { operation.run(); }
        catch (IllegalArgumentException | ArithmeticException expected) { return; }
        throw new AssertionError("Operation should have failed");
    }
}
