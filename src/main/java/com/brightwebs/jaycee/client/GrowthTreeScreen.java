package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.gameplay.GameplayActions;
import com.brightwebs.jaycee.gameplay.PlayerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Crisp Sapphire skill tree; scrolls at smaller GUI scales with no clipped nodes. */
public final class GrowthTreeScreen extends Screen {
    private record Node(String id, String name, String detail, String symbol, int tint, String category, int cap) {}
    private static final int GOLD = 0xFFE8CE91, PALE = 0xFFE1EFFC, SKY = 0xFFACC9E5;
    private static final List<Node> NODES = List.of(
        new Node("health", "Physical Fortitude", "+2 maximum HP (one heart) per rank", "HP", 0xFFF18AA0, "PHYSICAL", 100),
        new Node("soul", "Soul Fortification", "+20 maximum Spiritual Health", "SP", 0xFFD39AFE, "SPIRITUAL", 100),
        new Node("resistance", "Spirit Guard", "+2.5% spiritual resistance, up to 75%", "DEF", 0xFF97BEFF, "SPIRITUAL", 30),
        new Node("mana", "Mana Expansion", "+15 maximum Mana", "MP", 0xFFB896F6, "ARCANE", 100),
        new Node("regeneration", "Arcane Recovery", "+0.25 Mana per second", "REG", 0xFF71D5F9, "ARCANE", 100),
        new Node("efficiency", "Arcane Efficiency", "2% less casting Mana per rank, up to 40%", "ARC", 0xFFB78CFF, "ARCANE", 20),
        new Node("strength", "Mortal Strength", "+0.5 physical attack damage", "ATK", 0xFFF8D58C, "DIVINE", 100),
        new Node("patron", "Divine Affinity", "+0.15 Mana regen; unlocks patron passive", "DIV", 0xFFEECC82, "DIVINE", 100)
    );
    private final boolean ordinary;
    private int left, top, pw, ph, scroll, visibleRows, rowHeight, rowGap, gridTop;
    private final java.util.ArrayList<Button> nodeButtons = new java.util.ArrayList<>();
    private Button evolveButton;

