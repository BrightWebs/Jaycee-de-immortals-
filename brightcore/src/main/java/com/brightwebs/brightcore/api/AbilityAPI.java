package com.brightwebs.brightcore.api;

import com.brightwebs.brightcore.data.AbilityData;
import com.brightwebs.brightcore.data.BrightAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// everything goes through here. all the rules (owning it, cooldown, mana, slots) are checked
// in this one spot so the skills themselves don't have to
public final class AbilityAPI {
    private AbilityAPI() {}

    // reading, works on the client too with the synced copy

    public static AbilityData get(Player player) {
        return player.getData(BrightAttachments.ABILITIES);
    }

    public static boolean has(Player player, Ability ability) {
        return get(player).has(ability.id());
    }

    // skills you own that still exist, sorted by id so the menu doesn't jump around
    public static List<Ability> owned(Player player) {
        List<Ability> list = new ArrayList<>();
        for (Identifier id : get(player).owned()) {
            Ability a = AbilityRegistry.get(id);
            if (a != null) list.add(a);
        }
        list.sort(Comparator.comparing(a -> a.id().toString()));
        return list;
    }

    // changing stuff, server only

    private static void set(ServerPlayer player, AbilityData data) {
        player.setData(BrightAttachments.ABILITIES, data);  // saves and syncs
    }

    // gives a skill. false if you already have it or something cancelled it
    public static boolean learn(ServerPlayer player, Ability ability) {
        AbilityData data = get(player);
        if (data.has(ability.id())) return false;
        if (NeoForge.EVENT_BUS.post(new AbilityEvent.Learn(player, ability)).isCanceled()) return false;

        // just learned, not slotted. the player picks what goes in their slots from the library
        data = data.withAbility(ability.id());
        set(player, data);
        ability.onLearned(player);
        return true;
    }

    public static boolean forget(ServerPlayer player, Ability ability) {
        AbilityData data = get(player);
        if (!data.has(ability.id())) return false;
        set(player, data.withoutAbility(ability.id()));
        ability.onForgotten(player);
        NeoForge.EVENT_BUS.post(new AbilityEvent.Forget(player, ability));
        return true;
    }

    // puts a skill in one of the 50 slots (null clears it). takes it out of any other slot
    public static boolean assign(ServerPlayer player, int index, Ability ability) {
        if (index < 0 || index >= AbilityData.TOTAL_SLOTS) return false;
        AbilityData data = get(player);
        if (ability != null) {
            if (!data.has(ability.id()) || ability.isPassive()) return false;
            for (int i = 0; i < AbilityData.TOTAL_SLOTS; i++) {
                if (data.slotAt(i).map(ability.id()::equals).orElse(false)) data = data.withSlotAt(i, null);
            }
        }
        set(player, data.withSlotAt(index, ability == null ? null : ability.id()));
        return true;
    }

    public static void setPage(ServerPlayer player, int page) {
        set(player, get(player).withPage(page));
    }

    // starts a cooldown right now, for skills that set their own (chanted spells)
    // every cooldown shrinks as mastery grows: full price at no mastery, half once it's mastered, smoothly in between.
    // pass the unmastered cooldown, this does the rest
    public static void setCooldown(ServerPlayer player, Ability ability, int ticks) {
        long now = player.level().getGameTime();
        int t = scaledCooldown(player, ability, ticks);
        set(player, get(player).withCooldown(ability.id(), t <= 0 ? 0 : now + t));
    }

    public static int scaledCooldown(Player player, Ability ability, int ticks) {
        if (ticks <= 0) return 0;
        return Math.max(1, Math.round(ticks * (1f - 0.5f * masteryFraction(player, ability))));
    }

    // same for anything you have to hold or charge: the more you've used it, the quicker it comes out (half at mastery)
    public static int scaledCharge(Player player, Ability ability, int ticks) {
        if (ticks <= 0) return 0;
        return Math.max(1, Math.round(ticks * (1f - 0.5f * masteryFraction(player, ability))));
    }

