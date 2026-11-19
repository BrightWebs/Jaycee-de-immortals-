package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.jaycee.network.JayceeNetwork;
import com.brightwebs.jaycee.network.JayceeNetwork.AppraisalInfo;
import com.brightwebs.jaycee.status.StatusData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Analytical Appraisal's panel on the right, drawn to the approved mock-up. It stays up while the
 * scan is switched on and reads whatever creature you look at; switching the mode off hides it.
 * Coordinates are design pixels of the 1672 x 941 mock-up.
 */
final class AppraisalPanel {
    private static final float DESIGN_W = 1672, X = 1165, Y = 110, W = 480, H = 552;
    private static final float[] ROWS = {227, 261, 295, 329};
    private static final int[][] BAR = {{0xFFB81A2A, 0xFFFF5A6A}, {0xFFE07010, 0xFFFFD070}, {0xFF1A5AE0, 0xFF46CCFF}, {0xFFE0A000, 0xFFFFF070}};

    private AppraisalPanel() {}

    static void render(GuiGraphicsExtractor g, int sw, int sh) {
        AppraisalInfo info = JayceeNetwork.lastAppraisal;
        if (info.entity() == -1) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        boolean stale = info.entity() >= 0 && System.currentTimeMillis() - JayceeNetwork.lastAppraisalAt > 1500;
        Entity e = info.entity() >= 0 && !stale ? mc.level.getEntity(info.entity()) : null;
        float k = sw / DESIGN_W;

        g.pose().pushMatrix();
        g.pose().scale(k, k);
        g.pose().translate(X, Y);
        UI.tex(g, "appr_panel", 0, 0, W, H);
        if (!(e instanceof LivingEntity)) {
            UI.semi(g, Component.translatable("jaycee.appraisal.idle"), 160, 85, 18, 0xFF9FB8C8, 0);
            g.pose().popMatrix();
            return;
        }
        UI.text(g, info.name(), 160, 85, UI.fitSize(info.name(), 26, 14, 178, true), 0xFFFFFFFF, 0);
        UI.semi(g, UI.fit(info.race(), 18, 116, false), 223, 129, 18, 0xFFFFFFFF, 0);
        UI.text(g, StatusData.rank(info.ep()), 418, 128, 26, 0xFFC86BFF, 1);
        UI.text(g, UI.num(info.ep()), 254, 182, 24, 0xFFFFFFFF, 0);
        float[][] v = {{info.hp(), info.maxHp()}, {info.spirit(), info.maxSpirit()}, {info.mana(), info.maxMana()}, {info.aura(), info.maxAura()}};
        for (int i = 0; i < 4; i++) {
            float frac = v[i][1] <= 0 ? 0 : Math.min(1, v[i][0] / v[i][1]);
            float y = ROWS[i];
            UI.fill(g, 133, y - 4, 352, y + 4, 0xFF161A26);
            if (frac > 0) {
                float x2 = 133 + 219 * frac;
                UI.fill(g, 133, y - 4, x2, y + 4, BAR[i][0]);
                UI.fill(g, 133, y - 4, x2, y - 1, BAR[i][1]);
            }
            String val = EnergyAPI.format(v[i][0]) + " / " + EnergyAPI.format(v[i][1]);
            UI.semi(g, val, 436, y, UI.fitSize(val, 15, 10, 82, false), 0xFFFFFFFF, 2);
        }
        UI.semi(g, String.valueOf(info.armor()), 157, 367, 18, 0xFFFFFFFF, 0);
        skills(g, info);
        g.pose().popMatrix();

        // the creature itself, live in the portrait box
        LivingEntity le = (LivingEntity) e;
        int x1 = Math.round((X + 42) * k), y1 = Math.round((Y + 76) * k), x2 = Math.round((X + 128) * k), y2 = Math.round((Y + 166) * k);
        float tall = Math.max(le.getBbHeight(), le.getBbWidth());
        int size = Math.max(4, Math.round((y2 - y1) * 0.8f / Math.max(0.5f, tall)));
        InventoryScreen.extractEntityInInventoryFollowsMouse(g, x1, y1, x2, y2, size, 0.0625f, (x1 + x2) / 2f, (y1 + y2) / 2f, le);
    }

    private static void skills(GuiGraphicsExtractor g, AppraisalInfo info) {
        if (info.skills().isEmpty()) {
            UI.semi(g, Component.translatable("jaycee.appraisal.no_skills"), 240, 476, 16, 0xFF9FB8C8, 1);
            return;
        }
        int shown = Math.min(2, info.skills().size());
        for (int i = 0; i < shown; i++) {
            float y = 456 + i * 44;
            Ability a = AbilityRegistry.get(Identifier.tryParse(info.skills().get(i)));
            String name = a == null ? info.skills().get(i) : a.name().getString();
            if (a != null) UI.abilityIcon(g, a, 95, y, 34);
            String label = i == shown - 1 && info.skills().size() > shown ? name + "  +" + (info.skills().size() - shown) : name;
            UI.semi(g, UI.fit(label, 18, 290, false), 130, y, 18, 0xFFFFFFFF, 0);
            if (i < shown - 1) UI.fill(g, 60, y + 22, 420, y + 23, 0x403FC8F0);
        }
    }
}
