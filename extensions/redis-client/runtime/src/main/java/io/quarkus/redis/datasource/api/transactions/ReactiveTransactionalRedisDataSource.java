package io.quarkus.redis.datasource.api.transactions;

import io.quarkus.redis.datasource.api.bitmap.ReactiveTransactionalBitMapCommands;
import io.quarkus.redis.datasource.api.geo.ReactiveTransactionalGeoCommands;
import io.quarkus.redis.datasource.api.hash.ReactiveTransactionalHashCommands;
import io.quarkus.redis.datasource.api.hyperloglog.ReactiveTransactionalHyperLogLogCommands;
import io.quarkus.redis.datasource.api.keys.ReactiveTransactionalKeyCommands;
import io.quarkus.redis.datasource.api.list.ReactiveTransactionalListCommands;
import io.quarkus.redis.datasource.api.set.ReactiveTransactionalSetCommands;
import io.quarkus.redis.datasource.api.sortedset.ReactiveTransactionalSortedSetCommands;
import io.quarkus.redis.datasource.api.string.ReactiveTransactionalStringCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;

/**
 * Redis Data Source object used to execute commands in a Redis transaction ({@code MULTI}).
 * Note that the results of the enqueued commands are not available until the completion of the transaction.
 */
public interface ReactiveTransactionalRedisDataSource {

    /**
     * Discard the current transaction.
     */
    Uni<Void> discard();

    /**
     * Checks if the current transaction has been discarded by the user
     *
     * @return if the current transaction has been discarded by the user
     */
    boolean discarded();

    /**
     * Gets the object to execute commands manipulating hashes (a.k.a. {@code Map&lt;F, V&gt;}).
     * <p>
     * If you want to use a hash of {@code &lt;String -> Person&gt;} stored using String identifier, you would use:
     * {@code hash(String.class, String.class, Person.class)}.
     * If you want to use a hash of {@code &lt;String -> Person&gt;} stored using UUID identifier, you would use:
     * {@code hash(UUID.class, String.class, Person.class)}.
     *
     * @param redisKeyType the class of the keys
     * @param typeOfField the class of the fields
     * @param typeOfValue the class of the values
     * @param <K> the type of the redis key
     * @param <F> the type of the fields (map's keys)
     * @param <V> the type of the value
     * @return the object to execute commands manipulating hashes (a.k.a. {@code Map&lt;K, V&gt;}).
     */
    <K, F, V> ReactiveTransactionalHashCommands<K, F, V> hash(Class<K> redisKeyType, Class<F> typeOfField,
            Class<V> typeOfValue);

    /**
     * Gets the object to execute commands manipulating hashes (a.k.a. {@code Map&lt;String, V&gt;}).
     * <p>
     * This is a shortcut on {@code hash(String.class, String.class, V)}
     *
     * @param typeOfValue the class of the values
     * @param <V> the type of the value
     * @return the object to execute commands manipulating hashes (a.k.a. {@code Map&lt;String, V&gt;}).
     */
    default <V> ReactiveTransactionalHashCommands<String, String, V> hash(Class<V> typeOfValue) {
        return hash(String.class, String.class, typeOfValue);
    }

    /**
     * Gets the object to execute commands manipulating geo items (a.k.a. {@code {longitude, latitude, member}}).
     * <p>
     * {@code V} represents the type of the member, i.e. the localized <em>thing</em>.
     *
     * @param redisKeyType the class of the keys
     * @param memberType the class of the members
     * @param <K> the type of the redis key
     * @param <V> the type of the member
     * @return the object to execute geo commands.
     */
    <K, V> ReactiveTransactionalGeoCommands<K, V> geo(Class<K> redisKeyType, Class<V> memberType);

    /**
     * Gets the object to execute commands manipulating geo items (a.k.a. {@code {longitude, latitude, member}}).
     * <p>
     * {@code V} represents the type of the member, i.e. the localized <em>thing</em>.
     *
     * @param memberType the class of the members
     * @param <V> the type of the member
     * @return the object to execute geo commands.
     */
    default <V> ReactiveTransactionalGeoCommands<String, V> geo(Class<V> memberType) {
        return geo(String.class, memberType);
    }

    /**
     * Gets the object to execute commands manipulating keys and expiration times.
     *
     * @param redisKeyType the type of the keys
     * @param <K> the type of the key
     * @return the object to execute commands manipulating keys.
     */
    <K> ReactiveTransactionalKeyCommands<K> key(Class<K> redisKeyType);

    /**
     * Gets the object to execute commands manipulating keys and expiration times.
     *
     * @return the object to execute commands manipulating keys.
     */
    default ReactiveTransactionalKeyCommands<String> key() {
        return key(String.class);
    }

