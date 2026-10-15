package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.ability.AbilityCatalog;
import com.brightwebs.jaycee.ability.LoadoutClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Read-only presentation of the latest authoritative loadout snapshot. */
@EventBusSubscriber(modid = JayceeMod.ID, value = Dist.CLIENT)
public final class AbilityHud {
    private static final int SLOT_WIDTH = 34, SLOT_HEIGHT = 46, GAP = 3;
    private static final int WIDTH = SLOT_WIDTH * 4 + GAP * 3, HEIGHT = SLOT_HEIGHT + 15;

    private AbilityHud() {}

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!JayceeClient.gameplay(mc) || mc.options.hideGui) return;
        var state = LoadoutClientState.get();
        if (!state.accessible()) return;
        GuiGraphics g = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth(), screenH = mc.getWindow().getGuiScaledHeight();
        if (screenW < 32 || screenH < 100) return;
        float scale = Math.min(ClientPreferences.hudScale(),
            Math.min((screenW - 12f) / WIDTH, (screenH - 90f) / HEIGHT));
        if (scale <= 0) return;
        int width = (int) Math.ceil(WIDTH * scale), height = (int) Math.ceil(HEIGHT * scale);
        // Reserve the offhand slot too. Narrow windows place the row above vanilla status bars.
        boolean besideHotbar = screenW / 2 - 125 >= width + 12;
        int healthRows = (int) Math.ceil((mc.player.getMaxHealth() + mc.player.getAbsorptionAmount()) / 20f);
        int statusReserve = Math.max(80, 59 + (healthRows - 1) * Math.max(3, 12 - healthRows));
        int x = 6 + ClientPreferences.hudX();
        int y = screenH - height - (besideHotbar ? 6 : statusReserve) + ClientPreferences.hudY();
        x = Math.clamp(x, 2, Math.max(2, screenW - width - 2));
        y = Math.clamp(y, 2, Math.max(2, screenH - height - 2));
        g.pose().pushPose();
        try {
            g.pose().translate(x, y, 0);
            g.pose().scale(scale, scale, 1);
            text(g, mc, Component.translatable("hud.jaycee.page", state.page() + 1).getString(),
                1, 2, 55, 0xFFB9E8FF);
            String mana = Component.translatable("hud.jaycee.mana", state.mana()).getString();
            text(g, mc, mana, 59, 2, WIDTH - 60, 0xFF78BFFF);
            for (int slot = 0; slot < 4; slot++) {
                int index = state.page() * 4 + slot;
                int sx = slot * (SLOT_WIDTH + GAP), sy = 15;
                int id = state.abilities().get(index);
                var ability = AbilityCatalog.ALL.stream().filter(value -> value.id() == id).findFirst().orElse(null);
                int cooldown = state.cooldowns().get(index);
                boolean active = state.activeTicks().get(index) > 0;
                boolean disabled = !mc.player.isAlive() || mc.player.isSpectator()
                    || mc.player.isSleeping() || mc.player.isPassenger()
                    || ability == null || !state.unlocked().get(id)
                    || cooldown > 0 || state.mana() < ability.mana();
                DivineFrame.frame(g, sx, sy, SLOT_WIDTH, SLOT_HEIGHT, active);
                if (active) g.fill(sx + 3, sy + SLOT_HEIGHT - 3, sx + SLOT_WIDTH - 3, sy + SLOT_HEIGHT - 2, 0xFF87FFF0);
                if (ability != null) {
                    DivineGlyph.icon(g, sx + 5, sy + 14, 16, id, active);
                    // The catalog ID disambiguates glyphs that share a procedural silhouette.
                    text(g, mc, Integer.toString(id + 1), sx + 23, sy + 19, 10, 0xFFB9DBEF);
                    text(g, mc, ability.name(), sx + 4, sy + 35, SLOT_WIDTH - 8,
                        disabled ? 0xFF748795 : 0xFFD9F0FF);
                    if (disabled) g.fill(sx + 4, sy + 13, sx + 22, sy + 31, 0xA0071020);
                    if (cooldown > 0) {
                        int overlay = Math.clamp((int) Math.ceil(16.0 * cooldown / Math.max(1, ability.cooldownTicks())), 1, 16);
                        g.fill(sx + 5, sy + 30 - overlay, sx + 21, sy + 30, 0xB0182A48);
                        String seconds = Long.toString((cooldown + 19L) / 20L);
                        text(g, mc, seconds, sx + 5, sy + 19, 18, 0xFFFFFFFF);
                    }
                    if (ability.modes().size() > 1) {
                        text(g, mc, Integer.toString(state.modes().get(index) + 1), sx + 25, sy + 5, 6, 0xFFF3CF82);
                    }
                } else {
                    text(g, mc, "\u2014", sx + 13, sy + 20, 12, 0xFF638098);
                    text(g, mc, Component.translatable("hud.jaycee.empty").getString(),
                        sx + 4, sy + 35, SLOT_WIDTH - 8, 0xFF748795);
                }
                text(g, mc, JayceeClient.SLOTS[slot].getTranslatedKeyMessage().getString(),
                    sx + 4, sy + 5, 19, disabled ? 0xFF748795 : 0xFFB9E8FF);
            }
        } finally {
            g.pose().popPose();
        }
    }

    private static void text(GuiGraphics g, Minecraft mc, String text, int x, int y, int maxWidth, int color) {
        g.drawString(mc.font, mc.font.plainSubstrByWidth(text, maxWidth), x, y, color, false);
    }
}
