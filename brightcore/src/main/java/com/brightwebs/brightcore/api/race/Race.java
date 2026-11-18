package com.brightwebs.brightcore.api.race;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * A race (or one form of a race line). Register instances with a DeferredRegister on
 * {@link RaceRegistry#REGISTRY}. Everything is data; there are no hooks to override yet.
 *
 * Aura and Mana are rolled between min and max when a player becomes this race.
 * Body stats are absolute values (health 20 = ten hearts, speed 0.1 = a normal player).
 */
public final class Race {
    private final Properties p;

    public Race(Properties properties) {
        this.p = properties;
    }

    public double minAura() { return p.minAura; }
    public double maxAura() { return p.maxAura; }
    public double minMana() { return p.minMana; }
    public double maxMana() { return p.maxMana; }
    public double health() { return p.health; }
    public double spirit() { return p.spirit; }
    public double attack() { return p.attack; }
    public double attackSpeed() { return p.attackSpeed; }
    public double speed() { return p.speed; }
    public double knockbackResistance() { return p.knockback; }
    public double size() { return p.size; }
    public Alignment alignment() { return p.alignment; }
    /** Shown in the race menu on first join. */
    public boolean isStarting() { return p.starting; }
    /** The last form of a line: a being of divinity. */
    public boolean isDivine() { return p.divine; }
    /** 1 for a starting race, then 2, 3... up the line. */
    public int tier() { return p.tier; }
    public List<Evolution> evolutions() { return List.copyOf(p.evolutions); }

    public Identifier id() {
        Identifier id = RaceRegistry.REGISTRY.getKey(this);
        if (id == null) throw new IllegalStateException("Race used before it was registered");
        return id;
    }

    /** race.<namespace>.<path> and race.<namespace>.<path>.desc */
    public String descriptionId() {
        Identifier id = id();
        return "race." + id.getNamespace() + "." + id.getPath();
    }

    public Component name() { return Component.translatable(descriptionId()); }
    public Component description() { return Component.translatable(descriptionId() + ".desc"); }

    /**
     * One way this race can evolve.
     *
     * @param target    the race it becomes
     * @param epNeeded  EP (max Aura + max Mana) needed
     * @param xpLevels  experience levels needed (spent on evolving)
     */
    public record Evolution(Identifier target, double epNeeded, int xpLevels) {}

    public static final class Properties {
        private double minAura = 500, maxAura = 1000, minMana = 50, maxMana = 100;
        private double health = 20, spirit = 100, attack = 1, attackSpeed = 4, speed = 0.1, knockback = 0, size = 1;
        private Alignment alignment = Alignment.NEUTRAL;
        private boolean starting = false, divine = false;
        private int tier = 1;
        private final List<Evolution> evolutions = new ArrayList<>();

        public Properties aura(double min, double max) { this.minAura = min; this.maxAura = max; return this; }
        public Properties mana(double min, double max) { this.minMana = min; this.maxMana = max; return this; }
        public Properties health(double v) { this.health = v; return this; }
        public Properties spirit(double v) { this.spirit = v; return this; }
        public Properties attack(double v) { this.attack = v; return this; }
        public Properties attackSpeed(double v) { this.attackSpeed = v; return this; }
        public Properties speed(double v) { this.speed = v; return this; }
        public Properties knockbackResistance(double v) { this.knockback = v; return this; }
        public Properties size(double v) { this.size = v; return this; }
        public Properties alignment(Alignment a) { this.alignment = a; return this; }
        public Properties starting() { this.starting = true; return this; }
        public Properties divine() { this.divine = true; return this; }
        public Properties tier(int t) { this.tier = t; return this; }
        public Properties evolvesTo(Identifier target, double epNeeded, int xpLevels) {
            evolutions.add(new Evolution(target, epNeeded, xpLevels));
            return this;
        }
    }
}
