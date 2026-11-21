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

// skills bright core has itself (one resistance and one nullification per damage kind)
// plus the pool of uniques you can be born with, which content mods fill
public final class BrightSkills {
    private static final DeferredRegister<Ability> SKILLS = DeferredRegister.create(AbilityRegistry.REGISTRY, BrightCore.ID);

    private static final Map<DamageKind, Identifier> RESISTANCE = new EnumMap<>(DamageKind.class);
    private static final Map<DamageKind, Identifier> NULLIFICATION = new EnumMap<>(DamageKind.class);
    private static final List<Identifier> STARTING_UNIQUES = new ArrayList<>();

    // how much damage of one kind you have to take to learn its resistance
    public static final double DAMAGE_TO_LEARN = 200;
    // tensura's medium resistances (flame etc): 15 damage a point, 10x pool, 500 points
    public static final double HIT_FOR_POINT = 15, POOL_MULTIPLIER = 10, POINTS_TO_LEARN = 500;
    // EP you need, with the resistance mastered, for it to turn into a nullification
    public static final double EP_FOR_NULLIFICATION = 1_000_000;

    // content mod registers the resistance and nullification for a damage kind with its own names
    // if nobody registers one that kind just has no resistance
    public static void register(DeferredRegister<Ability> register, DamageKind kind, String resistance, String nullification) {
        register.register(resistance, () -> new ResistanceSkill(kind, false));
        register.register(nullification, () -> new ResistanceSkill(kind, true));
        RESISTANCE.put(kind, Identifier.fromNamespaceAndPath(register.getNamespace(), resistance));
        NULLIFICATION.put(kind, Identifier.fromNamespaceAndPath(register.getNamespace(), nullification));
    }

    private BrightSkills() {}

    public static void init(IEventBus modBus) {
        SKILLS.register(modBus);
    }

    public static ResistanceSkill resistance(DamageKind kind) {
        Identifier id = RESISTANCE.get(kind);
        return id == null ? null : (ResistanceSkill) AbilityRegistry.get(id);
    }

    public static ResistanceSkill nullification(DamageKind kind) {
        Identifier id = NULLIFICATION.get(kind);
        return id == null ? null : (ResistanceSkill) AbilityRegistry.get(id);
    }

    // content mods add the uniques a reborn player can roll
    public static void addStartingUnique(Identifier skill) {
        if (!STARTING_UNIQUES.contains(skill)) STARTING_UNIQUES.add(skill);
    }

    public static List<Identifier> startingUniques() {
        return List.copyOf(STARTING_UNIQUES);
    }
}
