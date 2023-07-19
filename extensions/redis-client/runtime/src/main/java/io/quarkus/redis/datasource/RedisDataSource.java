package io.quarkus.redis.datasource;

import static io.quarkus.redis.runtime.datasource.Marshaller.STRING_TYPE_REFERENCE;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.autosuggest.AutoSuggestCommands;
import io.quarkus.redis.datasource.bitmap.BitMapCommands;
import io.quarkus.redis.datasource.bloom.BloomCommands;
import io.quarkus.redis.datasource.countmin.CountMinCommands;
import io.quarkus.redis.datasource.cuckoo.CuckooCommands;
import io.quarkus.redis.datasource.geo.GeoCommands;
import io.quarkus.redis.datasource.graph.GraphCommands;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.hyperloglog.HyperLogLogCommands;
import io.quarkus.redis.datasource.json.JsonCommands;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.search.SearchCommands;
import io.quarkus.redis.datasource.set.SetCommands;
import io.quarkus.redis.datasource.sortedset.SortedSetCommands;
import io.quarkus.redis.datasource.stream.StreamCommands;
import io.quarkus.redis.datasource.string.StringCommands;
import io.quarkus.redis.datasource.timeseries.TimeSeriesCommands;
import io.quarkus.redis.datasource.topk.TopKCommands;
import io.quarkus.redis.datasource.transactions.OptimisticLockingTransactionResult;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.smallrye.common.annotation.Experimental;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

/**
 * Synchronous / Blocking Redis Data Source.
 * <p>
 * This class provides access to various <em>groups of methods</em>. Each method execute a Redis {@code command}.
 * Groups and methods are type-safe. The decomposition follows the Redis API group.
 * <p>
 * NOTE: Not all commands are exposed from this API. This is done on purpose. You can always use the low-level Redis
 * client to execute others commands.
 */
public interface RedisDataSource {

    /**
     * Retrieves a {@link RedisDataSource} using a single connection with the Redis server.
     * The connection is acquired from the pool, and released then the consumer completes.
     *
     * @param consumer the consumer receiving the connection and returning when the connection can be released.
     */
    void withConnection(Consumer<RedisDataSource> consumer);

    /**
     * Retrieves a {@link RedisDataSource} enqueuing commands in a Redis Transaction ({@code MULTI}).
     * Note that transaction acquires a single connection, and all the commands are enqueued in this connection.
     * The commands are only executed when the passed block completes.
     * <p>
     * The results of the commands are retrieved using the returned {@link TransactionResult}.
     * <p>
     * The user can discard a transaction using the {@link TransactionalRedisDataSource#discard()} method.
     * In this case, the produced {@link TransactionResult} will be empty.
     *
     * @param tx the consumer receiving the transactional redis data source. The enqueued commands are only executed
     *        at the end of the block.
     */
    TransactionResult withTransaction(Consumer<TransactionalRedisDataSource> tx);

    /**
     * Retrieves a {@link RedisDataSource} enqueuing commands in a Redis Transaction ({@code MULTI}).
     * Note that transaction acquires a single connection, and all the commands are enqueued in this connection.
     * The commands are only executed when the passed block completes.
     * <p>
     * The results of the commands are retrieved using the returned {@link TransactionResult}.
     * <p>
     * The user can discard a transaction using the {@link TransactionalRedisDataSource#discard()} method.
     * In this case, the produced {@link TransactionResult} will be empty.
     *
     * @param tx the consumer receiving the transactional redis data source. The enqueued commands are only executed
     *        at the end of the block.
     * @param watchedKeys the keys to watch during the execution of the transaction. If one of these key is modified before
     *        the completion of the transaction, the transaction is discarded.
     */
    TransactionResult withTransaction(Consumer<TransactionalRedisDataSource> tx, String... watchedKeys);

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
     * The {@code preTxBlock} returns a {@link I}. The produced value is received by the {@code tx} block,
     * which can use that value to execute the appropriate operation in the transaction. The produced value can also be
     * retrieved from the produced {@link OptimisticLockingTransactionResult}. Commands issued in the {@code preTxBlock }
     * must used the passed (single-connection) {@link RedisDataSource} instance.
     * <p>
     * If the {@code preTxBlock} throws an exception, the transaction is not executed, and the returned
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
    <I> OptimisticLockingTransactionResult<I> withTransaction(Function<RedisDataSource, I> preTxBlock,
            BiConsumer<I, TransactionalRedisDataSource> tx,
            String... watchedKeys);

