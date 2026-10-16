package com.brightwebs.jaycee.client;

/** Coordinates inside the approved Status inner region; never includes Equip Page. */
final class SkillMagicLayout {
    record Rect(int x, int y, int width, int height) {
        boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }
    static final Rect CATEGORIES = new Rect(14, 85, 112, 315);
    static final Rect LIST = new Rect(128, 85, 212, 315);
    static final Rect DETAILS = new Rect(342, 85, 182, 315);
    static final Rect ROWS = new Rect(134, 149, 200, 216);
    static final Rect TEXT = new Rect(350, 158, 166, 155);
    static final int VISIBLE_ROWS = 6, ROW_HEIGHT = 36, DETAIL_LINES = 15;
    private SkillMagicLayout() {}
}
