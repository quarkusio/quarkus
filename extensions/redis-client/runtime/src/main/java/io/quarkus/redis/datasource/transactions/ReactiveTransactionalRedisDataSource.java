package io.quarkus.redis.datasource.transactions;

import io.quarkus.redis.datasource.autosuggest.ReactiveTransactionalAutoSuggestCommands;
import io.quarkus.redis.datasource.bitmap.ReactiveTransactionalBitMapCommands;
import io.quarkus.redis.datasource.bloom.ReactiveTransactionalBloomCommands;
import io.quarkus.redis.datasource.countmin.ReactiveTransactionalCountMinCommands;
import io.quarkus.redis.datasource.cuckoo.ReactiveTransactionalCuckooCommands;
import io.quarkus.redis.datasource.geo.ReactiveTransactionalGeoCommands;
import io.quarkus.redis.datasource.graph.ReactiveTransactionalGraphCommands;
import io.quarkus.redis.datasource.hash.ReactiveTransactionalHashCommands;
import io.quarkus.redis.datasource.hyperloglog.ReactiveTransactionalHyperLogLogCommands;
import io.quarkus.redis.datasource.json.ReactiveTransactionalJsonCommands;
import io.quarkus.redis.datasource.keys.ReactiveTransactionalKeyCommands;
import io.quarkus.redis.datasource.list.ReactiveTransactionalListCommands;
import io.quarkus.redis.datasource.search.ReactiveTransactionalSearchCommands;
import io.quarkus.redis.datasource.set.ReactiveTransactionalSetCommands;
import io.quarkus.redis.datasource.sortedset.ReactiveTransactionalSortedSetCommands;
import io.quarkus.redis.datasource.stream.ReactiveTransactionalStreamCommands;
import io.quarkus.redis.datasource.string.ReactiveTransactionalStringCommands;
import io.quarkus.redis.datasource.timeseries.ReactiveTransactionalTimeSeriesCommands;
import io.quarkus.redis.datasource.topk.ReactiveTransactionalTopKCommands;
import io.quarkus.redis.datasource.value.ReactiveTransactionalValueCommands;
import io.smallrye.common.annotation.Experimental;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;

