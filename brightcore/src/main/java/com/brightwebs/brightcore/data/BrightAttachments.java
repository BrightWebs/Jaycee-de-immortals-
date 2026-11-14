package com.brightwebs.brightcore.data;

import com.brightwebs.brightcore.BrightCore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** The data attachment that holds {@link AbilityData} on every player. */
public final class BrightAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BrightCore.ID);

    public static final Supplier<AttachmentType<AbilityData>> ABILITIES = ATTACHMENTS.register("abilities",
            () -> AttachmentType.builder(() -> AbilityData.EMPTY)
                    .serialize(AbilityData.CODEC.fieldOf("data")) // saved with the player
                    .copyOnDeath()                                // kept on respawn
                    .sync(AbilityData.STREAM_CODEC)               // sent to the client on every change
                    .build());

    private BrightAttachments() {}

    public static void init(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
