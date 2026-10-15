package com.brightwebs.jaycee.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerModelPart;

/** Draws the real skin and respects live Skin Customization model-part flags. */
public final class PlayerPortrait {
    private PlayerPortrait() {}

    public static void render(GuiGraphics graphics, AbstractClientPlayer player, int x, int y, int size,
                              boolean threeDimensional) {
        if (threeDimensional) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x, y, x + size, y + size,
                    Math.max(8, size / 2), 0.0625F, x + size / 2.0F, y + size / 2.0F, player);
        } else {
            PlayerFaceRenderer.draw(graphics, player.getSkin().texture(), x, y, size,
                    player.isModelPartShown(PlayerModelPart.HAT), false);
        }
    }
}
