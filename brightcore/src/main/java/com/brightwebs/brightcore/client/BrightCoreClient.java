package com.brightwebs.brightcore.client;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.data.AbilityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Registers the ability bar as a HUD layer and holds the renderer that draws it. */
@EventBusSubscriber(modid = BrightCore.ID, value = Dist.CLIENT)
public final class BrightCoreClient {
    private static AbilityBarRenderer barRenderer = BrightCoreClient::defaultBar;

    private BrightCoreClient() {}

    /** Content mods call this during client setup to give the bar their own look. */
    public static void setBarRenderer(AbilityBarRenderer renderer) {
        barRenderer = renderer;
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(BrightCore.id("ability_bar"), (g, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.player.isSpectator()) return; // TODO: also hide when F1 hides the HUD
            barRenderer.render(g, g.guiWidth(), g.guiHeight(), mc.player, AbilityAPI.get(mc.player));
        });
    }

    // ---- helpers for renderers -------------------------------------------------

    /** 0 = ready, 1 = just used. */
    public static float cooldownFraction(Ability ability, AbilityData data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ability.cooldownTicks() <= 0) return 0;
        long left = data.cooldownLeft(ability.id(), mc.level.getGameTime());
        return Math.min(1f, left / (float) ability.cooldownTicks());
    }

    /** Whole seconds of cooldown left, rounded up. */
    public static int cooldownSeconds(Ability ability, AbilityData data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0;
        return (int) Math.ceil(data.cooldownLeft(ability.id(), mc.level.getGameTime()) / 20.0);
    }

    public static Ability abilityIn(AbilityData data, int slot) {
        return data.slot(slot).map(AbilityRegistry::get).orElse(null);
    }

    /** A plain bar used until a content mod provides its own. Bottom-right corner. */
    private static void defaultBar(GuiGraphicsExtractor g, int sw, int sh, net.minecraft.world.entity.player.Player player, AbilityData data) {
        var font = Minecraft.getInstance().font;
        int size = 20, gap = 2, n = AbilityData.SLOTS_PER_PAGE;
        int x0 = sw - n * (size + gap) - 4, y0 = sh - size - 14;
        for (int i = 0; i < n; i++) {
            int x = x0 + i * (size + gap);
            g.fill(x, y0, x + size, y0 + size, 0xAA000000);
            Ability a = abilityIn(data, i);
            if (a != null) {
                g.centeredText(font, a.name().getString().substring(0, 1), x + size / 2, y0 + 6, 0xFFFFFFFF);
                int h = Math.round(size * cooldownFraction(a, data));
                if (h > 0) g.fill(x, y0, x + size, y0 + h, 0xAA202020);
            }
            g.centeredText(font, BrightKeys.label(i), x + size / 2, y0 + size + 2, 0xFFAAAAAA);
        }
    }
}
