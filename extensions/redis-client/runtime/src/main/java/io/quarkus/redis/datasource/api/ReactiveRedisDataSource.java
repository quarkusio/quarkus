package io.quarkus.redis.datasource.api;

import java.util.function.Function;

import io.quarkus.redis.datasource.api.bitmap.ReactiveBitMapCommands;
import io.quarkus.redis.datasource.api.geo.ReactiveGeoCommands;
import io.quarkus.redis.datasource.api.hash.ReactiveHashCommands;
import io.quarkus.redis.datasource.api.hyperloglog.ReactiveHyperLogLogCommands;
import io.quarkus.redis.datasource.api.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.api.list.ReactiveListCommands;
import io.quarkus.redis.datasource.api.pubsub.ReactivePubSubCommands;
import io.quarkus.redis.datasource.api.set.ReactiveSetCommands;
import io.quarkus.redis.datasource.api.sortedset.ReactiveSortedSetCommands;
import io.quarkus.redis.datasource.api.string.ReactiveStringCommands;
import io.quarkus.redis.datasource.api.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.datasource.api.transactions.TransactionResult;
import io.quarkus.redis.datasource.api.transactions.TransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.Response;

/**
 * Non-Blocking and Reactive Redis Data Source.
 * <p>
 * This class provides access to various <em>groups of methods</em>. Each method execute a Redis {@code command}.
 * Groups and methods are type-safe. The decomposition follows the Redis API group.
 * <p>
 * NOTE: Not all commands are exposed from this API. This is done on purpose. You can always use the low-level Redis
 * client to execute others commands.
 */
public interface ReactiveRedisDataSource {

    /**
     * Retrieves a {@link ReactiveRedisDataSource} using a single connection with the Redis Server.
     * The connection is acquired from the pool and released when the {@code Uni} returned by {@code function} produces
     * a {@code null} item or a failure.
     *
     * @param function the function receiving the single-connection data source and producing {@code null} when the
     *        connection can be released.
     */
    Uni<Void> withConnection(Function<ReactiveRedisDataSource, Uni<Void>> function);

    /**
     * Retrieves a {@link RedisDataSource} enqueuing commands in a Redis Transaction ({@code MULTI}).
     * Note that transaction acquires a single connection, and all the commands are enqueued in this connection.
     * The commands are only executed when the passed block emits the {@code null} item.
     *
     * The results of the commands are retrieved using the produced {@link TransactionResult}.
     *
     * The user can discard a transaction using the {@link TransactionalRedisDataSource#discard()} method.
     * In this case, the produced {@link TransactionResult} will be empty.
     *
     * @param tx the consumer receiving the transactional redis data source. The enqueued commands are only executed
     *        at the end of the block.
     */
    Uni<TransactionResult> withTransaction(Function<ReactiveTransactionalRedisDataSource, Uni<Void>> tx);

    /**
     * Retrieves a {@link RedisDataSource} enqueuing commands in a Redis Transaction ({@code MULTI}).
     * Note that transaction acquires a single connection, and all the commands are enqueued in this connection.
     * The commands are only executed when the passed block emits the {@code null} item.
     *
     * The results of the commands are retrieved using the produced {@link TransactionResult}.
     *
     * The user can discard a transaction using the {@link TransactionalRedisDataSource#discard()} method.
     * In this case, the produced {@link TransactionResult} will be empty.
     *
     * @param tx the consumer receiving the transactional redis data source. The enqueued commands are only executed
     *        at the end of the block.
     * @param watchedKeys the keys to watch during the execution of the transaction. If one of these key is modified before
     *        the completion of the transaction, the transaction is discarded.
     */
    Uni<TransactionResult> withTransaction(Function<ReactiveTransactionalRedisDataSource, Uni<Void>> tx, String... watchedKeys);

    /**
     * Execute the command <a href="https://redis.io/commands/select">SELECT</a>.
     * Summary: Change the selected database for the current connection
     * Group: connection
     * Requires Redis 1.0.0
     * <p>
     * This method is expected to be used inside a {@link #withConnection(Function)} block.
     *
     * @param index the database index.
     **/
    Uni<Void> select(long index);

    /**
     * Execute the command <a href="https://redis.io/commands/flushall">FLUSHALL</a>.
     * Summary: Remove all keys from all databases
     * Group: server
     * Requires Redis 1.0.0
     **/
    Uni<Void> flushall();

    /**
     * Gets the object to execute commands manipulating hashes (a.k.a. {@code Map&lt;F, V&gt;}).
     * <p>
     * If you want to use a hash of {@code &lt;String -> Person&gt;} stored using String identifier, you would use:
     * {@code hash(String.class, String.class, Person.class)}.
     * If you want to use a hash of {@code &lt;String -> Person&gt;} stored using UUID identifier, you would use:
     * {@code hash(UUID.class, String.class, Person.class)}.
     *
     * @param redisKeyType the class of the keys
     * @param fieldType the class of the fields
     * @param valueType the class of the values
     * @param <K> the type of the redis key
     * @param <F> the type of the fields (map's keys)
     * @param <V> the type of the value
     * @return the object to execute commands manipulating hashes (a.k.a. {@code Map&lt;K, V&gt;}).
     */
    <K, F, V> ReactiveHashCommands<K, F, V> hash(Class<K> redisKeyType, Class<F> fieldType, Class<V> valueType);

