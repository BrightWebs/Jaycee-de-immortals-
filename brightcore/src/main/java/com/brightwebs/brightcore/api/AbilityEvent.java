package com.brightwebs.brightcore.api;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Events on NeoForge.EVENT_BUS so content mods can react to or block abilities
 * without touching Bright Core. (A future anti-magic field cancels Activate, for example.)
 */
public abstract class AbilityEvent extends Event {
    private final ServerPlayer player;
    private final Ability ability;

    protected AbilityEvent(ServerPlayer player, Ability ability) {
        this.player = player;
        this.ability = ability;
    }

    public ServerPlayer getPlayer() { return player; }
    public Ability getAbility() { return ability; }

    /** Before an ability fires (ownership checked; cooldown and mana not yet). Cancel to block it; the cost can be changed. */
    public static final class Activate extends AbilityEvent implements ICancellableEvent {
        private int manaCost;

        public Activate(ServerPlayer player, Ability ability, int manaCost) {
            super(player, ability);
            this.manaCost = manaCost;
        }

        public int getManaCost() { return manaCost; }
        public void setManaCost(int manaCost) { this.manaCost = Math.max(0, manaCost); }
    }

    /** After an ability fired and its cost was paid. */
    public static final class Activated extends AbilityEvent {
        private final int manaSpent, auraSpent;
        public Activated(ServerPlayer player, Ability ability, int manaSpent, int auraSpent) {
            super(player, ability); this.manaSpent = manaSpent; this.auraSpent = auraSpent;
        }
        public int getManaSpent() { return manaSpent; }
        public int getAuraSpent() { return auraSpent; }
    }

    /** Before a player learns an ability. Cancel to prevent it. */
    public static final class Learn extends AbilityEvent implements ICancellableEvent {
        public Learn(ServerPlayer player, Ability ability) { super(player, ability); }
    }

    /** After a player lost an ability. */
    public static final class Forget extends AbilityEvent {
        public Forget(ServerPlayer player, Ability ability) { super(player, ability); }
    }
}
