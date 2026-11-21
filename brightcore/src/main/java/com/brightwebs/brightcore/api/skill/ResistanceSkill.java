package com.brightwebs.brightcore.api.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityCategory;
import com.brightwebs.brightcore.api.SkillType;

// passive that cuts one kind of damage and gets stronger the more you get hit by it
public final class ResistanceSkill extends Ability {
    private static final float[] RES_THRESHOLD = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
    private static final float[] RES_MULT = {0.9f, 0.8f, 0.7f, 0.6f, 0.5f};
    private static final float[] NUL_THRESHOLD = {0.5f, 0.6f, 0.7f, 0.8f, 0.9f};
    private static final float[] NUL_MULT = {0.5f, 0.4f, 0.3f, 0.2f, 0.1f};

    private final DamageKind kind;
    private final boolean nullification;

    public ResistanceSkill(DamageKind kind, boolean nullification) {
        super(new Properties().switchable().type(nullification ? SkillType.NULLIFICATION : SkillType.RESISTANCE)
                .category(AbilityCategory.DEFENSE).passive());
        this.kind = kind;
        this.nullification = nullification;
    }

    public DamageKind kind() { return kind; }
    public boolean isNullification() { return nullification; }

    // amount = incoming damage, currentHealth = defender's health now, mastery = 0 to 1
    // returns the damage that gets through
    // same rule as tensura. nullification blocks it all. resistance halves it,
    // unless one hit is more than half your current health, then it goes through full
    public float reduce(float amount, float currentHealth, float mastery) {
        if (nullification) return 0;
        if (amount > currentHealth * 0.5f) return amount;
        return amount * 0.5f;
    }
}
