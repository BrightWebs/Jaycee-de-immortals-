package com.brightwebs.jaycee.client;

/** Exponential smoothing in seconds rather than frames; damage trails have a short hold. */
final class HudMotion {
    private double value;
    private double trail;
    private double target;
    private double hold;
    private double pulse;
    private boolean initialized;

    void update(double next, double seconds, boolean damageTrail) {
        next = Math.clamp(next, 0.0, 1.0);
        if (!initialized) {
            value = trail = target = next;
            initialized = true;
        }
        if (next < target - 0.0001) {
            hold = 0.45;
            pulse = 1.0;
        }
        target = next;
        value += (target - value) * (1.0 - Math.exp(-seconds * 14.0));
        if (target >= trail) trail = value;
        double trailSeconds = damageTrail ? Math.max(0.0, seconds - hold) : seconds;
        hold = Math.max(0.0, hold - seconds);
        trail += (value - trail) * (1.0 - Math.exp(-trailSeconds * 3.5));
        pulse = Math.max(0.0, pulse - seconds * 2.5);
    }

    double value() { return value; }
    double trail() { return trail; }
    double pulse() { return pulse; }
}
