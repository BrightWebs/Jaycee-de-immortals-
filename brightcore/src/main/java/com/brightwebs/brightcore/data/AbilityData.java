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
 * Everything Bright Core stores on a player. Immutable: every change makes a new copy and goes
 * through player.setData(...), which marks it for saving AND syncs it to that player's client.
 *
 * Abilities are stored by id, so a world still loads if an addon is removed.
 *
 * The ability bar has {@link #PAGES} pages of {@link #SLOTS_PER_PAGE} slots, stored flat:
 * slot index = page * 5 + slot.
 */
public record AbilityData(Set<Identifier> owned,
                          Map<Identifier, Long> cooldowns,
                          List<String> slots,
                          int page,
                          int mana,
                          int maxMana) {

    public static final int SLOTS_PER_PAGE = 5;
    public static final int PAGES = 10;
    public static final int TOTAL_SLOTS = SLOTS_PER_PAGE * PAGES;

    public static final AbilityData EMPTY = new AbilityData(Set.of(), Map.of(), List.of(), 0, 100, 100);

    public static final Codec<AbilityData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.listOf().xmap(l -> (Set<Identifier>) new HashSet<>(l), s -> List.copyOf(s))
                    .optionalFieldOf("owned", Set.of()).forGetter(AbilityData::owned),
            Codec.unboundedMap(Identifier.CODEC, Codec.LONG)
                    .optionalFieldOf("cooldowns", Map.of()).forGetter(AbilityData::cooldowns),
            Codec.STRING.listOf().optionalFieldOf("slots", List.of()).forGetter(AbilityData::slots),
            Codec.INT.optionalFieldOf("page", 0).forGetter(AbilityData::page),
            Codec.INT.optionalFieldOf("mana", 100).forGetter(AbilityData::mana),
            Codec.INT.optionalFieldOf("max_mana", 100).forGetter(AbilityData::maxMana)
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
        maxMana = Math.max(0, maxMana);
        mana = Math.clamp(mana, 0, maxMana);
    }

    // ---- reads -----------------------------------------------------------------

    public boolean has(Identifier ability) { return owned.contains(ability); }

    /** Any of the 50 slots, by flat index. */
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

    /** Ticks of cooldown left (0 = ready). */
    public long cooldownLeft(Identifier ability, long gameTime) {
        return Math.max(0, cooldowns.getOrDefault(ability, 0L) - gameTime);
    }

    // ---- copies with one change ---------------------------------------------------

    public AbilityData withAbility(Identifier ability) {
        Set<Identifier> s = new HashSet<>(owned);
        s.add(ability);
        return new AbilityData(s, cooldowns, slots, page, mana, maxMana);
    }

    public AbilityData withoutAbility(Identifier ability) {
        Set<Identifier> s = new HashSet<>(owned);
        s.remove(ability);
        Map<Identifier, Long> c = new HashMap<>(cooldowns);
        c.remove(ability);
        List<String> sl = new ArrayList<>(slots);
        sl.replaceAll(v -> v.equals(ability.toString()) ? "" : v);
        return new AbilityData(s, c, sl, page, mana, maxMana);
    }

    public AbilityData withSlotAt(int index, Identifier abilityOrNull) {
        List<String> sl = new ArrayList<>(slots);
        sl.set(index, abilityOrNull == null ? "" : abilityOrNull.toString());
        return new AbilityData(owned, cooldowns, sl, page, mana, maxMana);
    }

    public AbilityData withPage(int page) { return new AbilityData(owned, cooldowns, slots, page, mana, maxMana); }

    public AbilityData withCooldown(Identifier ability, long endsAt) {
        Map<Identifier, Long> c = new HashMap<>(cooldowns);
        c.put(ability, endsAt);
        return new AbilityData(owned, c, slots, page, mana, maxMana);
    }

    /** Drops finished cooldowns so the save file doesn't grow forever. */
    public AbilityData withExpiredCooldownsRemoved(long gameTime) {
        if (cooldowns.values().stream().noneMatch(t -> t <= gameTime)) return this;
        Map<Identifier, Long> c = new HashMap<>(cooldowns);
        c.values().removeIf(t -> t <= gameTime);
        return new AbilityData(owned, c, slots, page, mana, maxMana);
    }

    public AbilityData withMana(int mana) { return new AbilityData(owned, cooldowns, slots, page, mana, maxMana); }

    public AbilityData withMaxMana(int maxMana) { return new AbilityData(owned, cooldowns, slots, page, mana, maxMana); }
}
