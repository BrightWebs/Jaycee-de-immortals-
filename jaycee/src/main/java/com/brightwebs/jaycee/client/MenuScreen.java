package com.brightwebs.jaycee.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * The Existence System (M). One fixed frame: the artwork is 1536 x 1024 design pixels with the
 * side bar just outside its right edge, scaled to sit comfortably on screen with the game still
 * visible around it. The portrait, tabs and side bar never move; only the content changes.
 * Esc steps back one page (sub-pages return to where you came from, tabs return to Status),
 * and closes the menu from Status.
 */
public class MenuScreen extends Screen {
    static final int W = 1630, H = 1024;
    static final int[][] TABS = {{116, 128, 454, 200}, {466, 128, 780, 200}, {794, 128, 1118, 200}, {1132, 128, 1452, 200}};
    static final int SIDE_X = 1504, SIDE_Y = 286, SIDE_STEP = 108;

    enum Id { STATUS, STATS, SKILLS, MAGIC, RACE, TITLES, BONDS }

    private static Id remembered = Id.STATUS;
    private Id page, back = Id.STATUS;
    private final java.util.EnumMap<Id, Page> pages = new java.util.EnumMap<>(Id.class);
    private float k;
    private int left, top;

    public MenuScreen() {
        super(Component.translatable("jaycee.menu.title"));
        pages.put(Id.STATUS, new StatusPage(this));
        pages.put(Id.STATS, new StatsPage(this));
        pages.put(Id.SKILLS, new LibraryPage(this, false));
        pages.put(Id.MAGIC, new LibraryPage(this, true));
        pages.put(Id.RACE, new RacePage(this));
        pages.put(Id.TITLES, new TitlesPage(this));
        pages.put(Id.BONDS, new BondsPage(this));
        page = remembered == Id.RACE || remembered == Id.TITLES || remembered == Id.BONDS ? Id.STATUS : remembered;
    }

    LocalPlayer player() { return Minecraft.getInstance().player; }

    void open(Id id) {
        if (id == Id.BONDS || id == Id.RACE || id == Id.TITLES) back = page;
        page = id;
        if (id.ordinal() <= Id.MAGIC.ordinal()) remembered = id;
    }

    Id current() { return page; }

    @Override
    protected void init() {
        // Fit the frame to ~88% of the screen height (and never wider than 94% of the width).
        k = Math.min(width * 0.74f / W, height * 0.70f / H); // about 3/4 of the old size
        left = Math.round((width - W * k) / 2);
        top = Math.round((height - H * k) / 2);
    }

    /** Screen -> frame design pixels. */
    double fx(double sx) { return (sx - left) / k; }
    double fy(double sy) { return (sy - top) / k; }
    int sx(double fx) { return Math.round(left + (float) fx * k); }
    int sy(double fy) { return Math.round(top + (float) fy * k); }

