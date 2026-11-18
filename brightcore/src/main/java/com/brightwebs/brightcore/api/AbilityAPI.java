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

/**
 * The one place other code goes through. Every rule (ownership, cooldown, mana, slots)
 * lives here, so abilities never check anything themselves.
 */
public final class AbilityAPI {
    private AbilityAPI() {}

    // ---- reading (works on the client too, using the synced copy) ---------------

    public static AbilityData get(Player player) {
        return player.getData(BrightAttachments.ABILITIES);
    }

    public static boolean has(Player player, Ability ability) {
        return get(player).has(ability.id());
    }

    /** Owned abilities that still exist, sorted by name id for a stable menu order. */
    public static List<Ability> owned(Player player) {
        List<Ability> list = new ArrayList<>();
        for (Identifier id : get(player).owned()) {
            Ability a = AbilityRegistry.get(id);
            if (a != null) list.add(a);
        }
        list.sort(Comparator.comparing(a -> a.id().toString()));
        return list;
    }

    // ---- changing (server only) --------------------------------------------------

    private static void set(ServerPlayer player, AbilityData data) {
        player.setData(BrightAttachments.ABILITIES, data); // saves + syncs
    }

    /** Gives an ability. Returns false if already owned or a listener cancelled it. */
    public static boolean learn(ServerPlayer player, Ability ability) {
        AbilityData data = get(player);
        if (data.has(ability.id())) return false;
        if (NeoForge.EVENT_BUS.post(new AbilityEvent.Learn(player, ability)).isCanceled()) return false;

        data = data.withAbility(ability.id());
        // Put it in the first free slot, starting on the current page.
        if (!ability.isPassive()) {
            for (int n = 0; n < AbilityData.TOTAL_SLOTS; n++) {
                int index = (data.page() * AbilityData.SLOTS_PER_PAGE + n) % AbilityData.TOTAL_SLOTS;
                if (data.slotAt(index).isEmpty()) { data = data.withSlotAt(index, ability.id()); break; }
            }
        }
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

    /** Puts an ability in one of the 50 slots (or clears it when ability is null). Removes it from any other slot. */
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

    public static void resetCooldown(ServerPlayer player, Ability ability) {
        set(player, get(player).withCooldown(ability.id(), 0));
    }

    // ---- mastery ---------------------------------------------------------------

    public static int mastery(Player player, Ability ability) {
        return get(player).masteryOf(ability.id());
    }

    public static boolean isMastered(Player player, Ability ability) {
        return mastery(player, ability) >= ability.maxMastery();
    }

    /** 0 to 1. */
    public static float masteryFraction(Player player, Ability ability) {
        return Math.min(1f, mastery(player, ability) / (float) ability.maxMastery());
    }

    /** Adds mastery points, telling the player when the skill becomes mastered. */
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

    public static void setMastery(ServerPlayer player, Ability ability, int points) {
        set(player, get(player).withMastery(ability.id(), Math.min(points, ability.maxMastery())));
    }

    // ---- modes and toggles ---------------------------------------------------------

    public static int modeOf(Player player, Ability ability) {
        return Math.floorMod(get(player).modeOf(ability.id()), ability.modes().size());
    }

    /** Shift + key: switch the skill in that slot to its next mode. */
    public static void cycleMode(ServerPlayer player, int slot) {
        get(player).slot(slot).map(AbilityRegistry::get).ifPresent(ability -> {
            if (ability.modes().size() < 2) return;
            int next = (modeOf(player, ability) + 1) % ability.modes().size();
            set(player, get(player).withMode(ability.id(), next));
            player.sendOverlayMessage(Component.translatable("brightcore.message.mode", ability.name(), ability.modeName(next)));
        });
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

    // ---- activation ----------------------------------------------------------------

    /** The client pressed key 1-5 (sent as 0-4). The server looks up what's in that slot on the current page. */
    public static void activateSlot(ServerPlayer player, int slot) {
        get(player).slot(slot).map(AbilityRegistry::get).ifPresent(a -> tryActivate(player, a));
    }

    public static boolean tryActivate(ServerPlayer player, Ability ability) {
        if (player.isSpectator() || !player.isAlive()) return false;
        AbilityData data = get(player);
        Identifier id = ability.id();
        if (!data.has(id) || ability.isPassive()) return false;

        // Turning a toggle off is always free.
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

        long left = data.cooldownLeft(id, now);
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
        } else if (!ability.activate(player, level, modeIndex)) {
            return false;
        }

        EnergyAPI.spendMana(player, manaCost);
        EnergyAPI.spendAura(player, auraCost);
        AbilityData after = get(player); // the skill itself may have changed the data
        if (mode.cooldownTicks() > 0) after = after.withCooldown(id, now + mode.cooldownTicks());
        set(player, after);
        addMastery(player, ability, 1);

        NeoForge.EVENT_BUS.post(new AbilityEvent.Activated(player, ability));
        return true;
    }
}
