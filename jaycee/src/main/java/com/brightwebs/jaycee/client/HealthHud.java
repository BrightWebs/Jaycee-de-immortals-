package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.data.EnergyData;
import com.brightwebs.jaycee.JayceeMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Set;

// the health HUD from the mockup (docs/designs/health_hud.png): your whole character in
// a glowing circle, HP / SHP / MP / AP bars, armor, hunger with air bubbles
// vanilla hearts, armor, hunger and air are hidden, the XP bar stays normal
// coords are design pixels from the 1672 x 941 mockup
@EventBusSubscriber(modid = JayceeMod.ID, value = Dist.CLIENT)
public final class HealthHud {
    static final float DESIGN_W = 1672, SCALE = 0.72f;
    private static final Set<Identifier> HIDDEN = Set.of(VanillaGuiLayers.PLAYER_HEALTH, VanillaGuiLayers.ARMOR_LEVEL,
            VanillaGuiLayers.FOOD_LEVEL, VanillaGuiLayers.AIR_LEVEL);
    private static final String[] ICON = {"heart", "spirit", "mana", "aura"};
    private static final int[][] BAR = {{0xFFC8162A, 0xFFFF5A6A}, {0xFFE07010, 0xFFFFD070}, {0xFF1A6AE6, 0xFF52D6FF}, {0xFFE0A800, 0xFFFFF27A}};

    private HealthHud() {}

    @SubscribeEvent
    public static void hideVanilla(RenderGuiLayerEvent.Pre event) {
        com.brightwebs.brightcore.api.util.Safe.run("HealthHud.hide", () -> {
            LocalPlayer p = Minecraft.getInstance().player;
            if (p != null && !p.isSpectator() && !p.isCreative() && HIDDEN.contains(event.getName())) event.setCanceled(true);
        });
    }

    private static void piece(GuiGraphicsExtractor g, String tex, float x, float y, float w, float h) {
        UI.tex(g, tex, x - 10, y - 10, w + 20, h + 20);
    }

    static void render(GuiGraphicsExtractor g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || p.isSpectator() || p.isCreative()) return;
        float k = Math.min(0.32f, sw / DESIGN_W * SCALE);  // follows GUI scale
        EnergyData e = EnergyAPI.get(p);
        g.pose().pushMatrix();
        g.pose().scale(k, k);
        // stat panel and bars
        piece(g, "hh_panel", 180, 42, 410, 156);
        float[][] v = {{p.getHealth(), p.getMaxHealth()}, {(float) e.spirit(), (float) e.maxSpirit()},
                {(float) e.mana(), (float) e.maxMana()}, {(float) e.aura(), (float) e.maxAura()}};
        for (int i = 0; i < 4; i++) {
            float y = 67 + i * 37f;
            UI.tex(g, "appr_i_" + ICON[i], 222, y - 12, 24, 24);
            piece(g, "hh_bar", 256, y - 9, 196, 18);
            float frac = v[i][1] <= 0 ? 0 : Math.min(1, v[i][0] / v[i][1]);
            if (frac > 0) {
                UI.fill(g, 259, y - 6, 259 + 190 * frac, y + 6, BAR[i][0]);
                UI.fill(g, 259, y - 6, 259 + 190 * frac, y - 2, BAR[i][1]);
            }
            String val = EnergyAPI.format(v[i][0]) + " / " + EnergyAPI.format(v[i][1]);
            UI.text(g, val, 466, y, UI.fitSize(val, 19, 11, 116, true), 0xFFFFFFFF, 0);
        }
        // armor | hunger + air
        piece(g, "hh_pill_l", 30, 215, 142, 44);
        UI.tex(g, "appr_i_armor", 74, 225, 26, 26);
        UI.text(g, String.valueOf(p.getArmorValue()), 108, 238, 22, 0xFFFFFFFF, 0);
        piece(g, "hh_pill_r", 176, 215, 412, 44);
        UI.tex(g, "hh_i_food", 198, 222, 32, 32);
        UI.text(g, p.getFoodData().getFoodLevel() + " / 20", 240, 238, 22, 0xFFFFFFFF, 0);
        UI.fill(g, 340, 224, 342, 252, 0x804F7FA8);
        UI.tex(g, "hh_i_air", 357, 220, 34, 34);
        int max = Math.max(1, p.getMaxAirSupply()), bubbles = 6;
        float air = Math.max(0, p.getAirSupply()) / (float) max * bubbles;
        for (int i = 0; i < bubbles; i++) {
            float cx = 410 + i * 26, cy = 237;
            UI.rect(g, cx - 9, cy - 9, cx + 9, cy + 9, 2, 0xFF7FB8D8);
            if (i < Math.round(air)) UI.fill(g, cx - 6, cy - 6, cx + 6, cy + 6, 0xFF3FC8FF);
        }
        // portrait, the whole character live
        piece(g, "hh_portrait", 18, 20, 190, 190);
        g.pose().popMatrix();
        int x1 = Math.round(48 * k), y1 = Math.round(34 * k), x2 = Math.round(178 * k), y2 = Math.round(198 * k);
        InventoryScreen.extractEntityInInventoryFollowsMouse(g, x1, y1, x2, y2, Math.max(6, Math.round(78 * k)), 0.0625f,
                (x1 + x2) / 2f, (y1 + y2) / 2f - 20, p);
    }

    // old neon XP bar with the level in a hexagon (not used, the vanilla bar is back)
    private static void xpBar(GuiGraphicsExtractor g, int sw, int sh, LocalPlayer p) {
        float k = Math.min(0.32f, sw / DESIGN_W * SCALE);  // follows GUI scale
        float w = 640, cx = sw / k / 2, y = sh / k - 112 / SCALE * 0.9f;
        g.pose().pushMatrix();
        g.pose().scale(k, k);
        float x = cx - w / 2;
        piece(g, "hh_xp", x, y, w, 26);
        float prog = Math.max(0, Math.min(1, p.experienceProgress));
        if (prog > 0) {
            UI.fill(g, x + 12, y + 8, x + 12 + (w - 24) * prog, y + 18, 0xFF2EC83A);
            UI.fill(g, x + 12, y + 8, x + 12 + (w - 24) * prog, y + 11, 0xFF9CFF7A);
        }
        piece(g, "hh_hex", cx - 35, y - 7, 70, 40);
        UI.text(g, String.valueOf(p.experienceLevel), cx, y + 13, 26, 0xFF7CFF5A, 1);
        g.pose().popMatrix();
    }
}
