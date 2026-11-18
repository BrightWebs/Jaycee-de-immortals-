package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.SkillType;
import com.brightwebs.jaycee.JayceeMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.List;

/** The glowing skill icon atlas (96px cells, 8 per row) and the rank badges. */
public final class SkillIcons {
    static final Identifier ICONS = Identifier.fromNamespaceAndPath(JayceeMod.ID, "textures/gui/skill_icons.png");
    static final Identifier PARTS = Identifier.fromNamespaceAndPath(JayceeMod.ID, "textures/gui/status_parts.png");

    private static final List<String> ORDER = List.of(
            "glutton", "phantom", "stormborn", "wrath", "bastion", "sage", "thought_acceleration", "hellfire",
            "spatial_step", "magic_sense", "body_reinforcement", "mana_bolt", "mana_dash", "tenacity", "keen_senses", "stoneblood",
            "beast_sprint", "scavenger", "iron_gut", "war_roar", "gelatinous_body", "scaled_hide", "earthshaker", "infernal_blood",
            "deathless_flesh");

    private SkillIcons() {}

    static int index(Ability a) {
        int i = ORDER.indexOf(a.id().getPath());
        if (i >= 0) return i;
        if (a.type() == SkillType.RESISTANCE) return 25;
        if (a.type() == SkillType.NULLIFICATION) return 26;
        return 27;
    }

    /** Draws a skill icon size x size at x, y (in whatever space the pose is in). */
    static void icon(GuiGraphicsExtractor g, Ability a, int x, int y, int size) {
        int i = index(a);
        g.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, (i % 8) * 96, (i / 8) * 96, size, size, 96, 96, 768, 384);
    }

    /** Badge row in the parts atlas for a skill type. */
    static int badge(SkillType t) {
        return switch (t) {
            case UNIQUE -> 0;
            case EXTRA -> 1;
            case COMMON -> 2;
            case INTRINSIC -> 3;
            case ULTIMATE -> 4;
            case RESISTANCE -> 5;
            case NULLIFICATION -> 6;
        };
    }

    static void badge(GuiGraphicsExtractor g, SkillType t, int x, int y, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, PARTS, x, y, 525, 105 + badge(t) * 40, w, h, 120, 30, 1024, 512);
    }
}
