package com.brightwebs.brightcore.api.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityCategory;
import com.brightwebs.brightcore.api.SkillType;

/**
 * A passive skill that blunts one kind of harm, and grows stronger as it's mastered by being hit.
 *
 * Each mastery fifth (0-20%, 20-40%...) sets two numbers: hits weaker than a share of your current
 * health are shrugged off completely, and anything stronger is cut by a multiplier.
 * Resistance: shrug off up to 10-50% of current health, cut the rest to 90-50%.
 * Nullification: shrug off up to 50-90%, cut the rest to 50-10%.
 */
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

    /**
     * @param amount        incoming damage
     * @param currentHealth the defender's health right now
     * @param mastery       0 to 1
     * @return the damage that gets through
     */
    /**
     * Tensura's rule. Nullification blocks the damage completely. Resistance halves it, unless a
     * single hit is more than half your current health, which goes through in full.
     */
    public float reduce(float amount, float currentHealth, float mastery) {
        if (nullification) return 0;
        if (amount > currentHealth * 0.5f) return amount;
        return amount * 0.5f;
    }
}
