package io.quarkus.redis.datasource;

import static io.quarkus.redis.runtime.datasource.Marshaller.STRING_TYPE_REFERENCE;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.autosuggest.ReactiveAutoSuggestCommands;
import io.quarkus.redis.datasource.bitmap.ReactiveBitMapCommands;
import io.quarkus.redis.datasource.bloom.ReactiveBloomCommands;
import io.quarkus.redis.datasource.countmin.ReactiveCountMinCommands;
import io.quarkus.redis.datasource.cuckoo.ReactiveCuckooCommands;
import io.quarkus.redis.datasource.geo.ReactiveGeoCommands;
import io.quarkus.redis.datasource.graph.ReactiveGraphCommands;
import io.quarkus.redis.datasource.hash.ReactiveHashCommands;
import io.quarkus.redis.datasource.hyperloglog.ReactiveHyperLogLogCommands;
import io.quarkus.redis.datasource.json.ReactiveJsonCommands;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.list.ReactiveListCommands;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.quarkus.redis.datasource.search.ReactiveSearchCommands;
import io.quarkus.redis.datasource.set.ReactiveSetCommands;
import io.quarkus.redis.datasource.sortedset.ReactiveSortedSetCommands;
import io.quarkus.redis.datasource.stream.ReactiveStreamCommands;
import io.quarkus.redis.datasource.string.ReactiveStringCommands;
import io.quarkus.redis.datasource.timeseries.ReactiveTimeSeriesCommands;
import io.quarkus.redis.datasource.topk.ReactiveTopKCommands;
import io.quarkus.redis.datasource.transactions.OptimisticLockingTransactionResult;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.common.annotation.Experimental;
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
     * <p>
     * The results of the commands are retrieved using the produced {@link TransactionResult}.
     * <p>
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
     * <p>
     * The results of the commands are retrieved using the produced {@link TransactionResult}.
     * <p>
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
     * Retrieves a {@link RedisDataSource} enqueuing commands in a Redis Transaction ({@code MULTI}).
     * Note that transaction acquires a single connection, and all the commands are enqueued in this connection.
     * The commands are only executed when the passed block emits the {@code null} item.
     * <p>
     * This variant also allows executing code before the transaction gets started but after the key being watched:
     *
     * <pre>
     *     WATCH key
     *     // preTxBlock
     *     element = ZRANGE k 0 0
     *     // TxBlock
     *     MULTI
     *        ZREM k element
     *     EXEC
     * </pre>
     * <p>
     * The {@code preTxBlock} returns a {@link Uni Uni&lt;I&gt;}. The produced value is received by the {@code tx} block,
     * which can use that value to execute the appropriate operation in the transaction. The produced value can also be
     * retrieved from the produced {@link OptimisticLockingTransactionResult}. Commands issued in the {@code preTxBlock }
     * must used the passed (single-connection) {@link ReactiveRedisDataSource} instance.
     * <p>
     * If the {@code preTxBlock} throws an exception or emits a failure, the transaction is not executed, and the returned
     * {@link OptimisticLockingTransactionResult} is empty.
     * <p>
     * This construct allows implementing operation relying on optimistic locking.
     * The results of the commands are retrieved using the produced {@link OptimisticLockingTransactionResult}.
     * <p>
     * The user can discard a transaction using the {@link TransactionalRedisDataSource#discard()} method.
     * In this case, the produced {@link OptimisticLockingTransactionResult} will be empty.
     *
     * @param tx the consumer receiving the transactional redis data source. The enqueued commands are only executed
     *        at the end of the block.
     * @param watchedKeys the keys to watch during the execution of the transaction. If one of these key is modified before
     *        the completion of the transaction, the transaction is discarded.
     */
    <I> Uni<OptimisticLockingTransactionResult<I>> withTransaction(Function<ReactiveRedisDataSource, Uni<I>> preTxBlock,
            BiFunction<I, ReactiveTransactionalRedisDataSource, Uni<Void>> tx,
            String... watchedKeys);

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
    <K, F, V> ReactiveHashCommands<K, F, V> hash(TypeReference<K> redisKeyType, TypeReference<F> fieldType,
            TypeReference<V> valueType);

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
     * Gets the object to execute commands manipulating hashes (a.k.a. {@code Map&lt;String, V&gt;}).
     * <p>
     * This is a shortcut on {@code hash(String.class, String.class, V)}
     *
     * @param typeOfValue the class of the values
     * @param <V> the type of the value
     * @return the object to execute commands manipulating hashes (a.k.a. {@code Map&lt;String, V&gt;}).
     */
    default <V> ReactiveHashCommands<String, String, V> hash(TypeReference<V> typeOfValue) {
        return hash(STRING_TYPE_REFERENCE, STRING_TYPE_REFERENCE, typeOfValue);
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
     * @param redisKeyType the class of the keys
     * @param memberType the class of the members
     * @param <K> the type of the redis key
     * @param <V> the type of the member
     * @return the object to execute geo commands.
     */
    <K, V> ReactiveGeoCommands<K, V> geo(TypeReference<K> redisKeyType, TypeReference<V> memberType);

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
     * Gets the object to execute commands manipulating geo items (a.k.a. {@code {longitude, latitude, member}}).
     * <p>
     * {@code V} represents the type of the member, i.e. the localized <em>thing</em>.
     *
     * @param memberType the class of the members
     * @param <V> the type of the member
     * @return the object to execute geo commands.
     */
    default <V> ReactiveGeoCommands<String, V> geo(TypeReference<V> memberType) {
        return geo(STRING_TYPE_REFERENCE, memberType);
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
     * @param redisKeyType the type of the keys
     * @param <K> the type of the key
     * @return the object to execute commands manipulating keys.
     */
    <K> ReactiveKeyCommands<K> key(TypeReference<K> redisKeyType);

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
     * @param redisKeyType the type of the keys
     * @param valueType the type of the value sorted in the sorted sets
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @return the object to manipulate sorted sets.
     */
    <K, V> ReactiveSortedSetCommands<K, V> sortedSet(TypeReference<K> redisKeyType, TypeReference<V> valueType);

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
     * Gets the object to execute commands manipulating sorted sets.
     *
     * @param valueType the type of the value sorted in the sorted sets
     * @param <V> the type of the value
     * @return the object to manipulate sorted sets.
     */
    default <V> ReactiveSortedSetCommands<String, V> sortedSet(TypeReference<V> valueType) {
        return sortedSet(STRING_TYPE_REFERENCE, valueType);
    }

    /**
     * Gets the object to execute commands manipulating stored strings.
     * <p>
     * <strong>NOTE:</strong> Instead of {@code string}, this group is named {@code value} to avoid the confusion with the
     * Java String type. Indeed, Redis strings can be strings, numbers, byte arrays...
     *
     * @param redisKeyType the type of the keys
     * @param valueType the type of the value, often String, or the value are encoded/decoded using codecs.
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @return the object to manipulate stored strings.
     */
    <K, V> ReactiveValueCommands<K, V> value(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to execute commands manipulating stored strings.
     * <p>
     * <strong>NOTE:</strong> Instead of {@code string}, this group is named {@code value} to avoid the confusion with the
     * Java String type. Indeed, Redis strings can be strings, numbers, byte arrays...
     *
     * @param redisKeyType the type of the keys
     * @param valueType the type of the value, often String, or the value are encoded/decoded using codecs.
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @return the object to manipulate stored strings.
     */
    <K, V> ReactiveValueCommands<K, V> value(TypeReference<K> redisKeyType, TypeReference<V> valueType);

    /**
     * Gets the object to execute commands manipulating stored strings.
     * <p>
     * <strong>NOTE:</strong> Instead of {@code string}, this group is named {@code value} to avoid the confusion with the
     * Java String type. Indeed, Redis strings can be strings, numbers, byte arrays...
     *
     * @param valueType the type of the value, often String, or the value are encoded/decoded using codecs.
     * @param <V> the type of the value
     * @return the object to manipulate stored strings.
     */
    default <V> ReactiveValueCommands<String, V> value(Class<V> valueType) {
        return value(String.class, valueType);
    }

    /**
     * Gets the object to execute commands manipulating stored strings.
     * <p>
     * <strong>NOTE:</strong> Instead of {@code string}, this group is named {@code value} to avoid the confusion with the
     * Java String type. Indeed, Redis strings can be strings, numbers, byte arrays...
     *
     * @param valueType the type of the value, often String, or the value are encoded/decoded using codecs.
     * @param <V> the type of the value
     * @return the object to manipulate stored strings.
     */
    default <V> ReactiveValueCommands<String, V> value(TypeReference<V> valueType) {
        return value(STRING_TYPE_REFERENCE, valueType);
    }

    /**
     * Gets the object to execute commands manipulating stored strings.
     *
     * @param redisKeyType the type of the keys
     * @param valueType the type of the value, often String, or the value are encoded/decoded using codecs.
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @return the object to manipulate stored strings.
     * @deprecated Use {@link #value(Class, Class)} instead
     */
    @Deprecated
    <K, V> ReactiveStringCommands<K, V> string(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to execute commands manipulating stored strings.
     *
     * @param valueType the type of the value, often String, or the value are encoded/decoded using codecs.
     * @param <V> the type of the value
     * @return the object to manipulate stored strings.
     * @deprecated Use {@link #value(Class)} instead
     */
    @Deprecated
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
     * @param redisKeyType the type of the keys
     * @param memberType the type of the member stored in each set
     * @param <K> the type of the key
     * @param <V> the type of the member
     * @return the object to manipulate sets.
     */
    <K, V> ReactiveSetCommands<K, V> set(TypeReference<K> redisKeyType, TypeReference<V> memberType);

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
     * Gets the object to execute commands manipulating sets.
     *
     * @param memberType the type of the member stored in each set
     * @param <V> the type of the member
     * @return the object to manipulate sets.
     */
    default <V> ReactiveSetCommands<String, V> set(TypeReference<V> memberType) {
        return set(STRING_TYPE_REFERENCE, memberType);
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
     * Gets the object to execute commands manipulating lists.
     *
     * @param redisKeyType the type of the keys
     * @param memberType the type of the member stored in each list
     * @param <K> the type of the key
     * @param <V> the type of the member
     * @return the object to manipulate sets.
     */
    <K, V> ReactiveListCommands<K, V> list(TypeReference<K> redisKeyType, TypeReference<V> memberType);

    /**
     * Gets the object to execute commands manipulating lists.
     *
     * @param memberType the type of the member stored in each list
     * @param <V> the type of the member
     * @return the object to manipulate sets.
     */
    default <V> ReactiveListCommands<String, V> list(TypeReference<V> memberType) {
        return list(STRING_TYPE_REFERENCE, memberType);
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
     * @param redisKeyType the type of the keys
     * @param memberType the type of the member stored in the data structure
     * @param <K> the type of the key
     * @param <V> the type of the member
     * @return the object to manipulate hyper log log data structures.
     */
    <K, V> ReactiveHyperLogLogCommands<K, V> hyperloglog(TypeReference<K> redisKeyType, TypeReference<V> memberType);

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
     * Gets the object to execute commands manipulating hyperloglog data structures.
     *
     * @param memberType the type of the member stored in the data structure
     * @param <V> the type of the member
     * @return the object to manipulate hyper log log data structures.
     */
    default <V> ReactiveHyperLogLogCommands<String, V> hyperloglog(TypeReference<V> memberType) {
        return hyperloglog(STRING_TYPE_REFERENCE, memberType);
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
     * @param redisKeyType the type of the keys
     * @param <K> the type of the key
     * @return the object to manipulate bitmap data structures.
     */
    <K> ReactiveBitMapCommands<K> bitmap(TypeReference<K> redisKeyType);

    /**
     * Gets the object to execute commands manipulating bitmap data structures.
     *
     * @return the object to manipulate bitmap data structures.
     */
    default ReactiveBitMapCommands<String> bitmap() {
        return bitmap(String.class);
    }

    /**
     * Gets the object to execute commands manipulating streams.
     *
     * @param redisKeyType the class of the keys
     * @param fieldType the class of the fields included in the message exchanged on the streams
     * @param valueType the class of the values included in the message exchanged on the streams
     * @param <K> the type of the redis key
     * @param <F> the type of the fields (map's keys)
     * @param <V> the type of the value
     * @return the object to execute commands manipulating streams.
     */
    <K, F, V> ReactiveStreamCommands<K, F, V> stream(Class<K> redisKeyType, Class<F> fieldType, Class<V> valueType);

    /**
     * Gets the object to execute commands manipulating streams.
     *
     * @param redisKeyType the class of the keys
     * @param fieldType the class of the fields included in the message exchanged on the streams
     * @param valueType the class of the values included in the message exchanged on the streams
     * @param <K> the type of the redis key
     * @param <F> the type of the fields (map's keys)
     * @param <V> the type of the value
     * @return the object to execute commands manipulating streams.
     */
    <K, F, V> ReactiveStreamCommands<K, F, V> stream(TypeReference<K> redisKeyType, TypeReference<F> fieldType,
            TypeReference<V> valueType);

    /**
     * Gets the object to execute commands manipulating streams, using a string key, and string fields.
     *
     * @param <V> the type of the value
     * @return the object to execute commands manipulating streams.
     */
    default <V> ReactiveStreamCommands<String, String, V> stream(Class<V> typeOfValue) {
        return stream(String.class, String.class, typeOfValue);
    }

    /**
     * Gets the object to execute commands manipulating streams, using a string key, and string fields.
     *
     * @param <V> the type of the value
     * @return the object to execute commands manipulating streams.
     */
    default <V> ReactiveStreamCommands<String, String, V> stream(TypeReference<V> typeOfValue) {
        return stream(STRING_TYPE_REFERENCE, STRING_TYPE_REFERENCE, typeOfValue);
    }

    /**
     * Gets the object to publish and receive messages.
     *
     * @param messageType the type of message
     * @param <V> the type of message
     * @return the object to publish and subscribe to Redis channels
     */
    <V> ReactivePubSubCommands<V> pubsub(Class<V> messageType);

    /**
     * Gets the object to publish and receive messages.
     *
     * @param messageType the type of message
     * @param <V> the type of message
     * @return the object to publish and subscribe to Redis channels
     */
    <V> ReactivePubSubCommands<V> pubsub(TypeReference<V> messageType);

    /**
     * Gets the object to manipulate JSON values.
     * This group requires the <a href="https://redis.io/docs/stack/json/">RedisJSON module</a>.
     *
     * @return the object to manipulate JSON values.
     */
    default ReactiveJsonCommands<String> json() {
        return json(String.class);
    }

    /**
     * Gets the object to manipulate JSON values.
     * This group requires the <a href="https://redis.io/docs/stack/json/">RedisJSON module</a>.
     *
     * @param <K> the type of keys
     * @return the object to manipulate JSON values.
     */
    <K> ReactiveJsonCommands<K> json(Class<K> redisKeyType);

    /**
     * Gets the object to manipulate JSON values.
     * This group requires the <a href="https://redis.io/docs/stack/json/">RedisJSON module</a>.
     *
     * @param <K> the type of keys
     * @return the object to manipulate JSON values.
     */
    <K> ReactiveJsonCommands<K> json(TypeReference<K> redisKeyType);

    /**
     * Gets the object to manipulate Bloom filters.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a>.
     *
     * @param <V> the type of the values added into the Bloom filter
     * @return the object to manipulate bloom values.
     */
    default <V> ReactiveBloomCommands<String, V> bloom(Class<V> valueType) {
        return bloom(String.class, valueType);
    }

    /**
     * Gets the object to manipulate Bloom filters.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a>.
     *
     * @param <V> the type of the values added into the Bloom filter
     * @return the object to manipulate bloom values.
     */
    default <V> ReactiveBloomCommands<String, V> bloom(TypeReference<V> valueType) {
        return bloom(STRING_TYPE_REFERENCE, valueType);
    }

    /**
     * Gets the object to manipulate Bloom filters.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a>.
     *
     * @param <K> the type of keys
     * @param <V> the type of the values added into the Bloom filter
     * @return the object to manipulate bloom values.
     */
    <K, V> ReactiveBloomCommands<K, V> bloom(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to manipulate Bloom filters.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a>.
     *
     * @param <K> the type of keys
     * @param <V> the type of the values added into the Bloom filter
     * @return the object to manipulate bloom values.
     */
    <K, V> ReactiveBloomCommands<K, V> bloom(TypeReference<K> redisKeyType, TypeReference<V> valueType);

    /**
     * Gets the object to manipulate Cuckoo filters.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the Cuckoo
     * filter support).
     *
     * @param <V> the type of the values added into the Cuckoo filter
     * @return the object to manipulate Cuckoo values.
     */
    default <V> ReactiveCuckooCommands<String, V> cuckoo(Class<V> valueType) {
        return cuckoo(String.class, valueType);
    }

    /**
     * Gets the object to manipulate Cuckoo filters.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the Cuckoo
     * filter support).
     *
     * @param <V> the type of the values added into the Cuckoo filter
     * @return the object to manipulate Cuckoo values.
     */
    default <V> ReactiveCuckooCommands<String, V> cuckoo(TypeReference<V> valueType) {
        return cuckoo(STRING_TYPE_REFERENCE, valueType);
    }

    /**
     * Gets the object to manipulate Cuckoo filters.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the Cuckoo
     * filter support).
     *
     * @param <K> the type of keys
     * @param <V> the type of the values added into the Cuckoo filter
     * @return the object to manipulate Cuckoo values.
     */
    <K, V> ReactiveCuckooCommands<K, V> cuckoo(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to manipulate Cuckoo filters.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the Cuckoo
     * filter support).
     *
     * @param <K> the type of keys
     * @param <V> the type of the values added into the Cuckoo filter
     * @return the object to manipulate Cuckoo values.
     */
    <K, V> ReactiveCuckooCommands<K, V> cuckoo(TypeReference<K> redisKeyType, TypeReference<V> valueType);

    /**
     * Gets the object to manipulate Count-Min sketches.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the count-min
     * sketches support).
     *
     * @param <V> the type of the values added into the count-min sketches
     * @return the object to manipulate count-min sketches.
     */
    default <V> ReactiveCountMinCommands<String, V> countmin(Class<V> valueType) {
        return countmin(String.class, valueType);
    }

    /**
     * Gets the object to manipulate Count-Min sketches.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the count-min
     * sketches support).
     *
     * @param <V> the type of the values added into the count-min sketches
     * @return the object to manipulate count-min sketches.
     */
    default <V> ReactiveCountMinCommands<String, V> countmin(TypeReference<V> valueType) {
        return countmin(STRING_TYPE_REFERENCE, valueType);
    }

    /**
     * Gets the object to manipulate Count-Min sketches.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the count-min
     * sketches support).
     *
     * @param <K> the type of keys
     * @param <V> the type of the values added into the count-min sketches
     * @return the object to manipulate count-min sketches.
     */
    <K, V> ReactiveCountMinCommands<K, V> countmin(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to manipulate Count-Min sketches.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the count-min
     * sketches support).
     *
     * @param <K> the type of keys
     * @param <V> the type of the values added into the count-min sketches
     * @return the object to manipulate count-min sketches.
     */
    <K, V> ReactiveCountMinCommands<K, V> countmin(TypeReference<K> redisKeyType, TypeReference<V> valueType);

    /**
     * Gets the object to manipulate Top-K list.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the top-k
     * list support).
     *
     * @param <V> the type of the values added into the top-k lists
     * @return the object to manipulate top-k lists.
     */
    default <V> ReactiveTopKCommands<String, V> topk(Class<V> valueType) {
        return topk(String.class, valueType);
    }

    /**
     * Gets the object to manipulate Top-K list.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the top-k
     * list support).
     *
     * @param <V> the type of the values added into the top-k lists
     * @return the object to manipulate top-k lists.
     */
    default <V> ReactiveTopKCommands<String, V> topk(TypeReference<V> valueType) {
        return topk(STRING_TYPE_REFERENCE, valueType);
    }

    /**
     * Gets the object to manipulate Top-K list.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the top-k
     * list support).
     *
     * @param <K> the type of keys
     * @param <V> the type of the values added into the top-k lists
     * @return the object to manipulate top-k lists.
     */
    <K, V> ReactiveTopKCommands<K, V> topk(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to manipulate Top-K list.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the top-k
     * list support).
     *
     * @param <K> the type of keys
     * @param <V> the type of the values added into the top-k lists
     * @return the object to manipulate top-k lists.
     */
    <K, V> ReactiveTopKCommands<K, V> topk(TypeReference<K> redisKeyType, TypeReference<V> valueType);

    /**
     * Gets the object to manipulate graphs.
     * This group requires the <a href="https://redis.io/docs/stack/graph/">RedisGraph module</a>.
     *
     * @return the object to manipulate graphs lists.
     */
    @Experimental("The Redis graph support is experimental")
    default ReactiveGraphCommands<String> graph() {
        return graph(String.class);
    }

    /**
     * Gets the object to manipulate graphs.
     * This group requires the <a href="https://redis.io/docs/stack/graph/">RedisGraph module</a>.
     *
     * @param <K> the type of keys
     * @return the object to manipulate graphs lists.
     */
    @Experimental("The Redis graph support is experimental")
    <K> ReactiveGraphCommands<K> graph(Class<K> redisKeyType);

    /**
     * Gets the object to emit commands from the {@code search} group.
     * This group requires the <a href="https://redis.io/docs/stack/search/">RedisSearch module</a>.
     *
     * @param <K> the type of keys
     * @return the object to search documents
     * @deprecated Use the variant without parameter, as the index name must be a string.
     */
    @Experimental("The Redis search support is experimental")
    @Deprecated
    <K> ReactiveSearchCommands<K> search(Class<K> redisKeyType);

    /**
     * Gets the object to emit commands from the {@code search} group.
     * This group requires the <a href="https://redis.io/docs/stack/search/">RedisSearch module</a>.
     *
     * @return the object to search documents
     */
    @Experimental("The Redis Search support is experimental")
    default ReactiveSearchCommands<String> search() {
        return search(String.class);
    }

    /**
     * Gets the object to emit commands from the {@code auto-suggest} group.
     * This group requires the <a href="https://redis.io/docs/stack/search/">RedisSearch module</a>.
     *
     * @param <K> the type of keys
     * @return the object to get suggestions
     */
    @Experimental("The Redis auto-suggest support is experimental")
    <K> ReactiveAutoSuggestCommands<K> autosuggest(Class<K> redisKeyType);

    /**
     * Gets the object to emit commands from the {@code auto-suggest} group.
     * This group requires the <a href="https://redis.io/docs/stack/search/">RedisSearch module</a>.
     *
     * @param <K> the type of keys
     * @return the object to get suggestions
     */
    @Experimental("The Redis auto-suggest support is experimental")
    <K> ReactiveAutoSuggestCommands<K> autosuggest(TypeReference<K> redisKeyType);

    /**
     * Gets the object to emit commands from the {@code auto-suggest} group.
     * This group requires the <a href="https://redis.io/docs/stack/search/">RedisSearch module</a>.
     *
     * @return the object to get suggestions
     */
    @Experimental("The Redis auto-suggest support is experimental")
    default ReactiveAutoSuggestCommands<String> autosuggest() {
        return autosuggest(String.class);
    }

    /**
     * Gets the object to emit commands from the {@code time series} group.
     * This group requires the <a href="https://redis.io/docs/stack/timeseries/">Redis Time Series module</a>.
     *
     * @param <K> the type of keys
     * @return the object to manipulate time series
     */
    @Experimental("The Redis time series support is experimental")
    <K> ReactiveTimeSeriesCommands<K> timeseries(Class<K> redisKeyType);

    /**
     * Gets the object to emit commands from the {@code time series} group.
     * This group requires the <a href="https://redis.io/docs/stack/timeseries/">Redis Time Series module</a>.
     *
     * @param <K> the type of keys
     * @return the object to manipulate time series
     */
    @Experimental("The Redis time series support is experimental")
    <K> ReactiveTimeSeriesCommands<K> timeseries(TypeReference<K> redisKeyType);

    /**
     * Gets the object to emit commands from the {@code time series} group.
     * This group requires the <a href="https://redis.io/docs/stack/timeseries/">Redis Time Series module</a>.
     *
     * @return the object to manipulate time series
     */
    @Experimental("The Redis time series support is experimental")
    default ReactiveTimeSeriesCommands<String> timeseries() {
        return timeseries(String.class);
    }

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
