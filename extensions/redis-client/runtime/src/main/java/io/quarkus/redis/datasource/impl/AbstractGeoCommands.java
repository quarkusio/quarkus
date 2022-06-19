package io.quarkus.redis.datasource.impl;

import static io.quarkus.redis.datasource.impl.Validation.notNullOrEmpty;
import static io.quarkus.redis.datasource.impl.Validation.positive;
import static io.quarkus.redis.datasource.impl.Validation.validateLatitude;
import static io.quarkus.redis.datasource.impl.Validation.validateLongitude;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.Set;

import io.quarkus.redis.datasource.api.codecs.Codec;
import io.quarkus.redis.datasource.api.codecs.Codecs;
import io.quarkus.redis.datasource.api.geo.GeoAddArgs;
import io.quarkus.redis.datasource.api.geo.GeoItem;
import io.quarkus.redis.datasource.api.geo.GeoPosition;
import io.quarkus.redis.datasource.api.geo.GeoRadiusArgs;
import io.quarkus.redis.datasource.api.geo.GeoRadiusStoreArgs;
import io.quarkus.redis.datasource.api.geo.GeoSearchArgs;
import io.quarkus.redis.datasource.api.geo.GeoSearchStoreArgs;
import io.quarkus.redis.datasource.api.geo.GeoUnit;
import io.quarkus.redis.datasource.api.geo.GeoValue;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

class AbstractGeoCommands<K, V> extends AbstractRedisCommands {

    protected final Class<V> typeOfValue;
    protected final Codec<K> keyCodec;
    protected final Codec<V> valueCodec;

    AbstractGeoCommands(RedisCommandExecutor redis, Class<K> k, Class<V> v) {
        super(redis, new Marshaller(k, v));
        this.typeOfValue = v;
        this.keyCodec = Codecs.getDefaultCodecFor(k);
        this.valueCodec = Codecs.getDefaultCodecFor(v);
    }

    Uni<Response> _geoadd(K key, double longitude, double latitude, V member, GeoAddArgs args) {
        nonNull(key, "key");
        nonNull(member, "member");
        nonNull(args, "args");
        validateLongitude(longitude);
        validateLatitude(latitude);

        RedisCommand cmd = RedisCommand.of(Command.GEOADD)
                .put(marshaller.encode(key))
                .putAll(args.toArgs())
                .put(longitude)
                .put(latitude)
                .put(marshaller.encode(member));

        return execute(cmd);
    }

    Uni<Response> _geoadd(K key, GeoItem<V>... items) {
        nonNull(key, "key");
        notNullOrEmpty(items, "items");
        doesNotContainNull(items, "items");

        RedisCommand cmd = RedisCommand.of(Command.GEOADD)
                .put(marshaller.encode(key));
        for (GeoItem<V> item : items) {
            cmd
                    .put(Double.toString(item.longitude()))
                    .put(Double.toString(item.latitude()))
                    .put(marshaller.encode(item.member()));
        }
        return execute(cmd);
    }

    Uni<Response> _geoadd(K key, GeoAddArgs args, GeoItem<V>... items) {
        nonNull(key, "key");
        notNullOrEmpty(items, "items");
        doesNotContainNull(items, "items");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.GEOADD)
                .put(marshaller.encode(key))
                .put(args);

        for (GeoItem<V> item : items) {
            cmd.put(Double.toString(item.longitude()));
            cmd.put(Double.toString(item.latitude()));
            cmd.put(marshaller.encode(item.member()));
        }

