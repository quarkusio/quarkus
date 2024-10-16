package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.Validation.isBit;
import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.util.List;

import io.quarkus.redis.datasource.bitmap.BitFieldArgs;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

class AbstractBitMapCommands<K> extends AbstractRedisCommands {

    AbstractBitMapCommands(RedisCommandExecutor redis, Type k) {
        super(redis, new Marshaller(k));
    }

    Uni<Response> _bitcount(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.BITCOUNT).put(marshaller.encode(key)));
    }

    Uni<Response> _bitcount(K key, long start, long end) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.BITCOUNT).put(marshaller.encode(key)).put(start).put(end));
    }

    Uni<Response> _bitfield(K key, BitFieldArgs bitFieldArgs) {
        nonNull(key, "key");
        nonNull(bitFieldArgs, "bitFieldArgs");
        return execute(RedisCommand.of(Command.BITFIELD).put(marshaller.encode(key)).putArgs(bitFieldArgs));
    }

    Uni<Response> _bitpos(K key, int bit) {
        nonNull(key, "key");
        isBit(bit, "bit");
        return execute(RedisCommand.of(Command.BITPOS).put(marshaller.encode(key)).put(bit));
    }

    Uni<Response> _bitpos(K key, int bit, long start) {
        nonNull(key, "key");
        isBit(bit, "bit");
        return execute(RedisCommand.of(Command.BITPOS).put(marshaller.encode(key)).put(bit).put(start));
    }

    Uni<Response> _bitpos(K key, int bit, long start, long end) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.BITPOS).put(marshaller.encode(key)).put(bit).put(start).put(end));
    }

    Uni<Response> _bitopAnd(K destination, K... keys) {
        nonNull(destination, "destination");
        notNullOrEmpty(keys, "keys");
        return execute(RedisCommand.of(Command.BITOP).put("AND")
                .put(marshaller.encode(destination)).put(marshaller.encode(keys)));
    }

    Uni<Response> _bitopNot(K destination, K source) {
        nonNull(destination, "destination");
        nonNull(source, "source");
        return execute(RedisCommand.of(Command.BITOP).put("NOT")
                .put(marshaller.encode(destination)).put(marshaller.encode(source)));
    }

    final Uni<Response> _bitopOr(K destination, K... keys) {
        nonNull(destination, "destination");
        notNullOrEmpty(keys, "keys");
        return execute(RedisCommand.of(Command.BITOP).put("OR")
                .put(marshaller.encode(destination)).put(marshaller.encode(keys)));
    }

    @SafeVarargs
    final Uni<Response> _bitopXor(K destination, K... keys) {
        nonNull(destination, "destination");
        notNullOrEmpty(keys, "keys");
        return execute(RedisCommand.of(Command.BITOP).put("XOR")
                .put(marshaller.encode(destination)).put(marshaller.encode(keys)));
    }

    List<Long> decodeListOfLongs(Response r) {
        return marshaller.decodeAsList(r, Response::toLong);
    }

    Uni<Response> _setbit(K key, long offset, int value) {
        nonNull(key, "key");
        isBit(value, "value");
        return execute(RedisCommand.of(Command.SETBIT).put(marshaller.encode(key)).put(offset).put(value));
    }

    Uni<Response> _getbit(K key, long offset) {
        return execute(RedisCommand.of(Command.GETBIT).put(marshaller.encode(key)).put(offset));
    }

}
