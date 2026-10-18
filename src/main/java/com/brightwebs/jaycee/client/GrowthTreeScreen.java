package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.gameplay.GameplayActions;
import com.brightwebs.jaycee.gameplay.PlayerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Spend earned level points from the same saved pool used by /jc growth. */
public final class GrowthTreeScreen extends Screen {
    private record Node(String id, String title, String detail) {}
    private static final List<Node> NODES = List.of(
        new Node("health", "Physical Fortitude", "+2 maximum HP (one heart) per rank"),
        new Node("soul", "Soul Fortification", "+20 maximum Spiritual Health"),
        new Node("mana", "Mana Expansion", "+15 maximum Mana"),
        new Node("regeneration", "Arcane Recovery", "+0.25 Mana regenerated each second"),
        new Node("resistance", "Spirit Guard", "+2.5% spiritual resistance, up to 75%"),
        new Node("strength", "Mortal Strength", "+0.5 physical attack damage"),
        new Node("patron", "Divine Affinity", "+0.15 Mana regeneration; requires Olympian patron")
    );
    private final boolean ordinary;
    private int left, top, panelWidth, panelHeight;
    public GrowthTreeScreen(boolean ordinary) {
        super(Component.literal("Divine Skill Tree"));
        this.ordinary = ordinary;
    }
    private PlayerState state() { return PlayerState.get(Minecraft.getInstance().player); }
    @Override protected void init() {
        super.init();
        panelWidth = Math.min(580, width - 20);
        panelHeight = Math.min(360, height - 20);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        int gap = 5;
        int rowH = Math.max(19, Math.min(35, (panelHeight - 92 - gap * 6) / 7));
        int start = top + 51;
        int buttonWidth = Math.min(98, panelWidth / 4);
        for (int i = 0; i < NODES.size(); i++) {
            Node node = NODES.get(i);
            int yy = start + i * (rowH + gap);
            addRenderableWidget(Button.builder(Component.literal("+ Upgrade"), b -> GameplayActions.investGrowth(node.id()))
                .bounds(left + panelWidth - buttonWidth - 22, yy + 2, buttonWidth, Math.max(16, rowH - 4)).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Evolve"), b -> GameplayActions.evolveGrowth())
            .bounds(left + panelWidth - 97, top + panelHeight - 27, 78, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b ->
            Minecraft.getInstance().setScreen(new JayceeScreen(ordinary)))
            .bounds(left + 12, top + panelHeight - 27, 70, 20).build());
    }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float dt) {
        g.fill(0, 0, width, height, 0xE8091326);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float dt) {
        renderBackground(g, mx, my, dt);
        g.fill(left, top, left + panelWidth, top + panelHeight, 0xFF0D243C);
        g.renderOutline(left, top, panelWidth, panelHeight, 0xFFC3A66E);
        g.renderOutline(left + 3, top + 3, panelWidth - 6, panelHeight - 6, 0xFF5582AF);
        g.fill(left + 16, top + 13, left + panelWidth - 16, top + 15, 0xFFE3CA92);
        g.drawCenteredString(font, "DIVINE ASCENSION / SKILL TREE", width / 2, top + 22, 0xFFE9D49F);
        var snapshot = state();
        int points = snapshot.growthPoints();
        g.drawCenteredString(font, "Available skill points: " + points, width / 2, top + 37, 0xFFABD6ED);
        int gap = 5;
        int rowH = Math.max(19, Math.min(35, (panelHeight - 92 - gap * 6) / 7));
        int start = top + 51;
        for (int i = 0; i < NODES.size(); i++) {
            Node node = NODES.get(i);
            int yy = start + i * (rowH + gap);
            g.fill(left + 12, yy, left + panelWidth - 13, yy + rowH, 0xFF183554);
            g.renderOutline(left + 12, yy, panelWidth - 25, rowH, 0xFF476F92);
            int rank = snapshot.growthRank(node.id());
            g.drawString(font, node.title() + "  [" + rank + "/100]", left + 21, yy + 4, 0xFFEAD29E, false);
            if (rowH >= 27) g.drawString(font, node.detail(), left + 21, yy + 16, 0xFFA8C5DC, false);
        }
        int level = snapshot.divinity().getInt("DivineLevel");
        String rank = snapshot.divinity().getString("Rank");
        int target = switch (rank) {
            case "BLESSED" -> 20; case "DEMIGOD" -> 45;
            case "MINOR_GOD" -> 80; case "MAJOR_GOD" -> 120;
            default -> -1;
        };
        String gate = target > 0 ? "Next evolution: " + rank.replace('_', ' ') + " | Lv " + level + "/" + target
            : "Evolution: final rank or unavailable";
        g.drawCenteredString(font, gate, width / 2, top + panelHeight - 17, 0xFFABC3D9);
        super.render(g, mx, my, dt);
    }
    @Override public boolean isPauseScreen() { return false; }
}
