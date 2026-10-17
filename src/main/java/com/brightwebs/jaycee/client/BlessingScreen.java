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
        if (pending) return;
        int columns = 4, width = 104, height = 23;
        int left = this.width / 2 - 218;
        int top = this.height / 2 - 80;
        for (int i = 0; i < DivineTotemService.OLYMPIANS.size(); i++) {
            String id = DivineTotemService.OLYMPIANS.get(i);
            int x = left + (i % columns) * 110;
            int y = top + (i / columns) * 29;
            addRenderableWidget(Button.builder(Component.literal(cap(id)), button -> choose(id))
                .bounds(x, y, width, height).build());
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
        int cx = width / 2, cy = height / 2;
        g.fill(cx - 244, cy - 128, cx + 244, cy + 133, 0xEB07172D);
        g.fill(cx - 241, cy - 125, cx + 241, cy + 130, 0xFF102947);
        g.renderOutline(cx - 243, cy - 127, 486, 259, 0xFF9BB9ED);
        g.drawCenteredString(font, Component.literal("DIVINE INVITATION"), cx, cy - 111, 0xFFE1D3AA);
        if (!pending) {
            g.drawCenteredString(font, Component.literal("Select the Olympian whose favor you seek"), cx, cy - 96, 0xFFB5D2F0);
            g.drawCenteredString(font, Component.literal("Powerful Olympians are harder to impress."), cx, cy + 48, 0xFFCAD9EB);
            g.drawCenteredString(font, Component.literal("Raids and Luck Points can change your fate."), cx, cy + 63, 0xFFCAD9EB);
            g.drawCenteredString(font, Component.literal("One Divine Totem is spent when you make your request."), cx, cy + 99, 0xFFDDCBA8);
        } else {
            g.drawCenteredString(font, Component.literal("The gods are deciding..."), cx, cy - 73, 0xFFD2DDF8);
            g.drawCenteredString(font, Component.literal(cap(chosen)), cx, cy - 49, 0xFFE8CF99);
            // Purely theatrical alternating coin faces; the actual verdict was rolled server-side.
            boolean face = (revealTicks / Math.max(2, 9 - revealTicks / 10)) % 2 == 0;
            int color = face ? 0xFFFFDC88 : 0xFF95BCF2;
            g.fill(cx - 31, cy - 30, cx + 31, cy + 32, 0xFF07192B);
            g.renderOutline(cx - 30, cy - 29, 60, 60, color);
            g.drawCenteredString(font, Component.literal(accepted == null || revealTicks < 55
                ? (face ? "FATE" : "FORTUNE") : accepted ? "YES" : "NO"), cx, cy - 2, color);
            if (accepted != null && revealTicks >= 55) {
                String message = accepted ? "YOUR BLESSING HAS BEEN GRANTED" : "THE OLYMPIAN HAS DECLINED";
                g.drawCenteredString(font, Component.literal(message), cx, cy + 51,
                    accepted ? 0xFFA9EBD0 : 0xFFF0B7BD);
                g.drawCenteredString(font, Component.literal(accepted ? "250 Essence received. Press M for Divine Ascension."
                    : "Seek greater favor through raids and Luck Points."), cx, cy + 68, 0xFFCCDBEA);
                g.drawCenteredString(font, Component.literal("Press Escape to close"), cx, cy + 105, 0xFF8FA9C2);
            }
        }
        super.render(g, mx, my, partial);
    }
}
