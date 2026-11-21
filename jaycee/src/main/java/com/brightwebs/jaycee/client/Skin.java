package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.SkillType;

// which texture set a skill uses (card, badge, pill), by its rank
final class Skin {
    private Skin() {}

    static String rank(Ability a) {
        return switch (a.type()) {
            case UNIQUE -> "unique";
            case EXTRA -> "extra";
            case COMMON -> "common";
            case INTRINSIC -> "intrinsic";
            case ULTIMATE -> "ultimate";
            case RESISTANCE -> "resistance";
            case NULLIFICATION -> "nullification";
            case MAGIC -> "magic";
        };
    }

    // the outline pill in the libraries: rank for skills, classification for magic
    static String pill(Ability a) {
        return a.isMagic() ? (a.magicClass().isEmpty() ? "other" : a.magicClass()) : rank(a);
    }

    static boolean inCategory(Ability a, String cat) {
        return switch (cat) {
            case "resistance" -> a.type() == SkillType.RESISTANCE || a.type() == SkillType.NULLIFICATION;
            case "intrinsic" -> a.type() == SkillType.INTRINSIC;
            case "common" -> a.type() == SkillType.COMMON;
            case "extra" -> a.type() == SkillType.EXTRA;
            case "unique" -> a.type() == SkillType.UNIQUE;
            case "ultimate" -> a.type() == SkillType.ULTIMATE;
            default -> a.isMagic() && pill(a).equals(cat);
        };
    }
}
