package com.brightwebs.brightcore.api.skill;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityRegistry;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Skills Bright Core ships itself (one Resistance and one Nullification per damage kind),
 * plus the pool of Unique skills a player can be born with, which content mods fill.
 */
public final class BrightSkills {
    private static final DeferredRegister<Ability> SKILLS = DeferredRegister.create(AbilityRegistry.REGISTRY, BrightCore.ID);

    private static final Map<DamageKind, Identifier> RESISTANCE = new EnumMap<>(DamageKind.class);
    private static final Map<DamageKind, Identifier> NULLIFICATION = new EnumMap<>(DamageKind.class);
    private static final List<Identifier> STARTING_UNIQUES = new ArrayList<>();

    /** Damage of one kind a player must take (in hit points) to learn its Resistance. */
    public static final double DAMAGE_TO_LEARN = 200;
    /** EP needed, with the Resistance mastered, for it to become a Nullification. */
    public static final double EP_FOR_NULLIFICATION = 1_000_000;

    static {
        for (DamageKind kind : DamageKind.values()) {
            String r = kind.key() + "_resistance", n = kind.key() + "_nullification";
            SKILLS.register(r, () -> new ResistanceSkill(kind, false));
            SKILLS.register(n, () -> new ResistanceSkill(kind, true));
            RESISTANCE.put(kind, BrightCore.id(r));
            NULLIFICATION.put(kind, BrightCore.id(n));
        }
    }

    private BrightSkills() {}

    public static void init(IEventBus modBus) {
        SKILLS.register(modBus);
    }

    public static ResistanceSkill resistance(DamageKind kind) {
        return (ResistanceSkill) AbilityRegistry.get(RESISTANCE.get(kind));
    }

    public static ResistanceSkill nullification(DamageKind kind) {
        return (ResistanceSkill) AbilityRegistry.get(NULLIFICATION.get(kind));
    }

    /** Content mods add the Unique skills a newly reborn player can roll. */
    public static void addStartingUnique(Identifier skill) {
        if (!STARTING_UNIQUES.contains(skill)) STARTING_UNIQUES.add(skill);
    }

    public static List<Identifier> startingUniques() {
        return List.copyOf(STARTING_UNIQUES);
    }
}
