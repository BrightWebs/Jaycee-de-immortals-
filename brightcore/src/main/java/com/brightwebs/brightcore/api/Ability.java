package com.brightwebs.brightcore.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

// a skill. extend it, override what you need, register it on AbilityRegistry.REGISTRY
//
// type: intrinsic, common, extra, unique, ultimate, resistance or nullification
// modes: a skill can have a few, alt + key cycles them. each mode has its own cost and cooldown
// toggle: the key turns it on and off, tick() only runs while it's on
// passive: always on, can't be put on the bar
// mastery: goes up as you use it, check AbilityAPI.isMastered to make it stronger after
//
// all of this runs on the server, after we already checked you own it, the cooldown and the cost
public class Ability {
    private final Properties properties;

    public Ability(Properties properties) {
        this.properties = properties;
        if (properties.modes.isEmpty()) {
            properties.modes.add(new Mode("default", properties.manaCost, properties.auraCost, properties.cooldownTicks));
        }
    }

    // hooks

    // key got pressed in this mode. return true if it actually did something, cost and cooldown only get used then
    public boolean activate(ServerPlayer player, ServerLevel level, int mode) {
        return activate(player, level);
    }

    // for skills with just one mode
    public boolean activate(ServerPlayer player, ServerLevel level) {
        return false;
    }

    // held skills: runs every tick while the key is down. return false to stop
    public boolean onHeld(ServerPlayer player, ServerLevel level, int mode, int heldTicks) { return true; }

    // held skills: key was let go after heldTicks ticks
    public void onRelease(ServerPlayer player, ServerLevel level, int mode, int heldTicks) {}

    // return true if this press shouldn't care about the cooldown (like shift + predation switching block mode)
    public boolean ignoresCooldown(ServerPlayer player, int mode) { return false; }

    // every tick. passives always, toggles only while on
    public void tick(ServerPlayer player, ServerLevel level) {}

    // the passives switch got flipped in the skill menu
    public void onSwitch(ServerPlayer player, boolean on) {}

    // toggle got turned on or off
    public void onToggle(ServerPlayer player, boolean on) {}

    // owner hit something directly
    public void onHit(ServerPlayer player, LivingEntity target, float damage) {}

    public void onLearned(ServerPlayer player) {}

    public void onForgotten(ServerPlayer player) {}

    // info

    public SkillType type() { return properties.type; }
    public AbilityCategory category() { return properties.category; }
    public boolean isPassive() { return properties.passive; }
    public boolean isToggle() { return properties.toggle; }
    // held skills keep going while you hold the key (breaths, chanted spells)
    public boolean isHeld() { return properties.held; }
    // switchable skills get an on/off switch in the skill menu for their passive stuff
    // (great sage, resistances, etc). they start on
    public boolean isSwitchable() { return properties.switchable; }
    public List<Mode> modes() { return List.copyOf(properties.modes); }
    public Mode mode(int index) { return properties.modes.get(Math.floorMod(index, properties.modes.size())); }
    public int maxMastery() { return properties.type.maxMastery(); }
    public boolean isMagic() { return properties.type == SkillType.MAGIC; }
    // magic only: aspectual, elemental, holy, summoning or other
    public String magicClass() { return properties.magicClass; }
    // magic only: the school, like "fire" (shows as Fire Magic)
    public String school() { return properties.school; }
    public int castTicks() { return properties.castTicks; }
    public int range() { return properties.range; }

    // first mode's numbers, for the menus
    public int manaCost() { return mode(0).manaCost(); }
    public int cooldownTicks() { return mode(0).cooldownTicks(); }

    public Identifier id() {
        Identifier id = AbilityRegistry.REGISTRY.getKey(this);
        if (id == null) throw new IllegalStateException("Ability used before it was registered: " + getClass().getName());
        return id;
    }

    // ability.<namespace>.<path>, .desc and .mode.<key>
    public String descriptionId() {
        Identifier id = id();
        return "ability." + id.getNamespace() + "." + id.getPath();
    }

    public Component name() { return Component.translatable(descriptionId()); }
    public Component description() { return Component.translatable(descriptionId() + ".desc"); }
    public Component modeName(int index) {
        return modes().size() == 1 ? name() : Component.translatable(descriptionId() + ".mode." + mode(index).key());
    }

    // one way to use a skill
    public record Mode(String key, int manaCost, int auraCost, int cooldownTicks) {
        public static Mode of(String key, int mana, int aura, float cooldownSeconds) {
            return new Mode(key, mana, aura, Math.round(cooldownSeconds * 20));
        }
    }

    // builder

    public static final class Properties {
        private int manaCost = 0, auraCost = 0, cooldownTicks = 0;
        private SkillType type = SkillType.COMMON;
        private AbilityCategory category = AbilityCategory.OFFENSE;
        private boolean passive = false, toggle = false, held = false, switchable = false;
        private final List<Mode> modes = new ArrayList<>();
        private String magicClass = "", school = "";
        private int castTicks = 0, range = 0;

        public Properties manaCost(int mana) { this.manaCost = mana; return this; }
        public Properties auraCost(int aura) { this.auraCost = aura; return this; }
        public Properties cooldown(int ticks) { this.cooldownTicks = ticks; return this; }
        public Properties cooldownSeconds(float seconds) { this.cooldownTicks = Math.round(seconds * 20); return this; }
        public Properties type(SkillType type) { this.type = type; return this; }
        public Properties category(AbilityCategory category) { this.category = category; return this; }
        public Properties passive() { this.passive = true; return this; }
        // makes it a spell: classification, school, cast time and range
        public Properties magic(String magicClass, String school, float castSeconds, int range) {
            this.type = SkillType.MAGIC; this.magicClass = magicClass; this.school = school;
            this.castTicks = Math.round(castSeconds * 20); this.range = range; return this;
        }
        public Properties toggle() { this.toggle = true; return this; }
        // keeps working while the key is held, and gets told when you let go
        public Properties held() { this.held = true; return this; }
        // gives it the on/off switch for passives (starts on)
        public Properties switchable() { this.switchable = true; return this; }
        // adds a mode. if there's none it just uses manaCost/auraCost/cooldown
        public Properties mode(String key, int mana, int aura, float cooldownSeconds) {
            modes.add(Mode.of(key, mana, aura, cooldownSeconds));
            return this;
        }
    }
}