        return execute(cmd);
    }

    Uni<Response> _geodist(K key, V from, V to, GeoUnit unit) {
        nonNull(key, "key");
        nonNull(from, "from");
        nonNull(to, "to");
        nonNull(unit, "unit");

        return execute(RedisCommand.of(Command.GEODIST)
                .put(marshaller.encode(key))
                .put(marshaller.encode(from))
                .put(marshaller.encode(to))
                .put(unit.name()));
    }

    Uni<Response> _geohash(K key, V... members) {
        nonNull(key, "key");
        notNullOrEmpty(members, "members");
        doesNotContainNull(members, "members");

        RedisCommand cmd = RedisCommand.of(Command.GEOHASH);
        cmd.put(marshaller.encode(key));
        for (V member : members) {
            cmd.put(marshaller.encode(member));
        }
        return execute(cmd);
    }

    Uni<Response> _geopos(K key, V... members) {
        nonNull(key, "key");
        notNullOrEmpty(members, "members");
        doesNotContainNull(members, "members");

        RedisCommand cmd = RedisCommand.of(Command.GEOPOS)
                .put(marshaller.encode(key));
        for (V member : members) {
            cmd.put(marshaller.encode(member));
        }

        return execute(cmd);
    }

    Uni<Response> _georadius(K key, double longitude, double latitude, double radius, GeoUnit unit) {
        nonNull(key, "key");
        positive(radius, "radius");
        validateLongitude(longitude);
        validateLatitude(latitude);
        nonNull(unit, "unit");

        return execute(RedisCommand.of(Command.GEORADIUS)
                .put(marshaller.encode(key)).put(longitude).put(latitude).put(radius)
                .put(unit.name()));
    }

    Uni<Response> _georadius(K key, double longitude, double latitude, double radius, GeoUnit unit,
            GeoRadiusArgs geoArgs) {
        nonNull(key, "key");
        validateLongitude(longitude);
        validateLatitude(latitude);
        positive(radius, "radius");
        nonNull(unit, "unit");
        nonNull(geoArgs, "geoArgs");
        return execute(RedisCommand.of(Command.GEORADIUS)
                .put(marshaller.encode(key)).put(longitude).put(latitude).put(radius)
                .put(unit.name())
                .putArgs(geoArgs));
    }

    Uni<Response> _georadius(K key, double longitude, double latitude, double radius, GeoUnit unit,
            GeoRadiusStoreArgs<K> geoArgs) {
        nonNull(key, "key");
        validateLongitude(longitude);
        validateLatitude(latitude);
        positive(radius, "radius");
        nonNull(unit, "unit");
        nonNull(geoArgs, "geoArgs");
        return execute(RedisCommand.of(Command.GEORADIUS)
                .put(marshaller.encode(key)).put(longitude).put(latitude).put(radius)
                .put(unit.name())
                .putArgs(geoArgs, keyCodec));
    }

    Uni<Response> _georadiusbymember(K key, V member, double distance, GeoUnit unit, GeoRadiusArgs geoArgs) {
        nonNull(key, "key");
        nonNull(member, "member");
        positive(distance, "distance");
        nonNull(unit, "unit");
        nonNull(geoArgs, "geoArgs");
        return execute(RedisCommand.of(Command.GEORADIUSBYMEMBER).put(marshaller.encode(key)).put(marshaller.encode(member))
                .put(distance).put(unit.name())
                .putArgs(geoArgs));
    }

    Uni<Response> _georadiusbymember(K key, V member, double distance, GeoUnit unit) {
        nonNull(key, "key");
        nonNull(member, "member");
        positive(distance, "distance");
        nonNull(unit, "unit");
        return execute(RedisCommand.of(Command.GEORADIUSBYMEMBER).put(marshaller.encode(key))
                .put(marshaller.encode(member)).put(distance).put(unit.name()));
    }

    Uni<Response> _georadiusbymember(K key, V member, double distance, GeoUnit unit, GeoRadiusStoreArgs<K> geoArgs) {
        nonNull(key, "key");
        nonNull(member, "member");
        positive(distance, "distance");
        nonNull(unit, "unit");
        nonNull(geoArgs, "geoArgs");
        return execute(RedisCommand.of(Command.GEORADIUSBYMEMBER).put(marshaller.encode(key))
                .put(marshaller.encode(member))
                .put(distance).put(unit.name())
                .putArgs(geoArgs, keyCodec));
    }

    Uni<Response> _geosearch(K key, GeoSearchArgs<V> geoArgs) {
        nonNull(key, "key");
        nonNull(geoArgs, "geoArgs");
        return execute(RedisCommand.of(Command.GEOSEARCH).put(marshaller.encode(key))
                .putArgs(geoArgs, valueCodec));
    }

    Uni<Response> _geosearchstore(K destination, K key, GeoSearchStoreArgs<V> args, boolean storeDist) {
        nonNull(destination, "destination");
        nonNull(key, "key");
        nonNull(args, "args");
        return execute(RedisCommand.of(Command.GEOSEARCHSTORE).put(marshaller.encode(destination)).put(marshaller.encode(key))
                .putArgs(args, valueCodec)
                .putFlag(storeDist, "STOREDIST"));
    }

    List<String> decodeHashList(Response r) {
        return marshaller.decodeAsList(r, Response::toString);
    }

    Set<V> decodeRadiusSet(Response response) {
        return marshaller.decodeAsSet(response, typeOfValue);
    }

    Double decodeDistance(Response r) {
        if (r == null) {
            return null;
        }
        return r.toDouble();
    }

    List<GeoPosition> decodeGeoPositions(Response response) {
        return marshaller.decodeAsList(response, nested -> {
            if (nested == null) {
                return null;
            } else {
                return GeoPosition.of(nested.get(0).toDouble(), nested.get(1).toDouble());
            }
        });
    }

    List<GeoValue<V>> decodeAsListOfGeoValues(Response r, boolean withDistance, boolean withCoordinates,
            boolean withHash) {
        List<GeoValue<V>> list = new ArrayList<>();
        for (Response response : r) {
            // The first value is always the member

            if (!withCoordinates && !withHash && !withDistance) {
                // Redis only return the member.
                list.add(new GeoValue<>(marshaller.decode(typeOfValue, response), OptionalDouble.empty(), OptionalLong.empty(),
                        OptionalDouble.empty(), OptionalDouble.empty()));
                continue;
            }

            V member = marshaller.decode(typeOfValue, response.get(0));
            if (withCoordinates && withDistance && withHash) {
                double dist = response.get(1).toDouble();
                long hash = response.get(2).toLong();
                double longitude = response.get(3).get(0).toDouble();
                double latitude = response.get(3).get(1).toDouble();
                list.add(new GeoValue<>(member, OptionalDouble.of(dist), OptionalLong.of(hash),
                        OptionalDouble.of(longitude), OptionalDouble.of(latitude)));
            } else if (withCoordinates && withDistance) {
                double dist = response.get(1).toDouble();
                double longitude = response.get(2).get(0).toDouble();
                double latitude = response.get(2).get(1).toDouble();
                list.add(new GeoValue<>(member, OptionalDouble.of(dist), OptionalLong.empty(),
                        OptionalDouble.of(longitude), OptionalDouble.of(latitude)));
            } else if (withCoordinates && withHash) {
                long hash = response.get(1).toLong();
                double longitude = response.get(2).get(0).toDouble();
                double latitude = response.get(2).get(1).toDouble();
                list.add(new GeoValue<>(member, OptionalDouble.empty(), OptionalLong.of(hash),
                        OptionalDouble.of(longitude), OptionalDouble.of(latitude)));
            } else if (withCoordinates) {
                // Only coordinates
                double longitude = response.get(1).get(0).toDouble();
                double latitude = response.get(1).get(1).toDouble();
                list.add(new GeoValue<>(member, OptionalDouble.empty(), OptionalLong.empty(), OptionalDouble.of(longitude),
                        OptionalDouble.of(latitude)));
            } else if (withDistance && !withHash) {
                // Only distance
                double dist = response.get(1).toDouble();
                list.add(new GeoValue<>(member, OptionalDouble.of(dist), OptionalLong.empty(), OptionalDouble.empty(),
                        OptionalDouble.empty()));
            } else if (!withDistance) {
                // Only hash
                long hash = response.get(1).toLong();
                list.add(new GeoValue<>(member, OptionalDouble.empty(), OptionalLong.of(hash), OptionalDouble.empty(),
                        OptionalDouble.empty()));
            } else {
                // Distance and Hash
                double dist = response.get(1).toDouble();
                long hash = response.get(2).toLong();
                list.add(new GeoValue<>(member, OptionalDouble.of(dist), OptionalLong.of(hash),
                        OptionalDouble.empty(), OptionalDouble.empty()));
            }
        }
        return list;
    }
}
