package com.brightwebs.brightcore.api.race;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

// a race, or one form in a race line. register it on RaceRegistry.REGISTRY
// it's all just data for now, nothing to override
//
// aura and mana get rolled between min and max when you become it
// body stats are the real values (health 20 = ten hearts, speed 0.1 = normal player)
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
    // shows up in the race menu when you first join
    public boolean isStarting() { return p.starting; }
    // the last form in a line, a divine being
    public boolean isDivine() { return p.divine; }
    // 1 for starting races, then 2, 3... going up
    public int tier() { return p.tier; }
    public List<Evolution> evolutions() { return List.copyOf(p.evolutions); }
    // skills every member of this form gets when they become it
    public List<Identifier> intrinsics() { return List.copyOf(p.intrinsics); }

    public Identifier id() {
        Identifier id = RaceRegistry.REGISTRY.getKey(this);
        if (id == null) throw new IllegalStateException("Race used before it was registered");
        return id;
    }

    // race.<namespace>.<path> and race.<namespace>.<path>.desc
    public String descriptionId() {
        Identifier id = id();
        return "race." + id.getNamespace() + "." + id.getPath();
    }

    public Component name() { return Component.translatable(descriptionId()); }
    public Component description() { return Component.translatable(descriptionId() + ".desc"); }

    // one way this race can evolve
    // target = what it turns into, epNeeded = EP needed, xpLevels = levels spent on evolving
    public record Evolution(Identifier target, double epNeeded, int xpLevels, List<Requirement> requirements) {
        public Evolution(Identifier target, double epNeeded, int xpLevels) {
            this(target, epNeeded, xpLevels, List.of(new Requirement("ep", "", epNeeded, 100)));
        }
    }

    // one part of an evolution's requirements, tensura style. each part is worth weight %
    // and you can evolve at 100%
    // types: ep, item_consume / item_carry (arg = item id), skill (arg = skill id),
    // plus whatever the content mod adds (boss, awaken, named...)
    public record Requirement(String type, String arg, double amount, float weight) {}

    public static final class Properties {
        private double minAura = 500, maxAura = 1000, minMana = 50, maxMana = 100;
        private double health = 20, spirit = 100, attack = 1, attackSpeed = 4, speed = 0.1, knockback = 0, size = 1;
        private Alignment alignment = Alignment.NEUTRAL;
        private boolean starting = false, divine = false;
        private int tier = 1;
        private final List<Evolution> evolutions = new ArrayList<>();
        private final List<Identifier> intrinsics = new ArrayList<>();

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
        public Properties intrinsic(Identifier... skills) {
            intrinsics.addAll(List.of(skills));
            return this;
        }
        public Properties evolvesTo(Identifier target, List<Requirement> requirements) {
            double ep = requirements.stream().filter(r -> r.type().equals("ep")).mapToDouble(Requirement::amount).max().orElse(0);
            evolutions.add(new Evolution(target, ep, 0, List.copyOf(requirements)));
            return this;
        }
        public Properties evolvesTo(Identifier target, double epNeeded, int xpLevels) {
            evolutions.add(new Evolution(target, epNeeded, xpLevels));
            return this;
        }
    }
}
