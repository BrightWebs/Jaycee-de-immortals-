package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.gameplay.BlessingNetwork;
import com.brightwebs.jaycee.gameplay.DivineTotemService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Locale;

/** A sapphire-themed divine invitation with a suspenseful, hidden-odds coin reveal. */
public final class BlessingScreen extends Screen {
    private String chosen = "";
    private Boolean accepted;
    private int revealTicks;
    private boolean pending;
    private int panelLeft, panelTop, panelWidth, panelHeight;
    public BlessingScreen() { super(Component.literal("Divine Blessing")); }
    public static void open() { Minecraft.getInstance().setScreen(new BlessingScreen()); }
    public static void result(String god, boolean yes) {
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof BlessingScreen s && s.chosen.equals(god)) {
            s.accepted = yes;
            s.revealTicks = 0;
        } else if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(cap(god) +
                (yes ? " has blessed you. Your Divine Ascension begins." : " has declined your request.")), false);
        }
    }
    private static String cap(String id) {
        return id.isEmpty() ? id : id.substring(0, 1).toUpperCase(Locale.ROOT) + id.substring(1);
    }
    @Override protected void init() {
        super.init();
        panelWidth = Math.min(510, width - 20);
        panelHeight = Math.min(308, height - 20);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        if (pending) return;
        int cols = panelWidth >= 444 ? 4 : 2;
        int rows = (DivineTotemService.OLYMPIANS.size() + cols - 1) / cols;
        int gap = 5, cell = (panelWidth - 36 - (cols - 1) * gap) / cols;
        int buttonHeight = Math.clamp((panelHeight - 110) / rows - gap, 15, 25);
        int gridHeight = rows * (buttonHeight + gap) - gap;
        int startY = panelTop + 56 + Math.max(0, (panelHeight - 108 - gridHeight) / 2);
        int startX = panelLeft + 18;
        for (int i = 0; i < DivineTotemService.OLYMPIANS.size(); i++) {
            String id = DivineTotemService.OLYMPIANS.get(i);
            int x = startX + (i % cols) * (cell + gap);
            int y = startY + (i / cols) * (buttonHeight + gap);
            addRenderableWidget(Button.builder(Component.literal(cap(id)), button -> choose(id))
                .bounds(x, y, cell, buttonHeight).build());
        }
    }
    private void choose(String god) {
        if (pending) return;
        chosen = god;
        pending = true;
        clearWidgets();
        PacketDistributor.sendToServer(new BlessingNetwork.Choose(god));
    }
    @Override public void tick() {
        super.tick();
        if (pending) revealTicks++;
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g, mx, my, partial);
        int x = panelLeft, y = panelTop, w = panelWidth, h = panelHeight;
        int cx = width / 2;
        g.fill(x, y, x + w, y + h, 0xF0081326);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFF102847);
        g.renderOutline(x + 1, y + 1, w - 2, h - 2, 0xFF7AA5D2);
        g.fill(x + 12, y + 12, x + w - 12, y + 14, 0xFFCEAC6A);
        g.drawCenteredString(font, Component.literal("DIVINE INVITATION"), cx, y + 23, 0xFFEAD49E);
        if (!pending) {
            g.drawCenteredString(font, Component.literal("Choose the Olympian whose favor you seek"), cx, y + 39, 0xFFB7CDE8);
            g.drawCenteredString(font, Component.literal("Raids and Luck can sway the gods. No favor is guaranteed."), cx, y + h - 37, 0xFFC4D4EA);
            g.drawCenteredString(font, Component.literal("One Divine Totem is consumed when you request a blessing."), cx, y + h - 22, 0xFFE6CF9D);
        } else {
            g.drawCenteredString(font, Component.literal(cap(chosen) + " weighs your fate"), cx, y + 50, 0xFFC5DDF3);
            int coinY = y + h / 2;
            boolean spinning = accepted == null || revealTicks < 55;
            boolean gold = ((revealTicks / Math.max(2, 8 - revealTicks / 12)) & 1) == 0;
            int tone = gold ? 0xFFEDCD88 : 0xFF92BDE9;
            int diameter = Math.min(72, h / 3);
            int half = diameter / 2;
            g.fill(cx - half - 4, coinY - half - 4, cx + half + 4, coinY + half + 4, 0xFF081A31);
            g.fill(cx - half, coinY - half, cx + half, coinY + half, tone);
            g.fill(cx - half + 4, coinY - half + 4, cx + half - 4, coinY + half - 4, 0xFF173252);
            g.renderOutline(cx - half + 2, coinY - half + 2, diameter - 4, diameter - 4, 0xFFBBD0EF);
            g.drawCenteredString(font, Component.literal(spinning ? gold ? "FATE" : "LUCK"
                : accepted ? "YES" : "NO"), cx, coinY - 4, tone);
            if (!spinning) {
                g.drawCenteredString(font, Component.literal(accepted ? "BLESSING GRANTED" : "BLESSING DECLINED"),
                    cx, coinY + half + 17, accepted ? 0xFF9FE4C3 : 0xFFFFBFC6);
                g.drawCenteredString(font, Component.literal(accepted
                    ? "Divine Ascension unlocked. Press M after closing."
                    : "Win raids and earn Luck before trying again."), cx, coinY + half + 31, 0xFFD1E2F2);
            } else {
                g.drawCenteredString(font, Component.literal("The divine coin is turning..."), cx,
                    coinY + half + 20, 0xFFB1CCE8);
            }
            g.drawCenteredString(font, Component.literal("Press Esc to close"), cx, y + h - 20, 0xFF8DAAC9);
        }
        super.render(g, mx, my, partial);
    }
}
