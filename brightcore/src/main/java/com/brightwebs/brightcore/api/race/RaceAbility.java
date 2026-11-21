package com.brightwebs.brightcore.api.race;

import net.minecraft.server.level.ServerPlayer;

// a race's own ability, used with the race key (R), same idea as tensura
// register one per race id with RaceAbilities.register. runs on the server
public interface RaceAbility {
    // key went down
    void press(ServerPlayer player);

    // every tick while it's held. return false to stop
    default boolean held(ServerPlayer player, int heldTicks) { return false; }

    // key let go after heldTicks
    default void release(ServerPlayer player, int heldTicks) {}

    // every tick while you're this race (stuff like gliding)
    default void tick(ServerPlayer player) {}
}
