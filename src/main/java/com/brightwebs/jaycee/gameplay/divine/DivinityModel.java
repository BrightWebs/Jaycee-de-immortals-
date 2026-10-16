package com.brightwebs.jaycee.gameplay.divine;

import java.math.BigInteger;
import java.util.*;

/** Single-threaded world aggregate. All mutations validate fully before committing. No Minecraft dependencies. */
public final class DivinityModel {
    public enum Rank { MORTAL, BLESSED, DEMIGOD, MINOR_GOD, MAJOR_GOD, OLYMPIAN }
    public enum Release { REQUESTED, DENIED, PETITIONED }
    public record Config(long epCap, long essenceCap, long supremeCap, long regenPerSecond,
                         long minimumFee, int feePercent, long powerFee, long offerMillis, double distance) {
        public Config {
            if (epCap < 1 || essenceCap < 1 || supremeCap < 1 || regenPerSecond < 0
                    || minimumFee < 1 || feePercent < 1 || powerFee < 1 || offerMillis < 1000
                    || !Double.isFinite(distance) || distance < 1 || distance > 32)
                throw new IllegalArgumentException("Invalid divinity tuning");
        }
        public static Config defaults() { return new Config(3_000_000, 3_000_000, 2_000_000_000,
            1_000_000, 100, 125, 100, 120_000, 8); }
    }
    public record Profile(UUID id, String originalName, String username, String divineName, Rank rank,
                          long ep, long essence, boolean capLift, boolean bestow, UUID patron,
                          Set<String> npcPatrons, Map<String, String> grants, Release release) {
        public Profile {
            Objects.requireNonNull(id); Objects.requireNonNull(rank);
            npcPatrons = Set.copyOf(npcPatrons); grants = Map.copyOf(grants);
            if (ep < 0 || essence < 0) throw new IllegalArgumentException("Negative balance");
        }
        public String displayName() { return divineName.isEmpty() ? username : divineName; }
    }
    public record Proposal(UUID donor, UUID recipient, long amount, String ability, String name, boolean bind) {
        public Proposal {
            Objects.requireNonNull(donor); Objects.requireNonNull(recipient);
            Objects.requireNonNull(ability); Objects.requireNonNull(name);
            if (amount < 0 || ability.length() > 128 || !validName(name))
                throw new IllegalArgumentException("Use a nonnegative amount and a plain name of at most 48 characters");
        }
    }
    public record Offer(UUID id, Proposal proposal, long fee, long expires) {}
    public record Audit(long time, UUID actor, UUID target, String text) {}
    public record View(UUID id, String originalName, String username, String divineName, Rank rank,
                       Long ep, Long essence, boolean supreme, boolean capLift, boolean bestow,
                       UUID patron, Set<String> npcPatrons, Map<String, String> grants, Release release) {}
    public record Image(UUID authority, List<Profile> profiles, List<Offer> offers, List<Audit> audit, int carry) {}
    public static final Set<String> NPC_PATRONS = Set.of("zeus", "poseidon", "hades", "athena");
    private final Config config;
    private final Set<String> abilities;
    private final Map<UUID, Profile> profiles = new LinkedHashMap<>();
    private final Map<UUID, Offer> offers = new LinkedHashMap<>();
    private final Deque<Audit> audit = new ArrayDeque<>();
    private UUID authority;
    private int carry;
    public DivinityModel(Config config, Set<String> abilities) {
        this.config = Objects.requireNonNull(config); this.abilities = Set.copyOf(abilities);
    }
    public Config config() { return config; }
    public boolean supreme(UUID id) { return id != null && id.equals(authority); }
    public UUID authority() { return authority; }
    public Profile profile(UUID id) {
        var result = profiles.get(id);
        if (result == null) throw new IllegalArgumentException("Player has not joined this world");
        return result;
    }
    public Profile join(UUID id, String username) {
        if (username == null || username.isEmpty() || username.length() > 16)
            throw new IllegalArgumentException("Invalid Minecraft profile");
        var p = profiles.get(id);
        if (p == null) p = new Profile(id, username, username, "", Rank.MORTAL, 0, 0,
            false, false, null, Set.of(), Map.of(), null);
        else p = copy(p, username, p.divineName, p.rank, p.ep, p.essence, p.capLift, p.bestow,
            p.patron, p.npcPatrons, p.grants, p.release);
        profiles.put(id, p);
        return p;
    }
    /** Only the server's operator command calls this; test access and client snapshots are not authority. */
    public void assignAuthority(UUID id, boolean replace, long now) {
        if (id != null) profile(id);
        if (authority != null && !replace) fail("Jaycee authority is already assigned");
        UUID old = authority;
        authority = id; carry = 0;
        offers.clear();
        log(now, id, old, "Operator changed unique Jaycee authority; pending offers canceled");
    }
    public boolean owns(UUID id, String ability) {
        return abilities.contains(ability) && (supreme(id) || profile(id).grants.containsKey(ability));
    }
    public void adminGrant(UUID id, String ability, boolean granted, long now) {
        if (!abilities.contains(ability)) fail("Only implemented registered abilities may be granted");
        var p = profile(id); var grants = new HashMap<>(p.grants);
        if (granted) grants.put(ability, "operator"); else grants.remove(ability);
        put(p, p.divineName, p.rank, p.ep, p.essence, p.capLift, p.bestow, p.patron, p.npcPatrons, grants, p.release);
        log(now, id, id, "Operator " + (granted ? "granted " : "revoked ") + ability);
    }
    public void migrateRestore(UUID id, String restore, long now) {
        if (!profile(id).grants.containsKey(restore)) {
            adminGrant(id, restore, true, now);
            var p = profile(id); var grants = new HashMap<>(p.grants); grants.put(restore, "legacy:three_essences");
            put(p, p.divineName, p.rank, p.ep, p.essence, p.capLift, p.bestow, p.patron, p.npcPatrons, grants, p.release);
        }
    }
    public void adminRank(UUID id, Rank rank, long now) {
        var p = profile(id);
        put(p, p.divineName, rank, p.ep, p.essence, p.capLift, rank.ordinal() >= Rank.MINOR_GOD.ordinal(),
            p.patron, p.npcPatrons, p.grants, p.release);
        log(now, id, id, "Operator testing rank: " + rank);
    }
    public void adminBalance(UUID id, boolean ep, long value, long now) {
        if (value < 0) fail("Balance cannot be negative");
        var p = profile(id);
        if (value > (ep ? epCap(p) : essenceCap(p))) fail("Balance exceeds current cap");
        put(p, p.divineName, p.rank, ep ? value : p.ep, ep ? p.essence : value,
            p.capLift, p.bestow, p.patron, p.npcPatrons, p.grants, p.release);
        log(now, id, id, "Operator testing " + (ep ? "EP" : "Essence") + ": " + value);
    }
    public void adminNpc(UUID id, String npc, boolean add, long now) {
        if (!NPC_PATRONS.contains(npc)) fail("Unsupported patron metadata");
        var p = profile(id); var patrons = new HashSet<>(p.npcPatrons);
        if (add) patrons.add(npc); else patrons.remove(npc);
        put(p, p.divineName, p.rank, p.ep, p.essence, p.capLift, p.bestow, p.patron, patrons, p.grants, p.release);
        log(now, id, id, "Operator testing NPC lineage " + npc + ": " + add + "; no offering powers assigned");
    }
    /** Server award hooks; caps constrain new credits, never erase historical EP. */
    public long awardEp(UUID id, long amount) { return credit(id, amount, true); }
    public long creditEssence(UUID id, long amount) { return credit(id, amount, false); }
    private long credit(UUID id, long amount, boolean ep) {
        if (amount < 0) fail("Credit cannot be negative");
        var p = profile(id); long old = ep ? p.ep : p.essence;
        long added = Math.min(amount, Math.max(0, (ep ? epCap(p) : essenceCap(p)) - old));
        put(p, p.divineName, p.rank, ep ? old + added : p.ep, ep ? p.essence : old + added,
            p.capLift, p.bestow, p.patron, p.npcPatrons, p.grants, p.release);
        return added;
    }
    public long epCap(Profile p) { return p.capLift || supreme(p.id) ? Long.MAX_VALUE : config.epCap; }
    public long essenceCap(Profile p) { return supreme(p.id) ? config.supremeCap : p.capLift ? Long.MAX_VALUE : config.essenceCap; }
    public void tickRegeneration() {
        if (authority == null) return;
        long whole = config.regenPerSecond / 20;
        carry += (int)(config.regenPerSecond % 20);
        if (carry >= 20) { whole++; carry -= 20; }
        creditEssence(authority, whole);
    }
    public long establishmentFee(long essence) {
        if (essence < 0) fail("Negative Essence");
        return Math.max(config.minimumFee, exact(BigInteger.valueOf(essence)
            .multiply(BigInteger.valueOf(config.feePercent)).add(BigInteger.valueOf(99)).divide(BigInteger.valueOf(100))));
    }
    public long quote(Proposal proposal) {
        var donor = profile(proposal.donor); var recipient = profile(proposal.recipient);
        if (donor.id.equals(recipient.id)) fail("You cannot bestow upon yourself");
        if (supreme(recipient.id)) fail("Jaycee's true profile cannot be targeted");
        if (!supreme(donor.id) && (!donor.bestow || donor.rank.ordinal() < Rank.MINOR_GOD.ordinal()))
            fail("Bestowal requires Minor God rank and bestowal privileges");
        if (proposal.amount == 0 && proposal.ability.isEmpty() && proposal.name.isEmpty() && !proposal.bind)
            fail("Choose Essence, an owned power, a name, or an invitation");
        if (proposal.bind) {
            if (recipient.patron != null) fail("Recipient must be released before accepting a new patron");
            if (cycle(donor.id, recipient.id)) fail("This bond would create a patron cycle");
        } else if (!supreme(donor.id) && !donor.id.equals(recipient.patron)) {
            fail("Invite a new follower explicitly, or select your existing follower");
        }
        if (!proposal.ability.isEmpty()) {
            if (!abilities.contains(proposal.ability) || !owns(donor.id, proposal.ability))
                fail("You can only grant implemented powers you own");
            if (recipient.grants.containsKey(proposal.ability)) fail("Recipient already owns this power");
        }
        long fee = supreme(donor.id) ? 0 : exact(BigInteger.valueOf(proposal.bind ? establishmentFee(recipient.essence) : 0)
            .add(BigInteger.valueOf(proposal.ability.isEmpty() ? 0 : config.powerFee)));
        long debit = exact(BigInteger.valueOf(fee).add(BigInteger.valueOf(proposal.amount)));
        if (donor.essence < debit) fail("Insufficient Essence for the transfer plus burned establishment / power fee");
        if (proposal.amount > Math.max(0, essenceCap(recipient) - recipient.essence)) fail("Recipient Essence cap would be exceeded");
        return fee;
    }
    private boolean cycle(UUID donor, UUID recipient) {
        Set<UUID> seen = new HashSet<>();
        while (donor != null) {
            if (donor.equals(recipient) || !seen.add(donor)) return true;
            donor = profile(donor).patron;
        }
        return false;
    }
    public Offer offer(Proposal proposal, long now, boolean reachable) {
        if (!reachable) fail("Both players must be online, within range and visible");
        expire(now);
        if (offers.size() >= 256 || offers.values().stream().filter(o ->
                o.proposal.donor.equals(proposal.donor) || o.proposal.recipient.equals(proposal.recipient)).count() >= 16)
            fail("Too many pending offers; resolve an existing offer first");
        long fee = quote(proposal);
        var offer = new Offer(UUID.randomUUID(), proposal, fee, Math.addExact(now, config.offerMillis));
        offers.put(offer.id, offer);
        log(now, proposal.donor, proposal.recipient, "Offered consent-based bestowal " + offer.id);
        return offer;
    }
    public void accept(UUID recipientId, UUID offerId, long now, boolean reachable) {
        expire(now);
        var o = offers.get(offerId);
        if (o == null || !o.proposal.recipient.equals(recipientId)) fail("Offer no longer exists or is not yours");
        if (!reachable) fail("Both players must be online, within range and visible");
        var s = o.proposal;
        long fee = quote(s);
        if (fee != o.fee) fail("Eligibility or price changed; request a fresh offer");
        var donor = profile(s.donor); var recipient = profile(recipientId);
        long debit = Math.addExact(fee, s.amount);
        var grants = new HashMap<>(recipient.grants);
        if (!s.ability.isEmpty()) grants.put(s.ability, donor.id.toString());
        // Every possible validation and overflow check precedes either balance mutation.
        long balance = Math.addExact(recipient.essence, s.amount);
        put(donor, donor.divineName, donor.rank, donor.ep, donor.essence - debit, donor.capLift, donor.bestow,
            donor.patron, donor.npcPatrons, donor.grants, donor.release);
        put(recipient, s.name.isEmpty() ? recipient.divineName : s.name,
            s.bind && recipient.rank == Rank.MORTAL ? Rank.BLESSED : recipient.rank,
            recipient.ep, balance, recipient.capLift, recipient.bestow,
            s.bind ? donor.id : recipient.patron, recipient.npcPatrons, grants, recipient.release);
        offers.remove(offerId);
        log(now, donor.id, recipientId, "Accepted " + offerId + "; transferred " + s.amount
            + " Essence; fee burned " + fee + "; power " + s.ability + "; binding " + s.bind);
    }
    public void cancel(UUID actor, UUID id, long now) {
        var o = offers.get(id);
        if (o == null || (!actor.equals(o.proposal.donor) && !actor.equals(o.proposal.recipient))) fail("Offer is not yours");
        offers.remove(id); log(now, actor, o.proposal.recipient, "Offer declined / canceled " + id);
    }
    public void expire(long now) {
        var expired = offers.values().stream().filter(o -> o.expires <= now).toList();
        for (var o : expired) { offers.remove(o.id); log(now, o.proposal.donor, o.proposal.recipient, "Offer expired " + o.id); }
    }
    public void disconnect(UUID id, long now) {
        for (var o : List.copyOf(offers.values())) if (o.proposal.donor.equals(id) || o.proposal.recipient.equals(id)) {
            offers.remove(o.id); log(now, id, o.proposal.recipient, "Offer canceled: player offline");
        }
    }
    public void requestRelease(UUID id, long now) {
        var p = profile(id);
        if (p.patron == null || p.release != null) fail("No active bond, or a release request already exists");
        put(p, p.divineName, p.rank, p.ep, p.essence, p.capLift, p.bestow, p.patron, p.npcPatrons, p.grants, Release.REQUESTED);
        log(now, id, p.patron, "Release requested");
    }
    public void decideRelease(UUID actor, UUID id, boolean approve, long now) {
        var p = profile(id);
        if (!supreme(actor) && !actor.equals(p.patron)) fail("Only the patron or Jaycee may decide release");
        if (p.release == null) fail("No pending release request");
        put(p, p.divineName, p.rank, p.ep, p.essence, p.capLift, p.bestow, approve ? null : p.patron,
            p.npcPatrons, p.grants, approve ? null : Release.DENIED);
        log(now, actor, id, approve ? "Release approved" : "Release denied");
    }
    public boolean challengeEligible(UUID id) {
        var p = profile(id);
        return p.release == Release.DENIED && p.patron != null && p.ep > profile(p.patron).ep && !supreme(p.patron);
    }
    public void petition(UUID id, long now) {
        var p = profile(id);
        if (p.release != Release.DENIED) fail("A denied release is required before petitioning");
        put(p, p.divineName, p.rank, p.ep, p.essence, p.capLift, p.bestow, p.patron, p.npcPatrons, p.grants, Release.PETITIONED);
        log(now, id, authority, "Petition for release; combat / leadership resolution awaits adjudication");
    }
    public void adjudicate(UUID actor, UUID id, String operation, long now) {
        if (!supreme(actor)) fail("Only the assigned Jaycee authority may adjudicate");
        if (supreme(id)) fail("Use operator authority commands to change Jaycee authority");
        var p = profile(id);
        String name = p.divineName; Rank rank = p.rank; boolean cap = p.capLift, bestow = p.bestow;
        UUID patron = p.patron; Release release = p.release; Map<String, String> grants = p.grants;
        switch (operation) {
            case "cap_lift" -> cap = true;
            case "cap_revoke" -> cap = false;
            case "revoke_privileges" -> bestow = false;
            case "demote" -> { rank = Rank.values()[Math.max(0, rank.ordinal() - 1)]; bestow = false; }
            case "strip" -> { rank = Rank.MORTAL; bestow = false; cap = false; grants = Map.of(); name = ""; }
            case "release" -> { patron = null; release = null; }
            default -> fail("Unknown adjudication");
        }
        put(p, name, rank, p.ep, p.essence, cap, bestow, patron, p.npcPatrons, grants, release);
        log(now, actor, id, "Confirmed adjudication: " + operation);
    }
    public View view(UUID viewer, UUID target) {
        var p = profile(target); boolean hidden = supreme(target) && !supreme(viewer);
        var visibleGrants = new HashMap<String, String>();
        p.grants.forEach((ability, source) -> {
            if (viewer.equals(target) || owns(viewer, ability)) visibleGrants.put(ability, source);
        });
        return new View(p.id, p.originalName, p.username, hidden ? "" : p.divineName, hidden ? Rank.MORTAL : p.rank,
            hidden ? null : p.ep, hidden ? null : p.essence, supreme(target), !hidden && p.capLift, !hidden && p.bestow,
            hidden ? null : p.patron, hidden ? Set.of() : p.npcPatrons, hidden ? Map.of() : Map.copyOf(visibleGrants), hidden ? null : p.release);
    }
    public List<Profile> followers(UUID id) { return profiles.values().stream().filter(p -> id.equals(p.patron)).toList(); }
    public List<Offer> pending(UUID id) { return offers.values().stream().filter(o ->
        o.proposal.donor.equals(id) || o.proposal.recipient.equals(id)).toList(); }
    public List<Audit> history(UUID id) { return audit.stream().filter(a -> supreme(id) || id.equals(a.actor) || id.equals(a.target)).toList(); }
    public Image image() { return new Image(authority, List.copyOf(profiles.values()), List.copyOf(offers.values()), List.copyOf(audit), carry); }
    public void restore(Image image, long now) {
        profiles.clear(); offers.clear(); audit.clear();
        for (var p : image.profiles) profiles.put(p.id, p);
        authority = profiles.containsKey(image.authority) ? image.authority : null;
        carry = Math.clamp(image.carry, 0, 19);
        image.audit.stream().skip(Math.max(0, image.audit.size() - 256)).forEach(audit::addLast);
        for (var o : image.offers) log(now, o.proposal.donor, o.proposal.recipient, "Pending offer canceled on server restart " + o.id);
    }
    public static boolean validName(String name) {
        return name != null && name.length() <= 48 && name.codePoints().noneMatch(c -> Character.isISOControl(c)
            || c == 0x00a7 || Character.getType(c) == Character.FORMAT);
    }
    private static long exact(BigInteger value) {
        try { return value.longValueExact(); } catch (ArithmeticException e) { throw new IllegalArgumentException("Quote exceeds supported balance range"); }
    }
    private static void fail(String message) { throw new IllegalArgumentException(message); }
    private void log(long now, UUID actor, UUID target, String text) {
        if (audit.size() == 256) audit.removeFirst();
        audit.addLast(new Audit(now, actor, target, text));
    }
    private static Profile copy(Profile p, String username, String name, Rank rank, long ep, long essence,
                                boolean cap, boolean bestow, UUID patron, Set<String> npc, Map<String, String> grants, Release release) {
        return new Profile(p.id, p.originalName, username, name, rank, ep, essence, cap, bestow, patron, npc, grants, release);
    }
    private void put(Profile p, String name, Rank rank, long ep, long essence, boolean cap, boolean bestow,
                     UUID patron, Set<String> npc, Map<String, String> grants, Release release) {
        profiles.put(p.id, copy(p, p.username, name, rank, ep, essence, cap, bestow, patron, npc, grants, release));
    }
}