    public static boolean onCooldown(ServerPlayer player, Ability ability) {
        return get(player).cooldownLeft(ability.id(), player.level().getGameTime()) > 0;
    }

    public static void resetCooldown(ServerPlayer player, Ability ability) {
        set(player, get(player).withCooldown(ability.id(), 0));
    }

    // mastery

    public static int mastery(Player player, Ability ability) {
        return get(player).masteryOf(ability.id());
    }

    public static boolean isMastered(Player player, Ability ability) {
        return mastery(player, ability) >= ability.maxMastery();
    }

    // 0 to 1
    public static float masteryFraction(Player player, Ability ability) {
        return Math.min(1f, mastery(player, ability) / (float) ability.maxMastery());
    }

    // adds mastery points and tells the player when it's mastered
    // mastery as a level from 1 to 10, 10 is mastered
    public static int level(Player player, Ability ability) {
        return isMastered(player, ability) ? 10 : 1 + (int) Math.floor(masteryFraction(player, ability) * 9);
    }

    public static void addMastery(ServerPlayer player, Ability ability, int points) {
        AbilityData data = get(player);
        if (!data.has(ability.id())) return;
        int before = data.masteryOf(ability.id()), max = ability.maxMastery();
        if (before >= max) return;
        int after = Math.min(max, before + points);
        set(player, data.withMastery(ability.id(), after));
        if (after >= max) {
            player.sendSystemMessage(Component.translatable("brightcore.message.mastered", ability.name()));
        }
    }

    // one mastery point for using a skill, after listeners change it (great sage multiplies it)
    // leftover fractions carry over so small gains still add up
    public static void addMasteryPoint(ServerPlayer player, Ability ability) {
        double pts = NeoForge.EVENT_BUS.post(new AbilityEvent.MasteryGain(player, ability, 1)).getPoints();
        double carried = MASTERY_REST.getOrDefault(player.getUUID(), 0.0) + pts;
        int whole = (int) Math.floor(carried);
        MASTERY_REST.put(player.getUUID(), carried - whole);
        if (whole > 0) addMastery(player, ability, whole);
    }

    private static final java.util.Map<java.util.UUID, Double> MASTERY_REST = new java.util.concurrent.ConcurrentHashMap<>();

    public static void setMastery(ServerPlayer player, Ability ability, int points) {
        set(player, get(player).withMastery(ability.id(), Math.min(points, ability.maxMastery())));
    }

    // modes and toggles

    public static int modeOf(Player player, Ability ability) {
        return Math.floorMod(get(player).modeOf(ability.id()), ability.modes().size());
    }

    // alt + key: next mode for the skill in that slot
    public static void cycleMode(ServerPlayer player, int slot) {
        get(player).slot(slot).map(AbilityRegistry::get).ifPresent(ability -> {
            if (ability.modes().size() < 2) return;
            int next = (modeOf(player, ability) + 1) % ability.modes().size();
            set(player, get(player).withMode(ability.id(), next));
            player.sendOverlayMessage(Component.translatable("brightcore.message.mode", ability.name(), ability.modeName(next)));
        });
    }

    // sets the mode straight from a menu
    public static void setMode(ServerPlayer player, Ability ability, int mode) {
        if (!has(player, ability) || mode < 0 || mode >= ability.modes().size()) return;
        set(player, get(player).withMode(ability.id(), mode));
    }

    // is a switchable skill's passive on. it's stored flipped in the toggle set
    // so every switchable skill starts on, old saves too
    public static boolean isSwitchedOn(Player player, Ability ability) {
        return has(player, ability) && (!ability.isSwitchable() || !get(player).isToggled(ability.id()));
    }

    public static void setSwitchedOn(ServerPlayer player, Ability ability, boolean on) {
        if (!ability.isSwitchable() || !has(player, ability) || isSwitchedOn(player, ability) == on) return;
        set(player, get(player).withToggled(ability.id(), !on));
        ability.onSwitch(player, on);
        player.sendOverlayMessage(Component.translatable(on ? "brightcore.message.toggle_on" : "brightcore.message.toggle_off", ability.name()));
    }

