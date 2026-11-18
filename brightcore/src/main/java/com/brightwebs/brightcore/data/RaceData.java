package com.brightwebs.brightcore.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * A player's race. Stored by id so a world still loads if a race is removed.
 *
 * @param race   the race id, or "" before the player has chosen
 * @param chosen true once the player has picked from the race menu
 */
public record RaceData(String race, boolean chosen) {

    public static final RaceData EMPTY = new RaceData("", false);

    public static final Codec<RaceData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("race", "").forGetter(RaceData::race),
            Codec.BOOL.optionalFieldOf("chosen", false).forGetter(RaceData::chosen)
    ).apply(i, RaceData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RaceData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public Optional<Identifier> raceId() {
        return race.isEmpty() ? Optional.empty() : Optional.ofNullable(Identifier.tryParse(race));
    }
}