    public GrowthTreeScreen(boolean ordinary) {
        super(Component.literal("Divine Ascension / Skill Tree"));
        this.ordinary = ordinary;
    }
    private PlayerState state() { return PlayerState.get(Minecraft.getInstance().player); }
    @Override protected void init() {
        super.init();
        pw = Math.min(850, width - 18);
        ph = Math.min(545, height - 16);
        left = (width - pw) / 2;
        top = (height - ph) / 2;
        rowGap = 5;
        gridTop = top + 64;
        int available = ph - 117;
        rowHeight = Math.min(45, Math.max(27, (available - 7 * rowGap) / 8));
        visibleRows = Math.max(1, Math.min(8, (available + rowGap) / (rowHeight + rowGap)));
        scroll = Math.clamp(scroll, 0, Math.max(0, NODES.size() - visibleRows));
        rebuildNodeButtons();
        int bottom = top + ph - 31;
        addRenderableWidget(styledButton("Back", left + 14, bottom, 90, 22, () ->
            Minecraft.getInstance().setScreen(new JayceeScreen(ordinary))));
        evolveButton = addRenderableWidget(styledButton("Evolve", left + pw - 104, bottom, 90, 22,
            GameplayActions::evolveGrowth));
    }
    private Button styledButton(String text, int x, int y, int w, int h, Runnable action) {
        return new Button(x, y, w, h, Component.literal(text), b -> action.run(), n -> n.get()) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float dt) {
                int border = active ? isHoveredOrFocused() ? GOLD : 0xFF6EBBEA : 0xFF405B75;
                g.fill(getX(), getY(), getX()+getWidth(), getY()+getHeight(), border);
                g.fill(getX()+2, getY()+2, getX()+getWidth()-2, getY()+getHeight()-2,
                    active ? isHoveredOrFocused() ? 0xFF20476C : 0xFF0A2647 : 0xFF101F31);
                g.drawCenteredString(font, getMessage(), getX()+getWidth()/2,
                    getY()+(getHeight()-8)/2, active ? GOLD : 0xFF7B93AD);
            }
        };
    }
    private void rebuildNodeButtons() {
        for (Button b : nodeButtons) removeWidget(b);
        nodeButtons.clear();
        int bw = Math.min(114, Math.max(70, pw / 5));
        for (int i = 0; i < visibleRows; i++) {
            final Node node = NODES.get(i + scroll);
            int y = gridTop + i * (rowHeight + rowGap);
            Button button = styledButton("+ Upgrade", left + pw - bw - 16,
                y + 3, bw, Math.max(19, rowHeight - 6), () -> GameplayActions.investGrowth(node.id()));
            nodeButtons.add(addRenderableWidget(button));
        }
    }
    @Override public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (NODES.size() > visibleRows && dy != 0) {
            scroll = Math.clamp(scroll + (dy > 0 ? -1 : 1), 0, NODES.size()-visibleRows);
            rebuildNodeButtons();
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float dt) {
        g.fill(0, 0, width, height, 0xEF071424);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float dt) {
        renderBackground(g, mx, my, dt);
        g.fill(left - 2, top - 2, left + pw + 2, top + ph + 2, 0xFF061526);
        g.fill(left, top, left + pw, top + ph, 0xFF09203B);
        g.renderOutline(left, top, pw, ph, 0xFF8BCBF8);
        g.renderOutline(left + 4, top + 4, pw - 8, ph - 8, 0xFF315F90);
        g.fill(left + 15, top + 12, left + pw - 15, top + 14, 0xFFBDA36D);
        g.drawCenteredString(font, "DIVINE ASCENSION / SKILL TREE", width/2, top + 20, GOLD);
        PlayerState snapshot = state();
        g.drawCenteredString(font, "Available skill points: " + snapshot.growthPoints(),
            width/2, top + 37, SKY);
        String previous = "";
        for (int i = 0; i < visibleRows; i++) {
            Node n = NODES.get(i + scroll);
            int y = gridTop + i * (rowHeight + rowGap);
            int rx = left + 13, rw = pw - 26;
            g.fill(rx, y, rx + rw, y + rowHeight, 0xFF102D4B);
            g.renderOutline(rx, y, rw, rowHeight, 0xFF467EAB);
            int square = Math.min(rowHeight - 8, 30);
            g.fill(rx + 5, y + 4, rx + square + 9, y + square + 4, n.tint());
            g.fill(rx + 7, y + 6, rx + square + 7, y + square + 2, 0xFF0A233D);
            g.drawCenteredString(font, n.symbol(), rx + square/2 + 7, y + square/2, n.tint());
            int tx = rx + square + 18;
            int cap = Math.min(100, n.cap());
            String title = n.name() + "  [" + snapshot.growthRank(n.id()) + "/" + cap + "]";
            g.drawString(font, title, tx, y + Math.max(4, rowHeight/2 - 11), PALE, false);
            if (rowHeight >= 32) {
                g.drawString(font, font.plainSubstrByWidth(n.detail(), Math.max(50, rw - (tx-rx) - 145)),
                    tx, y + rowHeight/2 + 2, SKY, false);
            }
            if (!previous.equals(n.category())) {
                g.fill(rx + 4, y, rx + rw - 4, y + 1, n.tint());
                previous = n.category();
            }
        }
        if (NODES.size() > visibleRows) {
            g.drawCenteredString(font, "Scroll to see more upgrades (" + (scroll+1) + "-"
                + (scroll+visibleRows) + "/8)", width/2, top + ph - 51, SKY);
        }
        int level = snapshot.divinity().getInt("DivineLevel");
        String rank = snapshot.divinity().getString("Rank");
        int needed = switch (rank) {
            case "BLESSED" -> 20; case "DEMIGOD" -> 45;
            case "MINOR_GOD" -> 80; case "MAJOR_GOD" -> 120;
            default -> -1;
        };
        boolean eligible = needed > 0 && level >= needed;
        evolveButton.active = eligible;
        String status = needed < 0 ? "Evolution unavailable"
            : rank.replace('_',' ') + "  |  Level " + level + " / " + needed;
        g.drawCenteredString(font, status, width/2, top + ph - 24, eligible ? GOLD : SKY);
        super.render(g, mx, my, dt);
    }
    @Override public boolean isPauseScreen() { return false; }
}
