package io.quarkus.redis.lettuce.runtime.internal.stream;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.quarkus.redis.runtime.datasource.Validation.positive;
import static io.quarkus.redis.runtime.datasource.Validation.validateTimeout;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.lettuce.core.Consumer;
import io.lettuce.core.Limit;
import io.lettuce.core.Range;
import io.lettuce.core.XAutoClaimArgs;
import io.lettuce.core.XReadArgs.StreamOffset;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.models.stream.PendingMessages;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.stream.ClaimedMessages;
import io.quarkus.redis.datasource.stream.PendingMessage;
import io.quarkus.redis.datasource.stream.ReactiveStreamCommands;
import io.quarkus.redis.datasource.stream.StreamMessage;
import io.quarkus.redis.datasource.stream.StreamRange;
import io.quarkus.redis.datasource.stream.XAddArgs;
import io.quarkus.redis.datasource.stream.XClaimArgs;
import io.quarkus.redis.datasource.stream.XGroupCreateArgs;
import io.quarkus.redis.datasource.stream.XGroupSetIdArgs;
import io.quarkus.redis.datasource.stream.XPendingArgs;
import io.quarkus.redis.datasource.stream.XPendingSummary;
import io.quarkus.redis.datasource.stream.XReadArgs;
import io.quarkus.redis.datasource.stream.XReadGroupArgs;
import io.quarkus.redis.datasource.stream.XTrimArgs;
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionPool;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;

/**
 * Lettuce-backed implementation of {@link ReactiveStreamCommands}.
 *
 * @param <K> the key type
 * @param <F> the field type
 * @param <V> the value type
 */
