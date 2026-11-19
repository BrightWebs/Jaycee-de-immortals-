package com.brightwebs.brightcore.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * A skill. Subclass it, override what you need, and register an instance with a DeferredRegister
 * built on {@link AbilityRegistry#REGISTRY}.
 *
 * <ul>
 *   <li><b>Type</b>: Intrinsic, Common, Extra, Unique, Ultimate, Resistance, Nullification.</li>
 *   <li><b>Modes</b>: a skill can have several (Shift + its key cycles them). Each mode has its own
 *       Mana and Aura cost and cooldown, and {@link #activate(ServerPlayer, ServerLevel, int)} gets the index.</li>
 *   <li><b>Toggle</b>: pressing the key turns it on or off; {@link #tick} only runs while it's on.</li>
 *   <li><b>Passive</b>: always on, can't go on the bar.</li>
 *   <li><b>Mastery</b>: grows with use; {@link AbilityAPI#isMastered} lets a skill hit harder once mastered.</li>
 * </ul>
 *
 * All hooks run on the SERVER after Bright Core has checked ownership, cooldown, Mana and Aura.
 */
public class Ability {
    private final Properties properties;

    public Ability(Properties properties) {
        this.properties = properties;
        if (properties.modes.isEmpty()) {
            properties.modes.add(new Mode("default", properties.manaCost, properties.auraCost, properties.cooldownTicks));
        }
    }

    // ---- hooks ---------------------------------------------------------------

    /** The key was pressed in this mode. Return true if it did something (costs and cooldown are only spent then). */
    public boolean activate(ServerPlayer player, ServerLevel level, int mode) {
        return activate(player, level);
    }

    /** Single-mode shortcut. */
    public boolean activate(ServerPlayer player, ServerLevel level) {
        return false;
    }

    /**
     * Held skills only: runs every tick while the key stays down (heldTicks counts from 0).
     * Return false to stop holding.
     */
    public boolean onHeld(ServerPlayer player, ServerLevel level, int mode, int heldTicks) { return true; }

    /** Held skills only: the key was let go after heldTicks ticks. */
    public void onRelease(ServerPlayer player, ServerLevel level, int mode, int heldTicks) {}

    /** Return true when this press skips the cooldown check (Tensura: Shift + Predation changes its block mode). */
    public boolean ignoresCooldown(ServerPlayer player, int mode) { return false; }

    /** Runs every tick: always for passives, only while toggled on for toggles. */
    public void tick(ServerPlayer player, ServerLevel level) {}

    /** A toggle was switched on or off. */
    public void onToggle(ServerPlayer player, boolean on) {}

    /** The owner hit something with a direct attack. */
    public void onHit(ServerPlayer player, LivingEntity target, float damage) {}

    public void onLearned(ServerPlayer player) {}

    public void onForgotten(ServerPlayer player) {}

    // ---- info ----------------------------------------------------------------

    public SkillType type() { return properties.type; }
    public AbilityCategory category() { return properties.category; }
    public boolean isPassive() { return properties.passive; }
    public boolean isToggle() { return properties.toggle; }
    /** Held skills work while the key is down (breaths, chanted spells). */
    public boolean isHeld() { return properties.held; }
    public List<Mode> modes() { return List.copyOf(properties.modes); }
    public Mode mode(int index) { return properties.modes.get(Math.floorMod(index, properties.modes.size())); }
    public int maxMastery() { return properties.type.maxMastery(); }
    public boolean isMagic() { return properties.type == SkillType.MAGIC; }
    /** Magic only: "aspectual", "elemental", "holy", "summoning" or "other". */
    public String magicClass() { return properties.magicClass; }
    /** Magic only: the school, e.g. "fire" (shown as "Fire Magic"). */
    public String school() { return properties.school; }
    public int castTicks() { return properties.castTicks; }
    public int range() { return properties.range; }

    /** First mode's numbers, for menus. */
    public int manaCost() { return mode(0).manaCost(); }
    public int cooldownTicks() { return mode(0).cooldownTicks(); }

    public Identifier id() {
        Identifier id = AbilityRegistry.REGISTRY.getKey(this);
        if (id == null) throw new IllegalStateException("Ability used before it was registered: " + getClass().getName());
        return id;
    }

    /** ability.<namespace>.<path>, .desc, and .mode.<key> */
    public String descriptionId() {
        Identifier id = id();
        return "ability." + id.getNamespace() + "." + id.getPath();
    }

    public Component name() { return Component.translatable(descriptionId()); }
    public Component description() { return Component.translatable(descriptionId() + ".desc"); }
    public Component modeName(int index) {
        return modes().size() == 1 ? name() : Component.translatable(descriptionId() + ".mode." + mode(index).key());
    }

    /** One way to use a skill. */
    public record Mode(String key, int manaCost, int auraCost, int cooldownTicks) {
        public static Mode of(String key, int mana, int aura, float cooldownSeconds) {
            return new Mode(key, mana, aura, Math.round(cooldownSeconds * 20));
        }
    }

    // ---- builder -------------------------------------------------------------

    public static final class Properties {
        private int manaCost = 0, auraCost = 0, cooldownTicks = 0;
        private SkillType type = SkillType.COMMON;
        private AbilityCategory category = AbilityCategory.OFFENSE;
        private boolean passive = false, toggle = false, held = false;
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
        /** Makes this a spell: its classification, school, casting time and range. */
        public Properties magic(String magicClass, String school, float castSeconds, int range) {
            this.type = SkillType.MAGIC; this.magicClass = magicClass; this.school = school;
            this.castTicks = Math.round(castSeconds * 20); this.range = range; return this;
        }
        public Properties toggle() { this.toggle = true; return this; }
        /** The skill keeps working while its key is held, and hears when it is released. */
        public Properties held() { this.held = true; return this; }
        /** Add a mode. Skills with no modes get one from manaCost/auraCost/cooldown. */
        public Properties mode(String key, int mana, int aura, float cooldownSeconds) {
            modes.add(Mode.of(key, mana, aura, cooldownSeconds));
            return this;
        }
    }
}
