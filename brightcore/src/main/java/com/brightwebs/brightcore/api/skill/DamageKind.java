package com.brightwebs.brightcore.api.skill;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Predicate;

/** The kinds of harm a resistance can guard against. */
public enum DamageKind {
    FLAME("flame", s -> s.is(DamageTypeTags.IS_FIRE)),
    COLD("cold", s -> s.is(DamageTypeTags.IS_FREEZING)),
    LIGHTNING("lightning", s -> s.is(DamageTypeTags.IS_LIGHTNING)),
    EXPLOSION("explosion", s -> s.is(DamageTypeTags.IS_EXPLOSION)),
    POISON("poison", s -> s.is(DamageTypes.MAGIC) && s.getEntity() == null),
    MAGIC("magic", s -> s.is(DamageTypes.INDIRECT_MAGIC) || s.is(DamageTypes.SONIC_BOOM)
            || (s.is(DamageTypes.MAGIC) && s.getEntity() != null)),
    DECAY("decay", s -> s.is(DamageTypes.WITHER) || s.is(DamageTypes.WITHER_SKULL) || s.is(DamageTypes.DRAGON_BREATH)),
    FALL("fall", s -> s.is(DamageTypeTags.IS_FALL)),
    PHYSICAL("physical", s -> s.is(DamageTypeTags.IS_PROJECTILE)
            || s.is(DamageTypes.MOB_ATTACK) || s.is(DamageTypes.MOB_ATTACK_NO_AGGRO)
            || s.is(DamageTypes.PLAYER_ATTACK) || s.is(DamageTypes.STING) || s.is(DamageTypes.THORNS)
            || (s.getDirectEntity() instanceof LivingEntity && !s.is(DamageTypeTags.IS_FIRE)));

    private final String key;
    private final Predicate<DamageSource> test;

    DamageKind(String key, Predicate<DamageSource> test) {
        this.key = key;
        this.test = test;
    }

    public String key() { return key; }

    public boolean matches(DamageSource source) { return test.test(source); }
}
