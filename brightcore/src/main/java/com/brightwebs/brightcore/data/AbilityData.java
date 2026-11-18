package com.brightwebs.brightcore.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A player's skills: what they own, mastery, which toggles are on, each skill's selected mode,
 * cooldowns, damage taken toward learning resistances, and the ability bar's 10 pages x 5 slots
 * (stored flat: slot index = page * 5 + slot). Mana and Aura live in {@link EnergyData}.
 *
 * Immutable: changes go through AbilityAPI so they save and sync.
 */
public record AbilityData(Set<Identifier> owned,
                          Map<Identifier, Long> cooldowns,
                          List<String> slots,
                          int page,
                          Map<Identifier, Integer> mastery,
                          Set<Identifier> toggled,
                          Map<Identifier, Integer> modes,
                          Map<String, Double> damageTaken) {

    public static final int SLOTS_PER_PAGE = 5;
    public static final int PAGES = 10;
    public static final int TOTAL_SLOTS = SLOTS_PER_PAGE * PAGES;

    public static final AbilityData EMPTY = new AbilityData(Set.of(), Map.of(), List.of(), 0, Map.of(), Set.of(), Map.of(), Map.of());

    private static final Codec<Set<Identifier>> ID_SET =
            Identifier.CODEC.listOf().xmap(l -> (Set<Identifier>) new HashSet<>(l), s -> List.copyOf(s));

    public static final Codec<AbilityData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ID_SET.optionalFieldOf("owned", Set.of()).forGetter(AbilityData::owned),
            Codec.unboundedMap(Identifier.CODEC, Codec.LONG).optionalFieldOf("cooldowns", Map.of()).forGetter(AbilityData::cooldowns),
            Codec.STRING.listOf().optionalFieldOf("slots", List.of()).forGetter(AbilityData::slots),
            Codec.INT.optionalFieldOf("page", 0).forGetter(AbilityData::page),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT).optionalFieldOf("mastery", Map.of()).forGetter(AbilityData::mastery),
            ID_SET.optionalFieldOf("toggled", Set.of()).forGetter(AbilityData::toggled),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT).optionalFieldOf("modes", Map.of()).forGetter(AbilityData::modes),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("damage_taken", Map.of()).forGetter(AbilityData::damageTaken)
    ).apply(i, AbilityData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public AbilityData {
        owned = Set.copyOf(owned);
        cooldowns = Map.copyOf(cooldowns);
        List<String> fixed = new ArrayList<>(slots);
        while (fixed.size() < TOTAL_SLOTS) fixed.add("");
        slots = List.copyOf(fixed.subList(0, TOTAL_SLOTS));
        page = Math.floorMod(page, PAGES);
        mastery = Map.copyOf(mastery);
        toggled = Set.copyOf(toggled);
        modes = Map.copyOf(modes);
        damageTaken = Map.copyOf(damageTaken);
    }

    // ---- reads -------------------------------------------------------------------

    public boolean has(Identifier ability) { return owned.contains(ability); }
    public boolean isToggled(Identifier ability) { return toggled.contains(ability); }
    public int masteryOf(Identifier ability) { return mastery.getOrDefault(ability, 0); }
    public int modeOf(Identifier ability) { return modes.getOrDefault(ability, 0); }

    public Optional<Identifier> slotAt(int index) {
        if (index < 0 || index >= TOTAL_SLOTS) return Optional.empty();
        String s = slots.get(index);
        return s.isEmpty() ? Optional.empty() : Optional.ofNullable(Identifier.tryParse(s));
    }

    /** One of the 5 slots on the current page. */
    public Optional<Identifier> slot(int slot) {
        if (slot < 0 || slot >= SLOTS_PER_PAGE) return Optional.empty();
        return slotAt(page * SLOTS_PER_PAGE + slot);
    }

    public long cooldownLeft(Identifier ability, long gameTime) {
        return Math.max(0, cooldowns.getOrDefault(ability, 0L) - gameTime);
    }

    // ---- copies with one change ---------------------------------------------------------

    private AbilityData with(Set<Identifier> owned, Map<Identifier, Long> cooldowns, List<String> slots, int page,
                             Map<Identifier, Integer> mastery, Set<Identifier> toggled, Map<Identifier, Integer> modes,
                             Map<String, Double> damageTaken) {
        return new AbilityData(owned, cooldowns, slots, page, mastery, toggled, modes, damageTaken);
    }

    public AbilityData withAbility(Identifier ability) {
        Set<Identifier> s = new HashSet<>(owned);
        s.add(ability);
        return with(s, cooldowns, slots, page, mastery, toggled, modes, damageTaken);
    }

    public AbilityData withoutAbility(Identifier ability) {
        Set<Identifier> s = new HashSet<>(owned);
        s.remove(ability);
        Map<Identifier, Long> c = new HashMap<>(cooldowns);
        c.remove(ability);
        List<String> sl = new ArrayList<>(slots);
        sl.replaceAll(v -> v.equals(ability.toString()) ? "" : v);
        Map<Identifier, Integer> m = new HashMap<>(mastery);
        m.remove(ability);
        Set<Identifier> t = new HashSet<>(toggled);
        t.remove(ability);
        Map<Identifier, Integer> md = new HashMap<>(modes);
        md.remove(ability);
        return with(s, c, sl, page, m, t, md, damageTaken);
    }

    public AbilityData withSlotAt(int index, Identifier abilityOrNull) {
        List<String> sl = new ArrayList<>(slots);
        sl.set(index, abilityOrNull == null ? "" : abilityOrNull.toString());
        return with(owned, cooldowns, sl, page, mastery, toggled, modes, damageTaken);
    }

    public AbilityData withPage(int page) {
        return with(owned, cooldowns, slots, page, mastery, toggled, modes, damageTaken);
    }

    public AbilityData withCooldown(Identifier ability, long endsAt) {
        Map<Identifier, Long> c = new HashMap<>(cooldowns);
        c.put(ability, endsAt);
        return with(owned, c, slots, page, mastery, toggled, modes, damageTaken);
    }

    public AbilityData withExpiredCooldownsRemoved(long gameTime) {
        if (cooldowns.values().stream().noneMatch(t -> t <= gameTime)) return this;
        Map<Identifier, Long> c = new HashMap<>(cooldowns);
        c.values().removeIf(t -> t <= gameTime);
        return with(owned, c, slots, page, mastery, toggled, modes, damageTaken);
    }

    public AbilityData withMastery(Identifier ability, int points) {
        Map<Identifier, Integer> m = new HashMap<>(mastery);
        m.put(ability, Math.max(0, points));
        return with(owned, cooldowns, slots, page, m, toggled, modes, damageTaken);
    }

    public AbilityData withToggled(Identifier ability, boolean on) {
        Set<Identifier> t = new HashSet<>(toggled);
        if (on) t.add(ability); else t.remove(ability);
        return with(owned, cooldowns, slots, page, mastery, t, modes, damageTaken);
    }

    public AbilityData withMode(Identifier ability, int mode) {
        Map<Identifier, Integer> md = new HashMap<>(modes);
        md.put(ability, mode);
        return with(owned, cooldowns, slots, page, mastery, toggled, md, damageTaken);
    }

    public AbilityData withDamageTaken(String kind, double total) {
        Map<String, Double> d = new HashMap<>(damageTaken);
        d.put(kind, total);
        return with(owned, cooldowns, slots, page, mastery, toggled, modes, d);
    }
}