    /**
     * Gets the object to execute commands manipulating sorted sets.
     *
     * @param redisKeyType the type of the keys
     * @param valueType the type of the value sorted in the sorted sets
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @return the object to manipulate sorted sets.
     */
    <K, V> ReactiveTransactionalSortedSetCommands<K, V> sortedSet(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to execute commands manipulating sorted sets.
     *
     * @param valueType the type of the value sorted in the sorted sets
     * @param <V> the type of the value
     * @return the object to manipulate sorted sets.
     */
    default <V> ReactiveTransactionalSortedSetCommands<String, V> sortedSet(Class<V> valueType) {
        return sortedSet(String.class, valueType);
    }

    /**
     * Gets the object to execute commands manipulating stored strings.
     *
     * @param redisKeyType the type of the keys
     * @param valueType the type of the value, often String, or the value are encoded/decoded using codecs.
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @return the object to manipulate stored strings.
     */
    <K, V> ReactiveTransactionalStringCommands<K, V> string(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to execute commands manipulating stored strings.
     *
     * @param valueType the type of the value, often String, or the value are encoded/decoded using codecs.
     * @param <V> the type of the value
     * @return the object to manipulate stored strings.
     */
    default <V> ReactiveTransactionalStringCommands<String, V> string(Class<V> valueType) {
        return string(String.class, valueType);
    }

    /**
     * Gets the object to execute commands manipulating sets.
     *
     * @param redisKeyType the type of the keys
     * @param memberType the type of the member stored in each set
     * @param <K> the type of the key
     * @param <V> the type of the member
     * @return the object to manipulate sets.
     */
    <K, V> ReactiveTransactionalSetCommands<K, V> set(Class<K> redisKeyType, Class<V> memberType);

    /**
     * Gets the object to execute commands manipulating sets.
     *
     * @param memberType the type of the member stored in each set
     * @param <V> the type of the member
     * @return the object to manipulate sets.
     */
    default <V> ReactiveTransactionalSetCommands<String, V> set(Class<V> memberType) {
        return set(String.class, memberType);
    }

    /**
     * Gets the object to execute commands manipulating lists.
     *
     * @param redisKeyType the type of the keys
     * @param memberType the type of the member stored in each list
     * @param <K> the type of the key
     * @param <V> the type of the member
     * @return the object to manipulate sets.
     */
    <K, V> ReactiveTransactionalListCommands<K, V> list(Class<K> redisKeyType, Class<V> memberType);

    /**
     * Gets the object to execute commands manipulating lists.
     *
     * @param memberType the type of the member stored in each list
     * @param <V> the type of the member
     * @return the object to manipulate sets.
     */
    default <V> ReactiveTransactionalListCommands<String, V> list(Class<V> memberType) {
        return list(String.class, memberType);
    }

    /**
     * Gets the object to execute commands manipulating hyperloglog data structures.
     *
     * @param redisKeyType the type of the keys
     * @param memberType the type of the member stored in the data structure
     * @param <K> the type of the key
     * @param <V> the type of the member
     * @return the object to manipulate hyper log log data structures.
     */
    <K, V> ReactiveTransactionalHyperLogLogCommands<K, V> hyperloglog(Class<K> redisKeyType, Class<V> memberType);

    /**
     * Gets the object to execute commands manipulating hyperloglog data structures.
     *
     * @param memberType the type of the member stored in the data structure
     * @param <V> the type of the member
     * @return the object to manipulate hyper log log data structures.
     */
    default <V> ReactiveTransactionalHyperLogLogCommands<String, V> hyperloglog(Class<V> memberType) {
        return hyperloglog(String.class, memberType);
    }

    /**
     * Gets the object to execute commands manipulating bitmap data structures.
     *
     * @param redisKeyType the type of the keys
     * @param <K> the type of the key
     * @return the object to manipulate bitmap data structures.
     */
    <K> ReactiveTransactionalBitMapCommands<K> bitmap(Class<K> redisKeyType);

    /**
     * Gets the object to execute commands manipulating bitmap data structures.
     *
     * @return the object to manipulate bitmap data structures.
     */
    default ReactiveTransactionalBitMapCommands<String> bitmap() {
        return bitmap(String.class);
    }

    /**
     * Executes a command.
     * This method is used to execute commands not offered by the API.
     *
     * @param command the command name
     * @param args the parameters, encoded as String.
     * @return the response
     */
    Uni<Void> execute(String command, String... args);

    /**
     * Executes a command.
     * This method is used to execute commands not offered by the API.
     *
     * @param command the command
     * @param args the parameters, encoded as String.
     * @return the response
     */
    Uni<Void> execute(Command command, String... args);

    /**
     * Executes a command.
     * This method is used to execute commands not offered by the API.
     *
     * @param command the command
     * @param args the parameters, encoded as String.
     * @return the response
     */
    Uni<Void> execute(io.vertx.redis.client.Command command, String... args);
}
