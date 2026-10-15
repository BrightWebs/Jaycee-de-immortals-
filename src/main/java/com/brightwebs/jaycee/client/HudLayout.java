package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.config.HudConfig.Anchor;

/** Anchors are applied before offsets; the final rectangle stays inside the viewport. */
final class HudLayout {
    record Position(int x, int y) {}

    static Position place(Anchor anchor, int offsetX, int offsetY, int width, int height,
                          int viewportWidth, int viewportHeight) {
        int x = switch (anchor) {
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> viewportWidth - width;
            default -> 0;
        };
        int y = switch (anchor) {
            case MIDDLE_LEFT, MIDDLE_RIGHT -> (viewportHeight - height) / 2;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> viewportHeight - height;
            default -> 0;
        };
        return new Position(Math.clamp(x + offsetX, 0, Math.max(0, viewportWidth - width)),
                Math.clamp(y + offsetY, 0, Math.max(0, viewportHeight - height)));
    }

    private HudLayout() {}
}
