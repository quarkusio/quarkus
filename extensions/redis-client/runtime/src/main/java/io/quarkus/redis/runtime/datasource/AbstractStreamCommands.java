package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.quarkus.redis.runtime.datasource.Validation.positive;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.stream.StreamRange;
import io.quarkus.redis.datasource.stream.XAddArgs;
import io.quarkus.redis.datasource.stream.XClaimArgs;
import io.quarkus.redis.datasource.stream.XGroupCreateArgs;
import io.quarkus.redis.datasource.stream.XGroupSetIdArgs;
import io.quarkus.redis.datasource.stream.XPendingArgs;
import io.quarkus.redis.datasource.stream.XReadArgs;
import io.quarkus.redis.datasource.stream.XReadGroupArgs;
import io.quarkus.redis.datasource.stream.XTrimArgs;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

public class AbstractStreamCommands<K, F, V> extends AbstractRedisCommands {

    AbstractStreamCommands(RedisCommandExecutor redis, Type k, Type m, Type v) {
        super(redis, new Marshaller(k, m, v));
    }

    Uni<Response> _xack(K key, String group, String... ids) {
        nonNull(key, "key");
        nonNull(group, "group");
        notNullOrEmpty(ids, "ids");
        doesNotContainNull(ids, "ids");

        RedisCommand cmd = RedisCommand.of(Command.XACK)
                .put(marshaller.encode(key))
                .put(group)
                .putAll(ids);
        return execute(cmd);
    }

    Uni<Response> _xadd(K key, Map<F, V> payload) {
        return _xadd(key, new XAddArgs(), payload);
    }

    Uni<Response> _xadd(K key, XAddArgs args, Map<F, V> payload) {
        nonNull(key, "key");
        nonNull(args, "args");
        nonNull(payload, "payload");
        RedisCommand cmd = RedisCommand.of(Command.XADD)
                .put(marshaller.encode(key))
                .putArgs(args);
        for (Map.Entry<F, V> entry : payload.entrySet()) {
            cmd.put(marshaller.encode(entry.getKey()));
            cmd.putNullable(marshaller.encode(entry.getValue()));
        }
        return execute(cmd);
    }

