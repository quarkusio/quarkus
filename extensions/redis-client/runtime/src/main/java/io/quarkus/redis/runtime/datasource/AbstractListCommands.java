package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.DurationUtil.durationToSeconds;
import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.list.LPosArgs;
import io.quarkus.redis.datasource.list.Position;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

class AbstractListCommands<K, V> extends ReactiveSortable<K, V> {

    protected final Type typeOfValue;
    protected final Type typeOfKey;

    protected static final LPosArgs DEFAULT_INSTANCE = new LPosArgs();

    AbstractListCommands(RedisCommandExecutor redis, Type k, Type v) {
        super(redis, new Marshaller(k, v), v);
        this.typeOfKey = k;
        this.typeOfValue = v;
    }

    Uni<Response> _blmove(K source, K destination, Position positionInSource, Position positionInDest, Duration timeout) {
        nonNull(source, "source");
        nonNull(destination, "destination");
        nonNull(positionInSource, "positionInSource");
        nonNull(positionInDest, "positionInDest");
        validate(timeout, "timeout");

        return execute(RedisCommand.of(Command.BLMOVE).put(marshaller.encode(source))
                .put(marshaller.encode(destination))
                .put(positionInSource.name())
                .put(positionInDest.name())
                .put(durationToSeconds(timeout)));

    }

    V decodeV(Response r) {
        return marshaller.decode(typeOfValue, r);
    }

    Uni<Response> _blmpop(Duration timeout, Position position, K... keys) {
        nonNull(position, "position");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validate(timeout, "timeout");
        RedisCommand cmd = RedisCommand.of(Command.BLMPOP);
        cmd.put(durationToSeconds(timeout));
        cmd.put(keys.length);
        cmd.putAll(marshaller.encode(keys));
        cmd.put(position.name());

        return execute(cmd);
    }

    KeyValue<K, V> decodeKeyValue(Response r) {
        if (r != null && r.getDelegate() != null) {
            Response r1 = r.get(0);
            Response r2 = r.get(1);
            K key = marshaller.decode(typeOfKey, r1);
            V val = marshaller.decode(typeOfValue, r2);
            return new KeyValue<>(key, val);
        }
        return null;
    }

    KeyValue<K, V> decodeKeyValueWithList(Response r) {
        if (r != null && r.getDelegate() != null) {
            Response r1 = r.get(0);
            Response r2 = r.get(1).get(0);
            K key = marshaller.decode(typeOfKey, r1);
            V val = marshaller.decode(typeOfValue, r2);
            return new KeyValue<>(key, val);
        }
        return null;
    }

    Uni<Response> _blmpop(Duration timeout, Position position, int count, K... keys) {
        nonNull(position, "position");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validate(timeout, "timeout");
        positive(count, "count");

        RedisCommand cmd = RedisCommand.of(Command.BLMPOP);
        cmd.put(durationToSeconds(timeout));
        cmd.put(keys.length);
        cmd.putAll(marshaller.encode(keys));
        cmd.put(position.name());
        cmd.put("COUNT").put(count);

        return execute(cmd);

    }

    List<KeyValue<K, V>> decodeListOfKeyValue(Response r) {
        if (r == null || r.getDelegate() == null) {
            return Collections.emptyList();
        }
        List<KeyValue<K, V>> res = new ArrayList<>();
        K key = marshaller.decode(typeOfKey, r.get(0).toBytes());
        for (Response item : r.get(1)) {
            if (item == null) {
                res.add(KeyValue.of(key, null));
            } else {
                res.add(KeyValue.of(key, marshaller.decode(typeOfValue, item)));
            }
        }
        return res;
    }

    Uni<Response> _blpop(Duration timeout, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validate(timeout, "timeout");

        RedisCommand cmd = RedisCommand.of(Command.BLPOP);
        cmd.putAll(marshaller.encode(keys));
        cmd.put(durationToSeconds(timeout));

        return execute(cmd);
    }

    Uni<Response> _brpop(Duration timeout, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validate(timeout, "timeout");

        RedisCommand cmd = RedisCommand.of(Command.BRPOP);
        cmd.putAll(marshaller.encode(keys));
        cmd.put(durationToSeconds(timeout));

        return execute(cmd);
    }

    Uni<Response> _brpoplpush(Duration timeout, K source, K destination) {
        validate(timeout, "timeout");
        nonNull(source, "source");
        nonNull(destination, "destination");

        return execute(RedisCommand.of(Command.BRPOPLPUSH).put(marshaller.encode(source))
                .put(marshaller.encode(destination)).put(durationToSeconds(timeout)));
    }