    /** No blur or dimming: the game stays visible behind and around the menu. */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float dt) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        com.brightwebs.brightcore.api.util.Safe.run("MenuScreen.extractRenderState", () -> {
            LocalPlayer p = player();
            if (p == null) return;
            Page pg = pages.get(page);
            double fx = fx(mx), fy = fy(my);
    
            g.pose().pushMatrix();
            g.pose().translate(left, top);
            g.pose().scale(k, k);
            UI.tex(g, pg.background(), 0, 0, 1536, 1024);
            drawSideBar(g, fx, fy);
            g.pose().popMatrix();
    
            if (pg.showsPlayer()) {
                // Your own skin, armour and look, turning to follow the cursor.
                InventoryScreen.extractEntityInInventoryFollowsMouse(g, sx(140), sy(270), sx(420), sy(752),
                        Math.max(8, Math.round(205 * k)), 0.0625f, mx, my, p);
            }
    
            g.pose().pushMatrix();
            g.pose().translate(left, top);
            g.pose().scale(k, k);
            pg.draw(g, fx, fy);
            drawSideTooltip(g, fx, fy);
            g.pose().popMatrix();
            super.extractRenderState(g, mx, my, dt);
        });
    }

    private static final String[] SIDE_TIPS = {"jaycee.ui.side.bonds", "jaycee.ui.side.editor", "jaycee.ui.side.locked", "jaycee.ui.side.locked"};

    private void drawSideBar(GuiGraphicsExtractor g, double fx, double fy) {
        for (int i = 0; i < 4; i++) {
            float y = SIDE_Y + i * SIDE_STEP;
            boolean on = i == 0 && page == Id.BONDS;
            boolean hover = Page.in(fx, fy, SIDE_X, y, SIDE_X + 118, y + 104);
            if (on) UI.tex(g, "side_on", SIDE_X - 3, y - 3);
            else UI.tex(g, i < 2 ? "side_cyan" : "side_off", SIDE_X, y, 118, i < 2 ? 104 : 106, hover ? 0xFFFFFFFF : 0xFFE6F0F8);
            String icon = i == 0 ? "icon_bonds" : i == 1 ? "icon_editor" : "icon_lock";
            int[] s = UI.size(icon);
            UI.tex(g, icon, SIDE_X + 59 - s[0] / 2f, y + 52 - s[1] / 2f);
        }
    }

    private void drawSideTooltip(GuiGraphicsExtractor g, double fx, double fy) {
        for (int i = 0; i < 4; i++) {
            float y = SIDE_Y + i * SIDE_STEP;
            if (!Page.in(fx, fy, SIDE_X, y, SIDE_X + 118, y + 104)) continue;
            Component tip = Component.translatable(SIDE_TIPS[i]);
            float w = UI.width(tip, 18, false) + 28;
            float x2 = SIDE_X + 118, x1 = x2 - w, ty = y - 44;
            UI.fill(g, x1, ty, x2, ty + 36, 0xF00A1424);
            UI.rect(g, x1, ty, x2, ty + 36, 2, 0xFF78F0FF);
            UI.semi(g, tip, x1 + w / 2, ty + 18, 18, 0xFFE6FAFF, 1);
        }
    }

    // ---- input --------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return com.brightwebs.brightcore.api.util.Safe.call("MenuScreen.mouseClicked", () -> {
            double x = fx(event.x()), y = fy(event.y());
            int b = UI.button(event);
            Page pg = pages.get(page);
            if (b == 0 && page != Id.BONDS && page != Id.RACE && page != Id.TITLES) {
                for (int i = 0; i < TABS.length; i++) {
                    if (Page.in(x, y, TABS[i][0], TABS[i][1], TABS[i][2], TABS[i][3])) { open(Id.values()[i]); click(); return true; }
                }
            }
            for (int i = 0; i < 4; i++) {
                float sy = SIDE_Y + i * SIDE_STEP;
                if (b == 0 && Page.in(x, y, SIDE_X, sy, SIDE_X + 118, sy + 104)) {
                    if (i == 0) open(page == Id.BONDS ? back : Id.BONDS);
                    else if (i == 1 && player() != null) player().sendOverlayMessage(Component.translatable("jaycee.ui.editor_soon"));
                    click();
                    return true;
                }
            }
            if (pg.click(x, y, b)) { click(); return true; }
            return super.mouseClicked(event, doubleClick);
        }, false);
    }

    static void click() {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.35f));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        return com.brightwebs.brightcore.api.util.Safe.call("MenuScreen.mouseScrolled", () -> {
            if (dy != 0 && pages.get(page).scroll(fx(mx), fy(my), dy)) return true;
            return super.mouseScrolled(mx, my, dx, dy);
        }, false);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return com.brightwebs.brightcore.api.util.Safe.call("MenuScreen.charTyped", () -> {
            if (pages.get(page).typed(event.codepoint())) return true;
            return super.charTyped(event);
        }, false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return com.brightwebs.brightcore.api.util.Safe.call("MenuScreen.keyPressed", () -> {
            if (event.key() != 256 && pages.get(page).key(event.key())) return true;
            if (event.key() != 256 && JayceeClient.MENU.matches(event)) { page = Id.STATUS; onClose(); return true; }
            return super.keyPressed(event);
        }, false);
    }

    /** Esc: step back toward Status; close from Status. */
    @Override
    public void onClose() {
        switch (page) {
            case STATUS -> super.onClose();
            case BONDS -> page = back;
            default -> page = Id.STATUS;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
