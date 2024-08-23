package io.quarkus.redis.datasource.geo;

import java.util.List;
import java.util.Set;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.smallrye.mutiny.Uni;

/**
 * Allows executing commands from the {@code geo} group.
 * See <a href="https://redis.io/commands/?group=geo">the geo command list</a> for further information about these commands.
 *
 * Geo-localized items are tuples composed of longitude, latitude and the member. The member is of type {@code &lt;V&gt}.
 * Each key can store multiple items.
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 */
public interface ReactiveGeoCommands<K, V> extends ReactiveRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/geoadd">GEOADD</a>.
     * Summary: Add one geospatial item in the geospatial index represented using a sorted set
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param longitude the longitude coordinate according to WGS84.
     * @param latitude the latitude coordinate according to WGS84.
     * @param member the member to add.
     * @return {@code true} if the geospatial item was added, {@code false} otherwise
     **/
    Uni<Boolean> geoadd(K key, double longitude, double latitude, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/geoadd">GEOADD</a>.
     * Summary: Add one geospatial item in the geospatial index represented using a sorted set
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param position the geo position
     * @param member the member to add.
     * @return {@code true} if the geospatial item was added, {@code false} otherwise
     **/
    Uni<Boolean> geoadd(K key, GeoPosition position, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/geoadd">GEOADD</a>.
     * Summary: Add one geospatial item in the geospatial index represented using a sorted set
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param item the item to add
     * @return {@code true} if the geospatial item was added, {@code false} otherwise
     **/
    Uni<Boolean> geoadd(K key, GeoItem<V> item);

    /**
     * Execute the command <a href="https://redis.io/commands/geoadd">GEOADD</a>.
     * Summary: Add one or more geospatial items in the geospatial index represented using a sorted set
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param items the geo-item triplets containing the longitude, latitude and name / value
     * @return the number of elements added to the sorted set (excluding score updates).
     **/
    Uni<Integer> geoadd(K key, GeoItem<V>... items);

    /**
     * Execute the command <a href="https://redis.io/commands/geoadd">GEOADD</a>.
     * Summary: Add one geospatial item in the geospatial index represented using a sorted set
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param longitude the longitude coordinate according to WGS84.
     * @param latitude the latitude coordinate according to WGS84.
     * @param member the member to add.
     * @param args additional arguments.
     * @return {@code true} if the geospatial item was added, {@code false} otherwise
     **/
    Uni<Boolean> geoadd(K key, double longitude, double latitude, V member, GeoAddArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/geoadd">GEOADD</a>.
     * Summary: Add one geospatial item in the geospatial index represented using a sorted set
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param item the item to add
     * @param args additional arguments.
     * @return {@code true} if the geospatial item was added, {@code false} otherwise
     **/
    Uni<Boolean> geoadd(K key, GeoItem<V> item, GeoAddArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/geoadd">GEOADD</a>.
     * Summary: Add one or more geospatial items in the geospatial index represented using a sorted set
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param args additional arguments.
     * @param items the items containing the longitude, latitude and name / value
     * @return the number of elements added to the sorted set (excluding score updates). If the {@code CH} option is
     *         specified, the number of elements that were changed (added or updated).
     **/
    Uni<Integer> geoadd(K key, GeoAddArgs args, GeoItem<V>... items);

    /**
     * Execute the command <a href="https://redis.io/commands/geodist">GEODIST</a>.
     * Summary: Returns the distance between two members of a geospatial index
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param from from member
     * @param to to member
     * @param unit the unit
     * @return The command returns the distance as a double in the specified unit, or {@code empty} if one or both the
     *         elements are missing.
     **/
    Uni<Double> geodist(K key, V from, V to, GeoUnit unit);

    /**
     * Execute the command <a href="https://redis.io/commands/geohash">GEOHASH</a>.
     * Summary: Returns members of a geospatial index as standard geohash strings
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param members the members
     * @return The command returns an array where each element is the Geohash corresponding to each member name passed
     *         as argument to the command.
     **/
    Uni<List<String>> geohash(K key, V... members);

    /**
     * Execute the command <a href="https://redis.io/commands/geopos">GEOPOS</a>.
     * Summary: Returns longitude and latitude of members of a geospatial index
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param members the items
     * @return The command returns an array where each element is a{@link GeoPosition} representing longitude and
     *         latitude (x,y) of each member name passed as argument to the command. Non-existing elements are reported as
     *         {@code null} elements.
     **/
    Uni<List<GeoPosition>> geopos(K key, V... members);

    /**
     * Execute the command <a href="https://redis.io/commands/georadius">GEORADIUS</a>.
     * Summary: Query a sorted set representing a geospatial index to fetch members matching a given maximum distance from a
     * poUni<Integer>
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param longitude the longitude
     * @param latitude the latitude
     * @param radius the radius
     * @param unit the unit
     * @return the list of values.
     * @deprecated See https://redis.io/commands/georadius
     **/
    @Deprecated
    Uni<Set<V>> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit);

    /**
     * Execute the command <a href="https://redis.io/commands/georadius">GEORADIUS</a>.
     * Summary: Query a sorted set representing a geospatial index to fetch members matching a given maximum distance from a
     * poUni<Integer>
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param position the position
     * @param radius the radius
     * @param unit the unit
     * @return the list of values.
     * @deprecated See https://redis.io/commands/georadius
     **/
    @Deprecated
    Uni<Set<V>> georadius(K key, GeoPosition position, double radius, GeoUnit unit);

    /**
     * Execute the command <a href="https://redis.io/commands/georadius">GEORADIUS</a>.
     * Summary: Query a sorted set representing a geospatial index to fetch members matching a given maximum distance from a
     * poUni<Integer>
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param longitude the longitude
     * @param latitude the latitude
     * @param radius the radius
     * @param unit the unit
     * @param geoArgs the extra arguments of the {@code GEORADIUS} command
     * @return the list of {@link GeoValue}. Only the field requested using {@code geoArgs} are populated in the returned
     *         {@link GeoValue}.
     * @deprecated See https://redis.io/commands/georadius
     **/
    @Deprecated
    Uni<List<GeoValue<V>>> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit,
            GeoRadiusArgs geoArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/georadius">GEORADIUS</a>.
     * Summary: Query a sorted set representing a geospatial index to fetch members matching a given maximum distance from a
     * poUni<Integer>
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param position the position
     * @param radius the radius
     * @param unit the unit
     * @param geoArgs the extra arguments of the {@code GEORADIUS} command
     * @return the list of {@link GeoValue}. Only the field requested using {@code geoArgs} are populated in the returned
     *         {@link GeoValue}.
     * @deprecated See https://redis.io/commands/georadius
     **/
    @Deprecated
    Uni<List<GeoValue<V>>> georadius(K key, GeoPosition position, double radius, GeoUnit unit, GeoRadiusArgs geoArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/georadius">GEORADIUS</a>.
     * Summary: Query a sorted set representing a geospatial index to fetch members matching a given maximum distance from a
     * poUni<Integer>.
     * It also stores the results in a sorted set.
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param longitude the longitude
     * @param latitude the latitude
     * @param radius the radius
     * @param unit the unit
     * @param geoArgs the extra {@code STORE} arguments of the {@code GEORADIUS} command
     * @return The number of items contained in the result written at the configured key.
     * @deprecated See https://redis.io/commands/georadius
     **/
    @Deprecated
    Uni<Long> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusStoreArgs<K> geoArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/georadius">GEORADIUS</a>.
     * Summary: Query a sorted set representing a geospatial index to fetch members matching a given maximum distance from a
     * poUni<Integer>.
     * It also stores the results in a sorted set.
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param position the position
     * @param radius the radius
     * @param unit the unit
     * @param geoArgs the extra {@code STORE} arguments of the {@code GEORADIUS} command
     * @return The number of items contained in the result written at the configured key.
     * @deprecated See https://redis.io/commands/georadius
     **/
    @Deprecated
    Uni<Long> georadius(K key, GeoPosition position, double radius, GeoUnit unit, GeoRadiusStoreArgs<K> geoArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a>.
     * Summary: Query a sorted set representing a geospatial index to fetch members matching a given maximum distance from a
     * member
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param member the member
     * @param distance the max distance
     * @return the set of values
     * @deprecated See https://redis.io/commands/georadiusbymember
     **/
    @Deprecated
    Uni<Set<V>> georadiusbymember(K key, V member, double distance, GeoUnit unit);

    /**
     * Execute the command <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a>.
     * Summary: Query a sorted set representing a geospatial index to fetch members matching a given maximum distance from a
     * member
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param member the member
     * @param distance the max distance
     * @param geoArgs the extra arguments of the {@code GEORADIUS} command
     * @return the list of {@link GeoValue}. Only the field requested using {@code geoArgs} are populated in the
     *         returned {@link GeoValue values}.
     * @deprecated See https://redis.io/commands/georadiusbymember
     **/
    @Deprecated
    Uni<List<GeoValue<V>>> georadiusbymember(K key, V member, double distance, GeoUnit unit, GeoRadiusArgs geoArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a>.
     * Summary: Query a sorted set representing a geospatial index to fetch members matching a given maximum distance from a
     * member.
     * It also stores the results in a sorted set.
     * Group: geo
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param member the member
     * @param distance the max distance
     * @param geoArgs the extra arguments of the {@code GEORADIUS} command
     * @return The number of items contained in the result written at the configured key.
     * @deprecated See https://redis.io/commands/georadiusbymember
     **/
    @Deprecated
    Uni<Long> georadiusbymember(K key, V member, double distance, GeoUnit unit, GeoRadiusStoreArgs<K> geoArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/geosearch">GEOSEARCH</a>.
     * Summary: Query a sorted set representing a geospatial index to fetch members inside an area of a box or a circle.
     * Group: geo
     * Requires Redis 6.2.0
     *
     * @return the list of {@code GeoValue&lt;V>&gt;}. The populated data depends on the parameters configured in {@code args}.
     **/
    Uni<List<GeoValue<V>>> geosearch(K key, GeoSearchArgs<V> args);

    /**
     * Execute the command <a href="https://redis.io/commands/geosearchstore">GEOSEARCHSTORE</a>.
     * Summary: Query a sorted set representing a geospatial index to fetch members inside an area of a box or a circle,
     * and store the result in another key.
     * Group: geo
     * Requires Redis 6.2.0
     *
     * @return the number of elements in the resulting set.
     **/
    Uni<Long> geosearchstore(K destination, K key, GeoSearchStoreArgs<V> args, boolean storeDist);
}
