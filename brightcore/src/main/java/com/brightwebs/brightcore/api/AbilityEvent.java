package com.brightwebs.brightcore.api;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

// events on NeoForge.EVENT_BUS so other mods can react to skills or block them
// without touching bright core (like an anti-magic field cancelling Activate)
public abstract class AbilityEvent extends Event {
    private final ServerPlayer player;
    private final Ability ability;

    protected AbilityEvent(ServerPlayer player, Ability ability) {
        this.player = player;
        this.ability = ability;
    }

    public ServerPlayer getPlayer() { return player; }
    public Ability getAbility() { return ability; }

    // before a skill fires. ownership is checked, cooldown and mana aren't yet. cancel to block it, cost can be changed
    public static final class Activate extends AbilityEvent implements ICancellableEvent {
        private int manaCost;

        public Activate(ServerPlayer player, Ability ability, int manaCost) {
            super(player, ability);
            this.manaCost = manaCost;
        }

        public int getManaCost() { return manaCost; }
        public void setManaCost(int manaCost) { this.manaCost = Math.max(0, manaCost); }
    }

    // after a skill fired and got paid for
    public static final class Activated extends AbilityEvent {
        private final int manaSpent, auraSpent;
        public Activated(ServerPlayer player, Ability ability, int manaSpent, int auraSpent) {
            super(player, ability); this.manaSpent = manaSpent; this.auraSpent = auraSpent;
        }
        public int getManaSpent() { return manaSpent; }
        public int getAuraSpent() { return auraSpent; }
    }

    // before a player learns a skill. cancel to stop it
    public static final class Learn extends AbilityEvent implements ICancellableEvent {
        public Learn(ServerPlayer player, Ability ability) { super(player, ability); }
    }

    // after a player lost a skill
    public static final class Forget extends AbilityEvent {
        public Forget(ServerPlayer player, Ability ability) { super(player, ability); }
    }

    // mastery points about to be added for one use. listeners can raise or lower it
    public static final class MasteryGain extends AbilityEvent {
        private double points;
        public MasteryGain(ServerPlayer player, Ability ability, double points) { super(player, ability); this.points = points; }
        public double getPoints() { return points; }
        public void setPoints(double points) { this.points = Math.max(0, points); }
    }
}
