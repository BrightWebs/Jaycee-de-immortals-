package com.brightwebs.jaycee.gameplay.divine;

import com.brightwebs.jaycee.gameplay.*;
import com.mojang.brigadier.arguments.*;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** All callers run on the logical server thread. No client role, quoted price or balance is trusted. */
public final class DivinityService {
    private DivinityService() {}
    public static void register() {
        ModList.get().getModContainerById("jaycee").orElseThrow()
            .registerConfig(ModConfig.Type.SERVER, DivinityConfig.SPEC, "jaycee-divinity-server.toml");
        MobDivinityService.register();
    }
    public static DivinityModel model(ServerPlayer p) { return DivinitySavedData.get(p.server).model; }
    private static long now() { return System.currentTimeMillis(); }
    public static void join(ServerPlayer p) {
        var data = DivinitySavedData.get(p.server);
        data.model.join(p.getUUID(), p.getGameProfile().getName());
        unlockRestore(p);
        checkCoreAwakening(p);
        // Migrate trusted pre-ledger operator grants without treating JayceeTest as authority.
        var legacy = p.getPersistentData().getCompound(PlayerState.TAG);
        if (!legacy.getBoolean("DivinityMigrated")) {
            for (var skill : SkillDefinitions.ALL) if (skill.intrinsic()
                    && legacy.getCompound("Skills").getCompound(skill.id()).getBoolean("Granted"))
                data.model.adminGrant(p.getUUID(), skill.id(), true, now());
            legacy.putBoolean("DivinityMigrated", true);
            p.getPersistentData().put(PlayerState.TAG, legacy);
        }
        data.setDirty();
    }
    public static void unlockRestore(ServerPlayer p) {
        var legacy = p.getPersistentData().getCompound(PlayerState.TAG);
        if (PlayerState.get(p).consumed() >= 3 && !legacy.getBoolean("DivinityRestoreMigrated")) {
            var data = DivinitySavedData.get(p.server);
            data.model.migrateRestore(p.getUUID(), SkillDefinitions.RESTORE, now());
            legacy.putBoolean("DivinityRestoreMigrated", true);
            p.getPersistentData().put(PlayerState.TAG, legacy);
            data.setDirty();
        }
    }
    /**
     * Claims the unique world-level Jaycee role on the third core.
     * Server operators never obtain this role just by being operators.
     * Optional UUID override: -Djaycee.owner_uuid=<uuid>
     */
    public static void checkCoreAwakening(ServerPlayer p) {
        if (PlayerState.get(p).consumed() < 3) return;
        var d = DivinitySavedData.get(p.server);
        String configured = System.getProperty("jaycee.owner_uuid", "").trim();
        boolean designated = false;
        if (!configured.isEmpty()) {
            try { designated = java.util.UUID.fromString(configured).equals(p.getUUID()); }
            catch (IllegalArgumentException ignored) {}
        }
        try {
            if (designated && !d.model.supreme(p.getUUID())) {
                d.model.assignAuthority(p.getUUID(), true, now());
                d.setDirty();
            } else if (d.model.authority() == null) {
                d.model.assignAuthority(p.getUUID(), false, now());
                d.setDirty();
            }
            if (d.model.supreme(p.getUUID())) {
                var entry = p.server.getAdvancements().get(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("jaycee", "jaycee"));
                if (entry != null && !p.getAdvancements().getOrStartProgress(entry).isDone())
                    p.getAdvancements().award(entry, "claimed");
            }
        } catch (IllegalArgumentException ex) {
            p.sendSystemMessage(Component.literal("Jaycee claim unavailable: " + ex.getMessage()));
        }
    }