    Uni<Response> _lindex(K key, long index) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.LINDEX).put(marshaller.encode(key)).put(index));
    }

    Uni<Response> _linsertBeforePivot(K key, V pivot, V element) {
        nonNull(key, "key");
        nonNull(pivot, "pivot");
        nonNull(element, "element");
        return execute(RedisCommand.of(Command.LINSERT).put(marshaller.encode(key)).put("BEFORE")
                .put(marshaller.encode(pivot)).put(marshaller.encode(element)));
    }

    Uni<Response> _linsertAfterPivot(K key, V pivot, V element) {
        nonNull(key, "key");
        nonNull(pivot, "pivot");
        nonNull(element, "element");
        return execute(RedisCommand.of(Command.LINSERT).put(marshaller.encode(key)).put("AFTER")
                .put(marshaller.encode(pivot)).put(marshaller.encode(element)));
    }

    Uni<Response> _llen(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.LLEN).put(marshaller.encode(key)));
    }

    Uni<Response> _lmove(K source, K destination, Position positionInSource, Position positionInDest) {
        nonNull(source, "source");
        nonNull(destination, "destination");
        nonNull(positionInSource, "positionInSource");
        nonNull(positionInDest, "positionInDest");

        return execute(RedisCommand.of(Command.LMOVE)
                .put(marshaller.encode(source))
                .put(marshaller.encode(destination))
                .put(marshaller.encode(positionInSource.name()))
                .put(marshaller.encode(positionInDest.name())));
    }

    Uni<Response> _lmpop(Position position, K... keys) {
        nonNull(position, "position");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");

        RedisCommand cmd = RedisCommand.of(Command.LMPOP);
        cmd.put(keys.length);
        cmd.putAll(marshaller.encode(keys));
        cmd.put(position.name());
        return execute(cmd);
    }

    Uni<Response> _lmpop(Position position, int count, K... keys) {
        nonNull(position, "position");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        positive(count, "count");

        RedisCommand cmd = RedisCommand.of(Command.LMPOP);
        cmd.put(keys.length);
        cmd.putAll(marshaller.encode(keys));
        cmd.put(position.name());
        cmd.put("COUNT").put(count);
        return execute(cmd);
    }

    Uni<Response> _lpop(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.LPOP).put(marshaller.encode(key)));
    }

    Uni<Response> _lpop(K key, int count) {
        nonNull(key, "key");
        positive(count, "count");
        return execute(RedisCommand.of(Command.LPOP).put(marshaller.encode(key)).put(count));
    }

    List<V> decodeListV(Response r) {
        return marshaller.decodeAsList(r, typeOfValue);
    }

    Uni<Response> _lpos(K key, V element) {
        return _lpos(key, element, DEFAULT_INSTANCE);
    }

    Uni<Response> _lpos(K key, V element, LPosArgs args) {
        nonNull(key, "key");
        nonNull(element, "element");
        RedisCommand cmd = RedisCommand.of(Command.LPOS);
        cmd.put(marshaller.encode(key));
        cmd.put(marshaller.encode(element));
        cmd.putArgs(args);
        return execute(cmd);
    }

    Long decodeLongOrNull(Response r) {
        if (r == null) {
            return null;
        }
        return r.toLong();
    }

    Uni<Response> _lpos(K key, V element, int count) {
        return _lpos(key, element, count, DEFAULT_INSTANCE);
    }

    Uni<Response> _lpos(K key, V element, int count, LPosArgs args) {
        nonNull(key, "key");
        nonNull(element, "element");
        Validation.positiveOrZero(count, "count"); // 0 -> All matches
        RedisCommand cmd = RedisCommand.of(Command.LPOS);
        cmd.put(marshaller.encode(key));
        cmd.put(marshaller.encode(element));
        cmd.put("COUNT").put(count);
        cmd.putArgs(args);
        return execute(cmd);
    }

    List<Long> decodeListOfLongs(Response r) {
        return marshaller.decodeAsList(r, Response::toLong);
    }

    Uni<Response> _lpush(K key, V... elements) {
        nonNull(key, "key");
        notNullOrEmpty(elements, "elements");
        doesNotContainNull(elements, "elements");
        RedisCommand cmd = RedisCommand.of(Command.LPUSH);
        cmd.put(marshaller.encode(key)).putAll(marshaller.encode(elements));
        return execute(cmd);
    }

    Uni<Response> _lpushx(K key, V... elements) {
        nonNull(key, "key");
        notNullOrEmpty(elements, "elements");
        doesNotContainNull(elements, "elements");
        RedisCommand cmd = RedisCommand.of(Command.LPUSHX);
        cmd.put(marshaller.encode(key)).putAll(marshaller.encode(elements));
        return execute(cmd);
    }

    Uni<Response> _lrange(K key, long start, long stop) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.LRANGE).put(marshaller.encode(key))
                .put(start)
                .put(stop));
    }

    Uni<Response> _lrem(K key, long count, V element) {
        nonNull(key, "key");
        nonNull(element, "element");

        return execute(RedisCommand.of(Command.LREM).put(marshaller.encode(key))
                .put(count).put(marshaller.encode(element)));
    }

    Uni<Response> _lset(K key, long index, V element) {
        nonNull(key, "key");
        nonNull(element, "element");

        return execute(RedisCommand.of(Command.LSET).put(marshaller.encode(key))
                .put(index).put(marshaller.encode(element)));
    }

    Uni<Response> _ltrim(K key, long start, long stop) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.LTRIM).put(marshaller.encode(key))
                .put(start).put(stop));
    }

    Uni<Response> _rpop(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.RPOP).put(marshaller.encode(key)));
    }

    Uni<Response> _rpop(K key, int count) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.RPOP).put(marshaller.encode(key)).put(count));
    }

    Uni<Response> _rpoplpush(K source, K destination) {
        nonNull(source, "source");
        nonNull(destination, "destination");
        return execute(RedisCommand.of(Command.RPOPLPUSH)
                .put(marshaller.encode(source)).put(marshaller.encode(destination)));
    }

    Uni<Response> _rpush(K key, V... values) {
        nonNull(key, "key");
        notNullOrEmpty(values, "values");
        doesNotContainNull(values, "values");
        RedisCommand cmd = RedisCommand.of(Command.RPUSH);
        cmd.put(marshaller.encode(key)).putAll(marshaller.encode(values));
        return execute(cmd);

    }

    Uni<Response> _rpushx(K key, V... values) {
        nonNull(key, "key");
        notNullOrEmpty(values, "values");
        doesNotContainNull(values, "values");
        RedisCommand cmd = RedisCommand.of(Command.RPUSHX);
        cmd.put(marshaller.encode(key)).putAll(marshaller.encode(values));
        return execute(cmd);
    }
}
