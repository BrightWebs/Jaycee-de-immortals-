package com.brightwebs.jaycee.client;

public final class SkillMagicLayoutTest {
    private static int checks;
    public static void main(String[] args) {
        var left = SkillMagicLayout.CATEGORIES;
        var middle = SkillMagicLayout.LIST;
        var right = SkillMagicLayout.DETAILS;
        for (var r : new SkillMagicLayout.Rect[]{left, middle, right}) {
            check(r.x() >= 14 && r.x() + r.width() <= 524, "Inside approved inner width");
            check(r.y() == 85 && r.y() + r.height() == 400, "Inside approved inner height");
            check(r.x() + r.width() < 544, "Never touch Equip Page");
            check(r.contains(r.x(), r.y()), "Inclusive leading edge");
            check(!r.contains(r.x() + r.width(), r.y()), "Exclusive trailing edge");
        }
        check(left.x() + left.width() <= middle.x(), "Left and middle separate");
        check(middle.x() + middle.width() <= right.x(), "Middle and details separate");
        var rows = SkillMagicLayout.ROWS;
        check(rows.height() == SkillMagicLayout.VISIBLE_ROWS * SkillMagicLayout.ROW_HEIGHT, "Exactly complete list rows");
        check(middle.contains(rows.x(), rows.y()) &&
            middle.contains(rows.x() + rows.width() - 1, rows.y() + rows.height() - 1), "Rows inside middle");
        check(right.contains(SkillMagicLayout.TEXT.x(), SkillMagicLayout.TEXT.y()) &&
            right.contains(SkillMagicLayout.TEXT.x() + SkillMagicLayout.TEXT.width() - 1,
                SkillMagicLayout.TEXT.y() + SkillMagicLayout.TEXT.height() - 1), "Detail scroll region inside panel");
        check(Math.abs(left.width() / 506.0 - .22) < .01, "22 percent categories");
        check(Math.abs(middle.width() / 506.0 - .42) < .01, "42 percent list");
        check(Math.abs(right.width() / 506.0 - .36) < .01, "36 percent details");
        int[][] windows = {{1920, 1080}, {1438, 874}, {1280, 720}, {854, 480}, {640, 360}};
        for (int[] window : windows) for (int guiScale = 1; guiScale <= 4; guiScale++) {
            int width = window[0] / guiScale, height = window[1] / guiScale;
            var layout = new SapphireLayout(width, height);
            for (var r : new SkillMagicLayout.Rect[]{left, middle, right, rows, SkillMagicLayout.TEXT}) {
                double centerX = r.x() + r.width() / 2.0, centerY = r.y() + r.height() / 2.0;
                double sx = layout.left + centerX * layout.scale, sy = layout.top + centerY * layout.scale;
                check(r.contains(layout.localX(sx), layout.localY(sy)), "Transformed pointer stays inside widget");
                check(layout.screenX(r.x()) >= 0 && layout.screenX(r.x() + r.width()) <= width,
                    "Scaled region stays inside viewport horizontally");
                check(layout.screenY(r.y()) >= 0 && layout.screenY(r.y() + r.height()) <= height,
                    "Scaled region stays inside viewport vertically");
            }
        }
        System.out.println("SkillMagicLayoutTest: " + checks + " checks passed");
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
