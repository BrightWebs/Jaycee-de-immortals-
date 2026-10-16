package com.brightwebs.jaycee.client;

/** Verifies the screen's existing transform is suitable for native local-coordinate browser input. */
public final class AbilityBrowserLayoutTest {
    public static void main(String[] args) {
        for (int[] size : new int[][]{{1536, 1024}, {854, 480}, {427, 240}, {320, 180}}) {
            var layout = new SapphireLayout(size[0], size[1]);
            for (int[] point : new int[][]{{140, 109}, {297, 126}, {14, 112}, {300, 367}, {308, 176}, {524, 395}}) {
                double x = layout.left + point[0] * layout.scale;
                double y = layout.top + point[1] * layout.scale;
                if (Math.abs(layout.localX(x) - point[0]) > .00001
                        || Math.abs(layout.localY(y) - point[1]) > .00001)
                    throw new AssertionError("Pointer transform mismatch");
                if (SapphireLayout.slot(0).contains(point[0], point[1]))
                    throw new AssertionError("Browser overlaps Equip Page");
            }
            if (layout.left < 0 || layout.top < 0) throw new AssertionError("Frame leaves viewport");
        }
        System.out.println("AbilityBrowserLayoutTest: all checks passed");
    }
}
