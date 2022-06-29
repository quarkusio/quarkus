package io.quarkus.redis.datasource.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.api.hash.ReactiveHashScanCursor;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

public class HScanReactiveCursorImpl<F, V> extends AbstractRedisCommands implements ReactiveHashScanCursor<F, V> {

    private final byte[] key;
    private final Class<F> typeOfField;
    private final Class<V> typeOfValue;
    private long cursor;
    private final List<String> extra = new ArrayList<>();

    public <K> HScanReactiveCursorImpl(RedisCommandExecutor redis, K key, Marshaller marshaller, Class<F> typeOfField,
            Class<V> typeOfValue,
            List<String> extra) {
        super(redis, marshaller);
        this.key = marshaller.encode(key);
        this.typeOfField = typeOfField;
        this.typeOfValue = typeOfValue;
        this.cursor = INITIAL_CURSOR_ID;
        this.extra.addAll(extra);
    }

    @Override
    public boolean hasNext() {
        return cursor != 0;
    }

    @Override
    public Uni<Map<F, V>> next() {
        long pos = cursor == INITIAL_CURSOR_ID ? 0 : cursor;
        RedisCommand cmd = RedisCommand.of(Command.HSCAN);
        cmd.put(key);
        cmd.put(pos);
        cmd.putAll(extra);
        return execute(cmd)
                .invoke(response -> cursor = response.get(0).toLong())
                .map(response -> decode(response.get(1)));
    }

    public Map<F, V> decode(Response response) {
        Map<F, V> map = new LinkedHashMap<>();
        F field = null;
        for (Response member : response) {
            if (field == null) {
                field = marshaller.decode(typeOfField, member);
            } else {
                V val = marshaller.decode(typeOfValue, member);
                map.put(field, val);
                field = null;
            }
        }
        return map;
    }

    @Override
    public long cursorId() {
        return cursor;
    }
}
