package com.brightwebs.jaycee.gameplay.divine;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.brightwebs.jaycee.gameplay.divine.DivinityModel.*;

/** Dependency-free regression checks runnable directly with the Java 21 compiler/runtime. */
public final class DivinityModelTest {
    private static final UUID JAYCEE = new UUID(0, 1);
    private static final UUID DONOR = new UUID(0, 2);
    private static final UUID FOLLOWER = new UUID(0, 3);
    private static final UUID OTHER = new UUID(0, 4);
    private static final String POWER = "jaycee:soul_restore";
    private static final String SECOND_POWER = "jaycee:flight";
    private static final long NOW = 100;
    private static int assertions;
    private static int scenarios;

    private static Config tuning() {
        return new Config(10_000, 20_000, 50_000, 37, 7, 125, 11, 1_000, 8);
    }

    private static DivinityModel world(Config config) {
        var model = new DivinityModel(config, Set.of(POWER, SECOND_POWER));
        model.join(JAYCEE, "Jaycee");
        model.join(DONOR, "Donor");
        model.join(FOLLOWER, "Follower");
        model.join(OTHER, "Other");
        return model;
    }

    private static DivinityModel world() { return world(tuning()); }

    private static DivinityModel ordinary(Config config) {
        var model = world(config);
        model.adminRank(DONOR, Rank.MINOR_GOD, NOW);
        model.adminGrant(DONOR, POWER, true, NOW);
        model.adminBalance(DONOR, false, config.essenceCap(), NOW);
        return model;
    }

    private static DivinityModel supremeWorld() {
        var model = world();
        model.assignAuthority(JAYCEE, false, NOW);
        model.adminBalance(JAYCEE, false, model.config().supremeCap(), NOW);
        return model;
    }

    private static Proposal proposal(UUID donor, UUID recipient, long amount, String power, String name, boolean bind) {
        return new Proposal(donor, recipient, amount, power, name, bind);
    }

    private static Proposal invitation(UUID donor, UUID recipient) {
        return proposal(donor, recipient, 0, "", "", true);
    }

    private static Offer offer(DivinityModel model, Proposal proposal) {
        return model.offer(proposal, NOW, true);
    }

