package com.brightwebs.jaycee.gameplay.divine;

import java.util.*;

/** Pure ledger regressions; does not pretend to exercise Minecraft entity runtime. */
public final class MobDivinityModelTest {
    private static final String SPEED = "jaycee:divine_speed";
    private static final UUID DONOR = UUID.randomUUID(), OTHER = UUID.randomUUID(), MOB = UUID.randomUUID();
    private static int assertions;
    public static void main(String[] args) {
        var model = model();
        var c = context(null);
        var observed = mob();
        eq(model.mobs().size(), 0);
        model.observeMob(observed);
        eq(model.mobs().size(), 0);
        var s = proposal(100, SPEED, true);
        var q = model.quoteMob(s, observed, c, true, 100);
        eq(q.fee(), 200L); eq(q.total(), 300L); eq(q.balanceAfter(), 9700L); eq(q.recipientAfter(), 100L);
        eq(model.profile(DONOR).essence(), 10_000L); eq(model.mobs().size(), 0);
        var after = model.confirmMob(DONOR, q.id(), c, true, true, 101);
        eq(model.profile(DONOR).essence(), 9700L); eq(after.essence(), 100L); eq(after.ep(), 0L);
        eq(after.received(), 100L); eq(after.master(), DONOR); eq(after.grants().get(SPEED), DONOR.toString());
        rejects(() -> model.confirmMob(DONOR, q.id(), c, true, true, 102));
        eq(model.profile(DONOR).essence(), 9700L);
        rejects(() -> model.quoteMob(proposal(0, SPEED, false), after, c, true, 103));
        rejects(() -> model.quoteMob(proposal(1, "", true), after, c, true, 103));
        rejects(() -> model.quoteMob(proposal(1, "", false), after, c, false, 103));
        var q2 = model.quoteMob(proposal(1, "", false), after, c, true, 104);
        rejects(() -> model.confirmMob(DONOR, q2.id(), c, false, true, 105));
        rejects(() -> model.confirmMob(DONOR, q2.id(), c, true, true, 106));
        eq(model.profile(DONOR).essence(), 9700L);
        var q3 = model.quoteMob(proposal(1, "", false), after, c, true, 104);
        rejects(() -> model.confirmMob(DONOR, q3.id(), c, true, true, 30_104));
        var q4 = model.quoteMob(proposal(1, "", false), after, c, true, 104);
        rejects(() -> model.confirmMob(DONOR, q4.id(), context(OTHER), true, true, 105));
        var q5 = model.quoteMob(proposal(1, "", false), after, c, true, 104);
        model.adminRank(DONOR, DivinityModel.Rank.MORTAL, 105);
        rejects(() -> model.confirmMob(DONOR, q5.id(), c, true, true, 106));
        var restored = model();
        restored.restoreMobs(model.mobs());
        eq(restored.mob(MOB), after);
        eq(restored.mobQuote(DONOR, 1), null);
        var supreme = model();
        supreme.assignAuthority(DONOR, false, 0);
        rejects(() -> supreme.quoteMob(s, observed, context(OTHER), true, 1));
        rejects(() -> supreme.quoteMob(s, observed, c, false, 1));
        var free = supreme.quoteMob(s, observed, c, true, 1);
        eq(free.fee(), 0L);
        supreme.adminBalance(DONOR, false, 0, 1);
        rejects(() -> supreme.confirmMob(DONOR, free.id(), c, true, true, 2));
        eq(supreme.mob(MOB), null);
        rejects(() -> model().quoteMob(proposal(Long.MAX_VALUE, SPEED, true), observed, c, true, 1));
        var playerLoop = model();
        playerLoop.join(MOB, "NotAMob");
        rejects(() -> playerLoop.quoteMob(s, observed, c, true, 1));
        var offline = model();
        var offQuote = offline.quoteMob(s, observed, c, true, 1);
        offline.disconnect(DONOR, 2);
        rejects(() -> offline.confirmMob(DONOR, offQuote.id(), c, true, true, 3));
        var changed = model();
        var changedQuote = changed.quoteMob(s, observed, c, true, 1);
        var anotherDimension = new DivinityModel.MobContext(MOB, c.type(), "minecraft:the_nether", c.entityId(), null);
        rejects(() -> changed.confirmMob(DONOR, changedQuote.id(), anotherDimension, true, true, 2));
        var typeQuote = changed.quoteMob(s, observed, c, true, 1);
        var anotherType = new DivinityModel.MobContext(MOB, "minecraft:pig", c.dimension(), c.entityId(), null);
        rejects(() -> changed.confirmMob(DONOR, typeQuote.id(), anotherType, true, true, 2));
        var owned = model();
        var ownPet = owned.quoteMob(s, observed, context(DONOR), true, 1);
        eq(owned.confirmMob(DONOR, ownPet.id(), context(DONOR), true, true, 2).master(), DONOR);
        var wrongMaster = new DivinityModel.MobProfile(MOB, observed.type(), observed.dimension(), observed.original(),
            "", "", 10, 10, 0, 0, 0, OTHER, Map.of(), "active");
        var wrong = model();
        wrong.restoreMobs(List.of(wrongMaster));
        rejects(() -> wrong.quoteMob(proposal(1, "", false), wrongMaster, c, true, 1));
        var existingRich = new DivinityModel.MobProfile(MOB, observed.type(), observed.dimension(), observed.original(),
            "", "", 10, 10, 0, 101, 101, null, Map.of(), "active");
        var fees = model();
        fees.restoreMobs(List.of(existingRich));
        eq(fees.quoteMob(proposal(0, SPEED, true), existingRich, c, true, 1).fee(), 227L);
        var noFunds = model();
        noFunds.adminBalance(DONOR, false, 299, 0);
        rejects(() -> noFunds.quoteMob(s, observed, c, true, 1));
        eq(noFunds.mob(MOB), null); eq(noFunds.profile(DONOR).essence(), 299L);
        var regenerating = model();
        regenerating.assignAuthority(DONOR, false, 0);
        var regenQuote = regenerating.quoteMob(s, observed, c, true, 1);
        regenerating.tickRegeneration();
        long available = regenerating.profile(DONOR).essence();
        regenerating.confirmMob(DONOR, regenQuote.id(), c, true, true, 2);
        eq(regenerating.profile(DONOR).essence(), available - 100);
        var changedBalance = model();
        var spendQuote = changedBalance.quoteMob(s, observed, c, true, 1);
        changedBalance.adminBalance(DONOR, false, 9999, 1);
        rejects(() -> changedBalance.confirmMob(DONOR, spendQuote.id(), c, true, true, 2));
        String regen = "jaycee:regeneration";
        var observations = model();
        var injured = new DivinityModel.MobProfile(MOB, observed.type(), observed.dimension(), observed.original(),
            "", "", 5, 10, 12, 5, 17, DONOR, Map.of(regen, DONOR.toString()), "active");
        observations.restoreMobs(List.of(injured));
        observations.observeMob(new DivinityModel.MobProfile(MOB, injured.type(), injured.dimension(), injured.original(),
            "", "", 6, 10, 0, 0, 0, null, Map.of(), "active"));
        eq(observations.mob(MOB).essence(), 5L);
        eq(observations.mob(MOB).ep(), 12L); eq(observations.mob(MOB).received(), 17L);
        eq(observations.profile(DONOR).essence(), 10_000L);
        observations.assignAuthority(OTHER, false, 0);
        observations.adminBalance(OTHER, false, 100, 0);
        var originalQuote = observations.quoteMob(proposal(1, "", false), observations.mob(MOB), c, true, 1);
        var intervening = observations.quoteMob(new DivinityModel.Proposal(OTHER, MOB, 1, "", "", false),
            observations.mob(MOB), c, true, 1);
        observations.confirmMob(OTHER, intervening.id(), c, true, true, 2);
        rejects(() -> observations.confirmMob(DONOR, originalQuote.id(), c, true, true, 3));
        eq(observations.profile(DONOR).essence(), 10_000L);
        var deceased = new DivinityModel.MobProfile(MOB, injured.type(), injured.dimension(), injured.original(),
            "", "", 0, 10, 12, 5, 17, DONOR, injured.grants(), "deceased");
        observations.restoreMobs(List.of(deceased));
        observations.observeMob(injured);
        eq(observations.mob(MOB), deceased);
        rejects(() -> observations.quoteMob(proposal(1, "", false), injured, c, true, 1));
        var restarting = model();
        var restartQuote = restarting.quoteMob(s, observed, c, true, 1);
        restarting.restore(restarting.image(), 2);
        rejects(() -> restarting.confirmMob(DONOR, restartQuote.id(), c, true, true, 3));
        var cosmetic = model();
        var cosmeticQuote = cosmetic.quoteMob(s, observed, c, true, 1);
        cosmetic.awardEp(DONOR, 1);
        cosmetic.join(DONOR, "NewDonor");
        cosmetic.confirmMob(DONOR, cosmeticQuote.id(), c, true, true, 2);
        eq(cosmetic.profile(DONOR).essence(), 9700L);
        eq(cosmetic.profile(DONOR).ep(), 1L);
        var observationQuote = cosmetic.quoteMob(proposal(1, "", false), cosmetic.mob(MOB), c, true, 3);
        cosmetic.observeMob(new DivinityModel.MobProfile(MOB, observed.type(), observed.dimension(), observed.original(),
            "External name", "", 7, 10, 0, 0, 0, null, Map.of(), "active"));
        cosmetic.confirmMob(DONOR, observationQuote.id(), c, true, true, 4);
        eq(cosmetic.mob(MOB).essence(), 101L);
        eq(cosmetic.mob(MOB).health(), 7.0F);
        var revoked = model();
        var revokedQuote = revoked.quoteMob(s, observed, c, true, 1);
        revoked.adminGrant(DONOR, SPEED, false, 2);
        rejects(() -> revoked.confirmMob(DONOR, revokedQuote.id(), c, true, true, 3));
        var lifecycle = model();
        lifecycle.restoreMobs(List.of(injured));
        lifecycle.joinMob(injured, c);
        var moved = new DivinityModel.MobProfile(MOB, injured.type(), "minecraft:the_nether", injured.original(),
            "", "", 5, 10, 12, 5, 17, DONOR, injured.grants(), "active");
        var newContext = new DivinityModel.MobContext(MOB, c.type(), moved.dimension(), 124, null);
        lifecycle.joinMob(moved, newContext);
        eq(lifecycle.mob(MOB).dimension(), "minecraft:the_nether");
        var beforeUnload = lifecycle.quoteMob(proposal(1, "", false), moved, newContext, true, 1);
        var oldUnload = new DivinityModel.MobProfile(MOB, injured.type(), injured.dimension(), injured.original(),
            "", "", 5, 10, 12, 5, 17, DONOR, injured.grants(), "unloaded");
        eq(lifecycle.leaveMob(oldUnload, c, "CHANGED_DIMENSION", false), false);
        eq(lifecycle.mobQuote(DONOR, 2).id(), beforeUnload.id());
        eq(lifecycle.observeMob(injured, c), false);
        eq(lifecycle.mob(MOB).dimension(), "minecraft:the_nether");
        eq(lifecycle.mob(MOB).status(), "active");
        var newUnload = new DivinityModel.MobProfile(MOB, moved.type(), moved.dimension(), moved.original(),
            "", "", 5, 10, 12, 5, 17, DONOR, moved.grants(), "unloaded");
        eq(lifecycle.leaveMob(newUnload, newContext, "UNLOADED_TO_CHUNK", false), true);
        eq(lifecycle.mobQuote(DONOR, 2), null);
        eq(lifecycle.mob(MOB).status(), "unloaded");
        eq(lifecycle.mobRemoval(MOB), "");
        lifecycle.joinMob(moved, newContext);
        eq(lifecycle.mob(MOB).status(), "active");
        eq(lifecycle.leaveMob(newUnload, newContext, "DISCARDED", true), true);
        lifecycle.joinMob(moved, newContext);
        eq(lifecycle.mob(MOB).status(), "removed");
        eq(lifecycle.mobRemoval(MOB), "DISCARDED");
        rejects(() -> lifecycle.quoteMob(proposal(1, "", false), moved, newContext, true, 1));
        var recovered = model();
        recovered.restoreMobs(lifecycle.mobs()); recovered.restoreMobRemovals(lifecycle.mobRemovals());
        recovered.joinMob(moved, newContext);
        eq(recovered.mob(MOB).status(), "removed");
        eq(recovered.mobRemoval(MOB), "DISCARDED");
        eq(recovered.mob(MOB).master(), DONOR);
        var legacyRemoval = model();
        legacyRemoval.restoreMobs(List.of(newUnload));
        legacyRemoval.restoreMobRemovals(Map.of(MOB, "DISCARDED"));
        eq(legacyRemoval.mob(MOB).status(), "removed");
        var killed = model();
        killed.restoreMobs(List.of(injured)); killed.joinMob(injured, c);
        eq(killed.leaveMob(deceased, c, "KILLED", true), true);
        killed.joinMob(injured, c);
        eq(killed.mob(MOB).status(), "deceased");
        var authorized = model();
        eq(authorized.authorizedMobPower(DONOR, SPEED, 0), false);
        eq(authorized.authorizedMobPower(DONOR, SPEED, 1), true);
        authorized.adminRank(DONOR, DivinityModel.Rank.MORTAL, 0);
        eq(authorized.authorizedMobPower(DONOR, SPEED, 100), false);
        authorized.assignAuthority(DONOR, false, 0);
        authorized.adminGrant(DONOR, SPEED, false, 0);
        eq(authorized.authorizedMobPower(DONOR, SPEED, 0), true);
        eq(authorized.authorizedMobPower(DONOR, "unregistered:power", 100), false);
        var revokedPrivileges = model();
        revokedPrivileges.assignAuthority(OTHER, false, 0);
        revokedPrivileges.adjudicate(OTHER, DONOR, "revoke_privileges", 0);
        eq(revokedPrivileges.authorizedMobPower(DONOR, SPEED, 100), false);
        for (String type : List.of("minecraft:wandering_trader", "minecraft:trader_llama")) {
            var timed = model(); timed.assignAuthority(DONOR, false, 0);
            var timedProfile = new DivinityModel.MobProfile(MOB, type, observed.dimension(), type,
                "", "", 10, 10, 0, 0, 0, null, Map.of(), "active");
            var timedContext = new DivinityModel.MobContext(MOB, type, observed.dimension(), 123, null);
            rejects(() -> timed.quoteMob(s, timedProfile, timedContext, true, 1));
            eq(timed.profile(DONOR).essence(), 10_000L); eq(timed.mob(MOB), null);
            eq(timed.quoteMob(proposal(1, SPEED, false), timedProfile, timedContext, true, 1).fee(), 0L);
        }
        eq(DivinityModel.mobBindingReason("minecraft:villager"), "");
        System.out.println("MobDivinityModelTest: " + assertions + " assertions passed");
    }
    private static DivinityModel model() {
        var m = new DivinityModel(DivinityModel.Config.defaults(), Set.of(SPEED));
        m.join(DONOR, "Donor"); m.join(OTHER, "Other");
        m.adminRank(DONOR, DivinityModel.Rank.MINOR_GOD, 0);
        m.adminBalance(DONOR, false, 10_000, 0); m.adminGrant(DONOR, SPEED, true, 0);
        return m;
    }
    private static DivinityModel.Proposal proposal(long amount, String ability, boolean bind) {
        return new DivinityModel.Proposal(DONOR, MOB, amount, ability, "Blessed Cow", bind);
    }
    private static DivinityModel.MobContext context(UUID owner) {
        return new DivinityModel.MobContext(MOB, "minecraft:cow", "minecraft:overworld", 123, owner);
    }
    private static DivinityModel.MobProfile mob() {
        return new DivinityModel.MobProfile(MOB, "minecraft:cow", "minecraft:overworld", "entity.minecraft.cow",
            "", "", 10, 10, 0, 0, 0, null, Map.of(), "active");
    }
    private static void eq(Object actual, Object expected) {
        assertions++;
        if (!Objects.equals(actual, expected)) throw new AssertionError(actual + " != " + expected);
    }
    private static void rejects(Runnable action) {
        assertions++;
        try { action.run(); } catch (IllegalArgumentException | ArithmeticException expected) { return; }
        throw new AssertionError("Expected rejection");
    }
}
