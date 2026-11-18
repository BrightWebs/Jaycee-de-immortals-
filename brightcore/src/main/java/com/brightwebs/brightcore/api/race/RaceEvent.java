package com.brightwebs.brightcore.api.race;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Race events on NeoForge.EVENT_BUS. */
public abstract class RaceEvent extends Event {
    private final ServerPlayer player;

    protected RaceEvent(ServerPlayer player) { this.player = player; }

    public ServerPlayer getPlayer() { return player; }

    /** Before an evolution. Cancel to block it (for example, a god refusing the ascension). */
    public static final class Evolve extends RaceEvent implements ICancellableEvent {
        private final Race from, to;

        public Evolve(ServerPlayer player, Race from, Race to) {
            super(player);
            this.from = from;
            this.to = to;
        }

        public Race getFrom() { return from; }
        public Race getTo() { return to; }
    }
}