    public static void logout(ServerPlayer p) {
        var data = DivinitySavedData.get(p.server); data.model.disconnect(p.getUUID(), now());
        data.sessions.remove(p.getUUID()); data.setDirty();
    }
    public static void tick(ServerPlayer p) {
        var data = DivinitySavedData.get(p.server);
        if (data.model.supreme(p.getUUID())) { data.model.tickRegeneration(); data.setDirty(); }
        if (p.tickCount % 20 == 0) { data.model.expire(now()); data.setDirty(); }
    }
    /** Award a legitimate, one-time Olympian blessing only after a validated Totem draw. */
    public static void olympianBless(ServerPlayer player, String patron) {
        var data = DivinitySavedData.get(player.server);
        data.model.acceptNpcBlessing(player.getUUID(), patron, now());
        data.model.creditEssence(player.getUUID(), 250);
        data.setDirty();
        GameplayNetwork.sync(player);
    }
    public static long awardEp(ServerPlayer p, long amount) {
        var data = DivinitySavedData.get(p.server); long added = data.model.awardEp(p.getUUID(), amount);
        data.setDirty(); GameplayNetwork.sync(p); return added;
    }
    public static long creditEssence(ServerPlayer p, long amount) {
        var data = DivinitySavedData.get(p.server); long added = data.model.creditEssence(p.getUUID(), amount);
        data.setDirty(); GameplayNetwork.sync(p); return added;
    }
    public static void adminGrant(ServerPlayer p, String id, boolean granted) {
        var data = DivinitySavedData.get(p.server); data.model.adminGrant(p.getUUID(), id, granted, now()); data.setDirty();
    }
    /** Called only by the operator-gated command branch, never by gameplay payloads. */
    public static void commandEdit(ServerPlayer target, String operation, String value, long amount) {
        var data = DivinitySavedData.get(target.server);
        var model = data.model;
        var id = target.getUUID();
        switch (operation) {
            case "essence_add" -> model.adminAddEssence(id, amount, now());
            case "essence_set" -> model.adminSetEssence(id, amount, now());
            case "ep_set" -> model.adminSetEp(id, amount, now());
            case "level_set" -> model.adminSetLevel(id, Math.toIntExact(amount), now());
            case "grade_recalc" -> model.progression(id);
            case "patron_grant", "patron_revoke" -> model.adminNpc(id, value, operation.equals("patron_grant"), now());
            case "ability_grant", "ability_revoke" -> model.adminGrant(id, value, operation.equals("ability_grant"), now());
            default -> throw new IllegalArgumentException("Unknown administrative edit");
        }
        data.setDirty();
        for (var player : target.server.getPlayerList().getPlayers()) {
            SkillRuntime.invalidate(player);
            GameplayNetwork.sync(player);
        }
    }
    public static CompoundTag ownMirror(ServerPlayer p) {
        var m = model(p);
        try { m.profile(p.getUUID()); }
        catch (IllegalArgumentException e) { m.join(p.getUUID(), p.getGameProfile().getName()); }
        var tag = view(m.view(p.getUUID(), p.getUUID()));
        var own = m.profile(p.getUUID());
        progression(tag, m, p.getUUID());
        tag.putLong("EpCap", m.epCap(own)); tag.putLong("EssenceCap", m.essenceCap(own));
        tag.putLong("EssenceRegen", m.supreme(p.getUUID()) ? m.config().regenPerSecond() : 0);
        tag.putBoolean("ChallengeEligible", m.challengeEligible(p.getUUID()));
        return tag;
    }
    public static CompoundTag snapshot(ServerPlayer p) {
        var data = DivinitySavedData.get(p.server); var m = data.model; var id = p.getUUID();
        CompoundTag tag = ownMirror(p);
        var session = data.session(id);
        if (session.profileResponse != null) tag.putUUID("ProfileResponse", session.profileResponse);
        tag.putString("Result", session.result);
        var nearby = new ListTag();
        nearby(p).forEach(target -> nearby.add(personalizedView(m, id, target.getUUID())));
        tag.put("Nearby", nearby);
        if (session.target != null) {
            try { tag.put("Target", personalizedView(m, id, session.target)); } catch (IllegalArgumentException ignored) { session.target = null; }
        }
        var followers = new ListTag();
        if (m.supreme(id) || m.profile(id).rank().ordinal() >= DivinityModel.Rank.MINOR_GOD.ordinal())
            m.followers(id).stream()            .limit(200).forEach(f -> followers.add(personalizedView(m, id, f.id())));
        tag.put("Followers", followers);
        if (m.profile(id).patron() != null) tag.put("PatronProfile", personalizedView(m, id, m.profile(id).patron()));
        var pending = new ListTag();
        for (var o : m.pending(id)) {
            var t = new CompoundTag(); var s = o.proposal();
            t.putUUID("Id", o.id()); t.putUUID("Donor", s.donor()); t.putUUID("Recipient", s.recipient());
            t.putString("DonorName", publicName(m.view(id, s.donor())));
            t.putString("RecipientName", publicName(m.view(id, s.recipient())));
            t.putLong("Amount", s.amount()); t.putLong("Fee", o.fee()); t.putLong("Expires", o.expires());
            t.putString("Ability", s.ability()); t.putString("Name", s.name()); t.putBoolean("Bind", s.bind());
            pending.add(t);
        }
        tag.put("Pending", pending);
        var history = new ListTag();
        var entries = m.history(id);
        entries.stream().skip(Math.max(0, entries.size() - 40)).forEach(a ->
            history.add(StringTag.valueOf(a.time() + " · " + a.text())));
        tag.put("History", history);
        if (m.supreme(id)) {
            var petitions = new ListTag();
            m.image().profiles().stream().filter(v -> v.release() == DivinityModel.Release.PETITIONED)
                .limit(40).forEach(v -> petitions.add(personalizedView(m, id, v.id())));
            tag.put("Petitions", petitions);
        }
        if (session.draft != null && session.draftExpiry > now()) {
            var draft = new CompoundTag(); draft.putUUID("Id", session.draftId);
            draft.putLong("Fee", session.draftFee); draft.putLong("Amount", session.draft.amount());
            draft.putLong("Total", Math.addExact(session.draftFee, session.draft.amount()));
            draft.putLong("BalanceAfter", m.profile(id).essence() - draft.getLong("Total"));
            draft.putString("Ability", session.draft.ability()); draft.putString("Name", session.draft.name());
            draft.putBoolean("Bind", session.draft.bind()); draft.putUUID("Target", session.draft.recipient());
            tag.put("Quote", draft);
        }
        MobDivinityService.snapshot(p, tag, session);
        return tag;
    }
    private static CompoundTag view(DivinityModel.View v) {
        var t = new CompoundTag(); t.putUUID("Id", v.id()); t.putString("Original", v.originalName());
        t.putString("Username", v.username()); t.putBoolean("Supreme", v.supreme());
        if (v.ep() == null) { t.putBoolean("Hidden", true); return t; }
        t.putString("DivineName", v.divineName()); t.putString("Rank", v.rank().name());
        t.putLong("Ep", v.ep()); t.putLong("Essence", v.essence());
        t.putBoolean("CapLift", v.capLift()); t.putBoolean("Bestow", v.bestow());
        if (v.patron() != null) t.putUUID("Patron", v.patron());
        if (v.release() != null) t.putString("Release", v.release().name());
        var patrons = new ListTag(); v.npcPatrons().stream().sorted().forEach(n -> patrons.add(StringTag.valueOf(n))); t.put("Npc", patrons);
        var grants = new CompoundTag(); v.grants().forEach(grants::putString); t.put("Grants", grants);
        return t;
    }
    private static CompoundTag personalizedView(DivinityModel model, UUID viewer, UUID target) {
        var tag = view(model.view(viewer, target));
        if (viewer.equals(target) || model.supreme(viewer)) progression(tag, model, target);
        return tag;
    }
    static void progression(CompoundTag tag, DivinityModel model, UUID id) {
        var progress = model.progression(id);
        tag.putLong("EarnedEssence", progress.earned());
        tag.putInt("DivineLevel", progress.level());
        tag.putString("Grade", progress.grade());
        tag.putLong("LevelProgress", progress.intoLevel());
        tag.putLong("LevelRequired", progress.nextCost());
    }
    private static String publicName(DivinityModel.View v) {
        return v.divineName().isEmpty() ? v.username() : v.divineName();
    }
    private static boolean reachable(ServerPlayer from, ServerPlayer to) {
        return to != null && from != to && from.isAlive() && to.isAlive() && !from.isSpectator() && !to.isSpectator()
            && from.serverLevel() == to.serverLevel()
            && from.distanceToSqr(to) <= Math.pow(model(from).config().distance(), 2)
            && from.hasLineOfSight(to) && to.hasLineOfSight(from);
    }
    private static List<ServerPlayer> nearby(ServerPlayer p) {
        return playerCandidates(p).stream().limit(20).toList();
    }
    private static List<ServerPlayer> playerCandidates(ServerPlayer p) {
        return p.server.getPlayerList().getPlayers().stream().filter(t -> reachable(p, t))
            .sorted(Comparator.comparingDouble(p::distanceToSqr)).toList();
    }
    private static net.minecraft.world.entity.LivingEntity lookingAt(ServerPlayer p) {
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(model(p).config().distance()));
        return java.util.stream.Stream.concat(playerCandidates(p).stream(), MobDivinityService.candidates(p).stream())
            .filter(t -> t.getBoundingBox().inflate(0.2).clip(eye, end).isPresent())
            .min(Comparator.comparingDouble(t -> t.getBoundingBox().inflate(0.2).clip(eye, end).orElse(end).distanceToSqr(eye)))
            .orElse(null);
    }
    public static void handle(ServerPlayer p, DivinityNetwork.Request r) {
        var data = DivinitySavedData.get(p.server); var m = data.model; var session = data.session(p.getUUID());
        long tick = p.server.overworld().getGameTime();
        if (session.rateTick != tick) { session.rateTick = tick; session.requests = 0; }
        if (++session.requests > 4) return;
        try {
            if (r.amount() < 0 || r.ability().length() > 128 || !DivinityModel.validName(r.name()))
                throw new IllegalArgumentException("Invalid bestowal input");
            if (!MobDivinityService.handle(p, r, session)) switch (r.operation()) {
                case "profile" -> {
                    MobDivinityService.clear(p, session);
                    var target = lookingAt(p);
                    if (target instanceof ServerPlayer) session.target = target.getUUID();
                    else if (target != null) session.mobTarget = target.getUUID();
                    session.result = target == null ? "Choose a nearby player or mob. No automatic transfer." : "Target profile requested. No resources spent.";
                    if (target instanceof net.minecraft.world.entity.Mob mob && !MobPowerAdapters.eligibility(mob).isEmpty())
                        session.result = MobPowerAdapters.eligibility(mob) + ". Inspection only; no resources spent.";
                    session.profileResponse = r.nonce();
                }
                case "refresh" -> {}
                case "clear" -> {
                    session.draft = null; session.draftId = null;
                    m.clearMobQuote(p.getUUID());
                    session.result = "Bestowal canceled. No resources spent.";
                }
                case "select" -> {
                    MobDivinityService.clear(p, session);
                    var target = p.server.getPlayerList().getPlayer(r.target());
                    if (!reachable(p, target) && !m.followers(p.getUUID()).stream().anyMatch(f -> f.id().equals(r.target()))
                            && !(m.supreme(p.getUUID()) && m.profile(r.target()).release() == DivinityModel.Release.PETITIONED))
                        throw new IllegalArgumentException("Select a visible nearby player or your existing follower");
                    session.target = r.target(); session.draft = null;
                }
                case "quote" -> {
                    session.draft = null;
                    if (!r.target().equals(session.target)) throw new IllegalArgumentException("Select a target first");
                    var target = p.server.getPlayerList().getPlayer(r.target());
                    if (!reachable(p, target)) throw new IllegalArgumentException("Bestowal requires both players nearby and visible");
                    var proposal = new DivinityModel.Proposal(p.getUUID(), r.target(), r.amount(), r.ability(), r.name().strip(), r.bind());
                    checkMastery(p, proposal);
                    session.draftFee = m.quote(proposal); session.draft = proposal;
                    session.draftId = UUID.randomUUID(); session.draftExpiry = now() + 30_000;
                    session.result = "Quote only. Establishment and power fees are burned separately; EP is never spent.";
                }
                case "send" -> {
                    if (session.draft == null || !r.nonce().equals(session.draftId) || session.draftExpiry <= now())
                        throw new IllegalArgumentException("Quote expired. Request a new quote.");
                    var s = session.draft; checkMastery(p, s);
                    if (m.quote(s) != session.draftFee) throw new IllegalArgumentException("Quote changed. Request a new quote.");
                    var target = p.server.getPlayerList().getPlayer(s.recipient());
                    var offer = m.offer(s, now(), reachable(p, target));
                    session.draft = null; session.result = "Offer sent. Nothing deducted until recipient accepts.";
                    target.sendSystemMessage(Component.literal("Divine bestowal from " + p.getGameProfile().getName()
                        + ": " + s.amount() + " Essence, power " + (s.ability().isEmpty() ? "none" : s.ability())
                        + ", name " + (s.name().isEmpty() ? "unchanged" : s.name()) + ", bind " + s.bind()
                        + ". Press N to inspect, or ").append(Component.literal("[ACCEPT]")
                        .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/jc essence accept " + offer.id()))))
                        .append(" ").append(Component.literal("[DECLINE]").withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/jc essence decline " + offer.id())))));
                }
                case "accept" -> {
                    var offer = m.pending(p.getUUID()).stream().filter(o -> o.id().equals(r.nonce())).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Offer no longer exists"));
                    var donor = p.server.getPlayerList().getPlayer(offer.proposal().donor());
                    if (donor != null) checkMastery(donor, offer.proposal());
                    m.accept(p.getUUID(), r.nonce(), now(), donor != null && reachable(donor, p));
                    session.result = "Bestowal accepted. Profile and balances committed.";
                }
                case "decline", "cancel" -> { m.cancel(p.getUUID(), r.nonce(), now()); session.result = "Offer closed; no resources spent."; }
                case "request_release" -> { m.requestRelease(p.getUUID(), now()); session.result = "Release requested."; }
                case "petition" -> { m.petition(p.getUUID(), now()); session.result = "Petition recorded. No combat victory or automatic role reversal is implied."; }
                case "approve_release", "deny_release" -> {
                    m.decideRelease(p.getUUID(), r.target(), r.operation().equals("approve_release"), now());
                    session.result = "Release decision recorded.";
                }
                case "adjudicate" -> {
                    if (!r.bind()) throw new IllegalArgumentException("Explicit confirmation required");
                    m.adjudicate(p.getUUID(), r.target(), r.ability(), now());
                    session.result = "Confirmed adjudication recorded.";
                }
                default -> throw new IllegalArgumentException("Unknown divine operation");
            }
        } catch (IllegalArgumentException | ArithmeticException e) {
            session.result = e.getMessage() == null ? "Bestowal could not be completed." : e.getMessage();
            p.displayClientMessage(Component.literal(session.result), false);
        }
        data.setDirty();
        // Snapshots are personalized; never broadcast the aggregate or Jaycee's private stats.
        for (var player : p.server.getPlayerList().getPlayers()) GameplayNetwork.sync(player);
    }
    private static void checkMastery(ServerPlayer donor, DivinityModel.Proposal proposal) {
        if (!proposal.ability().isEmpty() && !model(donor).supreme(donor.getUUID())
                && PlayerState.get(donor).skillLevel(proposal.ability()) < 1)
            throw new IllegalArgumentException("Master at least level 1 before bestowing this power");
    }
    public static void registerDebug(com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> debug) {
        var authority = Commands.literal("jayceeauthority").requires(s -> s.hasPermission(2));
        authority.then(Commands.literal("claim").executes(c -> run(c.getSource().getPlayerOrException(), () -> {
            var p = c.getSource().getPlayerOrException(); var d = DivinitySavedData.get(p.server);
            d.model.assignAuthority(p.getUUID(), false, now()); d.setDirty();
        })));
        for (String op : List.of("assign", "reassign")) authority.then(Commands.literal(op)
            .then(Commands.argument("player", EntityArgument.player()).executes(c -> {
                var p = EntityArgument.getPlayer(c, "player");
                return run(p, () -> { var d = DivinitySavedData.get(p.server);
                    d.model.assignAuthority(p.getUUID(), op.equals("reassign"), now()); d.setDirty(); });
            })));
        authority.then(Commands.literal("unassign").executes(c -> {
            var d = DivinitySavedData.get(c.getSource().getServer()); d.model.assignAuthority(null, true, now()); d.setDirty();
            c.getSource().getServer().getPlayerList().getPlayers().forEach(GameplayNetwork::sync); return 1;
        }));
        debug.then(authority);
        var admin = Commands.literal("divineadmin").requires(s -> s.hasPermission(2));
        for (String op : List.of("ep", "essence")) admin.then(Commands.literal(op)
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", LongArgumentType.longArg(0)).executes(c -> {
                    var p = EntityArgument.getPlayer(c, "player");
                    return run(p, () -> model(p).adminBalance(p.getUUID(), op.equals("ep"), LongArgumentType.getLong(c, "amount"), now()));
                }))));
        admin.then(Commands.literal("rank").then(Commands.argument("player", EntityArgument.player())
            .then(Commands.argument("rank", StringArgumentType.word())
                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(Arrays.stream(DivinityModel.Rank.values()).map(Enum::name), b))
                .executes(c -> { var p = EntityArgument.getPlayer(c, "player");
                    return run(p, () -> model(p).adminRank(p.getUUID(), DivinityModel.Rank.valueOf(StringArgumentType.getString(c, "rank")), now())); }))));
        for (String op : List.of("npc_add", "npc_remove")) admin.then(Commands.literal(op)
            .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("patron", StringArgumentType.word())
                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(DivinityModel.NPC_PATRONS, b))
                .executes(c -> { var p = EntityArgument.getPlayer(c, "player");
                    return run(p, () -> model(p).adminNpc(p.getUUID(), StringArgumentType.getString(c, "patron"), op.equals("npc_add"), now())); }))));
        for (String op : List.of("grant", "revoke")) admin.then(Commands.literal(op)
            .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("ability", StringArgumentType.greedyString())
                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                    AbilityCatalog.registered().stream().filter(AbilityCatalog.Art::implemented).map(AbilityCatalog.Art::id), b))
                .executes(c -> { var p = EntityArgument.getPlayer(c, "player");
                    return run(p, () -> model(p).adminGrant(p.getUUID(), StringArgumentType.getString(c, "ability"), op.equals("grant"), now())); }))));
        debug.then(admin);
    }
    @FunctionalInterface private interface Command { void run() throws com.mojang.brigadier.exceptions.CommandSyntaxException; }
    private static int run(ServerPlayer p, Command command) {
        try {
            command.run(); DivinitySavedData.get(p.server).setDirty();
            for (var player : p.server.getPlayerList().getPlayers()) {
                SkillRuntime.invalidate(player); GameplayNetwork.sync(player);
            }
            p.sendSystemMessage(Component.literal("Divine server operation recorded (operator commands are testing/configuration, not natural progression)."));
            return 1;
        } catch (IllegalArgumentException | com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            p.sendSystemMessage(Component.literal(e.getMessage())); return 0;
        }
    }
}
