package com.brightwebs.jaycee.training;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// attribute levels and EXP (saved), plus the recent training feed for the stats menu
// (synced to the owner only, not saved). the feed lists what trained recently and from what
public record TrainingData(Map<String, Integer> levels, Map<String, Double> exp, List<Recent> recent) {
    public record Recent(String attr, String activity, int gained, long lastTick) {
        static final StreamCodec<ByteBuf, Recent> STREAM = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Recent::attr, ByteBufCodecs.STRING_UTF8, Recent::activity,
                ByteBufCodecs.VAR_INT, Recent::gained, ByteBufCodecs.VAR_LONG, Recent::lastTick, Recent::new);
    }

    public static final TrainingData EMPTY = new TrainingData(Map.of(), Map.of(), List.of());

    public static final Codec<TrainingData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("levels", Map.of()).forGetter(TrainingData::levels),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("exp", Map.of()).forGetter(TrainingData::exp)
    ).apply(i, (l, e) -> new TrainingData(l, e, List.of())));

    public static final StreamCodec<ByteBuf, TrainingData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), d -> new HashMap<>(d.levels()),
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.DOUBLE), d -> new HashMap<>(d.exp()),
            Recent.STREAM.apply(ByteBufCodecs.list()), TrainingData::recent,
            (l, e, r) -> new TrainingData(Map.copyOf(l), Map.copyOf(e), List.copyOf(r)));

    public int level(Attr a) { return levels.getOrDefault(a.key(), 0); }
    public double exp(Attr a) { return exp.getOrDefault(a.key(), 0.0); }
}