    /**
     * Execute the command <a href="https://redis.io/commands/select">SELECT</a>.
     * Summary: Change the selected database for the current connection
     * Group: connection
     * Requires Redis 1.0.0
     * <p>
     * This method is expected to be used inside a {@link #withConnection(Consumer)} block.
     *
     * @param index the database index.
     **/
    void select(long index);

    /**
     * Execute the command <a href="https://redis.io/commands/flushall">FLUSHALL</a>.
     * Summary: Remove all keys from all databases
     * Group: server
     * Requires Redis 1.0.0
     **/
    void flushall();

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
    <K, F, V> HashCommands<K, F, V> hash(Class<K> redisKeyType, Class<F> typeOfField, Class<V> typeOfValue);

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
    <K, F, V> HashCommands<K, F, V> hash(TypeReference<K> redisKeyType, TypeReference<F> typeOfField,
            TypeReference<V> typeOfValue);

    /**
     * Gets the object to execute commands manipulating hashes (a.k.a. {@code Map&lt;String, V&gt;}).
     * <p>
     * This is a shortcut on {@code hash(String.class, String.class, V)}
     *
     * @param typeOfValue the class of the values
     * @param <V> the type of the value
     * @return the object to execute commands manipulating hashes (a.k.a. {@code Map&lt;String, V&gt;}).
     */
    default <V> HashCommands<String, String, V> hash(Class<V> typeOfValue) {
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
    default <V> HashCommands<String, String, V> hash(TypeReference<V> typeOfValue) {
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
    <K, V> GeoCommands<K, V> geo(Class<K> redisKeyType, Class<V> memberType);

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
    <K, V> GeoCommands<K, V> geo(TypeReference<K> redisKeyType, TypeReference<V> memberType);

    /**
     * Gets the object to execute commands manipulating geo items (a.k.a. {@code {longitude, latitude, member}}).
     * <p>
     * {@code V} represents the type of the member, i.e. the localized <em>thing</em>.
     *
     * @param memberType the class of the members
     * @param <V> the type of the member
     * @return the object to execute geo commands.
     */
    default <V> GeoCommands<String, V> geo(Class<V> memberType) {
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
    default <V> GeoCommands<String, V> geo(TypeReference<V> memberType) {
        return geo(STRING_TYPE_REFERENCE, memberType);
    }

    /**
     * Gets the object to execute commands manipulating keys and expiration times.
     *
     * @param redisKeyType the type of the keys
     * @param <K> the type of the key
     * @return the object to execute commands manipulating keys.
     */
    <K> KeyCommands<K> key(Class<K> redisKeyType);

    /**
     * Gets the object to execute commands manipulating keys and expiration times.
     *
     * @param redisKeyType the type of the keys
     * @param <K> the type of the key
     * @return the object to execute commands manipulating keys.
     */
    <K> KeyCommands<K> key(TypeReference<K> redisKeyType);

    /**
     * Gets the object to execute commands manipulating keys and expiration times.
     *
     * @return the object to execute commands manipulating keys.
     */
    default KeyCommands<String> key() {
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
    <K, V> SortedSetCommands<K, V> sortedSet(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to execute commands manipulating sorted sets.
     *
     * @param redisKeyType the type of the keys
     * @param valueType the type of the value sorted in the sorted sets
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @return the object to manipulate sorted sets.
     */
    <K, V> SortedSetCommands<K, V> sortedSet(TypeReference<K> redisKeyType, TypeReference<V> valueType);

    /**
     * Gets the object to execute commands manipulating sorted sets.
     *
     * @param valueType the type of the value sorted in the sorted sets
     * @param <V> the type of the value
     * @return the object to manipulate sorted sets.
     */
    default <V> SortedSetCommands<String, V> sortedSet(Class<V> valueType) {
        return sortedSet(String.class, valueType);
    }

    /**
     * Gets the object to execute commands manipulating sorted sets.
     *
     * @param valueType the type of the value sorted in the sorted sets
     * @param <V> the type of the value
     * @return the object to manipulate sorted sets.
     */
    default <V> SortedSetCommands<String, V> sortedSet(TypeReference<V> valueType) {
        return sortedSet(STRING_TYPE_REFERENCE, valueType);
    }

    /**
     * Gets the object to execute commands manipulating stored strings.
     *
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
    <K, V> ValueCommands<K, V> value(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to execute commands manipulating stored strings.
     *
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
    <K, V> ValueCommands<K, V> value(TypeReference<K> redisKeyType, TypeReference<V> valueType);

    /**
     * Gets the object to execute commands manipulating stored strings.
     *
     * <p>
     * <strong>NOTE:</strong> Instead of {@code string}, this group is named {@code value} to avoid the confusion with the
     * Java String type. Indeed, Redis strings can be strings, numbers, byte arrays...
     *
     * @param valueType the type of the value, often String, or the value are encoded/decoded using codecs.
     * @param <V> the type of the value
     * @return the object to manipulate stored strings.
     */
    default <V> ValueCommands<String, V> value(Class<V> valueType) {
        return value(String.class, valueType);
    }

    /**
     * Gets the object to execute commands manipulating stored strings.
     *
     * <p>
     * <strong>NOTE:</strong> Instead of {@code string}, this group is named {@code value} to avoid the confusion with the
     * Java String type. Indeed, Redis strings can be strings, numbers, byte arrays...
     *
     * @param valueType the type of the value, often String, or the value are encoded/decoded using codecs.
     * @param <V> the type of the value
     * @return the object to manipulate stored strings.
     */
    default <V> ValueCommands<String, V> value(TypeReference<V> valueType) {
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
    <K, V> StringCommands<K, V> string(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to execute commands manipulating stored strings.
     *
     * @param valueType the type of the value, often String, or the value are encoded/decoded using codecs.
     * @param <V> the type of the value
     * @return the object to manipulate stored strings.
     * @deprecated Use {@link #value(Class)} instead
     */
    @Deprecated
    default <V> StringCommands<String, V> string(Class<V> valueType) {
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
    <K, V> SetCommands<K, V> set(Class<K> redisKeyType, Class<V> memberType);

    /**
     * Gets the object to execute commands manipulating sets.
     *
     * @param redisKeyType the type of the keys
     * @param memberType the type of the member stored in each set
     * @param <K> the type of the key
     * @param <V> the type of the member
     * @return the object to manipulate sets.
     */
    <K, V> SetCommands<K, V> set(TypeReference<K> redisKeyType, TypeReference<V> memberType);

    /**
     * Gets the object to execute commands manipulating sets.
     *
     * @param memberType the type of the member stored in each set
     * @param <V> the type of the member
     * @return the object to manipulate sets.
     */
    default <V> SetCommands<String, V> set(Class<V> memberType) {
        return set(String.class, memberType);
    }

    /**
     * Gets the object to execute commands manipulating sets.
     *
     * @param memberType the type of the member stored in each set
     * @param <V> the type of the member
     * @return the object to manipulate sets.
     */
    default <V> SetCommands<String, V> set(TypeReference<V> memberType) {
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
    <K, V> ListCommands<K, V> list(Class<K> redisKeyType, Class<V> memberType);

    /**
     * Gets the object to execute commands manipulating lists.
     *
     * @param memberType the type of the member stored in each list
     * @param <V> the type of the member
     * @return the object to manipulate sets.
     */
    default <V> ListCommands<String, V> list(Class<V> memberType) {
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
    <K, V> ListCommands<K, V> list(TypeReference<K> redisKeyType, TypeReference<V> memberType);

    /**
     * Gets the object to execute commands manipulating lists.
     *
     * @param memberType the type of the member stored in each list
     * @param <V> the type of the member
     * @return the object to manipulate sets.
     */
    default <V> ListCommands<String, V> list(TypeReference<V> memberType) {
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
    <K, V> HyperLogLogCommands<K, V> hyperloglog(Class<K> redisKeyType, Class<V> memberType);

    /**
     * Gets the object to execute commands manipulating hyperloglog data structures.
     *
     * @param redisKeyType the type of the keys
     * @param memberType the type of the member stored in the data structure
     * @param <K> the type of the key
     * @param <V> the type of the member
     * @return the object to manipulate hyper log log data structures.
     */
    <K, V> HyperLogLogCommands<K, V> hyperloglog(TypeReference<K> redisKeyType, TypeReference<V> memberType);

    /**
     * Gets the object to execute commands manipulating hyperloglog data structures.
     *
     * @param memberType the type of the member stored in the data structure
     * @param <V> the type of the member
     * @return the object to manipulate hyper log log data structures.
     */
    default <V> HyperLogLogCommands<String, V> hyperloglog(Class<V> memberType) {
        return hyperloglog(String.class, memberType);
    }

    /**
     * Gets the object to execute commands manipulating hyperloglog data structures.
     *
     * @param memberType the type of the member stored in the data structure
     * @param <V> the type of the member
     * @return the object to manipulate hyper log log data structures.
     */
    default <V> HyperLogLogCommands<String, V> hyperloglog(TypeReference<V> memberType) {
        return hyperloglog(STRING_TYPE_REFERENCE, memberType);
    }

    /**
     * Gets the object to execute commands manipulating bitmap data structures.
     *
     * @param redisKeyType the type of the keys
     * @param <K> the type of the key
     * @return the object to manipulate bitmap data structures.
     */
    <K> BitMapCommands<K> bitmap(Class<K> redisKeyType);

    /**
     * Gets the object to execute commands manipulating bitmap data structures.
     *
     * @param redisKeyType the type of the keys
     * @param <K> the type of the key
     * @return the object to manipulate bitmap data structures.
     */
    <K> BitMapCommands<K> bitmap(TypeReference<K> redisKeyType);

    /**
     * Gets the object to execute commands manipulating bitmap data structures.
     *
     * @return the object to manipulate bitmap data structures.
     */
    default BitMapCommands<String> bitmap() {
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
    <K, F, V> StreamCommands<K, F, V> stream(Class<K> redisKeyType, Class<F> fieldType, Class<V> valueType);

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
    <K, F, V> StreamCommands<K, F, V> stream(TypeReference<K> redisKeyType, TypeReference<F> fieldType,
            TypeReference<V> valueType);

    /**
     * Gets the object to execute commands manipulating streams, using a string key, and string fields.
     *
     * @param <V> the type of the value
     * @return the object to execute commands manipulating streams.
     */
    default <V> StreamCommands<String, String, V> stream(Class<V> typeOfValue) {
        return stream(String.class, String.class, typeOfValue);
    }

    /**
     * Gets the object to execute commands manipulating streams, using a string key, and string fields.
     *
     * @param <V> the type of the value
     * @return the object to execute commands manipulating streams.
     */
    default <V> StreamCommands<String, String, V> stream(TypeReference<V> typeOfValue) {
        return stream(STRING_TYPE_REFERENCE, STRING_TYPE_REFERENCE, typeOfValue);
    }

    /**
     * Gets the object to manipulate JSON values.
     * This group requires the <a href="https://redis.io/docs/stack/json/">RedisJSON module</a>.
     *
     * @return the object to manipulate JSON values.
     */
    default JsonCommands<String> json() {
        return json(String.class);
    }

    /**
     * Gets the object to manipulate JSON values.
     * This group requires the <a href="https://redis.io/docs/stack/json/">RedisJSON module</a>.
     *
     * @param <K> the type of keys
     * @return the object to manipulate JSON values.
     */
    <K> JsonCommands<K> json(Class<K> redisKeyType);

    /**
     * Gets the object to manipulate JSON values.
     * This group requires the <a href="https://redis.io/docs/stack/json/">RedisJSON module</a>.
     *
     * @param <K> the type of keys
     * @return the object to manipulate JSON values.
     */
    <K> JsonCommands<K> json(TypeReference<K> redisKeyType);

    /**
     * Gets the object to manipulate Bloom filters.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a>.
     *
     * @param <V> the type of the values added into the Bloom filter
     * @return the object to manipulate bloom filters.
     */
    default <V> BloomCommands<String, V> bloom(Class<V> valueType) {
        return bloom(String.class, valueType);
    }

    /**
     * Gets the object to manipulate Bloom filters.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a>.
     *
     * @param <V> the type of the values added into the Bloom filter
     * @return the object to manipulate bloom filters.
     */
    default <V> BloomCommands<String, V> bloom(TypeReference<V> valueType) {
        return bloom(STRING_TYPE_REFERENCE, valueType);
    }

    /**
     * Gets the object to manipulate Bloom filters.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a>.
     *
     * @param <K> the type of keys
     * @param <V> the type of the values added into the Bloom filter
     * @return the object to manipulate bloom filters.
     */
    <K, V> BloomCommands<K, V> bloom(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to manipulate Bloom filters.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a>.
     *
     * @param <K> the type of keys
     * @param <V> the type of the values added into the Bloom filter
     * @return the object to manipulate bloom filters.
     */
    <K, V> BloomCommands<K, V> bloom(TypeReference<K> redisKeyType, TypeReference<V> valueType);

    /**
     * Gets the object to manipulate Cuckoo filters.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the Cuckoo
     * filter support).
     *
     * @param <V> the type of the values added into the Cuckoo filter
     * @return the object to manipulate Cuckoo filters.
     */
    default <V> CuckooCommands<String, V> cuckoo(Class<V> valueType) {
        return cuckoo(String.class, valueType);
    }

    /**
     * Gets the object to manipulate Cuckoo filters.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the Cuckoo
     * filter support).
     *
     * @param <V> the type of the values added into the Cuckoo filter
     * @return the object to manipulate Cuckoo filters.
     */
    default <V> CuckooCommands<String, V> cuckoo(TypeReference<V> valueType) {
        return cuckoo(STRING_TYPE_REFERENCE, valueType);
    }

    /**
     * Gets the object to manipulate Cuckoo filters.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the Cuckoo
     * filter support).
     *
     * @param <K> the type of keys
     * @param <V> the type of the values added into the Cuckoo filter
     * @return the object to manipulate Cuckoo filters.
     */
    <K, V> CuckooCommands<K, V> cuckoo(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to manipulate Cuckoo filters.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the Cuckoo
     * filter support).
     *
     * @param <K> the type of keys
     * @param <V> the type of the values added into the Cuckoo filter
     * @return the object to manipulate Cuckoo filters.
     */
    <K, V> CuckooCommands<K, V> cuckoo(TypeReference<K> redisKeyType, TypeReference<V> valueType);

    /**
     * Gets the object to manipulate Count-Min sketches.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the count-min
     * sketches support).
     *
     * @param <V> the type of the values added into the count-min sketches
     * @return the object to manipulate count-min sketches.
     */
    default <V> CountMinCommands<String, V> countmin(Class<V> valueType) {
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
    default <V> CountMinCommands<String, V> countmin(TypeReference<V> valueType) {
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
    <K, V> CountMinCommands<K, V> countmin(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to manipulate Count-Min sketches.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the count-min
     * sketches support).
     *
     * @param <K> the type of keys
     * @param <V> the type of the values added into the count-min sketches
     * @return the object to manipulate count-min sketches.
     */
    <K, V> CountMinCommands<K, V> countmin(TypeReference<K> redisKeyType, TypeReference<V> valueType);

    /**
     * Gets the object to manipulate Top-K list.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the top-k
     * list support).
     *
     * @param <V> the type of the values added into the top-k lists
     * @return the object to manipulate top-k lists.
     */
    default <V> TopKCommands<String, V> topk(Class<V> valueType) {
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
    default <V> TopKCommands<String, V> topk(TypeReference<V> valueType) {
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
    <K, V> TopKCommands<K, V> topk(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to manipulate Top-K list.
     * This group requires the <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the top-k
     * list support).
     *
     * @param <K> the type of keys
     * @param <V> the type of the values added into the top-k lists
     * @return the object to manipulate top-k lists.
     */
    <K, V> TopKCommands<K, V> topk(TypeReference<K> redisKeyType, TypeReference<V> valueType);

    /**
     * Gets the object to manipulate graphs.
     * This group requires the <a href="https://redis.io/docs/stack/graph/">RedisGraph module</a>.
     *
     * @return the object to manipulate graphs.
     */
    @Experimental("The Redis graph support is experimental, in addition, the graph module EOL")
    default GraphCommands<String> graph() {
        return graph(String.class);
    }

    /**
     * Gets the object to manipulate graphs.
     * This group requires the <a href="https://redis.io/docs/stack/graph/">RedisGraph module</a>.
     *
     * @param <K> the type of keys
     * @return the object to manipulate graphs lists.
     */
    @Experimental("The Redis graph support is experimental, in addition, the graph module EOL")
    <K> GraphCommands<K> graph(Class<K> redisKeyType);

    /**
     * Gets the object to emit commands from the {@code search} group.
     * This group requires the <a href="https://redis.io/docs/stack/search/">RedisSearch module</a>.
     *
     * @param <K> the type of keys
     * @return the object to search documents
     * @deprecated Use the variant without parameter, as the index name must be a string
     */
    @Experimental("The Redis search support is experimental")
    @Deprecated
    <K> SearchCommands<K> search(Class<K> redisKeyType);

    /**
     * Gets the object to emit commands from the {@code search} group.
     * This group requires the <a href="https://redis.io/docs/stack/search/">RedisSearch module</a>.
     *
     * @return the object to search documents
     */
    @Experimental("The Redis Search support is experimental")
    default SearchCommands<String> search() {
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
    <K> AutoSuggestCommands<K> autosuggest(Class<K> redisKeyType);

    /**
     * Gets the object to emit commands from the {@code auto-suggest} group.
     * This group requires the <a href="https://redis.io/docs/stack/search/">RedisSearch module</a>.
     *
     * @param <K> the type of keys
     * @return the object to get suggestions
     */
    @Experimental("The Redis auto-suggest support is experimental")
    <K> AutoSuggestCommands<K> autosuggest(TypeReference<K> redisKeyType);

    /**
     * Gets the object to emit commands from the {@code auto-suggest} group.
     * This group requires the <a href="https://redis.io/docs/stack/search/">RedisSearch module</a>.
     *
     * @return the object to get suggestions
     */
    @Experimental("The Redis auto-suggest support is experimental")
    default AutoSuggestCommands<String> autosuggest() {
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
    <K> TimeSeriesCommands<K> timeseries(Class<K> redisKeyType);

    /**
     * Gets the object to emit commands from the {@code time series} group.
     * This group requires the <a href="https://redis.io/docs/stack/timeseries/">Redis Time Series module</a>.
     *
     * @param <K> the type of keys
     * @return the object to manipulate time series
     */
    @Experimental("The Redis time series support is experimental")
    <K> TimeSeriesCommands<K> timeseries(TypeReference<K> redisKeyType);

    /**
     * Gets the object to emit commands from the {@code time series} group.
     * This group requires the <a href="https://redis.io/docs/stack/timeseries/">Redis Time Series module</a>.
     *
     * @return the object to manipulate time series
     */
    @Experimental("The Redis time series support is experimental")
    default TimeSeriesCommands<String> timeseries() {
        return timeseries(String.class);
    }

    /**
     * Gets the objects to publish and receive messages.
     *
     * @param messageType the type of message
     * @param <V> the type of message
     * @return the object to publish and subscribe to Redis channels
     */
    <V> PubSubCommands<V> pubsub(Class<V> messageType);

    /**
     * Gets the objects to publish and receive messages.
     *
     * @param messageType the type of message
     * @param <V> the type of message
     * @return the object to publish and subscribe to Redis channels
     */
    <V> PubSubCommands<V> pubsub(TypeReference<V> messageType);

    /**
     * Executes a command.
     * This method is used to execute commands not offered by the API.
     *
     * @param command the command name
     * @param args the parameters, encoded as String.
     * @return the response
     */
    Response execute(String command, String... args);

    /**
     * Executes a command.
     * This method is used to execute commands not offered by the API.
     *
     * @param command the command
     * @param args the parameters, encoded as String.
     * @return the response
     */
    Response execute(Command command, String... args);

    /**
     * Executes a command.
     * This method is used to execute commands not offered by the API.
     *
     * @param command the command
     * @param args the parameters, encoded as String.
     * @return the response
     */
    Response execute(io.vertx.redis.client.Command command, String... args);

    /**
     * @return the reactive data source.
     */
    ReactiveRedisDataSource getReactive();

}
