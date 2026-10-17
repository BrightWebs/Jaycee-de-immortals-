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
    private static final int GOLD = 0xFFE5C685, BLUE = 0xFFB5D5EF;
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
    private static String domain(String id) {
        return switch (id) {
            case "zeus" -> "SKY & THUNDER"; case "poseidon" -> "OCEANS";
            case "hades" -> "UNDERWORLD"; case "athena" -> "WISDOM";
            case "ares" -> "WAR"; case "apollo" -> "SUN & PROPHECY";
            case "artemis" -> "THE HUNT"; case "hermes" -> "SPEED & TRAVEL";
            case "hephaestus" -> "FORGE & FIRE"; case "aphrodite" -> "LOVE";
            case "demeter" -> "HARVEST"; case "dionysus" -> "REVELRY";
            case "hera" -> "SOVEREIGNTY"; case "hestia" -> "HEARTH";
            default -> "OLYMPUS";
        };
    }
    @Override protected void init() {
        super.init();
        panelWidth = Math.min(554, Math.max(200, width - 20));
        panelHeight = Math.min(310, Math.max(150, height - 16));
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        if (pending) return;
        int cols = panelWidth >= 440 ? 4 : 2;
        int rows = (DivineTotemService.OLYMPIANS.size() + cols - 1) / cols;
        int gap = 7, cell = (panelWidth - 32 - (cols - 1) * gap) / cols;
        int available = panelHeight - 109;
        int cardHeight = Math.max(13, (available - (rows - 1) * gap) / rows);
        int startY = panelTop + 62;
        int startX = panelLeft + 16;
        for (int n = 0; n < DivineTotemService.OLYMPIANS.size(); n++) {
            String god = DivineTotemService.OLYMPIANS.get(n);
            int x = startX + (n % cols) * (cell + gap);
            int y = startY + (n / cols) * (cardHeight + gap);
            addRenderableWidget(new Button(x, y, cell, cardHeight, Component.literal(cap(god)),
                button -> choose(god), narration -> narration.get()) {
                @Override protected void renderWidget(GuiGraphics g, int mx, int my, float delta) {
                    boolean selected = isHoveredOrFocused();
                    int border = selected ? 0xFFF5DB9C : 0xFF537BA4;
                    g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), border);
                    g.fill(getX() + 2, getY() + 2, getX() + getWidth() - 2, getY() + getHeight() - 2,
                        selected ? 0xFF254C70 : 0xFF142F50);
                    g.fill(getX() + 6, getY() + 5, getX() + 8, getY() + getHeight() - 5,
                        selected ? GOLD : 0xFF5C8DB5);
                    int middle = getX() + getWidth() / 2;
                    if (getHeight() >= 31) {
                        g.drawCenteredString(font, getMessage(), middle, getY() + 8, selected ? GOLD : 0xFFE1EAF6);
                        g.drawCenteredString(font, Component.literal(domain(god)), middle, getY() + 21,
                            selected ? 0xFFE0D2AC : 0xFF83ABC9);
                    } else {
                        g.drawCenteredString(font, getMessage(), middle,
                            getY() + (getHeight() - 8) / 2, selected ? GOLD : 0xFFE1EAF6);
                    }
                }
            });
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
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xF0061323);
        g.fill(x, y, x + w, y + h, 0xFF0E223B);
        g.renderOutline(x, y, w, h, 0xFF7398C4);
        g.renderOutline(x + 4, y + 4, w - 8, h - 8, 0xFF244C71);
        g.fill(x + 16, y + 12, x + w - 16, y + 14, 0xFFD8B776);
        g.drawCenteredString(font, Component.literal("THE COUNCIL OF OLYMPUS"), cx, y + 22, GOLD);
        g.drawCenteredString(font, Component.literal("A divine invitation awaits your answer"), cx, y + 41, BLUE);
        if (!pending) {
            g.fill(x + 16, y + h - 39, x + w - 16, y + h - 38, 0xFF4D789E);
            g.drawCenteredString(font, Component.literal("Raids and Luck strengthen your plea. Fate is never certain."), cx, y + h - 31, BLUE);
            g.drawCenteredString(font, Component.literal("Select a god to offer one Divine Totem."), cx, y + h - 17, GOLD);
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