    private static void bind(DivinityModel model, UUID donor, UUID recipient) {
        var pending = offer(model, invitation(donor, recipient));
        model.accept(recipient, pending.id(), NOW + 1, true);
    }

    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }

    private static void rejected(Runnable operation, String message) {
        assertions++;
        try { operation.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError(message);
    }

    private static void unchanged(DivinityModel model, Runnable operation, String message) {
        var before = model.image();
        rejected(operation, message);
        check(model.image().equals(before), message + ": rejection changed profiles, offers, audit or carry");
    }

    private static void run(Runnable scenario) {
        scenario.run();
        scenarios++;
    }

    private static void uniqueAuthority() {
        var model = world();
        for (var rank : Rank.values()) {
            model.adminRank(DONOR, rank, NOW);
            check(!model.supreme(DONOR) && model.authority() == null, "Testing rank is not unique authority: " + rank);
        }
        model.adminGrant(DONOR, POWER, true, NOW);
        check(!model.owns(DONOR, SECOND_POWER), "Operator test grant does not unlock every power");
        unchanged(model, () -> model.adjudicate(DONOR, FOLLOWER, "cap_lift", NOW), "Testing Olympian cannot adjudicate");
        model.assignAuthority(JAYCEE, false, NOW);
        check(model.supreme(JAYCEE) && !model.supreme(DONOR) && !model.supreme(null), "Exactly one assigned authority");
        check(model.profile(JAYCEE).rank() == Rank.MORTAL, "Authority is independent of testing rank");
        check(model.owns(JAYCEE, POWER) && model.owns(JAYCEE, SECOND_POWER), "Authority owns all registered powers");
        check(!model.owns(JAYCEE, "jaycee:unimplemented"), "Authority does not invent unregistered powers");
        unchanged(model, () -> model.assignAuthority(DONOR, false, NOW), "Replacement requires explicit permission");
        unchanged(model, () -> model.assignAuthority(new UUID(0, 99), true, NOW), "Unknown player cannot become authority");
        unchanged(model, () -> offer(model, invitation(DONOR, JAYCEE)), "True authority cannot receive a bestowal");
        unchanged(model, () -> model.adjudicate(JAYCEE, JAYCEE, "strip", NOW), "Adjudication cannot strip authority");
    }

    private static void authorityChangesCancelOffers() {
        var model = ordinary(tuning());
        var first = offer(model, invitation(DONOR, FOLLOWER));
        model.assignAuthority(JAYCEE, false, NOW);
        check(model.pending(DONOR).isEmpty(), "Initial authority assignment cancels ordinary pending offers");
        unchanged(model, () -> model.accept(FOLLOWER, first.id(), NOW + 1, true), "Canceled assignment offer cannot replay");
        model.adminBalance(JAYCEE, false, 50, NOW);
        var second = offer(model, proposal(JAYCEE, FOLLOWER, 10, POWER, "", true));
        model.tickRegeneration();
        check(model.image().carry() != 0, "Nonintegral refill accumulates carry");
        var oldAuthority = model.profile(JAYCEE);
        model.assignAuthority(OTHER, true, NOW);
        check(model.supreme(OTHER) && !model.supreme(JAYCEE), "Reassignment removes former authority");
        check(model.profile(JAYCEE).equals(oldAuthority), "Reassignment does not overwrite player history");
        check(model.pending(FOLLOWER).isEmpty() && model.image().carry() == 0, "Reassignment clears offers and refill carry");
        check(!model.owns(JAYCEE, POWER), "Former authority does not retain implicit grants");
        unchanged(model, () -> model.accept(FOLLOWER, second.id(), NOW + 1, true), "Reassigned offer cannot replay");
        var third = offer(model, invitation(OTHER, FOLLOWER));
        model.assignAuthority(null, true, NOW);
        check(model.authority() == null && !model.supreme(OTHER) && model.image().offers().isEmpty(), "Unassignment cancels offers");
        unchanged(model, () -> model.accept(FOLLOWER, third.id(), NOW + 1, true), "Unassigned offer cannot replay");
        var before = model.image();
        for (int i = 0; i < 20; i++) model.tickRegeneration();
        check(model.image().equals(before), "No authority means no automatic refill");
    }

    private static void ordinaryEstablishmentAndAtomicAcceptance() {
        // This locks down reserve-based quoting, not a claim about final economic tuning.
        for (var config : List.of(Config.defaults(), tuning(),
                new Config(8_000, 30_000, 50_000, 0, 13, 173, 29, 1_000, 3))) {
            var model = ordinary(config);
            for (long sample : new long[]{0, 1, 2, 79, 80, 81, 801}) {
                long expected = Math.max(config.minimumFee(), BigInteger.valueOf(sample)
                    .multiply(BigInteger.valueOf(config.feePercent())).add(BigInteger.valueOf(99))
                    .divide(BigInteger.valueOf(100)).longValueExact());
                check(model.establishmentFee(sample) == expected, "Minimum fee and upward percentage rounding at reserve " + sample);
            }
            long ep = 23, amount = 47, initialEssence = 800;
            model.adminBalance(DONOR, true, 97, NOW);
            model.adminBalance(FOLLOWER, true, ep, NOW);
            model.adminBalance(FOLLOWER, false, initialEssence, NOW);
            long expectedEstablishment = Math.max(config.minimumFee(),
                BigInteger.valueOf(initialEssence).multiply(BigInteger.valueOf(config.feePercent()))
                    .add(BigInteger.valueOf(99)).divide(BigInteger.valueOf(100)).longValueExact());
            check(model.establishmentFee(initialEssence) == expectedEstablishment, "Configured establishment percentage and rounding");
            if (config.feePercent() == 125) check(expectedEstablishment == 1_000, "Ordinary 125 percent Essence reserve establishment");
            var request = proposal(DONOR, FOLLOWER, amount, POWER, "Aurora", true);
            long quoted = model.quote(request);
            model.adminBalance(FOLLOWER, true, ep + 1, NOW);
            check(model.quote(request) == quoted, "EP changes do not affect reserve-based establishment price");
            model.adminBalance(FOLLOWER, true, ep, NOW);
            var donorBefore = model.profile(DONOR);
            var recipientBefore = model.profile(FOLLOWER);
            var pending = offer(model, request);
            long fee = expectedEstablishment + config.powerFee();
            check(pending.fee() == fee && model.quote(request) == fee, "Establishment and power fees are quoted together");
            check(model.profile(DONOR).equals(donorBefore) && model.profile(FOLLOWER).equals(recipientBefore),
                "Offering reserves neither balances nor ownership");
            model.accept(FOLLOWER, pending.id(), NOW + 1, true);
            var donor = model.profile(DONOR);
            var recipient = model.profile(FOLLOWER);
            check(donor.essence() == donorBefore.essence() - fee - amount, "Donor pays transfer and burned fees exactly once");
            check(recipient.essence() == initialEssence + amount, "Recipient receives transfer only, not the fee");
            check(donor.ep() == donorBefore.ep() && recipient.ep() == ep, "EP is never transferred or spent as Essence");
            check(donor.essence() + recipient.essence() == donorBefore.essence() + initialEssence - fee, "Only fee leaves total Essence");
            check(recipient.rank() == Rank.BLESSED && DONOR.equals(recipient.patron()), "Consent atomically establishes lineage and rank");
            check(recipient.divineName().equals("Aurora") && recipient.displayName().equals("Aurora"), "Consent applies chosen name");
            check(recipient.grants().get(POWER).equals(DONOR.toString()) && model.owns(DONOR, POWER), "Grant provenance recorded without removing donor's power");
            check(!recipient.bestow() && !recipient.capLift() && model.pending(FOLLOWER).isEmpty(), "Invitation grants no implicit privileges or caps");
            unchanged(model, () -> model.accept(FOLLOWER, pending.id(), NOW + 2, true), "Acceptance is single use");
            var repeat = offer(model, proposal(DONOR, FOLLOWER, 5, "", "", false));
            check(repeat.fee() == 0, "Existing follower transfer has no new establishment fee");
            model.accept(FOLLOWER, repeat.id(), NOW + 2, true);
            check(model.profile(FOLLOWER).ep() == ep && model.profile(FOLLOWER).essence() == initialEssence + amount + 5,
                "Existing follower receives only Essence");
        }
    }

    private static void insufficientAndStaleOffers() {
        var model = ordinary(tuning());
        model.adminBalance(FOLLOWER, false, 123, NOW);
        var request = proposal(DONOR, FOLLOWER, 71, POWER, "Pending", true);
        long debit = model.quote(request) + request.amount();
        for (long balance : new long[]{0, debit - 1}) {
            model.adminBalance(DONOR, false, balance, NOW);
            unchanged(model, () -> offer(model, request), "Insufficient total debit rejects creation");
        }
        model.adminBalance(DONOR, false, debit, NOW);
        var pending = offer(model, request);
        model.adminBalance(DONOR, false, debit - 1, NOW);
        unchanged(model, () -> model.accept(FOLLOWER, pending.id(), NOW + 1, true), "Acceptance rechecks changed donor balance");
        model.adminBalance(DONOR, false, model.config().essenceCap(), NOW);
        model.adminBalance(FOLLOWER, false, 124, NOW);
        unchanged(model, () -> model.accept(FOLLOWER, pending.id(), NOW + 1, true), "Changed establishment price requires a fresh offer");
        model.adminBalance(FOLLOWER, false, 123, NOW);
        model.adminBalance(FOLLOWER, true, 124, NOW);
        check(model.quote(request) == pending.fee(), "EP change alone does not stale an offer price");
        model.adminGrant(DONOR, POWER, false, NOW);
        unchanged(model, () -> model.accept(FOLLOWER, pending.id(), NOW + 1, true), "Revoked ownership invalidates pending power grant");
        model.adminGrant(DONOR, POWER, true, NOW);
        model.adminRank(DONOR, Rank.DEMIGOD, NOW);
        unchanged(model, () -> model.accept(FOLLOWER, pending.id(), NOW + 1, true), "Lower rank invalidates pending bestowal");
        model.adminRank(DONOR, Rank.MINOR_GOD, NOW);
        model.assignAuthority(JAYCEE, false, NOW);
        var afterReassignment = offer(model, request);
        model.adjudicate(JAYCEE, DONOR, "revoke_privileges", NOW);
        unchanged(model, () -> model.accept(FOLLOWER, afterReassignment.id(), NOW + 1, true), "Revoked privileges invalidate pending bestowal");

        var capped = supremeWorld();
        var capOffer = offer(capped, proposal(JAYCEE, FOLLOWER, request.amount(), "", "", false));
        capped.adminBalance(FOLLOWER, false, capped.config().essenceCap() - request.amount() + 1, NOW);
        unchanged(capped, () -> capped.accept(FOLLOWER, capOffer.id(), NOW + 1, true), "Acceptance rechecks recipient cap");

        var funded = ordinary(tuning());
        long exactDebit = funded.quote(request) + request.amount();
        funded.adminBalance(DONOR, false, exactDebit, NOW);
        var first = offer(funded, request);
        var competing = offer(funded, request);
        funded.accept(FOLLOWER, first.id(), NOW + 1, true);
        check(funded.profile(DONOR).essence() == 0 && funded.profile(FOLLOWER).essence() == request.amount(),
            "Exact transfer plus fee balance is sufficient and spends to zero");
        unchanged(funded, () -> funded.accept(FOLLOWER, competing.id(), NOW + 1, true),
            "Competing offer cannot double-spend or establish the same follower twice");
    }

    private static void consentLifecycleAndReachability() {
        var model = supremeWorld();
        var request = proposal(JAYCEE, FOLLOWER, 17, POWER, "Chosen", true);
        unchanged(model, () -> model.offer(request, NOW, false), "Offline, hidden or out-of-range proposal rejected");
        var pending = offer(model, request);
        unchanged(model, () -> model.accept(FOLLOWER, pending.id(), NOW + 1, false), "Acceptance requires current reachability");
        unchanged(model, () -> model.accept(OTHER, pending.id(), NOW + 1, true), "Unrelated player cannot accept");
        unchanged(model, () -> model.accept(JAYCEE, pending.id(), NOW + 1, true), "Donor cannot accept for recipient");
        unchanged(model, () -> model.cancel(OTHER, pending.id(), NOW), "Unrelated player cannot decline or cancel");
        var beforeDecline = model.image().profiles();
        model.cancel(FOLLOWER, pending.id(), NOW + 1);
        check(model.image().profiles().equals(beforeDecline) && model.pending(FOLLOWER).isEmpty(), "Decline changes no player state");
        unchanged(model, () -> model.accept(FOLLOWER, pending.id(), NOW + 2, true), "Declined offer cannot replay");
        unchanged(model, () -> model.cancel(FOLLOWER, pending.id(), NOW + 2), "Repeated decline rejected");
        var canceled = offer(model, request);
        model.cancel(JAYCEE, canceled.id(), NOW + 1);
        check(model.image().profiles().equals(beforeDecline), "Donor cancellation changes no player state");
        var expiring = offer(model, request);
        model.expire(expiring.expires() - 1);
        check(model.pending(FOLLOWER).size() == 1, "Offer remains valid immediately before exact expiry");
        rejected(() -> model.accept(FOLLOWER, expiring.id(), expiring.expires(), true), "Exact expiry cannot be accepted");
        check(model.pending(FOLLOWER).isEmpty() && model.image().profiles().equals(beforeDecline), "Expiry discards offer without transfer");
        for (UUID disconnected : List.of(JAYCEE, FOLLOWER)) {
            var offline = offer(model, request);
            model.disconnect(OTHER, NOW + 1);
            check(model.pending(FOLLOWER).size() == 1, "Unrelated disconnect preserves offer");
            model.disconnect(disconnected, NOW + 1);
            check(model.pending(FOLLOWER).isEmpty(), "Either participant disconnect cancels offer");
            unchanged(model, () -> model.accept(FOLLOWER, offline.id(), NOW + 2, true), "Disconnected offer cannot replay");
        }
        var valid = offer(model, request);
        model.accept(FOLLOWER, valid.id(), valid.expires() - 1, true);
        check(model.profile(FOLLOWER).essence() == 17, "Valid consent before deadline succeeds");
    }

    private static void grantsAndProvenance() {
        var model = ordinary(tuning());
        unchanged(model, () -> offer(model, proposal(DONOR, FOLLOWER, 0, SECOND_POWER, "", true)), "Ordinary donor must own granted ability");
        unchanged(model, () -> offer(model, proposal(DONOR, FOLLOWER, 0, "jaycee:preview", "", true)), "Unknown power cannot be bestowed");
        unchanged(model, () -> model.adminGrant(DONOR, "jaycee:preview", true, NOW), "Operator cannot invent registered powers");
        check(model.profile(DONOR).grants().get(POWER).equals("operator"), "Operator grant records provenance");
        model.migrateRestore(OTHER, POWER, NOW);
        check(model.profile(OTHER).grants().get(POWER).equals("legacy:three_essences"), "Legacy migration records explicit provenance");
        var migrated = model.image();
        model.migrateRestore(OTHER, POWER, NOW);
        check(model.image().equals(migrated), "Legacy migration preserves an existing grant");
        model.migrateRestore(DONOR, POWER, NOW);
        check(model.profile(DONOR).grants().get(POWER).equals("operator"), "Migration preserves existing provenance");
        model.adminNpc(FOLLOWER, "athena", true, NOW);
        check(!model.owns(FOLLOWER, POWER) && !model.profile(FOLLOWER).bestow(), "NPC lineage is metadata, not a power grant");
        var grant = offer(model, proposal(DONOR, FOLLOWER, 0, POWER, "", true));
        model.accept(FOLLOWER, grant.id(), NOW + 1, true);
        check(model.owns(FOLLOWER, POWER) && model.profile(FOLLOWER).grants().get(POWER).equals(DONOR.toString()),
            "Accepted zero-transfer grant records actual donor");
        unchanged(model, () -> offer(model, proposal(DONOR, FOLLOWER, 0, POWER, "", false)), "Duplicate power grant rejected");
        model.adminGrant(FOLLOWER, POWER, false, NOW);
        check(!model.owns(FOLLOWER, POWER) && !model.profile(FOLLOWER).grants().containsKey(POWER), "Revocation removes actual ownership");
        model.adminRank(DONOR, Rank.OLYMPIAN, NOW);
        unchanged(model, () -> model.adjudicate(DONOR, FOLLOWER, "strip", NOW), "Ordinary highest rank cannot adjudicate");
        model.assignAuthority(JAYCEE, false, NOW);
        var free = offer(model, proposal(JAYCEE, OTHER, 0, SECOND_POWER, "", false));
        check(free.fee() == 0, "Authority grants registered powers without fees or a prior bond");
        model.accept(OTHER, free.id(), NOW + 1, true);
        check(model.profile(OTHER).grants().get(SECOND_POWER).equals(JAYCEE.toString()), "Authority grant also records donor provenance");
    }

    private static void invalidProposalsAndCycles() {
        var model = ordinary(tuning());
        unchanged(model, () -> offer(model, invitation(DONOR, DONOR)), "Self bestowal rejected");
        unchanged(model, () -> offer(model, proposal(DONOR, FOLLOWER, 0, "", "", false)), "Empty request rejected");
        check(model.quote(proposal(DONOR, FOLLOWER, 1, "", "", false)) == 0,
            "A permitted donor can offer pure Essence without forcing a bond");
        for (var rank : List.of(Rank.MORTAL, Rank.BLESSED, Rank.DEMIGOD)) {
            model.adminRank(OTHER, rank, NOW);
            unchanged(model, () -> offer(model, invitation(OTHER, FOLLOWER)), "Below-Minor-God rank cannot bestow");
        }
        for (String name : List.of("bad\nname", "bad\u00a7name", "bad\u200bname", "x".repeat(49))) {
            rejected(() -> proposal(DONOR, FOLLOWER, 0, "", name, true), "Unsafe or overlong divine name rejected");
        }
        rejected(() -> proposal(DONOR, FOLLOWER, -1, "", "", true), "Negative transfer rejected");
        rejected(() -> proposal(DONOR, FOLLOWER, 0, "x".repeat(129), "", true), "Overlong ability ID rejected");
        bind(model, DONOR, FOLLOWER);
        model.adminRank(FOLLOWER, Rank.MINOR_GOD, NOW);
        model.adminBalance(FOLLOWER, false, model.config().essenceCap(), NOW);
        unchanged(model, () -> offer(model, invitation(FOLLOWER, DONOR)), "Direct patron cycle rejected");
        bind(model, FOLLOWER, OTHER);
        model.adminRank(OTHER, Rank.MINOR_GOD, NOW);
        model.adminBalance(OTHER, false, model.config().essenceCap(), NOW);
        unchanged(model, () -> offer(model, invitation(OTHER, DONOR)), "Transitive patron cycle rejected");
        unchanged(model, () -> offer(model, invitation(DONOR, OTHER)), "Existing patron must release before rebinding");
        check(model.followers(DONOR).stream().map(Profile::id).toList().equals(List.of(FOLLOWER)),
            "Follower listing reports direct lineage only");
        model.join(FOLLOWER, "Renamed");
        check(model.profile(FOLLOWER).originalName().equals("Follower") && model.profile(FOLLOWER).username().equals("Renamed")
            && DONOR.equals(model.profile(FOLLOWER).patron()), "Rejoin updates username without losing original name or bond");
    }

    private static void capLiftAndOverflow() {
        var model = supremeWorld();
        check(model.awardEp(FOLLOWER, Long.MAX_VALUE) == model.config().epCap(), "Ordinary EP credit clamps to configured cap");
        check(model.creditEssence(FOLLOWER, Long.MAX_VALUE) == model.config().essenceCap(), "Ordinary Essence credit clamps independently");
        check(model.awardEp(FOLLOWER, 1) == 0 && model.creditEssence(FOLLOWER, 1) == 0, "Full caps accept no additional credit");
        unchanged(model, () -> model.adminBalance(FOLLOWER, true, model.config().epCap() + 1, NOW), "Admin EP cannot bypass ordinary cap");
        unchanged(model, () -> model.adminBalance(FOLLOWER, false, model.config().essenceCap() + 1, NOW), "Admin Essence cannot bypass ordinary cap");
        unchanged(model, () -> model.awardEp(FOLLOWER, -1), "Negative EP award rejected");
        unchanged(model, () -> model.creditEssence(FOLLOWER, -1), "Negative Essence credit rejected");
        model.adjudicate(JAYCEE, FOLLOWER, "cap_lift", NOW);
        check(model.epCap(model.profile(FOLLOWER)) == Long.MAX_VALUE && model.essenceCap(model.profile(FOLLOWER)) == Long.MAX_VALUE,
            "Cap lift affects both separate resources");
        check(model.awardEp(FOLLOWER, Long.MAX_VALUE) == Long.MAX_VALUE - model.config().epCap(), "Lifted EP fills without overflow");
        check(model.creditEssence(FOLLOWER, Long.MAX_VALUE) == Long.MAX_VALUE - model.config().essenceCap(), "Lifted Essence fills without overflow");
        check(model.profile(FOLLOWER).ep() == Long.MAX_VALUE && model.profile(FOLLOWER).essence() == Long.MAX_VALUE,
            "Long maximum balances remain nonnegative");
        check(model.awardEp(FOLLOWER, Long.MAX_VALUE) == 0 && model.creditEssence(FOLLOWER, Long.MAX_VALUE) == 0,
            "Further Long maximum credits saturate safely");
        model.adjudicate(JAYCEE, FOLLOWER, "cap_revoke", NOW);
        check(model.profile(FOLLOWER).ep() == Long.MAX_VALUE && model.profile(FOLLOWER).essence() == Long.MAX_VALUE,
            "Cap revocation preserves historical balances");
        check(model.awardEp(FOLLOWER, 1) == 0 && model.creditEssence(FOLLOWER, 1) == 0,
            "Historical over-cap balances receive no new credit");
        check(model.epCap(model.profile(JAYCEE)) == Long.MAX_VALUE
            && model.essenceCap(model.profile(JAYCEE)) == model.config().supremeCap(), "Authority EP and Essence have separate caps");
        unchanged(model, () -> model.establishmentFee(Long.MAX_VALUE), "Unrepresentable percentage quote rejected, not wrapped");

        var overflow = ordinary(new Config(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, 0, 1, 100, 1, 1_000, 8));
        overflow.adminBalance(FOLLOWER, false, Long.MAX_VALUE, NOW);
        check(overflow.establishmentFee(Long.MAX_VALUE) == Long.MAX_VALUE, "BigInteger percentage can return exact Long maximum");
        unchanged(overflow, () -> offer(overflow, proposal(DONOR, FOLLOWER, 0, POWER, "", true)), "Fee plus power fee overflow rejects atomically");
        unchanged(overflow, () -> offer(overflow, proposal(DONOR, FOLLOWER, 1, "", "", true)), "Fee plus transfer overflow rejects atomically");
        overflow.adminBalance(FOLLOWER, false, 0, NOW);
        unchanged(overflow, () -> offer(overflow, proposal(DONOR, FOLLOWER, Long.MAX_VALUE, "", "", true)), "Transfer plus minimum fee overflow rejects atomically");
        overflow.assignAuthority(JAYCEE, false, NOW);
        overflow.adminBalance(JAYCEE, false, Long.MAX_VALUE, NOW);
        overflow.adminBalance(FOLLOWER, false, Long.MAX_VALUE - 1, NOW);
        unchanged(overflow, () -> offer(overflow, proposal(JAYCEE, FOLLOWER, 2, "", "", false)), "Recipient overflow rejected before addition");
        var exact = offer(overflow, proposal(JAYCEE, FOLLOWER, 1, "", "", false));
        overflow.accept(FOLLOWER, exact.id(), NOW + 1, true);
        check(overflow.profile(FOLLOWER).essence() == Long.MAX_VALUE && overflow.profile(JAYCEE).essence() == Long.MAX_VALUE - 1,
            "Transfer to exact Long maximum succeeds without overflow");
    }

    private static void releaseAndPetition() {
        var model = ordinary(tuning());
        model.assignAuthority(JAYCEE, false, NOW);
        unchanged(model, () -> model.requestRelease(FOLLOWER, NOW), "Unbound player cannot request release");
        bind(model, DONOR, FOLLOWER);
        unchanged(model, () -> model.petition(FOLLOWER, NOW), "Petition requires denied release");
        unchanged(model, () -> model.decideRelease(DONOR, FOLLOWER, true, NOW), "Decision requires release request");
        model.requestRelease(FOLLOWER, NOW);
        check(model.profile(FOLLOWER).release() == Release.REQUESTED && DONOR.equals(model.profile(FOLLOWER).patron()),
            "Request alone does not dissolve patron bond");
        unchanged(model, () -> model.requestRelease(FOLLOWER, NOW), "Duplicate request rejected");
        unchanged(model, () -> model.decideRelease(OTHER, FOLLOWER, true, NOW), "Unrelated player cannot decide release");
        model.decideRelease(DONOR, FOLLOWER, false, NOW);
        check(model.profile(FOLLOWER).release() == Release.DENIED && !model.challengeEligible(FOLLOWER), "Denial persists bond without automatic challenge");
        model.adminBalance(DONOR, true, 20, NOW);
        model.adminBalance(FOLLOWER, true, 20, NOW);
        check(!model.challengeEligible(FOLLOWER), "Equal EP does not qualify for challenge");
        model.adminBalance(FOLLOWER, true, 21, NOW);
        check(model.challengeEligible(FOLLOWER), "Strictly greater follower EP qualifies after denial");
        var before = model.profile(FOLLOWER);
        model.petition(FOLLOWER, NOW);
        check(model.profile(FOLLOWER).release() == Release.PETITIONED && DONOR.equals(model.profile(FOLLOWER).patron())
            && model.profile(FOLLOWER).ep() == before.ep() && model.profile(FOLLOWER).essence() == before.essence(),
            "Petition awaits adjudication and changes neither bond nor balances");
        unchanged(model, () -> model.petition(FOLLOWER, NOW), "Petition is not replayable");
        unchanged(model, () -> model.adjudicate(DONOR, FOLLOWER, "release", NOW), "Patron is not authority for adjudication");
        unchanged(model, () -> model.adjudicate(JAYCEE, FOLLOWER, "invented", NOW), "Unknown adjudication rejects without mutation");
        model.adjudicate(JAYCEE, FOLLOWER, "release", NOW);
        check(model.profile(FOLLOWER).patron() == null && model.profile(FOLLOWER).release() == null, "Authority adjudication releases petitioner");
        bind(model, DONOR, FOLLOWER);
        model.requestRelease(FOLLOWER, NOW);
        model.decideRelease(DONOR, FOLLOWER, true, NOW);
        check(model.profile(FOLLOWER).patron() == null && model.profile(FOLLOWER).rank() == Rank.BLESSED,
            "Patron approval releases without erasing earned rank");
        bind(model, JAYCEE, FOLLOWER);
        model.requestRelease(FOLLOWER, NOW);
        model.decideRelease(JAYCEE, FOLLOWER, false, NOW);
        check(!model.challengeEligible(FOLLOWER), "Authority patron cannot be challenged even with lower EP");
        model.decideRelease(JAYCEE, FOLLOWER, true, NOW);
        check(model.profile(FOLLOWER).patron() == null && model.profile(FOLLOWER).release() == null, "Authority can approve release");
    }

    private static void privateAuthorityView() {
        var model = ordinary(tuning());
        bind(model, DONOR, FOLLOWER);
        model.adminRank(FOLLOWER, Rank.OLYMPIAN, NOW);
        model.adminBalance(FOLLOWER, true, 123, NOW);
        model.adminBalance(FOLLOWER, false, 456, NOW);
        model.adminNpc(FOLLOWER, "zeus", true, NOW);
        model.adminGrant(FOLLOWER, POWER, true, NOW);
        var named = offer(model, proposal(DONOR, FOLLOWER, 0, "", "Secret", false));
        model.accept(FOLLOWER, named.id(), NOW + 1, true);
        model.requestRelease(FOLLOWER, NOW);
        model.assignAuthority(JAYCEE, false, NOW);
        model.adjudicate(JAYCEE, FOLLOWER, "cap_lift", NOW);
        model.assignAuthority(FOLLOWER, true, NOW);
        var hidden = model.view(OTHER, FOLLOWER);
        check(hidden.supreme() && hidden.id().equals(FOLLOWER) && hidden.username().equals("Follower")
            && hidden.originalName().equals("Follower"), "Public authority view preserves only public identity");
        check(hidden.ep() == null && hidden.essence() == null && hidden.rank() == Rank.MORTAL
            && hidden.divineName().isEmpty(), "Public authority view redacts balances, rank and divine name");
        check(!hidden.capLift() && !hidden.bestow() && hidden.patron() == null && hidden.release() == null
            && hidden.grants().isEmpty() && hidden.npcPatrons().isEmpty(), "Public authority view redacts lineage and privileges");
        var own = model.view(FOLLOWER, FOLLOWER);
        check(own.ep() == 123 && own.essence() == 456 && own.rank() == Rank.OLYMPIAN
            && own.divineName().equals("Secret") && own.capLift() && own.bestow(), "Authority sees own true profile");
        check(own.patron().equals(DONOR) && own.release() == Release.REQUESTED
            && own.npcPatrons().equals(Set.of("zeus")) && own.grants().equals(Map.of(POWER, "operator")),
            "Authority sees own private lineage and provenance");
        check(model.view(OTHER, DONOR).ep().equals(model.profile(DONOR).ep()), "Ordinary profiles retain nonprivate balances");
        check(model.history(FOLLOWER).equals(model.image().audit()), "Assigned authority sees world audit");
        check(model.history(OTHER).stream().allMatch(a -> OTHER.equals(a.actor()) || OTHER.equals(a.target())),
            "Ordinary audit limited to involved player");
    }

    private static void persistenceRoundTrip() {
        var model = ordinary(tuning());
        model.assignAuthority(JAYCEE, false, NOW);
        bind(model, DONOR, FOLLOWER);
        var named = offer(model, proposal(DONOR, FOLLOWER, 0, "", "Dawn", false));
        model.accept(FOLLOWER, named.id(), NOW + 1, true);
        model.requestRelease(FOLLOWER, NOW);
        model.decideRelease(DONOR, FOLLOWER, false, NOW);
        model.petition(FOLLOWER, NOW);
        model.adminNpc(FOLLOWER, "hades", true, NOW);
        model.migrateRestore(OTHER, POWER, NOW);
        model.adjudicate(JAYCEE, FOLLOWER, "cap_lift", NOW);
        model.adminBalance(JAYCEE, false, 77, NOW);
        model.tickRegeneration();
        var pending = offer(model, proposal(DONOR, FOLLOWER, 13, POWER, "AfterRestart", false));
        var image = model.image();
        var restored = world();
        restored.restore(image, NOW + 2);
        check(restored.authority().equals(image.authority()) && restored.image().profiles().equals(image.profiles()),
            "Image roundtrip retains authority and all profile fields");
        check(restored.image().carry() == image.carry(), "Image roundtrip preserves fractional refill carry");
        check(restored.pending(DONOR).isEmpty() && restored.image().offers().isEmpty(), "Restart deliberately cancels pending offers");
        check(restored.image().audit().size() == image.audit().size() + 1
            && restored.history(JAYCEE).getLast().text().contains(pending.id().toString()), "Restart cancellation is audited");
        unchanged(restored, () -> restored.accept(FOLLOWER, pending.id(), NOW + 3, true), "Persisted offer cannot replay after restart");
        model.tickRegeneration();
        restored.tickRegeneration();
        check(restored.profile(JAYCEE).equals(model.profile(JAYCEE)) && restored.image().carry() == model.image().carry(),
            "Restored refill resumes exactly from saved carry");
        check(image.offers().size() == 1 && !image.profiles().equals(restored.image().profiles()),
            "Saved Image is a detached snapshot, not a live aggregate");
        for (int carry : new int[]{-1, 20, Integer.MAX_VALUE}) {
            restored.restore(new Image(new UUID(0, 999), image.profiles(), List.of(), image.audit(), carry), NOW);
            check(restored.authority() == null && restored.image().carry() == Math.clamp(carry, 0, 19),
                "Restore removes absent authority and bounds invalid carry");
        }
        for (int i = 0; i < 300; i++) model.adminRank(OTHER, Rank.MORTAL, NOW + i);
        check(model.image().audit().size() == 256, "Audit retains only last 256 events");
        restored.restore(model.image(), NOW);
        check(restored.image().audit().size() == 256, "Restart offer cancellation also respects audit bound");
    }

    private static void recipientPrivateGrantFiltering() {
        var model = ordinary(tuning());
        var granted = offer(model, proposal(DONOR, FOLLOWER, 0, POWER, "", true));
        model.accept(FOLLOWER, granted.id(), NOW + 1, true);
        model.adminGrant(FOLLOWER, SECOND_POWER, true, NOW);
        model.assignAuthority(JAYCEE, false, NOW);
        var actualGrants = Map.of(POWER, DONOR.toString(), SECOND_POWER, "operator");
        var beforeViews = model.image();
        check(model.view(FOLLOWER, FOLLOWER).grants().equals(actualGrants),
            "Recipient sees all own powers and their actual provenance");
        check(model.view(OTHER, FOLLOWER).grants().isEmpty(),
            "Unrelated observer learns no powers they do not own");
        var shared = model.view(DONOR, FOLLOWER);
        check(shared.grants().equals(Map.of(POWER, DONOR.toString())),
            "Donor sees only recipient powers the donor owns, preserving recipient provenance");
        check(model.view(JAYCEE, FOLLOWER).grants().equals(actualGrants),
            "Assigned authority can see registered recipient powers through implicit ownership");
        check(model.image().equals(beforeViews), "Filtered views do not mutate ownership or audit");
        model.adminGrant(DONOR, SECOND_POWER, true, NOW);
        check(model.view(DONOR, FOLLOWER).grants().equals(actualGrants),
            "New viewer ownership reveals matching recipient power without changing its provenance");
        check(shared.grants().equals(Map.of(POWER, DONOR.toString())), "Previously returned grant view remains a snapshot");
        model.adminGrant(DONOR, POWER, false, NOW);
        check(model.view(DONOR, FOLLOWER).grants().equals(Map.of(SECOND_POWER, "operator")),
            "Viewer revocation immediately hides unowned recipient powers");
        check(model.profile(FOLLOWER).grants().equals(actualGrants) && model.owns(FOLLOWER, POWER),
            "Viewer revocation and filtering never revoke recipient powers");
        model.assignAuthority(FOLLOWER, true, NOW);
        check(model.view(DONOR, FOLLOWER).grants().isEmpty(),
            "Authority masking overrides shared-power visibility");
        check(model.view(FOLLOWER, FOLLOWER).grants().equals(actualGrants),
            "Authority still sees own complete provenance");
    }

    private static void refillLimits() {
        for (long rate : new long[]{0, 1, 19, 20, 21, 37, 1_000_000, Long.MAX_VALUE}) {
            var config = new Config(100, 200, Long.MAX_VALUE, rate, 1, 125, 1, 1_000, 8);
            var model = world(config);
            model.assignAuthority(JAYCEE, false, NOW);
            model.adminBalance(DONOR, false, 13, NOW);
            model.adminBalance(JAYCEE, true, 17, NOW);
            for (int i = 0; i < 20; i++) model.tickRegeneration();
            check(model.profile(JAYCEE).essence() == rate && model.image().carry() == 0,
                "Twenty ticks refill exactly configured per-second amount: " + rate);
            check(model.profile(JAYCEE).ep() == 17 && model.profile(DONOR).essence() == 13,
                "Refill changes only authority Essence");
        }
        var model = supremeWorld();
        model.adminBalance(JAYCEE, false, model.config().supremeCap() - 1, NOW);
        for (int i = 0; i < 40; i++) model.tickRegeneration();
        check(model.profile(JAYCEE).essence() == model.config().supremeCap(), "Refill clamps to authority Essence cap");
        var transfer = offer(model, proposal(JAYCEE, FOLLOWER, 10, "", "", false));
        model.accept(FOLLOWER, transfer.id(), NOW + 1, true);
        check(model.profile(JAYCEE).essence() == model.config().supremeCap() - 10, "Authority transfer spends actual balance");
        for (int i = 0; i < 20; i++) model.tickRegeneration();
        check(model.profile(JAYCEE).essence() == model.config().supremeCap(), "Spent Essence refills, never exceeds cap");
        model.adminBalance(JAYCEE, false, 0, NOW);
        unchanged(model, () -> offer(model, proposal(JAYCEE, OTHER, 1, "", "", false)), "Authority is not an infinite immediate balance");
    }

    private static void pendingOfferLimits() {
        var model = supremeWorld();
        for (int i = 0; i < 16; i++) offer(model, invitation(JAYCEE, FOLLOWER));
        unchanged(model, () -> offer(model, invitation(JAYCEE, OTHER)), "Per-donor pending offer limit enforced");
        var first = model.pending(JAYCEE).getFirst();
        model.cancel(JAYCEE, first.id(), NOW);
        offer(model, invitation(JAYCEE, OTHER));
        check(model.pending(JAYCEE).size() == 16, "Resolving offer frees one slot");
        model.expire(NOW + model.config().offerMillis());
        check(model.pending(JAYCEE).isEmpty(), "Expiry frees pending capacity");

        var many = world();
        for (int i = 0; i < 17; i++) {
            UUID donor = new UUID(1, i);
            many.join(donor, "Donor" + i);
            many.adminRank(donor, Rank.MINOR_GOD, NOW);
            many.adminBalance(donor, false, many.config().essenceCap(), NOW);
            if (i < 16) offer(many, invitation(donor, FOLLOWER));
            else unchanged(many, () -> offer(many, invitation(donor, FOLLOWER)), "Per-recipient pending limit enforced across donors");
        }
        var global = world();
        for (int i = 0; i < 16; i++) {
            UUID donor = new UUID(2, i);
            global.join(donor, "Donor" + i);
            global.adminRank(donor, Rank.MINOR_GOD, NOW);
            global.adminBalance(donor, false, global.config().essenceCap(), NOW);
            for (int j = 0; j < 16; j++) {
                UUID recipient = new UUID(3, i * 16L + j);
                global.join(recipient, "Recipient" + j);
                offer(global, invitation(donor, recipient));
            }
        }
        global.adminRank(DONOR, Rank.MINOR_GOD, NOW);
        global.adminBalance(DONOR, false, global.config().essenceCap(), NOW);
        unchanged(global, () -> offer(global, invitation(DONOR, FOLLOWER)), "Global 256 pending offer limit enforced");
        global.expire(NOW + global.config().offerMillis());
        offer(global, invitation(DONOR, FOLLOWER));
        check(global.image().offers().size() == 1, "Expired global offers do not permanently exhaust capacity");
    }

    public static void main(String[] args) {
        run(DivinityModelTest::uniqueAuthority);
        run(DivinityModelTest::authorityChangesCancelOffers);
        run(DivinityModelTest::ordinaryEstablishmentAndAtomicAcceptance);
        run(DivinityModelTest::insufficientAndStaleOffers);
        run(DivinityModelTest::consentLifecycleAndReachability);
        run(DivinityModelTest::grantsAndProvenance);
        run(DivinityModelTest::invalidProposalsAndCycles);
        run(DivinityModelTest::capLiftAndOverflow);
        run(DivinityModelTest::releaseAndPetition);
        run(DivinityModelTest::privateAuthorityView);
        run(DivinityModelTest::recipientPrivateGrantFiltering);
        run(DivinityModelTest::persistenceRoundTrip);
        run(DivinityModelTest::refillLimits);
        run(DivinityModelTest::pendingOfferLimits);
        System.out.println("DivinityModelTest: " + scenarios + " scenarios, " + assertions + " assertions passed");
    }
}
