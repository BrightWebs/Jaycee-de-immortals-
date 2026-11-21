package com.brightwebs.jaycee.ichor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// a player's ichor. go through IchorAPI to change it so it saves and syncs
// ichor = the real amount, birthright = what they got on first join,
// revealed = what they last found out by praying (-1 = never prayed)
public record IchorData(int ichor, Birthright birthright, int revealed) {

    public static final IchorData EMPTY = new IchorData(0, Birthright.UNDRAWN, -1);

    public static final Codec<IchorData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("ichor", 0).forGetter(IchorData::ichor),
            Birthright.CODEC.optionalFieldOf("birthright", Birthright.UNDRAWN).forGetter(IchorData::birthright),
            Codec.INT.optionalFieldOf("revealed", -1).forGetter(IchorData::revealed)
    ).apply(i, IchorData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, IchorData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public IchorData {
        ichor = Math.clamp(ichor, 0, Grade.MAX_ICHOR);
    }

    public Grade grade() { return Grade.of(ichor); }

    // through the mist: from seer up you always know your own ichor
    public boolean seesOwnIchor() { return ichor >= Grade.SIGHT_THRESHOLD; }

    public IchorData withIchor(int value) { return new IchorData(value, birthright, revealed); }
    public IchorData withBirthright(Birthright b) { return new IchorData(ichor, b, revealed); }
    public IchorData withRevealed(int value) { return new IchorData(ichor, birthright, value); }
}