    // the menu switch. passives for switchables, on/off for toggles
    public static void flipSwitch(ServerPlayer player, Ability ability) {
        if (!has(player, ability)) return;
        if (ability.isSwitchable()) setSwitchedOn(player, ability, !isSwitchedOn(player, ability));
        else if (ability.isToggle()) {
            if (isToggled(player, ability)) setToggled(player, ability, false);
            else tryActivate(player, ability);
        }
    }

    public static boolean isToggled(Player player, Ability ability) {
        return get(player).isToggled(ability.id());
    }

    public static void setToggled(ServerPlayer player, Ability ability, boolean on) {
        if (isToggled(player, ability) == on) return;
        set(player, get(player).withToggled(ability.id(), on));
        ability.onToggle(player, on);
        player.sendOverlayMessage(Component.translatable(on ? "brightcore.message.toggle_on" : "brightcore.message.toggle_off", ability.name()));
    }

    // using skills

    // client pressed key 1-5 (comes in as 0-4), server checks what's in that slot on this page
    public static void activateSlot(ServerPlayer player, int slot) {
        get(player).slot(slot).map(AbilityRegistry::get).ifPresent(a -> tryActivate(player, a));
    }

    // client let go of a slot key, ends a held skill from that slot
    public static void releaseSlot(ServerPlayer player, int slot) {
        get(player).slot(slot).map(AbilityRegistry::get).ifPresent(a -> AbilityHolds.release(player, a));
    }

    public static boolean tryActivate(ServerPlayer player, Ability ability) {
        if (player.isSpectator() || !player.isAlive()) return false;
        AbilityData data = get(player);
        Identifier id = ability.id();
        if (!data.has(id) || ability.isPassive()) return false;

        // turning a toggle off is always free
        if (ability.isToggle() && data.isToggled(id)) {
            setToggled(player, ability, false);
            return true;
        }

        int modeIndex = modeOf(player, ability);
        Ability.Mode mode = ability.mode(modeIndex);
        AbilityEvent.Activate event = NeoForge.EVENT_BUS.post(new AbilityEvent.Activate(player, ability, mode.manaCost()));
        if (event.isCanceled()) return false;

        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();

        long left = ability.ignoresCooldown(player, modeIndex) ? 0 : data.cooldownLeft(id, now);
        if (left > 0) {
            player.sendOverlayMessage(Component.translatable("brightcore.message.cooldown",
                    ability.name(), String.format("%.1f", left / 20f)));
            return false;
        }
        boolean free = player.isCreative();
        int manaCost = free ? 0 : event.getManaCost();
        int auraCost = free ? 0 : mode.auraCost();
        var energy = EnergyAPI.get(player);
        if (energy.mana() < manaCost) {
            player.sendOverlayMessage(Component.translatable("brightcore.message.no_mana", ability.name()));
            return false;
        }
        if (energy.aura() < auraCost) {
            player.sendOverlayMessage(Component.translatable("brightcore.message.no_aura", ability.name()));
            return false;
        }

        if (ability.isToggle()) {
            setToggled(player, ability, true);
        } else if (ability.isHeld()) {
            if (!com.brightwebs.brightcore.api.util.Safe.call("ability " + id, () -> ability.activate(player, level, modeIndex), false)) return false;
            AbilityHolds.start(player, ability, modeIndex);
        } else if (!com.brightwebs.brightcore.api.util.Safe.call("ability " + id, () -> ability.activate(player, level, modeIndex), false)) {
            return false;
        }

        EnergyAPI.spendMana(player, manaCost);
        EnergyAPI.spendAura(player, auraCost);
        AbilityData after = get(player);  // the skill might've changed the data
        if (mode.cooldownTicks() > 0) after = after.withCooldown(id, now + scaledCooldown(player, ability, mode.cooldownTicks()));
        set(player, after);
        // skills give their own mastery points like tensura, so nothing gets added here

        NeoForge.EVENT_BUS.post(new AbilityEvent.Activated(player, ability, manaCost, auraCost));
        return true;
    }
}
