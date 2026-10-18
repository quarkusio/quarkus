package io.quarkus.redis.datasource.stream;

import java.time.Duration;
import java.util.Map;

import io.quarkus.redis.datasource.ReactiveTransactionalRedisCommands;
import io.smallrye.mutiny.Uni;

/**
 * Allows executing commands manipulating streams.
 * See <a href="https://redis.io/commands/?group=stream">the stream command list</a> for further information about these
 * commands.
 * <p>
 * <p>
 * The messages are represented as {@code Map<F, V>}.
 * This API is intended to be used in a Redis transaction ({@code MULTI}), thus, all command methods return {@code Uni<Void>}.
 *
 * @param <K> the type of the keys, often {@link String}
 * @param <F> the key type of the messages, generally {@link String}
 * @param <V> the value type of the messages
 */
public interface ReactiveTransactionalStreamCommands<K, F, V> extends ReactiveTransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/xack">XACK</a>.
     * Summary: Marks a pending message as correctly processed, effectively removing it from the pending entries list
     * of the consumer group. Return value of the command is the number of messages successfully acknowledged, that is,
     * the IDs we were actually able to resolve in the PEL.
     * <p>
     * The {@code XACK} command removes one or multiple messages from the Pending Entries List (PEL) of a stream consumer
     * group. A message is pending, and as such stored inside the PEL, when it was delivered to some consumer, normally
     * as a side effect of calling {@code XREADGROUP}, or when a consumer took ownership of a message
     * calling {@code XCLAIM}. The pending message was delivered to some consumer but the server is yet not sure it was
     * processed at least once. So new calls to {@code XREADGROUP} to grab the messages history for a consumer
     * (for instance using an ID of 0), will return such message. Similarly, the pending message will be listed by the
     * {@code XPENDING} command, that inspects the PEL.
     * <p>
     * Once a consumer successfully processes a message, it should call {@code XACK} so that such message does not get
     * processed again, and as a side effect, the PEL entry about this message is also purged, releasing memory from
     * the Redis server.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key the key
     * @param group the name of the consumer group
     * @param ids the message ids to acknowledge
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xack(K key, String group, String... ids);

    /**
     * Execute the command <a href="https://redis.io/commands/xadd">XADD</a>.
     * Summary: Appends the specified stream entry to the stream at the specified key. If the key does not exist, as a
     * side effect of running this command the key is created with a stream value. The creation of stream's key can be
     * disabled with the {@code NOMKSTREAM} option.
     * <p>
     * An entry is composed of a list of field-value pairs. The field-value pairs are stored in the same order they are
     * given by the user. Commands that read the stream, such as {@code XRANGE} or {@code XREAD}, are guaranteed to
     * return the fields and values exactly in the same order they were added by {@code XADD}.
     * <p>
     * {@code XADD} is the only Redis command that can add data to a stream, but there are other commands, such as
     * {@code XDEL} and {@code XTRIM}, that are able to remove data from a stream.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key the key
     * @param payload the payload to write to the stream, must not be {@code null}
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xadd(K key, Map<F, V> payload);

    /**
     * Execute the command <a href="https://redis.io/commands/xadd">XADD</a>.
     * Summary: Appends the specified stream entry to the stream at the specified key. If the key does not exist, as a
     * side effect of running this command the key is created with a stream value. The creation of stream's key can be
     * disabled with the {@code NOMKSTREAM} option.
     * <p>
     * An entry is composed of a list of field-value pairs. The field-value pairs are stored in the same order they are
     * given by the user. Commands that read the stream, such as {@code XRANGE} or {@code XREAD}, are guaranteed to
     * return the fields and values exactly in the same order they were added by {@code XADD}.
     * <p>
     * {@code XADD} is the only Redis command that can add data to a stream, but there are other commands, such as
     * {@code XDEL} and {@code XTRIM}, that are able to remove data from a stream.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key the key
     * @param args the extra parameters
     * @param payload the payload to write to the stream, must not be {@code null}
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xadd(K key, XAddArgs args, Map<F, V> payload);

    /**
     * Execute the command <a href="https://redis.io/commands/xautoclaim">XAUTOCLAIM</a>.
     * Summary: Changes (or acquires) ownership of messages in a consumer group, as if the messages were delivered to
     * the specified consumer.
     * <p>
     * This command transfers ownership of pending stream entries that match the specified criteria. Conceptually,
     * {@code XAUTOCLAIM} is equivalent to calling {@code XPENDING} and then {@code XCLAIM}, but provides a more
     * straightforward way to deal with message delivery failures via {@code SCAN}-like semantics.
     * <p>
     * Like {@code XCLAIM}, the command operates on the stream entries at {@code key} and in the context of the provided
     * {@code group}. It transfers ownership to {@code consumer} of messages pending for more than {@code min-idle-time}
     * milliseconds and having an equal or greater ID than {@code start}.
     * <p>
     * Group: stream
     * Requires Redis 6.2.0+
     * <p>
     *
     * @param key key the key
     * @param group string the consumer group
     * @param consumer string the consumer id
     * @param minIdleTime the min pending time of the message to claim
     * @param start the min id of the message to claim
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start);

    /**
     * Execute the command <a href="https://redis.io/commands/xautoclaim">XAUTOCLAIM</a>.
     * Summary: Changes (or acquires) ownership of messages in a consumer group, as if the messages were delivered to
     * the specified consumer.
     * <p>
     * This command transfers ownership of pending stream entries that match the specified criteria. Conceptually,
     * {@code XAUTOCLAIM} is equivalent to calling {@code XPENDING} and then {@code XCLAIM}, but provides a more
     * straightforward way to deal with message delivery failures via {@code SCAN}-like semantics.
     * <p>
     * Like {@code XCLAIM}, the command operates on the stream entries at {@code key} and in the context of the provided
     * {@code group}. It transfers ownership to {@code consumer} of messages pending for more than {@code min-idle-time}
     * milliseconds and having an equal or greater ID than {@code start}.
     * <p>
     * Group: stream
     * Requires Redis 6.2.0+
     * <p>
     *
     * @param key key the key
     * @param group string the consumer group
     * @param consumer string the consumer id
     * @param minIdleTime the min pending time of the message to claim
     * @param start the min id of the message to claim
     * @param count the upper limit of the number of entries to claim, default is 100.
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/xautoclaim">XAUTOCLAIM</a>.
     * Summary: Changes (or acquires) ownership of messages in a consumer group, as if the messages were delivered to
     * the specified consumer.
     * <p>
     * This command transfers ownership of pending stream entries that match the specified criteria. Conceptually,
     * {@code XAUTOCLAIM} is equivalent to calling {@code XPENDING} and then {@code XCLAIM}, but provides a more
     * straightforward way to deal with message delivery failures via {@code SCAN}-like semantics.
     * <p>
     * Like {@code XCLAIM}, the command operates on the stream entries at {@code key} and in the context of the provided
     * {@code group}. It transfers ownership to {@code consumer} of messages pending for more than {@code min-idle-time}
     * milliseconds and having an equal or greater ID than {@code start}.
     * <p>
     * Group: stream
     * Requires Redis 6.2.0+
     * <p>
     *
     * @param key key the key
     * @param group string the consumer group
     * @param consumer string the consumer id
     * @param minIdleTime the min pending time of the message to claim
     * @param start the min id of the message to claim
     * @param count the upper limit of the number of entries to claim, default is 100.
     * @param justId if {@code true} the returned structure would only contain the id of the messages and not the payloads
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start, int count, boolean justId);

    /**
     * Execute the command <a href="https://redis.io/commands/xclaim">XCLAIM</a>.
     * Summary: In the context of a stream consumer group, this command changes the ownership of a pending message, so
     * that the new owner is the consumer specified as the command argument.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key key the key
     * @param group string the consumer group
     * @param consumer string the consumer id
     * @param minIdleTime the min pending time of the message to claim
     * @param id the message ids to claim, must not be empty, must not contain {@code null}
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xclaim(K key, String group, String consumer, Duration minIdleTime, String... id);

    /**
     * Execute the command <a href="https://redis.io/commands/xclaim">XCLAIM</a>.
     * Summary: In the context of a stream consumer group, this command changes the ownership of a pending message, so
     * that the new owner is the consumer specified as the command argument.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key key the key
     * @param group string the consumer group
     * @param consumer string the consumer id
     * @param minIdleTime the min pending time of the message to claim
     * @param args the extra command parameters
     * @param id the message ids to claim, must not be empty, must not contain {@code null}
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xclaim(K key, String group, String consumer, Duration minIdleTime, XClaimArgs args, String... id);

    /**
     * Execute the command <a href="https://redis.io/commands/xdel">XDEL</a>.
     * Summary: Removes the specified entries from a stream, and returns the number of entries deleted. This number may
     * be less than the number of IDs passed to the command in the case where some of the specified IDs do not exist in
     * the stream.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key key the key
     * @param id the message ids, must not be empty, must not contain {@code null}
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xdel(K key, String... id);

    /**
     * Execute the command <a href="https://redis.io/commands/xgroup-create">XGROUP CREATE</a>.
     * Summary: Create a new consumer group uniquely identified by {@code groupname} for the stream stored at {@code key}
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key key the key
     * @param groupname the name of the group, must be unique, and not {@code null}
     * @param from the last delivered entry in the stream from the new group's perspective. The special ID {@code $}
     *        is the ID of the last entry in the stream, but you can substitute it with any valid ID.
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xgroupCreate(K key, String groupname, String from);

    /**
     * Execute the command <a href="https://redis.io/commands/xgroup-create">XGROUP CREATE</a>.
     * Summary: Create a new consumer group uniquely identified by {@code groupname} for the stream stored at {@code key}
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key key the key
     * @param groupname the name of the group, must be unique, and not {@code null}
     * @param from the last delivered entry in the stream from the new group's perspective. The special ID {@code $}
     *        is the ID of the last entry in the stream, but you can substitute it with any valid ID.
     * @param args the extra command parameters
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xgroupCreate(K key, String groupname, String from, XGroupCreateArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/xgroup-createconsumer">XGROUP CREATECONSUMER</a>.
     * Summary: Create a consumer named {@code consumername} in the consumer group {@code groupname} of the stream
     * that's stored at {@code key}.
     * <p>
     * Consumers are also created automatically whenever an operation, such as {@code XREADGROUP}, references a consumer
     * that doesn't exist.
     * <p>
     * Group: stream
     * Requires Redis 6.2.0+
     * <p>
     *
     * @param key key the key
     * @param groupname the name of the group, must be unique, and not {@code null}
     * @param consumername the consumer name
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xgroupCreateConsumer(K key, String groupname, String consumername);

    /**
     * Execute the command <a href="https://redis.io/commands/xgroup-delconsumer">XGROUP DELCONSUMER</a>.
     * Summary: Deletes a consumer from the consumer group.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key key the key
     * @param groupname the name of the group, must be unique, and not {@code null}
     * @param consumername the consumer name
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xgroupDelConsumer(K key, String groupname, String consumername);

    /**
     * Execute the command <a href="https://redis.io/commands/xgroup-destroy">XGROUP DESTROY</a>.
     * Summary: Completely destroys a consumer group. The consumer group will be destroyed even if there are active
     * consumers, and pending messages, so make sure to call this command only when really needed.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key key the key
     * @param groupname the name of the group, must be unique, and not {@code null}
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xgroupDestroy(K key, String groupname);

    /**
     * Execute the command <a href="https://redis.io/commands/xgroup-setid">XGROUP SETID</a>.
     * Summary: Set the last delivered ID for a consumer group.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key key the key
     * @param groupname the name of the group, must be unique, and not {@code null}
     * @param from the last delivered entry in the stream from the new group's perspective. The special ID {@code $}
     *        is the ID of the last entry in the stream, but you can substitute it with any valid ID.
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xgroupSetId(K key, String groupname, String from);

    /**
     * Execute the command <a href="https://redis.io/commands/xgroup-setid">XGROUP SETID</a>.
     * Summary: Set the last delivered ID for a consumer group.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key key the key
     * @param groupname the name of the group, must be unique, and not {@code null}
     * @param from the last delivered entry in the stream from the new group's perspective. The special ID {@code $}
     *        is the ID of the last entry in the stream, but you can substitute it with any valid ID.
     * @param args the extra command parameters
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xgroupSetId(K key, String groupname, String from, XGroupSetIdArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/xlen">XLEN</a>.
     * Summary: Returns the number of entries inside a stream.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key key the key
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xlen(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/xrange">XRANGE</a>.
     * Summary: The command returns the stream entries matching a given range of IDs.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key key the key
     * @param range the range, must not be {@code null}
     * @param count the max number of entries to return
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xrange(K key, StreamRange range, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/xrange">XRANGE</a>.
     * Summary: The command returns the stream entries matching a given range of IDs.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key key the key
     * @param range the range, must not be {@code null}
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xrange(K key, StreamRange range);

    /**
     * Execute the command <a href="https://redis.io/commands/xread">XREAD</a>.
     * Summary: Read data from one or multiple streams, only returning entries with an ID greater than the last received
     * ID reported by the caller. This command has an option to block if items are not available, in a similar fashion
     * to {@code BRPOP} or {@code BZPOPMIN} and others.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key key the key of the stream
     * @param id the last read id
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xread(K key, String id);

    /**
     * Execute the command <a href="https://redis.io/commands/xread">XREAD</a>.
     * Summary: Read data from one or multiple streams, only returning entries with an ID greater than the last received
     * ID reported by the caller. This command has an option to block if items are not available, in a similar fashion
     * to {@code BRPOP} or {@code BZPOPMIN} and others.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param lastIdsPerStream the map of key -> id indicating the last received id per stream to read
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xread(Map<K, String> lastIdsPerStream);

    /**
     * Execute the command <a href="https://redis.io/commands/xread">XREAD</a>.
     * Summary: Read data from one or multiple streams, only returning entries with an ID greater than the last received
     * ID reported by the caller. This command has an option to block if items are not available, in a similar fashion
     * to {@code BRPOP} or {@code BZPOPMIN} and others.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key key the key of the stream
     * @param id the last read id
     * @param args the extra parameter
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xread(K key, String id, XReadArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/xread">XREAD</a>.
     * Summary: Read data from one or multiple streams, only returning entries with an ID greater than the last received
     * ID reported by the caller. This command has an option to block if items are not available, in a similar fashion
     * to {@code BRPOP} or {@code BZPOPMIN} and others.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param lastIdsPerStream the map of key -> id indicating the last received id per stream to read
     * @param args the extra parameter
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xread(Map<K, String> lastIdsPerStream, XReadArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/xreadgroup">XREADGROUP</a>.
     * Summary: The {@code XREADGROUP} command is a special version of the {@code XREAD} command with support for
     * consumer groups.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param group the group name
     * @param consumer the consumer name
     * @param key the stream key
     * @param id the last read id
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xreadgroup(String group, String consumer, K key, String id);

    /**
     * Execute the command <a href="https://redis.io/commands/xreadgroup">XREADGROUP</a>.
     * Summary: The {@code XREADGROUP} command is a special version of the {@code XREAD} command with support for
     * consumer groups.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param group the group name
     * @param consumer the consumer name
     * @param lastIdsPerStream the map of key -> id indicating the last received id per stream to read
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xreadgroup(String group, String consumer, Map<K, String> lastIdsPerStream);

    /**
     * Execute the command <a href="https://redis.io/commands/xreadgroup">XREADGROUP</a>.
     * Summary: The {@code XREADGROUP} command is a special version of the {@code XREAD} command with support for
     * consumer groups.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param group the group name
     * @param consumer the consumer name
     * @param key the stream key
     * @param id the last read id
     * @param args the extra parameter
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xreadgroup(String group, String consumer, K key, String id, XReadGroupArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/xreadgroup">XREADGROUP</a>.
     * Summary: The {@code XREADGROUP} command is a special version of the {@code XREAD} command with support for
     * consumer groups.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param group the group name
     * @param consumer the consumer name
     * @param lastIdsPerStream the map of key -> id indicating the last received id per stream to read
     * @param args the extra parameter
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xreadgroup(String group, String consumer, Map<K, String> lastIdsPerStream, XReadGroupArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/xrevrange">XREVRANGE</a>.
     * Summary: This command is exactly like {@code XRANGE}, but with the notable difference of returning the entries
     * in reverse order, and also taking the start-end range in reverse order: in {@code XREVRANGE} you need to state
     * the end ID and later the start ID, and the command will produce all the element between (or exactly like) the
     * two IDs, starting from the end side.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key key the key
     * @param range the range, must not be {@code null}
     * @param count the max number of entries to return
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xrevrange(K key, StreamRange range, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/xrange">XRANGE</a>.
     * Summary: This command is exactly like {@code XRANGE}, but with the notable difference of returning the entries
     * in reverse order, and also taking the start-end range in reverse order: in {@code XREVRANGE} you need to state
     * the end ID and later the start ID, and the command will produce all the element between (or exactly like) the
     * two IDs, starting from the end side.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key key the key
     * @param range the range, must not be {@code null}
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xrevrange(K key, StreamRange range);

    /**
     * Execute the command <a href="https://redis.io/commands/xtrim">XTRIM</a>.
     * Summary: Trims the stream by evicting older entries (entries with lower IDs) if needed.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key the key
     * @param threshold the threshold
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xtrim(K key, String threshold);

    /**
     * Execute the command <a href="https://redis.io/commands/xtrim">XTRIM</a>.
     * Summary: Trims the stream by evicting older entries (entries with lower IDs) if needed.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     *
     * @param key the key
     * @param args the extra parameters
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> xtrim(K key, XTrimArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/xpending">XPENDING</a>.
     * Summary: The XPENDING command is the interface to inspect the list of pending messages, and is as thus a very
     * important command in order to observe and understand what is happening with a streams consumer groups: what
     * clients are active, what messages are pending to be consumed, or to see if there are idle messages.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     * This variant of xpending uses the <em>summary</em> form.
     *
     * @param key the key
     * @param group the group
     * @return A {@link io.smallrye.mutiny.Uni} emitting the xpending summary
     */
    Uni<Void> xpending(K key, String group);

    /**
     * Execute the command <a href="https://redis.io/commands/xpending">XPENDING</a>.
     * Summary: The XPENDING command is the interface to inspect the list of pending messages, and is as thus a very
     * important command in order to observe and understand what is happening with a streams consumer groups: what
     * clients are active, what messages are pending to be consumed, or to see if there are idle messages.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     * This variant of xpending uses the <em>extended</em> form.
     *
     * @param key the key
     * @param group the group
     * @param range the range
     * @param count the number of message to include
     * @return A {@link io.smallrye.mutiny.Uni} emitting the list of pending messages
     */
    Uni<Void> xpending(K key, String group, StreamRange range, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/xpending">XPENDING</a>.
     * Summary: The XPENDING command is the interface to inspect the list of pending messages, and is as thus a very
     * important command in order to observe and understand what is happening with a streams consumer groups: what
     * clients are active, what messages are pending to be consumed, or to see if there are idle messages.
     * <p>
     * Group: stream
     * Requires Redis 5.0.0+
     * <p>
     * This variant of xpending uses the <em>extended</em> form.
     * <p>
     * If the extra parameter include the name of the consumer, the produced list will only contain 0 or 1 item.
     *
     * @param key the key
     * @param group the group
     * @param range the range
     * @param count the number of message to include
     * @param args the extra argument (idle and consumer)
     * @return A {@link io.smallrye.mutiny.Uni} emitting the list of pending messages
     */
    Uni<Void> xpending(K key, String group, StreamRange range, int count, XPendingArgs args);
}