/**
 * Redis Data Source object used to execute commands in a Redis transaction ({@code MULTI}). Note that the results of
 * the enqueued commands are not available until the completion of the transaction.
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
     * {@code hash(String.class, String.class, Person.class)}. If you want to use a hash of
     * {@code &lt;String -> Person&gt;} stored using UUID identifier, you would use:
     * {@code hash(UUID.class, String.class, Person.class)}.
     *
     * @param redisKeyType
     *        the class of the keys
     * @param typeOfField
     *        the class of the fields
     * @param typeOfValue
     *        the class of the values
     * @param <K>
     *        the type of the redis key
     * @param <F>
     *        the type of the fields (map's keys)
     * @param <V>
     *        the type of the value
     *
     * @return the object to execute commands manipulating hashes (a.k.a. {@code Map&lt;K, V&gt;}).
     */
    <K, F, V> ReactiveTransactionalHashCommands<K, F, V> hash(Class<K> redisKeyType, Class<F> typeOfField,
            Class<V> typeOfValue);

    /**
     * Gets the object to execute commands manipulating hashes (a.k.a. {@code Map&lt;String, V&gt;}).
     * <p>
     * This is a shortcut on {@code hash(String.class, String.class, V)}
     *
     * @param typeOfValue
     *        the class of the values
     * @param <V>
     *        the type of the value
     *
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
     * @param redisKeyType
     *        the class of the keys
     * @param memberType
     *        the class of the members
     * @param <K>
     *        the type of the redis key
     * @param <V>
     *        the type of the member
     *
     * @return the object to execute geo commands.
     */
    <K, V> ReactiveTransactionalGeoCommands<K, V> geo(Class<K> redisKeyType, Class<V> memberType);

    /**
     * Gets the object to execute commands manipulating geo items (a.k.a. {@code {longitude, latitude, member}}).
     * <p>
     * {@code V} represents the type of the member, i.e. the localized <em>thing</em>.
     *
     * @param memberType
     *        the class of the members
     * @param <V>
     *        the type of the member
     *
     * @return the object to execute geo commands.
     */
    default <V> ReactiveTransactionalGeoCommands<String, V> geo(Class<V> memberType) {
        return geo(String.class, memberType);
    }

    /**
     * Gets the object to execute commands manipulating keys and expiration times.
     *
     * @param redisKeyType
     *        the type of the keys
     * @param <K>
     *        the type of the key
     *
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
     * @param redisKeyType
     *        the type of the keys
     * @param valueType
     *        the type of the value sorted in the sorted sets
     * @param <K>
     *        the type of the key
     * @param <V>
     *        the type of the value
     *
     * @return the object to manipulate sorted sets.
     */
    <K, V> ReactiveTransactionalSortedSetCommands<K, V> sortedSet(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to execute commands manipulating sorted sets.
     *
     * @param valueType
     *        the type of the value sorted in the sorted sets
     * @param <V>
     *        the type of the value
     *
     * @return the object to manipulate sorted sets.
     */
    default <V> ReactiveTransactionalSortedSetCommands<String, V> sortedSet(Class<V> valueType) {
        return sortedSet(String.class, valueType);
    }

    /**
     * Gets the object to execute commands manipulating stored strings.
     * <p>
     * <strong>NOTE:</strong> Instead of {@code string}, this group is named {@code value} to avoid the confusion with
     * the Java String type. Indeed, Redis strings can be strings, numbers, byte arrays...
     *
     * @param redisKeyType
     *        the type of the keys
     * @param valueType
     *        the type of the value, often String, or the value are encoded/decoded using codecs.
     * @param <K>
     *        the type of the key
     * @param <V>
     *        the type of the value
     *
     * @return the object to manipulate stored strings.
     */
    <K, V> ReactiveTransactionalValueCommands<K, V> value(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to execute commands manipulating stored strings.
     * <p>
     * <strong>NOTE:</strong> Instead of {@code string}, this group is named {@code value} to avoid the confusion with
     * the Java String type. Indeed, Redis strings can be strings, numbers, byte arrays...
     *
     * @param valueType
     *        the type of the value, often String, or the value are encoded/decoded using codecs.
     * @param <V>
     *        the type of the value
     *
     * @return the object to manipulate stored strings.
     */
    default <V> ReactiveTransactionalValueCommands<String, V> value(Class<V> valueType) {
        return value(String.class, valueType);
    }

    /**
     * Gets the object to execute commands manipulating stored strings.
     *
     * @param redisKeyType
     *        the type of the keys
     * @param valueType
     *        the type of the value, often String, or the value are encoded/decoded using codecs.
     * @param <K>
     *        the type of the key
     * @param <V>
     *        the type of the value
     *
     * @return the object to manipulate stored strings.
     *
     * @deprecated Use {@link #value(Class, Class)} instead
     */
    @Deprecated
    <K, V> ReactiveTransactionalStringCommands<K, V> string(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to execute commands manipulating stored strings.
     *
     * @param valueType
     *        the type of the value, often String, or the value are encoded/decoded using codecs.
     * @param <V>
     *        the type of the value
     *
     * @return the object to manipulate stored strings.
     *
     * @deprecated Use {@link #value(Class)} instead
     */
    @Deprecated
    default <V> ReactiveTransactionalStringCommands<String, V> string(Class<V> valueType) {
        return string(String.class, valueType);
    }

    /**
     * Gets the object to execute commands manipulating sets.
     *
     * @param redisKeyType
     *        the type of the keys
     * @param memberType
     *        the type of the member stored in each set
     * @param <K>
     *        the type of the key
     * @param <V>
     *        the type of the member
     *
     * @return the object to manipulate sets.
     */
    <K, V> ReactiveTransactionalSetCommands<K, V> set(Class<K> redisKeyType, Class<V> memberType);

    /**
     * Gets the object to execute commands manipulating sets.
     *
     * @param memberType
     *        the type of the member stored in each set
     * @param <V>
     *        the type of the member
     *
     * @return the object to manipulate sets.
     */
    default <V> ReactiveTransactionalSetCommands<String, V> set(Class<V> memberType) {
        return set(String.class, memberType);
    }

    /**
     * Gets the object to execute commands manipulating lists.
     *
     * @param redisKeyType
     *        the type of the keys
     * @param memberType
     *        the type of the member stored in each list
     * @param <K>
     *        the type of the key
     * @param <V>
     *        the type of the member
     *
     * @return the object to manipulate sets.
     */
    <K, V> ReactiveTransactionalListCommands<K, V> list(Class<K> redisKeyType, Class<V> memberType);

    /**
     * Gets the object to execute commands manipulating lists.
     *
     * @param memberType
     *        the type of the member stored in each list
     * @param <V>
     *        the type of the member
     *
     * @return the object to manipulate sets.
     */
    default <V> ReactiveTransactionalListCommands<String, V> list(Class<V> memberType) {
        return list(String.class, memberType);
    }

    /**
     * Gets the object to execute commands manipulating hyperloglog data structures.
     *
     * @param redisKeyType
     *        the type of the keys
     * @param memberType
     *        the type of the member stored in the data structure
     * @param <K>
     *        the type of the key
     * @param <V>
     *        the type of the member
     *
     * @return the object to manipulate hyper log log data structures.
     */
    <K, V> ReactiveTransactionalHyperLogLogCommands<K, V> hyperloglog(Class<K> redisKeyType, Class<V> memberType);

    /**
     * Gets the object to execute commands manipulating hyperloglog data structures.
     *
     * @param memberType
     *        the type of the member stored in the data structure
     * @param <V>
     *        the type of the member
     *
     * @return the object to manipulate hyper log log data structures.
     */
    default <V> ReactiveTransactionalHyperLogLogCommands<String, V> hyperloglog(Class<V> memberType) {
        return hyperloglog(String.class, memberType);
    }

    /**
     * Gets the object to execute commands manipulating bitmap data structures.
     *
     * @param redisKeyType
     *        the type of the keys
     * @param <K>
     *        the type of the key
     *
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
     * Gets the object to execute commands manipulating streams.
     * <p>
     *
     * @param redisKeyType
     *        the class of the keys
     * @param typeOfField
     *        the class of the fields
     * @param typeOfValue
     *        the class of the values
     * @param <K>
     *        the type of the redis key
     * @param <F>
     *        the type of the fields (map's keys)
     * @param <V>
     *        the type of the value
     *
     * @return the object to execute commands manipulating streams.
     */
    <K, F, V> ReactiveTransactionalStreamCommands<K, F, V> stream(Class<K> redisKeyType, Class<F> typeOfField,
            Class<V> typeOfValue);

    /**
     * Gets the object to execute commands manipulating stream..
     * <p>
     * This is a shortcut on {@code stream(String.class, String.class, V)}
     *
     * @param typeOfValue
     *        the class of the values
     * @param <V>
     *        the type of the value
     *
     * @return the object to execute commands manipulating streams.
     */
    default <V> ReactiveTransactionalStreamCommands<String, String, V> stream(Class<V> typeOfValue) {
        return stream(String.class, String.class, typeOfValue);
    }

    /**
     * Gets the object to manipulate JSON values. This group requires the
     * <a href="https://redis.io/docs/stack/json/">RedisJSON module</a>.
     *
     * @return the object to manipulate JSON values.
     */
    default ReactiveTransactionalJsonCommands<String> json() {
        return json(String.class);
    }

    /**
     * Gets the object to manipulate JSON values. This group requires the
     * <a href="https://redis.io/docs/stack/json/">RedisJSON module</a>.
     *
     * @param <K>
     *        the type of keys
     *
     * @return the object to manipulate JSON values.
     */
    <K> ReactiveTransactionalJsonCommands<K> json(Class<K> redisKeyType);

    /**
     * Gets the object to manipulate Bloom filters. This group requires the
     * <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a>.
     *
     * @param valueType
     *        the type of value to store in the filters
     * @param <V>
     *        the type of value
     *
     * @return the object to manipulate Bloom filters
     */
    default <V> ReactiveTransactionalBloomCommands<String, V> bloom(Class<V> valueType) {
        return bloom(String.class, valueType);
    }

    /**
     * Gets the object to manipulate Bloom filters. This group requires the
     * <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a>.
     *
     * @param redisKeyType
     *        the type of the key
     * @param valueType
     *        the type of value to store in the filters
     * @param <K>
     *        the type of key
     * @param <V>
     *        the type of value
     *
     * @return the object to manipulate Bloom filters
     */
    <K, V> ReactiveTransactionalBloomCommands<K, V> bloom(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to manipulate Cuckoo filters. This group requires the
     * <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the Cuckoo filter support).
     *
     * @param <V>
     *        the type of the values added into the Cuckoo filter
     *
     * @return the object to manipulate Cuckoo values.
     */
    default <V> ReactiveTransactionalCuckooCommands<String, V> cuckoo(Class<V> valueType) {
        return cuckoo(String.class, valueType);
    }

    /**
     * Gets the object to manipulate Cuckoo filters. This group requires the
     * <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the Cuckoo filter support).
     *
     * @param <K>
     *        the type of keys
     * @param <V>
     *        the type of the values added into the Cuckoo filter
     *
     * @return the object to manipulate Cuckoo values.
     */
    <K, V> ReactiveTransactionalCuckooCommands<K, V> cuckoo(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to manipulate Count-Min sketches. This group requires the
     * <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the count-min sketches support).
     *
     * @param <V>
     *        the type of the values added into the count-min sketches
     *
     * @return the object to manipulate count-min sketches.
     */
    default <V> ReactiveTransactionalCountMinCommands<String, V> countmin(Class<V> valueType) {
        return countmin(String.class, valueType);
    }

    /**
     * Gets the object to manipulate Count-Min sketches. This group requires the
     * <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the count-min sketches support).
     *
     * @param <K>
     *        the type of keys
     * @param <V>
     *        the type of the values added into the count-min sketches
     *
     * @return the object to manipulate count-min sketches.
     */
    <K, V> ReactiveTransactionalCountMinCommands<K, V> countmin(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to manipulate Top-K list. This group requires the
     * <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the top-k list support).
     *
     * @param <V>
     *        the type of the values added into the top-k lists
     *
     * @return the object to manipulate top-k lists.
     */
    default <V> ReactiveTransactionalTopKCommands<String, V> topk(Class<V> valueType) {
        return topk(String.class, valueType);
    }

    /**
     * Gets the object to manipulate Top-K list. This group requires the
     * <a href="https://redis.io/docs/stack/bloom/">RedisBloom module</a> (including the top-k list support).
     *
     * @param <K>
     *        the type of keys
     * @param <V>
     *        the type of the values added into the top-k lists
     *
     * @return the object to manipulate top-k lists.
     */
    <K, V> ReactiveTransactionalTopKCommands<K, V> topk(Class<K> redisKeyType, Class<V> valueType);

    /**
     * Gets the object to manipulate graphs. This group requires the
     * <a href="https://redis.io/docs/stack/graph/">RedisGraph module</a>.
     *
     * @return the object to manipulate graphs lists.
     */
    @Experimental("The Redis graph support is experimental")
    default ReactiveTransactionalGraphCommands<String> graph() {
        return graph(String.class);
    }

    /**
     * Gets the object to manipulate graphs. This group requires the
     * <a href="https://redis.io/docs/stack/graph/">RedisGraph module</a>.
     *
     * @param <K>
     *        the type of keys
     *
     * @return the object to manipulate graphs lists.
     */
    @Experimental("The Redis graph support is experimental")
    <K> ReactiveTransactionalGraphCommands<K> graph(Class<K> redisKeyType);

    /**
     * Gets the object to emit commands from the {@code search} group. This group requires the
     * <a href="https://redis.io/docs/stack/search/">RedisSearch module</a>.
     *
     * @param <K>
     *        the type of keys
     *
     * @return the object to search documents
     */
    @Experimental("The Redis search support is experimental")
    <K> ReactiveTransactionalSearchCommands search(Class<K> redisKeyType);

    /**
     * Gets the object to emit commands from the {@code search} group. This group requires the
     * <a href="https://redis.io/docs/stack/search/">RedisSearch module</a>.
     *
     * @return the object to search documents
     */
    @Experimental("The Redis Search support is experimental")
    default ReactiveTransactionalSearchCommands search() {
        return search(String.class);
    }

    /**
     * Gets the object to emit commands from the {@code auto-suggest} group. This group requires the
     * <a href="https://redis.io/docs/stack/search/">RedisSearch module</a>.
     *
     * @param <K>
     *        the type of keys
     *
     * @return the object to get suggestions
     */
    @Experimental("The Redis auto-suggest support is experimental")
    <K> ReactiveTransactionalAutoSuggestCommands<K> autosuggest(Class<K> redisKeyType);

    /**
     * Gets the object to emit commands from the {@code auto-suggest} group. This group requires the
     * <a href="https://redis.io/docs/stack/search/">RedisSearch module</a>.
     *
     * @return the object to get suggestions
     */
    @Experimental("The Redis auto-suggest support is experimental")
    default ReactiveTransactionalAutoSuggestCommands<String> autosuggest() {
        return autosuggest(String.class);
    }

    /**
     * Gets the object to emit commands from the {@code time series} group. This group requires the
     * <a href="https://redis.io/docs/stack/timeseries/">Redis Time Series module</a>.
     *
     * @param <K>
     *        the type of keys
     *
     * @return the object to manipulate time series
     */
    @Experimental("The Redis time series support is experimental")
    <K> ReactiveTransactionalTimeSeriesCommands<K> timeseries(Class<K> redisKeyType);

    /**
     * Gets the object to emit commands from the {@code time series} group. This group requires the
     * <a href="https://redis.io/docs/stack/timeseries/">Redis Time Series module</a>.
     *
     * @return the object to manipulate time series
     */
    @Experimental("The Redis time series support is experimental")
    default ReactiveTransactionalTimeSeriesCommands<String> timeseries() {
        return timeseries(String.class);
    }

    /**
     * Executes a command. This method is used to execute commands not offered by the API.
     *
     * @param command
     *        the command name
     * @param args
     *        the parameters, encoded as String.
     *
     * @return the response
     */
    Uni<Void> execute(String command, String... args);

    /**
     * Executes a command. This method is used to execute commands not offered by the API.
     *
     * @param command
     *        the command
     * @param args
     *        the parameters, encoded as String.
     *
     * @return the response
     */
    Uni<Void> execute(Command command, String... args);

    /**
     * Executes a command. This method is used to execute commands not offered by the API.
     *
     * @param command
     *        the command
     * @param args
     *        the parameters, encoded as String.
     *
     * @return the response
     */
    Uni<Void> execute(io.vertx.redis.client.Command command, String... args);
}