public class LettuceReactiveStreamCommandsImpl<K, F, V> extends AbstractLettuceCommands<K, V>
        implements ReactiveStreamCommands<K, F, V>, ReactiveRedisCommands {

    private final ReactiveRedisDataSource dataSource;
    private final Type fieldType;

    public LettuceReactiveStreamCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnection connection, LettuceConnectionPool pool, Type keyType,
            Type fieldType, Type valueType) {
        super(connection, keyType, valueType, new Marshaller(keyType, fieldType, valueType), pool);
        this.dataSource = dataSource;
        this.fieldType = fieldType;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Integer> xack(K key, String group, String... ids) {
        return _xack(key, group, ids).toUni();
    }

    LettuceCommand<Long, Integer> _xack(K key, String group, String... ids) {
        nonNull(key, "key");
        nonNull(group, "group");
        notNullOrEmpty(ids, "ids");
        doesNotContainNull(ids, "ids");
        return LettuceCommand.of(() -> async.xack(marshaller.encode(key), marshaller.encode(group), ids),
                AbstractLettuceCommands::toInteger);
    }

    @Override
    public Uni<String> xadd(K key, Map<F, V> payload) {
        return _xadd(key, payload).toUni();
    }

    LettuceCommand<String, String> _xadd(K key, Map<F, V> payload) {
        return _xadd(key, new XAddArgs(), payload);
    }

    @Override
    public Uni<String> xadd(K key, XAddArgs args, Map<F, V> payload) {
        return _xadd(key, args, payload).toUni();
    }

    LettuceCommand<String, String> _xadd(K key, XAddArgs args, Map<F, V> payload) {
        nonNull(key, "key");
        nonNull(args, "args");
        notNullOrEmpty(payload, "payload");
        io.lettuce.core.XAddArgs lettuceArgs = LettuceStreamCommandsConverters.toXAddArgs(args);
        return LettuceCommand.of(() -> async.xadd(marshaller.encode(key), lettuceArgs, encodeMapWithNullableValues(payload)));
    }

    @Override
    public Uni<ClaimedMessages<K, F, V>> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start) {
        return _xautoclaim(key, group, consumer, minIdleTime, start).toUni();
    }

    LettuceCommand<io.lettuce.core.models.stream.ClaimedMessages<byte[], byte[]>, ClaimedMessages<K, F, V>> _xautoclaim(K key,
            String group, String consumer, Duration minIdleTime, String start) {
        validateXAutoClaimParams(key, group, consumer, minIdleTime, start);
        XAutoClaimArgs<byte[]> args = XAutoClaimArgs.Builder
                .xautoclaim(consumer(group, consumer), minIdleTime, start);
        return LettuceCommand.of(() -> async.xautoclaim(marshaller.encode(key), args), this::decodeClaimedMessages);
    }

    @Override
    public Uni<ClaimedMessages<K, F, V>> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start,
            int count) {
        return _xautoclaim(key, group, consumer, minIdleTime, start, count).toUni();
    }

    LettuceCommand<io.lettuce.core.models.stream.ClaimedMessages<byte[], byte[]>, ClaimedMessages<K, F, V>> _xautoclaim(K key,
            String group, String consumer, Duration minIdleTime, String start, int count) {
        validateXAutoClaimParams(key, group, consumer, minIdleTime, start);
        positive(count, "count");
        XAutoClaimArgs<byte[]> args = XAutoClaimArgs.Builder
                .xautoclaim(consumer(group, consumer), minIdleTime, start);
        args.count(count);
        return LettuceCommand.of(() -> async.xautoclaim(marshaller.encode(key), args), this::decodeClaimedMessages);
    }

    @Override
    public Uni<ClaimedMessages<K, F, V>> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start,
            int count, boolean justId) {
        return _xautoclaim(key, group, consumer, minIdleTime, start, count, justId).toUni();
    }

    LettuceCommand<io.lettuce.core.models.stream.ClaimedMessages<byte[], byte[]>, ClaimedMessages<K, F, V>> _xautoclaim(K key,
            String group, String consumer, Duration minIdleTime, String start, int count, boolean justId) {
        validateXAutoClaimParams(key, group, consumer, minIdleTime, start);
        positive(count, "count");
        XAutoClaimArgs<byte[]> args = XAutoClaimArgs.Builder
                .xautoclaim(consumer(group, consumer), minIdleTime, start);
        args.count(count);
        if (justId) {
            args.justid();
        }
        return LettuceCommand.of(() -> async.xautoclaim(marshaller.encode(key), args), this::decodeClaimedMessages);
    }

    private void validateXAutoClaimParams(K key, String group, String consumer, Duration minIdleTime, String start) {
        nonNull(key, "key");
        notNullOrBlank(group, "group");
        notNullOrBlank(consumer, "consumer");
        validateTimeout(minIdleTime, "minIdleTime");
        notNullOrBlank(start, "start");
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xclaim(K key, String group, String consumer, Duration minIdleTime,
            String... id) {
        return _xclaim(key, group, consumer, minIdleTime, id).toUni();
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xclaim(K key,
            String group, String consumer, Duration minIdleTime, String... id) {
        nonNull(key, "key");
        notNullOrBlank(group, "group");
        notNullOrBlank(consumer, "consumer");
        validateTimeout(minIdleTime, "minIdleTime");
        notNullOrEmpty(id, "id");
        doesNotContainNull(id, "id");
        Consumer<byte[]> lettuceConsumer = consumer(group, consumer);
        return LettuceCommand.of(() -> async.xclaim(marshaller.encode(key), lettuceConsumer, minIdleTime.toMillis(), id),
                this::decodeStreamMessages);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xclaim(K key, String group, String consumer, Duration minIdleTime,
            XClaimArgs args, String... id) {
        return _xclaim(key, group, consumer, minIdleTime, args, id).toUni();
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xclaim(K key,
            String group, String consumer, Duration minIdleTime, XClaimArgs args, String... id) {
        nonNull(key, "key");
        notNullOrBlank(group, "group");
        notNullOrBlank(consumer, "consumer");
        validateTimeout(minIdleTime, "minIdleTime");
        nonNull(args, "args");
        notNullOrEmpty(id, "id");
        doesNotContainNull(id, "id");
        Consumer<byte[]> lettuceConsumer = consumer(group, consumer);
        io.lettuce.core.XClaimArgs lettuceArgs = LettuceStreamCommandsConverters.toXClaimArgs(args, minIdleTime);
        return LettuceCommand.of(() -> async.xclaim(marshaller.encode(key), lettuceConsumer, lettuceArgs, id),
                this::decodeStreamMessages);
    }

    @Override
    public Uni<Integer> xdel(K key, String... id) {
        return _xdel(key, id).toUni();
    }

    LettuceCommand<Long, Integer> _xdel(K key, String... id) {
        nonNull(key, "key");
        notNullOrEmpty(id, "id");
        doesNotContainNull(id, "id");
        return LettuceCommand.of(() -> async.xdel(marshaller.encode(key), id), AbstractLettuceCommands::toInteger);
    }

    @Override
    public Uni<Void> xgroupCreate(K key, String groupname, String from) {
        return _xgroupCreate(key, groupname, from).toUni();
    }

    LettuceCommand<String, Void> _xgroupCreate(K key, String groupname, String from) {
        nonNull(key, "key");
        notNullOrBlank(groupname, "groupname");
        notNullOrBlank(from, "from");
        return LettuceCommand.discarding(
                () -> async.xgroupCreate(StreamOffset.from(marshaller.encode(key), from), marshaller.encode(groupname)));
    }

    @Override
    public Uni<Void> xgroupCreate(K key, String groupname, String from, XGroupCreateArgs args) {
        return _xgroupCreate(key, groupname, from, args).toUni();
    }

    LettuceCommand<String, Void> _xgroupCreate(K key, String groupname, String from, XGroupCreateArgs args) {
        nonNull(key, "key");
        notNullOrBlank(groupname, "groupname");
        notNullOrBlank(from, "from");
        nonNull(args, "args");
        io.lettuce.core.XGroupCreateArgs lettuceArgs = LettuceStreamCommandsConverters.toXGroupCreateArgs(args);
        return LettuceCommand.discarding(() -> async.xgroupCreate(StreamOffset.from(marshaller.encode(key), from),
                marshaller.encode(groupname), lettuceArgs));
    }

    @Override
    public Uni<Boolean> xgroupCreateConsumer(K key, String groupname, String consumername) {
        return _xgroupCreateConsumer(key, groupname, consumername).toUni();
    }

    LettuceCommand<Boolean, Boolean> _xgroupCreateConsumer(K key, String groupname, String consumername) {
        nonNull(key, "key");
        notNullOrBlank(groupname, "groupname");
        notNullOrBlank(consumername, "consumername");
        Consumer<byte[]> lettuceConsumer = consumer(groupname, consumername);
        return LettuceCommand.of(() -> async.xgroupCreateconsumer(marshaller.encode(key), lettuceConsumer));
    }

    @Override
    public Uni<Long> xgroupDelConsumer(K key, String groupname, String consumername) {
        return _xgroupDelConsumer(key, groupname, consumername).toUni();
    }

    LettuceCommand<Long, Long> _xgroupDelConsumer(K key, String groupname, String consumername) {
        nonNull(key, "key");
        notNullOrBlank(groupname, "groupname");
        notNullOrBlank(consumername, "consumername");
        Consumer<byte[]> lettuceConsumer = consumer(groupname, consumername);
        return LettuceCommand.of(() -> async.xgroupDelconsumer(marshaller.encode(key), lettuceConsumer));
    }

    @Override
    public Uni<Boolean> xgroupDestroy(K key, String groupname) {
        return _xgroupDestroy(key, groupname).toUni();
    }

    LettuceCommand<Boolean, Boolean> _xgroupDestroy(K key, String groupname) {
        nonNull(key, "key");
        notNullOrBlank(groupname, "groupname");
        return LettuceCommand.of(() -> async.xgroupDestroy(marshaller.encode(key), marshaller.encode(groupname)));
    }

    @Override
    public Uni<Void> xgroupSetId(K key, String groupname, String from) {
        return _xgroupSetId(key, groupname, from).toUni();
    }

    LettuceCommand<String, Void> _xgroupSetId(K key, String groupname, String from) {
        nonNull(key, "key");
        notNullOrBlank(groupname, "groupname");
        notNullOrBlank(from, "from");
        return LettuceCommand.discarding(
                () -> async.xgroupSetid(StreamOffset.from(marshaller.encode(key), from), marshaller.encode(groupname)));
    }

    @Override
    public Uni<Void> xgroupSetId(K key, String groupname, String from, XGroupSetIdArgs args) {
        return _xgroupSetId(key, groupname, from, args).toUni();
    }

    LettuceCommand<String, Void> _xgroupSetId(K key, String groupname, String from, XGroupSetIdArgs args) {
        nonNull(key, "key");
        notNullOrBlank(groupname, "groupname");
        notNullOrBlank(from, "from");
        nonNull(args, "args");
        CommandArgs<byte[], byte[]> commandArgs = LettuceStreamCommandsConverters.toXGroupSetIdCommandArgs(
                marshaller.encode(key), marshaller.encode(groupname), from, args);
        return LettuceCommand.discarding(
                () -> async.dispatch(CommandType.XGROUP, new StatusOutput<>(ByteArrayCodec.INSTANCE), commandArgs));
    }

    @Override
    public Uni<Long> xlen(K key) {
        return _xlen(key).toUni();
    }

    LettuceCommand<Long, Long> _xlen(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.xlen(marshaller.encode(key)));
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xrange(K key, StreamRange range, int count) {
        return _xrange(key, range, count).toUni();
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xrange(K key,
            StreamRange range, int count) {
        nonNull(key, "key");
        nonNull(range, "range");
        positive(count, "count");
        Range<String> lettuceRange = LettuceStreamCommandsConverters.toRange(range);
        return LettuceCommand.of(() -> async.xrange(marshaller.encode(key), lettuceRange, Limit.from(count)),
                this::decodeStreamMessages);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xrange(K key, StreamRange range) {
        return _xrange(key, range).toUni();
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xrange(K key,
            StreamRange range) {
        nonNull(key, "key");
        nonNull(range, "range");
        Range<String> lettuceRange = LettuceStreamCommandsConverters.toRange(range);
        return LettuceCommand.of(() -> async.xrange(marshaller.encode(key), lettuceRange), this::decodeStreamMessages);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xread(K key, String id) {
        return _xread(key, id).toUni();
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xread(K key,
            String id) {
        nonNull(key, "key");
        notNullOrBlank(id, "id");
        StreamOffset<byte[]>[] offsets = toStreamOffsets(key, id);
        return LettuceCommand.of(() -> async.xread(offsets), this::decodeStreamMessages);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xread(Map<K, String> lastIdsPerStream) {
        return _xread(lastIdsPerStream).toUni();
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xread(
            Map<K, String> lastIdsPerStream) {
        nonNull(lastIdsPerStream, "lastIdsPerStream");
        StreamOffset<byte[]>[] offsets = toStreamOffsets(lastIdsPerStream);
        return LettuceCommand.of(() -> async.xread(offsets), this::decodeStreamMessages);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xread(K key, String id, XReadArgs args) {
        if (LettuceStreamCommandsConverters.blocks(args)) {
            return blocking(cmds -> _xread(cmds, key, id, args));
        }
        return _xread(key, id, args).toUni();
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xread(K key,
            String id, XReadArgs args) {
        return _xread(async, key, id, args);
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xread(
            RedisClusterAsyncCommands<byte[], byte[]> cmds, K key, String id, XReadArgs args) {
        nonNull(key, "key");
        notNullOrBlank(id, "id");
        nonNull(args, "args");
        io.lettuce.core.XReadArgs lettuceArgs = LettuceStreamCommandsConverters.toXReadArgs(args);
        StreamOffset<byte[]>[] offsets = toStreamOffsets(key, id);
        return LettuceCommand.of(() -> cmds.xread(lettuceArgs, offsets), this::decodeStreamMessages);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xread(Map<K, String> lastIdsPerStream, XReadArgs args) {
        if (LettuceStreamCommandsConverters.blocks(args)) {
            return blocking(cmds -> _xread(cmds, lastIdsPerStream, args));
        }
        return _xread(lastIdsPerStream, args).toUni();
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xread(
            Map<K, String> lastIdsPerStream, XReadArgs args) {
        return _xread(async, lastIdsPerStream, args);
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xread(
            RedisClusterAsyncCommands<byte[], byte[]> cmds, Map<K, String> lastIdsPerStream, XReadArgs args) {
        nonNull(lastIdsPerStream, "lastIdsPerStream");
        nonNull(args, "args");
        io.lettuce.core.XReadArgs lettuceArgs = LettuceStreamCommandsConverters.toXReadArgs(args);
        StreamOffset<byte[]>[] offsets = toStreamOffsets(lastIdsPerStream);
        return LettuceCommand.of(() -> cmds.xread(lettuceArgs, offsets), this::decodeStreamMessages);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xreadgroup(String group, String consumer, K key, String id) {
        return _xreadgroup(group, consumer, key, id).toUni();
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xreadgroup(
            String group, String consumer, K key, String id) {
        notNullOrBlank(group, "group");
        notNullOrBlank(consumer, "consumer");
        nonNull(key, "key");
        notNullOrBlank(id, "id");
        Consumer<byte[]> lettuceConsumer = consumer(group, consumer);
        StreamOffset<byte[]>[] offsets = toStreamOffsets(key, id);
        return LettuceCommand.of(() -> async.xreadgroup(lettuceConsumer, offsets), this::decodeStreamMessages);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xreadgroup(String group, String consumer, Map<K, String> lastIdsPerStream) {
        return _xreadgroup(group, consumer, lastIdsPerStream).toUni();
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xreadgroup(
            String group, String consumer, Map<K, String> lastIdsPerStream) {
        notNullOrBlank(group, "group");
        notNullOrBlank(consumer, "consumer");
        nonNull(lastIdsPerStream, "lastIdsPerStream");
        Consumer<byte[]> lettuceConsumer = consumer(group, consumer);
        StreamOffset<byte[]>[] offsets = toStreamOffsets(lastIdsPerStream);
        return LettuceCommand.of(() -> async.xreadgroup(lettuceConsumer, offsets), this::decodeStreamMessages);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xreadgroup(String group, String consumer, K key, String id,
            XReadGroupArgs args) {
        if (LettuceStreamCommandsConverters.blocks(args)) {
            return blocking(cmds -> _xreadgroup(cmds, group, consumer, key, id, args));
        }
        return _xreadgroup(group, consumer, key, id, args).toUni();
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xreadgroup(
            String group, String consumer, K key, String id, XReadGroupArgs args) {
        return _xreadgroup(async, group, consumer, key, id, args);
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xreadgroup(
            RedisClusterAsyncCommands<byte[], byte[]> cmds, String group, String consumer, K key, String id,
            XReadGroupArgs args) {
        notNullOrBlank(group, "group");
        notNullOrBlank(consumer, "consumer");
        nonNull(key, "key");
        notNullOrBlank(id, "id");
        nonNull(args, "args");
        Consumer<byte[]> lettuceConsumer = consumer(group, consumer);
        io.lettuce.core.XReadArgs lettuceArgs = LettuceStreamCommandsConverters.toXReadArgs(args);
        StreamOffset<byte[]>[] offsets = toStreamOffsets(key, id);
        return LettuceCommand.of(() -> cmds.xreadgroup(lettuceConsumer, lettuceArgs, offsets), this::decodeStreamMessages);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xreadgroup(String group, String consumer, Map<K, String> lastIdsPerStream,
            XReadGroupArgs args) {
        if (LettuceStreamCommandsConverters.blocks(args)) {
            return blocking(cmds -> _xreadgroup(cmds, group, consumer, lastIdsPerStream, args));
        }
        return _xreadgroup(group, consumer, lastIdsPerStream, args).toUni();
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xreadgroup(
            String group, String consumer, Map<K, String> lastIdsPerStream, XReadGroupArgs args) {
        return _xreadgroup(async, group, consumer, lastIdsPerStream, args);
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xreadgroup(
            RedisClusterAsyncCommands<byte[], byte[]> cmds, String group, String consumer, Map<K, String> lastIdsPerStream,
            XReadGroupArgs args) {
        notNullOrBlank(group, "group");
        notNullOrBlank(consumer, "consumer");
        nonNull(lastIdsPerStream, "lastIdsPerStream");
        nonNull(args, "args");
        Consumer<byte[]> lettuceConsumer = consumer(group, consumer);
        io.lettuce.core.XReadArgs lettuceArgs = LettuceStreamCommandsConverters.toXReadArgs(args);
        StreamOffset<byte[]>[] offsets = toStreamOffsets(lastIdsPerStream);
        return LettuceCommand.of(() -> cmds.xreadgroup(lettuceConsumer, lettuceArgs, offsets), this::decodeStreamMessages);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xrevrange(K key, StreamRange range, int count) {
        return _xrevrange(key, range, count).toUni();
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xrevrange(K key,
            StreamRange range, int count) {
        nonNull(key, "key");
        nonNull(range, "range");
        positive(count, "count");
        Range<String> lettuceRange = LettuceStreamCommandsConverters.toReverseRange(range);
        return LettuceCommand.of(() -> async.xrevrange(marshaller.encode(key), lettuceRange, Limit.from(count)),
                this::decodeStreamMessages);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xrevrange(K key, StreamRange range) {
        return _xrevrange(key, range).toUni();
    }

    LettuceCommand<List<io.lettuce.core.StreamMessage<byte[], byte[]>>, List<StreamMessage<K, F, V>>> _xrevrange(K key,
            StreamRange range) {
        nonNull(key, "key");
        nonNull(range, "range");
        Range<String> lettuceRange = LettuceStreamCommandsConverters.toReverseRange(range);
        return LettuceCommand.of(() -> async.xrevrange(marshaller.encode(key), lettuceRange), this::decodeStreamMessages);
    }

    @Override
    public Uni<Long> xtrim(K key, String threshold) {
        return _xtrim(key, threshold).toUni();
    }

    LettuceCommand<Long, Long> _xtrim(K key, String threshold) {
        nonNull(threshold, "threshold");
        return _xtrim(key, new XTrimArgs().minid(threshold));
    }

    @Override
    public Uni<Long> xtrim(K key, XTrimArgs args) {
        return _xtrim(key, args).toUni();
    }

    LettuceCommand<Long, Long> _xtrim(K key, XTrimArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        io.lettuce.core.XTrimArgs lettuceArgs = LettuceStreamCommandsConverters.toXTrimArgs(args);
        return LettuceCommand.of(() -> async.xtrim(marshaller.encode(key), lettuceArgs));
    }

    @Override
    public Uni<XPendingSummary> xpending(K key, String group) {
        return _xpending(key, group).toUni();
    }

    LettuceCommand<PendingMessages, XPendingSummary> _xpending(K key, String group) {
        nonNull(key, "key");
        nonNull(group, "group");
        return LettuceCommand.of(() -> async.xpending(marshaller.encode(key), marshaller.encode(group)),
                LettuceReactiveStreamCommandsImpl::decodePendingSummary);
    }

    @Override
    public Uni<List<PendingMessage>> xpending(K key, String group, StreamRange range, int count) {
        return xpending(key, group, range, count, null);
    }

    @Override
    public Uni<List<PendingMessage>> xpending(K key, String group, StreamRange range, int count, XPendingArgs args) {
        return _xpending(key, group, range, count, args).toUni();
    }

    LettuceCommand<List<io.lettuce.core.models.stream.PendingMessage>, List<PendingMessage>> _xpending(K key,
            String group, StreamRange range, int count, XPendingArgs args) {
        nonNull(key, "key");
        nonNull(group, "group");
        nonNull(range, "range");
        positive(count, "count");
        io.lettuce.core.XPendingArgs<byte[]> lettuceArgs = LettuceStreamCommandsConverters.toXPendingArgs(
                marshaller.encode(group), range, count, args);
        return LettuceCommand.of(() -> async.xpending(marshaller.encode(key), lettuceArgs),
                LettuceReactiveStreamCommandsImpl::decodePendingMessages);
    }

    // Lettuce declares xread/xreadgroup with a generic varargs parameter (StreamOffset<K>...) on an interface,
    // which cannot carry @SafeVarargs. Passing a single StreamOffset would make javac create a generic array at
    // every call site and emit an unchecked warning, so the array is built here and the suppression stays local.
    @SuppressWarnings("unchecked")
    private StreamOffset<byte[]>[] toStreamOffsets(K key, String id) {
        return new StreamOffset[] { StreamOffset.from(marshaller.encode(key), id) };
    }

    @SuppressWarnings("unchecked")
    private StreamOffset<byte[]>[] toStreamOffsets(Map<K, String> lastIdsPerStream) {
        StreamOffset<byte[]>[] offsets = new StreamOffset[lastIdsPerStream.size()];
        int i = 0;
        for (Map.Entry<K, String> entry : lastIdsPerStream.entrySet()) {
            offsets[i++] = StreamOffset.from(marshaller.encode(entry.getKey()), entry.getValue());
        }
        return offsets;
    }

    private static XPendingSummary decodePendingSummary(PendingMessages pending) {
        Range<String> ids = pending.getMessageIds();
        String lowest = ids == null ? null : ids.getLower().getValue();
        String highest = ids == null ? null : ids.getUpper().getValue();
        Map<String, Long> consumers = pending.getConsumerMessageCount();
        return new XPendingSummary(pending.getCount(), lowest, highest,
                consumers == null || consumers.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(consumers));
    }

    private static List<PendingMessage> decodePendingMessages(List<io.lettuce.core.models.stream.PendingMessage> list) {
        return decodeList(list, m -> new PendingMessage(m.getId(), m.getConsumer(),
                Duration.ofMillis(m.getMsSinceLastDelivery()), (int) m.getRedeliveryCount()));
    }

    private StreamMessage<K, F, V> decodeStreamMessage(io.lettuce.core.StreamMessage<byte[], byte[]> message) {
        K key = decodeKey(message.getStream());
        Map<F, V> payload = decodeMap(fieldType, message.getBody());
        Long millisElapsedFromDelivery = message.getMillisElapsedFromDelivery();
        if (millisElapsedFromDelivery != null) {
            Long deliveredCount = message.getDeliveredCount();
            return new StreamMessage<>(key, message.getId(), payload, Duration.ofMillis(millisElapsedFromDelivery),
                    deliveredCount == null ? 0 : deliveredCount.intValue());
        }
        return new StreamMessage<>(key, message.getId(), payload);
    }

    private List<StreamMessage<K, F, V>> decodeStreamMessages(
            List<io.lettuce.core.StreamMessage<byte[], byte[]>> messages) {
        return decodeList(messages, this::decodeStreamMessage);
    }

    private ClaimedMessages<K, F, V> decodeClaimedMessages(
            io.lettuce.core.models.stream.ClaimedMessages<byte[], byte[]> lettuceMessages) {
        return new ClaimedMessages<>(lettuceMessages.getId(), decodeStreamMessages(lettuceMessages.getMessages()));
    }

    private Consumer<byte[]> consumer(String group, String consumer) {
        return Consumer.from(marshaller.encode(group), marshaller.encode(consumer));
    }

}
