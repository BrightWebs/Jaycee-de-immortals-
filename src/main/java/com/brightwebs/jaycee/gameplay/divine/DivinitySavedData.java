package com.brightwebs.jaycee.gameplay.divine;

import com.brightwebs.jaycee.gameplay.AbilityCatalog;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Overworld-only authority; player NBT contains presentation mirrors, never the ledger. */
public final class DivinitySavedData extends SavedData {
    public final DivinityModel model = new DivinityModel(DivinityConfig.values(),
        AbilityCatalog.registered().stream().filter(AbilityCatalog.Art::implemented).map(AbilityCatalog.Art::id).collect(Collectors.toSet()));
    final Map<UUID, Session> sessions = new HashMap<>();
    static final class Session {
        UUID target, mobTarget, draftId;
        UUID profileResponse;
        DivinityModel.Proposal draft;
        long draftFee, draftExpiry, rateTick;
        int requests;
        String result = "";
    }
    Session session(UUID id) { return sessions.computeIfAbsent(id, ignored -> new Session()); }
    public static DivinitySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(DivinitySavedData::new, DivinitySavedData::load), "jaycee_divinity");
    }
    public DivinitySavedData() {}
    public static DivinitySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new DivinitySavedData();
        var profiles = new ArrayList<DivinityModel.Profile>();
        for (Tag value : tag.getList("Profiles", Tag.TAG_COMPOUND)) {
            var p = (CompoundTag)value;
            var npc = new HashSet<String>();
            for (Tag entry : p.getList("Npc", Tag.TAG_STRING)) npc.add(entry.getAsString());
            var grants = new HashMap<String, String>();
            var g = p.getCompound("Grants");
            for (String key : g.getAllKeys()) grants.put(key, g.getString(key));
            profiles.add(new DivinityModel.Profile(p.getUUID("Id"), p.getString("Original"), p.getString("Username"),
                p.getString("Name"), DivinityModel.Rank.valueOf(p.getString("Rank")), p.getLong("Ep"), p.getLong("Essence"),
                p.getBoolean("CapLift"), p.getBoolean("Bestow"), p.hasUUID("Patron") ? p.getUUID("Patron") : null,
                npc, grants, p.contains("Release") ? DivinityModel.Release.valueOf(p.getString("Release")) : null));
        }
        var offers = new ArrayList<DivinityModel.Offer>();
        for (Tag value : tag.getList("Offers", Tag.TAG_COMPOUND)) {
            var o = (CompoundTag)value;
            offers.add(new DivinityModel.Offer(o.getUUID("Id"),
                new DivinityModel.Proposal(o.getUUID("Donor"), o.getUUID("Recipient"), o.getLong("Amount"),
                    o.getString("Ability"), o.getString("Name"), o.getBoolean("Bind")),
                o.getLong("Fee"), o.getLong("Expires")));
        }
        var audit = new ArrayList<DivinityModel.Audit>();
        for (Tag value : tag.getList("Audit", Tag.TAG_COMPOUND)) {
            var a = (CompoundTag)value;
            audit.add(new DivinityModel.Audit(a.getLong("Time"), a.hasUUID("Actor") ? a.getUUID("Actor") : null,
                a.hasUUID("Target") ? a.getUUID("Target") : null, a.getString("Text")));
        }
        data.model.restore(new DivinityModel.Image(tag.hasUUID("Authority") ? tag.getUUID("Authority") : null,
            profiles, offers, audit, tag.getInt("Carry")), System.currentTimeMillis());
        var mobs = new ArrayList<DivinityModel.MobProfile>();
        for (Tag value : tag.getList("Mobs", Tag.TAG_COMPOUND)) {
            var t = ((CompoundTag)value).copy();
            if (!Set.of("deceased", "removed").contains(t.getString("Status"))) t.putString("Status", "unloaded");
            mobs.add(MobDivinityTags.read(t));
        }
        data.model.restoreMobs(mobs);
        var removals = new HashMap<UUID, String>();
        for (Tag value : tag.getList("MobRemovals", Tag.TAG_COMPOUND)) {
            var t = (CompoundTag)value;
            if (t.hasUUID("Id")) removals.put(t.getUUID("Id"), t.getString("Reason"));
        }
        data.model.restoreMobRemovals(removals);
        // v3 balances and mob received counters contain gifts/admin values with no provenance.
        // Migration deliberately starts lifetime earned and awardable reserves at zero.
        if (tag.getInt("Version") >= 4)
            data.model.restoreProgress(readProgress(tag, "PlayerProgress"), readProgress(tag, "MobProgress"));
        data.setDirty();
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var image = model.image();
        tag.putInt("Version", 4);
        tag.put("PlayerProgress", writeProgress(model.playerProgressStates()));
        tag.put("MobProgress", writeProgress(model.mobProgressStates()));
        var mobs = new ListTag(); model.mobs().forEach(p -> mobs.add(MobDivinityTags.write(p)));
        tag.put("Mobs", mobs);
        var removals = new ListTag();
        model.mobRemovals().forEach((id, reason) -> {
            var t = new CompoundTag(); t.putUUID("Id", id); t.putString("Reason", reason); removals.add(t);
        });
        tag.put("MobRemovals", removals);
        if (image.authority() != null) tag.putUUID("Authority", image.authority());
        tag.putInt("Carry", image.carry());
        var profiles = new ListTag();
        for (var p : image.profiles()) {
            var t = new CompoundTag();
            t.putUUID("Id", p.id()); t.putString("Original", p.originalName()); t.putString("Username", p.username());
            t.putString("Name", p.divineName()); t.putString("Rank", p.rank().name());
            t.putLong("Ep", p.ep()); t.putLong("Essence", p.essence()); t.putBoolean("CapLift", p.capLift());
            t.putBoolean("Bestow", p.bestow());
            if (p.patron() != null) t.putUUID("Patron", p.patron());
            if (p.release() != null) t.putString("Release", p.release().name());
            var npc = new ListTag(); p.npcPatrons().stream().sorted().forEach(id -> npc.add(StringTag.valueOf(id))); t.put("Npc", npc);
            var grants = new CompoundTag(); p.grants().forEach(grants::putString); t.put("Grants", grants);
            profiles.add(t);
        }
        tag.put("Profiles", profiles);
        var offers = new ListTag();
        for (var o : image.offers()) {
            var t = new CompoundTag(); var p = o.proposal();
            t.putUUID("Id", o.id()); t.putUUID("Donor", p.donor()); t.putUUID("Recipient", p.recipient());
            t.putLong("Amount", p.amount()); t.putString("Ability", p.ability()); t.putString("Name", p.name());
            t.putBoolean("Bind", p.bind()); t.putLong("Fee", o.fee()); t.putLong("Expires", o.expires()); offers.add(t);
        }
        tag.put("Offers", offers);
        var audit = new ListTag();
        for (var a : image.audit()) {
            var t = new CompoundTag(); t.putLong("Time", a.time()); t.putString("Text", a.text());
            if (a.actor() != null) t.putUUID("Actor", a.actor());
            if (a.target() != null) t.putUUID("Target", a.target());
            audit.add(t);
        }
        tag.put("Audit", audit);
        return tag;
    }
    private static Map<UUID, DivinityModel.ProgressState> readProgress(CompoundTag tag, String key) {
        var values = new HashMap<UUID, DivinityModel.ProgressState>();
        for (Tag value : tag.getList(key, Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag)value;
            if (entry.hasUUID("Id")) values.put(entry.getUUID("Id"),
                new DivinityModel.ProgressState(Math.max(0, entry.getLong("Earned")), Math.max(0, entry.getLong("Awardable"))));
        }
        return values;
    }
    private static ListTag writeProgress(Map<UUID, DivinityModel.ProgressState> states) {
        var values = new ListTag();
        states.forEach((id, state) -> {
            var entry = new CompoundTag();
            entry.putUUID("Id", id); entry.putLong("Earned", state.earned()); entry.putLong("Awardable", state.awardable());
            values.add(entry);
        });
        return values;
    }
}
