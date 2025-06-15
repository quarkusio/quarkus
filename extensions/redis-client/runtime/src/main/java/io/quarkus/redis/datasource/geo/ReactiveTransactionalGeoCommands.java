package io.quarkus.redis.datasource.geo;

import io.quarkus.redis.datasource.ReactiveTransactionalRedisCommands;
import io.smallrye.mutiny.Uni;

public interface ReactiveTransactionalGeoCommands<K, V> extends ReactiveTransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/geoadd">GEOADD</a>. Summary: Add one geospatial item in
     * the geospatial index represented using a sorted set Group: geo Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param longitude
     *        the longitude coordinate according to WGS84.
     * @param latitude
     *        the latitude coordinate according to WGS84.
     * @param member
     *        the member to add.
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> geoadd(K key, double longitude, double latitude, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/geoadd">GEOADD</a>. Summary: Add one geospatial item in
     * the geospatial index represented using a sorted set Group: geo Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param position
     *        the geo position
     * @param member
     *        the member to add.
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> geoadd(K key, GeoPosition position, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/geoadd">GEOADD</a>. Summary: Add one geospatial item in
     * the geospatial index represented using a sorted set Group: geo Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param item
     *        the item to add
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> geoadd(K key, GeoItem<V> item);

    /**
     * Execute the command <a href="https://redis.io/commands/geoadd">GEOADD</a>. Summary: Add one or more geospatial
     * items in the geospatial index represented using a sorted set Group: geo Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param items
     *        the geo-item triplets containing the longitude, latitude and name / value
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> geoadd(K key, GeoItem<V>... items);

    /**
     * Execute the command <a href="https://redis.io/commands/geoadd">GEOADD</a>. Summary: Add one geospatial item in
     * the geospatial index represented using a sorted set Group: geo Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param longitude
     *        the longitude coordinate according to WGS84.
     * @param latitude
     *        the latitude coordinate according to WGS84.
     * @param member
     *        the member to add.
     * @param args
     *        additional arguments.
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> geoadd(K key, double longitude, double latitude, V member, GeoAddArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/geoadd">GEOADD</a>. Summary: Add one geospatial item in
     * the geospatial index represented using a sorted set Group: geo Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param item
     *        the item to add
     * @param args
     *        additional arguments.
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> geoadd(K key, GeoItem<V> item, GeoAddArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/geoadd">GEOADD</a>. Summary: Add one or more geospatial
     * items in the geospatial index represented using a sorted set Group: geo Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param args
     *        additional arguments.
     * @param items
     *        the items containing the longitude, latitude and name / value
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> geoadd(K key, GeoAddArgs args, GeoItem<V>... items);

    /**
     * Execute the command <a href="https://redis.io/commands/geodist">GEODIST</a>. Summary: Returns the distance
     * between two members of a geospatial index Group: geo Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param from
     *        from member
     * @param to
     *        to member
     * @param unit
     *        the unit
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> geodist(K key, V from, V to, GeoUnit unit);

    /**
     * Execute the command <a href="https://redis.io/commands/geohash">GEOHASH</a>. Summary: Returns members of a
     * geospatial index as standard geohash strings Group: geo Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param members
     *        the members
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> geohash(K key, V... members);

    /**
     * Execute the command <a href="https://redis.io/commands/geopos">GEOPOS</a>. Summary: Returns longitude and
     * latitude of members of a geospatial index Group: geo Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param members
     *        the items
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> geopos(K key, V... members);

    /**
     * Execute the command <a href="https://redis.io/commands/georadius">GEORADIUS</a>. Summary: Query a sorted set
     * representing a geospatial index to fetch members matching a given maximum distance from a point Group: geo
     * Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param longitude
     *        the longitude
     * @param latitude
     *        the latitude
     * @param radius
     *        the radius
     * @param unit
     *        the unit
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     *
     * @deprecated See https://redis.io/commands/georadius
     */
    @Deprecated
    Uni<Void> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit);

    /**
     * Execute the command <a href="https://redis.io/commands/georadius">GEORADIUS</a>. Summary: Query a sorted set
     * representing a geospatial index to fetch members matching a given maximum distance from a point Group: geo
     * Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param position
     *        the position
     * @param radius
     *        the radius
     * @param unit
     *        the unit
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     *
     * @deprecated See https://redis.io/commands/georadius
     */
    @Deprecated
    Uni<Void> georadius(K key, GeoPosition position, double radius, GeoUnit unit);

    /**
     * Execute the command <a href="https://redis.io/commands/georadius">GEORADIUS</a>. Summary: Query a sorted set
     * representing a geospatial index to fetch members matching a given maximum distance from a point Group: geo
     * Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param longitude
     *        the longitude
     * @param latitude
     *        the latitude
     * @param radius
     *        the radius
     * @param unit
     *        the unit
     * @param geoArgs
     *        the extra arguments of the {@code GEORADIUS} command
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     *
     * @deprecated See https://redis.io/commands/georadius
     */
    @Deprecated
    Uni<Void> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusArgs geoArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/georadius">GEORADIUS</a>. Summary: Query a sorted set
     * representing a geospatial index to fetch members matching a given maximum distance from a point Group: geo
     * Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param position
     *        the position
     * @param radius
     *        the radius
     * @param unit
     *        the unit
     * @param geoArgs
     *        the extra arguments of the {@code GEORADIUS} command
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     *
     * @deprecated See https://redis.io/commands/georadius
     */
    @Deprecated
    Uni<Void> georadius(K key, GeoPosition position, double radius, GeoUnit unit, GeoRadiusArgs geoArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/georadius">GEORADIUS</a>. Summary: Query a sorted set
     * representing a geospatial index to fetch members matching a given maximum distance from a point. It also stores
     * the results in a sorted set. Group: geo Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param longitude
     *        the longitude
     * @param latitude
     *        the latitude
     * @param radius
     *        the radius
     * @param unit
     *        the unit
     * @param geoArgs
     *        the extra {@code STORE} arguments of the {@code GEORADIUS} command
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     *
     * @deprecated See https://redis.io/commands/georadius
     */
    @Deprecated
    Uni<Void> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit,
            GeoRadiusStoreArgs<K> geoArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/georadius">GEORADIUS</a>. Summary: Query a sorted set
     * representing a geospatial index to fetch members matching a given maximum distance from a point. It also stores
     * the results in a sorted set. Group: geo Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param position
     *        the position
     * @param radius
     *        the radius
     * @param unit
     *        the unit
     * @param geoArgs
     *        the extra {@code STORE} arguments of the {@code GEORADIUS} command
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     *
     * @deprecated See https://redis.io/commands/georadius
     */
    @Deprecated
    Uni<Void> georadius(K key, GeoPosition position, double radius, GeoUnit unit, GeoRadiusStoreArgs<K> geoArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a>. Summary: Query a
     * sorted set representing a geospatial index to fetch members matching a given maximum distance from a member
     * Group: geo Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param member
     *        the member
     * @param distance
     *        the max distance
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     *
     * @deprecated See https://redis.io/commands/georadiusbymember
     */
    @Deprecated
    Uni<Void> georadiusbymember(K key, V member, double distance, GeoUnit unit);

    /**
     * Execute the command <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a>. Summary: Query a
     * sorted set representing a geospatial index to fetch members matching a given maximum distance from a member
     * Group: geo Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param member
     *        the member
     * @param distance
     *        the max distance
     * @param geoArgs
     *        the extra arguments of the {@code GEORADIUS} command
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     *
     * @deprecated See https://redis.io/commands/georadiusbymember
     */
    @Deprecated
    Uni<Void> georadiusbymember(K key, V member, double distance, GeoUnit unit, GeoRadiusArgs geoArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a>. Summary: Query a
     * sorted set representing a geospatial index to fetch members matching a given maximum distance from a member. It
     * also stores the results in a sorted set. Group: geo Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param member
     *        the member
     * @param distance
     *        the max distance
     * @param geoArgs
     *        the extra arguments of the {@code GEORADIUS} command
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     *
     * @deprecated See https://redis.io/commands/georadiusbymember
     */
    @Deprecated
    Uni<Void> georadiusbymember(K key, V member, double distance, GeoUnit unit, GeoRadiusStoreArgs<K> geoArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/geosearch">GEOSEARCH</a>. Summary: Query a sorted set
     * representing a geospatial index to fetch members inside an area of a box or a circle. Group: geo Requires Redis
     * 6.2.0
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> geosearch(K key, GeoSearchArgs<V> args);

    /**
     * Execute the command <a href="https://redis.io/commands/geosearchstore">GEOSEARCHSTORE</a>. Summary: Query a
     * sorted set representing a geospatial index to fetch members inside an area of a box or a circle, and store the
     * result in another key. Group: geo Requires Redis 6.2.0
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> geosearchstore(K destination, K key, GeoSearchStoreArgs<V> args, boolean storeDist);
}