    /**
     * Gets the object to execute commands manipulating hashes (a.k.a. {@code Map&lt;String, V&gt;}).
     * <p>
     * This is a shortcut on {@code hash(String.class, String.class, V)}
     *
     * @param typeOfValue the class of the values
     * @param <V> the type of the value
     * @return the object to execute commands manipulating hashes (a.k.a. {@code Map&lt;String, V&gt;}).
     */
    default <V> ReactiveHashCommands<String, String, V> hash(Class<V> typeOfValue) {
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
    <K, V> ReactiveGeoCommands<K, V> geo(Class<K> redisKeyType, Class<V> memberType);

    /**
     * Gets the object to execute commands manipulating geo items (a.k.a. {@code {longitude, latitude, member}}).
     * <p>
     * {@code V} represents the type of the member, i.e. the localized <em>thing</em>.
     *
     * @param memberType the class of the members
     * @param <V> the type of the member
     * @return the object to execute geo commands.
     */
    default <V> ReactiveGeoCommands<String, V> geo(Class<V> memberType) {
        return geo(String.class, memberType);
    }

    /**
     * Gets the object to execute commands manipulating keys and expiration times.
     *
     * @param redisKeyType the type of the keys
     * @param <K> the type of the key
     * @return the object to execute commands manipulating keys.
     */
    <K> ReactiveKeyCommands<K> key(Class<K> redisKeyType);

    /**
     * Gets the object to execute commands manipulating keys and expiration times.
     *
     * @return the object to execute commands manipulating keys.
     */
    default ReactiveKeyCommands<String> key() {
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
    <K, V> ReactiveSortedSetCommands<K, V> sortedSet(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to execute commands manipulating sorted sets.
     *
     * @param valueType the type of the value sorted in the sorted sets
     * @param <V> the type of the value
     * @return the object to manipulate sorted sets.
     */
    default <V> ReactiveSortedSetCommands<String, V> sortedSet(Class<V> valueType) {
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
    <K, V> ReactiveStringCommands<K, V> string(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to execute commands manipulating stored strings.
     *
     * @param valueType the type of the value, often String, or the value are encoded/decoded using codecs.
     * @param <V> the type of the value
     * @return the object to manipulate stored strings.
     */
    default <V> ReactiveStringCommands<String, V> string(Class<V> valueType) {
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
    <K, V> ReactiveSetCommands<K, V> set(Class<K> redisKeyType, Class<V> memberType);

    /**
     * Gets the object to execute commands manipulating sets.
     *
     * @param memberType the type of the member stored in each set
     * @param <V> the type of the member
     * @return the object to manipulate sets.
     */
    default <V> ReactiveSetCommands<String, V> set(Class<V> memberType) {
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
    <K, V> ReactiveListCommands<K, V> list(Class<K> redisKeyType, Class<V> memberType);

    /**
     * Gets the object to execute commands manipulating lists.
     *
     * @param memberType the type of the member stored in each list
     * @param <V> the type of the member
     * @return the object to manipulate sets.
     */
    default <V> ReactiveListCommands<String, V> list(Class<V> memberType) {
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
    <K, V> ReactiveHyperLogLogCommands<K, V> hyperloglog(Class<K> redisKeyType, Class<V> memberType);

    /**
     * Gets the object to execute commands manipulating hyperloglog data structures.
     *
     * @param memberType the type of the member stored in the data structure
     * @param <V> the type of the member
     * @return the object to manipulate hyper log log data structures.
     */
    default <V> ReactiveHyperLogLogCommands<String, V> hyperloglog(Class<V> memberType) {
        return hyperloglog(String.class, memberType);
    }

    /**
     * Gets the object to execute commands manipulating bitmap data structures.
     *
     * @param redisKeyType the type of the keys
     * @param <K> the type of the key
     * @return the object to manipulate bitmap data structures.
     */
    <K> ReactiveBitMapCommands<K> bitmap(Class<K> redisKeyType);

    /**
     * Gets the object to execute commands manipulating bitmap data structures.
     *
     * @return the object to manipulate bitmap data structures.
     */
    default ReactiveBitMapCommands<String> bitmap() {
        return bitmap(String.class);
    }

    /**
     * Gets the objects to publish and receive messages.
     *
     * @param messageType the type of message
     * @param <V> the type of message
     * @return the object to publish and subscribe to Redis channels
     */
    <V> ReactivePubSubCommands<V> pubsub(Class<V> messageType);

    /**
     * Executes a command.
     * This method is used to execute commands not offered by the API.
     *
     * @param command the command name
     * @param args the parameters, encoded as String.
     * @return the response
     */
    Uni<Response> execute(String command, String... args);

    /**
     * Executes a command.
     * This method is used to execute commands not offered by the API.
     *
     * @param command the command
     * @param args the parameters, encoded as String.
     * @return the response
     */
    Uni<Response> execute(Command command, String... args);

    /**
     * Executes a command.
     * This method is used to execute commands not offered by the API.
     *
     * @param command the command
     * @param args the parameters, encoded as String.
     * @return the response
     */
    Uni<Response> execute(io.vertx.redis.client.Command command, String... args);

    /**
     * @return the underlying Redis client.
     */
    Redis getRedis();
}
