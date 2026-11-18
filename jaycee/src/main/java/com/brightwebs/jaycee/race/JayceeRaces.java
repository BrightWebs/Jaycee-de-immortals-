package com.brightwebs.jaycee.race;

import com.brightwebs.brightcore.api.race.Alignment;
import com.brightwebs.brightcore.api.race.Race;
import com.brightwebs.brightcore.api.race.RaceRegistry;
import com.brightwebs.jaycee.JayceeMod;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Every race line: five forms each, the last a Divine form (the road to godhood the Greek
 * pantheon will plug into later). All numbers come from one tier table, shaped per line by a
 * {@link Shape}, so balancing means changing a few numbers here.
 */
public final class JayceeRaces {
    private static final DeferredRegister<Race> RACES = DeferredRegister.create(RaceRegistry.REGISTRY, JayceeMod.ID);

    /** Base numbers per tier (index 0 = tier 1). */
    private static final double[] AURA_MIN = {700, 15_000, 120_000, 1_000_000, 8_000_000};
    private static final double[] AURA_MAX = {1_100, 30_000, 200_000, 1_600_000, 12_000_000};
    private static final double[] MANA_MIN = {50, 5_000, 50_000, 400_000, 3_000_000};
    private static final double[] MANA_MAX = {100, 10_000, 80_000, 600_000, 5_000_000};
    private static final double[] HEALTH = {20, 60, 160, 400, 1000};
    private static final double[] SPIRIT = {100, 400, 1_500, 5_000, 20_000};
    private static final double[] ATTACK = {1, 3, 6, 12, 25};
    private static final double[] ATTACK_SPEED = {4.0, 4.2, 4.4, 4.6, 4.8};
    private static final double[] SPEED = {0.10, 0.11, 0.12, 0.13, 0.14};
    private static final double[] KNOCKBACK = {0, 0.1, 0.25, 0.4, 0.6};
    /** EP and experience levels needed to reach tier 2, 3, 4 and 5. */
    private static final double[] EP_NEEDED = {10_000, 100_000, 1_000_000, 10_000_000};
    private static final int[] XP_NEEDED = {30, 50, 75, 100};

    /** How a line bends the base table: multipliers, plus size and nature. */
    private record Shape(double aura, double mana, double health, double attack, double speed,
                         double size, Alignment alignment, boolean holyAtPeak) {}

    private static final Shape BALANCED = new Shape(1, 1.2, 1, 1, 1, 1, Alignment.NEUTRAL, true);

    static {
        line(BALANCED, "human", "awakened_human", "hallowed_human", "exalted_human", "divine_human");
        line(new Shape(0.8, 1.5, 0.9, 0.9, 1.1, 1, Alignment.NEUTRAL, true),
                "elf", "high_elf", "elder_elf", "starborn_elf", "divine_elf");
        line(new Shape(1.2, 0.8, 1.3, 1.1, 0.9, 0.85, Alignment.NEUTRAL, true),
                "dwarf", "forgeborn_dwarf", "runelord_dwarf", "mountain_sovereign", "divine_dwarf");
        line(new Shape(1.1, 0.8, 1.1, 1.3, 1.2, 1, Alignment.NEUTRAL, true),
                "beastfolk", "beast_warrior", "beast_lord", "spirit_beast", "divine_beast");
        line(new Shape(0.8, 0.9, 0.8, 0.9, 1.15, 0.8, Alignment.NEUTRAL, true),
                "goblin", "hobgoblin", "goblin_warlord", "goblin_sovereign", "divine_goblin");
        line(new Shape(1.3, 0.7, 1.4, 1.3, 0.95, 1.1, Alignment.NEUTRAL, true),
                "orc", "high_orc", "orc_warlord", "orc_calamity", "divine_boar");
        line(new Shape(1.2, 0.9, 1.2, 1.4, 1, 1.15, Alignment.NEUTRAL, true),
                "ogre", "oni", "elder_oni", "oni_sovereign", "divine_oni");
        line(new Shape(0.8, 1.6, 0.7, 0.8, 0.95, 0.7, Alignment.NEUTRAL, true),
                "slime", "greater_slime", "elder_slime", "ancient_slime", "divine_slime");
        line(new Shape(1.2, 1, 1.2, 1.2, 1, 1.05, Alignment.NEUTRAL, true),
                "lizardfolk", "drakefolk", "dragonkin", "elder_dragonkin", "divine_dragon");
        line(new Shape(1.5, 0.7, 1.8, 1.5, 0.85, 1.6, Alignment.NEUTRAL, true),
                "giant", "hill_giant", "ancient_giant", "titanborn", "divine_titan");
        line(new Shape(1.1, 1.4, 1, 1.1, 1.05, 1, Alignment.DEMONIC, false),
                "lesser_daemon", "daemon", "greater_daemon", "archdaemon", "daemon_sovereign");
        line(new Shape(1, 1.3, 0.9, 1.1, 1, 1, Alignment.UNDEAD, false),
                "wight", "grave_knight", "wight_king", "lich_lord", "deathless_sovereign");
    }

    private JayceeRaces() {}

    /** Registers five forms in order; each evolves into the next. */
    /** Each line's Intrinsic skill, by its first form. */
    private static final java.util.Map<String, String> INTRINSIC = java.util.Map.ofEntries(
            java.util.Map.entry("human", "tenacity"),
            java.util.Map.entry("elf", "keen_senses"),
            java.util.Map.entry("dwarf", "stoneblood"),
            java.util.Map.entry("beastfolk", "beast_sprint"),
            java.util.Map.entry("goblin", "scavenger"),
            java.util.Map.entry("orc", "iron_gut"),
            java.util.Map.entry("ogre", "war_roar"),
            java.util.Map.entry("slime", "gelatinous_body"),
            java.util.Map.entry("lizardfolk", "scaled_hide"),
            java.util.Map.entry("giant", "earthshaker"),
            java.util.Map.entry("lesser_daemon", "infernal_blood"),
            java.util.Map.entry("wight", "deathless_flesh"));

    private static void line(Shape s, String... forms) {
        final String intrinsic = INTRINSIC.get(forms[0]);
        for (int i = 0; i < forms.length; i++) {
            final int t = i;
            final String next = i + 1 < forms.length ? forms[i + 1] : null;
            RACES.register(forms[i], () -> {
                Race.Properties p = new Race.Properties()
                        .tier(t + 1)
                        .aura(AURA_MIN[t] * s.aura(), AURA_MAX[t] * s.aura())
                        .mana(MANA_MIN[t] * s.mana(), MANA_MAX[t] * s.mana())
                        .health(Math.round(HEALTH[t] * s.health()))
                        .spirit(SPIRIT[t])
                        .attack(ATTACK[t] * s.attack())
                        .attackSpeed(ATTACK_SPEED[t])
                        .speed(SPEED[t] * s.speed())
                        .knockbackResistance(KNOCKBACK[t])
                        .size(s.size())
                        .alignment(t == 4 && s.holyAtPeak() ? Alignment.HOLY : s.alignment());
                if (intrinsic != null) p.intrinsic(Identifier.fromNamespaceAndPath(JayceeMod.ID, intrinsic));
                if (t == 0) p.starting();
                if (t == 4) p.divine();
                if (next != null) p.evolvesTo(Identifier.fromNamespaceAndPath(JayceeMod.ID, next), EP_NEEDED[t], XP_NEEDED[t]);
                return new Race(p);
            });
        }
    }

    public static void init(IEventBus modBus) {
        RACES.register(modBus);
    }
}
