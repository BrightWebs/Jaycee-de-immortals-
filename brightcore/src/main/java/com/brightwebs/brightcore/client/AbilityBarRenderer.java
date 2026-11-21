package com.brightwebs.brightcore.client;

import com.brightwebs.brightcore.data.AbilityData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

// draws the skill bar. bright core has a plain one, content mods can swap in their own with
// BrightCoreClient.setBarRenderer. position, data and keys are still handled here
@FunctionalInterface
public interface AbilityBarRenderer {
    void render(GuiGraphicsExtractor g, int screenWidth, int screenHeight, Player player, AbilityData data);
}
