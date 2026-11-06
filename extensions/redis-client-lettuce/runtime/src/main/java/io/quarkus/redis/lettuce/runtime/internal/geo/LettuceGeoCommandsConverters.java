package io.quarkus.redis.lettuce.runtime.internal.geo;

import java.util.List;

import io.lettuce.core.GeoArgs;
import io.lettuce.core.GeoCoordinates;
import io.lettuce.core.GeoValue;
import io.lettuce.core.GeoWithin;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.GeoWithinListOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.quarkus.redis.datasource.codecs.Codec;
import io.quarkus.redis.datasource.geo.GeoAddArgs;
import io.quarkus.redis.datasource.geo.GeoItem;
import io.quarkus.redis.datasource.geo.GeoPosition;
import io.quarkus.redis.datasource.geo.GeoRadiusArgs;
import io.quarkus.redis.datasource.geo.GeoRadiusStoreArgs;
import io.quarkus.redis.datasource.geo.GeoSearchArgs;
import io.quarkus.redis.datasource.geo.GeoSearchStoreArgs;
import io.quarkus.redis.datasource.geo.GeoUnit;
import io.quarkus.redis.lettuce.runtime.internal.ArgReplay;
import io.quarkus.redis.runtime.datasource.Marshaller;

final class LettuceGeoCommandsConverters {

    private LettuceGeoCommandsConverters() {
        // Utility class
    }

    static io.lettuce.core.GeoAddArgs toGeoAddArgs(GeoAddArgs quarkus) {
        // Snapshot the tokens now so the XX/NX validation in toArgs() runs at call time, not when Lettuce builds
        // the command.
        List<Object> tokens = quarkus.toArgs();
        return new io.lettuce.core.GeoAddArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
    }

    @SuppressWarnings("unchecked")
    static <V> GeoValue<byte[]>[] toGeoValues(GeoItem<V>[] items, Marshaller marshaller) {
        GeoValue<byte[]>[] values = new GeoValue[items.length];
        for (int i = 0; i < items.length; i++) {
            GeoItem<V> item = items[i];
            values[i] = GeoValue.just(item.longitude(), item.latitude(), marshaller.encode(item.member()));
        }
        return values;
    }

    static GeoArgs.Unit toUnit(GeoUnit unit) {
        return switch (unit) {
            case M -> GeoArgs.Unit.m;
            case KM -> GeoArgs.Unit.km;
            case FT -> GeoArgs.Unit.ft;
            case MI -> GeoArgs.Unit.mi;
        };
    }

    static GeoArgs toGeoArgs(GeoRadiusArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        GeoArgs lettuce = new GeoArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
        // Set fields for Lettuce output parsing
        if (quarkus.hasDistance()) {
            lettuce.withDistance();
        }
        if (quarkus.hasCoordinates()) {
            lettuce.withCoordinates();
        }
        if (quarkus.hasHash()) {
            lettuce.withHash();
        }
        return lettuce;
    }

    static <K> CommandArgs<byte[], byte[]> toGeoRadiusStoreCommandArgs(byte[] key, double longitude,
            double latitude, double radius, GeoUnit unit, GeoRadiusStoreArgs<K> quarkus, Codec keyCodec) {
        CommandArgs<byte[], byte[]> args = new CommandArgs<>(ByteArrayCodec.INSTANCE)
                .addKey(key).add(longitude).add(latitude).add(radius).add(unit.toString());
        ArgReplay.replay(quarkus.toArgs(keyCodec), args);
        return args;
    }

    static <K> CommandArgs<byte[], byte[]> toGeoRadiusByMemberStoreCommandArgs(byte[] key, byte[] member,
            double distance, GeoUnit unit, GeoRadiusStoreArgs<K> quarkus, Codec keyCodec) {
        CommandArgs<byte[], byte[]> args = new CommandArgs<>(ByteArrayCodec.INSTANCE)
                .addKey(key).addValue(member).add(distance).add(unit.toString());
        ArgReplay.replay(quarkus.toArgs(keyCodec), args);
        return args;
    }

    static <V> CommandArgs<byte[], byte[]> toGeoSearchCommandArgs(byte[] key, GeoSearchArgs<V> quarkus,
            Codec valueCodec) {
        CommandArgs<byte[], byte[]> args = new CommandArgs<>(ByteArrayCodec.INSTANCE).addKey(key);
        ArgReplay.replay(quarkus.toArgs(valueCodec), args);
        return args;
    }

    static <V> CommandArgs<byte[], byte[]> toGeoSearchStoreCommandArgs(byte[] destination, byte[] key,
            GeoSearchStoreArgs<V> quarkus, Codec valueCodec, boolean storeDist) {
        CommandArgs<byte[], byte[]> args = new CommandArgs<>(ByteArrayCodec.INSTANCE).addKey(destination).addKey(key);
        ArgReplay.replay(quarkus.toArgs(valueCodec), args);
        if (storeDist) {
            args.add("STOREDIST");
        }
        return args;
    }

    static <V> CommandOutput<byte[], byte[], List<GeoWithin<byte[]>>> toGeoWithinOutput(GeoSearchArgs<V> quarkus) {
        return new GeoWithinListOutput<>(ByteArrayCodec.INSTANCE, quarkus.hasDistance(), quarkus.hasHash(),
                quarkus.hasCoordinates());
    }

    static GeoPosition toGeoPosition(GeoCoordinates coordinates) {
        if (coordinates == null) {
            return null;
        }
        return GeoPosition.of(coordinates.getX().doubleValue(), coordinates.getY().doubleValue());
    }

}
