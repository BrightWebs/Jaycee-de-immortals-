package com.brightwebs.brightcore.api.race;

import net.minecraft.server.level.ServerPlayer;

/**
 * A race's own ability, used with the Race Ability key (R by default), like Tensura: Reincarnated.
 * Register one per race id with {@link RaceAbilities#register}. Runs on the server.
 */
public interface RaceAbility {
    /** The key went down. */
    void press(ServerPlayer player);

    /** Every tick while the key is held (heldTicks from 0). Return false to stop holding. */
    default boolean held(ServerPlayer player, int heldTicks) { return false; }

    /** The key was let go after heldTicks. */
    default void release(ServerPlayer player, int heldTicks) {}

    /** Every tick while the player is this race (upkeep such as gliding). */
    default void tick(ServerPlayer player) {}
}
