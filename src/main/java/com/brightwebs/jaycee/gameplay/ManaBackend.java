package com.brightwebs.jaycee.gameplay;

/** A server-player-bound adapter. consume must be atomic and reject invalid amounts. */
public interface ManaBackend {
    double getCurrent();
    double getMax();
    double getRegen();
    boolean consume(double amount);
}
