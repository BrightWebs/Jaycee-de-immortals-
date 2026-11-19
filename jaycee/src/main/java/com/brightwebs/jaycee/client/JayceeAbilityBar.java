package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.client.BrightCoreClient;
import com.brightwebs.brightcore.client.BrightKeys;
import com.brightwebs.brightcore.data.AbilityData;
import com.brightwebs.brightcore.data.EnergyData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * The skill HUD on the left of the screen, drawn to the approved mock-ups.
 * Resting: each key (Z X C V) in its octagon with the skill's icon beside it.
 * Holding Switch Mode (Left Alt): the slots slide out into cards with the skill's name and mode,
 * and the preset switcher appears underneath.
 *
 * Coordinates are design pixels of the 1672 x 941 mock-up, scaled to the window.
 */
final class JayceeAbilityBar {
    private static final float DESIGN_W = 1672, PITCH = 76, TOP_FRACTION = 298f / 941f;
    private static float open; // 0 resting .. 1 expanded
    private static long lastNanos;

    private JayceeAbilityBar() {}

    /** Draws a sprite made with a 10 px glow margin around its shape. */
    private static void piece(GuiGraphicsExtractor g, String tex, float x, float y, float w, float h, float alpha) {
        int a = Math.round(Math.max(0, Math.min(1, alpha)) * 255);
        UI.tex(g, tex, x - 10, y - 10, w + 20, h + 20, (a << 24) | 0xFFFFFF);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    static void render(GuiGraphicsExtractor g, int sw, int sh, Player player, AbilityData data) {
        com.brightwebs.brightcore.api.util.Safe.run("HealthHud.render", () -> HealthHud.render(g, sw, sh));
        com.brightwebs.brightcore.api.util.Safe.run("AppraisalPanel.render", () -> AppraisalPanel.render(g, sw, sh));
        com.brightwebs.brightcore.api.util.Safe.run("JayceeAbilityBar.render", () -> {
            long now = System.nanoTime();
            float dt = lastNanos == 0 ? 0 : Math.min(0.1f, (now - lastNanos) / 1e9f);
            lastNanos = now;
            float target = BrightKeys.MODE_SWITCH.isDown() ? 1 : 0;
            open += (target - open) * Math.min(1, dt * 14);
            if (Math.abs(target - open) < 0.01f) open = target;
            float t = open * open * (3 - 2 * open); // smoothstep

            float k = sw / DESIGN_W * 0.72f; // smaller skill bar
            float top = (sh / k) * TOP_FRACTION;
            EnergyData energy = EnergyAPI.get(player);

            g.pose().pushMatrix();
            g.pose().scale(k, k);
            for (int i = 0; i < AbilityData.SLOTS_PER_PAGE; i++) {
                float y = top + i * PITCH;
                float badgeX = lerp(33, 57, t), iconX = lerp(57, 80, t);
                Ability a = BrightCoreClient.abilityIn(data, i);
                if (t > 0.01f) card(g, a, player, data, iconX, y, t);
                slot(g, a, player, data, energy, iconX, y);
                piece(g, "hud_key", badgeX - 23, y + 6, 46, 46, 1);
                UI.text(g, BrightKeys.label(i), badgeX, y + 29, UI.fitSize(BrightKeys.label(i), 22, 10, 34, true), 0xFFFFFFFF, 1);
            }
            if (t > 0.01f) preset(g, data, top + AbilityData.SLOTS_PER_PAGE * PITCH + 8, t);
            g.pose().popMatrix();
        });
    }

    /** The icon tile: cooldown shade, red when unaffordable, green line while toggled on. */
    private static void slot(GuiGraphicsExtractor g, Ability a, Player player, AbilityData data, EnergyData energy, float x, float y) {
        piece(g, a == null ? "hud_frame_empty" : "hud_frame", x, y - 4, 70, 66, 1);
        if (a == null) return;
        UI.abilityIcon(g, a, x + 35, y + 29, 54);
        float cd = BrightCoreClient.cooldownFraction(a, data);
        if (cd > 0) {
            UI.fill(g, x + 3, y - 1, x + 67, y - 1 + 60 * cd, 0xB0050A18);
            UI.text(g, String.valueOf(BrightCoreClient.cooldownSeconds(a, data)), x + 35, y + 29, 20, 0xFFFFFFFF, 1);
        } else if (!player.isCreative() && energy.mana() < a.mode(AbilityAPI.modeOf(player, a)).manaCost()) {
            UI.fill(g, x + 3, y - 1, x + 67, y + 59, 0x55E04050);
        }
        if (a.isToggle() && data.isToggled(a.id())) UI.fill(g, x + 8, y + 57, x + 62, y + 59, 0xFF5FE07A);
    }

    /** The name card that slides out while Switch Mode is held. */
    private static void card(GuiGraphicsExtractor g, Ability a, Player player, AbilityData data, float x, float y, float t) {
        piece(g, "hud_card", x, y - 2, 250, 62, t);
        if (t < 0.12f) return; // text colours with almost no alpha would draw fully opaque
        int alpha = Math.round(t * 255) << 24;
        float tx = x + 86;
        if (a == null) {
            UI.semi(g, Component.translatable("jaycee.hud.empty"), tx, y + 29, 17, alpha | 0x9FB8C8, 0);
            return;
        }
        String name = a.name().getString();
        UI.text(g, name, tx, y + 18, UI.fitSize(name, 20, 12, 150, true), alpha | 0xFFFFFF, 0);
        UI.semi(g, Component.translatable("jaycee.hud.mode", modeLabel(a, player, data)), tx, y + 42, 13, alpha | 0xEBF0FF, 0);
    }

    private static Component modeLabel(Ability a, Player player, AbilityData data) {
        if (a.modes().size() > 1) return a.modeName(AbilityAPI.modeOf(player, a));
        if (a.isToggle()) return Component.translatable(data.isToggled(a.id()) ? "jaycee.hud.on" : "jaycee.hud.off");
        if (a.isHeld()) return Component.translatable("jaycee.hud.hold");
        return Component.translatable("jaycee.hud.default");
    }

    /** "Preset 1 / 9" with its arrows. */
    private static void preset(GuiGraphicsExtractor g, AbilityData data, float y, float t) {
        int alpha = Math.round(t * 255) << 24;
        piece(g, "hud_preset", 45, y, 212, 34, t);
        piece(g, "hud_chev_l", 58, y + 7, 13, 20, t);
        piece(g, "hud_chev_r", 228, y + 7, 13, 20, t);
        if (t < 0.12f) return;
        UI.semi(g, Component.translatable("jaycee.hud.preset", data.page() + 1, AbilityData.PAGES), 151, y + 17, 18, alpha | 0xFFFFFF, 1);
    }
}
