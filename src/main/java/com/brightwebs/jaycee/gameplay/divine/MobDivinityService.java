package com.brightwebs.jaycee.gameplay.divine;

import com.brightwebs.jaycee.gameplay.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.EventPriority;

final class MobDivinityService {
    private static final String MIRROR = "JayceeMobDivinity";
    private MobDivinityService() {}
    static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, MobDivinityService::join);
        NeoForge.EVENT_BUS.addListener(MobDivinityService::leave);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, MobDivinityService::death);
        NeoForge.EVENT_BUS.addListener(MobDivinityService::tick);
    }
    static DivinityModel.MobContext context(Mob mob) {
        UUID tameOwner = mob instanceof AbstractHorse horse ? horse.getOwnerUUID()
            : mob instanceof OwnableEntity pet ? pet.getOwnerUUID() : null;
        return new DivinityModel.MobContext(mob.getUUID(), BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(),
            mob.level().dimension().location().toString(), mob.getId(),
            tameOwner);
    }
    static DivinityModel.MobProfile observe(Mob mob, String status) {
        var c = context(mob);
        return new DivinityModel.MobProfile(c.id(), c.type(), c.dimension(), mob.getType().getDescriptionId(),
            mob.hasCustomName() ? mob.getCustomName().getString() : "", "", mob.getHealth(), mob.getMaxHealth(),
            0, 0, 0, null, Map.of(), status);
    }
    static Mob loaded(MinecraftServer server, UUID id) {
        if (id == null) return null;
        for (var level : server.getAllLevels()) {
            var entity = level.getEntity(id); // UUID lookup never loads chunks.
            if (entity instanceof Mob mob) return mob;
        }
        return null;
    }
    static boolean reachable(ServerPlayer p, Mob mob) {
        return mob != null && mob.isAlive() && !mob.isRemoved() && p.isAlive() && !p.isSpectator()
            && p.level() == mob.level() && p.distanceToSqr(mob) <= Math.pow(DivinityService.model(p).config().distance(), 2)
            && p.hasLineOfSight(mob);
    }
    static List<Mob> nearby(ServerPlayer p) {
        return candidates(p).stream().limit(20).toList();
    }
    static List<Mob> candidates(ServerPlayer p) {
        double range = DivinityService.model(p).config().distance();
        return p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(range),
            m -> reachable(p, m)).stream().sorted(Comparator.comparingDouble(p::distanceToSqr)).toList();
    }
    static void clear(ServerPlayer p, DivinitySavedData.Session session) {
        session.target = null; session.mobTarget = null; session.draft = null;
        DivinityService.model(p).clearMobQuote(p.getUUID());
    }
    static boolean handle(ServerPlayer p, DivinityNetwork.Request r, DivinitySavedData.Session s) {
        var model = DivinityService.model(p);
        switch (r.operation()) {
            case "mob_clear" -> { clear(p, s); s.result = "Selection cleared; no resources spent."; }
            case "mob_select" -> {
                clear(p, s);
                var mob = loaded(p.server, r.target()); var old = model.mob(r.target());
                if (!reachable(p, mob) && (old == null || !(p.getUUID().equals(old.master()) || model.supreme(p.getUUID()))))
                    throw new IllegalArgumentException("Select a visible nearby mob or your existing subordinate");
                s.mobTarget = r.target(); s.result = "Mob selected; no resources spent.";
            }
            case "mob_quote" -> {
                model.clearMobQuote(p.getUUID()); s.draft = null;
                if (!r.target().equals(s.mobTarget)) throw new IllegalArgumentException("Select the mob first");
                var mob = loaded(p.server, s.mobTarget);
                if (!reachable(p, mob)) throw new IllegalArgumentException("Mob must be alive, nearby, visible and in this dimension");
                var proposal = new DivinityModel.Proposal(p.getUUID(), mob.getUUID(), r.amount(), r.ability(), r.name().strip(), r.bind());
                mastery(p, proposal.ability());
                var reason = MobPowerAdapters.reason(mob, proposal.ability());
                if (!reason.isEmpty()) throw new IllegalArgumentException(reason);
                model.quoteMob(proposal, observe(mob, "active"), context(mob), true, System.currentTimeMillis());
                s.result = "Quote only; expires in 30 seconds. Balance after is a guaranteed minimum; later regeneration credits remain. Evolution/EP conversion unavailable.";
            }
            case "mob_confirm" -> {
                var q = model.mobQuote(p.getUUID(), System.currentTimeMillis());
                try {
                    if (q == null || !q.proposal().recipient().equals(s.mobTarget))
                        throw new IllegalArgumentException("Mob quote expired; select and quote again");
                    var mob = loaded(p.server, s.mobTarget);
                    mastery(p, q.proposal().ability());
                    var after = model.confirmMob(p.getUUID(), r.nonce(), mob == null ? null : context(mob),
                        reachable(p, mob), mob != null && MobPowerAdapters.reason(mob, q.proposal().ability()).isEmpty(),
                        System.currentTimeMillis());
                    DivinitySavedData.get(p.server).setDirty();
                    if (q.proposal().bind()) mob.setPersistenceRequired();
                    mirror(mob, after);
                    s.result = "Mob bestowal committed; no automatic evolution or promotion.";
                } finally { model.clearMobQuote(p.getUUID()); }
            }
            default -> { return false; }
        }
        return true;
    }
    private static void mastery(ServerPlayer p, String ability) {
        if (!ability.isEmpty() && !DivinityService.model(p).supreme(p.getUUID())
                && PlayerState.get(p).skillLevel(ability) < 1)
            throw new IllegalArgumentException("Master at least level 1 before bestowing this power");
    }
    static CompoundTag view(ServerPlayer viewer, DivinityModel.MobProfile profile) {
        var model = DivinityService.model(viewer); var live = loaded(viewer.server, profile.id());
        if (live != null && !context(live).type().equals(profile.type())) live = null;
        var t = MobDivinityTags.write(profile);
        t.putInt("EntityId", live != null && profile.status().equals("active") ? live.getId() : -1);
        if (live != null && live.isAlive() && profile.status().equals("active")) {
            t.putFloat("Health", live.getHealth()); t.putFloat("MaxHealth", live.getMaxHealth());
            t.putString("CustomName", live.hasCustomName() ? live.getCustomName().getString() : "");
        }
        String name = "";
        if (profile.master() != null) {
            var master = model.view(viewer.getUUID(), profile.master());
            name = master.ep() == null || master.divineName().isEmpty() ? master.username() : master.divineName();
        }
        t.putString("MasterName", name);
        String eligibility = profile.status().equals("deceased") ? "Deceased ledger entry; revival/rebinding unavailable"
            : profile.status().equals("removed") ? "Permanently removed ledger entry: " + model.mobRemoval(profile.id()) + "; replacement/rebinding unavailable"
            : !model.mobRemoval(profile.id()).isEmpty() ? "Entity permanently removed: " + model.mobRemoval(profile.id()) + "; replacement/rebinding unavailable"
            : live == null ? "Entity is not loaded; no live compatibility check available"
            : MobPowerAdapters.eligibility(live);
        if (eligibility.isEmpty() && context(live).tameOwner() != null
                && !viewer.getUUID().equals(context(live).tameOwner()))
            eligibility = "Another player's pet requires its owner's consent; mob consent is not implemented";
        if (eligibility.isEmpty() && !reachable(viewer, live))
            eligibility = "Bestowal requires a live nearby visible mob in this dimension";
        t.putBoolean("Eligible", eligibility.isEmpty()); t.putString("EligibilityReason", eligibility);
        t.putString("Reason", eligibility);
        String bindingReason = DivinityModel.mobBindingReason(profile.type());
        t.putBoolean("BindingCompatible", bindingReason.isEmpty()); t.putString("BindingReason", bindingReason);
        t.putBoolean("TerminalRemoval", !model.mobRemoval(profile.id()).isEmpty());
        t.putString("RemovalReason", model.mobRemoval(profile.id()));
        t.putString("PersistenceWarning", bindingReason.isEmpty() ? persistenceWarning(profile.master() != null)
            : bindingReason + " " + persistenceWarning(profile.master() != null));
        return t;
    }
    static void snapshot(ServerPlayer p, CompoundTag tag, DivinitySavedData.Session s) {
        var model = DivinityService.model(p); var nearby = new ListTag();
        for (var mob : nearby(p)) nearby.add(view(p, Optional.ofNullable(model.mob(mob.getUUID())).orElseGet(() -> observe(mob, "active"))));
        tag.put("NearbyMobs", nearby);
        var followers = new ListTag();
        model.mobs().stream().filter(m -> m.master() != null
                && (model.supreme(p.getUUID()) || p.getUUID().equals(m.master())))
            .limit(200).forEach(m -> followers.add(view(p, m)));
        tag.put("MobFollowers", followers);
        Mob target = loaded(p.server, s.mobTarget);
        var storedTarget = model.mob(s.mobTarget);
        boolean mayInspectRemote = storedTarget != null
            && (p.getUUID().equals(storedTarget.master()) || model.supreme(p.getUUID()));
        if (s.mobTarget != null && !mayInspectRemote && !reachable(p, target)) {
            s.mobTarget = null; target = null; model.clearMobQuote(p.getUUID());
        }
        tag.putString("TargetKind", s.mobTarget != null ? "mob" : s.target != null ? "player" : "");
        if (s.mobTarget != null) {
            var profile = model.mob(s.mobTarget);
            if (profile == null && target != null) profile = observe(target, "active");
            if (profile != null) tag.put("MobTarget", view(p, profile));
        }
        var powers = new ListTag();
        for (var art : AbilityCatalog.registered()) if (art.implemented() && model.authorizedMobPower(p.getUUID(), art.id(),
                model.supreme(p.getUUID()) ? 0 : PlayerState.get(p).skillLevel(art.id()))) {
            var t = new CompoundTag();
            t.putString("Id", art.id()); t.putString("Name", art.name()); t.putString("Description", MobPowerAdapters.effect(art.id()));
            String reason = target == null ? "Select a loaded mob to check compatibility" : MobPowerAdapters.reason(target, art.id());
            if (reason.isEmpty() && model.mob(target.getUUID()) != null && model.mob(target.getUUID()).grants().containsKey(art.id()))
                reason = "Mob already owns this power";
            t.putBoolean("Compatible", reason.isEmpty()); t.putString("Reason", reason);
            t.putString("Effect", MobPowerAdapters.effect(art.id()));
            t.putString("Requirements", "Owned power; mastery level 1 (unless supreme); safe species; owner consent. Evolution unavailable.");
            t.putLong("Cost", model.supreme(p.getUUID()) ? 0 : model.config().powerFee()); powers.add(t);
        }
        tag.put("MobPowers", powers);
        tag.putString("MobPowerRequirements", "Only currently authorized powers are listed: Minor God rank, bestowal privileges, owned implemented power and mastery level 1. Supreme bypasses rank, privileges and mastery, never compatibility.");
        var q = model.mobQuote(p.getUUID(), System.currentTimeMillis());
        if (q != null && q.proposal().recipient().equals(s.mobTarget)) {
            var t = new CompoundTag(); var proposal = q.proposal();
            t.putUUID("Id", q.id()); t.putUUID("Target", proposal.recipient()); t.putLong("Amount", proposal.amount());
            t.putLong("Fee", q.fee()); t.putLong("Total", q.total()); t.putLong("BalanceAfter", q.balanceAfter());
            t.putLong("RecipientAfter", q.recipientAfter()); t.putLong("Expires", q.expires());
            t.putLong("RemainingMillis", Math.max(0, q.expires() - System.currentTimeMillis()));
            t.putBoolean("BalanceAfterMinimum", true);
            t.putString("Ability", proposal.ability()); t.putString("Name", proposal.name()); t.putBoolean("Bind", proposal.bind());
            t.putString("Effect", proposal.ability().isEmpty() ? "No power selected; transfer, name and/or binding only."
                : MobPowerAdapters.effect(proposal.ability()));
            t.putString("Requirements", "Live nearby visible mob in this dimension; valid ownership and funds; owned mastered power if selected. Evolution/EP conversion unavailable.");
            t.putString("PersistenceWarning", persistenceWarning(proposal.bind() || q.before().master() != null));
            tag.put("MobQuote", t);
        }
    }
    private static String persistenceWarning(boolean bound) {
        return bound ? "Bound subordinates require normal persistence. Death and vanilla conversion/removal still retire this entity UUID; no migration, resurrection, replacement or refund."
            : "Unbound gifts do not force persistence. Unnamed nonpersistent mobs may naturally despawn; gifts follow only this UUID/lifetime, never a conversion/replacement. Bind where compatible for subordinate persistence; no refund.";
    }
    private static void mirror(Mob mob, DivinityModel.MobProfile profile) {
        var tag = MobDivinityTags.write(profile);
        if (mob.getServer() != null) {
            var removal = DivinitySavedData.get(mob.getServer()).model.mobRemoval(profile.id());
            tag.putBoolean("TerminalRemoval", !removal.isEmpty()); tag.putString("RemovalReason", removal);
        }
        mob.getPersistentData().put(MIRROR, tag);
        if (!profile.divineName().isEmpty()) mob.setCustomName(Component.literal(profile.divineName()));
    }
    private static void update(Mob mob, String status) {
        if (mob.level().isClientSide() || mob.getServer() == null) return;
        var data = DivinitySavedData.get(mob.getServer());
        var stored = data.model.mob(mob.getUUID());
        if (stored == null || !stored.type().equals(context(mob).type())) return;
        if (!data.model.observeMob(observe(mob, status), context(mob))) return;
        mirror(mob, data.model.mob(mob.getUUID())); data.setDirty();
    }
    private static void join(EntityJoinLevelEvent e) {
        if (!(e.getEntity() instanceof Mob mob) || mob.level().isClientSide() || mob.getServer() == null) return;
        var data = DivinitySavedData.get(mob.getServer());
        if (data.model.mob(mob.getUUID()) == null) {
            MobPowerAdapters.clear(mob);
            mob.getPersistentData().remove(MIRROR);
            return;
        }
        data.model.joinMob(observe(mob, "active"), context(mob));
        var stored = data.model.mob(mob.getUUID());
        if (!stored.type().equals(context(mob).type())) { MobPowerAdapters.clear(mob); return; }
        mirror(mob, stored); data.setDirty();
        if (stored != null && stored.master() != null && stored.status().equals("active")
                && data.model.mobRemoval(mob.getUUID()).isEmpty()) mob.setPersistenceRequired();
    }
    private static void leave(EntityLeaveLevelEvent e) {
        if (!(e.getEntity() instanceof Mob mob) || mob.level().isClientSide() || mob.getServer() == null) return;
        var data = DivinitySavedData.get(mob.getServer()); var reason = mob.getRemovalReason();
        String status = reason == Entity.RemovalReason.KILLED ? "deceased"
            : reason != null && reason.shouldDestroy() ? "removed" : "unloaded";
        if (data.model.leaveMob(observe(mob, status), context(mob),
                reason == null ? "UNKNOWN" : reason.name(), reason != null && reason.shouldDestroy())) {
            mirror(mob, data.model.mob(mob.getUUID())); data.setDirty();
        }
    }
    private static void death(LivingDeathEvent e) {
        // Cancellable death notifications cannot create permanent tombstones before actual removal.
        if (e.getEntity() instanceof Mob mob && mob.getRemovalReason() == Entity.RemovalReason.KILLED)
            update(mob, "deceased");
    }
    private static void tick(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof Mob mob) || mob.level().isClientSide() || mob.getServer() == null) return;
        var model = DivinitySavedData.get(mob.getServer()).model;
        var profile = model.mob(mob.getUUID());
        if (profile == null) return;
        if (!model.liveMob(context(mob))) { MobPowerAdapters.clear(mob); return; }
        MobPowerAdapters.tick(mob, profile);
        if (mob.tickCount % 20 == 0 && mob.isAlive()) update(mob, "active");
    }
}
