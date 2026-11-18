package com.brightwebs.brightcore.data;

import com.brightwebs.brightcore.BrightCore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Data attachments Bright Core keeps on entities. */
public final class BrightAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BrightCore.ID);

    /** Abilities, loadout and cooldowns (players). */
    public static final Supplier<AttachmentType<AbilityData>> ABILITIES = ATTACHMENTS.register("abilities",
            () -> AttachmentType.builder(() -> AbilityData.EMPTY)
                    .serialize(AbilityData.CODEC.fieldOf("data"))
                    .copyOnDeath()
                    .sync(AbilityData.STREAM_CODEC)
                    .build());

    /** Aura, Mana and Spirit (every living being). Only synced to the player it belongs to. */
    public static final Supplier<AttachmentType<EnergyData>> ENERGY = ATTACHMENTS.register("energy",
            () -> AttachmentType.builder(() -> EnergyData.EMPTY)
                    .serialize(EnergyData.CODEC.fieldOf("data"))
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, EnergyData.STREAM_CODEC)
                    .build());

    /** The player's race. */
    public static final Supplier<AttachmentType<RaceData>> RACE = ATTACHMENTS.register("race",
            () -> AttachmentType.builder(() -> RaceData.EMPTY)
                    .serialize(RaceData.CODEC.fieldOf("data"))
                    .copyOnDeath()
                    .sync(RaceData.STREAM_CODEC)
                    .build());

    private BrightAttachments() {}

    public static void init(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
