package com.brightwebs.brightcore.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Base class for every ability. Subclass it, override what you need, and register an
 * instance with a DeferredRegister built on {@link AbilityRegistry#REGISTRY}.
 *
 * All hooks run on the SERVER. Bright Core has already checked ownership, cooldown and
 * mana before {@link #activate} is called, so abilities stay small.
 */
public abstract class Ability {
    private final Properties properties;

    protected Ability(Properties properties) {
        this.properties = properties;
    }

    // ---- hooks ---------------------------------------------------------------

    /** The ability key was pressed. Return true if it did something (cost and cooldown are only spent then). */
    public abstract boolean activate(ServerPlayer player, ServerLevel level);

    /** Runs every tick for every player who owns this ability. For passives. */
    public void tick(ServerPlayer player, ServerLevel level) {}

    /** The owner hit something with a direct attack. */
    public void onHit(ServerPlayer player, LivingEntity target, float damage) {}

    public void onLearned(ServerPlayer player) {}

    public void onForgotten(ServerPlayer player) {}

    // ---- info ----------------------------------------------------------------

    public int manaCost() { return properties.manaCost; }
    public int cooldownTicks() { return properties.cooldownTicks; }
    public AbilityCategory category() { return properties.category; }
    /** Passive abilities are ticked but can't be put on the ability bar. */
    public boolean isPassive() { return properties.passive; }

    /** Registry id, e.g. jaycee:thunderbolt. */
    public Identifier id() {
        Identifier id = AbilityRegistry.REGISTRY.getKey(this);
        if (id == null) throw new IllegalStateException("Ability used before it was registered: " + getClass().getName());
        return id;
    }

    /** Translation keys: ability.<namespace>.<path> and ability.<namespace>.<path>.desc */
    public String descriptionId() {
        Identifier id = id();
        return "ability." + id.getNamespace() + "." + id.getPath();
    }

    public Component name() { return Component.translatable(descriptionId()); }

    public Component description() { return Component.translatable(descriptionId() + ".desc"); }

    // ---- builder -------------------------------------------------------------

    public static final class Properties {
        private int manaCost = 0;
        private int cooldownTicks = 0;
        private AbilityCategory category = AbilityCategory.OFFENSE;
        private boolean passive = false;

        public Properties manaCost(int mana) { this.manaCost = mana; return this; }
        public Properties cooldown(int ticks) { this.cooldownTicks = ticks; return this; }
        public Properties cooldownSeconds(float seconds) { this.cooldownTicks = Math.round(seconds * 20); return this; }
        public Properties category(AbilityCategory category) { this.category = category; return this; }
        public Properties passive() { this.passive = true; return this; }
    }
}
