package com.brightwebs.brightcore.client;

import com.brightwebs.brightcore.data.AbilityData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

/**
 * Draws the ability bar. Bright Core ships a plain default; a content mod swaps in its own look with
 * {@link BrightCoreClient#setBarRenderer}. Position, data and keys stay Bright Core's job.
 */
@FunctionalInterface
public interface AbilityBarRenderer {
    void render(GuiGraphicsExtractor g, int screenWidth, int screenHeight, Player player, AbilityData data);
}