    Uni<Response> _xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start, int count) {
        nonNull(key, "key");
        notNullOrBlank(group, "group");
        notNullOrBlank(consumer, "consumer");
        ParameterValidation.validate(minIdleTime, "minIdleTime");
        notNullOrBlank(start, "start");
        positive(count, "count");
        RedisCommand cmd = RedisCommand.of(Command.XAUTOCLAIM)
                .put(marshaller.encode(key))
                .put(group).put(consumer).put(minIdleTime.toMillis()).put(start)
                .put("COUNT").put(count);

        return execute(cmd);
    }

    Uni<Response> _xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start) {
        nonNull(key, "key");
        notNullOrBlank(group, "group");
        notNullOrBlank(consumer, "consumer");
        ParameterValidation.validate(minIdleTime, "minIdleTime");
        notNullOrBlank(start, "start");
        RedisCommand cmd = RedisCommand.of(Command.XAUTOCLAIM)
                .put(marshaller.encode(key))
                .put(group).put(consumer).put(minIdleTime.toMillis()).put(start);

        return execute(cmd);
    }

    Uni<Response> _xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start, int count,
            boolean justId) {
        nonNull(key, "key");
        notNullOrBlank(group, "group");
        notNullOrBlank(consumer, "consumer");
        ParameterValidation.validate(minIdleTime, "minIdleTime");
        notNullOrBlank(start, "start");
        positive(count, "count");
        RedisCommand cmd = RedisCommand.of(Command.XAUTOCLAIM)
                .put(marshaller.encode(key))
                .put(group).put(consumer).put(minIdleTime.toMillis()).put(start);
        if (count > 0) {
            cmd.put("COUNT").put(count);
        }
        if (justId) {
            cmd.put("JUSTID");
        }
        return execute(cmd);
    }

    Uni<Response> _xclaim(K key, String group, String consumer, Duration minIdleTime, String... id) {
        nonNull(key, "key");
        notNullOrBlank(group, "group");
        notNullOrBlank(consumer, "consumer");
        ParameterValidation.validate(minIdleTime, "minIdleTime");
        notNullOrEmpty(id, "id");
        doesNotContainNull(id, "id");

        RedisCommand cmd = RedisCommand.of(Command.XCLAIM)
                .put(marshaller.encode(key))
                .put(group)
                .put(consumer)
                .put(Long.toString(minIdleTime.toMillis()))
                .putAll(id);
        return execute(cmd);
    }

    Uni<Response> _xclaim(K key, String group, String consumer, Duration minIdleTime, XClaimArgs args, String... id) {
        nonNull(key, "key");
        notNullOrBlank(group, "group");
        notNullOrBlank(consumer, "consumer");
        ParameterValidation.validate(minIdleTime, "minIdleTime");
        nonNull(args, "args");
        notNullOrEmpty(id, "id");
        doesNotContainNull(id, "id");

        RedisCommand cmd = RedisCommand.of(Command.XCLAIM)
                .put(marshaller.encode(key))
                .put(group)
                .put(consumer)
                .put(Long.toString(minIdleTime.toMillis()))
                .putAll(id)
                .putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _xdel(K key, String... id) {
        nonNull(key, "key");
        notNullOrEmpty(id, "id");
        doesNotContainNull(id, "id");

        RedisCommand cmd = RedisCommand.of(Command.XDEL)
                .put(marshaller.encode(key))
                .putAll(id);
        return execute(cmd);
    }

    Uni<Response> _xgroupCreate(K key, String groupname, String from) {
        nonNull(key, "key");
        notNullOrBlank(groupname, "groupname");
        notNullOrBlank(from, "from");

        RedisCommand cmd = RedisCommand.of(Command.XGROUP)
                .put("CREATE")
                .put(marshaller.encode(key))
                .put(groupname)
                .put(from);
        return execute(cmd);
    }

    Uni<Response> _xgroupCreate(K key, String groupname, String from, XGroupCreateArgs args) {
        nonNull(key, "key");
        notNullOrBlank(groupname, "groupname");
        notNullOrBlank(from, "from");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.XGROUP)
                .put("CREATE")
                .put(marshaller.encode(key))
                .put(groupname)
                .put(from)
                .putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _xgroupCreateConsumer(K key, String groupname, String consumername) {
        nonNull(key, "key");
        notNullOrBlank(groupname, "groupname");
        notNullOrBlank(consumername, "consumername");
        RedisCommand cmd = RedisCommand.of(Command.XGROUP)
                .put("CREATECONSUMER")
                .put(marshaller.encode(key))
                .put(groupname)
                .put(consumername);
        return execute(cmd);
    }

    Uni<Response> _xgroupDelConsumer(K key, String groupname, String consumername) {
        nonNull(key, "key");
        notNullOrBlank(groupname, "groupname");
        notNullOrBlank(consumername, "consumername");
        RedisCommand cmd = RedisCommand.of(Command.XGROUP)
                .put("DELCONSUMER")
                .put(marshaller.encode(key))
                .put(groupname)
                .put(consumername);
        return execute(cmd);
    }

    Uni<Response> _xgroupDestroy(K key, String groupname) {
        nonNull(key, "key");
        notNullOrBlank(groupname, "groupname");

        RedisCommand cmd = RedisCommand.of(Command.XGROUP)
                .put("DESTROY")
                .put(marshaller.encode(key))
                .put(groupname);
        return execute(cmd);
    }

    Uni<Response> _xgroupSetId(K key, String groupname, String from) {
        nonNull(key, "key");
        notNullOrBlank(groupname, "groupname");
        notNullOrBlank(from, "from");

        RedisCommand cmd = RedisCommand.of(Command.XGROUP)
                .put("SETID")
                .put(marshaller.encode(key))
                .put(groupname)
                .put(from);
        return execute(cmd);
    }

    Uni<Response> _xgroupSetId(K key, String groupname, String from, XGroupSetIdArgs args) {
        nonNull(key, "key");
        notNullOrBlank(groupname, "groupname");
        notNullOrBlank(from, "from");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.XGROUP)
                .put("SETID")
                .put(marshaller.encode(key))
                .put(groupname)
                .put(from)
                .putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _xlen(K key) {
        nonNull(key, "key");
        RedisCommand cmd = RedisCommand.of(Command.XLEN)
                .put(marshaller.encode(key));
        return execute(cmd);
    }

    Uni<Response> _xrange(K key, StreamRange range, int count) {
        nonNull(key, "key");
        nonNull(range, "range");
        positive(count, "count");
        RedisCommand cmd = RedisCommand.of(Command.XRANGE)
                .put(marshaller.encode(key))
                .putArgs(range)
                .put("COUNT").put(count);
        return execute(cmd);
    }

    Uni<Response> _xrange(K key, StreamRange range) {
        nonNull(key, "key");
        nonNull(range, "range");
        RedisCommand cmd = RedisCommand.of(Command.XRANGE)
                .put(marshaller.encode(key))
                .putArgs(range);
        return execute(cmd);
    }

    Uni<Response> _xread(K key, String id) {
        nonNull(key, "key");
        notNullOrBlank(id, "id");
        RedisCommand cmd = RedisCommand.of(Command.XREAD)
                .put("STREAMS")
                .put(marshaller.encode(key))
                .put(id);
        return execute(cmd);
    }

    Uni<Response> _xread(Map<K, String> lastIdsPerStream) {
        nonNull(lastIdsPerStream, "lastIdsPerStream");
        RedisCommand cmd = RedisCommand.of(Command.XREAD)
                .put("STREAMS");

        writeStreamsAndIds(lastIdsPerStream, cmd);

        return execute(cmd);
    }

    Uni<Response> _xread(K key, String id, XReadArgs args) {
        nonNull(key, "key");
        notNullOrBlank(id, "id");
        nonNull(args, "args");
        RedisCommand cmd = RedisCommand.of(Command.XREAD)
                .putArgs(args)
                .put("STREAMS")
                .put(marshaller.encode(key))
                .put(id);
        return execute(cmd);
    }

    Uni<Response> _xread(Map<K, String> lastIdsPerStream, XReadArgs args) {
        nonNull(args, "args");
        RedisCommand cmd = RedisCommand.of(Command.XREAD)
                .putArgs(args)
                .put("STREAMS");

        writeStreamsAndIds(lastIdsPerStream, cmd);

        return execute(cmd);
    }

    private void writeStreamsAndIds(Map<K, String> lastIdsPerStream, RedisCommand cmd) {
        List<String> ids = new ArrayList<>();
        for (Map.Entry<K, String> entry : lastIdsPerStream.entrySet()) {
            cmd.put(marshaller.encode(entry.getKey()));
            ids.add(entry.getValue());
        }
        cmd.putAll(ids);
    }

    Uni<Response> _xreadgroup(String group, String consumer, K key, String id) {
        notNullOrBlank(group, "group");
        notNullOrBlank(consumer, "consumer");
        nonNull(key, "key");
        notNullOrBlank(id, "id");

        RedisCommand cmd = RedisCommand.of(Command.XREADGROUP)
                .put("GROUP")
                .put(group)
                .put(consumer)
                .put("STREAMS")
                .put(marshaller.encode(key))
                .put(id);
        return execute(cmd);
    }

    Uni<Response> _xreadgroup(String group, String consumer, Map<K, String> lastIdsPerStream) {
        notNullOrBlank(group, "group");
        notNullOrBlank(consumer, "consumer");
        nonNull(lastIdsPerStream, "lastIdsPerStream");

        RedisCommand cmd = RedisCommand.of(Command.XREADGROUP)
                .put("GROUP")
                .put(group)
                .put(consumer)
                .put("STREAMS");

        writeStreamsAndIds(lastIdsPerStream, cmd);

        return execute(cmd);

    }

    Uni<Response> _xreadgroup(String group, String consumer, K key, String id, XReadGroupArgs args) {
        notNullOrBlank(group, "group");
        notNullOrBlank(consumer, "consumer");
        nonNull(key, "key");
        notNullOrBlank(id, "id");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.XREADGROUP)
                .put("GROUP")
                .put(group)
                .put(consumer)
                .putArgs(args)
                .put("STREAMS")
                .put(marshaller.encode(key))
                .put(id);
        return execute(cmd);
    }

    Uni<Response> _xreadgroup(String group, String consumer, Map<K, String> lastIdsPerStream, XReadGroupArgs args) {
        notNullOrBlank(group, "group");
        notNullOrBlank(consumer, "consumer");
        nonNull(lastIdsPerStream, "lastIdsPerStream");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.XREADGROUP)
                .put("GROUP")
                .put(group)
                .put(consumer)
                .putArgs(args)
                .put("STREAMS");

        writeStreamsAndIds(lastIdsPerStream, cmd);

        return execute(cmd);
    }

    Uni<Response> _xrevrange(K key, StreamRange range, int count) {
        nonNull(key, "key");
        nonNull(range, "range");
        positive(count, "count");

        RedisCommand cmd = RedisCommand.of(Command.XREVRANGE)
                .put(marshaller.encode(key))
                .putArgs(range)
                .put("COUNT")
                .put(count);
        return execute(cmd);
    }

    Uni<Response> _xrevrange(K key, StreamRange range) {
        nonNull(key, "key");
        nonNull(range, "range");
        RedisCommand cmd = RedisCommand.of(Command.XREVRANGE)
                .put(marshaller.encode(key))
                .putArgs(range);
        return execute(cmd);
    }

    Uni<Response> _xtrim(K key, XTrimArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.XTRIM)
                .put(marshaller.encode(key))
                .putArgs(args);

        return execute(cmd);
    }

    Uni<Response> _xpending(K key, String group) {
        nonNull(key, "key");
        nonNull(key, "group");

        RedisCommand cmd = RedisCommand.of(Command.XPENDING)
                .put(marshaller.encode(key))
                .put(group);
        return execute(cmd);
    }

    Uni<Response> _xpending(K key, String group, StreamRange range, int count, XPendingArgs args) {
        nonNull(key, "key");
        nonNull(key, "group");
        nonNull(range, "range");
        positive(count, "count");

        RedisCommand cmd = RedisCommand.of(Command.XPENDING)
                .put(marshaller.encode(key))
                .put(group);

        // IDLE must be before the range and count
        if (args != null && args.idle() != null) {
            cmd.put("IDLE");
            cmd.put(args.idle().toMillis());
        }

        cmd.putArgs(range)
                .put(count);

        if (args != null) {
            cmd.putArgs(args);
        }

        return execute(cmd);
    }
}
