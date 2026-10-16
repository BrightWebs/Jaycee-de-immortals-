package com.brightwebs.jaycee.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

final class PatronEmblem {
    static void icon(GuiGraphics g, String id, int x, int y, int size) {
        var texture = ResourceLocation.fromNamespaceAndPath("jaycee", "textures/patron/" + id + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent())
            g.blit(texture, x, y, 0, 0, size, size, size, size);
        else DivineFrame.gem(g, x + size / 2, y + size / 2, Math.max(2, size / 8), PatronTheme.find(id).accent());
    }
    static void render(GuiGraphics g, List<PatronTheme.Theme> patrons, double ticks, boolean reduced) {
        if (patrons.size() < 2) {
            String id = patrons.isEmpty() ? "unblessed" : patrons.getFirst().id();
            icon(g, id, 234, 128, 132);
            if (!reduced) {
                var theme = PatronTheme.palette(patrons);
                int radius = 62 + (int)Math.round(Math.sin(ticks / 28) * 2);
                for (int i = 0; i < 4; i++)
                    DivineFrame.gem(g, 300 + (i % 2 == 0 ? (i == 0 ? radius : -radius) : 0),
                        194 + (i % 2 == 1 ? (i == 1 ? radius : -radius) : 0), 1, theme.secondary());
            }
            return;
        }
        icon(g, "unblessed", 279, 173, 42);
        // Two rings keep all 22 supported lineage symbols distinct and within the original emblem bounds.
        int outer = Math.min(14, patrons.size());
        for (int i = 0; i < patrons.size(); i++) {
            boolean inner = i >= outer;
            int count = inner ? patrons.size() - outer : outer, index = inner ? i - outer : i;
            int size = patrons.size() <= 4 ? 32 : patrons.size() <= 8 ? 25 : 20;
            double a = PatronTheme.angle(index, count, ticks, reduced);
            int radius = inner ? 32 : 62;
            int x = 300 + (int)Math.round(Math.cos(a) * radius) - size / 2;
            int y = 194 + (int)Math.round(Math.sin(a) * radius) - size / 2;
            icon(g, patrons.get(i).id(), x, y, size);
        }
    }
    private PatronEmblem() {}
}
