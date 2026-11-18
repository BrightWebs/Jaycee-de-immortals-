package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.data.EnergyData;
import com.brightwebs.brightcore.client.BrightCoreClient;
import com.brightwebs.brightcore.client.BrightKeys;
import com.brightwebs.brightcore.data.AbilityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

/**
 * The bottom-right ability bar in the menu's sapphire style: four slots for the current preset,
 * the key under each slot, a draining cooldown shade with seconds left, a red tint when you
 * can't afford it, the mana bar and the page number.
 */
final class JayceeAbilityBar {
    private static final int SLOT = 22, GAP = 3, PAD = 9;
    private static final int W = PAD * 2 + AbilityData.SLOTS_PER_PAGE * SLOT + (AbilityData.SLOTS_PER_PAGE - 1) * GAP;
    private static final int H = 52;

    private JayceeAbilityBar() {}

    static void render(GuiGraphicsExtractor g, int sw, int sh, Player player, AbilityData data) {
        var font = Minecraft.getInstance().font;
        int x = sw - W - 4, y = sh - H - 4;

        DivineFrame.frame(g, x, y, W, H, false);

        // Top row: page on the left, mana bar on the right.
        EnergyData energy = EnergyAPI.get(player);
        g.text(font, (data.page() + 1) + "/" + AbilityData.PAGES, x + PAD, y + 6, 0xFF9CD4FF, false);
        int barX = x + 40, barW = W - 40 - PAD;
        g.fill(barX, y + 7, barX + barW, y + 12, 0xFF203B5A);
        float mana = energy.maxMana() <= 0 ? 0 : (float) (energy.mana() / energy.maxMana());
        g.fill(barX + 1, y + 8, barX + 1 + Math.round((barW - 2) * mana), y + 11, 0xFF52B7EE);

        // Four slots.
        int sy = y + 17;
        for (int i = 0; i < AbilityData.SLOTS_PER_PAGE; i++) {
            int sx = x + PAD + i * (SLOT + GAP);
            Ability a = BrightCoreClient.abilityIn(data, i);
            if (a == null) {
                g.fill(sx, sy, sx + SLOT, sy + SLOT, 0xFF355D9A);
                g.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, 0xCC0B1C38);
            } else {
                g.fill(sx, sy, sx + SLOT, sy + SLOT, 0xCC081626);
                SkillIcons.icon(g, a, sx + 1, sy + 1, SLOT - 2);
                float cd = BrightCoreClient.cooldownFraction(a, data);
                if (cd > 0) {
                    int h = Math.round((SLOT - 2) * cd);
                    g.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + 1 + h, 0xB0050A18);
                    g.centeredText(font, String.valueOf(BrightCoreClient.cooldownSeconds(a, data)), sx + SLOT / 2, sy + 7, 0xFFFFFFFF);
                } else if (!player.isCreative() && energy.mana() < a.manaCost()) {
                    g.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, 0x66E04050);
                }
            }
            String key = font.plainSubstrByWidth(BrightKeys.label(i), SLOT + 2);
            g.centeredText(font, key, sx + SLOT / 2, sy + SLOT + 3, 0xFFB5D9F4);
        }
    }
}
