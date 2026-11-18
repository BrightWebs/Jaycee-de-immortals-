package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.client.BrightCoreClient;
import com.brightwebs.brightcore.client.BrightKeys;
import com.brightwebs.brightcore.data.AbilityData;
import com.brightwebs.brightcore.data.EnergyData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

/**
 * The bottom-right HUD bar in the Existence System style: the four Z/X/C/V skills of the current
 * preset with their icons, cooldown shade, an "on" line for toggles, a red tint when you can't
 * afford them, the preset number and a mana bar.
 */
final class JayceeAbilityBar {
    private static final int SLOT = 24, GAP = 4, PAD = 6;
    private static final int W = PAD * 2 + AbilityData.SLOTS_PER_PAGE * SLOT + (AbilityData.SLOTS_PER_PAGE - 1) * GAP;
    private static final int H = 50;

    private JayceeAbilityBar() {}

    static void render(GuiGraphicsExtractor g, int sw, int sh, Player player, AbilityData data) {
        int x = sw - W - 6, y = sh - H - 6;
        UI.fill(g, x, y, x + W, y + H, 0xC0050C16);
        UI.rect(g, x, y, x + W, y + H, 1, 0xFF2FB8E0);
        UI.fill(g, x + 1, y + 1, x + W - 1, y + 2, 0x803FE0FF);
        EnergyData energy = EnergyAPI.get(player);
        UI.semi(g, "P" + (data.page() + 1), x + PAD, y + 7, 7, 0xFF9FE6FF, 0);
        int barX = x + 22, barW = W - 22 - PAD;
        double mana = energy.maxMana() <= 0 ? 0 : energy.mana() / energy.maxMana();
        UI.bar(g, barX, y + 4, barW, 5, mana, 0xFF1A5AFF, 0xFF2AB0FF);
        int sy = y + 13;
        for (int i = 0; i < AbilityData.SLOTS_PER_PAGE; i++) {
            int sx = x + PAD + i * (SLOT + GAP);
            Ability a = BrightCoreClient.abilityIn(data, i);
            UI.fill(g, sx, sy, sx + SLOT, sy + SLOT, 0xE0081626);
            UI.rect(g, sx, sy, sx + SLOT, sy + SLOT, 1, a == null ? 0xFF1F5070 : 0xFF3FC8F0);
            if (a != null) {
                UI.abilityIcon(g, a, sx + SLOT / 2f, sy + SLOT / 2f, SLOT - 2);
                float cd = BrightCoreClient.cooldownFraction(a, data);
                if (cd > 0) {
                    UI.fill(g, sx + 1, sy + 1, sx + SLOT - 1, sy + 1 + (SLOT - 2) * cd, 0xB0050A18);
                    UI.text(g, String.valueOf(BrightCoreClient.cooldownSeconds(a, data)), sx + SLOT / 2f, sy + SLOT / 2f, 9, 0xFFFFFFFF, 1);
                } else if (!player.isCreative() && energy.mana() < a.manaCost()) {
                    UI.fill(g, sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, 0x66E04050);
                }
                if (a.isToggle() && data.isToggled(a.id())) UI.fill(g, sx + 2, sy + SLOT - 3, sx + SLOT - 2, sy + SLOT - 1, 0xFF5FE07A);
            }
            UI.text(g, BrightKeys.label(i), sx + SLOT / 2f, sy + SLOT + 6, 7, 0xFFB5D9F4, 1);
        }
    }
}
