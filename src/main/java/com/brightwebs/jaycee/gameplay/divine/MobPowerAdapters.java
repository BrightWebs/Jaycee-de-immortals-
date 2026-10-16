package com.brightwebs.jaycee.gameplay.divine;

import com.brightwebs.jaycee.gameplay.SkillDefinitions;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.*;

/** Explicit opt-in adapters. No AI, owner, entity type, or resource pool is replaced. */
public final class MobPowerAdapters {
    private MobPowerAdapters() {}
    private static final ResourceLocation SPEED = ResourceLocation.fromNamespaceAndPath("jaycee", "mob_divine_speed");
    private static final Map<ResourceLocation, Predicate<Mob>> SPECIES = new HashMap<>();
    private static final Set<String> UNSUPPORTED_SPEED = Set.of("bat", "bee", "blaze", "ghast", "parrot", "phantom",
        "cod", "dolphin", "pufferfish", "salmon", "squid", "glow_squid", "tadpole", "tropical_fish", "guardian",
        "slime", "magma_cube");
    public interface Adapter {
        String id();
        String effect();
        String reason(Mob mob);
        void tick(Mob mob);
        void clear(Mob mob);
    }
    private static final Map<String, Adapter> ADAPTERS = new LinkedHashMap<>();
    static {
        for (String type : List.of("armadillo", "axolotl", "bat", "bee", "blaze", "bogged", "camel", "cat",
                "cave_spider", "chicken", "cod", "cow", "creeper", "dolphin", "donkey", "drowned", "enderman",
                "endermite", "fox", "frog", "ghast", "glow_squid", "goat", "guardian", "hoglin", "horse",
                "husk", "iron_golem", "llama", "magma_cube", "mooshroom", "mule", "ocelot", "panda", "parrot",
                "phantom", "pig", "piglin", "piglin_brute", "pillager", "polar_bear", "pufferfish", "rabbit",
                "salmon", "sheep", "silverfish", "skeleton", "skeleton_horse", "slime", "sniffer", "snow_golem",
                "spider", "squid", "stray", "strider", "tadpole", "trader_llama", "tropical_fish", "turtle",
                "villager", "vindicator", "wandering_trader", "witch", "wolf", "zoglin", "zombie", "zombie_horse",
                "zombie_villager", "zombified_piglin"))
            registerSpecies(ResourceLocation.fromNamespaceAndPath("minecraft", type), mob -> true);
        register(new Adapter() {
            public String id() { return SkillDefinitions.SPEED; }
            public String effect() { return "Mob implementation tuning (not canon): passive +20% base ground movement speed, capped at +0.10 attribute units; no AI changes or Essence regeneration."; }
            public String reason(Mob mob) {
                String type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath();
                if (UNSUPPORTED_SPEED.contains(type))
                    return "Flying, swimming or hopping movement needs a dedicated speed adapter; no functional ground-speed grant";
                return mob.getAttribute(Attributes.MOVEMENT_SPEED) == null ? "No movement speed attribute" : "";
            }
            public void tick(Mob mob) {
                var a = mob.getAttribute(Attributes.MOVEMENT_SPEED);
                if (a == null) return;
                double bonus = Math.clamp(a.getBaseValue() * 0.2, 0, 0.10);
                var current = a.getModifier(SPEED);
                if (current != null && current.amount() == bonus) return;
                a.removeModifier(SPEED);
                if (bonus > 0) a.addTransientModifier(new AttributeModifier(SPEED, bonus, AttributeModifier.Operation.ADD_VALUE));
            }
            public void clear(Mob mob) {
                var a = mob.getAttribute(Attributes.MOVEMENT_SPEED);
                if (a != null) a.removeModifier(SPEED);
            }
        });
        register(new Adapter() {
            public String id() { return SkillDefinitions.REGEN; }
            public String effect() {
                return "Provisional mob-only passive adapter, NOT the player's mana-paid Regeneration: up to 1 health point "
                    + "(half a heart) per 20 server ticks, bounded by missing health. No fuel or upkeep; no EP, Essence "
                    + "or mana creation/consumption. One-time bestowal power fee still applies. Not canon or evolution.";
            }
            public String reason(Mob mob) { return mob.isInvertedHealAndHarm() ? "Undead healing is unsupported" : ""; }
            public void tick(Mob mob) {
                if (mob.tickCount % 20 != 0 || !mob.isAlive() || mob.getHealth() >= mob.getMaxHealth()
                        || mob.getServer() == null) return;
                mob.heal(Math.min(1.0F, mob.getMaxHealth() - mob.getHealth()));
            }
            public void clear(Mob mob) {}
        });
    }
    public static void register(Adapter adapter) {
        if (ADAPTERS.putIfAbsent(adapter.id(), adapter) != null)
            throw new IllegalArgumentException("Duplicate mob power adapter");
    }
    /** Server integration extension point: review a species before explicitly opting it in. */
    public static void registerSpecies(ResourceLocation type, Predicate<Mob> compatibility) {
        if (SPECIES.putIfAbsent(Objects.requireNonNull(type), Objects.requireNonNull(compatibility)) != null)
            throw new IllegalArgumentException("Duplicate mob species adapter");
    }
    public static String eligibility(Mob mob) {
        var type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        var compatible = SPECIES.get(type);
        return compatible != null && compatible.test(mob) ? ""
            : "Boss, scripted, modded, or unreviewed entity: no safe species adapter registered";
    }
    public static String reason(Mob mob, String ability) {
        var reason = eligibility(mob);
        if (!reason.isEmpty() || ability.isEmpty()) return reason;
        var adapter = ADAPTERS.get(ability);
        return adapter == null ? "No functional registered mob adapter for this power" : adapter.reason(mob);
    }
    public static String effect(String ability) {
        var adapter = ADAPTERS.get(ability);
        return adapter == null ? "Unavailable: no safe mob implementation" : adapter.effect();
    }
    public static void tick(Mob mob, DivinityModel.MobProfile profile) {
        if (profile == null) return;
        for (var adapter : ADAPTERS.values()) {
            if (profile != null && profile.status().equals("active") && profile.grants().containsKey(adapter.id())
                    && profile.type().equals(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString())
                    && reason(mob, adapter.id()).isEmpty())
                adapter.tick(mob);
            else adapter.clear(mob);
        }
    }
    public static void clear(Mob mob) {
        for (var adapter : ADAPTERS.values()) adapter.clear(mob);
    }
}
